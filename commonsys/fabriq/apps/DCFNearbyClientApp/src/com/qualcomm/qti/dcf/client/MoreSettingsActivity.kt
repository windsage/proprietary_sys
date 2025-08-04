/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.qualcomm.qti.dcf.nearbyclient.R

class MoreSettingsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_settings)

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

        supportActionBar?.setTitle(R.string.title_more_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.preference_container, MoreSettingsFragment())
            .commit()
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

    class MoreSettingsFragment: PreferenceFragmentCompat() {

        private val bluetoothReceiver = BluetoothReceiver()

        private var scanEnablePreference: SwitchPreference? = null
        private var broadcastEnablePreference: SwitchPreference? = null
        private var screenSharingEnablePreference: SwitchPreference? = null
        private var contextSyncEnablePreference: SwitchPreference? = null

        override fun onResume() {
            super.onResume()
            requireContext().registerReceiver(bluetoothReceiver, IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            })
        }

        override fun onPause() {
            super.onPause()
            requireContext().unregisterReceiver(bluetoothReceiver)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.more_settings, rootKey)

            val isBluetoothOn = isBluetoothOn()
            scanEnablePreference =
                findPreference<SwitchPreference?>(UserSettings.KEY_SCAN_ENABLE)?.apply {
                    isEnabled = isBluetoothOn
                }
            broadcastEnablePreference =
                findPreference<SwitchPreference?>(UserSettings.KEY_BROADCAST_ENABLE)?.apply {
                    isEnabled = isBluetoothOn
                }
            screenSharingEnablePreference =
                findPreference<SwitchPreference?>(UserSettings.KEY_WFD_ENABLE)?.apply {
                    isEnabled = isBluetoothOn
                }
            contextSyncEnablePreference =
                findPreference<SwitchPreference?>(UserSettings.KEY_CONTEXT_SYNC_ENABLE)?.apply {
                    isEnabled = isBluetoothOn
                }
        }

        private fun isBluetoothOn(): Boolean {
            val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE)
                    as BluetoothManager
            if (bluetoothManager.adapter == null) {
                return false
            }

            return bluetoothManager.adapter.isEnabled
        }

        inner class BluetoothReceiver: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val bluetoothState = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.STATE_ON)

                if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                    scanEnablePreference?.isEnabled = false
                    broadcastEnablePreference?.isEnabled = false
                    screenSharingEnablePreference?.isEnabled = false
                    contextSyncEnablePreference?.isEnabled = false
                } else if (bluetoothState == BluetoothAdapter.STATE_ON) {
                    scanEnablePreference?.isEnabled = true
                    broadcastEnablePreference?.isEnabled = true
                    screenSharingEnablePreference?.isEnabled = true
                    contextSyncEnablePreference?.isEnabled = true
                }
            }
        }
    }
}