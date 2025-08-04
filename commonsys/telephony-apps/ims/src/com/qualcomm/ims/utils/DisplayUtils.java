/* Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.ims.utils;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.WindowManager;
import android.view.WindowMetrics;

public class DisplayUtils {
    /**
     * Return the window size including all system bar areas.
     * @param context
     */
    public static Point getDeviceScreenSize(Context context) {
        if (context == null) {
            return null;
        }
        WindowManager windowManager = context.getSystemService(WindowManager.class);
        if (windowManager == null) {
            return null;
        }
        final WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
        if (metrics == null) {
            return null;
        }
        // Legacy size that Display#getSize reports
        final Rect bounds = metrics.getBounds();
        final Point legacySize = new Point(bounds.width(), bounds.height());
        return legacySize;
    }
}
