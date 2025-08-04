/*
 * Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.telephony.ims.ImsSsData;

import org.codeaurora.ims.ImsPhoneCommandsInterface.RadioState;
import org.codeaurora.ims.sms.StatusReport;
import org.codeaurora.ims.sms.IncomingSms;

import java.util.List;

/*
 * Internal interface that forwards indications from the hardware abstraction layer (HAL)
 */
public interface IImsRadioIndication {

    void onServiceUp();

    void onServiceDown();

    void onCallStateChanged(List<DriverCallIms> driverCallImsList);

    void onImsSmsStatusReport(StatusReport smsStatusReport);

    void onIncomingImsSms(IncomingSms imsSms);

    void onRing();

    void onRingbackTone(boolean tone);

    void onRegistrationChanged(ImsRegistrationInfo registrationInfo);

    void onHandover(HoInfo hoInfo);

    void onServiceStatusChanged(List<ServiceStatus> srvStatusList);

    void onRadioStateChanged(RadioState radioState);

    void onEnterEmergencyCallBackMode();

    void onExitEmergencyCallBackMode();

    void onTtyNotification(int[] mode);

    void onRefreshConferenceInfo(ConfInfo info);

    void onRefreshViceInfo(ViceUriInfo viceInfo);

    void onModifyCall(CallModify callModifyInfo);

    void onSuppServiceNotification(SuppNotifyInfo suppServiceNotifInfo);

    void onMessageWaiting(Mwi mwiIndication);

    void onGeolocationInfoRequested(GeoLocationInfo geoLocationInfo);

    void onIncomingCallAutoRejected(DriverCallIms driverCallIms);

    void onImsSubConfigChanged(ImsSubConfigDetails configDetails);

    void onParticipantStatusInfo(ParticipantStatusDetails participantStatusInfo);

    void onRegistrationBlockStatus(RegistrationBlockStatusInfo registrationBlockStatusInfo);

    void onRttMessageReceived(String msg);

    void onVoiceInfoChanged(int voiceInfo);

    void onVoWiFiCallQuality(int[] voWiFiCallQuality);

    void onSupplementaryServiceIndication(ImsSsData ssData);

    void onVopsChanged(boolean isVopsEnabled);

    void onMultiIdentityRegistrationStatusChange(
            List<MultiIdentityLineInfo> linesInfo);

    void onMultiIdentityInfoPending();

    void onModemSupportsWfcRoamingModeConfiguration(
            boolean wfcRoamingConfigurationSupport);

    void onUssdMessageFailed(UssdInfo ussdInfo);

    void onUssdReceived(UssdInfo ussdInfo);

    void onCallComposerInfoAvailable(int callId, CallComposerInfo callComposerInfo);

    void onRetrievingGeoLocationDataStatus(int geoLocationDataStatus);

    void onSipDtmfReceived(String configCode);

    void onServiceDomainChanged(int domain);

    void onSmsCallBackModeChanged(int mode);

    void onConferenceCallStateCompleted();

    /*String dtmf will have size 1 */
    void onIncomingDtmfStart(int callId, String dtmf);

    /*String dtmf will have size 1 */
    void onIncomingDtmfStop(int callId, String dtmf);

    void onMultiSimVoiceCapabilityChanged(int voiceCapability);

    void onPreAlertingCallInfoAvailable(PreAlertingCallInfo info);

    void onCiWlanNotification(boolean show);

    void onSrtpEncryptionInfo(int callId, int category);
}
