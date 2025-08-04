/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import com.qualcomm.ims.utils.Log;

import org.codeaurora.ims.ImsPhoneCommandsInterface.RadioState;
import org.codeaurora.ims.sms.IncomingSms;
import org.codeaurora.ims.sms.StatusReport;

import java.util.ArrayList;
import java.util.List;

import vendor.qti.hardware.radio.ims.AutoCallRejectionInfo;
import vendor.qti.hardware.radio.ims.AutoCallRejectionInfo2;
import vendor.qti.hardware.radio.ims.CallComposerAutoRejectionInfo;
import vendor.qti.hardware.radio.ims.CallComposerInfo;
import vendor.qti.hardware.radio.ims.CallInfo;
import vendor.qti.hardware.radio.ims.CallForwardInfo;
import vendor.qti.hardware.radio.ims.CallModifyInfo;
import vendor.qti.hardware.radio.ims.CfData;
import vendor.qti.hardware.radio.ims.ConferenceCallState;
import vendor.qti.hardware.radio.ims.ConferenceInfo;
import vendor.qti.hardware.radio.ims.DtmfInfo;
import vendor.qti.hardware.radio.ims.EmergencyCallBackMode;
import vendor.qti.hardware.radio.ims.GeoLocationDataStatus;
import vendor.qti.hardware.radio.ims.HandoverInfo;
import vendor.qti.hardware.radio.ims.ImsSubConfigInfo;
import vendor.qti.hardware.radio.ims.MessageWaitingIndication;
import vendor.qti.hardware.radio.ims.MultiIdentityLineInfo;
import vendor.qti.hardware.radio.ims.MultiSimVoiceCapability;
import vendor.qti.hardware.radio.ims.ParticipantStatusInfo;
import vendor.qti.hardware.radio.ims.PreAlertingCallInfo;
import vendor.qti.hardware.radio.ims.RegistrationBlockStatusInfo;
import vendor.qti.hardware.radio.ims.RegistrationInfo;
import vendor.qti.hardware.radio.ims.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.SipErrorInfo;
import vendor.qti.hardware.radio.ims.SmsCallBackMode;
import vendor.qti.hardware.radio.ims.SmsSendStatusReport;
import vendor.qti.hardware.radio.ims.SrtpEncryptionInfo;
import vendor.qti.hardware.radio.ims.SsInfoData;
import vendor.qti.hardware.radio.ims.StkCcUnsolSsResult;
import vendor.qti.hardware.radio.ims.SuppServiceNotification;
import vendor.qti.hardware.radio.ims.SystemServiceDomain;
import vendor.qti.hardware.radio.ims.ToneOperation;
import vendor.qti.hardware.radio.ims.TtyInfo;
import vendor.qti.hardware.radio.ims.UssdModeType;
import vendor.qti.hardware.radio.ims.ViceInfo;
import vendor.qti.hardware.radio.ims.VoiceInfo;
import vendor.qti.hardware.radio.ims.VoWiFiCallQuality;

/* This class handles AIDL indications, converts objects from AIDL->Telephony java type
 * and forwards the response to ImsSenderRxr
 */
public class ImsRadioIndicationAidl extends vendor.qti.hardware.radio.ims.IImsRadioIndication.Stub {

    private IImsRadioIndication mImsRadioIndication;
    private int mPhoneId;
    private final String mLogSuffix;

    public ImsRadioIndicationAidl(IImsRadioIndication indication, int phoneId) {
        mImsRadioIndication = indication;
        mPhoneId = phoneId;
        mLogSuffix ="[SUB" + mPhoneId + "]";
    }

    private void log(String msg) {
        Log.i(this, msg + mLogSuffix);
    }

    private void loge(String msg) {
        Log.e(this, msg + mLogSuffix);
    }

    private void logv(String msg) {
        Log.v(this, msg + mLogSuffix);
    }

    @Override
    public final int getInterfaceVersion() {
        return vendor.qti.hardware.radio.ims.IImsRadioIndication.VERSION;
    }

    @Override
    public String getInterfaceHash() {
        return vendor.qti.hardware.radio.ims.IImsRadioIndication.HASH;
    }

    @Override
    public void onCallStateChanged(CallInfo[] callList) {
        log("onCallStateChanged()");

        if (callList == null) {
            loge("Call list is null.");
            return;
        }

        ArrayList<DriverCallIms> response = StableAidl.toDriverCallImsArray(callList);
        mImsRadioIndication.onCallStateChanged(response);
    }

    @Override
    public void onRing() {
        log("onRing()");
        mImsRadioIndication.onRing();
    }

    @Override
    public void onRingbackTone(int tone) {
        int[] response = new int[1];

        response[0] = StableAidl.toRingbackTone(tone);
        log("onRingbackTone() response: " + response[0]);

        boolean playtone = (response[0] == CallDetails.RINGBACK_TONE_START);
        mImsRadioIndication.onRingbackTone(playtone);
    }

    @Override
    public void onRegistrationChanged(RegistrationInfo registration) {
        log("onRegistrationChanged()");
        ImsRegistrationInfo regMessage = StableAidl.toImsRegistration(registration);
        mImsRadioIndication.onRegistrationChanged(regMessage);
    }

    @Override
    public void onHandover(HandoverInfo handover) {
        log("onHandover()");
        HoInfo outHandover = StableAidl.toHandover(handover);
        mImsRadioIndication.onHandover(outHandover);
    }

    @Override
    public void onServiceStatusChanged(ServiceStatusInfo[] srvStatusList) {
        log("onServiceStatusChanged()");
        ArrayList<ServiceStatus> ret = StableAidl.toServiceStatus(srvStatusList);
        mImsRadioIndication.onServiceStatusChanged(ret);
    }

    @Override
    public void onRadioStateChanged(int radioState) {
        log("onRadioStateChanged()");
        RadioState outRadioState = StableAidl.toRadioState(radioState);
        mImsRadioIndication.onRadioStateChanged(outRadioState);
    }

    @Override
    public void onEmergencyCallBackModeChanged(int mode) {
        log("onEmergencyCallBackModeChanged() ECB mode : " + mode);
        switch (mode) {
            case EmergencyCallBackMode.EXIT:
                mImsRadioIndication.onExitEmergencyCallBackMode();
                break;
            case EmergencyCallBackMode.ENTER:
                mImsRadioIndication.onEnterEmergencyCallBackMode();
                break;
            case EmergencyCallBackMode.INVALID:
            default:
                loge("onEmergencyCallBackModeChanged: invalid ECBM");
                break;
        }
    }

    @Override
    public void onTtyNotification(TtyInfo ttyInfo) {
        log("onTtyNotification ()");
        if (ttyInfo == null) {
            loge("onTtyNotification: ttyInfo is null");
            return;
        }
        mImsRadioIndication.onTtyNotification(new int[]{StableAidl.toTtyMode(ttyInfo.mode)});
    }

    @Override
    public void onRefreshConferenceInfo(ConferenceInfo conferenceInfo) {
        log("onRefreshConferenceInfo()");
        if (conferenceInfo == null) {
            loge("onRefreshConferenceInfo: Conferenceinfo is null");
            return;
        }

        ConfInfo info = StableAidl.toConferenceInfo(conferenceInfo);
        mImsRadioIndication.onRefreshConferenceInfo(info);
    }

    @Override
    public void onRefreshViceInfo(ViceInfo viceInfo) {
        log("onRefreshViceInfo ()");
        if (viceInfo == null || viceInfo.viceInfoUri == null) {
            loge("onRefreshViceInfo: Viceinfo or viceinfouri is null");
            return;
        }
        ViceUriInfo info = StableAidl.toViceUriInfo(viceInfo);
        mImsRadioIndication.onRefreshViceInfo(info);
    }

    @Override
    public void onModifyCall(CallModifyInfo callModifyInfo) {
        log("onModifyCall()");
        if (callModifyInfo == null) {
            loge("onModifyCall: callModifyInfo is null");
            return;
        }
        CallModify ret = StableAidl.toCallModify(callModifyInfo);
        mImsRadioIndication.onModifyCall(ret);
    }

    @Override
    public void onSuppServiceNotification(SuppServiceNotification suppServiceNotification) {
        log("onSuppServiceNotification()");
        SuppNotifyInfo suppNotifyInfo = StableAidl.toSuppNotifyInfo(suppServiceNotification);
        mImsRadioIndication.onSuppServiceNotification(suppNotifyInfo);
    }

    @Override
    public void onMessageWaiting(MessageWaitingIndication messageWaitingIndication) {
        log("onMessageWaiting()");
        Mwi mwi = StableAidl.toMessageWaitingIndication(messageWaitingIndication);
        mImsRadioIndication.onMessageWaiting(mwi);
    }

    @Override
    public void onGeolocationInfoRequested(double lat, double lon) {
        log("onGeolocationInfoRequested()");
        mImsRadioIndication.onGeolocationInfoRequested(new GeoLocationInfo(lat, lon));
    }

    @Override
    public void onImsSubConfigChanged(ImsSubConfigInfo config) {
        log("onImsSubConfigChanged()");
        ImsSubConfigDetails ret = StableAidl.toImsSubconfigDetails(config);
        mImsRadioIndication.onImsSubConfigChanged(ret);
    }

    @Override
    public void onParticipantStatusInfo(ParticipantStatusInfo participantStatusInfo) {
        log("onParticipantStatusInfo()");
        ParticipantStatusDetails ret = StableAidl.toParticipantStatus(participantStatusInfo);
        mImsRadioIndication.onParticipantStatusInfo(ret);
    }

    @Override
    public void onRegistrationBlockStatus(RegistrationBlockStatusInfo blockStatusInfo) {
        log("onRegistrationBlockStatus()");
        org.codeaurora.ims.RegistrationBlockStatusInfo ret = StableAidl.toRegistrationBlockStatus(
                blockStatusInfo);
        mImsRadioIndication.onRegistrationBlockStatus(ret);
    }

    @Override
    public void onRttMessageReceived(String message) {
        log("onRttMessageReceived()");
        mImsRadioIndication.onRttMessageReceived(message);
    }

    @Override
    public void onVoWiFiCallQuality(int voWiFiCallQuality) {
        log("onVoWiFiCallQuality()");
        int[] ret = StableAidl.toVoWiFiQuality(voWiFiCallQuality);
        mImsRadioIndication.onVoWiFiCallQuality(ret);
    }

    @Override
    public void onSupplementaryServiceIndication(StkCcUnsolSsResult ss) {
        log("onSupplementaryServiceIndication()");
        ImsSsData ssData = StableAidl.toImsSsData(ss);
        mImsRadioIndication.onSupplementaryServiceIndication(ssData);
    }

    @Override
    public void onSmsSendStatusReport(SmsSendStatusReport smsStatusReport) {
        log("onSmsSendStatusReport()");
        if (smsStatusReport.pdu == null) {
            loge("smsStatusReport.pdu is null");
            return;
        }
        StatusReport smsReport = StableAidl.toStatusReport(smsStatusReport);
        mImsRadioIndication.onImsSmsStatusReport(smsReport);
    }

    @Override
    public void onIncomingSms(vendor.qti.hardware.radio.ims.IncomingSms imsSms) {
        log("onIncomingSms()");
        if (imsSms.pdu == null || imsSms.format == null) {
            loge("pdu or format is null");
            return;
        }
        IncomingSms newSms = StableAidl.toIncomingSms(imsSms);
        mImsRadioIndication.onIncomingImsSms(newSms);
    }

    @Override
    public void onVopsChanged(boolean isVopsEnabled) {
        log("onVopsChanged()");
        mImsRadioIndication.onVopsChanged(isVopsEnabled);
    }

    @Override
    public void onVoiceInfoChanged(int voiceInfo) {
        log("onVoiceInfoChanged()");
        mImsRadioIndication.onVoiceInfoChanged(StableAidl.toVoiceInfo(voiceInfo));
    }

    @Override
    public void onMultiIdentityRegistrationStatusChange(MultiIdentityLineInfo[] info) {
        log("onMultiIdentityRegistrationStatusChange()");
        ArrayList<org.codeaurora.ims.MultiIdentityLineInfo> linesInfo =
                StableAidl.toMultiIdentityLineInfoList(info);
        mImsRadioIndication.onMultiIdentityRegistrationStatusChange(linesInfo);
    }

    @Override
    public void onMultiIdentityInfoPending() {
        log("onMultiIdentityInfoPending()");
        mImsRadioIndication.onMultiIdentityInfoPending();
    }

    @Override
    public void onModemSupportsWfcRoamingModeConfiguration(
            boolean wfcRoamingConfigurationSupport) {
        log("onModemSupportsWfcRoamingModeConfiguration()");
        mImsRadioIndication.onModemSupportsWfcRoamingModeConfiguration(
                wfcRoamingConfigurationSupport);
    }

    @Override
    public void onUssdMessageFailed(int type, SipErrorInfo errorDetails) {
        log("onUssdMessageFailed()");
        if(type == UssdModeType.INVALID) {
            loge("UssMode type is invalid");
            return;
        }
        UssdInfo ussdInfo = StableAidl.toUssdInfo(type, "", errorDetails);
        mImsRadioIndication.onUssdMessageFailed(ussdInfo);
    }

    @Override
    public void onUssdReceived(int type, String msg, SipErrorInfo errorDetails) {
        log("onUssdReceived()");
        UssdInfo ussdInfo = StableAidl.toUssdInfo(type, msg, errorDetails);
        mImsRadioIndication.onUssdReceived(ussdInfo);
    }

    @Override
    public void onCallComposerInfoAvailable(CallComposerInfo info) {
        mImsRadioIndication.onCallComposerInfoAvailable(info.callId,
                StableAidl.toCallComposerInfo(info));
    }

    @Override
    public void onIncomingCallComposerCallAutoRejected(
            CallComposerAutoRejectionInfo autoRejectionInfo) {
        log("onIncomingCallComposerCallAutoRejected()");

        if (autoRejectionInfo == null || autoRejectionInfo.autoCallRejectionInfo == null) {
            loge("onIncomingCallComposerCallAutoRejected: rejectInfo is null. Returning");
            return;
        }

        if (autoRejectionInfo.callComposerInfo == null) {
            log("onIncomingCallComposerCallAutoRejected: callComposerInfo is null");
            onIncomingCallAutoRejected(autoRejectionInfo.autoCallRejectionInfo);
            return;
        }

        DriverCallIms dc = StableAidl.toDriverCallIms(autoRejectionInfo);
        mImsRadioIndication.onIncomingCallAutoRejected(dc);
    }

    @Override
    public void onIncomingCallAutoRejected(AutoCallRejectionInfo autoCallRejectionInfo) {
        log("onIncomingCallAutoRejected()");

        if (autoCallRejectionInfo == null) {
            loge("onIncomingCallAutoRejected: rejectInfo is null. Returning");
            return;
        }

        DriverCallIms dc = StableAidl.toDriverCallIms(autoCallRejectionInfo);
        logv("onIncomingCallAutoRejected :: Call auto rejected from : " +
                autoCallRejectionInfo.number);

        mImsRadioIndication.onIncomingCallAutoRejected(dc);
    }

    @Override
    public void onRetrievingGeoLocationDataStatus(int geoLocationDataStatus) {
        log("onRetrievingGeoLocationDataStatus()");
        if(geoLocationDataStatus == GeoLocationDataStatus.INVALID) {
            loge("onRetrievingGeoLocationDataStatus: GeoLocationDataStatus is invalid. Returning");
            return;
        }
        int ret = StableAidl.toGeoLocationStatus(geoLocationDataStatus);
        mImsRadioIndication.onRetrievingGeoLocationDataStatus(ret);
    }

    @Override
    public void onSipDtmfReceived(String configCode) {
        log("onSipDtmfReceived()");
        mImsRadioIndication.onSipDtmfReceived(configCode);
    }

    @Override
    public void onServiceDomainChanged(int domain) {
        log("onServiceDomainChanged()");
        if (domain == SystemServiceDomain.INVALID) {
            loge("SystemServiceDomain is invalid");
            return;
        }
        mImsRadioIndication.onServiceDomainChanged(StableAidl.toServiceDomain(domain));
    }

    @Override
    public void onSmsCallBackModeChanged(int mode) {
        log("onSmsCallBackModeChanged()");
        if (mode == SmsCallBackMode.INVALID) {
            loge("SmsCallBackMode is invalid");
            return;
        }
        mImsRadioIndication.onSmsCallBackModeChanged(StableAidl.toSmsCallBackMode(mode));
    }

    @Override
    public void onConferenceCallStateCompleted() {
        log("onConferenceCallStateCompleted()");
        mImsRadioIndication.onConferenceCallStateCompleted();
    }

    @Override
    public void onIncomingDtmfStart(DtmfInfo dtmfInfo) {
        log("onIncomingDtmfStart()");
        mImsRadioIndication.onIncomingDtmfStart(dtmfInfo.callId, dtmfInfo.dtmf);
    }

    @Override
    public void onIncomingDtmfStop(DtmfInfo dtmfInfo) {
        log("onIncomingDtmfStop()");
        mImsRadioIndication.onIncomingDtmfStop(dtmfInfo.callId, dtmfInfo.dtmf);
    }

    @Override
    public void onMultiSimVoiceCapabilityChanged(int voiceCapability) {
        log("onMultiSimVoiceCapabilityChanged()");
        mImsRadioIndication.onMultiSimVoiceCapabilityChanged(StableAidl.toMultiSimVoiceCapability(
                voiceCapability));
    }

    @Override
    public void onPreAlertingCallInfoAvailable(PreAlertingCallInfo info) {
        log("onPreAlertingCallInfoAvailable()");

        if (info.callComposerInfo == null && info.ecnamInfo == null && !info.isDcCall) {
            log("onPreAlertingCallInfoAvailable: callComposerInfo and ecnamInfo is null"
                   + " and isDcCall is false ");
            return;
        }
        mImsRadioIndication.onPreAlertingCallInfoAvailable(StableAidl.toPreAlertingInfo(info));
    }

    @Override
    public void onIncomingCallAutoRejected2(AutoCallRejectionInfo2 autoCallRejectionInfo) {
        log("onIncomingCallAutoRejected2()");

        if (autoCallRejectionInfo == null || autoCallRejectionInfo.autoCallRejectionInfo == null) {
            loge("onIncomingCallAutoRejected2: rejectInfo is null. Returning");
            return;
        }

        if (autoCallRejectionInfo.callComposerInfo == null &&
                autoCallRejectionInfo.ecnamInfo == null &&
                !autoCallRejectionInfo.isDcCall &&
                (autoCallRejectionInfo.callReason == null ||
                autoCallRejectionInfo.callReason.isEmpty())) {
            log("onIncomingCallAutoRejected2: callComposerInfo and ecnamInfo is null");
            onIncomingCallAutoRejected(autoCallRejectionInfo.autoCallRejectionInfo);
            return;
        }

        DriverCallIms dc = StableAidl.toDriverCallIms(autoCallRejectionInfo);
        mImsRadioIndication.onIncomingCallAutoRejected(dc);
    }

    @Override
    public void onCiWlanNotification(int type) {
        log("onCiWlanNotification()");
        mImsRadioIndication.onCiWlanNotification(StableAidl.toCiWlanNotification(type));
    }

    @Override
    public void onSrtpEncryptionStatusChanged(SrtpEncryptionInfo info) {
        log("onStrpStatusIndication()");
        mImsRadioIndication.onSrtpEncryptionInfo(info.callId, info.categories);
    }
}
