/*
  * Copyright (c) 2015, 2017-2021 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
  *
  * Not a Contribution.
  *
  * Copyright (C) 2006 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */

package org.codeaurora.ims;

import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;

import com.qualcomm.ims.utils.Log;
import java.util.ArrayList;
import java.util.List;

import org.codeaurora.ims.ImsPhoneCommandsInterface.RadioState;
import org.codeaurora.ims.sms.IncomingSms;
import org.codeaurora.ims.sms.StatusReport;

import vendor.qti.hardware.radio.ims.V1_0.*;
import vendor.qti.hardware.radio.ims.V1_3.AutoCallRejectionInfo;
import vendor.qti.hardware.radio.ims.V1_4.MultiIdentityLineInfoHal;
import vendor.qti.hardware.radio.ims.V1_2.ImsSmsSendStatusReport;
import vendor.qti.hardware.radio.ims.V1_2.IncomingImsSms;

/* This class handles HIDL indications, converts objects from HIDL->Telephony java type
 * and forwards the response to ImsSenderRxr
 */

class ImsRadioIndicationHidl extends vendor.qti.hardware.radio.ims.V1_9.IImsRadioIndication.Stub {

    private IImsRadioIndication mImsRadioIndication;

    public ImsRadioIndicationHidl(IImsRadioIndication indication) {
        mImsRadioIndication = indication;
    }

    @Override
    public void onCallStateChanged_1_9(
            ArrayList<vendor.qti.hardware.radio.ims.V1_9.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_9()");

        if (callList == null) {
            Log.e(this, "Call list is null.");
            return;
        }

        ArrayList<DriverCallIms> response = new ArrayList<DriverCallIms>();
        int numOfCalls = callList.size();

        for (int i = 0; i < numOfCalls; ++i) {
            DriverCallIms dc = ImsRadioUtils.buildDriverCallImsFromHal(callList.get(i));
            response.add(dc);
        }
        mImsRadioIndication.onCallStateChanged(response);
    }

    @Override
    public void onCallStateChanged_1_8(
            ArrayList<vendor.qti.hardware.radio.ims.V1_8.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_8()");
        onCallStateChanged_1_9(ImsRadioUtilsV19.migrateCallListFrom(callList));
    }

    @Override
    public void onCallStateChanged_1_7(
            ArrayList<vendor.qti.hardware.radio.ims.V1_7.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_7()");
        onCallStateChanged_1_8(ImsRadioUtilsV18.migrateCallListFrom(callList));
    }

    @Override
    public void onCallStateChanged_1_6(
            ArrayList<vendor.qti.hardware.radio.ims.V1_6.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_6()");
        onCallStateChanged_1_7(ImsRadioUtilsV17.migrateCallListFrom(callList));
    }

    @Override
    public void onCallStateChanged_1_5(
            ArrayList<vendor.qti.hardware.radio.ims.V1_5.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_5()");
        onCallStateChanged_1_6(ImsRadioUtilsV16.migrateCallListFrom(callList));
    }

    @Override
    public void onCallStateChanged_1_4(ArrayList<
            vendor.qti.hardware.radio.ims.V1_4.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_4()");
        onCallStateChanged_1_5(ImsRadioUtilsV15.migrateCallListFrom(callList));
    }

    @Override
    public void onCallStateChanged_1_3(ArrayList<
            vendor.qti.hardware.radio.ims.V1_3.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_3()");
        onCallStateChanged_1_4(ImsRadioUtilsV14.migrateCallListFromV13(callList));
    }

    @Override
    public void onCallStateChanged_1_2(ArrayList<
            vendor.qti.hardware.radio.ims.V1_2.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_2()");
        onCallStateChanged_1_3(ImsRadioUtilsV13.migrateCallListFromV12(callList));
    }

    @Override
    public void onCallStateChanged_1_1(ArrayList<
            vendor.qti.hardware.radio.ims.V1_1.CallInfo> callList) {
        Log.i(this, "onCallStateChanged_1_1()");
        onCallStateChanged_1_2(ImsRadioUtilsV12.migrateCallListFromV11(callList));
    }

    @Override
    public void onCallStateChanged(ArrayList<CallInfo> callList) {
        Log.i(this, "onCallStateChanged()");
        onCallStateChanged_1_1(ImsRadioUtils.migrateCallListToV11(callList));
    }

    @Override
    public void onImsSmsStatusReport(ImsSmsSendStatusReport smsStatusReport) {
        Log.i(this, "onImsSmsStatusReport()");

        if (smsStatusReport.pdu != null) {
            StatusReport smsReport = ImsRadioUtils.statusReportfromHidl(smsStatusReport);
            mImsRadioIndication.onImsSmsStatusReport(smsReport);
        }
    }

    @Override
    public void onIncomingImsSms(IncomingImsSms imsSms) {
        Log.i(this, "onIncomingImsSms()");

        if (imsSms.pdu != null && imsSms.format != null) {
            IncomingSms newSms = ImsRadioUtils.incomingSmsfromHidl(imsSms);
            mImsRadioIndication.onIncomingImsSms(newSms);
        }
    }

    @Override
    public void onRing() {
        Log.i(this, "onRing()");
        mImsRadioIndication.onRing();
    }

    @Override
    public void onRingbackTone(int tone) {
        int[] response = new int[1];

        response[0] = ImsRadioUtils.ringbackToneFromHal(tone);
        Log.i(this, "onRingbackTone() response: " + response[0]);

        boolean playtone = false;
        if (response != null) playtone = (response[0] == 1);

        mImsRadioIndication.onRingbackTone(playtone);
    }


    @Override
    public void onRegistrationChanged(RegistrationInfo registration) {
        Log.i(this, "onRegistrationChanged()");
        onRegistrationChanged_1_6(ImsRadioUtilsV16.migrateRegistrationInfo(registration));
    }

    @Override
    public void onRegistrationChanged_1_6(
            vendor.qti.hardware.radio.ims.V1_6.RegistrationInfo registration) {
        Log.i(this, "onRegistrationChanged_1_6()");
        ImsRegistrationInfo regMessage = ImsRadioUtils.registrationFromHal(registration);
        mImsRadioIndication.onRegistrationChanged(regMessage);
    }

    @Override
    public void onHandover(HandoverInfo inHandover) {
        Log.i(this, "onHandover()");
        onHandover_1_6(ImsRadioUtilsV16.migrateHandoverInfo(inHandover));
    }

    @Override
    public void onHandover_1_6(
            vendor.qti.hardware.radio.ims.V1_6.HandoverInfo inHandover) {
        Log.i(this, "onHandover_1_6()");
        HoInfo outHandover = ImsRadioUtils.handoverFromHal(inHandover);
        mImsRadioIndication.onHandover(outHandover);
    }

    @Override
    public void onServiceStatusChanged(ArrayList<ServiceStatusInfo> srvStatusList) {
        Log.i(this, "onServiceStatusChanged()");
        onServiceStatusChanged_1_6(ImsRadioUtilsV16.migrateServiceStatusInfo(srvStatusList));
    }

    @Override
    public void onServiceStatusChanged_1_6(
            ArrayList<vendor.qti.hardware.radio.ims.V1_6.ServiceStatusInfo> srvStatusList) {
        Log.i(this, "onServiceStatusChanged_1_6()");
        ArrayList<ServiceStatus> ret = ImsRadioUtils.handleSrvStatus(srvStatusList);
        mImsRadioIndication.onServiceStatusChanged(ret);
    }

    @Override
    public void onRadioStateChanged(int radioState) {
        Log.i(this, "onRadioStateChanged()");

        switch (radioState) {
            case vendor.qti.hardware.radio.ims.V1_0.RadioState.RADIO_STATE_OFF:
                mImsRadioIndication.onRadioStateChanged(RadioState.RADIO_OFF);
                break;
            case vendor.qti.hardware.radio.ims.V1_0.RadioState.RADIO_STATE_UNAVAILABLE:
                mImsRadioIndication.onRadioStateChanged(RadioState.RADIO_UNAVAILABLE);
                break;
            case vendor.qti.hardware.radio.ims.V1_0.RadioState.RADIO_STATE_ON:
                mImsRadioIndication.onRadioStateChanged(RadioState.RADIO_ON);
                break;
            default:
                Log.e(this, "onRadioStateChanged: Invalid Radio State Change");
                break;
        }
    }

    @Override
    public void onEnterEmergencyCallBackMode() {
        Log.i(this, "onEnterEmergencyCallBackMode()");
        mImsRadioIndication.onEnterEmergencyCallBackMode();
    }

    @Override
    public void onExitEmergencyCallBackMode() {
        Log.i(this, "onExitEmergencyCallBackMode()");
        mImsRadioIndication.onExitEmergencyCallBackMode();
    }

    @Override
    public void onTtyNotification(TtyInfo ttyInfo) {
        Log.i(this, "onTtyNotification()");
        if (ttyInfo == null) {
            Log.e(this, "onTtyNotification: ttyInfo is null");
            return;
        }
        mImsRadioIndication.onTtyNotification(new int[]{ImsRadioUtils.ttyModeFromHal(
                ttyInfo.mode)});
    }

    @Override
    public void onRefreshConferenceInfo(ConferenceInfo conferenceInfo) {
        Log.i(this, "onRefreshConferenceInfo()");
        if (conferenceInfo == null) {
            Log.e(this, "onRefreshConferenceInfo: Conferenceinfo is null");
            return;
        }
        ConfInfo info = new ConfInfo();

        if (conferenceInfo.confInfoUri != null && !conferenceInfo.confInfoUri.isEmpty()) {
            info.setConfInfoUri(conferenceInfo.confInfoUri);
            Log.v(this, "onRefreshConferenceInfo: confUri = " +
                    conferenceInfo.confInfoUri);
        }

        if (conferenceInfo.conferenceCallState != ConferenceCallState.INVALID) {
            info.setConfCallState(ImsRadioUtils.conferenceCallStateFromHal(
                    conferenceInfo.conferenceCallState));
        }
        Log.v(this, "onRefreshConferenceInfo: conferenceCallState = " +
                conferenceInfo.conferenceCallState);
        mImsRadioIndication.onRefreshConferenceInfo(info);
    }

    @Override
    public void onRefreshViceInfo(ViceInfo viceInfo) {
        Log.i(this, "onRefreshViceInfo()");
        if (viceInfo == null || viceInfo.viceInfoUri == null) {
            Log.e(this, "onRefreshViceInfo: Viceinfo or viceinfouri is null");
            return;
        }
        ViceUriInfo info = new ViceUriInfo(viceInfo.viceInfoUri);
        // Notify the registrants in both the following cases :
        // 1) Valid Vice XML present
        // 2) Vice XML with 0 length. For ex: Modem triggers indication with 0 length
        // when APM toggle happens. Notify so that UI gets cleared
        Log.v(this, "onRefreshViceInfo: viceUri = " + viceInfo.viceInfoUri);
        mImsRadioIndication.onRefreshViceInfo(info);
    }

    /**
     * MessageId.UNSOL_MODIFY_CALL
     *
     * @param CallModifyInfo call modify info.
     */
    @Override
    public void onModifyCall_1_9(vendor.qti.hardware.radio.ims.V1_9.CallModifyInfo callModifyInfo) {
        Log.i(this, "onModifyCall_1_9()");
        if (callModifyInfo == null) {
            Log.e(this, "onModifyCall: callModifyInfo is null");
            return;
        }
        CallModify ret = ImsRadioUtils.callModifyFromHal(callModifyInfo);
        mImsRadioIndication.onModifyCall(ret);
    }

    @Override
    public void onModifyCall(CallModifyInfo callModifyInfo) {
        Log.i(this, "onModifyCall()");
        onModifyCall_1_9(ImsRadioUtils.migrateCallModifyInfoToV19(callModifyInfo));
    }

    @Override
    public void onSuppServiceNotification(SuppServiceNotification
            suppServiceNotification) {
        Log.i(this, "onSuppServiceNotification()");
        SuppNotifyInfo suppNotiyInfo = ImsRadioUtils.
                suppServiceNotificationFromHal(suppServiceNotification);
        mImsRadioIndication.onSuppServiceNotification(suppNotiyInfo);
    }

    @Override
    public void onMessageWaiting(MessageWaitingIndication messageWaitingIndication) {
        Log.i(this, "onMessageWaiting()");
        Mwi mwi = ImsRadioUtils.messageWaitingIndicationFromHal(messageWaitingIndication);
        mImsRadioIndication.onMessageWaiting(mwi);
    }

    @Override
    public void onGeolocationInfoRequested(double lat, double lon) {
        Log.i(this, "onGeolocationInfoRequested()");
        GeoLocationInfo ret =
                ImsRadioUtils.geolocationIndicationFromHal(lat, lon);
        mImsRadioIndication.onGeolocationInfoRequested(ret);

    }

    @Override
    public void onIncomingCallComposerCallAutoRejected(
            vendor.qti.hardware.radio.ims.V1_6.AutoCallRejectionInfo rejectInfo,
            vendor.qti.hardware.radio.ims.V1_6.CallComposerInfo callComposerInfo) {
        Log.i(this, "onIncomingCallComposerCallAutoRejected()");
        if (callComposerInfo == null) {
            Log.i(this, "onIncomingCallComposerCallAutoRejected: callComposerInfo is null");
            onIncomingCallAutoRejected_1_6(rejectInfo);
        } else if (rejectInfo == null) {
            Log.e(this, "onIncomingCallComposerCallAutoRejected: rejectInfo is null ");
            return;
        }
        DriverCallIms dc = ImsRadioUtilsV16.toDriverCallIms(rejectInfo, callComposerInfo);
        mImsRadioIndication.onIncomingCallAutoRejected(dc);
    }

    @Override
    public void onIncomingCallAutoRejected_1_6(
            vendor.qti.hardware.radio.ims.V1_6.AutoCallRejectionInfo rejectInfo) {
        Log.i(this, "onIncomingCallAutoRejected_1_6()");

        if (rejectInfo == null) {
            Log.e(this, "onIncomingCallAutoRejected: rejectInfo is null. Returning");
            return;
        }

        DriverCallIms dc = ImsRadioUtilsV16.toDriverCallIms(rejectInfo, null);
        Log.v(this, "onIncomingCallAutoRejected :: Call auto rejected from : " + rejectInfo.number);

        mImsRadioIndication.onIncomingCallAutoRejected(dc);
    }

    @Override
    public void onIncomingCallAutoRejected_1_5(
            vendor.qti.hardware.radio.ims.V1_5.AutoCallRejectionInfo rejectInfo) {
        onIncomingCallAutoRejected_1_6(ImsRadioUtilsV16.migrateAutoCallRejectionInfoFrom(
                rejectInfo));
    }

    @Override
    public void onIncomingCallAutoRejected(AutoCallRejectionInfo rejectInfo) {
        onIncomingCallAutoRejected_1_5(ImsRadioUtilsV15.migrateAutoCallRejectionInfoFrom(
                rejectInfo));
    }

    @Override
    public void onImsSubConfigChanged(ImsSubConfigInfo config) {
        Log.i(this, "onImsSubConfigChanged()");
        ImsSubConfigDetails ret = ImsRadioUtils.imsSubconfigFromHal(config);
        mImsRadioIndication.onImsSubConfigChanged(ret);
    }

    @Override
    public void onParticipantStatusInfo(ParticipantStatusInfo participantStatusInfo) {
        Log.i(this, "onParticipantStatusInfo()");
        ParticipantStatusDetails ret =
                ImsRadioUtils.participantStatusFromHal(participantStatusInfo);
        mImsRadioIndication.onParticipantStatusInfo(ret);
    }

    @Override
    public void onRegistrationBlockStatus(boolean hasBlockStatusOnWwan, BlockStatus
            blockStatusOnWwan, boolean hasBlockStatusOnWlan,
            BlockStatus blockStatusOnWlan) {
        Log.i(this, "onRegistrationBlockStatus()");
        RegistrationBlockStatusInfo ret =
                ImsRadioUtils.registrationBlockStatusFromHal(hasBlockStatusOnWwan,
                blockStatusOnWwan, hasBlockStatusOnWlan, blockStatusOnWlan);
        mImsRadioIndication.onRegistrationBlockStatus(ret);
    }

    @Override
    public void onRttMessageReceived(String msg) {
        Log.i(this, "onRttMessageReceived()");
        mImsRadioIndication.onRttMessageReceived(msg);
    }

    @Override
    public void onVoiceInfoChanged(int voiceInfo) {
        Log.v(this, "onVoiceInfoChanged: VoiceInfo = " + voiceInfo);
        mImsRadioIndication.onVoiceInfoChanged(ImsRadioUtils.voiceInfoTypeFromHal(voiceInfo));
    }

    @Override
    public void onVoWiFiCallQuality(int voWiFiCallQuality) {
        Log.i(this, "onVoWiFiCallQuality()");
        int[] ret = ImsRadioUtils.voWiFiCallQualityFromHal(voWiFiCallQuality);
        mImsRadioIndication.onVoWiFiCallQuality(ret);
    }

    @Override
    public void onSupplementaryServiceIndication(StkCcUnsolSsResult ss) {
        Log.i(this, "onSupplementaryServiceIndication()");
        ImsSsData.Builder ssDataBuilder  = new ImsSsData.Builder(
                ImsRadioUtils.serviceTypeFromRILServiceType(ss.serviceType),
                ImsRadioUtils.requestTypeFromRILRequestType(ss.requestType),
                ImsRadioUtils.teleserviceTypeFromRILTeleserviceType(ss.teleserviceType),
                ss.serviceClass, ss.result);
        ImsSsData ssData = ssDataBuilder.build();

        int num;
        if (ssData.isTypeCf() && ssData.isTypeInterrogation()) {
            List <ImsCallForwardInfo> cfInfo = new ArrayList<>();
            if (ss.cfData == null) {
                Log.d(this, "cfData is null, which is unexpected for: " +
                        ssData.getServiceType());
            } else {
                CfData cfData = ss.cfData.get(0);
                num = cfData.cfInfo.size();
                for (int i = 0; i < num; i++) {
                    vendor.qti.hardware.radio.ims.V1_0.CallForwardInfo rCfInfo = cfData.
                            cfInfo.get(i);
                    cfInfo.add(new ImsCallForwardInfo(ImsRadioUtils.
                            getCallForwardReasonFromSsData(rCfInfo.reason), rCfInfo.status,
                                rCfInfo.toa, rCfInfo.serviceClass, rCfInfo.number,
                                rCfInfo.timeSeconds));

                    Log.i(this, "[SS Data] CF Info " + i + " : " +  cfInfo.get(i));
                }
            }
            ssDataBuilder.setCallForwardingInfo(cfInfo);
        } else if (ssData.isTypeIcb() && ssData.isTypeInterrogation()) {
            List <ImsSsInfo> imsSsInfo = new ArrayList<>();
            if (ss.cbNumInfo == null) {
                Log.d(this, "cbNumInfo is null, which is unexpected for: " +
                            ssData.getServiceType());
            } else {
                num = ss.cbNumInfo.size();
                for (int i = 0; i < num; i++) {
                    ImsSsInfo.Builder imsSsInfoBuilder =
                            new ImsSsInfo.Builder(ss.cbNumInfo.get(i).status);

                    imsSsInfoBuilder.setIncomingCommunicationBarringNumber(
                            ss.cbNumInfo.get(i).number);
                    imsSsInfo.add(imsSsInfoBuilder.build());
                    Log.i(this, "[SS Data] ICB Info " + i + " : " +  imsSsInfo.get(i));
                }
            }
            ssDataBuilder.setSuppServiceInfo(imsSsInfo);
        } else {
            /** Handling for SS_CLIP/SS_CLIR/SS_COLP/SS_WAIT
                Currently, max size of the array sent is 2.
                Expected format for all except SS_CLIR is:
                status - ssInfo[0]
                provision status - (Optional) ssInfo[1]
             */
            List <ImsSsInfo> imsSsInfo = new ArrayList<>();
            if (ss.ssInfoData == null) {
                Log.d(this, "imsSsInfo is null, which is unexpected for: " +
                            ssData.getServiceType());
            } else {
                SsInfoData ssInfoData = ss.ssInfoData.get(0);
                num = ssInfoData.ssInfo.size();
                if (num > 0) {
                    ImsSsInfo.Builder imsSsInfoBuilder =
                            new ImsSsInfo.Builder(ssInfoData.ssInfo.get(0));
                    if (ssData.isTypeClir() && ssData.isTypeInterrogation()) {
                        // creating ImsSsInfoBuilder with first int as status not used
                        imsSsInfoBuilder.setClirOutgoingState(ssInfoData.ssInfo.get(0));
                        if (num > 1) {
                            imsSsInfoBuilder.setClirInterrogationStatus(ssInfoData.ssInfo.get(1));
                        }
                    } else if (num > 1) {
                        imsSsInfoBuilder.setProvisionStatus(ssInfoData.ssInfo.get(1));
                    }
                    imsSsInfo.add(imsSsInfoBuilder.build());
                }
            }
            ssDataBuilder.setSuppServiceInfo(imsSsInfo);
        }
        mImsRadioIndication.onSupplementaryServiceIndication(ssData);
    }

    @Override
    public void onVopsChanged(boolean isVopsEnabled) {
        Log.i(this, "onVopsChanged()");
        mImsRadioIndication.onVopsChanged(isVopsEnabled);
    }

    @Override
    public void onMultiIdentityRegistrationStatusChange(
            ArrayList<MultiIdentityLineInfoHal> lines) {
        Log.i(this, "onMultiIdentityRegistrationStatusChange()");
        ArrayList<MultiIdentityLineInfo> linesInfo = new ArrayList<>();
        for (MultiIdentityLineInfoHal line : lines) {
            MultiIdentityLineInfo lineInfo =  ImsRadioUtilsV14.
                    fromMultiIdentityLineInfoHal(line);
            linesInfo.add(lineInfo);
        }
        mImsRadioIndication.onMultiIdentityRegistrationStatusChange(linesInfo);
    }

    @Override
    public void onMultiIdentityInfoPending() {
        Log.i(this, "onMultiIdentityInfoPending()");
        mImsRadioIndication.onMultiIdentityInfoPending();
    }

    @Override
    public void onModemSupportsWfcRoamingModeConfiguration(
            boolean wfcRoamingConfigurationSupport) {
        Log.i(this, "wfcRoamingConfigurationSupport = " + wfcRoamingConfigurationSupport);
        mImsRadioIndication.onModemSupportsWfcRoamingModeConfiguration(
                wfcRoamingConfigurationSupport);
    }

    @Override
    public void onUssdMessageFailed(int type, SipErrorInfo errorDetails) {
        Log.i(this, "onUssdMessageFailed() type: " + type);
        UssdInfo ussdInfo = ImsRadioUtils.ussdInfoFromHal(type, "", errorDetails);
        mImsRadioIndication.onUssdMessageFailed(ussdInfo);
    }

    /**
     * Callback for getting the indication for USSD from network via the
     * IImsRadioIndication interface
     * @param type related to USSD mode type.
     * @param msg is received USSD message.
     * @param errorDetails related to SIP messaging from the network.
     *
     */
    @Override
    public void onUssdReceived(int type, String msg, SipErrorInfo errorDetails) {
        Log.i(this, "onUssdReceived() type: " + type);
        UssdInfo ussdInfo = ImsRadioUtils.ussdInfoFromHal(type, msg, errorDetails);
        mImsRadioIndication.onUssdReceived(ussdInfo);
    }

    /**
     * Callback for getting the indication for call composer elements available
     * during the pre-alerting call state
     * @param callComposerInfo received call composer elements
     */
    @Override
    public void onCallComposerInfoAvailable(
            vendor.qti.hardware.radio.ims.V1_6.CallComposerInfo callComposerInfo) {
        CallComposerInfo info = ImsRadioUtilsV16.buildCallComposerInfoFromHal(callComposerInfo);
        mImsRadioIndication.onCallComposerInfoAvailable(callComposerInfo.callId, info);
    }

    @Override
    public void onRetrievingGeoLocationDataStatus(int geoLocationDataStatus) {
        Log.i(this, "onRetrievingGeoLocationDataStatus()");
        int ret = ImsRadioUtilsV16.geoLocationDataStatusFromHal(geoLocationDataStatus);
        mImsRadioIndication.onRetrievingGeoLocationDataStatus(ret);
    }

    @Override
    public void onSipDtmfReceived(String configCode) {
        Log.i(this, "onSipDtmfReceived()");
        mImsRadioIndication.onSipDtmfReceived(configCode);
    }

    /**
     * Callback for getting the indication when modem sends NAS system info via the
     * IImsRadioIndication interface.
     * @param domain is the UE's service domain.
     */
    @Override
    public void onServiceDomainChanged(int domain) {
        Log.i(this, "onServiceDomainChanged()");
        mImsRadioIndication.onServiceDomainChanged(
                ImsRadioUtilsV18.serviceDomainFromHal(domain));
    }

    /**
     * Callback for getting the indication when modem enters/leaves the SMS
     * callback mode via the IImsRadioIndication interface.
     * Modem enters SCBM for certain carriers after an emergency SMS is sent by user.
     * @param mode is the state of modem when its in SCBM mode or not.
     */
    @Override
    public void onSmsCallBackModeChanged(int mode) {
        Log.i(this, "onSmsCallBackModeChanged() mode: " + mode);
        mImsRadioIndication.onSmsCallBackModeChanged(
                ImsRadioUtilsV18.scbmStatusFromHal(mode));
    }

    @Override
    public void onConferenceCallStateCompleted() {
        Log.i(this, "onConferenceCallStateCompleted()");
        mImsRadioIndication.onConferenceCallStateCompleted();
    }
}
