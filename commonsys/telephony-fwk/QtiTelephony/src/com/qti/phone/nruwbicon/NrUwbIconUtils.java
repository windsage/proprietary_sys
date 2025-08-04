/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.nruwbicon;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class provides utility methods for NR UWB icon parameters
 */
public class NrUwbIconUtils {
    private static final String LOG_TAG = "NrUwbIconUtils";

    private static final List<Integer> sValidSib2Values = List.of(0, 1, 2);

    // Modes
    private static final int MODE_CONNECTED = 1;
    private static final int MODE_IDLE = 2;
    private static final int MODE_CONNECTED_AND_IDLE = 3;

    // Timer types
    private static final String TIMER_SCG_TO_MCG = "scg_to_mcg";
    private static final String TIMER_IDLE_TO_CONNECT = "idle_to_connect";
    private static final String TIMER_IDLE = "idle";
    private static Map<String, Integer> sTimerMap = Map.of(
        TIMER_SCG_TO_MCG, 0,
        TIMER_IDLE_TO_CONNECT, 1,
        TIMER_IDLE, 2
    );

    private static final int REFRESH_TIME_MAX = 255;

    public static boolean isSib2ValueValid(int value) {
        if (!sValidSib2Values.contains(value)) {
            Log.e(LOG_TAG, "Invalid SIB2 value");
            return false;
        }
        return true;
    }

    public static boolean isRefreshTimerTypeValid(String type) {
        if (type.equals(TIMER_SCG_TO_MCG) || type.equals(TIMER_IDLE_TO_CONNECT) ||
                type.equals(TIMER_IDLE)) {
            return true;
        }
        Log.e(LOG_TAG, "Invalid refresh timer type = " + type);
        return false;
    }

    public static int getRefreshTimerTypeFromString(String type) {
        return sTimerMap.get(type);
    }

    public static boolean isRefreshTimerValueValid(String value) {
        int numericTimerValue = 0;
        try {
            numericTimerValue = Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            Log.e(LOG_TAG, "Invalid timer value", ex);
            return false;
        }
        if (numericTimerValue < 0) {
            Log.e(LOG_TAG, "Negative timer value");
            return false;
        }
        return true;
    }

    public static boolean isModeValid(int mode) {
        if ((mode == MODE_CONNECTED) || (mode == MODE_IDLE) || (mode == MODE_CONNECTED_AND_IDLE)) {
            return true;
        }
        return false;
    }

    public static boolean isMinBandwidthValid(int bwMode, int bwValue) {
        if (!isModeValid(bwMode) || bwValue <= 0 || bwValue == Integer.MAX_VALUE) {
            Log.e(LOG_TAG, "Invalid or unprovided min bw config - mode = " + bwMode + ", value = " +
                    bwValue);
            return false;
        }
        return true;
    }

    public static int[] extractValidBands(int[] bandsArray) {
        ArrayList<Integer> filteredBandsArray = new ArrayList<>();
        if (bandsArray == null) {
            Log.e(LOG_TAG, "Received null bandsArray");
            return new int[0];
        }

        // Exclude negative values
        for (int band : bandsArray) {
            if (band > 0) {
                filteredBandsArray.add(band);
            } else {
                Log.e(LOG_TAG, "Invalid band = " + band);
            }
        }
        int[] validBandsArray = new int[filteredBandsArray.size()];
        for (int i = 0; i < filteredBandsArray.size(); i++) {
            validBandsArray[i] = filteredBandsArray.get(i);
        }

        if (filteredBandsArray.isEmpty()) {
            Log.d(LOG_TAG, "No valid bands found");
        }

        return validBandsArray;
    }

    public static int convertRefreshTime(String timeValue) {
        int retValue = 0;
        try {
            retValue = Integer.valueOf(timeValue);
        } catch (NumberFormatException nfe) {
            Log.e(LOG_TAG, "Invalid refresh time = " + timeValue);
        }
        if (retValue > REFRESH_TIME_MAX) {
            retValue = REFRESH_TIME_MAX;
        }
        return retValue;
    }
}
