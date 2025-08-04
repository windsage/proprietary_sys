/**
 * Copyright (c) 2015-2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import org.codeaurora.ims.QtiImsExtManager;
import org.codeaurora.ims.utils.QtiCarrierConfigHelper;
import org.codeaurora.ims.utils.QtiImsExtUtils;

import com.qualcomm.ims.utils.Log;
import com.qualcomm.ims.vt.ImsVideoGlobals;

import android.telephony.ims.stub.ImsConfigImplBase;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.List;

public class ImsService extends android.telephony.ims.ImsService {

    private static final String LOG_TAG = "ImsService";

    private static ImsSubController mSubController;

    private void setup() {
        getQtiCarrierConfigHelper().setup(this);
        migrateDb();
        mSubController = new ImsSubController(this);
        ImsVideoGlobals.init(getServiceSubs(), this);
        mSubController.registerListener(ImsVideoGlobals.getInstance());
    }

    static public List<ImsServiceSub> getServiceSubs() {
        return (mSubController != null) ? mSubController.getServiceSubs() : null;
    }

    // Migrate db keys from vendor to AOSP
    private void migrateDb() {
        final String QTI_IMS_RTT_MODE = "rtt_mode";
        final int RTT_MODE_INVALID =  -1;
        //Read rtt mode from old db key and save in Settings.Secure.RTT_CALLING_MODE key.
        //This needs to be performed only once when new s/w with updated key is run first time.
        int rttMode = android.provider.Settings.Global.getInt(this.getContentResolver(),
                QTI_IMS_RTT_MODE, RTT_MODE_INVALID);
        if (rttMode != RTT_MODE_INVALID) {
            Log.v(this, "upgradeDb: migrate to new db key for rtt. mode=" + rttMode);
            android.provider.Settings.Secure.putInt(this.getContentResolver(),
                    Settings.Secure.RTT_CALLING_MODE, rttMode);
            android.provider.Settings.Global.putInt(this.getContentResolver(),
                    QTI_IMS_RTT_MODE, RTT_MODE_INVALID);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(this, "ImsService created!");
        setup();
    }

    /**
     * When called, provide the {@link ImsFeatureConfiguration} that this {@link ImsService}
     * currently supports. This will trigger the framework to set up the {@link ImsFeature}s that
     * correspond to the {@link ImsFeature}s configured here.
     *
     * Use {@link #onUpdateSupportedImsFeatures(ImsFeatureConfiguration)} to change the supported
     * {@link ImsFeature}s.
     *
     * @return an {@link ImsFeatureConfiguration} containing Features this ImsService supports.
     */
    @Override
    public ImsFeatureConfiguration querySupportedImsFeatures() {
        // NOTE: This method returns the same features that are declared in the AndroidManifest
        // file, as supported.
        ImsFeatureConfiguration.Builder features = new ImsFeatureConfiguration.Builder();
        for (int i = 0; i < mSubController.getActiveModemCount(); i++) {
            features.addFeature(i, ImsFeature.FEATURE_MMTEL)
                    .addFeature(i, ImsFeature.FEATURE_EMERGENCY_MMTEL);
        }
        return features.build();
    }


    /**
     * The ImsService has been bound and is ready for ImsFeature creation based on the Features that
     * the ImsService has registered for with the framework, either in the manifest or via
     * {@link #querySupportedImsFeatures()}.
     *
     * The ImsService should use this signal instead of onCreate/onBind or similar to perform
     * feature initialization because the framework may bind to this service multiple times to
     * query the ImsService's {@link ImsFeatureConfiguration} via
     * {@link #querySupportedImsFeatures()}before creating features.
     */
    @Override
    public void readyForFeatureCreation() {
        Log.i(this, "readyForFeatureCreation :: No-op");
        // NOTE: This method is a no-op. IMS Service initializaiton will have to move
        // into this method if/when AOSP starts binding to IMS Service multiple times,
    }

    /**
     * The framework has enabled IMS for the slot specified, the ImsService should register for IMS
     * and perform all appropriate initialization to bring up all ImsFeatures.
     */
    @Override
    public void enableIms(int slotId) {
        Log.i(this, "enableIms :: slotId=" + slotId);
        ImsServiceSub serviceSub = (mSubController != null) ?
                mSubController.getServiceSub(slotId) : null;
        if (serviceSub == null) {
            Log.e(this, "enableIms :: Invalid slotId " + slotId);
            return;
        }
        serviceSub.turnOnIms();
    }

    /**
     * The framework has disabled IMS for the slot specified. The ImsService must deregister for IMS
     * and set capability status to false for all ImsFeatures.
     */
    @Override
    public void disableIms(int slotId) {
        Log.i(this, "disableIms :: slotId=" + slotId);
        ImsServiceSub serviceSub = (mSubController != null) ?
                mSubController.getServiceSub(slotId) : null;
        if (serviceSub == null) {
            Log.e(this, "disableIms :: Invalid slotId " + slotId);
            return;
        }
        serviceSub.turnOffIms();
    }

    /**
     * When called, the framework is requesting that a new {@link MmTelFeature} is created for the
     * specified slot.
     *
     * @param slotId The slot ID that the MMTEL Feature is being created for.
     * @return The newly created {@link MmTelFeature} associated with the slot or null if the
     * feature is not supported.
     */
    @Override
    public MmTelFeature createMmTelFeature(int slotId) {
        // NOTE: Since ImsServiceSub/MmTelFeature instances are created independently (as
        // part of this IMS Service's initialization), we simply return a created
        // instance, corresponding to the slotId.
        Log.d(this, "createMmTelFeature :: slotId=" + slotId);
        ImsServiceSub serviceSub = (mSubController != null) ?
                mSubController.getServiceSub(slotId) : null;
        if (serviceSub == null) {
            Log.e(this, "createMmTelFeature :: Invalid slotId " + slotId);
            return null;
        }
        return serviceSub;
    }

    /**
     * Return the {@link ImsConfigImplBase} implementation associated with the provided slot. This
     * will be used by the platform to get/set specific IMS related configurations.
     *
     * @param slotId The slot that the IMS configuration is associated with.
     * @return ImsConfig implementation that is associated with the specified slot.
     */
    @Override
    public ImsConfigImplBase getConfig(int slotId) {
        Log.d(this, "getConfig :: slotId=" + slotId);
        ImsServiceSub serviceSub = (mSubController != null) ?
                mSubController.getServiceSub(slotId) : null;
        if (serviceSub == null) {
            Log.e(this, "getConfig :: invalid slotId=" + slotId);
            return null;
        }
        return (ImsConfigImplBase) serviceSub.getConfigInterface();
    }

    /**
     * Return the {@link ImsRegistrationImplBase} implementation associated with the provided slot.
     *
     * @param slotId The slot that is associated with the IMS Registration.
     * @return the ImsRegistration implementation associated with the slot.
     */
    @Override
    public ImsRegistrationImplBase getRegistration(int slotId) {
        Log.d(this, "getRegistration :: slotId=" + slotId);
        ImsServiceSub serviceSub = (mSubController != null) ?
                mSubController.getServiceSub(slotId) : null;
        if (serviceSub == null) {
            Log.e(this, "getRegistration :: invalid slotId=" + slotId);
            return null;
        }
        return serviceSub.getImsRegistrationInterface();
    }

    @Override
    public void onDestroy() {
        Log.i(this, "Ims Service Destroyed Successfully...");
        mSubController.unregisterListener(ImsVideoGlobals.getInstance());
        ImsVideoGlobals.getInstance().dispose();
        if (mSubController != null) {
            mSubController.dispose();
        }
        mSubController = null;
        getQtiCarrierConfigHelper().teardown();
        super.onDestroy();
    }

    @VisibleForTesting
    public QtiCarrierConfigHelper getQtiCarrierConfigHelper() {
        return QtiCarrierConfigHelper.getInstance();
    }

    @Override
    public Executor getExecutor() {
        return this.getMainExecutor();
    }
}
