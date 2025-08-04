/*
 * Copyright (c) 2020 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.wrapper;

import android.os.SystemProperties;

public final class QSystemProperties {

    public static String get(final String key) {
        return SystemProperties.get(key);
    }

    private QSystemProperties() {

    }
}
