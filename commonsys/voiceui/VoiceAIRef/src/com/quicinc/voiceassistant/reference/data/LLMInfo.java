/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.data;

import android.content.ComponentName;

public class LLMInfo {
    private final String mLLMName;

    private final ComponentName mActivityComponent;

    public LLMInfo(String LLMName, ComponentName activity) {
        mLLMName = LLMName;
        mActivityComponent = activity;
    }

    public String getLLMName() {
        return mLLMName;
    }

    public ComponentName getActivityComponent() {
        return mActivityComponent;
    }

}
