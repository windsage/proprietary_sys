/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client

import android.os.Parcel
import android.os.Parcelable
import com.qualcomm.qti.dcf.client.screensharing.ScreenSharingProperty

data class DCFPresenceDevice(
    val id: String,
    var name: String,
    var type: Int,
    var status: Int,
    var batteryStatus: Int,
    var batteryLevel: Int,
    val bleAddress: String,
    val wifiAddress: String,
    var isLocalDevice: Boolean = false,
    var screenSharingProperty: ScreenSharingProperty? = null,
): Parcelable {

    @Target(AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class DeviceType {
        companion object {
            const val UNKNOWN = 0x00
            const val PHONE = 0x01
            const val TABLET = 0x02
            const val DISPLAY = 0x03
            const val LAPTOP = 0x04
            const val TV = 0x05
            const val WATCH = 0x06
        }
    }

    @Target(AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class DeviceStatus {
        companion object {
            const val IDLE = 0x0010
            const val ACTIVE = 0x0011
            const val DEVICE_LOCKED = 0x0020
            const val DEVICE_UNLOCKED = 0x0022 // Device Unlocked, Screen on
        }
    }

    @Target(AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class BatteryChargingStatus {
        companion object {
            const val CHARGED = 0x00
            const val CHARGING = 0x01
            const val DISCHARGING = 0x02
            const val UNKNOWN = 0x03
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readByte() != 0.toByte(),
        parcel.readParcelable(ScreenSharingProperty::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeInt(type)
        parcel.writeInt(status)
        parcel.writeInt(batteryStatus)
        parcel.writeInt(batteryLevel)
        parcel.writeString(bleAddress)
        parcel.writeString(wifiAddress)
        parcel.writeByte(if (isLocalDevice) 1 else 0)
        parcel.writeParcelable(screenSharingProperty, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DCFPresenceDevice> {
        override fun createFromParcel(parcel: Parcel): DCFPresenceDevice {
            return DCFPresenceDevice(parcel)
        }

        override fun newArray(size: Int): Array<DCFPresenceDevice?> {
            return arrayOfNulls(size)
        }
    }

}