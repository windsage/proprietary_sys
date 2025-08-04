/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteclient;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import android.telephony.TelephonyManager;

import com.qualcomm.qti.servicelib.ServiceLib;

/*
 * This is the factory class that creates a HIDL or AIDL UimRemoteClient HAL instance
 */
public class UimRemoteClientFactory {

    private static final String LOG_TAG = "UimRemoteClientFactory";
    private static int mPhoneCount;
    private static IUimRemoteClientInterface[] mUimRemoteClient;

    public static final IUimRemoteClientInterface[] makeUimRemoteClient(Context context) {
        mPhoneCount = getPhoneCount(context);
        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        boolean noRil = SystemProperties.getBoolean("ro.radio.noril", false);
        boolean isCellularSupported = !noRil &&
                (tm.isVoiceCapable() || tm.isSmsCapable() || tm.isDataCapable());
        if (!isCellularSupported) {
            Log.i(LOG_TAG, "RIL is not supported");
            return makeUimRemoteClientNotSupportedHal();
        } else if (isBoardApiLevelMatched() && isAidlAvailable()) {
            return makeUimRemoteClientAidl(context);
        } else {
            return makeUimRemoteClientHidl(context);
        }
    }

    private static IUimRemoteClientInterface[] makeUimRemoteClientNotSupportedHal() {
        mUimRemoteClient = new UimRemoteClientNotSupportedHal[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mUimRemoteClient[i] = new UimRemoteClientNotSupportedHal();
        }
        return mUimRemoteClient;
    }

    private static IUimRemoteClientInterface[] makeUimRemoteClientAidl(Context context) {
        mUimRemoteClient = new UimRemoteClientAidl[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mUimRemoteClient[i] = new UimRemoteClientAidl(i, context);
        }
        return mUimRemoteClient;
    }

    private static IUimRemoteClientInterface[] makeUimRemoteClientHidl(Context context) {
        mUimRemoteClient = new UimRemoteClientHidl[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mUimRemoteClient[i] = new UimRemoteClientHidl(i, context);
        }
        return mUimRemoteClient;
    }

    static boolean isAidlAvailable() {
        try {
            ServiceLib serviceLibObj = new ServiceLib();
            return serviceLibObj.isDeclared(
                    "vendor.qti.hardware.radio.uim_remote_client."
                    + "IUimRemoteServiceClient/uimRemoteClient0");
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
