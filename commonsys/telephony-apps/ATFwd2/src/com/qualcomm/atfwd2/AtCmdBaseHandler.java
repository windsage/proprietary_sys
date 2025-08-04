/**
 * Copyright (c) 2010,2011,2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd2;

import android.content.Context;
import android.os.Handler;

public abstract class AtCmdBaseHandler extends Handler implements AtCmdHandler {

    protected Context mContext;

    public AtCmdBaseHandler(Context c) {
        mContext = c;
    }

    protected Context getContext() {
        return mContext;
    }
}
