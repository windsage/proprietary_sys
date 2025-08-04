/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.nearby.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.widget.Toast
import com.qualcomm.qti.dcf.nearbyclient.R
import java.util.concurrent.Executors

object DCFScanManager: UserSettings.OnSettingsChangedListener {

    @Target(AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ScanMode {
        companion object {
            const val NO_POWER = -1
            const val LOW_POWER = 0
            const val BALANCED = 1
            const val LOW_LATENCY = 2
        }
    }

    @Target(AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ResultType {
        companion object {
            const val DISCOVER = 0
            const val UPDATE = 1
            const val LOST = 2
        }
    }

    private const val NOTIFICATION_CHANNEL_ID_PRESENCE = "channel_id_presence"
    private const val NOTIFICATION_ID_PRESENCE = 10001
    private const val PRESENCE_INTENT_REQUEST_CODE = 0


    private const val WORKER_THREAD_NAME = "DCF_Nearby_Worker"
    private const val MSG_LIFE_CHECK: Int = 100
    private const val MSG_DISPATCH_SCAN_RESULT: Int = 101

    private const val LIFE_CHECK_IMMEDIATELY = 0L
    private const val LIFE_CHECK_INTERVAL = 1 * 1000L

    // see NearbyManager#ScanStatus.SUCCESS
    private const val SCAN_SUCCESS = 1
    private const val PATH_LOSS: Int = 1000
    private const val PRESENCE_ACTION = 124

    private lateinit var notificationManager: NotificationManager
    private lateinit var mPresenceChannel: NotificationChannel
    private lateinit var presencePendingIntent: PendingIntent
    private lateinit var wakeLock: WakeLock
    private var shouldShowNotification = true
    val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {
            if (activity is PresenceDevicesActivity) {
                shouldShowNotification = false
            }
        }

        override fun onActivityPaused(activity: Activity) {
            if (activity is PresenceDevicesActivity) {
                shouldShowNotification = true
            }
        }

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }

    private lateinit var applicationContext: Context

    private var isScanning = false
    private var devicesMap: MutableMap<String, DCFDevice> = mutableMapOf()
    private val devicesListeners: MutableList<DCFDevicesListener> = mutableListOf()
    private val scanCallback: ScanCallback = object : ScanCallback {
        override fun onDiscovered(device: NearbyDevice) {
            if (device !is PresenceDevice) return

            Log.d(TAG, "Nearby Presence: A device discovered($device, ${device.name}")

            // schedule to worker thread to avoid concurrent issues.
            scheduleDispatchScanResult(ScanResult(device, ResultType.DISCOVER))
        }

        override fun onUpdated(device: NearbyDevice) {
            if (device !is PresenceDevice) return
            Log.d(TAG, "Nearby Presence: A device updated($device, ${device.name}," +
                    "${device.deviceId})!")

            // schedule to worker thread to avoid concurrent issues.
            scheduleDispatchScanResult(ScanResult(device, ResultType.UPDATE))
        }

        override fun onLost(device: NearbyDevice) {
            if (device !is PresenceDevice) return

            Log.d(TAG, "Nearby Presence: A device lost($device, ${device.name}," +
                    "${device.deviceId})!")

            // schedule to worker thread to avoid concurrent issues.
            scheduleDispatchScanResult(ScanResult(device, ResultType.LOST))
        }
    }

    private lateinit var scanHandler: DcfScanHandler

    @JvmStatic
    fun getInstance(): DCFScanManager = DCFScanManager

    fun init(context: Context){
        applicationContext = context

        // init worker thread handler and start it.
        scanHandler = DcfScanHandler(HandlerThread(WORKER_THREAD_NAME).also {
            it.start()
        }.looper)

        UserSettings.addOnSettingsChangedListener(this)

        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mPresenceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID_PRESENCE,
            context.getString(R.string.notification_channel_name_presence),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(mPresenceChannel)
        presencePendingIntent = PendingIntent.getActivity(
            context,
            PRESENCE_INTENT_REQUEST_CODE,
            Intent(context, PresenceDevicesActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, context.packageName
            )
    }

    fun registerDCFDevicesListener(listener: DCFDevicesListener) {
        if (!isScanning && UserSettings.scanEnable) {
            isScanning = startScan().also { scanStatus ->
                if (scanStatus == SCAN_SUCCESS) {
                    Toast.makeText(applicationContext, "Started scanning success",
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "Failed to start scanning, " +
                            "status:$scanStatus", Toast.LENGTH_LONG).show()
                }
            } == SCAN_SUCCESS
        }
        devicesListeners.add(listener)
        // broadcast to new registered listener immediately.(maybe no device)
        listener.onDevicesChanged(devicesMap.values.toList())
    }

    fun unregisterDCFDevicesListener(listener: DCFDevicesListener) {
        devicesListeners.remove(listener)
    }

    override fun onSettingsChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when(key){
            UserSettings.KEY_SCAN_ENABLE -> {
                val enable = UserSettings.scanEnable
                Log.i(TAG, "Scan ${if (enable) "enable" else "disable"}")
                if (enable) {
                    isScanning = startScan().also { scanStatus ->
                        if (scanStatus == SCAN_SUCCESS) {
                            Toast.makeText(applicationContext, "Started scanning success",
                                Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(applicationContext, "Failed to start scanning, " +
                                    "status:$scanStatus", Toast.LENGTH_LONG).show()
                        }
                    } == SCAN_SUCCESS
                } else {
                    stopScan()
                }
            }
            UserSettings.KEY_SCAN_MODE -> {
                Log.i(TAG, "Scan Mode was updated to: ${UserSettings.scanMode}")
                if (UserSettings.scanEnable) {
                    stopScan()
                    isScanning = startScan().also { scanStatus ->
                        if (scanStatus == SCAN_SUCCESS) {
                            Toast.makeText(applicationContext, "Started scanning success",
                                Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(applicationContext, "Failed to start scanning, " +
                                    "status:$scanStatus", Toast.LENGTH_LONG).show()
                        }
                    } == SCAN_SUCCESS
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan(dataFilter: List<DataElement>? = null): Int {
        if (!credentialsValid) return -1

        val scanRequestBuilder = ScanRequest.Builder()
            .setScanType(ScanRequest.SCAN_TYPE_NEARBY_PRESENCE)
            .setScanMode(UserSettings.scanMode)

        val publicCredential = PublicCredential.Builder(
            getSecretId()!!,
            getAuthenticityKey()!!,
            getPublicKey()!!,
            getEncryptedMetadataBytes()!!,
            getMetadataEncryptionKeyTag()!!)
            .build()

        val presenceScanFilterBuilder = PresenceScanFilter.Builder()
            .setMaxPathLoss(PATH_LOSS)
            .addCredential(publicCredential)
            .addPresenceAction(PRESENCE_ACTION)

        if (!dataFilter.isNullOrEmpty()) {
            for (df in dataFilter){
                presenceScanFilterBuilder.addExtendedProperty(df)
            }
        }
        scanRequestBuilder.addScanFilter(presenceScanFilterBuilder.build())

        return applicationContext.getSystemService(NearbyManager::class.java)
            .startScan(scanRequestBuilder.build(), Executors.newSingleThreadExecutor(), scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan(){
        applicationContext.getSystemService(NearbyManager::class.java).stopScan(scanCallback)
        isScanning = false
    }

    private fun dispatchScanResults(block: (() -> Boolean)? = null){
        // if return false, mean no need to dispatch scan results.
        if (block?.invoke() == false) return

        for (listener in devicesListeners){
            listener.onDevicesChanged(devicesMap.values.toList())
        }
    }

    private fun convertToDCFDevice(device: PresenceDevice): DCFDevice =
        DCFDevice(getDeviceId(device), device.extendedProperties.map { de -> convertToDCFDe(de) },
            System.currentTimeMillis())

    private fun getDeviceId(device: PresenceDevice): String {
        var deviceId = device.deviceId

        device.extendedProperties.forEach {
            if (it.key == DE_TAG_DEVICE_ID){
                deviceId = it.value.decodeToString()
            }
        }

        return deviceId
    }

    private fun convertToDCFDe(de: DataElement): DCFDataElement = DCFDataElement(de.key, de.value)

    private fun showDiscoveredNotification() {
        wakeLock.acquire()
        val discoveredNotification: Notification = Notification.Builder(applicationContext,
            NOTIFICATION_CHANNEL_ID_PRESENCE)
            .setSmallIcon(R.drawable.ic_baseline_phone_android_24)
            .setContentTitle(applicationContext.getString(R.string.notification_presence_Title))
            .setContentText(applicationContext.getString(R.string.notification_presence_content))
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(presencePendingIntent)
            .build()
        notificationManager.notify(NOTIFICATION_ID_PRESENCE, discoveredNotification)
        wakeLock.release()
    }

    private fun scheduleLifeCheckDelay(delay: Long) {
        scanHandler.removeMessages(MSG_LIFE_CHECK)
        scanHandler.sendMessageDelayed(Message.obtain(scanHandler, MSG_LIFE_CHECK), delay)
    }

    private fun scheduleDispatchScanResult(scanResult: ScanResult) {
        scanHandler.sendMessage(Message.obtain(scanHandler, MSG_DISPATCH_SCAN_RESULT).apply {
            obj = scanResult
        })
    }

    private fun handleCheckDeviceLifePeriodically() {

        var changed = false
        // filter devices that still alive.
        // if some device dies, mark and result will be dispatched later.
        devicesMap = devicesMap.filter {
            it.value.isAlive().also { isAlive -> if (!isAlive) changed = true }
        }.toMutableMap()

        // some device dies, dispatch devices that still alive.
        if (changed) {
            dispatchScanResults()
        }

        // Reschedule if still have alive devices, otherwise cancel
        if (devicesMap.isNotEmpty()) {
            scheduleLifeCheckDelay(LIFE_CHECK_INTERVAL)
        } else {
            Log.d(TAG, "No alive devices, stop checking device life")
            scanHandler.removeMessages(MSG_LIFE_CHECK)
        }
    }

    private fun handleDispatchScanResult(scanResult: ScanResult) {
        val newDcfDevice = convertToDCFDevice(scanResult.device)
        val id = newDcfDevice.deviceId

        dispatchScanResults {
            when (scanResult.resultType) {
                ResultType.DISCOVER -> {
                    // immediately schedule a life check of existing devices
                    scheduleLifeCheckDelay(LIFE_CHECK_IMMEDIATELY)

                    if (devicesMap[id] == null) {
                        // if is a new device, show a notification
                        if (shouldShowNotification) {
                            showDiscoveredNotification()
                        }
                        devicesMap[id] = newDcfDevice
                        true
                    } else if (devicesMap[id] == newDcfDevice) {
                        // already have a same device in the map, just update discovery timestamp
                        devicesMap[id]!!.discoveryTimestampMillis = System.currentTimeMillis()
                        // assign false to avoid dispatch scan results
                        false
                    } else {
                        // updated existing device and dispatch it.
                        devicesMap[id] = newDcfDevice
                        true
                    }
                }
                ResultType.UPDATE -> {
                    // update existing device directly.
                    devicesMap[id] = newDcfDevice
                    true
                }
                ResultType.LOST -> {
                    // remove existing device directly.
                    devicesMap.remove(id)
                    true
                }
                else -> false
            }
        }
    }

    private class DcfScanHandler(looper: Looper): Handler(looper) {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what) {
                MSG_LIFE_CHECK -> handleCheckDeviceLifePeriodically()
                MSG_DISPATCH_SCAN_RESULT -> handleDispatchScanResult(msg.obj as ScanResult)
            }
        }
    }

    private class ScanResult(val device: PresenceDevice, val resultType: @ResultType Int)
}
