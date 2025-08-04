/*
 * Copyright (c) 2021-2022, 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.telephonyservice;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

/*
 * This is the factory class that creates a HIDL or AIDL QcRilAudio HAL instance
 */
public final class QcRilAudioHalFactory {

    private static final String TAG = "QcRilAudioHalFactory";

    public static final IAudioControllerCallback newQcRilAudioHal(Context context, int slotId,
            IAudioController cb) {
        if (!isRadioSupported()) {
            Log.i(TAG, "Initialize default HAL as target does not support ril");
            return new QcRilAudioNotSupportedHal();
        } else if (isBoardApiLevelMatched(context) && QcRilAudioAidl.isAidlAvailable(slotId)) {
            Log.i(TAG, "Initializing QcRilAudio AIDL");
            return new QcRilAudioAidl(slotId, cb);
        } else {
            Log.i(TAG, "Initializing QcRilAudio HIDL as AIDL is not available");
            return new QcRilAudioHidl(slotId, cb);
        }
    }

    public static final boolean isRadioSupported() {
        return !SystemProperties.getBoolean("ro.radio.noril", false);
    }

    // Helper function that checks the board.api_level to determine if AIDL is supported
    // QcRilAudioAidl is supported from Android T onwards
    private static boolean isBoardApiLevelMatched(Context context) {
        int apiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        int apiProdLevel = SystemProperties.getInt("ro.product.first_api_level", 0);
        Log.d(TAG, "isBoardApiLevelMatched: apiLevel = "
                + apiLevel +  " ProdLevel = " + apiProdLevel);
        if ((apiLevel > Build.VERSION_CODES.S_V2) ||
                ((apiLevel == 0) && (apiProdLevel > Build.VERSION_CODES.S_V2))) {
            Log.d(TAG, "isBoardApiLevelMatched: true...");
            return true;
        }
        return false;
    }
}
