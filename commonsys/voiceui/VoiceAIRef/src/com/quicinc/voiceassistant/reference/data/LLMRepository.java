/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.data;

import android.content.ComponentName;

import com.quicinc.voiceassistant.reference.ASRTTSActivity;

import java.util.ArrayList;
import java.util.List;

public final class LLMRepository {
    private static final List<LLMInfo> LLM_INFOS = new ArrayList<>();

    static {
        LLM_INFOS.add(new LLMInfo("VoiceAI Reference",
                new ComponentName("com.quicinc.voiceassistant.reference",
                        ASRTTSActivity.class.getName())));
    }

    public static ComponentName getCurrentLLMIntent() {
        return LLM_INFOS.get(0).getActivityComponent();
    }
}
