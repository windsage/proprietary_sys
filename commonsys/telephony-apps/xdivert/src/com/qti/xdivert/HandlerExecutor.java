/*
 * Copyright (c) 2020-2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.xdivert;

import android.os.Handler;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * An adapter {@link Executor} that posts all executed tasks onto the given
 * {@link Handler}.
 */
class HandlerExecutor implements Executor {

    private final Handler mHandler;

    public HandlerExecutor(Handler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        mHandler = handler;
    }

    @Override
    public void execute(Runnable command) {
        if (!mHandler.post(command)) {
            throw new RejectedExecutionException(mHandler + " is shutting down");
        }
    }
}
