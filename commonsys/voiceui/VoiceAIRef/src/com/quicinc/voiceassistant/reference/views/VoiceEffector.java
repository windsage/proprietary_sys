/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voiceassistant.reference.views;

import android.view.View;


public class VoiceEffector {
    private final static float MIN_THRESHOLD = 0.2f;
    public final static float EFFECTS_MAX = 14.5f;
    public final static float EFFECTS_MIN = 10.0f;

    private final View effectView;
    private final float min;
    private final float max;
    private final float scaleMax;
    private boolean updatable;

    public VoiceEffector(View view, float scaleMax) {
        this.effectView = view;
        this.min = EFFECTS_MIN;
        this.max = EFFECTS_MAX;
        this.scaleMax = scaleMax;
        this.updatable = false;
        view.setVisibility(View.INVISIBLE);
    }

    public void update(float value) {
        if (updatable) {
            float percentageRate = getPercentageRate(value);
            float scaleSize;
            if (MIN_THRESHOLD < percentageRate) {
                scaleSize = percentageRate * scaleMax;
            } else {
                scaleSize = MIN_THRESHOLD * scaleMax;
            }
            setScaleXY(scaleSize);
        }
    }

    private float getPercentageRate(float value) {
        if (min >= value) {
            return 0.0f;
        } else if (max <= value) {
            return 1.0f;
        } else {
            value -= min;
            return value / (max - min);
        }
    }

    public void stop() {
        updatable = false;
        setScaleXY(0);

        effectView.setVisibility(View.INVISIBLE);
    }

    public void start() {
        updatable = true;
        setScaleXY(0);

        effectView.setVisibility(View.VISIBLE);
    }

    private void setScaleXY(float scaleSize) {
        effectView.setScaleX(scaleSize);
        effectView.setScaleY(scaleSize);
    }

}
