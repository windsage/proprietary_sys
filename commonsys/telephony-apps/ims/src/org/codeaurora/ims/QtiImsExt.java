/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package org.codeaurora.ims;

import android.content.Context;
import android.os.Bundle;

import com.qualcomm.ims.utils.Log;
import org.codeaurora.ims.internal.IImsArController;
import org.codeaurora.ims.internal.ICrsCrbtController;
import org.codeaurora.ims.internal.IQtiImsExt;
import org.codeaurora.ims.internal.IQtiImsExtListener;
import org.codeaurora.ims.internal.IImsMultiIdentityInterface;
import org.codeaurora.ims.internal.IImsScreenShareController;
import org.codeaurora.ims.QtiImsExtBase;
import org.codeaurora.ims.ImsCallUtils;
import org.codeaurora.ims.QtiCallConstants;

import java.util.List;

public class QtiImsExt extends QtiImsExtBase {

    private Context mContext;
    private List<ImsServiceSub> mServiceSub;

    public QtiImsExt(Context context, List<ImsServiceSub> serviceSub) {
        super(context.getMainExecutor(), context);
        mContext = context;
        mServiceSub = serviceSub;
    }

    @Override
    protected void onSetCallForwardUncondTimer(int phoneId, int startHour, int startMinute,
            int endHour, int endMinute, int action, int condition, int serviceClass, String number,
            IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).setCallForwardUncondTimer(startHour, startMinute,
                endHour, endMinute, action, condition, serviceClass, number, listener);
    }

    @Override
    protected void onGetCallForwardUncondTimer(int phoneId, int reason, int serviceClass,
            IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).getCallForwardUncondTimer(reason,
                serviceClass, listener);
    }

    @Override
    protected void onResumePendingCall(int phoneId, int videoState) {
        mServiceSub.get(phoneId).resumePendingCall(videoState);
    }

    @Override
    protected void onSendCancelModifyCall(int phoneId, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).sendCancelModifyCall(listener);
    }

    @Override
    protected void onSetUssdInfoListener(int phoneId, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).setUssdInfoListener(listener);
    }

    @Override
    protected void onQueryVopsStatus(int phoneId, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).queryVopsStatus(listener);
    }

    @Override
    protected void onQuerySsacStatus(int phoneId, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).querySsacStatus(listener);
    }

    @Override
    protected void onRegisterForParticipantStatusInfo(int phoneId, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).registerForParticipantStatusInfo(listener);
    }

    @Override
    protected void onUpdateVoltePreference(int phoneId, int preference,
            IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).updateVoltePreference(preference, listener);
    }

    @Override
    protected void onQueryVoltePreference(int phoneId, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).queryVoltePreference(listener);
    }

    @Override
    protected void onGetHandoverConfig(int phoneId, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).getHandoverConfig(listener);
    }

    @Override
    protected void onSetHandoverConfig(int phoneId, int hoConfig, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).setHandoverConfig(hoConfig, listener);
    }

    @Override
    protected int onGetVvmAppConfig(int phoneId) {
        try {
            return mServiceSub.get(phoneId).getConfigInterface()
                    .getConfigInt(QtiCallConstants.QTI_CONFIG_VVM_APP);
        } catch (Exception re) {
            Log.e(this, "onGetVvmAppConfig :: Exception : " + re.getMessage());
        }

        return -1;
    }

    @Override
    protected int onSetVvmAppConfig(int phoneId, int defaultVvmApp) {
        try {
            return mServiceSub.get(phoneId).getConfigInterface().setConfig(
                    QtiCallConstants.QTI_CONFIG_VVM_APP, defaultVvmApp);
        } catch (Exception re) {
            Log.e(this, "onSetVvmAppConfig :: Exception : " + re.getMessage());
        }

        return -1;
    }

    @Override
    protected int onGetRcsAppConfig(int phoneId) {
        try {
            return mServiceSub.get(phoneId).getConfigInterface()
                    .getConfigInt(QtiCallConstants.QTI_CONFIG_SMS_APP);
        } catch (Exception re) {
            Log.e(this, "onGetRcsAppConfig :: Exception : " + re.getMessage());
        }

        return -1;
    }

    @Override
    protected int onSetRcsAppConfig(int phoneId, int defaultSmsApp) {
        try {
            return mServiceSub.get(phoneId).getConfigInterface().setConfig(
                    QtiCallConstants.QTI_CONFIG_SMS_APP, defaultSmsApp);
        } catch (Exception re) {
            Log.e(this, "onSetRcsAppConfig :: Exception : " + re.getMessage());
        }

        return -1;
    }

    @Override
    protected IImsMultiIdentityInterface onGetMultiIdentityInterface(int phoneId) {
        ImsMultiIdentityControllerBase v = mServiceSub.get(phoneId).getMultiIdentityImpl();
        return v != null ? v.getBinder() : null;
    }

    @Override
    protected IImsScreenShareController onGetScreenShareController(int phoneId) {
        ImsScreenShareControllerBase v = mServiceSub.get(phoneId).getScreenShareController();
        return v != null ? v.getBinder() : null;
    }

    @Override
    protected int onGetImsFeatureState(int phoneId) {
        return mServiceSub.get(phoneId).getImsFeatureState();
    }

    @Override
    protected void onSetAnswerExtras(int phoneId, Bundle extras) {
        mServiceSub.get(phoneId).setAnswerExtras(extras);
    }

    @Override
    protected boolean onIsCallComposerEnabled(int phoneId) {
        return mServiceSub.get(phoneId).isCallComposerSupported();
    }

    @Override
    protected ICrsCrbtController onGetCrsCrbtController(int phoneId) {
        CrsCrbtControllerBase v = mServiceSub.get(phoneId).getCrsCrbtController();
        return v != null ? v.getBinder() : null;
    }

    @Override
    protected void onQueryCallForwardStatus(int phoneId, int reason, int serviceClass,
            boolean expectMore, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).queryCallForwardStatus(reason,
                serviceClass, expectMore, listener);
    }

    @Override
    protected void onQueryCallBarringStatus(int phoneId, int cbType, String password,
            int serviceClass, boolean expectMore, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).queryCallBarringStatus(cbType, password, serviceClass,
                expectMore, listener);
    }

    @Override
    protected void onExitScbm(int phoneId, IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).exitScbm(listener);
    }

    @Override
    protected boolean onIsExitScbmFeatureSupported(int phoneId) {
        return mServiceSub.get(phoneId).isExitScbmFeatureSupported();
    }

    @Override
    protected void onSetDataChannelCapabilityListener(int phoneId,
            IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).setDataChannelCapabilityListener(listener);
    }

    @Override
    protected boolean onIsDataChannelEnabled(int phoneId) {
        return mServiceSub.get(phoneId).isDataChannelSupported();
    }

    @Override
    protected void onSendVosSupportStatus(int phoneId, boolean isVosSupported,
            IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).sendVosSupportStatus(isVosSupported, listener);
    }

    @Override
    protected void onSendVosActionInfo(int phoneId, VosActionInfo vosActionInfo,
            IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).sendVosActionInfo(vosActionInfo, listener);
    }

    @Override
    protected IImsArController onGetArController(int phoneId) {
        ImsArControllerBase v = mServiceSub.get(phoneId).getArController();
        return v != null ? v.getBinder() : null;
    }

    @Override
    protected void onSetGlassesFree3dVideoCapability(int phoneId, boolean enable3dVideo,
            IQtiImsExtListener listener) {
        mServiceSub.get(phoneId).setGlassesFree3dVideoCapability(enable3dVideo, listener);
    }
}
