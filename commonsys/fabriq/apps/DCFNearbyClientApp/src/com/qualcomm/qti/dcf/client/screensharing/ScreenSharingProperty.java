/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client.screensharing;

import android.os.Parcel;
import android.os.Parcelable;

public class ScreenSharingProperty implements Parcelable {

    private int mWfdState;
    private int mWfdType;
    private String mDeviceAddress;

    public ScreenSharingProperty(int wfdState, int wfdType) {
        this(wfdState, wfdType, "");
    }

    ScreenSharingProperty(int wfdState, int wfdType, String deviceAddress) {
        mWfdState = wfdState;
        mWfdType = wfdType;
        mDeviceAddress = deviceAddress;
    }

    protected ScreenSharingProperty(Parcel in) {
        mWfdState = in.readInt();
        mWfdType = in.readInt();
        mDeviceAddress = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mWfdState);
        dest.writeInt(mWfdType);
        dest.writeString(mDeviceAddress);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ScreenSharingProperty> CREATOR = new Creator<ScreenSharingProperty>() {
        @Override
        public ScreenSharingProperty createFromParcel(Parcel in) {
            return new ScreenSharingProperty(in);
        }

        @Override
        public ScreenSharingProperty[] newArray(int size) {
            return new ScreenSharingProperty[size];
        }
    };

    public void setWfdState(int state) {
        mWfdState = state;
    }

    public int getWfdState() {
        return mWfdState;
    }

    public int getWfdType() {
        return mWfdType;
    }

    public String getDeviceAddress() {
        return mDeviceAddress;
    }
}