/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.utility;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

public final class UIThreadExecutor {

    private UIThreadExecutor() {
    }

    public static <T, V> void success(IOperationCallback<T, V> callback, T param) {
        if (callback == null) return;
        execute(() -> callback.onSuccess(param));
    }

    public static <T, V> void success(WeakReference<IOperationCallback<T, V>> callbackRef,
                                      T param) {
        IOperationCallback<T, V> callback = callbackRef.get();
        if (callback == null) return;
        execute(() -> callback.onSuccess(param));
    }

    public static <T, V> void failed(IOperationCallback<T, V> callback, V param) {
        if (callback == null) return;
        execute(() -> callback.onFailure(param));
    }

    public static <T, V> void failed(WeakReference<IOperationCallback<T, V>> callbackRef,
                                     V param) {
        IOperationCallback<T, V> callback = callbackRef.get();
        if (callback == null) return;
        execute(() -> callback.onFailure(param));
    }

    public static void execute(Runnable runnable) {
        if (runnable == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            Handler handler = getHandler();
            handler.post(runnable);
        }
    }

    public static void execute(WeakReference<Runnable> runnableRef) {
        Runnable runnable = runnableRef.get();
        if (runnable == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            Handler handler = getHandler();
            handler.post(runnable);
        }
    }

    private static Handler getHandler() {
        return HandlerHolder.INSTANCE;
    }

    private static class HandlerHolder {
        private static final Handler INSTANCE = new Handler(Looper.getMainLooper());
    }
}
