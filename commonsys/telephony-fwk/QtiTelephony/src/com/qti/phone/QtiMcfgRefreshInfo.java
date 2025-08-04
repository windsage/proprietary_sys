/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qti.phone;

public class QtiMcfgRefreshInfo {
    private static final String TAG = "QtiMcfgRefreshInfo";

    public static final int MCFG_REFRESH_START = 0;
    public static final int MCFG_REFRESH_COMPLETE = 1;
    public static final int MCFG_CLIENT_REFRESH = 2;

    private int mSubId;
    private int mMcfgState;

    public QtiMcfgRefreshInfo(int subId, int state) {
        mSubId = subId;
        mMcfgState = state;
    }

    public int getSubId() {
        return mSubId;
    }

    public int getMcfgState() {
        return mMcfgState;
    }

    public String toString() {
        return "QtiMcfgRefreshInfo{" + " mSubId= " + mSubId + " mMcfgState= " + mMcfgState +  "}";
    }
}

