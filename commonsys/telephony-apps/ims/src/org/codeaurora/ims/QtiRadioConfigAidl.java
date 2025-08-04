/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfig;

public class QtiRadioConfigAidl implements org.codeaurora.ims.IQtiRadioConfig {
    private IQtiRadioConfig mQtiRadioConfig;
    private IQtiRadioConfigIndication mIndication;
    private final Object mHalSync = new Object();
    static final String QTI_RADIO_CONFIG_SERVICE_NAME =
            "vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfig/default";
    private final String LOG_TAG = "QtiRadioConfigAidl";
    private Context mContext;
    private QtiRadioConfigDeathRecipient mQtiRadioConfigDeathRecipient;
    private IBinder mQtiRadioConfigBinder;

    // The constant here is the value corresponding to the argument "feature" in
    // isFeatureSupported(int feature) API in IQtiRadioConfig AIDL. The values
    // will increase by 1 for every new feature.
    public static final int INVALID_FEATURE = -1;
    public static final int INTERNAL_AIDL_REORDERING = 1;
    public static final int DSDS_TRANSITION = 2;
    // private static final int SMART_TEMP_DDS_VIA_RADIO_CONFIG = 3;
    // private static final int EMERGENCY_ENHANCEMENT = 4;
    // private static final int TDSCDMA_SUPPORTED = 5;
    // Value 3,4,5 are already used in telephony-fwk project, so
    // using value 6 and 7 for uvs crbt call and glassess free 3D video
    public static final int UVS_CRBT_CALL = 6;
    public static final int GLASSES_FREE_3D_VIDEO = 7;
    public static final int CONCURRENT_CONFERENCE_EMERGENCY_CALL = 8;

    public QtiRadioConfigAidl(Context context, IQtiRadioConfigIndication indication) {
        mContext = context;
        mIndication = indication;
        mQtiRadioConfigDeathRecipient = new QtiRadioConfigDeathRecipient();
        initQtiRadioConfig();
    }

    private void initQtiRadioConfig() {
        try {
            Log.d(LOG_TAG, "initQtiRadioConfig");
            IBinder binder  = Binder.allowBlocking(
                    ServiceManager.waitForDeclaredService(QTI_RADIO_CONFIG_SERVICE_NAME));
            if (binder == null) {
                Log.e(LOG_TAG, "initQtiRadioConfig failed");
                return;
            }

            IQtiRadioConfig qtiRadioConfig =
                    vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfig.Stub.asInterface(
                            binder);
            if (qtiRadioConfig == null) {
                Log.e(LOG_TAG,"Get binder for IQtiRadioConfig stable AIDL failed");
                return;
            }

            synchronized (mHalSync) {
                mQtiRadioConfigBinder = binder;
                mQtiRadioConfig = qtiRadioConfig;
            }
            binder.linkToDeath(mQtiRadioConfigDeathRecipient, 0 /* Not Used */);
            mIndication.onServiceUp();
        } catch (Exception ex) {
            Log.e(LOG_TAG, "initQtiRadioConfig: Exception: " + ex);
            resetQtiRadioConfigHalInterfaces();
        }
    }

    @Override
    public boolean isFeatureSupported(int feature) throws RemoteException {
        boolean featureSupported = false;
        int aidlFeature = StableAidl.fromFeature(feature);
        if (aidlFeature == INVALID_FEATURE) { return false; }
        try {
            featureSupported = getQtiRadioConfig().isFeatureSupported(aidlFeature);
            Log.d(LOG_TAG, "isFeatureSupported " + feature + " = " + featureSupported);
            return featureSupported;
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "isFeatureSupported Failed", ex);
            return false;
        }
    }

    final class QtiRadioConfigDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service dies
         */
        @Override
        public void binderDied() {
            Log.e(LOG_TAG, "IQtiRadioConfig died");
            resetQtiRadioConfigHalInterfaces();
            initQtiRadioConfig();
        }
    }

    private IQtiRadioConfig getQtiRadioConfig() throws RemoteException {
        synchronized (mHalSync) {
            if (mQtiRadioConfig != null) {
                return mQtiRadioConfig;
            } else {
                throw new RemoteException("mQtiRadioConfig is null");
            }
        }
    }

    private void resetQtiRadioConfigHalInterfaces() {
        Log.d(LOG_TAG, "resetQtiRadioConfigHalInterfaces: Resetting HAL interfaces.");
        mIndication.onServiceDown();
        IBinder binder = null;
        synchronized (mHalSync) {
            mQtiRadioConfig = null;
            binder = mQtiRadioConfigBinder;
            mQtiRadioConfigBinder = null;
        }
        if (binder != null) {
            try {
                boolean result = binder.unlinkToDeath(
                        mQtiRadioConfigDeathRecipient, 0 /* Not used */);
            } catch (Exception ex) {
                Log.e(LOG_TAG, "Failed to unlink IQtiRadioConfig death recipient", ex);
            }
        }
    }
}
