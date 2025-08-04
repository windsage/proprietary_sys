/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.telephony.ims.ImsReasonInfo;

import org.codeaurora.ims.sms.SmsResponse;

import java.util.List;

/*
 * Internal interface that forwards responses from the hardware abstraction layer
 * (HAL).
 */

public interface IImsRadioResponse {

    void onDialResponse(int token, int errorCode);

    void onSendImsSmsResponse(int token, SmsResponse response);

    void onGetConfigResponse(int token, int errorCode, Object ret);

    void onSetConfigResponse(int token, int errorCode, Object ret);

    void onQueryServiceStatusResponse(int token, int errorCode,
                                    List<ServiceStatus> serviceStatusInfoList);

    void onSetServiceStatusResponse(int token, int errorCode);

    void onResumeResponse(int token, int errorCode, ImsReasonInfo errorInfo);

    void onHoldResponse(int token, int errorCode, ImsReasonInfo errorInfo);

    void onHangupResponse(int token, int errorCode);

    void onAnswerResponse(int token, int errorCode);

    void onRequestRegistrationChangeResponse(int token, int errorCode);

    void onGetRegistrationResponse(int token, int errorCode,
                                   ImsRegistrationInfo registration);

    void onSuppServiceStatusResponse(int token, int errorCode,
                                     SuppSvcResponse suppSvcResponse);

    void onConferenceResponse(int token, int errorCode, ImsReasonInfo errorInfo);

    void onGetClipResponse(int token, int errorCode, SuppService clipProvisionStatus);

    void onGetClirResponse(int token, int errorCode, int[] clirInfo);

    void onQueryCallForwardStatusResponse(int token, int errorCode,
                                          ImsCallForwardTimerInfo timerInfo[]);

    void onGetCallWaitingResponse(int token, int errorCode, int[] response);

    void onSetClirResponse(int token, int errorCode);

    void onGetColrResponse(int token, int errorCode, SuppService colrInfo);

    void onExitEmergencyCallbackModeResponse(int token, int errorCode);

    void onSendDtmfResponse(int token, int errorCode);

    void onStartDtmfResponse(int token, int errorCode);

    void onStopDtmfResponse(int token, int errorCode);

    void onSetUiTTYModeResponse(int token, int errorCode);

    void onModifyCallInitiateResponse(int token, int errorCode);

    void onCancelModifyCallResponse(int token, int errorCode);

    void onModifyCallConfirmResponse(int token, int errorCode);

    void onExplicitCallTransferResponse(int token, int errorCode, ImsReasonInfo errorInfo);

    void onSetSuppServiceNotificationResponse(int token, int errorCode,
                                                   int serviceStatusClass);

    void onGetRtpStatisticsResponse(int token, int errorCode,
                                         long packetCount);

    void onGetRtpErrorStatisticsResponse(int token, int errorCode,
                                              long packetErrorCount);

    void onAddParticipantResponse(int token, int errorCode);

    void onDeflectCallResponse(int token, int errorCode);

    void onSendGeolocationInfoResponse(int token, int errorCode);

    void onGetImsSubConfigResponse(int token, int errorCode,
                                 ImsSubConfigDetails subConfigInfo);

    void onSendUssdResponse(int token, int errorCode, ImsReasonInfo errorDetails);

    void onSendSipDtmfResponse(int token, int errorCode);

    void onCancelPendingUssdResponse(int token, int errorCode,
                                     ImsReasonInfo errorDetails);

    void onSendRttMessageResponse(int token, int errorCode);

    void onRegisterMultiIdentityLinesResponse(int token, int errorCode);

    void onQueryVirtualLineInfoResponse(int token, String msisdn,
                                      VirtualLineInfo virtualLineInfo);

    void onSetCallForwardStatusResponse(int token, int errorCode,
                                      CallForwardStatusInfo callForwardStatusInfo);

    void onSetMediaConfigurationResponse(int token, int errorCode);

    void onQueryMultiSimVoiceCapabilityResponse(int token, int errorCode, int voiceCapability);

    void exitSmsCallBackModeResponse(int token, int errorCode);

    void onSendVosSupportStatusResponse(int token, int errorCode);

    void onSendVosActionInfoResponse(int token, int errorCode);

    void onSetGlassesFree3dVideoCapabilityResponse(int token, int errorCode);

    void onAbortConferenceResponse(int token, int errorCode);
}
