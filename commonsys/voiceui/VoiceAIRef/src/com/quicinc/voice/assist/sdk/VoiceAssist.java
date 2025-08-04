/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk;

import android.content.Context;

public final class VoiceAssist {
    private static VoiceAssist mInstance;
    private final Context mContext;
    private String mSMRootPath;
    private VoiceAssist(Context context, String root) {
        mContext = context;
        mSMRootPath = root;
    }
    public static VoiceAssist getInstance() {
        return mInstance;
    }
    public static void initialize(Context applicationContext, String smRootPath) {
        if (mInstance == null) mInstance = new VoiceAssist(applicationContext, smRootPath);
    }
    public String getSMRootPath() {
        return mSMRootPath;
    }
}
