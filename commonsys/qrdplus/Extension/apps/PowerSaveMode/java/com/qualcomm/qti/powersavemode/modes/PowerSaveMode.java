/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.powersavemode.modes;

public class PowerSaveMode {

    private final int mHintType;
    private final String mName;

    public PowerSaveMode(int hintType, String name) {
        mHintType = hintType;
        mName = name;
    }

    public int getHintType() {
        return mHintType;
    }

    public String getName() {
        return mName;
    }

    @Override
    public String toString() {
        return "PowerSaveMode[" + "hintType = " + mHintType + ", name = " + mName + "]";
    }
}
