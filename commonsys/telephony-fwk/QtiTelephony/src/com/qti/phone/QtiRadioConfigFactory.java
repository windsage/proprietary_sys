/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.content.Context;
import android.os.Build;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

/*
 * This is the factory class that creates an AIDL IQtiRadioConfig HAL instance
 */
public final class QtiRadioConfigFactory {
    private static final String TAG = "QtiRadioConfigFactory";
    private static IQtiRadioConfigConnectionInterface mQtiRadioConfigAidl,
            mQtiRadioConfigNotSupportedHal;
    private static Context mContext;

    public static final IQtiRadioConfigConnectionInterface makeQtiRadioConfig(Context context) {
        mContext = context;
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        boolean noRil = SystemProperties.getBoolean("ro.radio.noril", false);
        boolean isCellularSupported = !noRil &&
                (tm.isVoiceCapable() || tm.isSmsCapable() || tm.isDataCapable());
        if (isAidlAvailable() && isCellularSupported) {
            return makeQtiRadioConfigAidl();
        } else {
            Log.d(TAG, "isCellularSupported=" + isCellularSupported);
            return makeQtiRadioConfigNotSupportedHal();
        }
    }

    private static int makeRadioVersion(int major, int minor) {
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private static boolean isAidlHalAvailable() {
        final int RADIO_HAL_VERSION_1_6 = makeRadioVersion(1, 6); // Radio HAL Version S

        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            Log.e(TAG, "TelephonyManger is NULL");
            return false;
        }
        Pair<Integer, Integer> radioVersion = telephonyManager.getRadioHalVersion();
        int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);

        Log.d(TAG, "isAidlHalAvailable: halVersion = " + halVersion);
        if (halVersion >= RADIO_HAL_VERSION_1_6) {
            return true;
        }
        return false;
    }

    // Helper function that checks the Android version. QtiRadioConfigAidl is supported from Android
    // T onwards
    private static boolean isAidlAvailable() {
        int osVersion = SystemProperties.getInt("ro.board.api_level", 0);
        Log.d(TAG, "isAidlAvailable: osVersion= " + osVersion);
        if ((osVersion >= Build.VERSION_CODES.S) ||
                ((osVersion == 0) && isAidlHalAvailable())) {
            try {
                return ServiceManager.isDeclared(QtiRadioConfigAidl.QTI_RADIO_CONFIG_SERVICE_NAME);
            } catch (SecurityException ex) {
                Log.e(TAG, "Security exception while calling into AIDL", ex);
            }
        }
        return false;
    }

    private static IQtiRadioConfigConnectionInterface makeQtiRadioConfigNotSupportedHal() {
        return new QtiRadioConfigNotSupportedHal();
    }

    private static IQtiRadioConfigConnectionInterface makeQtiRadioConfigAidl() {
        return new QtiRadioConfigAidl(mContext);
    }
}
