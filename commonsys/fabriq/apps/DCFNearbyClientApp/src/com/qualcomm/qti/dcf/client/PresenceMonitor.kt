/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.qualcomm.qti.dcf.client.service.IDeviceInfoService
import com.qualcomm.qti.dcf.nearbyclient.R
import kotlin.math.abs

object PresenceMonitor: DCFDevicesListener, UserSettings.OnSettingsChangedListener  {

    const val DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00"
    private const val TAG = "PresenceMonitor"
    private const val DEVICE_INFO_ACTION = "com.qualcomm.qti.dcf.action.DEVICE_INFO_SERVICE"
    private const val SERVICE_PACKAGE = "com.qualcomm.qti.dcf.client.service"


    private const val DE_ADDRESS_TYPE_BLE_PUBLIC = 0x00
    private const val DE_ADDRESS_TYPE_BLE_PRIVATE = 0x01
    private const val DE_ADDRESS_TYPE_BT_CLASSIC = 0x02
    private const val DE_ADDRESS_TYPE_WIFI = 0x03
    private const val DE_ADDRESS_TYPE_UWB = 0x04

    private const val ADDRESS_LENGTH_IN_BYTE = 6

    var batteryLevel: Int = 0
    var batteryChargingStatus: Int = 0
    var deviceInfoListener: OnDeviceInfoAvailableListener? = null

    private var lastBroadcastBatteryLevel: Int = -1
    private var lastBroadcastChargingStatus = -1

    private val onDevicesChangedListeners: MutableList<OnDevicesChangedListener> = mutableListOf()
    private val onSingleDeviceChangedListenerMap: MutableMap<String, OnSingleDeviceChangedListener>
            = mutableMapOf()
    private val batteryListeners: MutableList<BatteryListener> = mutableListOf()

    private val batteryReceiver = BatteryReceiver()
    private val bluetoothReceiver = BluetoothReceiver()

    private var deviceInfoService: IDeviceInfoService? = null
    private val deviceInfoConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "onServiceConnected: get Binder success")
            deviceInfoService = IDeviceInfoService.Stub.asInterface(service)
            // maybe valid default device name or user edited device name
            val currentDeviceName = UserSettings.deviceName
            DCFAdvertiseManager.getInstance().addDataElements(
                parseToDE(
                    getBleAddress().replace(":", ""),
                    currentDeviceName,
                    UserSettings.deviceType,
                    UserSettings.deviceStatus,
                    batteryChargingStatus,
                    batteryLevel,
                    getBleAddress(),
                    getWifiAddress(),
                ))
            // store the current device name
            UserSettings.deviceName = currentDeviceName
            deviceInfoListener?.onDeviceInfoAvailable()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            deviceInfoService = null
        }
    }

    override fun onDevicesChanged(devices: List<DCFDevice>) {
        val deviceMap = devices.map { dcfDevice ->
            parseToPresenceDevice(dcfDevice)
        }.associateBy { it.id }

        onDevicesChangedListeners.forEach {
            it.onDevicesChanged(deviceMap.values.toList())
        }
        deviceMap.forEach {
            onSingleDeviceChangedListenerMap[it.key]?.onSingleDeviceChanged(it.value)
        }
    }

    override fun onSettingsChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when(key){
            UserSettings.KEY_DEVICE_NAME,
            UserSettings.KEY_DEVICE_TYPE,
            UserSettings.KEY_DEVICE_STATUS ->
                DCFAdvertiseManager.getInstance().addDataElements(parseToDE(
                        name = UserSettings.deviceName, type = UserSettings.deviceType,
                        status = UserSettings.deviceStatus))
        }
    }

    fun init(context: Context){
        context.applicationContext.registerReceiver(batteryReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        })

        context.applicationContext.registerReceiver(bluetoothReceiver, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        })

        // initial current battery charging status and battery level
        IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }.also {
            batteryChargingStatus = getBatteryChargingStatus(it)
            batteryLevel = getBatteryLevel(it)
        }

        UserSettings.addOnSettingsChangedListener(this)

        context.bindService(Intent(DEVICE_INFO_ACTION).apply {
                setPackage(SERVICE_PACKAGE)
            }, deviceInfoConnection, Context.BIND_AUTO_CREATE).also {
            Log.w(TAG, "init: Service Bind ${if (it) "success" else "failed !!!"}")
        }
    }

    fun addOnDevicesChangedListener(listener: OnDevicesChangedListener){
        val isNeedListenDevicesChange = onDevicesChangedListeners.isEmpty()
                && onSingleDeviceChangedListenerMap.isEmpty()
        onDevicesChangedListeners.add(listener)

        if (isNeedListenDevicesChange)
            DCFScanManager.getInstance().registerDCFDevicesListener(this)
    }

    fun removeOnDevicesChangedListener(listener: OnDevicesChangedListener){
        onDevicesChangedListeners.remove(listener)
        if (onDevicesChangedListeners.isEmpty() && onSingleDeviceChangedListenerMap.isEmpty())
            DCFScanManager.getInstance().unregisterDCFDevicesListener(this)
    }

    fun addOnSingleDeviceChangedListener(deviceId: String, listener: OnSingleDeviceChangedListener){
        val isNeedListenDevicesChange = onDevicesChangedListeners.isEmpty()
                && onSingleDeviceChangedListenerMap.isEmpty()
        onSingleDeviceChangedListenerMap[deviceId] = listener
        if (isNeedListenDevicesChange)
            DCFScanManager.getInstance().registerDCFDevicesListener(this)
    }

    fun removeOnSingleDeviceChangedListener(deviceId: String, listener: OnSingleDeviceChangedListener){
        onSingleDeviceChangedListenerMap.remove(deviceId, listener)
        if (onDevicesChangedListeners.isEmpty() && onSingleDeviceChangedListenerMap.isEmpty())
            DCFScanManager.getInstance().unregisterDCFDevicesListener(this)
    }

    fun addBatteryListener(listener: BatteryListener){
        batteryListeners.add(listener)
        listener.onBatteryStatusChanged(batteryChargingStatus, batteryLevel)
    }

    fun removeBatteryListener(listener: BatteryListener){
        batteryListeners.remove(listener)
    }

    fun createLocalDevice(): DCFPresenceDevice =
        DCFPresenceDevice(
            getBleAddress().replace(":", ""),
            UserSettings.deviceName,
            UserSettings.deviceType,
            UserSettings.deviceStatus,
            batteryChargingStatus,
            batteryLevel,
            getBleAddress(),
            getWifiAddress(),
            true)

    fun getBleAddress(): String = deviceInfoService?.getBleAddress() ?: DEFAULT_MAC_ADDRESS

    fun getWifiAddress(): String = deviceInfoService?.getWifiFactoryAddress() ?: DEFAULT_MAC_ADDRESS

    private fun parseToPresenceDevice(dcfDevice: DCFDevice): DCFPresenceDevice {
        var deviceId = ""
        var deviceName = ""
        var deviceType = 1
        var deviceStatus = 0
        var bleAddress = ""
        var wifiAddress = ""
        var batteryStatus = 0
        var batteryLevel = 100

        for (de in dcfDevice.dataElements){
            when(de.tag){
                DE_TAG_DEVICE_ID -> {
                    deviceId = byteArrayToAddress(de.data, false)!!
                }
                DE_TAG_DEVICE_INFO -> {
                    // first byte for Device Type
                    deviceType = de.data[0].toInt()
                    // the second and third byte for Device Status
                    deviceStatus = (de.data[1].toInt() shl 8) + de.data[2]
                    // the fourth byte are name length, we don't need it.
                    deviceName = de.data.decodeToString(4)
                }
                DE_TAG_DEVICE_ADDRESS -> {
                    var index = 0
                    while (index < de.data.size){
                        when(de.data[index].toInt()){
                            DE_ADDRESS_TYPE_BT_CLASSIC -> bleAddress = byteArrayToAddress(de.data
                                .sliceArray(index + 1..index + ADDRESS_LENGTH_IN_BYTE))!!
                            DE_ADDRESS_TYPE_WIFI -> wifiAddress = byteArrayToAddress(de.data
                                .sliceArray(index + 1..index + ADDRESS_LENGTH_IN_BYTE))!!
                        }
                        index += (ADDRESS_LENGTH_IN_BYTE + 1)
                    }
                }
                DE_TAG_BATTERY_STATUS -> {
                    batteryStatus = de.data[0].toInt()
                    batteryLevel = de.data[1].toInt()
                }
            }
        }

        return DCFPresenceDevice(deviceId, deviceName, deviceType, deviceStatus, batteryStatus,
            batteryLevel, bleAddress, wifiAddress)
    }

    private fun parseToDE(id: String? = null,
                          name: String? = null,
                          type: Int = 0,
                          status: Int = 0,
                          batteryStatus: Int = -1,
                          batteryLevel: Int = -1,
                          bleAddress: String? = null,
                          wifiAddress: String? = null): List<DCFDataElement> =
        mutableListOf<DCFDataElement>().apply {
            if (id != null) add(DCFDataElement(DE_TAG_DEVICE_ID, addressToByteArray(id)))
            if (name != null)
                add(DCFDataElement(DE_TAG_DEVICE_INFO, byteArrayOf(type.toByte(),
                        ((status shr 8) and 0xff).toByte(), (status and 0xff).toByte(),
                        name.toByteArray().size.toByte()) + name.toByteArray()))
            if (batteryStatus != -1)
                add(DCFDataElement(DE_TAG_BATTERY_STATUS,
                    byteArrayOf(batteryStatus.toByte(), batteryLevel.toByte())))
            if (bleAddress != null){
                var addressArray = byteArrayOf(DE_ADDRESS_TYPE_BT_CLASSIC.toByte()) +
                        addressToByteArray(bleAddress)
                if (wifiAddress != null){
                    addressArray += (byteArrayOf(DE_ADDRESS_TYPE_WIFI.toByte())
                            + addressToByteArray(wifiAddress))
                }
                add(DCFDataElement(DE_TAG_DEVICE_ADDRESS, addressArray))
            }
        }

    private fun getBatteryLevel(batteryStatus: Intent?): Int =
        batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        }!!

    private fun getBatteryChargingStatus(batteryStatus: Intent?): Int {
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING) ?: BatteryManager.BATTERY_STATUS_NOT_CHARGING
        return when(status) {
            BatteryManager.BATTERY_STATUS_FULL -> DCFPresenceDevice.BatteryChargingStatus.CHARGED
            BatteryManager.BATTERY_STATUS_CHARGING ->
                DCFPresenceDevice.BatteryChargingStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING ->
                DCFPresenceDevice.BatteryChargingStatus.DISCHARGING
            else -> DCFPresenceDevice.BatteryChargingStatus.UNKNOWN
        }
    }

    private fun byteArrayToAddress(array: ByteArray, needSeparate: Boolean = true): String? {
        if (array.size > ADDRESS_LENGTH_IN_BYTE) return null

        val address = StringBuilder()
        for ((index, byte) in array.withIndex()){
            address.append(String.format("%02x", byte))
            if (needSeparate && index != array.size - 1)
                address.append(":")
        }

        return address.toString()
    }

    private fun addressToByteArray(address: String): ByteArray {
        var array = byteArrayOf()
        var index = 0
        while (index < address.length){
            if (address[index] == ':') {
                index += 1
                continue
            }
            array += address.slice(index..index + 1).toInt(16).toByte()
            index += 2
        }

        return array
    }

    interface OnDevicesChangedListener {
        fun onDevicesChanged(devices: List<DCFPresenceDevice>)
    }

    interface OnSingleDeviceChangedListener {
        fun onSingleDeviceChanged(device: DCFPresenceDevice)
    }

    interface BatteryListener {
        fun onBatteryStatusChanged(newStatus: Int, newLevel: Int)
    }

    interface OnDeviceInfoAvailableListener {
        fun onDeviceInfoAvailable()
    }

    class BatteryReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            batteryLevel = getBatteryLevel(intent)
            batteryChargingStatus = getBatteryChargingStatus(intent)

            if (batteryListeners.isNotEmpty()){
                for (listener in batteryListeners){
                    listener.onBatteryStatusChanged(batteryChargingStatus, batteryLevel)
                }
            }

            Log.i(TAG, "onReceive: Battery Status Changed. level:$batteryLevel " +
                    "- charging status:$batteryChargingStatus")

            if (lastBroadcastBatteryLevel == -1 || lastBroadcastChargingStatus == -1 ||
                lastBroadcastChargingStatus != batteryChargingStatus ||
                abs(lastBroadcastBatteryLevel - batteryLevel) >=
                UserSettings.batteryBroadcastInterval
            ){
                if (deviceInfoService != null) {
                    DCFAdvertiseManager.getInstance().addDataElements(
                        parseToDE(
                            batteryStatus = batteryChargingStatus,
                            batteryLevel = batteryLevel,
                            bleAddress = getBleAddress(),
                            wifiAddress = getWifiAddress(),
                        ))
                }
                lastBroadcastBatteryLevel = batteryLevel
                lastBroadcastChargingStatus = batteryChargingStatus
            }
        }
    }

    class BluetoothReceiver: BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val bluetoothState = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.STATE_ON)

            when(bluetoothState) {
                BluetoothAdapter.STATE_ON -> {
                    // need to restart broadcast if bluetooth turn on.
                    DCFAdvertiseManager.getInstance().addDataElements(
                        parseToDE(
                            getBleAddress().replace(":", ""),
                            UserSettings.deviceName,
                            UserSettings.deviceType,
                            UserSettings.deviceStatus,
                            batteryChargingStatus,
                            batteryLevel,
                            getBleAddress(),
                            getWifiAddress(),
                        ))
                }
                BluetoothAdapter.STATE_OFF -> Toast.makeText(context,
                    context!!.getString(R.string.toast_warning_bluetooth_off), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

}
