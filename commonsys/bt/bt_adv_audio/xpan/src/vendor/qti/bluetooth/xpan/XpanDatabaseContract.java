/*****************************************************************
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import android.net.Uri;
import android.util.Log;

public final class XpanDatabaseContract {
    private static String TAG = "XpanDatabaseContract";

    static final String TABLE_DEVICE_DATA = "devicedata";
    static final Uri CONTENT_URI_DEVICE_DATA =
            Uri.parse("content://vendor.qti.bluetooth.xpan.provider/" + TABLE_DEVICE_DATA);

    static final class DeviceData {
        interface Columns {
            String ADDRESS = "address";
            String BEARER = "bearer";
            String WHC_SUPPORTED = "whc_supported";
        }

        static final String BEARER_LEA = "LEA";
        static final String BEARER_P2P = "P2P";
        static final String BEARER_WHC = "WHC";

        static String bearer2Str(int bearer) {
            switch (bearer) {
            case XpanConstants.BEARER_AP:
                return BEARER_WHC;
            case XpanConstants.BEARER_P2P:
                return BEARER_P2P;
            case XpanConstants.BEARER_LE:
                return BEARER_LEA;
            default:
                return null;
            }
        }

        static final int WHC_SUPPORTED_NO = 0;
        static final int WHC_SUPPORTED_YES = 1;
    }
}

