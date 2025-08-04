/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.qualcomm.qti.dcf.client.contextsync.ContextSyncController
import com.qualcomm.qti.dcf.client.screensharing.ScreenSharingController
import com.qualcomm.qti.dcf.nearbyclient.R

object UserSettings : SharedPreferences.OnSharedPreferenceChangeListener {

    const val KEY_DEVICE_NAME = "key_device_name"
    const val KEY_DEVICE_TYPE = "key_device_type"
    const val KEY_DEVICE_STATUS = "key_device_status"
    const val KEY_SCAN_ENABLE = "key_scan_enable"
    const val KEY_SCAN_MODE = "key_scan_mode"
    const val KEY_BROADCAST_ENABLE = "key_broadcast_enable"
    const val KEY_BATTERY_BROADCAST_INTERVAL = "key_battery_broadcast_interval"
    const val KEY_CONTEXT_SYNC_ENABLE = "key_context_sync_enable"
    const val KEY_SEND_NOTIFICATION_INTERVAL = "key_send_notification_interval"
    const val KEY_WFD_ENABLE = "key_wfd_enable"
    const val KEY_WFD_TYPE = "key_wfd_type"

    private val DEFAULT_DEVICE_NAME: String
        get() = applicationContext.getString(R.string.default_device_name_prefix) +
                PresenceMonitor.getBleAddress().let {
                    it.replace(":", "").let { newStr ->
                        newStr.substring(newStr.length - 4, newStr.length)
                    }
                }

    var deviceName: String
        get() = sharedPreferences.getString(KEY_DEVICE_NAME, DEFAULT_DEVICE_NAME)!!
        set(value) {
            with(sharedPreferences.edit()) {
                putString(KEY_DEVICE_NAME, value)
                apply()
            }
        }
    val deviceType: @DCFPresenceDevice.DeviceType Int
        get() = sharedPreferences.getInt(KEY_DEVICE_TYPE, DCFPresenceDevice.DeviceType.PHONE)
    val deviceStatus: @DCFPresenceDevice.DeviceStatus Int
        get() = sharedPreferences.getInt(KEY_DEVICE_STATUS, DCFPresenceDevice.DeviceStatus.ACTIVE)
    val scanEnable: Boolean
        get() = sharedPreferences.getBoolean(KEY_SCAN_ENABLE, true)
    val scanMode: Int
        get() = sharedPreferences.getString(KEY_SCAN_MODE,
            DCFScanManager.ScanMode.BALANCED.toString())!!.toInt()
    val broadcastEnable: Boolean
        get() = sharedPreferences.getBoolean(KEY_BROADCAST_ENABLE, true)
    val batteryBroadcastInterval: Int
        get() = sharedPreferences.getString(KEY_BATTERY_BROADCAST_INTERVAL, "10")!!.toInt()
    val contextSyncEnable: Boolean
        get() = sharedPreferences.getBoolean(KEY_CONTEXT_SYNC_ENABLE, false)
    val notificationInterval: Int
        get() = sharedPreferences.getString(KEY_SEND_NOTIFICATION_INTERVAL,
            ContextSyncController.DEFAULT_INTERVAL.toString())!!.toInt()
    val wfdEnable: Boolean
        get() = sharedPreferences.getBoolean(KEY_WFD_ENABLE, false)
    val wfdType: Int
        get() = sharedPreferences.getString(KEY_WFD_TYPE, ScreenSharingController
                .WfdType.SOURCE.toString())!!.toInt()

    private lateinit var applicationContext: Context
    private lateinit var sharedPreferences: SharedPreferences
    private val onSettingsChangedListeners: MutableList<OnSettingsChangedListener> = mutableListOf()
    private val onEnvironmentReadyListeners: MutableList<OnEnvironmentReadyListener>
        = mutableListOf()

    fun init(context: Context) {
        applicationContext = context
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context).apply {
            registerOnSharedPreferenceChangeListener(this@UserSettings)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (onSettingsChangedListeners.isNotEmpty()) {
            for (listener in onSettingsChangedListeners) {
                listener.onSettingsChanged(sharedPreferences, key)
            }
        }
    }

    fun notifyEnvironmentReady() {
        for (listener in onEnvironmentReadyListeners){
            listener.onEnvironmentReady()
        }
    }

    fun addOnSettingsChangedListener(onSettingsChangedListener: OnSettingsChangedListener) {
        if (onSettingsChangedListeners.contains(onSettingsChangedListener))
            return
        onSettingsChangedListeners.add(onSettingsChangedListener)
    }

    fun removeOnSettingsChangedListener(onSettingsChangedListener: OnSettingsChangedListener) {
        onSettingsChangedListeners.remove(onSettingsChangedListener)
    }

    fun addOnEnvironmentReadyListener(onEnvironmentReadyListener: OnEnvironmentReadyListener) {
        onEnvironmentReadyListeners.add(onEnvironmentReadyListener)
    }

    fun removeOnEnvironmentReadyListener(onEnvironmentReadyListener: OnEnvironmentReadyListener) {
        onEnvironmentReadyListeners.remove(onEnvironmentReadyListener)
    }

    interface OnSettingsChangedListener {
        fun onSettingsChanged(sharedPreferences: SharedPreferences?, key: String?)
    }

    interface OnEnvironmentReadyListener {
        fun onEnvironmentReady()
    }
}
