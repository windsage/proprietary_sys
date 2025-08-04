/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.InputFilter
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceManager
import com.qualcomm.qti.dcf.client.screensharing.ScreenSharingController
import com.qualcomm.qti.dcf.client.screensharing.ScreenSharingProperty
import com.qualcomm.qti.dcf.nearbyclient.R

class DeviceDetailActivity: AppCompatActivity(), View.OnClickListener,
    PresenceMonitor.OnSingleDeviceChangedListener,
    ScreenSharingController.ScreenSharingDevicesListener, PresenceMonitor.BatteryListener,
    UserSettings.OnSettingsChangedListener, ScreenSharingController.WfdStatusListener{

    companion object {
        private const val TAG = "DeviceDetailActivity"

        private const val DEVICE_NAME_LENGTH_LIMITATION = 11
    }

    private lateinit var device: DCFPresenceDevice

    private lateinit var deviceNameTextView: TextView
    private lateinit var deviceTypeTextView: TextView
    private lateinit var deviceStatusTextView: TextView
    private lateinit var deviceIdTextView: TextView
    private lateinit var batteryStatusTextView: TextView
    private lateinit var batteryLevelTextView: TextView
    private lateinit var bleAddressTextView: TextView
    private lateinit var wifiAddressTextView: TextView
    private lateinit var screenShareStateImageView: ImageView
    private lateinit var screenShareTextView: TextView
    private lateinit var screenShareRoleTextView: TextView
    private lateinit var screenShareConnectTextView: TextView
    private lateinit var capabilityGroup: Group

    private val CONNECT = 0
    private val DISCONNECT = 1
    private var handler : Handler = Handler()

    override fun onSettingsChanged(sharedPreferences: SharedPreferences?, key: String?) {
        var hasChanged = false;
        when (key) {
            UserSettings.KEY_DEVICE_NAME -> {
                device.name = UserSettings.deviceName
                hasChanged = true
            }
            UserSettings.KEY_DEVICE_TYPE -> {
                device.type = UserSettings.deviceType
                hasChanged = true
            }
            UserSettings.KEY_DEVICE_STATUS -> {
                device.status = UserSettings.deviceStatus
                hasChanged = true;
            }
        }
        if (hasChanged) {
            updateUI(device)
        }
    }

    override fun onWfdStateChanged(state: Int) {
        if (device.isLocalDevice) {
            device.screenSharingProperty?.wfdState = state
        }
        updateScreenSharingStatus(device.screenSharingProperty)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)

        // Handle overlays by setting padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content))
        { view, windowInsets ->
            val systemBarInsets: Insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBarInsets.left,
                top = systemBarInsets.top,
                right = systemBarInsets.right,
                bottom = systemBarInsets.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }

        supportActionBar?.setTitle(R.string.title_device_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        deviceNameTextView = findViewById(R.id.device_name)
        deviceTypeTextView = findViewById(R.id.device_type)
        deviceStatusTextView = findViewById(R.id.device_status)
        deviceIdTextView = findViewById(R.id.device_id)
        batteryStatusTextView = findViewById(R.id.battery_status)
        batteryLevelTextView = findViewById(R.id.battery_level)
        bleAddressTextView = findViewById(R.id.ble_address)
        wifiAddressTextView = findViewById(R.id.wifi_address)
        screenShareStateImageView = findViewById(R.id.screen_share_state)
        screenShareTextView = findViewById(R.id.screen_share_text)
        screenShareRoleTextView = findViewById(R.id.screen_share_role)
        screenShareConnectTextView = findViewById(R.id.screen_share_connect)
        capabilityGroup = findViewById(R.id.category_capability_group)

        device = intent.extras?.getParcelable<DCFPresenceDevice>(EXTRA_KEY_DEVICE,
            DCFPresenceDevice::class.java)!!.also {
            updateUI(it)
            updateScreenSharingStatus(it.screenSharingProperty)
        }
    }

    override fun onResume() {
        super.onResume()
        ScreenSharingController.getInstance().registerWfdStatusListener(this)
        if (device.isLocalDevice){
            PresenceMonitor.addBatteryListener(this)
            UserSettings.addOnSettingsChangedListener(this)
        } else {
            PresenceMonitor.addOnSingleDeviceChangedListener(device.id, this)
            ScreenSharingController.getInstance().registerScreenSharingDevicesListener(this)
        }
    }

    override fun onPause() {
        super.onPause()
        ScreenSharingController.getInstance().unregisterWfdStatusListener(this)
        if (device.isLocalDevice){
            PresenceMonitor.removeBatteryListener(this)
            UserSettings.removeOnSettingsChangedListener(this)
        } else {
            PresenceMonitor.removeOnSingleDeviceChangedListener(device.id, this)
            ScreenSharingController.getInstance().unregisterScreenSharingDevicesListener(this)
        }
    }

    override fun onSingleDeviceChanged(device: DCFPresenceDevice) {
        device.screenSharingProperty = this.device.screenSharingProperty
        this.device = device
        handler.post {updateUI(device)}
    }

    override fun onDevicesPropertyChanged(devicesProperty: Map<String, ScreenSharingProperty>) {
        device.screenSharingProperty = if (devicesProperty.containsKey(device.id)) {
            devicesProperty[device.id]
        } else null
        handler.post {updateScreenSharingStatus(device.screenSharingProperty)}
    }

    override fun onBatteryStatusChanged(newStatus: Int, newLevel: Int) {
        updateBatteryStatus(newStatus, newLevel)
    }

    private fun updateUI(device: DCFPresenceDevice) {
        deviceNameTextView.text = device.name
        deviceTypeTextView.text = when(device.type){
            DCFPresenceDevice.DeviceType.PHONE -> getString(R.string.enum_device_type_phone)
            DCFPresenceDevice.DeviceType.TABLET -> getString(R.string.enum_device_type_tablet)
            DCFPresenceDevice.DeviceType.DISPLAY -> getString(R.string.enum_device_type_display)
            DCFPresenceDevice.DeviceType.LAPTOP -> getString(R.string.enum_device_type_laptop)
            DCFPresenceDevice.DeviceType.TV -> getString(R.string.enum_device_type_tv)
            DCFPresenceDevice.DeviceType.WATCH -> getString(R.string.enum_device_type_watch)
            else -> getString(R.string.enum_device_type_unknown)
        }
        deviceStatusTextView.text = when(device.status){
            DCFPresenceDevice.DeviceStatus.ACTIVE -> getString(R.string.enum_device_status_active)
            DCFPresenceDevice.DeviceStatus.IDLE -> getString(R.string.enum_device_status_idle)
            DCFPresenceDevice.DeviceStatus.DEVICE_LOCKED ->
                getString(R.string.enum_device_status_device_locked)
            DCFPresenceDevice.DeviceStatus.DEVICE_UNLOCKED ->
                getString(R.string.enum_device_status_device_unlocked)
            else -> getString(R.string.enum_device_status_unknown) }
        deviceIdTextView.text = device.id

        updateBatteryStatus(device.batteryStatus, device.batteryLevel)

        bleAddressTextView.text = device.bleAddress
        wifiAddressTextView.text = device.wifiAddress

        if (device.isLocalDevice){
            deviceNameTextView.apply {
                isClickable = true
                setCompoundDrawablesWithIntrinsicBounds(null, null,
                    getDrawable(R.drawable.ic_baseline_keyboard_arrow_right_24), null)
                setOnClickListener(this@DeviceDetailActivity)
            }
            deviceTypeTextView.apply {
                isClickable = true
                setCompoundDrawablesWithIntrinsicBounds(null, null,
                    getDrawable(R.drawable.ic_baseline_keyboard_arrow_right_24), null)
                setOnClickListener(this@DeviceDetailActivity)
            }
            deviceStatusTextView.apply {
                isClickable = true
                setCompoundDrawablesWithIntrinsicBounds(null, null,
                    getDrawable(R.drawable.ic_baseline_keyboard_arrow_right_24), null)
                setOnClickListener(this@DeviceDetailActivity)
            }
        }
    }

    private fun updateBatteryStatus(batteryStatus: Int, batteryLevel: Int) {
        batteryStatusTextView.text = when(batteryStatus){
            DCFPresenceDevice.BatteryChargingStatus.CHARGED ->
                getString(R.string.enum_battery_status_charged)
            DCFPresenceDevice.BatteryChargingStatus.CHARGING ->
                getString(R.string.enum_battery_status_charging)
            DCFPresenceDevice.BatteryChargingStatus.DISCHARGING ->
                getString(R.string.enum_battery_status_discharging)
            else -> getString(R.string.enum_battery_status_unknown)
        }
        batteryLevelTextView.text = batteryLevel.toString() + "%"
    }

    private fun updateScreenSharingStatus(screenSharingProperty: ScreenSharingProperty?) {
        capabilityGroup.visibility = screenSharingProperty?.let {
            screenShareStateImageView.setImageResource(when(it.wfdState) {
                ScreenSharingController.WfdState.IDLE ->
                    R.drawable.ic_baseline_cast_connected_blue_24
                ScreenSharingController.WfdState.BUSY ->
                    R.drawable.ic_baseline_cast_connected_purple_24
                else -> R.drawable.ic_baseline_cast_connected_gray_24
            })
            screenShareTextView.text = getString(R.string.device_detail_item_screen_share)
            if (it.wfdType == ScreenSharingController.WfdType.SINK) {
                screenShareRoleTextView.text = getString(R.string.enum_screen_share_sink)
            } else if (it.wfdType == ScreenSharingController.WfdType.SOURCE) {
                screenShareRoleTextView.text = getString(R.string.enum_screen_share_source)
            }

            screenShareConnectTextView.apply {
                isClickable = false
                tag = null
                text = null
            }
            if (ScreenSharingController.getInstance().isScreenSharingEnabled) {
                val localWfdType = ScreenSharingController.getInstance().getWfdType()
                val localWfdSate = ScreenSharingController.getInstance().getWfdState()

                if (localWfdType == ScreenSharingController.WfdType.SOURCE
                    && localWfdSate == ScreenSharingController.WfdState.IDLE
                    && it.wfdType == ScreenSharingController.WfdType.SINK
                    && it.wfdState == ScreenSharingController.WfdState.IDLE) {
                    screenShareConnectTextView.apply {
                        isClickable = true
                        tag = CONNECT
                        text = getString(R.string.state_screen_share_connect)
                        setOnClickListener(this@DeviceDetailActivity)
                    }
                }

                if (it.wfdState == ScreenSharingController.WfdState.BUSY
                    && localWfdSate == ScreenSharingController.WfdState.BUSY
                    && it.deviceAddress == ScreenSharingController.getInstance().
                    getConnectedDeviceAddress()) {
                    screenShareConnectTextView.apply {
                        isClickable = true
                        tag = DISCONNECT
                        text = getString(R.string.state_screen_share_disconnect)
                        setOnClickListener(this@DeviceDetailActivity)
                    }
                }
            }
            View.VISIBLE
        } ?: View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(view: View?) {
        var dialog: AlertDialog? = null
        when(view?.id){
            R.id.device_name -> {
                val newDeviceName = EditText(this).apply {
                    setText(device.name)
                    filters = arrayOf(InputFilter.LengthFilter(DEVICE_NAME_LENGTH_LIMITATION))
                }
                dialog = AlertDialog.Builder(this).apply {
                    setTitle(R.string.dialog_title_config_device_name)
                    setView(newDeviceName)
                    setPositiveButton(R.string.dialog_ok) { _, _ ->
                        UserSettings.deviceName = newDeviceName.text.toString()
                        deviceNameTextView.text = newDeviceName.text
                        Log.i(TAG, "new Device Name:${newDeviceName.text} was configured.")
                    }
                    setNegativeButton(R.string.dialog_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    setCancelable(true)
                }.create()
                Toast.makeText(this, R.string.toast_device_name_input_limitation,
                    Toast.LENGTH_LONG).show()
            }
            R.id.device_type -> {
                val deviceTypes = arrayOf(
                    getString(R.string.enum_device_type_phone),
                    getString(R.string.enum_device_type_tablet),
                    getString(R.string.enum_device_type_display),
                    getString(R.string.enum_device_type_laptop),
                    getString(R.string.enum_device_type_tv),
                    getString(R.string.enum_device_type_watch),
                    getString(R.string.enum_device_type_unknown))
                var newType = device.type
                dialog = AlertDialog.Builder(this).apply {
                    setTitle(R.string.dialog_title_config_device_type)
                    setSingleChoiceItems(deviceTypes, (newType - 1).let {
                        if (it < 0) deviceTypes.size - 1 else it
                    }) { _, which ->
                        newType = (which + 1) % deviceTypes.size
                        Log.i(TAG, "new Device Type:${deviceTypes[which]} was chosen.")
                    }
                    setPositiveButton(R.string.dialog_ok) { _, _ ->
                        if (newType != device.type){
                            deviceTypeTextView.text = deviceTypes[(newType - 1).let {
                                if (it < 0) deviceTypes.size - 1 else it }]
                            with(PreferenceManager.getDefaultSharedPreferences(
                                this@DeviceDetailActivity).edit()){
                                putInt(UserSettings.KEY_DEVICE_TYPE, newType)
                                apply()
                            }
                        }
                    }
                    setNegativeButton(R.string.dialog_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    setCancelable(true)
                }.create()
            }
            R.id.device_status -> {
                val deviceStatusList = arrayOf(
                    getString(R.string.enum_device_status_active),
                    getString(R.string.enum_device_status_idle),
                    getString(R.string.enum_device_status_device_locked),
                    getString(R.string.enum_device_status_device_unlocked))
                val deviceStatusValues = arrayOf(
                    DCFPresenceDevice.DeviceStatus.ACTIVE,
                    DCFPresenceDevice.DeviceStatus.IDLE,
                    DCFPresenceDevice.DeviceStatus.DEVICE_LOCKED,
                    DCFPresenceDevice.DeviceStatus.DEVICE_UNLOCKED,)
                var newStatus = device.status
                dialog = AlertDialog.Builder(this).apply {
                    setTitle(R.string.dialog_title_config_device_status)
                    setSingleChoiceItems(deviceStatusList,
                        deviceStatusValues.indexOf(newStatus)) {_, which ->
                        newStatus = which
                        Log.i(TAG, "new Device Status:${deviceStatusList[which]} was chosen.")
                    }
                    setPositiveButton(R.string.dialog_ok) { _, which ->
                        if (newStatus != device.status){
                            deviceStatusTextView.text = deviceStatusList[newStatus]
                            with(PreferenceManager.getDefaultSharedPreferences(
                                this@DeviceDetailActivity).edit()){
                                putInt(UserSettings.KEY_DEVICE_STATUS,
                                    deviceStatusValues[newStatus])
                                apply()
                            }
                        }
                    }
                    setNegativeButton(R.string.dialog_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    setCancelable(true)
                }.create()
            }
        }

        dialog?.show()

        when(view?.id) {
            R.id.screen_share_connect -> {
                if (view?.tag == CONNECT) {
                    dialog = AlertDialog.Builder(this).apply {
                        setMessage(getString(R.string.state_screen_share_connecting))
                        setCancelable(false)
                    }.create()
                    dialog.show()
                    screenShareConnectTextView.apply {
                        isClickable = false
                        text = getString(R.string.state_screen_share_connecting)
                    }
                    Log.i(TAG, "start connecting for screen sharing")
                    ScreenSharingController.getInstance().startScreenSharing(
                            device.screenSharingProperty?.deviceAddress,
                            object : ScreenSharingController.ScreenSharingActionListener {
                                override fun onSuccess() {
                                    Log.i(TAG, "onSuccess: establishConnection")
                                    updateScreenSharingStatus(device.screenSharingProperty)
                                    dialog?.dismiss()
                                }

                                override fun onFailure(reasonCode: Int) {
                                    Log.i(TAG, "onFailure: establishConnection")
                                    updateScreenSharingStatus(device.screenSharingProperty)
                                    dialog?.dismiss()
                                    Toast.makeText(this@DeviceDetailActivity, getString(
                                        R.string.toast_error_screen_sharing_connect_failed),
                                        Toast.LENGTH_LONG).show()
                                }
                            })
                } else if (view?.tag == DISCONNECT) {
                    dialog = AlertDialog.Builder(this).apply {
                        setMessage(getString(R.string.state_screen_share_disconnecting))
                        setCancelable(false)
                    }.create()
                    dialog.show()
                    screenShareConnectTextView.apply {
                        isClickable = false
                        text = getString(R.string.state_screen_share_disconnecting)
                    }
                    Log.i(TAG, "start disconnecting for screen sharing")
                    ScreenSharingController.getInstance().stopScreenSharing(object :
                            ScreenSharingController.ScreenSharingActionListener {
                        override fun onSuccess() {
                            Log.i(TAG, "onSuccess: teardownConnection")
                            updateScreenSharingStatus(device.screenSharingProperty)
                            dialog?.dismiss()
                        }

                        override fun onFailure(reasonCode: Int) {
                            Log.i(TAG, "onFailure: teardownConnection")
                            updateScreenSharingStatus(device.screenSharingProperty)
                            dialog?.dismiss()
                            Toast.makeText(this@DeviceDetailActivity, getString(
                                R.string.toast_error_screen_sharing_disconnect_failed),
                                Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
        }
    }
}
