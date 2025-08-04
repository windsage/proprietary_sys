/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.qualcomm.qti.dcf.client.screensharing.ScreenSharingController
import com.qualcomm.qti.dcf.nearbyclient.R

class DevicesAdapter(private val onClick: (DCFPresenceDevice) -> Unit):
    ListAdapter<DCFPresenceDevice, DevicesAdapter.DeviceHolder>(DeviceDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceHolder =
        DeviceHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_presence_device,
            parent, false), onClick)

    override fun onBindViewHolder(holder: DeviceHolder, position: Int)
        = holder.bind(getItem(position))

    class DeviceHolder(itemView: View, val onClick: (DCFPresenceDevice) -> Unit)
        : RecyclerView.ViewHolder(itemView){

        private val deviceIcon: ImageView = itemView.findViewById(R.id.device_icon)
        private val deviceName: TextView = itemView.findViewById(R.id.device_name)
        private val deviceStatus: TextView = itemView.findViewById(R.id.device_status)
        private val screenShareState: ImageView = itemView.findViewById(R.id.screen_share_state)
        private var currentDevice: DCFPresenceDevice? = null

        init {
            itemView.setOnClickListener {
                currentDevice?.let { onClick(it) }
            }
        }

        fun bind(device: DCFPresenceDevice) {
            currentDevice = device

            deviceIcon.setImageResource(
                when(currentDevice!!.type){
                    DCFPresenceDevice.DeviceType.PHONE -> R.drawable.ic_baseline_phone_android_24
                    DCFPresenceDevice.DeviceType.LAPTOP -> R.drawable.ic_baseline_laptop_24
                    DCFPresenceDevice.DeviceType.WATCH -> R.drawable.ic_baseline_watch_24
                    else -> R.drawable.ic_baseline_device_unknown_24
                })
            deviceName.text =
                if (device.isLocalDevice) {
                    val localDeviceSpannable = SpannableString(itemView.context.getString(
                        R.string.local_device_prefix, currentDevice!!.name))
                    localDeviceSpannable.setSpan(StyleSpan(Typeface.BOLD), 0, 12,
                        Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                    localDeviceSpannable.setSpan(StyleSpan(Typeface.BOLD),
                        localDeviceSpannable.length - 1, localDeviceSpannable.length,
                        Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                    localDeviceSpannable
                } else {
                    currentDevice!!.name
                }
            deviceStatus.text = itemView.context.getString(R.string.device_status_and_battery,
                when(currentDevice!!.status){
                    DCFPresenceDevice.DeviceStatus.ACTIVE ->
                        itemView.context.getString(R.string.enum_device_status_active)
                    DCFPresenceDevice.DeviceStatus.IDLE ->
                        itemView.context.getString(R.string.enum_device_status_idle)
                    DCFPresenceDevice.DeviceStatus.DEVICE_LOCKED ->
                        itemView.context.getString(R.string.enum_device_status_device_locked)
                    DCFPresenceDevice.DeviceStatus.DEVICE_UNLOCKED ->
                        itemView.context.getString(R.string.enum_device_status_device_unlocked)
                    else -> itemView.context.getString(R.string.enum_device_status_unknown) },
                currentDevice!!.batteryLevel)

            screenShareState.visibility = currentDevice!!.screenSharingProperty?.let {
                screenShareState.setImageResource(when(it.wfdState) {
                    ScreenSharingController.WfdState.IDLE ->
                        R.drawable.ic_baseline_cast_connected_blue_24
                    ScreenSharingController.WfdState.BUSY ->
                        R.drawable.ic_baseline_cast_connected_purple_24
                    else -> R.drawable.ic_baseline_cast_connected_gray_24
                })
                View.VISIBLE
            } ?: View.GONE
        }

    }
}

object DeviceDiffCallback : DiffUtil.ItemCallback<DCFPresenceDevice>() {
    override fun areItemsTheSame(oldItem: DCFPresenceDevice, newItem: DCFPresenceDevice): Boolean{
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: DCFPresenceDevice, newItem: DCFPresenceDevice): Boolean{
        return oldItem.id == newItem.id
    }
}