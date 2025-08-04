/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.content.Context;
import android.os.Build;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Pair;
import androidx.annotation.VisibleForTesting;

import com.qualcomm.ims.utils.Log;

/*
 * This is the factory class that creates a HIDL or AIDL IImsRadio HAL instance
 * or a default Notsupported HAL if the target does not support telephony/ril
 */

public final class ImsRadioHalFactory {

    private static final String TAG = "ImsRadioHalFactory";

    private static int makeRadioVersion(int major, int minor){
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private static boolean isAidlHalAvailable(Context context){
        final int RADIO_HAL_VERSION_1_6 = makeRadioVersion(1, 6); // Radio HAL Version S

        if (context == null) {
            Log.e(TAG, "Context is NULL");
            return false;
        }
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
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

    public static final IImsRadio newImsRadioHal(IImsRadioResponse respCallback,
            IImsRadioIndication indCallback, int phoneId, Context context) {
        int apiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        if (SystemProperties.getBoolean("ro.radio.noril", false)) {
            Log.i(TAG, "Initialize default HAL as target does not support ril");
            return new ImsRadioNotSupportedHal();
        } else if (((apiLevel >= Build.VERSION_CODES.S) ||
                ((apiLevel == 0) && isAidlHalAvailable(context)))
                && ImsRadioAidl.isAidlAvailable(phoneId)) {
            Log.i(TAG, "Initializing IImsRadio AIDL");
            return new ImsRadioAidl(respCallback, indCallback, phoneId, context);
        } else {
            Log.i(TAG, "Initializing IImsRadio HIDL as AIDL is not available");
            return new ImsRadioHidl(respCallback, indCallback, phoneId);
        }
    }
}
