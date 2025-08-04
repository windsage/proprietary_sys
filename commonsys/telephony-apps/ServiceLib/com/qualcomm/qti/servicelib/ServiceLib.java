/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.servicelib;

import android.os.IBinder;

public class ServiceLib {
    private static native IBinder nativeWaitForService(String name);
    private static native boolean nativeIsDeclared(String name);

    static {
        System.loadLibrary("jni_aidl_service");
    }

    public IBinder waitForDeclaredService(String name) {
        return isDeclared(name) ? nativeWaitForService(name) : null;
    }

    public boolean isDeclared(String name) {
        return nativeIsDeclared(name);
    }
}
