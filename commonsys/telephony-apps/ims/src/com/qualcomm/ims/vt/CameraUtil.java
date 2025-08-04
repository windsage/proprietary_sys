/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.ims.vt;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics.Key;
import android.util.Log;
import android.util.Size;

/**
 * Provides utilities for camera
 */

public class CameraUtil {
    private static final String TAG = "VideoCall_CameraUtil";
    private static final boolean DBG = true;

    private CameraUtil() {
    }

    public static int getCameraFacing(Context context, String cameraId) {
        int cameraFacing = ImsMediaConstants.CAMERA_FACING_FRONT;
        try {
            cameraFacing = (getCameraCharacteristics(context, cameraId,
                        CameraCharacteristics.LENS_FACING)
                    == CameraCharacteristics.LENS_FACING_FRONT) ?
                ImsMediaConstants.CAMERA_FACING_FRONT : ImsMediaConstants.CAMERA_FACING_BACK;
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraFacing: Failed to retrieve getFacing, " + e);
        }
        return cameraFacing;
    }

    public static int getCameraMountAngle(Context context, String cameraId) {
        int mountAngle = 0;
        try {
            mountAngle = getCameraCharacteristics(context, cameraId,
                    CameraCharacteristics.SENSOR_ORIENTATION);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraFacing: Failed to retrieve getFacing, " + e);
        }
        return mountAngle;
    }

    private static <T> T getCameraCharacteristics(Context context, String cameraId, Key<T> key)
        throws CameraAccessException {
            if (cameraId == null) {
                Log.e(TAG, "getCameraCharacteristics: camera id is null");
                return null;
            }
            CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = cm.getCameraCharacteristics(cameraId);
            return characteristics.get(key);
    }

    public static Size calculateArPreviewSize(Context context, String cameraId,
           int width, int height) {
        if (width <= 0 || height <= 0 || cameraId == null) {
            return new Size(ImsMediaConstants.INVALID_WIDTH, ImsMediaConstants.INVALID_HEIGHT);
        }
        final int mountAngle = getCameraMountAngle(context, cameraId);
        Size previewSize = new Size(width, height);
        boolean isPortrait = width < height;
        if (!isPortrait && mountAngle % 180 != 0) {
            /* If negotiated dimensions are landscape and if camera mount angle
               is 90 or 270, then set portrait preview size */
            previewSize = new Size(height, width);
        } else if (isPortrait && mountAngle % 180 == 0) {
            /* This cases gets executed mostly for tablet devices wherein camera mount angle
               is either 0 or 180 in which case the requirement is to set portrait preview size */
            previewSize = new Size(width, height);
        } else if (isPortrait) {
            /* Configure preview always to landscape */
            previewSize = new Size(height, width);
        } else if (!isPortrait) {
            /* Configure preview always to landscape */
            previewSize = new Size(width, height);
        }
        Log.v(TAG, "calculateArPreviewSize mountAngle = " + mountAngle +
                " previewSize = " + previewSize);
        return previewSize;
    }
}
