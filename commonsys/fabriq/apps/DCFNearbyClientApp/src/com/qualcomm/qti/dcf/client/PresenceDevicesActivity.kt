/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qualcomm.qti.dcf.client.screensharing.ScreenSharingController
import com.qualcomm.qti.dcf.client.screensharing.ScreenSharingProperty
import com.qualcomm.qti.dcf.nearbyclient.R

const val EXTRA_KEY_DEVICE = "extra_key_device"

class PresenceDevicesActivity : AppCompatActivity(), PresenceMonitor.OnDevicesChangedListener,
    PresenceMonitor.BatteryListener, ScreenSharingController.ScreenSharingDevicesListener,
    ScreenSharingController.WfdStatusListener, PresenceMonitor.OnDeviceInfoAvailableListener,
    UserSettings.OnSettingsChangedListener {

    companion object {
        private const val TAG = "PresenceDevicesActivity"
        private const val REQUEST_CODE_PERMISSION = 1
        private const val REQUEST_CODE_BLUETOOTH = 2
        private const val REQUEST_CODE_SERVICE_PERMISSION = 3

        private const val LIST_INDEX_LOCAL_DEVICE = 0

        private const val PACKAGE_NAME_DCF_SERVICE = "com.qualcomm.qti.dcf.client.service"
        private const val RELATIVE_SERVICE_PERMISSIONS_ACTIVITY_CLASS_NAME =
            ".RequestPermissionsActivity"
        private const val EXTRA_KEY_DCF_SERVICE_REQUIRE_PERMISSIONS =
            "extra_key_require_permissions"
    }

    private lateinit var deviceList: RecyclerView
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var runtimePermissions: Array<String>
    private var presenceDevices: MutableList<DCFPresenceDevice> = mutableListOf()

    override fun onDevicesChanged(devices: List<DCFPresenceDevice>) {
        Log.i(TAG, "ui devices list update: ${devices.size}")
        runOnUiThread {
            val nearbyDevicesIndex = LIST_INDEX_LOCAL_DEVICE + 1
            for (index in (presenceDevices.size - 1) downTo nearbyDevicesIndex) {
                presenceDevices.removeAt(index)
            }
            devicesAdapter.submitList(presenceDevices.apply {
                if (!devices.isNullOrEmpty())
                // the first item is local device item.
                    addAll(nearbyDevicesIndex, devices)
            })
            devicesAdapter.notifyDataSetChanged()
        }
    }

    override fun onBatteryStatusChanged(newStatus: Int, newLevel: Int) {
        updateLocalDevice(batteryLevel = newLevel, batteryStatus = newStatus)
    }

    override fun onDevicesPropertyChanged(devicesProperty: Map<String, ScreenSharingProperty>) {
        runOnUiThread {
            for (device in presenceDevices) {
                if (device.isLocalDevice) continue
                device.screenSharingProperty = if (devicesProperty.containsKey(device.id)) {
                    devicesProperty[device.id]
                } else null
            }
            devicesAdapter.submitList(presenceDevices)
            devicesAdapter.notifyDataSetChanged()
        }
    }

    override fun onWfdStateChanged(state: Int) {
        updateLocalDevice()
    }

    override fun onDeviceInfoAvailable() {
        devicesAdapter.submitList(presenceDevices.apply {
            removeAt(LIST_INDEX_LOCAL_DEVICE)
            add(LIST_INDEX_LOCAL_DEVICE, PresenceMonitor.createLocalDevice())
        })
        devicesAdapter.notifyItemChanged(LIST_INDEX_LOCAL_DEVICE)
    }

    override fun onSettingsChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            UserSettings.KEY_DEVICE_NAME -> updateLocalDevice(name = UserSettings.deviceName)
            UserSettings.KEY_DEVICE_TYPE -> updateLocalDevice(type = UserSettings.deviceType)
            UserSettings.KEY_DEVICE_STATUS -> updateLocalDevice(status = UserSettings.deviceStatus)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presence_devices)

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

        supportActionBar?.setTitle(R.string.title_main)
        devicesAdapter = DevicesAdapter { device ->
            startActivity(Intent(this, DeviceDetailActivity()::class.java).apply {
                putExtra(EXTRA_KEY_DEVICE, device)
            })
        }
        deviceList = findViewById<RecyclerView?>(R.id.device_list).apply {
            layoutManager = LinearLayoutManager(this@PresenceDevicesActivity)
            adapter = devicesAdapter.apply {
                submitList(presenceDevices.apply {
                    add(0, PresenceMonitor.createLocalDevice())
                })
            }
        }

        runtimePermissions = packageManager
            .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions!!
            .filter { permission ->
                packageManager.getPermissionInfo(permission, PackageManager.GET_META_DATA)
                    .protection == PermissionInfo.PROTECTION_DANGEROUS
            }.toTypedArray()

        checkEnvironmentReady(ready = ::checkDcfServicePermissions) {
            Toast.makeText(this@PresenceDevicesActivity,
                getString(R.string.toast_warning_permissions_deny), Toast.LENGTH_LONG).show()
            requestPermissions(runtimePermissions, REQUEST_CODE_PERMISSION)
        }

        PresenceMonitor.deviceInfoListener = this
    }

    override fun onResume() {
        super.onResume()
        PresenceMonitor.addBatteryListener(this)
        updateLocalDevice(UserSettings.deviceName,
            UserSettings.deviceType,
            UserSettings.deviceStatus)
        UserSettings.addOnSettingsChangedListener(this)
        if (checkPermissionsGranted()) {
            PresenceMonitor.addOnDevicesChangedListener(this)
            ScreenSharingController.getInstance().registerScreenSharingDevicesListener(this)
            ScreenSharingController.getInstance().registerWfdStatusListener(this)
        }
    }

    override fun onPause() {
        super.onPause()
        PresenceMonitor.removeBatteryListener(this)
        UserSettings.removeOnSettingsChangedListener(this)
        if (checkPermissionsGranted()){
            PresenceMonitor.removeOnDevicesChangedListener(this)
            ScreenSharingController.getInstance().unregisterScreenSharingDevicesListener(this)
            ScreenSharingController.getInstance().unregisterWfdStatusListener(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            checkEnvironmentReady(ready = ::checkDcfServicePermissions) {
                if (shouldJumpToPermissionSettings(permissions, grantResults)) {
                    startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply {
                            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                            data = Uri.parse("package:$packageName")
                        }, REQUEST_CODE_PERMISSION)
                } else {
                    Toast.makeText(this@PresenceDevicesActivity,
                        getString(R.string.toast_error_permissions_deny), Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_CODE_BLUETOOTH -> {
                if (resultCode == RESULT_OK){
                    checkDcfServicePermissions()
                } else {
                    Toast.makeText(this@PresenceDevicesActivity,
                        getString(R.string.toast_error_bluetooth_off), Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            REQUEST_CODE_PERMISSION -> {
                checkEnvironmentReady(ready = ::checkDcfServicePermissions) {
                    Toast.makeText(this@PresenceDevicesActivity,
                        getString(R.string.toast_error_permissions_deny), Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            REQUEST_CODE_SERVICE_PERMISSION -> {
                if (resultCode == RESULT_OK) {
                    handleEnvironmentReady()
                } else {
                    Toast.makeText(this@PresenceDevicesActivity,
                        getString(R.string.toast_error_service_permissions_deny), Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, MoreSettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkEnvironmentReady(ready: () -> Unit,
                                      permissionsDeny: (() -> Unit)? = null): Boolean {
        if (!checkPermissionsGranted(permissionsDeny)) {
            return false
        }

        // if permissions already granted, continue to check Bluetooth status
        if (checkBluetoothOn(ready)){
            return true
        }

        return false
    }

    private fun checkPermissionsGranted(permissionsDeny: (() -> Unit)? = null): Boolean {
        for (permission in runtimePermissions){
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED){
                permissionsDeny?.invoke()
                return false
            }
        }

        return true
    }

    @SuppressLint("MissingPermission")
    private fun checkBluetoothOn(ready: () -> Unit): Boolean =
        if (isBluetoothOn()) {
            ready()
            true
        } else {
            Toast.makeText(this@PresenceDevicesActivity,
                getString(R.string.toast_warning_bluetooth_off), Toast.LENGTH_LONG).show()
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                REQUEST_CODE_BLUETOOTH)
            false
        }

    private fun isBluetoothOn(): Boolean {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager.adapter == null) {
            return false
        }

        return bluetoothManager.adapter.isEnabled
    }

    private fun checkDcfServicePermissions(){
        val remoteRequirePermissions = packageManager
            .getPackageInfo(PACKAGE_NAME_DCF_SERVICE, PackageManager.GET_PERMISSIONS)
            .requestedPermissions!!
            .filter { permission ->
                val permissionInfo = packageManager
                    .getPermissionInfo(permission, PackageManager.GET_META_DATA)
                isServiceRequire(permissionInfo)
            }

        if (remoteRequirePermissions.isEmpty()){
            handleEnvironmentReady()
        } else {
            startActivityForResult(Intent().apply {
                component = ComponentName.createRelative(PACKAGE_NAME_DCF_SERVICE,
                    RELATIVE_SERVICE_PERMISSIONS_ACTIVITY_CLASS_NAME)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_KEY_DCF_SERVICE_REQUIRE_PERMISSIONS,
                    remoteRequirePermissions.toTypedArray())
            }, REQUEST_CODE_SERVICE_PERMISSION)
        }
    }

    private fun isServiceRequire(permission: PermissionInfo): Boolean =
        permission.protection == PermissionInfo.PROTECTION_DANGEROUS &&
                packageManager.checkPermission(permission.name, PACKAGE_NAME_DCF_SERVICE) ==
                PackageManager.PERMISSION_DENIED

    private fun shouldJumpToPermissionSettings(permissions: Array<out String>,
                                              grantResults: IntArray): Boolean {
        val permissionArray: Array<String> = Array(permissions.size) { "" }
        permissions.copyInto(permissionArray)
        for ((index, grantResult) in grantResults.withIndex()){
            if (grantResult == PackageManager.PERMISSION_DENIED &&
                !shouldShowRequestPermissionRationale(permissionArray[index])) {
                return true
            }
        }
        return false
    }

    private fun handleEnvironmentReady() {
        PresenceMonitor.init(this)
        UserSettings.notifyEnvironmentReady()
    }

    private fun updateLocalDevice(name: String? = null,
                                  type: Int = -1,
                                  status: Int = -1,
                                  batteryStatus: Int = -1,
                                  batteryLevel: Int = -1) {
        if (presenceDevices.isEmpty()) return

        val localDevice = presenceDevices[LIST_INDEX_LOCAL_DEVICE]

        if (name != null && !TextUtils.equals(localDevice.name, name)) {
            localDevice.name = name
        }

        if (type != -1 && localDevice.type != type) {
            localDevice.type = type
        }

        if (status != -1 && localDevice.status != status) {
            localDevice.status = status
        }

        if (batteryStatus != -1 && localDevice.batteryStatus != batteryStatus) {
            localDevice.batteryStatus = batteryStatus
        }

        if (batteryLevel != -1 && localDevice.batteryLevel != batteryLevel) {
            localDevice.batteryLevel = batteryLevel
        }

        localDevice.screenSharingProperty =
                if (ScreenSharingController.getInstance().isScreenSharingEnabled)
                    ScreenSharingProperty(ScreenSharingController.getInstance().wfdState,
                    ScreenSharingController.getInstance().wfdType)
                else null

        devicesAdapter.submitList(presenceDevices)
        devicesAdapter.notifyItemChanged(LIST_INDEX_LOCAL_DEVICE)
    }
}