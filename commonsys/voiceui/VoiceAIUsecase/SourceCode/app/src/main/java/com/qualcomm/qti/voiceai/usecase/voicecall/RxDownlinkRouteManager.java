/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import android.content.Context;

public class RxDownlinkRouteManager extends LinkRouteManager{
    public RxDownlinkRouteManager(Context context) {
        super(context, Constants.RX);
    }
}
