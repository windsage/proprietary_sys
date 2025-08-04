/* Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.ims.utils;

import android.graphics.Point;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Range;

import java.util.ArrayList;
import java.util.List;

public class MediaCodecUtils {
    private static MediaCodecInfo getMediaCodecInfo(String mimeType) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        if (codecList == null) {
            return null;
        }
        for (MediaCodecInfo info : codecList.getCodecInfos()) {
            if (info != null &&!info.isEncoder()) {
                String[] types = info.getSupportedTypes();
                for (int j = 0; j < types.length; j++) {
                    if (types[j].equalsIgnoreCase(mimeType)) {
                        //When MediaCodecList builds the list, all codecs are sorted by rank,
                        //the first one is most preferred one and return this preferred one.
                        return info;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return AVC/HEVC decoder max resolution
     * @param mimeType
     */
    public static Point getVideoDecoderMaxSupportedDimension(String mimeType) {
        MediaCodecInfo info = getMediaCodecInfo(mimeType);
        if (info == null) {
            return null;
        }
        Range widthRange = info.getCapabilitiesForType(info.getSupportedTypes()[0])
                .getVideoCapabilities().getSupportedWidths();
        Range heightRange = info.getCapabilitiesForType(info.getSupportedTypes()[0])
                .getVideoCapabilities().getSupportedHeights();
        if (widthRange == null || heightRange == null) {
            return null;
        }
        return new Point((int)widthRange.getUpper(), (int)heightRange.getUpper());
    }
}
