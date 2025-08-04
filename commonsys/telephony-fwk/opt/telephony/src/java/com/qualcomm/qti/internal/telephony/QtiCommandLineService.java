/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.internal.telephony;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.util.Log;

import com.qualcomm.qti.internal.telephony.shell.QtiShellCommand;
import com.qualcomm.qti.internal.telephony.shell.QtiShellHandlerImpl;

import java.io.FileDescriptor;

final public class QtiCommandLineService extends Binder {
    private static final String LOG_TAG = "QtiCommandLineService";
    private static final String SERVICE_NAME = "extphone";

    private Context mContext;
    private QtiShellHandlerImpl mQtiShellHandlerImpl;

    private QtiCommandLineService(Context context) {
        mContext = context.getApplicationContext();
        mQtiShellHandlerImpl = new QtiShellHandlerImpl(mContext);
    }

    public static void advertise(Context context) {
        if (isEngOrUserDebugVersion()) {
            ServiceManager.addService(SERVICE_NAME, new QtiCommandLineService(context));
        } else {
            Log.w(LOG_TAG, "Only advertise it on engineering or user debug build");
        }
    }

    @Override
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err,
            String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new QtiShellCommand(mQtiShellHandlerImpl).exec(this, in, out, err, args, callback,
                resultReceiver);
    }

    private static boolean isEngOrUserDebugVersion() {
        return "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }
}
