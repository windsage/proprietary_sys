/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.enrollment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.quicinc.voice.activation.aidl.IEnrollmentService;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A public class which handles connection with QVA engine services. it binds QVA engine services to
 * establish connection with them, it must be created and connected before being used with
 * EnrollmentManager operation.
 */
public class EnrollmentConnector extends AbstractConnector {
    public final static String ENROLLMENT_SERVICE_NAME =
            "com.quicinc.voice.activation.engineservice.EnrollmentService";
    private final static Intent SERVICE_INTENT =
            new Intent()
                    .setClassName(Constants.QVA_PACKAGE_NAME, ENROLLMENT_SERVICE_NAME);

    public EnrollmentConnector(Context context) {
        super(context, SERVICE_INTENT);
    }


    public IEnrollmentService getEnrollmentService() {
        return IEnrollmentService.Stub.asInterface(getBinder());
    }

    public void startUserVoiceEnrollment(Enrollment enrollment,
                                         WeakReference<IOperationCallback<ArrayList<String>,
                                         EnrollmentFailedReason>> callback) {
        EnrollmentManager.startUserVoiceEnrollment(this, enrollment, callback);
    }

    public void startUtteranceTraining(String utteranceName,
                                       WeakReference<IUtteranceCallback> callback) {
        EnrollmentManager.startUtteranceTraining(this, utteranceName, callback);
    }

    public void cancelUtteranceTraining(String utteranceName,
                                        IOperationCallback<Bundle, EnrollmentFailedReason> callback) {
        EnrollmentManager.cancelUtteranceTraining(this, utteranceName, callback);
    }

    public void commitUserVoiceEnrollment(WeakReference<IOperationCallback<
            EnrollmentSuccessInfo, EnrollmentFailedReason>> callback) {
        EnrollmentManager.commitUserVoiceEnrollment(this, callback);
    }

    public void finishUserVoiceEnrollment() {
        EnrollmentManager.finishUserVoiceEnrollment(this);
    }

    public void removeGeneralUV(IOperationCallback<String, String> callback) {
        EnrollmentManager.removeGeneralUV(this, callback);
    }

    public boolean isGeneralUVEnrolled() {
        return EnrollmentManager.isGeneralUVEnrolled(this);
    }
}
