/*
 * Copyright (c) 2015-2017 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.util.Log;

import com.qualcomm.qti.internal.nrNetworkService.MainServiceImpl;

import org.codeaurora.internal.IExtTelephony;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * This class implements IExtTelephony aidl interface.
 */
public class ExtTelephonyServiceImpl extends IExtTelephony.Stub {
    private static final String LOG_TAG = "ExtTelephonyServiceImpl";
    private static final boolean DBG = true;

    private static final String TELEPHONY_SERVICE_NAME = "qti.radio.extphone";
    private static Context mContext;

    private static ExtTelephonyServiceImpl sInstance = null;

    private QtiRilInterface mQtiRilInterface;
    private QtiUiccEfHelper mQtiUiccEfHelper;

    public static ExtTelephonyServiceImpl init(Context context) {
        synchronized (ExtTelephonyServiceImpl.class) {
            mContext = context;
            if (sInstance == null) {
                sInstance = new ExtTelephonyServiceImpl();
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    public static ExtTelephonyServiceImpl getInstance() {
        if (sInstance == null) {
            Log.wtf(LOG_TAG, "getInstance null");
        }
        return sInstance;
    }

    private ExtTelephonyServiceImpl() {
        if (DBG) logd("init constructor, " + this);

        if (ServiceManager.getService(TELEPHONY_SERVICE_NAME) == null) {
            logd("ExtTelephonyServiceImpl: Adding IExtTelephony to ServiceManager as "
                 + TELEPHONY_SERVICE_NAME);
            ServiceManager.addService(TELEPHONY_SERVICE_NAME, this);
        }

        mQtiRilInterface = QtiRilInterface.getInstance(mContext);
        mQtiUiccEfHelper = new QtiUiccEfHelper(mContext);
    }

    private void enforceReadPrivilegedPermission(String message) {
        Log.d(LOG_TAG, "enforceReadPrivilegedPermission for " + message);
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE, message);
    }

    @Override
    public int getPhoneIdForECall() {
        enforceReadPrivilegedPermission("getPhoneIdForECall");
        return QtiEmergencyCallHelper.getPhoneIdForECall(mContext);
    }

    @Override
    public boolean hasGetIccFileHandler(int slotId, int family) {
       if (mContext.checkCallingOrSelfPermission(
               android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE) !=
                       PackageManager.PERMISSION_GRANTED) {
             throw new SecurityException(
                    "Requires android.permission.READ_PRIVILEGED_PHONE_STATE permission");
       }

       if(mQtiUiccEfHelper.loadIccFileHandler(slotId, family) != null) {
           return true;
       } else {
           return false;
       }
    }

    @Override
    public boolean readEfFromIcc(int slotId, int family, int efId) {
       if (mContext.checkCallingOrSelfPermission(
               android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE) !=
                       PackageManager.PERMISSION_GRANTED) {
             throw new SecurityException(
                    "Requires android.permission.READ_PRIVILEGED_PHONE_STATE permission");
       }
       return mQtiUiccEfHelper.readUiccEf(slotId, family, efId);
    }

    @Override
    public boolean writeEfToIcc(int slotId, int family, int efId, byte[] efData) {
       if (mContext.checkCallingOrSelfPermission(
               android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE) !=
                       PackageManager.PERMISSION_GRANTED) {
             throw new SecurityException(
                    "Requires android.permission.READ_PRIVILEGED_PHONE_STATE permission");
       }
       return mQtiUiccEfHelper.writeUiccEf(slotId, family, efId, efData);
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            writer.println("Permission Denial: can't dump ExtPhone from pid="
                    + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid()
                    + "without permission "
                    + android.Manifest.permission.DUMP);
            writer.flush();
            return;
        }
        MainServiceImpl.getInstance().dump(fd, writer, args);

    }
}
