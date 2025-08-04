/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteserver;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import android.telephony.TelephonyManager;

import com.qualcomm.qti.servicelib.ServiceLib;

/*
 * This is the factory class that creates a HIDL or AIDL UimRemoteServer HAL instance
 */
public class UimRemoteServerFactory {

    private static final String LOG_TAG = "UimRemoteServerFactory";
    private static int mPhoneCount;
    private static IUimRemoteServerInterface[] mUimRemoteServer;

    public static final IUimRemoteServerInterface[] makeUimRemoteServer(Context context) {
        mPhoneCount = getPhoneCount(context);
        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        boolean noRil = SystemProperties.getBoolean("ro.radio.noril", false);
        boolean isCellularSupported = !noRil &&
                (tm.isVoiceCapable() || tm.isSmsCapable() || tm.isDataCapable());
        if (!isCellularSupported) {
            Log.i(LOG_TAG, "RIL is not supported");
            return makeUimRemoteServerNotSupportedHal();
        } else if (isBoardApiLevelMatched() && isAidlAvailable()) {
            return makeUimRemoteServerAidl(context);
        } else {
            return makeUimRemoteServerHidl();
        }
    }

    private static IUimRemoteServerInterface[] makeUimRemoteServerNotSupportedHal() {
        mUimRemoteServer = new UimRemoteServerNotSupportedHal[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mUimRemoteServer[i] = new UimRemoteServerNotSupportedHal();
        }
        return mUimRemoteServer;
    }

    private static IUimRemoteServerInterface[] makeUimRemoteServerAidl(Context context) {
        mUimRemoteServer = new UimRemoteServerAidl[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mUimRemoteServer[i] = new UimRemoteServerAidl(i, context);
        }
        return mUimRemoteServer;
    }

    private static IUimRemoteServerInterface[] makeUimRemoteServerHidl() {
        mUimRemoteServer = new UimRemoteServerHidl[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mUimRemoteServer[i] = new UimRemoteServerHidl(i);
        }
        return mUimRemoteServer;
    }

    static boolean isAidlAvailable() {
        try {
            ServiceLib serviceLibObj = new ServiceLib();
            return serviceLibObj.isDeclared(
                    "vendor.qti.hardware.radio.uim_remote_server."
                    + "IUimRemoteServiceServer/uimRemoteServer0");
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Security exception while call into AIDL: "+ e);
        }
        return false;
    }

    private static boolean isBoardApiLevelMatched() {
        int apiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        int apiProdLevel = SystemProperties.getInt("ro.product.first_api_level", 0);
        Log.d(LOG_TAG, "isBoardApiLevelMatched: apiLevel= " + apiLevel +
                " ProdLevel= " + apiProdLevel);
        if (apiLevel >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                ((apiLevel == 0) && (apiProdLevel >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE))) {
            Log.d(LOG_TAG, "isBoardApiLevelMatched: true...");
            return true;
        }
        return false;
    }

    private static int getPhoneCount(Context context) {
        TelephonyManager tm = (TelephonyManager) context.
                getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getActiveModemCount();
    }

}
