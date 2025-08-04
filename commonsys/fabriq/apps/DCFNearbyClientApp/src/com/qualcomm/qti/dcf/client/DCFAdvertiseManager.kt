/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.nearby.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.*
import java.util.concurrent.Executors

object DCFAdvertiseManager: UserSettings.OnSettingsChangedListener {

    private const val BLE_MEDIUM = 1
    private const val PRESENCE_ACTION = 124

    private lateinit var applicationContext: Context
    private var isStartingAdvertise = false
    private var mDataElements: List<DCFDataElement> = listOf()

    private val broadcastCallback: BroadcastCallback = BroadcastCallback {
        Log.i(TAG, ": broadcast status changed: ${when(it) {
            BroadcastCallback.STATUS_OK -> "OK"
            BroadcastCallback.STATUS_FAILURE -> "Failure"
            BroadcastCallback.STATUS_FAILURE_ALREADY_REGISTERED -> "Failure already register"
            BroadcastCallback.STATUS_FAILURE_SIZE_EXCEED_LIMIT -> "Failure size exceed limit"
            BroadcastCallback.STATUS_FAILURE_MISSING_PERMISSIONS -> "Failure missing permissions"
            else -> "unknown" }
        }")

        isStartingAdvertise = false
    }

    private const val MSG_START_ADVERTISE = 0
    private const val MSG_STOP_ADVERTISE = 1
    private const val WORKER_THREAD_NAME = "DCF_Nearby_Advertise"
    private lateinit var advertiseHandler: DcfAdvertiseHandler

    @JvmStatic
    fun getInstance(): DCFAdvertiseManager = DCFAdvertiseManager

    fun init(context: Context){
        applicationContext = context
        advertiseHandler = DcfAdvertiseHandler(HandlerThread(WORKER_THREAD_NAME).also {
            it.start()
        }.looper)
        UserSettings.addOnSettingsChangedListener(this)
    }

    fun addDataElements(datas: List<DCFDataElement>){
        val deMap = mDataElements.associateBy { de -> de.tag }.toMutableMap().apply {
            putAll(datas.associateBy { de -> de.tag })
        }
        mDataElements = deMap.values.toList()

        // Before starting a new broadcast, the old broadcast must be stopped.
        // Otherwise it will return failure on V.
        scheduleStopBroadcast()
        scheduleStartBroadcast(mDataElements.map { dcfDe -> transfer(dcfDe) })
    }

    fun removeDataElements(datas: List<DCFDataElement>){
        val deMap = mDataElements.associateBy { de -> de.tag }.toMutableMap()
        datas.forEach { dcfDe ->
            if (deMap.containsKey(dcfDe.tag))
                deMap.remove(dcfDe.tag)
        }

        mDataElements = deMap.values.toList()

        scheduleStopBroadcast()
        scheduleStartBroadcast(mDataElements.map { dcfDe -> transfer(dcfDe) })
    }

    override fun onSettingsChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == UserSettings.KEY_BROADCAST_ENABLE){
            val enable = UserSettings.broadcastEnable
            Log.i(TAG, "advertising ${if (enable) "enable" else "disable"}")
            if (enable){
                scheduleStartBroadcast(mDataElements.map { dcfDe -> transfer(dcfDe) })
            } else {
                scheduleStopBroadcast()
            }
        }
    }

    private fun scheduleStartBroadcast(data: List<DataElement>, delay: Long = 0) {
        advertiseHandler.removeMessages(MSG_START_ADVERTISE)
        advertiseHandler.sendMessageDelayed(Message.obtain(advertiseHandler, MSG_START_ADVERTISE,
            data), delay)
    }

    @SuppressLint("MissingPermission")
    private fun startBroadcast(data: List<DataElement>){
        if (!UserSettings.broadcastEnable) return

        if (!credentialsValid) return

        if (isStartingAdvertise) {
            Log.w(TAG, "retry startBroadcast in 100ms!")
            scheduleStartBroadcast(data, 100)
            return
        }

        isStartingAdvertise = true

        val privateCredential = PrivateCredential.Builder(getSecretId()!!, getAuthenticityKey()!!,
            getMetadataEncryptionKey()!!, "presence-demo")
            .setIdentityType(PresenceCredential.IDENTITY_TYPE_PRIVATE)
            .build()

        val broadcastRequestBuilder = PresenceBroadcastRequest.Builder(
            Collections.singletonList(BLE_MEDIUM), getSalt()!!, privateCredential)
            .setVersion(BroadcastRequest.PRESENCE_VERSION_V1)
            .addAction(PRESENCE_ACTION)

        if (!data.isNullOrEmpty()){
            for (de in data){
                broadcastRequestBuilder.addExtendedProperty(de)
            }
        }

        applicationContext.getSystemService(NearbyManager::class.java)
            .startBroadcast(broadcastRequestBuilder.build(), Executors.newSingleThreadExecutor(),
                broadcastCallback)
    }

    private fun scheduleStopBroadcast(delay: Long = 0) {
        advertiseHandler.removeMessages(MSG_STOP_ADVERTISE)
        advertiseHandler.sendMessageDelayed(Message.obtain(advertiseHandler, MSG_STOP_ADVERTISE),
            delay)
    }

    @SuppressLint("MissingPermission")
    private fun stopBroadcast(){
        if (isStartingAdvertise) {
            Log.w(TAG, "retry stopBroadcast in 50ms!")
            scheduleStopBroadcast(50)
            return
        }

        applicationContext.getSystemService(NearbyManager::class.java).stopBroadcast(broadcastCallback)
    }

    private fun transfer(dcfDe: DCFDataElement): DataElement = DataElement(dcfDe.tag, dcfDe.data)

    private class DcfAdvertiseHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what) {
                MSG_START_ADVERTISE -> startBroadcast(msg.obj as List<DataElement>)
                MSG_STOP_ADVERTISE -> stopBroadcast()
            }
        }
    }
}