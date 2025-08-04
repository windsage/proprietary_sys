/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.graphics.Point;
import android.location.Address;
import android.net.Uri;
import android.os.RemoteException;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.telephony.ims.stub.ImsUtImplBase;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;

import com.qualcomm.ims.utils.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.codeaurora.ims.Feature;
import org.codeaurora.ims.ImsAnnotations.EmergencyCallRouting;
import org.codeaurora.ims.ImsAnnotations.EmergencyServiceCategories;
import org.codeaurora.ims.ImsPhoneCommandsInterface.RadioState;
import org.codeaurora.ims.RedialInfo;
import org.codeaurora.ims.sms.SmsResponse;
import org.codeaurora.ims.sms.StatusReport;
import org.codeaurora.ims.sms.IncomingSms;
import org.codeaurora.ims.utils.QtiImsExtUtils;

import vendor.qti.hardware.radio.ims.AddressInfo;
import vendor.qti.hardware.radio.ims.AnswerRequest;
import vendor.qti.hardware.radio.ims.AutoCallRejectionInfo;
import vendor.qti.hardware.radio.ims.AutoCallRejectionInfo2;
import vendor.qti.hardware.radio.ims.AcknowledgeSmsInfo;
import vendor.qti.hardware.radio.ims.AcknowledgeSmsReportInfo;
import vendor.qti.hardware.radio.ims.BlockReasonDetails;
import vendor.qti.hardware.radio.ims.BlockReasonType;
import vendor.qti.hardware.radio.ims.BlockStatus;
import vendor.qti.hardware.radio.ims.CallComposerAutoRejectionInfo;
import vendor.qti.hardware.radio.ims.CallComposerDialRequest;
import vendor.qti.hardware.radio.ims.CallDomain;
import vendor.qti.hardware.radio.ims.CallFailCause;
import vendor.qti.hardware.radio.ims.CallFailCauseResponse;
import vendor.qti.hardware.radio.ims.CallForwardInfo;
import vendor.qti.hardware.radio.ims.CallFwdTimerInfo;
import vendor.qti.hardware.radio.ims.CallInfo;
import vendor.qti.hardware.radio.ims.CallLocation;
import vendor.qti.hardware.radio.ims.CallModifyFailCause;
import vendor.qti.hardware.radio.ims.CallModifyInfo;
import vendor.qti.hardware.radio.ims.CallProgressInfoType;
import vendor.qti.hardware.radio.ims.CallState;
import vendor.qti.hardware.radio.ims.CallType;
import vendor.qti.hardware.radio.ims.CallWaitingInfo;
import vendor.qti.hardware.radio.ims.CbNumInfo;
import vendor.qti.hardware.radio.ims.CbNumListInfo;
import vendor.qti.hardware.radio.ims.CfData;
import vendor.qti.hardware.radio.ims.CiWlanNotificationInfo;
import vendor.qti.hardware.radio.ims.ClipProvisionStatus;
import vendor.qti.hardware.radio.ims.ClipStatus;
import vendor.qti.hardware.radio.ims.ClirInfo;
import vendor.qti.hardware.radio.ims.ClirMode;
import vendor.qti.hardware.radio.ims.Codec;
import vendor.qti.hardware.radio.ims.ColrInfo;
import vendor.qti.hardware.radio.ims.ComputedAudioQuality;
import vendor.qti.hardware.radio.ims.ConferenceAbortReason;
import vendor.qti.hardware.radio.ims.ConferenceAbortReasonInfo;
import vendor.qti.hardware.radio.ims.ConferenceCallState;
import vendor.qti.hardware.radio.ims.ConferenceInfo;
import vendor.qti.hardware.radio.ims.ConfigInfo;
import vendor.qti.hardware.radio.ims.ConfigItem;
import vendor.qti.hardware.radio.ims.ConfigFailureCause;
import vendor.qti.hardware.radio.ims.ConfParticipantOperation;
import vendor.qti.hardware.radio.ims.CrsType;
import vendor.qti.hardware.radio.ims.DeflectRequestInfo;
import vendor.qti.hardware.radio.ims.DialRequest;
import vendor.qti.hardware.radio.ims.DtmfInfo;
import vendor.qti.hardware.radio.ims.EctType;
import vendor.qti.hardware.radio.ims.EmergencyCallRoute;
import vendor.qti.hardware.radio.ims.EmergencyDialRequest;
import vendor.qti.hardware.radio.ims.EmergencyServiceCategory;
import vendor.qti.hardware.radio.ims.ExplicitCallTransferInfo;
import vendor.qti.hardware.radio.ims.ExtraType;
import vendor.qti.hardware.radio.ims.FacilityType;
import vendor.qti.hardware.radio.ims.GeoLocationDataStatus;
import vendor.qti.hardware.radio.ims.GeoLocationInfo;
import vendor.qti.hardware.radio.ims.HandoverInfo;
import vendor.qti.hardware.radio.ims.HandoverType;
import vendor.qti.hardware.radio.ims.HangupRequestInfo;
import vendor.qti.hardware.radio.ims.ImsSubConfigInfo;
import vendor.qti.hardware.radio.ims.IpPresentation;
import vendor.qti.hardware.radio.ims.MediaConfig;
import vendor.qti.hardware.radio.ims.MessageDetails;
import vendor.qti.hardware.radio.ims.MessageSummary;
import vendor.qti.hardware.radio.ims.MessageWaitingIndication;
import vendor.qti.hardware.radio.ims.MwiMessagePriority;
import vendor.qti.hardware.radio.ims.MwiMessageType;
import vendor.qti.hardware.radio.ims.NotificationType;
import vendor.qti.hardware.radio.ims.ParticipantStatusInfo;
import vendor.qti.hardware.radio.ims.RegFailureReasonType;
import vendor.qti.hardware.radio.ims.RadioTechType;
import vendor.qti.hardware.radio.ims.RegistrationInfo;
import vendor.qti.hardware.radio.ims.RegState;
import vendor.qti.hardware.radio.ims.Result;
import vendor.qti.hardware.radio.ims.RttMode;
import vendor.qti.hardware.radio.ims.Size;
import vendor.qti.hardware.radio.ims.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.ServiceClassProvisionStatus;
import vendor.qti.hardware.radio.ims.ServiceClassStatus;
import vendor.qti.hardware.radio.ims.SipErrorInfo;
import vendor.qti.hardware.radio.ims.SmsCallBackMode;
import vendor.qti.hardware.radio.ims.SmsDeliverStatus;
import vendor.qti.hardware.radio.ims.SmsReportStatus;
import vendor.qti.hardware.radio.ims.SmsSendFailureReason;
import vendor.qti.hardware.radio.ims.SmsSendRequest;
import vendor.qti.hardware.radio.ims.SmsSendStatus;
import vendor.qti.hardware.radio.ims.SmsSendStatusReport;
import vendor.qti.hardware.radio.ims.SsInfoData;
import vendor.qti.hardware.radio.ims.SsRequestType;
import vendor.qti.hardware.radio.ims.SsServiceType;
import vendor.qti.hardware.radio.ims.SsTeleserviceType;
import vendor.qti.hardware.radio.ims.StatusForAccessTech;
import vendor.qti.hardware.radio.ims.StatusType;
import vendor.qti.hardware.radio.ims.StkCcUnsolSsResult;
import vendor.qti.hardware.radio.ims.SuppServiceNotification;
import vendor.qti.hardware.radio.ims.SuppServiceStatus;
import vendor.qti.hardware.radio.ims.SuppServiceStatusRequest;
import vendor.qti.hardware.radio.ims.SuppSvcOperationType;
import vendor.qti.hardware.radio.ims.SystemServiceDomain;
import vendor.qti.hardware.radio.ims.TirMode;
import vendor.qti.hardware.radio.ims.ToneOperation;
import vendor.qti.hardware.radio.ims.TtyInfo;
import vendor.qti.hardware.radio.ims.TtyMode;
import vendor.qti.hardware.radio.ims.UssdModeType;
import vendor.qti.hardware.radio.ims.VerificationStatus;
import vendor.qti.hardware.radio.ims.ViceInfo;
import vendor.qti.hardware.radio.ims.VoiceInfo;
import vendor.qti.hardware.radio.ims.VoWiFiCallQuality;

/* Utility class that converts types to/from AIDL types */
public class StableAidl {

    private static String TAG = "StableAidl";
    private static final int INVALID_CONNID = 0;

    private StableAidl() {}

    public static SmsSendRequest fromSmsRequest(int messageRef, String format, String smsc,
            boolean shallRetry, byte[] pdu) {
        SmsSendRequest smsRequest = new SmsSendRequest();
        smsRequest.messageRef = messageRef;
        smsRequest.format = format;
        smsRequest.smsc = smsc == null ? "" : smsc;
        smsRequest.shallRetry = shallRetry;
        smsRequest.pdu = new byte[pdu.length];
        for (int i = 0; i < smsRequest.pdu.length; i++) {
            smsRequest.pdu[i] = pdu[i];
        }
        return smsRequest;
    }

    public static SmsResponse toSmsResponse(int messageRef,
            int smsStatusResult, int failureCause, int networkErrorCode, int rat) {
        int statusResult = toSmsSendStatus(smsStatusResult);
        int reason = StableAidlErrorCode.toSmsManagerError(failureCause);
        int radioTech = toRadioTech(rat);
        return new SmsResponse(messageRef, statusResult, reason, networkErrorCode, radioTech);
    }

    private static int toSmsSendStatus(int hidlResult) {
        switch (hidlResult) {
            case SmsSendStatus.OK:
                return ImsSmsImplBase.SEND_STATUS_OK;
            case SmsSendStatus.ERROR:
                return ImsSmsImplBase.SEND_STATUS_ERROR;
            case SmsSendStatus.ERROR_RETRY:
                return ImsSmsImplBase.SEND_STATUS_ERROR_RETRY;
            case SmsSendStatus.ERROR_FALLBACK:
                return ImsSmsImplBase.SEND_STATUS_ERROR_FALLBACK;
            default:
                return ImsSmsImplBase.SEND_STATUS_ERROR;
        }
    }

    public static ServiceStatusInfo[] fromServiceStatusInfoList(
            List<CapabilityStatus> capabilityStatusList, int restrictCause) {
        ServiceStatusInfo[] serviceStatusInfoList = new ServiceStatusInfo[
                capabilityStatusList.size()];
        for (int i = 0; i < capabilityStatusList.size(); ++i) {
            CapabilityStatus capabilityStatus = capabilityStatusList.get(i);
            ServiceStatusInfo serviceStatusInfo = fromServiceStatusInfo(
                    mapCapabilityToSrvType(capabilityStatus.getCapability()),
                    mapRegistrationTechToRadioTech(capabilityStatus.getRadioTech()),
                    mapValueToServiceStatus(capabilityStatus.getStatus()),
                    restrictCause);
            serviceStatusInfoList[i] = serviceStatusInfo;
        }
        return serviceStatusInfoList;
    }

    private static ServiceStatusInfo fromServiceStatusInfo(int srvType, int rat, int enabled,
             int restrictCause) {
        StatusForAccessTech statusForAccessTech = new StatusForAccessTech();
        statusForAccessTech.networkMode = fromRadioTech(rat);
        statusForAccessTech.status = fromStatusType(enabled);
        statusForAccessTech.restrictCause = restrictCause;

        //Initializing RegistrationInfo
        statusForAccessTech.hasRegistration = false;
        statusForAccessTech.registration = new RegistrationInfo();
        statusForAccessTech.registration.errorMessage = "";
        statusForAccessTech.registration.pAssociatedUris = "";

        ServiceStatusInfo serviceStatusInfo = new ServiceStatusInfo();
        serviceStatusInfo.isValid = true;
        serviceStatusInfo.callType = fromCallType(srvType);
        serviceStatusInfo.accTechStatus = new StatusForAccessTech[1];
        serviceStatusInfo.accTechStatus[0] = statusForAccessTech;

        return serviceStatusInfo;
    }

    public static int mapCapabilityToSrvType (int capability) {
        switch (capability) {
            case MmTelCapabilities.CAPABILITY_TYPE_VOICE:
                return CallDetails.CALL_TYPE_VOICE;
            case MmTelCapabilities.CAPABILITY_TYPE_VIDEO:
                return CallDetails.CALL_TYPE_VT;
            case MmTelCapabilities.CAPABILITY_TYPE_UT:
                return CallDetails.CALL_TYPE_UT;
            case MmTelCapabilities.CAPABILITY_TYPE_SMS:
                return CallDetails.CALL_TYPE_SMS;
            default:
                return CallDetails.CALL_TYPE_UNKNOWN;
        }
    }

    public static int mapRegistrationTechToRadioTech(int rat) {
        switch (rat) {
            case ImsRegistrationImplBase.REGISTRATION_TECH_LTE:
                return RadioTech.RADIO_TECH_LTE;
            case ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN:
                return RadioTech.RADIO_TECH_IWLAN;
            case ImsRegistrationImplBase.REGISTRATION_TECH_NR:
                return RadioTech.RADIO_TECH_NR5G;
            case ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM:
                return RadioTech.RADIO_TECH_C_IWLAN;
            default:
                return RadioTech.RADIO_TECH_UNKNOWN;
        }
    }

    public static int mapValueToServiceStatus(int value) {
        switch (value) {
            case ProvisioningManager.PROVISIONING_VALUE_ENABLED:
                return ServiceStatus.STATUS_ENABLED;
            case ProvisioningManager.PROVISIONING_VALUE_DISABLED:
                return ServiceStatus.STATUS_DISABLED;
            default:
                return ServiceStatus.STATUS_DISABLED;
        }
    }

    private static int fromClirMode(int clirMode) {
        switch (clirMode) {
            case ImsSsInfo.CLIR_OUTGOING_DEFAULT:
                return ClirMode.DEFAULT;
            case ImsSsInfo.CLIR_OUTGOING_INVOCATION:
                return ClirMode.INVOCATION;
            case ImsSsInfo.CLIR_OUTGOING_SUPPRESSION:
                return ClirMode.SUPRESSION;
            default:
                return ClirMode.DEFAULT;
        }
    }

    public static DialRequest fromDialRequest(String address, int clirMode, CallDetails callDetails,
            boolean isEncrypted, RedialInfo redialInfo) {
        DialRequest dialRequest = new DialRequest();
        if (address != null) {
            dialRequest.address = address;
        } else {
            dialRequest.address = "";
        }

        dialRequest.clirMode = fromClirMode(clirMode);

        dialRequest.callDetails = fromCallDetails(callDetails);

        dialRequest.isConferenceUri = hasConferenceUri(callDetails);

        dialRequest.isCallPull = (callDetails != null) ? callDetails.getCallPull() : false;

        dialRequest.isEncrypted = isEncrypted;

        final MultiIdentityLineInfo multiIdentityLineInfo = (callDetails != null) ?
                callDetails.getMultiIdentityLineInfo() : null;
        dialRequest.multiLineInfo = fromMultiIdentityLineInfo(multiIdentityLineInfo);

        dialRequest.redialInfo = fromRedialInfo(redialInfo);

        return dialRequest;
    }

    public static EmergencyDialRequest fromEmergencyDialRequest(EmergencyCallInfo eInfo,
            String address, int clirMode, CallDetails callDetails, boolean isEncrypted,
            RedialInfo redialInfo) {

        if (eInfo == null) {
            return null;
        }

        EmergencyDialRequest eDialRequest = new EmergencyDialRequest();
        eDialRequest.dialRequest = fromDialRequest(address, clirMode, callDetails, isEncrypted,
                redialInfo);
        eDialRequest.categories = fromEmergencyServiceCategory(
               eInfo.getEmergencyServiceCategories());
        eDialRequest.urns = eInfo.getEmergencyUrns().toArray(new String[0]);
        eDialRequest.route = fromEmergencyCallRouting(eInfo.getEmergencyCallRouting());
        eDialRequest.hasKnownUserIntentEmergency = eInfo.hasKnownUserIntentEmergency();
        eDialRequest.isTesting = eInfo.isEmergencyCallTesting();
        return eDialRequest;
    }

    public static CallComposerDialRequest fromCallComposerDialRequest(CallComposerInfo from,
            String address, int clirMode, CallDetails callDetails, boolean isEncrypted,
            RedialInfo redialInfo) {

        if (from == null) {
            return null;
        }

        CallComposerDialRequest ccDialRequest = new CallComposerDialRequest();
        ccDialRequest.dialRequest = fromDialRequest(address, clirMode, callDetails,
                isEncrypted, redialInfo);
        ccDialRequest.callComposerInfo = fromCallComposerInfo(from);

        return ccDialRequest;
    }

    private static vendor.qti.hardware.radio.ims.CallComposerInfo fromCallComposerInfo(
            CallComposerInfo from) {

        vendor.qti.hardware.radio.ims.CallComposerInfo to =
            new vendor.qti.hardware.radio.ims.CallComposerInfo();

        to.priority = fromPriority(from.getPriority());
        if (from.getSubject() != null) {
            to.subject = from.getSubject().toCharArray();
        } else {
            to.subject = new char[0];
        }

        if (from.getLocation() != null) {
            to.location = fromLocation(from.getLocation());
        } else {
            to.location = new CallLocation();
        }

        if (from.getImageUrl() != null) {
            to.imageUrl = from.getImageUrl().toString();
        } else {
            to.imageUrl = "";
        }

        return to;
    }

    /**
     * Converts CallDetails object to the AIDL CallDetails.
     *
     * @param callDetails IMS call details
     * @return AIDL CallDetails
     *
     */
    private static vendor.qti.hardware.radio.ims.CallDetails fromCallDetails(
            CallDetails callDetails) {
        vendor.qti.hardware.radio.ims.CallDetails halCallDetails =
                new vendor.qti.hardware.radio.ims.CallDetails();
        //Initialize CallDetails
        halCallDetails.extras = new String[0];
        halCallDetails.localAbility = new ServiceStatusInfo[0];
        halCallDetails.peerAbility = new ServiceStatusInfo[0];
        halCallDetails.sipAlternateUri = "";

        if (callDetails == null) {
            return halCallDetails;
        }

        halCallDetails.callType = fromCallType(callDetails.call_type);
        halCallDetails.callDomain = fromCallDomain(callDetails.call_domain);
        halCallDetails.rttMode = fromRttMode(callDetails.getRttMode());
        int extrasLength = (callDetails.extras != null) ? callDetails.extras.length : 0;
        halCallDetails.extras = new String[extrasLength];
        for (int i = 0; i < extrasLength; i++) {
            halCallDetails.extras[i] = callDetails.extras[i];
        }
        return halCallDetails;
    }

    private static int fromCallType(int callType) {
        switch (callType) {
            case CallDetails.CALL_TYPE_VOICE:
                return CallType.VOICE;
            case CallDetails.CALL_TYPE_VT_TX:
                return CallType.VT_TX;
            case CallDetails.CALL_TYPE_VT_RX:
                return CallType.VT_RX;
            case CallDetails.CALL_TYPE_VT:
                return CallType.VT;
            case CallDetails.CALL_TYPE_VT_NODIR:
                return CallType.VT_NODIR;
            case CallDetails.CALL_TYPE_CS_VS_TX:
                return CallType.CS_VS_TX;
            case CallDetails.CALL_TYPE_CS_VS_RX:
                return CallType.CS_VS_RX;
            case CallDetails.CALL_TYPE_PS_VS_TX:
                return CallType.PS_VS_TX;
            case CallDetails.CALL_TYPE_PS_VS_RX:
                return CallType.PS_VS_RX;
            case CallDetails.CALL_TYPE_SMS:
                return CallType.SMS;
            case CallDetails.CALL_TYPE_UT:
                return CallType.UT;
            case CallDetails.CALL_TYPE_UNKNOWN:
            default:
                return CallType.UNKNOWN;
        }
    }

    private static int fromCallDomain(int callDomain) {
        switch (callDomain) {
            case CallDetails.CALL_DOMAIN_UNKNOWN:
                return CallDomain.UNKNOWN;
            case CallDetails.CALL_DOMAIN_CS:
                return CallDomain.CS;
            case CallDetails.CALL_DOMAIN_PS:
                return CallDomain.PS;
            case CallDetails.CALL_DOMAIN_AUTOMATIC:
                return CallDomain.AUTOMATIC;
            case CallDetails.CALL_DOMAIN_NOT_SET:
            default:
                return CallDomain.NOT_SET;
        }
    }

    public static int toCallDomain(int callDomain) {
        switch (callDomain) {
            case CallDomain.UNKNOWN:
                return CallDetails.CALL_DOMAIN_UNKNOWN;
            case CallDomain.CS:
                return CallDetails.CALL_DOMAIN_CS;
            case CallDomain.PS:
                return CallDetails.CALL_DOMAIN_PS;
            case CallDomain.AUTOMATIC:
                return CallDetails.CALL_DOMAIN_AUTOMATIC;
            case CallDomain.NOT_SET:
            case CallDomain.INVALID:
            default:
                return CallDetails.CALL_DOMAIN_NOT_SET;
        }
    }

    private static int fromRttMode(int rttMode) {
        switch (rttMode) {
            case ImsStreamMediaProfile.RTT_MODE_FULL:
                return RttMode.FULL;
            case ImsStreamMediaProfile.RTT_MODE_DISABLED:
            default:
                return RttMode.DISABLED;
        }
    }

    private static int toRttMode(int rttMode) {
        switch (rttMode) {
            case RttMode.FULL:
                return ImsStreamMediaProfile.RTT_MODE_FULL;
            case RttMode.DISABLED:
            default:
                return ImsStreamMediaProfile.RTT_MODE_DISABLED;
        }
    }

    /**
     * Get the conference uri information from call details
     *
     * @param callDetails IMS call details
     * @return isConferenceUri true/false if it is a conference call or not
     *
     */
    private static boolean hasConferenceUri(CallDetails callDetails) {
        if (callDetails == null || callDetails.extras == null) {
            return false;
        }

        String value = callDetails.getValueForKeyFromExtras(callDetails.extras,
                CallDetails.EXTRAS_IS_CONFERENCE_URI);

        return value != null && Boolean.valueOf(value);
    }

    /**
    * Utility function to populate MulltiIdentity Information in AIDL
    *
    * If from is null, the deafult value of MultiIdentityLineInfo class will be used
    */
    public static vendor.qti.hardware.radio.ims.MultiIdentityLineInfo fromMultiIdentityLineInfo(
            MultiIdentityLineInfo from) {
        vendor.qti.hardware.radio.ims.MultiIdentityLineInfo to =
            new vendor.qti.hardware.radio.ims.MultiIdentityLineInfo();
        if (from == null) {
            to.msisdn = "";
            return to;
        }

        to.msisdn = from.getMsisdn();
        to.lineType = from.getLineType();
        to.registrationStatus = from.getLineStatus();
        return to;
    }

    public static vendor.qti.hardware.radio.ims.MultiIdentityLineInfo[]
            fromMultiIdentityLineInfoList(Collection <MultiIdentityLineInfo> linesInfo) {
        List<vendor.qti.hardware.radio.ims.MultiIdentityLineInfo> halLinesInfo =
                new ArrayList<vendor.qti.hardware.radio.ims.MultiIdentityLineInfo>();
        for (MultiIdentityLineInfo line : linesInfo) {
            halLinesInfo.add(fromMultiIdentityLineInfo(line));
        }
        return halLinesInfo.toArray(new vendor.qti.hardware.radio.ims.MultiIdentityLineInfo[0]);
    }

    public static ArrayList<MultiIdentityLineInfo> toMultiIdentityLineInfoList(
            vendor.qti.hardware.radio.ims.MultiIdentityLineInfo[] info) {
        ArrayList<MultiIdentityLineInfo> linesInfo = new ArrayList<MultiIdentityLineInfo>();
        for (vendor.qti.hardware.radio.ims.MultiIdentityLineInfo line : info) {
            MultiIdentityLineInfo lineInfo = StableAidl.toMultiIdentityLineInfo(line);
            linesInfo.add(lineInfo);
        }
        return linesInfo;
    }

    public static MultiIdentityLineInfo toMultiIdentityLineInfo(
            vendor.qti.hardware.radio.ims.MultiIdentityLineInfo from) {
        MultiIdentityLineInfo to = new MultiIdentityLineInfo(
                from.msisdn, from.lineType, from.registrationStatus);
        return to;
    }

    public static vendor.qti.hardware.radio.ims.RedialInfo fromRedialInfo(RedialInfo from) {
        vendor.qti.hardware.radio.ims.RedialInfo to =
                new vendor.qti.hardware.radio.ims.RedialInfo();
        to.callFailReason = CallFailCause.ERROR_UNSPECIFIED;
        to.callFailRadioTech = RadioTechType.INVALID;
        if (from == null) {
            return to;
        }

        to.callFailReason = fromImsReasonInfo(from.getRetryCallFailCause());
        to.callFailRadioTech = fromRadioTech(from.getRetryCallFailRadioTech());
        return to;
    }

    private static int fromImsReasonInfo(int imsReason) {
        Log.d(TAG, "imsReason= " + imsReason);
        switch(imsReason) {
            case ImsReasonInfo.CODE_SIP_METHOD_NOT_ALLOWED:
                return CallFailCause.SIP_METHOD_NOT_ALLOWED;
            case ImsReasonInfo.CODE_SIP_PROXY_AUTHENTICATION_REQUIRED:
                return CallFailCause.SIP_PROXY_AUTHENTICATION_REQUIRED;
            case ImsReasonInfo.CODE_SIP_REQUEST_ENTITY_TOO_LARGE:
                return CallFailCause.SIP_REQUEST_ENTITY_TOO_LARGE;
            case ImsReasonInfo.CODE_SIP_EXTENSION_REQUIRED:
                return CallFailCause.SIP_EXTENSION_REQUIRED;
            case ImsReasonInfo.CODE_SIP_REQUEST_URI_TOO_LARGE:
                return CallFailCause.SIP_REQUEST_URI_TOO_LARGE;
            case ImsReasonInfo.CODE_SIP_INTERVAL_TOO_BRIEF:
                return CallFailCause.SIP_INTERVAL_TOO_BRIEF;
            case ImsReasonInfo.CODE_SIP_CALL_OR_TRANS_DOES_NOT_EXIST:
                return CallFailCause.SIP_CALL_OR_TRANS_DOES_NOT_EXIST;
            case ImsReasonInfo.CODE_REJECTED_ELSEWHERE:
                return CallFailCause.REJECTED_ELSEWHERE;
            case ImsReasonInfo.CODE_USER_REJECTED_SESSION_MODIFICATION:
                return CallFailCause.USER_REJECTED_SESSION_MODIFICATION;
            case ImsReasonInfo.CODE_USER_CANCELLED_SESSION_MODIFICATION:
                return CallFailCause.USER_CANCELLED_SESSION_MODIFICATION;
            case ImsReasonInfo.CODE_SESSION_MODIFICATION_FAILED:
                return CallFailCause.SESSION_MODIFICATION_FAILED;
            case ImsReasonInfo.CODE_SIP_LOOP_DETECTED:
                return CallFailCause.SIP_LOOP_DETECTED;
            case ImsReasonInfo.CODE_SIP_TOO_MANY_HOPS:
                return CallFailCause.SIP_TOO_MANY_HOPS;
            case ImsReasonInfo.CODE_SIP_AMBIGUOUS:
                return CallFailCause.SIP_AMBIGUOUS;
            case ImsReasonInfo.CODE_SIP_REQUEST_PENDING:
                return CallFailCause.SIP_REQUEST_PENDING;
            case ImsReasonInfo.CODE_SIP_UNDECIPHERABLE:
                return CallFailCause.SIP_UNDECIPHERABLE;
            case QtiCallConstants.CODE_RETRY_ON_IMS_WITHOUT_RTT:
                return CallFailCause.RETRY_ON_IMS_WITHOUT_RTT;
            case ImsReasonInfo.CODE_SIP_USER_MARKED_UNWANTED:
                return CallFailCause.SIP_USER_MARKED_UNWANTED;
            case ImsReasonInfo.CODE_USER_DECLINE:
                return CallFailCause.USER_REJECT;
            case ImsReasonInfo.CODE_USER_TERMINATED:
                return CallFailCause.USER_BUSY;
            case ImsReasonInfo.CODE_LOW_BATTERY:
                return CallFailCause.LOW_BATTERY;
            case ImsReasonInfo.CODE_BLACKLISTED_CALL_ID:
                return CallFailCause.BLACKLISTED_CALL_ID;
            default:
                Log.v(TAG, "Unsupported imsReason for ending call. Passing end cause as 'misc'.");
                return CallFailCause.ERROR_UNSPECIFIED;
        }
    }

    private static int fromRadioTech(int radioTech) {
        switch (radioTech) {
            case RadioTech.RADIO_TECH_ANY:
                return RadioTechType.ANY;
            case RadioTech.RADIO_TECH_UNKNOWN:
                return RadioTechType.UNKNOWN;
            case RadioTech.RADIO_TECH_GPRS:
                return RadioTechType.GPRS;
            case RadioTech.RADIO_TECH_EDGE:
                return RadioTechType.EDGE;
            case RadioTech.RADIO_TECH_UMTS:
                return RadioTechType.UMTS;
            case RadioTech.RADIO_TECH_IS95A:
                return RadioTechType.IS95A;
            case RadioTech.RADIO_TECH_IS95B:
                return RadioTechType.IS95B;
            case RadioTech.RADIO_TECH_1xRTT:
                return RadioTechType.RTT_1X;
            case RadioTech.RADIO_TECH_EVDO_0:
                return RadioTechType.EVDO_0;
            case RadioTech.RADIO_TECH_EVDO_A:
                return RadioTechType.EVDO_A;
            case RadioTech.RADIO_TECH_HSDPA:
                return RadioTechType.HSDPA;
            case RadioTech.RADIO_TECH_HSUPA:
                return RadioTechType.HSUPA;
            case RadioTech.RADIO_TECH_HSPA:
                return RadioTechType.HSPA;
            case RadioTech.RADIO_TECH_EVDO_B:
                return RadioTechType.EVDO_B;
            case RadioTech.RADIO_TECH_EHRPD:
                return RadioTechType.EHRPD;
            case RadioTech.RADIO_TECH_LTE:
                return RadioTechType.LTE;
            case RadioTech.RADIO_TECH_HSPAP:
                return RadioTechType.HSPAP;
            case RadioTech.RADIO_TECH_GSM:
                return RadioTechType.GSM;
            case RadioTech.RADIO_TECH_TD_SCDMA:
                return RadioTechType.TD_SCDMA;
            case RadioTech.RADIO_TECH_WIFI:
                return RadioTechType.WIFI;
            case RadioTech.RADIO_TECH_IWLAN:
                return RadioTechType.IWLAN;
            case RadioTech.RADIO_TECH_NR5G:
                return RadioTechType.NR5G;
            case RadioTech.RADIO_TECH_C_IWLAN:
                return RadioTechType.C_IWLAN;
            default:
                return RadioTechType.INVALID;
        }
    }

    private static int fromEmergencyServiceCategory(@EmergencyServiceCategories int categories) {
        int toHalCategories = EmergencyServiceCategory.UNSPECIFIED;
        if ((categories & EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_POLICE)
                == EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_POLICE) {
            toHalCategories |= EmergencyServiceCategory.POLICE;
        }
        if ((categories & EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AMBULANCE)
                == EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AMBULANCE) {
            toHalCategories |= EmergencyServiceCategory.AMBULANCE;
        }
        if ((categories & EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE)
                == EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE) {
            toHalCategories |= EmergencyServiceCategory.FIRE_BRIGADE;
        }
        if ((categories & EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD)
                == EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD) {
            toHalCategories |= EmergencyServiceCategory.MARINE_GUARD;
        }
        if ((categories & EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE)
                == EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE) {
            toHalCategories |= EmergencyServiceCategory.MOUNTAIN_RESCUE;
        }
        if ((categories & EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MIEC)
                == EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MIEC) {
            toHalCategories |= EmergencyServiceCategory.MIEC;
        }
        if ((categories & EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AIEC)
                == EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AIEC) {
            toHalCategories |= EmergencyServiceCategory.AIEC;
        }
        return toHalCategories;
    }

    private static int fromEmergencyCallRouting(@EmergencyCallRouting int routing) {
        switch (routing) {
            case EmergencyNumber.EMERGENCY_CALL_ROUTING_NORMAL:
                return EmergencyCallRoute.NORMAL;
            case EmergencyNumber.EMERGENCY_CALL_ROUTING_EMERGENCY:
                return EmergencyCallRoute.EMERGENCY;
            default:
                return EmergencyCallRoute.UNKNOWN;
        }
    }

    private static int fromPriority(int priority) {
        switch (priority) {
            case CallComposerInfo.PRIORITY_URGENT:
                return vendor.qti.hardware.radio.ims.CallPriority.URGENT;
            default:
                return vendor.qti.hardware.radio.ims.CallPriority.NORMAL;
        }
    }

    private static CallLocation fromLocation(CallComposerInfo.Location from) {

        CallLocation location = new CallLocation();
        location.radius = from.getRadius();
        location.latitude = from.getLatitude();
        location.longitude = from.getLongitude();

        return location;
    }

    public static CallModifyInfo fromCallModify(CallModify callModify) {
        CallModifyInfo callModifyInfo = new CallModifyInfo();
        callModifyInfo.callIndex = callModify.call_index;

        callModifyInfo.callDetails = fromCallDetails(callModify.call_details);

        return callModifyInfo;
    }

    public static HangupRequestInfo fromHangup(int connectionId, String userUri, String confUri,
            boolean mpty, int failCause, String errorInfo) {
        HangupRequestInfo hangup = new HangupRequestInfo();
        /* If Calltracker has a matching local connection the connection id will be used.
         * if there is no matching connection object and if it is a remotely added participant
         * then connection id will not be present hence 0
         */
        if (connectionId != INVALID_CONNID) {
            hangup.connIndex = connectionId;
        } else {
            hangup.connIndex = Integer.MAX_VALUE;
        }

        if (userUri != null) {
            hangup.connUri = userUri;
        } else {
            hangup.connUri = "";
        }

        hangup.multiParty = mpty;
        hangup.conf_id = Integer.MAX_VALUE;

        hangup.failCauseResponse = fromCallFailCauseResponse(failCause, errorInfo);

        return hangup;
    }

    private static CallFailCauseResponse fromCallFailCauseResponse(int failCause,
            String errorInfo) {
        // Initialize CallFailCauseResponse
        CallFailCauseResponse failCauseResponse = new CallFailCauseResponse();
        failCauseResponse.errorInfo = new byte[0];
        failCauseResponse.networkErrorString = "";
        failCauseResponse.hasErrorDetails = false;
        failCauseResponse.errorDetails = new SipErrorInfo();
        failCauseResponse.errorDetails.errorString = "";

        if (failCause == Integer.MAX_VALUE) {
            return failCauseResponse;
        }

        if (errorInfo != null && !errorInfo.isEmpty()) {
            Log.v(TAG, "hangupWithReason errorInfo = " + errorInfo);
            failCauseResponse.errorInfo = errorInfo.getBytes();
        }
        int callFailCause = fromImsReasonInfo(failCause);
        failCauseResponse.failCause = callFailCause;
        Log.v(TAG, "hangupWithReason callFailCause=" + callFailCause);
        // Check for unsupported call end reason. If so, set
        // the errorInfo string to the reason code, similar to KK.
        if (callFailCause == CallFailCause.MISC) {
            failCauseResponse.errorInfo = Integer.toString(failCause).getBytes();
        }
        Log.v(TAG, "hangupWithReason MISC callFailCause, errorInfo=" + failCause);
        return failCauseResponse;
    }

    public static ColrInfo fromColrValue(int presentationValue) {
        ColrInfo colrValue = new ColrInfo();
        colrValue.presentation = fromIpPresentation(presentationValue);

        // Initialize SipErrorInfo
        colrValue.errorDetails = new SipErrorInfo();
        colrValue.errorDetails.errorString = "";
        return colrValue;
    }

    private static int fromIpPresentation(int presentation) {
        switch (presentation) {
            case SuppService.IP_PRESENTATION_NUM_ALLOWED:
                return IpPresentation.NUM_ALLOWED;
            case SuppService.IP_PRESENTATION_NUM_RESTRICTED:
                return IpPresentation.NUM_RESTRICTED;
            case SuppService.IP_PRESENTATION_NUM_DEFAULT:
                 return IpPresentation.NUM_DEFAULT;
            default:
                return IpPresentation.INVALID;
        }
    }

    public static SuppService toSuppService(ColrInfo colrInfo) {
        SuppService colrValue = new SuppService();

        colrValue.setServiceClassStatus(toServiceClassStatus(colrInfo.status));
        colrValue.setProvisionStatus(toServiceClassProvisionStatus(colrInfo.provisionStatus));
        colrValue.setStatus(toServiceClassStatus(colrInfo.status));

        final int presentation = toIpPresentation(colrInfo.presentation);
        if (presentation != IpPresentation.INVALID) {
            colrValue.setPresentation(presentation);
            Log.v(TAG, "getColrResponse from ImsRadio. presentation " + presentation);
        }

        colrValue.setErrorDetails(toSipError(colrInfo.errorDetails));
        Log.v(TAG, "getColrResponse from ImsRadio. errorcode " + colrInfo.errorDetails.errorCode +
                " string " + colrInfo.errorDetails.errorString);
        return colrValue;
    }

    public static int toServiceClassStatus(int inServiceStatus) {
        switch(inServiceStatus) {
            case ServiceClassStatus.ENABLED:
                return SuppSvcResponse.ENABLED;
            case ServiceClassStatus.DISABLED:
                return SuppSvcResponse.DISABLED;
            default:
                return SuppSvcResponse.INVALID;
        }
    }

    public static int toServiceClassProvisionStatus(int inServiceProvisionStatus) {
        switch(inServiceProvisionStatus) {
            case ServiceClassProvisionStatus.NOT_PROVISIONED:
                return SuppService.NOT_PROVISIONED;
            case ServiceClassProvisionStatus.PROVISIONED:
                return SuppService.PROVISIONED;
            default:
                return SuppService.STATUS_UNKNOWN;
        }
    }

    public static ConfigInfo fromImsConfig(int item, boolean boolValue,
            int intValue, String strValue, int errorCause) {
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.item = fromImsConfigItem(item);
        configInfo.hasBoolValue = true;
        configInfo.boolValue = boolValue;
        configInfo.intValue = intValue;
        configInfo.stringValue = "";
        if (strValue != null) {
            configInfo.stringValue = strValue;
        }
        configInfo.errorCause = StableAidlErrorCode.fromImsConfigErrorCode(errorCause);
        return configInfo;
    }

    private static int fromImsConfigItem(int item) {
        switch(item){
            case ImsConfigItem.NONE:
                return ConfigItem.NONE;
            case ImsConfigItem.VOCODER_AMRMODESET:
                return ConfigItem.VOCODER_AMRMODESET;
            case ImsConfigItem.VOCODER_AMRWBMODESET:
                return ConfigItem.VOCODER_AMRWBMODESET;
            case ImsConfigItem.SIP_SESSION_TIMER:
                return ConfigItem.SIP_SESSION_TIMER;
            case ImsConfigItem.MIN_SESSION_EXPIRY:
                return ConfigItem.MIN_SESSION_EXPIRY;
            case ImsConfigItem.CANCELLATION_TIMER:
                return ConfigItem.CANCELLATION_TIMER;
            case ImsConfigItem.T_DELAY:
                return ConfigItem.T_DELAY;
            case ImsConfigItem.SILENT_REDIAL_ENABLE:
                return ConfigItem.SILENT_REDIAL_ENABLE;
            case ImsConfigItem.SIP_T1_TIMER:
                return ConfigItem.SIP_T1_TIMER;
            case ImsConfigItem.SIP_T2_TIMER:
                return ConfigItem.SIP_T2_TIMER;
            case ImsConfigItem.SIP_TF_TIMER:
                return ConfigItem.SIP_TF_TIMER;
            case ImsConfigItem.VLT_SETTING_ENABLED:
                return ConfigItem.VLT_SETTING_ENABLED;
            case ImsConfigItem.LVC_SETTING_ENABLED:
                return ConfigItem.LVC_SETTING_ENABLED;
            case ImsConfigItem.DOMAIN_NAME:
                return ConfigItem.DOMAIN_NAME;
            case ImsConfigItem.SMS_FORMAT:
                return ConfigItem.SMS_FORMAT;
            case ImsConfigItem.SMS_OVER_IP:
                return ConfigItem.SMS_OVER_IP;
            case ImsConfigItem.PUBLISH_TIMER:
                return ConfigItem.PUBLISH_TIMER;
            case ImsConfigItem.PUBLISH_TIMER_EXTENDED:
                return ConfigItem.PUBLISH_TIMER_EXTENDED;
            case ImsConfigItem.CAPABILITIES_CACHE_EXPIRATION:
                return ConfigItem.CAPABILITIES_CACHE_EXPIRATION;
            case ImsConfigItem.AVAILABILITY_CACHE_EXPIRATION:
                return ConfigItem.AVAILABILITY_CACHE_EXPIRATION;
            case ImsConfigItem.CAPABILITIES_POLL_INTERVAL:
                return ConfigItem.CAPABILITIES_POLL_INTERVAL;
            case ImsConfigItem.SOURCE_THROTTLE_PUBLISH:
                return ConfigItem.SOURCE_THROTTLE_PUBLISH;
            case ImsConfigItem.MAX_NUM_ENTRIES_IN_RCL:
                return ConfigItem.MAX_NUM_ENTRIES_IN_RCL;
            case ImsConfigItem.CAPAB_POLL_LIST_SUB_EXP:
                return ConfigItem.CAPAB_POLL_LIST_SUB_EXP;
            case ImsConfigItem.GZIP_FLAG:
                return ConfigItem.GZIP_FLAG;
            case ImsConfigItem.EAB_SETTING_ENABLED:
                return ConfigItem.EAB_SETTING_ENABLED;
            case ImsConfigItem.MOBILE_DATA_ENABLED:
                return ConfigItem.MOBILE_DATA_ENABLED;
            case ImsConfigItem.VOICE_OVER_WIFI_ENABLED:
                return ConfigItem.VOICE_OVER_WIFI_ENABLED;
            case ImsConfigItem.VOICE_OVER_WIFI_ROAMING:
                return ConfigItem.VOICE_OVER_WIFI_ROAMING;
            case ImsConfigItem.VOICE_OVER_WIFI_MODE:
                return ConfigItem.VOICE_OVER_WIFI_MODE;
            case ImsConfigItem.CAPABILITY_DISCOVERY_ENABLED:
                return ConfigItem.CAPABILITY_DISCOVERY_ENABLED;
            case ImsConfigItem.EMERGENCY_CALL_TIMER:
                return ConfigItem.EMERGENCY_CALL_TIMER;
            case ImsConfigItem.SSAC_HYSTERESIS_TIMER:
                return ConfigItem.SSAC_HYSTERESIS_TIMER;
            case ImsConfigItem.VOLTE_USER_OPT_IN_STATUS:
                return ConfigItem.VOLTE_USER_OPT_IN_STATUS;
            case ImsConfigItem.LBO_PCSCF_ADDRESS:
                return ConfigItem.LBO_PCSCF_ADDRESS;
            case ImsConfigItem.KEEP_ALIVE_ENABLED:
                return ConfigItem.KEEP_ALIVE_ENABLED;
            case ImsConfigItem.REGISTRATION_RETRY_BASE_TIME_SEC:
                return ConfigItem.REGISTRATION_RETRY_BASE_TIME_SEC;
            case ImsConfigItem.REGISTRATION_RETRY_MAX_TIME_SEC:
                return ConfigItem.REGISTRATION_RETRY_MAX_TIME_SEC;
            case ImsConfigItem.SPEECH_START_PORT:
                return ConfigItem.SPEECH_START_PORT;
            case ImsConfigItem.SPEECH_END_PORT:
                return ConfigItem.SPEECH_END_PORT;
            case ImsConfigItem.SIP_INVITE_REQ_RETX_INTERVAL_MSEC:
                return ConfigItem.SIP_INVITE_REQ_RETX_INTERVAL_MSEC;
            case ImsConfigItem.SIP_INVITE_RSP_WAIT_TIME_MSEC:
                return ConfigItem.SIP_INVITE_RSP_WAIT_TIME_MSEC;
            case ImsConfigItem.SIP_INVITE_RSP_RETX_WAIT_TIME_MSEC:
                return ConfigItem.SIP_INVITE_RSP_RETX_WAIT_TIME_MSEC;
            case ImsConfigItem.SIP_NON_INVITE_REQ_RETX_INTERVAL_MSEC:
                return ConfigItem.SIP_NON_INVITE_REQ_RETX_INTERVAL_MSEC;
            case ImsConfigItem.SIP_NON_INVITE_TXN_TIMEOUT_TIMER_MSEC:
                return ConfigItem.SIP_NON_INVITE_TXN_TIMEOUT_TIMER_MSEC;
            case ImsConfigItem.SIP_INVITE_RSP_RETX_INTERVAL_MSEC:
                return ConfigItem.SIP_INVITE_RSP_RETX_INTERVAL_MSEC;
            case ImsConfigItem.SIP_ACK_RECEIPT_WAIT_TIME_MSEC:
                return ConfigItem.SIP_ACK_RECEIPT_WAIT_TIME_MSEC;
            case ImsConfigItem.SIP_ACK_RETX_WAIT_TIME_MSEC:
                return ConfigItem.SIP_ACK_RETX_WAIT_TIME_MSEC;
            case ImsConfigItem.SIP_NON_INVITE_REQ_RETX_WAIT_TIME_MSEC:
                return ConfigItem.SIP_NON_INVITE_REQ_RETX_WAIT_TIME_MSEC;
            case ImsConfigItem.SIP_NON_INVITE_RSP_RETX_WAIT_TIME_MSEC:
                return ConfigItem.SIP_NON_INVITE_RSP_RETX_WAIT_TIME_MSEC;
            case ImsConfigItem.AMR_WB_OCTET_ALIGNED_PT:
                return ConfigItem.AMR_WB_OCTET_ALIGNED_PT;
            case ImsConfigItem.AMR_WB_BANDWIDTH_EFFICIENT_PT:
                return ConfigItem.AMR_WB_BANDWIDTH_EFFICIENT_PT;
            case ImsConfigItem.AMR_OCTET_ALIGNED_PT:
                return ConfigItem.AMR_OCTET_ALIGNED_PT;
            case ImsConfigItem.AMR_BANDWIDTH_EFFICIENT_PT:
                return ConfigItem.AMR_BANDWIDTH_EFFICIENT_PT;
            case ImsConfigItem.DTMF_WB_PT:
                return ConfigItem.DTMF_WB_PT;
            case ImsConfigItem.DTMF_NB_PT:
                return ConfigItem.DTMF_NB_PT;
            case ImsConfigItem.AMR_DEFAULT_MODE:
                return ConfigItem.AMR_DEFAULT_MODE;
            case ImsConfigItem.SMS_PSI:
                return ConfigItem.SMS_PSI;
            case ImsConfigItem.VIDEO_QUALITY:
                return ConfigItem.VIDEO_QUALITY;
            case ImsConfigItem.THRESHOLD_LTE1:
                return ConfigItem.THRESHOLD_LTE1;
            case ImsConfigItem.THRESHOLD_LTE2:
                return ConfigItem.THRESHOLD_LTE2;
            case ImsConfigItem.THRESHOLD_LTE3:
                return ConfigItem.THRESHOLD_LTE3;
            case ImsConfigItem.THRESHOLD_1x:
                return ConfigItem.THRESHOLD_1x;
            case ImsConfigItem.THRESHOLD_WIFI_A:
                return ConfigItem.THRESHOLD_WIFI_A;
            case ImsConfigItem.THRESHOLD_WIFI_B:
                return ConfigItem.THRESHOLD_WIFI_B;
            case ImsConfigItem.T_EPDG_LTE:
                return ConfigItem.T_EPDG_LTE;
            case ImsConfigItem.T_EPDG_WIFI:
                return ConfigItem.T_EPDG_WIFI;
            case ImsConfigItem.T_EPDG_1x:
                return ConfigItem.T_EPDG_1x;
            case ImsConfigItem.VWF_SETTING_ENABLED:
                return ConfigItem.VWF_SETTING_ENABLED;
            case ImsConfigItem.VCE_SETTING_ENABLED:
                return ConfigItem.VCE_SETTING_ENABLED;
            case ImsConfigItem.SMS_APP:
                return ConfigItem.SMS_APP;
            case ImsConfigItem.VVM_APP:
                return ConfigItem.VVM_APP;
            case ImsConfigItem.RTT_SETTING_ENABLED:
                return ConfigItem.RTT_SETTING_ENABLED;
            case ImsConfigItem.VOICE_OVER_WIFI_ROAMING_MODE:
                return ConfigItem.VOICE_OVER_WIFI_ROAMING_MODE;
            case ImsConfigItem.AUTO_REJECT_CALL_MODE:
                return ConfigItem.SET_AUTO_REJECT_CALL_MODE_CONFIG;
            case ImsConfigItem.CALL_COMPOSER_MODE:
                return ConfigItem.MMTEL_CALL_COMPOSER_CONFIG;
            case ImsConfigItem.VOICE_OVER_WIFI_ENTITLEMENT_ID:
                return ConfigItem.VOWIFI_ENTITLEMENT_ID;
            case ImsConfigItem.B2C_ENRICHED_CALLING_MODE:
                return ConfigItem.B2C_ENRICHED_CALLING_CONFIG;
            case ImsConfigItem.DATA_CHANNEL_MODE:
                return ConfigItem.DATA_CHANNEL;
            case ImsConfigItem.VOLTE_PROVISIONING_RESTRICT_HOME:
                return ConfigItem.VOLTE_PROVISIONING_RESTRICT_HOME;
            case ImsConfigItem.VOLTE_PROVISIONING_RESTRICT_ROAMING:
                return ConfigItem.VOLTE_PROVISIONING_RESTRICT_ROAMING;
            default:
                return ConfigItem.INVALID;
        }
    }

    private static ImsConfigItem toImsConfig(ConfigInfo configInfo) {
        if (configInfo == null) {
            return null;
        }

        ImsConfigItem config = new ImsConfigItem();
        config.setItem(toImsConfigItem(configInfo.item));
        if (configInfo.hasBoolValue) {
            config.setBoolValue(configInfo.boolValue);
        }

        if (configInfo.intValue != Integer.MAX_VALUE) {
            config.setIntValue(configInfo.intValue);
        }

        config.setStringValue(configInfo.stringValue);

        if (configInfo.errorCause != ConfigFailureCause.INVALID) {
            config.setErrorCause(StableAidlErrorCode.toImsConfigErrorCode(
                   configInfo.errorCause));
        }

        return config;
    }

    private static int toImsConfigItem(int item) {
        switch (item) {
            case ConfigItem.NONE:
                return ImsConfigItem.NONE;
            case ConfigItem.VOCODER_AMRMODESET:
                return ImsConfigItem.VOCODER_AMRMODESET;
            case ConfigItem.VOCODER_AMRWBMODESET:
                return ImsConfigItem.VOCODER_AMRWBMODESET;
            case ConfigItem.SIP_SESSION_TIMER:
                return ImsConfigItem.SIP_SESSION_TIMER;
            case ConfigItem.MIN_SESSION_EXPIRY:
                return ImsConfigItem.MIN_SESSION_EXPIRY;
            case ConfigItem.CANCELLATION_TIMER:
                return ImsConfigItem.CANCELLATION_TIMER;
            case ConfigItem.T_DELAY:
                return ImsConfigItem.T_DELAY;
            case ConfigItem.SILENT_REDIAL_ENABLE:
                return ImsConfigItem.SILENT_REDIAL_ENABLE;
            case ConfigItem.SIP_T1_TIMER:
                return ImsConfigItem.SIP_T1_TIMER;
            case ConfigItem.SIP_T2_TIMER:
                return ImsConfigItem.SIP_T2_TIMER;
            case ConfigItem.SIP_TF_TIMER:
                return ImsConfigItem.SIP_TF_TIMER;
            case ConfigItem.VLT_SETTING_ENABLED:
                return ImsConfigItem.VLT_SETTING_ENABLED;
            case ConfigItem.LVC_SETTING_ENABLED:
                return ImsConfigItem.LVC_SETTING_ENABLED;
            case ConfigItem.DOMAIN_NAME:
                return ImsConfigItem.DOMAIN_NAME;
            case ConfigItem.SMS_FORMAT:
                return ImsConfigItem.SMS_FORMAT;
            case ConfigItem.SMS_OVER_IP:
                return ImsConfigItem.SMS_OVER_IP;
            case ConfigItem.PUBLISH_TIMER:
                return ImsConfigItem.PUBLISH_TIMER;
            case ConfigItem.PUBLISH_TIMER_EXTENDED:
                return ImsConfigItem.PUBLISH_TIMER_EXTENDED;
            case ConfigItem.CAPABILITIES_CACHE_EXPIRATION:
                return ImsConfigItem.CAPABILITIES_CACHE_EXPIRATION;
            case ConfigItem.AVAILABILITY_CACHE_EXPIRATION:
                return ImsConfigItem.AVAILABILITY_CACHE_EXPIRATION;
            case ConfigItem.CAPABILITIES_POLL_INTERVAL:
                return ImsConfigItem.CAPABILITIES_POLL_INTERVAL;
            case ConfigItem.SOURCE_THROTTLE_PUBLISH:
                return ImsConfigItem.SOURCE_THROTTLE_PUBLISH;
            case ConfigItem.MAX_NUM_ENTRIES_IN_RCL:
                return ImsConfigItem.MAX_NUM_ENTRIES_IN_RCL;
            case ConfigItem.CAPAB_POLL_LIST_SUB_EXP:
                return ImsConfigItem.CAPAB_POLL_LIST_SUB_EXP;
            case ConfigItem.GZIP_FLAG:
                return ImsConfigItem.GZIP_FLAG;
            case ConfigItem.EAB_SETTING_ENABLED:
                return ImsConfigItem.EAB_SETTING_ENABLED;
            case ConfigItem.MOBILE_DATA_ENABLED:
                return ImsConfigItem.MOBILE_DATA_ENABLED;
            case ConfigItem.VOICE_OVER_WIFI_ENABLED:
                return ImsConfigItem.VOICE_OVER_WIFI_ENABLED;
            case ConfigItem.VOICE_OVER_WIFI_ROAMING:
                return ImsConfigItem.VOICE_OVER_WIFI_ROAMING;
            case ConfigItem.VOICE_OVER_WIFI_MODE:
                return ImsConfigItem.VOICE_OVER_WIFI_MODE;
            case ConfigItem.CAPABILITY_DISCOVERY_ENABLED:
                return ImsConfigItem.CAPABILITY_DISCOVERY_ENABLED;
            case ConfigItem.EMERGENCY_CALL_TIMER:
                return ImsConfigItem.EMERGENCY_CALL_TIMER;
            case ConfigItem.SSAC_HYSTERESIS_TIMER:
                return ImsConfigItem.SSAC_HYSTERESIS_TIMER;
            case ConfigItem.VOLTE_USER_OPT_IN_STATUS:
                return ImsConfigItem.VOLTE_USER_OPT_IN_STATUS;
            case ConfigItem.LBO_PCSCF_ADDRESS:
                return ImsConfigItem.LBO_PCSCF_ADDRESS;
            case ConfigItem.KEEP_ALIVE_ENABLED:
                return ImsConfigItem.KEEP_ALIVE_ENABLED;
            case ConfigItem.REGISTRATION_RETRY_BASE_TIME_SEC:
                return ImsConfigItem.REGISTRATION_RETRY_BASE_TIME_SEC;
            case ConfigItem.REGISTRATION_RETRY_MAX_TIME_SEC:
                return ImsConfigItem.REGISTRATION_RETRY_MAX_TIME_SEC;
            case ConfigItem.SPEECH_START_PORT:
                return ImsConfigItem.SPEECH_START_PORT;
            case ConfigItem.SPEECH_END_PORT:
                return ImsConfigItem.SPEECH_END_PORT;
            case ConfigItem.SIP_INVITE_REQ_RETX_INTERVAL_MSEC:
                return ImsConfigItem.SIP_INVITE_REQ_RETX_INTERVAL_MSEC;
            case ConfigItem.SIP_INVITE_RSP_WAIT_TIME_MSEC:
                return ImsConfigItem.SIP_INVITE_RSP_WAIT_TIME_MSEC;
            case ConfigItem.SIP_INVITE_RSP_RETX_WAIT_TIME_MSEC:
                return ImsConfigItem.SIP_INVITE_RSP_RETX_WAIT_TIME_MSEC;
            case ConfigItem.SIP_NON_INVITE_REQ_RETX_INTERVAL_MSEC:
                return ImsConfigItem.SIP_NON_INVITE_REQ_RETX_INTERVAL_MSEC;
            case ConfigItem.SIP_NON_INVITE_TXN_TIMEOUT_TIMER_MSEC:
                return ImsConfigItem.SIP_NON_INVITE_TXN_TIMEOUT_TIMER_MSEC;
            case ConfigItem.SIP_INVITE_RSP_RETX_INTERVAL_MSEC:
                return ImsConfigItem.SIP_INVITE_RSP_RETX_INTERVAL_MSEC;
            case ConfigItem.SIP_ACK_RECEIPT_WAIT_TIME_MSEC:
                return ImsConfigItem.SIP_ACK_RECEIPT_WAIT_TIME_MSEC;
            case ConfigItem.SIP_ACK_RETX_WAIT_TIME_MSEC:
                return ImsConfigItem.SIP_ACK_RETX_WAIT_TIME_MSEC;
            case ConfigItem.SIP_NON_INVITE_REQ_RETX_WAIT_TIME_MSEC:
                return ImsConfigItem.SIP_NON_INVITE_REQ_RETX_WAIT_TIME_MSEC;
            case ConfigItem.SIP_NON_INVITE_RSP_RETX_WAIT_TIME_MSEC:
                return ImsConfigItem.SIP_NON_INVITE_RSP_RETX_WAIT_TIME_MSEC;
            case ConfigItem.AMR_WB_OCTET_ALIGNED_PT:
                return ImsConfigItem.AMR_WB_OCTET_ALIGNED_PT;
            case ConfigItem.AMR_WB_BANDWIDTH_EFFICIENT_PT:
                return ImsConfigItem.AMR_WB_BANDWIDTH_EFFICIENT_PT;
            case ConfigItem.AMR_OCTET_ALIGNED_PT:
                return ImsConfigItem.AMR_OCTET_ALIGNED_PT;
            case ConfigItem.AMR_BANDWIDTH_EFFICIENT_PT:
                return ImsConfigItem.AMR_BANDWIDTH_EFFICIENT_PT;
            case ConfigItem.DTMF_WB_PT:
                return ImsConfigItem.DTMF_WB_PT;
            case ConfigItem.DTMF_NB_PT:
                return ImsConfigItem.DTMF_NB_PT;
            case ConfigItem.AMR_DEFAULT_MODE:
                return ImsConfigItem.AMR_DEFAULT_MODE;
            case ConfigItem.SMS_PSI:
                return ImsConfigItem.SMS_PSI;
            case ConfigItem.VIDEO_QUALITY:
                return ImsConfigItem.VIDEO_QUALITY;
            case ConfigItem.THRESHOLD_LTE1:
                return ImsConfigItem.THRESHOLD_LTE1;
            case ConfigItem.THRESHOLD_LTE2:
                return ImsConfigItem.THRESHOLD_LTE2;
            case ConfigItem.THRESHOLD_LTE3:
                return ImsConfigItem.THRESHOLD_LTE3;
            case ConfigItem.THRESHOLD_1x:
                return ImsConfigItem.THRESHOLD_1x;
            case ConfigItem.THRESHOLD_WIFI_A:
                return ImsConfigItem.THRESHOLD_WIFI_A;
            case ConfigItem.THRESHOLD_WIFI_B:
                return ImsConfigItem.THRESHOLD_WIFI_B;
            case ConfigItem.T_EPDG_LTE:
                return ImsConfigItem.T_EPDG_LTE;
            case ConfigItem.T_EPDG_WIFI:
                return ImsConfigItem.T_EPDG_WIFI;
            case ConfigItem.T_EPDG_1x:
                return ImsConfigItem.T_EPDG_1x;
            case ConfigItem.VWF_SETTING_ENABLED:
                return ImsConfigItem.VWF_SETTING_ENABLED;
            case ConfigItem.VCE_SETTING_ENABLED:
                return ImsConfigItem.VCE_SETTING_ENABLED;
            case ConfigItem.SMS_APP:
                return ImsConfigItem.SMS_APP;
            case ConfigItem.VVM_APP:
                return ImsConfigItem.VVM_APP;
            case ConfigItem.VOICE_OVER_WIFI_ROAMING_MODE:
                return ImsConfigItem.VOICE_OVER_WIFI_ROAMING_MODE;
            case ConfigItem.SET_AUTO_REJECT_CALL_MODE_CONFIG:
                return ImsConfigItem.AUTO_REJECT_CALL_MODE;
            case ConfigItem.MMTEL_CALL_COMPOSER_CONFIG:
                return ImsConfigItem.CALL_COMPOSER_MODE;
            case ConfigItem.VOWIFI_ENTITLEMENT_ID:
                return ImsConfigItem.VOICE_OVER_WIFI_ENTITLEMENT_ID;
            case ConfigItem.B2C_ENRICHED_CALLING_CONFIG:
                return ImsConfigItem.B2C_ENRICHED_CALLING_MODE;
            case ConfigItem.DATA_CHANNEL:
                return ImsConfigItem.DATA_CHANNEL_MODE;
            case ConfigItem.VOLTE_PROVISIONING_RESTRICT_HOME:
                return ImsConfigItem.VOLTE_PROVISIONING_RESTRICT_HOME;
            case ConfigItem.VOLTE_PROVISIONING_RESTRICT_ROAMING:
                return ImsConfigItem.VOLTE_PROVISIONING_RESTRICT_ROAMING;
            default:
                return ImsConfigItem.NONE;
        }
    }

    public static SuppService toSuppService(ClipProvisionStatus clipProvisionStatus) {
        SuppService clipProvStatus = new SuppService();
        final int clipStatus = clipProvisionStatus.clipStatus;
        if (clipStatus != ClipStatus.INVALID) {
            clipProvStatus.setStatus(toSuppServiceStatus(clipProvisionStatus.clipStatus));
            Log.v(StableAidl.class, "toSuppService. Clipstatus " + clipProvisionStatus);
        }

        clipProvStatus.setErrorDetails(toSipError(
                clipProvisionStatus.errorDetails));
        Log.v(StableAidl.class, "toSuppService. Errorcode " +
                clipProvisionStatus.errorDetails.errorCode + "String " +
                clipProvisionStatus.errorDetails.errorString);
        return clipProvStatus;
    }

    private static int toSuppServiceStatus(int clipStatus) {
        switch(clipStatus) {
            case ClipStatus.NOT_PROVISIONED:
                return SuppService.NOT_PROVISIONED;
            case ClipStatus.PROVISIONED:
                return SuppService.PROVISIONED;
            case ClipStatus.STATUS_UNKNOWN:
            default:
                return SuppService.STATUS_UNKNOWN;
        }
    }

    public static int toIpPresentation(int presentation) {
        switch(presentation) {
            case IpPresentation.NUM_ALLOWED:
                return SuppService.IP_PRESENTATION_NUM_ALLOWED;
            case IpPresentation.NUM_RESTRICTED:
                return SuppService.IP_PRESENTATION_NUM_RESTRICTED;
            case IpPresentation.NUM_DEFAULT:
            default:
                 return SuppService.IP_PRESENTATION_NUM_DEFAULT;
        }
    }

    public static ImsReasonInfo toSipError(SipErrorInfo errorInfo) {
        int imsReasonCode = ImsReasonInfo.CODE_UNSPECIFIED;
        int imsReasonExtraCode = ImsReasonInfo.CODE_UNSPECIFIED;
        if (errorInfo == null) {
            return new ImsReasonInfo(imsReasonCode, imsReasonExtraCode, null);
        }
        String imsReasonExtraMessage = errorInfo.errorString;
        if (errorInfo.errorCode != Integer.MAX_VALUE) {
            imsReasonExtraCode = errorInfo.errorCode;
        }
        return new ImsReasonInfo(imsReasonCode, imsReasonExtraCode, imsReasonExtraMessage);
    }

    public static ImsSubConfigDetails toImsSubconfigDetails(ImsSubConfigInfo subConfigInfo) {

        if (subConfigInfo == null) {
            return null;
        }

        ImsSubConfigDetails subConfig = new ImsSubConfigDetails();
        if (subConfigInfo.simultStackCount != Integer.MAX_VALUE) {
            subConfig.setSimultStackCount(subConfigInfo.simultStackCount);
        }

        for (int i = 0; i < subConfigInfo.imsStackEnabled.length; i++) {
            subConfig.addImsStackEnabled(subConfigInfo.imsStackEnabled[i]);
        }

        return subConfig;
    }

    public static AnswerRequest fromAnswerRequest(int callType, int presentation, int rttMode) {
        AnswerRequest answerRequest = new AnswerRequest();
        answerRequest.callType = fromCallType(callType);
        answerRequest.presentation = fromTirPresentation(presentation);
        answerRequest.mode = fromRttMode(rttMode);
        return answerRequest;
    }

    private static int fromTirPresentation(int presentation) {
        switch(presentation) {
            case QtiImsExtUtils.QTI_IMS_TIR_PRESENTATION_UNRESTRICTED:
                return IpPresentation.NUM_ALLOWED;
            case QtiImsExtUtils.QTI_IMS_TIR_PRESENTATION_RESTRICTED:
                return IpPresentation.NUM_RESTRICTED;
            default:
                return IpPresentation.NUM_DEFAULT;
        }
    }

    public static int toRegState(int state) {
        switch (state) {
            case RegState.REGISTERED:
                return ImsRegistrationInfo.REGISTERED;
            case RegState.REGISTERING:
                return ImsRegistrationInfo.REGISTERING;
            case RegState.NOT_REGISTERED:
                return ImsRegistrationInfo.NOT_REGISTERED;
            default:
                return ImsRegistrationInfo.INVALID;
        }
    }

    public static int fromRegState(int state) {
        switch (state) {
            case ImsRegistrationInfo.REGISTERED:
                return RegState.REGISTERED;
            case ImsRegistrationInfo.NOT_REGISTERED:
                return RegState.NOT_REGISTERED;
            case ImsRegistrationInfo.REGISTERING:
                return RegState.REGISTERING;
            default:
                return RegState.INVALID;
        }
    }

    public static ImsRegistrationInfo toImsRegistration(RegistrationInfo inRegistration) {
        if (inRegistration == null) {
            return null;
        }

        ImsRegistrationInfo outRegistration = new ImsRegistrationInfo();
        if (inRegistration.state != RegState.INVALID) {
            outRegistration.setState(toRegState(inRegistration.state));
        }

        if (inRegistration.errorCode != Integer.MAX_VALUE) {
            outRegistration.setErrorCode(inRegistration.errorCode);
        }

        outRegistration.setErrorMessage(inRegistration.errorMessage);

        if (inRegistration.radioTech != RadioTechType.INVALID) {
            outRegistration.setRadioTech(toRadioTech(inRegistration.radioTech));
        }

        outRegistration.setPAssociatedUris(inRegistration.pAssociatedUris);

        return outRegistration;
    }

    public static ArrayList<ServiceStatus> toServiceStatus(ServiceStatusInfo[] inList) {

        if (inList == null) {
            Log.e(TAG, "inList is null.");
            return null;
        }

        ServiceStatus[] outList =  copySrvStatusList(inList);
        ArrayList<ServiceStatus> response = new ArrayList<ServiceStatus>(Arrays.asList(outList));

        return response;
    }

    private static ServiceStatus[] copySrvStatusList(ServiceStatusInfo[] fromList) {
        ServiceStatus[] toList = null;
        if (fromList == null) {
            return null;
        }

        int listLen = fromList.length;
        Log.v(TAG, "Num of SrvUpdates = " + listLen);
        toList = new ServiceStatus[listLen];
        for (int i = 0; i < listLen; i++) {
            ServiceStatusInfo fromInfo = fromList[i];
            if (fromInfo != null) {
                toList[i] = toServiceStatus(fromInfo);
            } else {
                Log.e(TAG, "Null service status in list");
            }
        }
        return toList;
    }

    private static ServiceStatus toServiceStatus(ServiceStatusInfo fromInfo) {
        ServiceStatus toInfo = new ServiceStatus();
        toInfo.isValid = fromInfo.isValid;
        toInfo.type = toCallType(fromInfo.callType);
        if (fromInfo.accTechStatus.length > 0) {
          toInfo.accessTechStatus = unpackAccTechStatus(fromInfo);
        } else {
          toInfo.accessTechStatus = new ServiceStatus.StatusForAccessTech[1];
          toInfo.accessTechStatus[0] =
              new ServiceStatus.StatusForAccessTech();
          ServiceStatus.StatusForAccessTech act = toInfo.accessTechStatus[0];
          act.networkMode = RadioTech.RADIO_TECH_LTE;

          if (fromInfo.status != StatusType.INVALID) {
            act.status = toStatusType(fromInfo.status);
          }
          if (fromInfo.restrictCause != Integer.MAX_VALUE) {
            act.restrictCause = fromInfo.restrictCause;
          }
        }

        if (fromInfo.status != StatusType.INVALID) {
          toInfo.status = toStatusType(fromInfo.status);
        }

        if (fromInfo.restrictCause != Integer.MAX_VALUE &&
            fromInfo.restrictCause != CallDetails.CALL_RESTRICT_CAUSE_NONE &&
            fromInfo.status == StatusType.ENABLED) {
          Log.v(TAG, "Partially Enabled Status due to Restrict Cause");
          toInfo.status = ServiceStatus.STATUS_PARTIALLY_ENABLED;
        }

        toInfo.rttMode = toRttMode(fromInfo.rttMode);

        return toInfo;
    }

    private static int toCallType(int callType) {
        switch (callType) {
            case CallType.CALLCOMPOSER:
                return org.codeaurora.ims.CallDetails.CALL_TYPE_CALLCOMPOSER;
            case CallType.USSD:
                return org.codeaurora.ims.CallDetails.CALL_TYPE_USSD;
            case CallType.DC:
                return org.codeaurora.ims.CallDetails.CALL_TYPE_DC;
            case CallType.VOICE:
                return CallDetails.CALL_TYPE_VOICE;
            case CallType.VT_TX:
                return CallDetails.CALL_TYPE_VT_TX;
            case CallType.VT_RX:
                return CallDetails.CALL_TYPE_VT_RX;
            case CallType.VT:
                return CallDetails.CALL_TYPE_VT;
            case CallType.VT_NODIR:
                return CallDetails.CALL_TYPE_VT_NODIR;
            case CallType.CS_VS_TX:
                return CallDetails.CALL_TYPE_CS_VS_TX;
            case CallType.CS_VS_RX:
                return CallDetails.CALL_TYPE_CS_VS_RX;
            case CallType.PS_VS_TX:
                return CallDetails.CALL_TYPE_PS_VS_TX;
            case CallType.PS_VS_RX:
                return CallDetails.CALL_TYPE_PS_VS_RX;
            case CallType.SMS:
                return CallDetails.CALL_TYPE_SMS;
            case CallType.UT:
                return CallDetails.CALL_TYPE_UT;
            case CallType.UNKNOWN:
            default:
                return CallDetails.CALL_TYPE_UNKNOWN;
        }
    }

    /*
     *   Unpacks the status for access tech from Hal into proto object. The proto object is used to
     *   fill the array of ServiceStatus.StatusForAccessTech.
     *
     *   @param info Service status info from hal.
     *
     *   @return Array of ServiceStatus.StatusForAccessTech.
     *
     */
    private static ServiceStatus.StatusForAccessTech[] unpackAccTechStatus(ServiceStatusInfo info) {
        int statusListLen = info.accTechStatus.length;

        ServiceStatus.StatusForAccessTech[] statusForAccessTech =
                new ServiceStatus.StatusForAccessTech[statusListLen];

        for (int j = 0; j < statusListLen; j++) {
            statusForAccessTech[j] = new ServiceStatus.StatusForAccessTech();
            statusForAccessTech[j] = toStatusForAccessTech(info.accTechStatus[j]);
            Log.v(TAG, " networkMode = " + statusForAccessTech[j].networkMode +
                    " status = " + statusForAccessTech[j].status +
                    " restrictCause = " + statusForAccessTech[j].restrictCause +
                    " registered = " + statusForAccessTech[j].registered);
       }
       return statusForAccessTech;
    }

    private static ServiceStatus.StatusForAccessTech toStatusForAccessTech(
            StatusForAccessTech inStatus) {
        ServiceStatus.StatusForAccessTech outStatus = null;

        if (inStatus != null) {
            outStatus = new ServiceStatus.StatusForAccessTech();
            if (inStatus.networkMode != RadioTechType.INVALID) {
                outStatus.networkMode = toRadioTech(inStatus.networkMode);
            }

            if (inStatus.status != StatusType.INVALID) {
                outStatus.status = toStatusType(inStatus.status);
            }

            if (inStatus.restrictCause != Integer.MAX_VALUE) {
                outStatus.restrictCause = inStatus.restrictCause;
            }

            if (inStatus.hasRegistration) {
                if (inStatus.registration != null) { // Registered is
                                                     // optional field
                    outStatus.registered = toRegState(inStatus.registration.state);
                } else {
                    outStatus.registered = ImsRegistrationInfo.NOT_REGISTERED;;
                }
            }
        }
        return outStatus;
    }

    private static int toRadioTech(int radioTech) {
        switch (radioTech) {
            case RadioTechType.ANY:
                return RadioTech.RADIO_TECH_ANY;
            case RadioTechType.UNKNOWN:
                return RadioTech.RADIO_TECH_UNKNOWN;
            case RadioTechType.GPRS:
                return RadioTech.RADIO_TECH_GPRS;
            case RadioTechType.EDGE:
                return RadioTech.RADIO_TECH_EDGE;
            case RadioTechType.UMTS:
                return RadioTech.RADIO_TECH_UMTS;
            case RadioTechType.IS95A:
                return RadioTech.RADIO_TECH_IS95A;
            case RadioTechType.IS95B:
                return RadioTech.RADIO_TECH_IS95B;
            case RadioTechType.RTT_1X:
                return RadioTech.RADIO_TECH_1xRTT;
            case RadioTechType.EVDO_0:
                return RadioTech.RADIO_TECH_EVDO_0;
            case RadioTechType.EVDO_A:
                return RadioTech.RADIO_TECH_EVDO_A;
            case RadioTechType.HSDPA:
                return RadioTech.RADIO_TECH_HSDPA;
            case RadioTechType.HSUPA:
                return RadioTech.RADIO_TECH_HSUPA;
            case RadioTechType.HSPA:
                return RadioTech.RADIO_TECH_HSPA;
            case RadioTechType.EVDO_B:
                return RadioTech.RADIO_TECH_EVDO_B;
            case RadioTechType.EHRPD:
                return RadioTech.RADIO_TECH_EHRPD;
            case RadioTechType.LTE:
                return RadioTech.RADIO_TECH_LTE;
            case RadioTechType.HSPAP:
                return RadioTech.RADIO_TECH_HSPAP;
            case RadioTechType.GSM:
                return RadioTech.RADIO_TECH_GSM;
            case RadioTechType.TD_SCDMA:
                return RadioTech.RADIO_TECH_TD_SCDMA;
            case RadioTechType.WIFI:
                return RadioTech.RADIO_TECH_WIFI;
            case RadioTechType.IWLAN:
                return RadioTech.RADIO_TECH_IWLAN;
            case RadioTechType.NR5G:
                return RadioTech.RADIO_TECH_NR5G;
            case RadioTechType.C_IWLAN:
                return RadioTech.RADIO_TECH_C_IWLAN;
            default:
                return RadioTech.RADIO_TECH_INVALID;
        }
    }

    private static int toStatusType(int status) {
        switch (status) {
            case StatusType.DISABLED:
                return ServiceStatus.STATUS_DISABLED;
            case StatusType.PARTIALLY_ENABLED:
                return ServiceStatus.STATUS_PARTIALLY_ENABLED;
            case StatusType.ENABLED:
                return ServiceStatus.STATUS_ENABLED;
            case StatusType.NOT_SUPPORTED:
            default:
                return ServiceStatus.STATUS_NOT_SUPPORTED;
        }
    }

    public static int fromStatusType(int status) {
        switch (status) {
            case ServiceStatus.STATUS_DISABLED:
                return StatusType.DISABLED;
            case ServiceStatus.STATUS_PARTIALLY_ENABLED:
                return StatusType.PARTIALLY_ENABLED;
            case ServiceStatus.STATUS_ENABLED:
                return StatusType.ENABLED;
            case ServiceStatus.STATUS_NOT_SUPPORTED:
                return StatusType.NOT_SUPPORTED;
            default:
                return StatusType.INVALID;
        }
    }

    public static RadioState toRadioState(int radioState) {
        switch (radioState) {
            case vendor.qti.hardware.radio.ims.RadioState.OFF:
                return RadioState.RADIO_OFF;
            case vendor.qti.hardware.radio.ims.RadioState.UNAVAILABLE:
                return RadioState.RADIO_UNAVAILABLE;
            case vendor.qti.hardware.radio.ims.RadioState.ON:
                return RadioState.RADIO_ON;
            default:
                Log.e(TAG, "toRadioState: Invalid Radio State Change");
                return RadioState.RADIO_UNAVAILABLE;
        }
    }

    public static ConfInfo toConferenceInfo(ConferenceInfo conferenceInfo) {
        ConfInfo info = new ConfInfo();

        if (conferenceInfo.confInfoUri != null && conferenceInfo.confInfoUri.length > 0) {
            info.setConfInfoUri(ImsUtils.toByteArrayList(conferenceInfo.confInfoUri));
            Log.v(TAG, "onRefreshConferenceInfo: confUri = " + conferenceInfo.confInfoUri);
        }

        if (conferenceInfo.conferenceCallState != ConferenceCallState.INVALID) {
            info.setConfCallState(StableAidl.toConferenceCallState(
                    conferenceInfo.conferenceCallState));
        }
        Log.v(TAG, "onRefreshConferenceInfo: conferenceCallState = " +
                conferenceInfo.conferenceCallState);
        return info;
    }

    public static CallModify toCallModify(CallModifyInfo callModifyInfo) {
        CallModify callModify = new CallModify();

        callModify.call_details = toCallDetails(callModifyInfo.callDetails);

        if (callModifyInfo.callIndex != Integer.MAX_VALUE) {
            callModify.call_index = callModifyInfo.callIndex;
        }

        callModify.error = ImsErrorCode.SUCCESS;
        if (callModifyInfo.failCause != CallModifyFailCause.E_INVALID) {
            callModify.error = toCallModifyFailCause(callModifyInfo.failCause);
        }

        return callModify;
    }

    public static CallDetails toCallDetails(
            vendor.qti.hardware.radio.ims.CallDetails inCallDetails) {
        CallDetails outCallDetails = new CallDetails();

        outCallDetails.call_type = toCallType(inCallDetails.callType);

        if (inCallDetails.callDomain != CallDomain.INVALID) {
            outCallDetails.call_domain = toCallDomain(inCallDetails.callDomain);
        }

        if (inCallDetails.callSubstate != Integer.MAX_VALUE) {
            outCallDetails.callsubstate = toCallSubstateConstants(
                    inCallDetails.callSubstate);
        }

        if (inCallDetails.mediaId != Integer.MAX_VALUE) {
            outCallDetails.callMediaId = inCallDetails.mediaId;
        }

        outCallDetails.extras = new String[inCallDetails.extras.length];
        for (int i = 0; i < inCallDetails.extras.length; ++i) {
            outCallDetails.extras[i] = inCallDetails.extras[i];
        }

        outCallDetails.localAbility = copySrvStatusList(inCallDetails.localAbility);
        outCallDetails.peerAbility = copySrvStatusList(inCallDetails.peerAbility);

        if (inCallDetails.causeCode != Integer.MAX_VALUE) {
            outCallDetails.causeCode = inCallDetails.causeCode;
        }

        if (inCallDetails.rttMode != RttMode.INVALID) {
            outCallDetails.rttMode = toRttMode(inCallDetails.rttMode);
        }

        if (!inCallDetails.sipAlternateUri.isEmpty()) {
            outCallDetails.sipAlternateUri = inCallDetails.sipAlternateUri;
        }

        outCallDetails.setVosSupport(inCallDetails.isVosSupported);

        outCallDetails.setVisualizedVoiceCall(inCallDetails.isVisualizedVoiceCall);

        outCallDetails.setCrbtCall(inCallDetails.isCrbtCall);

        outCallDetails.setThreeDimensionalVideoType(inCallDetails.threeDimensionalVideoType);

        Log.v(TAG, "Call Details = " + outCallDetails);

        return outCallDetails;
    }

    private static int toCallSubstateConstants(int callSubstate) {
        switch (callSubstate) {
            case CallDetails.CALL_SUBSTATE_AUDIO_CONNECTED_SUSPENDED:
                return QtiCallConstants.CALL_SUBSTATE_AUDIO_CONNECTED_SUSPENDED;
            case CallDetails.CALL_SUBSTATE_VIDEO_CONNECTED_SUSPENDED:
                return QtiCallConstants.CALL_SUBSTATE_VIDEO_CONNECTED_SUSPENDED;
            case CallDetails.CALL_SUBSTATE_AVP_RETRY:
                return QtiCallConstants.CALL_SUBSTATE_AVP_RETRY;
            case CallDetails.CALL_SUBSTATE_MEDIA_PAUSED:
                return QtiCallConstants.CALL_SUBSTATE_MEDIA_PAUSED;
            case CallDetails.CALL_SUBSTATE_NONE:
            default:
                return QtiCallConstants.CALL_SUBSTATE_NONE;
        }
    }

    private static int toCallModifyFailCause(int failCause) {
        switch (failCause) {
            case CallModifyFailCause.E_SUCCESS:
                return ImsErrorCode.SUCCESS;
            case CallModifyFailCause.E_RADIO_NOT_AVAILABLE:
                return ImsErrorCode.RADIO_NOT_AVAILABLE;
            case CallModifyFailCause.E_GENERIC_FAILURE:
                return ImsErrorCode.GENERIC_FAILURE;
            case CallModifyFailCause.E_REQUEST_NOT_SUPPORTED:
                return ImsErrorCode.REQUEST_NOT_SUPPORTED;
            case CallModifyFailCause.E_CANCELLED:
                return ImsErrorCode.CANCELLED;
            case CallModifyFailCause.E_UNUSED:
                return ImsErrorCode.UNUSED;
            case CallModifyFailCause.E_INVALID_PARAMETER:
                return ImsErrorCode.INVALID_PARAMETER;
            case CallModifyFailCause.E_REJECTED_BY_REMOTE:
                return ImsErrorCode.REJECTED_BY_REMOTE;
            case CallModifyFailCause.E_IMS_DEREGISTERED:
                return ImsErrorCode.IMS_DEREGISTERED;
            case CallModifyFailCause.E_NETWORK_NOT_SUPPORTED:
                return ImsErrorCode.NETWORK_NOT_SUPPORTED;
            case CallModifyFailCause.E_HOLD_RESUME_FAILED:
                return ImsErrorCode.HOLD_RESUME_FAILED;
            case CallModifyFailCause.E_HOLD_RESUME_CANCELED:
                return ImsErrorCode.HOLD_RESUME_CANCELED;
            case CallModifyFailCause.E_REINVITE_COLLISION:
                return ImsErrorCode.REINVITE_COLLISION;
            default:
                return ImsErrorCode.SUCCESS;
        }
    }

    public static int toRingbackTone(int tone) {
        switch (tone) {
            case ToneOperation.START:
                return CallDetails.RINGBACK_TONE_START;
            case ToneOperation.STOP:
            case ToneOperation.INVALID:
            default:
                return CallDetails.RINGBACK_TONE_STOP;
        }
    }

    public static RegistrationBlockStatusInfo toRegistrationBlockStatus(
            vendor.qti.hardware.radio.ims.RegistrationBlockStatusInfo halRegBlockStatus) {
        RegistrationBlockStatusInfo regBlockStatus = new RegistrationBlockStatusInfo();

        if (halRegBlockStatus.blockStatusOnWwan != null &&
                halRegBlockStatus.blockStatusOnWwan.blockReason != BlockReasonType.INVALID) {
            regBlockStatus.setStatusOnWwan(toBlockStatus(halRegBlockStatus.blockStatusOnWwan));
        }

        if (halRegBlockStatus.blockStatusOnWlan != null &&
                halRegBlockStatus.blockStatusOnWlan.blockReason != BlockReasonType.INVALID) {
            regBlockStatus.setStatusOnWlan(toBlockStatus(halRegBlockStatus.blockStatusOnWlan));
        }

        return regBlockStatus;
    }

    private static BlockStatusInfo toBlockStatus(BlockStatus inBlockStatus) {
        if (inBlockStatus == null) {
            return null;
        }

        BlockStatusInfo outBlockStatus = new BlockStatusInfo();

        if (inBlockStatus.blockReason != BlockReasonType.INVALID) {
            outBlockStatus.setReason(toBlockReason(inBlockStatus.blockReason));
        }

        outBlockStatus.setReasonDetails(toBlockReasonDetails(inBlockStatus.blockReasonDetails));

        return outBlockStatus;
    }

    private static int toBlockReason(int inBlockReason) {
        switch (inBlockReason) {
            case BlockReasonType.PDP_FAILURE:
                return BlockStatusInfo.REASON_PDP_FAILURE;
            case BlockReasonType.REGISTRATION_FAILURE:
                return BlockStatusInfo.REASON_REGISTRATION_FAILURE;
            case BlockReasonType.HANDOVER_FAILURE:
                return BlockStatusInfo.REASON_HANDOVER_FAILURE;
            case BlockReasonType.OTHER_FAILURE:
                return BlockStatusInfo.REASON_OTHER_FAILURE;
            default:
                return BlockStatusInfo.REASON_INVALID;
        }
    }

    public static BlockReasonDetailsInfo toBlockReasonDetails(
            BlockReasonDetails inBlockReasonDetails) {
        BlockReasonDetailsInfo outBlockReasonDetails = new BlockReasonDetailsInfo();

        if (inBlockReasonDetails.regFailureReasonType != RegFailureReasonType.INVALID) {
            outBlockReasonDetails.setRegFailureReasonType(toRegFailureReason(
                    inBlockReasonDetails.regFailureReasonType));
        }

        if (inBlockReasonDetails.regFailureReason != Integer.MAX_VALUE) {
            outBlockReasonDetails.setRegFailureReason(inBlockReasonDetails.regFailureReason);
        }

        return outBlockReasonDetails;
    }

    public static int toRegFailureReason(int inRegFailureReasonType) {
        switch (inRegFailureReasonType) {
            case RegFailureReasonType.MOBILE_IP:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_MOBILE_IP;
            case RegFailureReasonType.INTERNAL:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_INTERNAL;
            case RegFailureReasonType.CALL_MANAGER_DEFINED:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_CALL_MANAGER_DEFINED;
            case RegFailureReasonType.TYPE_3GPP_SPEC_DEFINED:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_3GPP_SPEC_DEFINED;
            case RegFailureReasonType.PPP:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_PPP;
            case RegFailureReasonType.EHRPD:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_EHRPD;
            case RegFailureReasonType.IPV6:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_IPV6;
            case RegFailureReasonType.IWLAN:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_IWLAN;
            case RegFailureReasonType.HANDOFF:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_HANDOFF;
            case RegFailureReasonType.UNSPECIFIED:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_UNSPECIFIED;
            default:
                return BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_INVALID;
        }
    }

    public static int toConferenceCallState(int conferenceCallState) {
         switch (conferenceCallState) {
            case ConferenceCallState.RINGING:
                return ConfInfo.RINGING;
            case ConferenceCallState.FOREGROUND:
                return ConfInfo.FOREGROUND;
            case ConferenceCallState.BACKGROUND:
                return ConfInfo.BACKGROUND;
            default:
                return ConfInfo.INVALID;
        }
    }

    public static ParticipantStatusDetails toParticipantStatus(
            ParticipantStatusInfo participantStatusInfo) {

        if (participantStatusInfo == null) {
            return null;
        }

        ParticipantStatusDetails participantStatus = new ParticipantStatusDetails();

        if (participantStatusInfo.callId != Integer.MAX_VALUE) {
            participantStatus.setCallId(participantStatusInfo.callId);
        }

        if (participantStatusInfo.operation != ConfParticipantOperation.INVALID) {
            participantStatus.setOperation(toParticipantStatus(participantStatusInfo.operation));
        }

        participantStatus.setParticipantUri(participantStatusInfo.participantUri);

        if (participantStatusInfo.sipStatus != Integer.MAX_VALUE) {
            participantStatus.setSipStatus(participantStatusInfo.sipStatus);
        }

        participantStatus.setIsEct(participantStatusInfo.isEct);

        return participantStatus;
    }

    private static int toParticipantStatus(int type) {
        switch (type) {
            case ConfParticipantOperation.ADD:
                return ParticipantStatusDetails.ADD;
            case ConfParticipantOperation.REMOVE:
                return ParticipantStatusDetails.REMOVE;
            default:
                return ParticipantStatusDetails.INVALID;
        }
    }

    public static HoInfo toHandover(HandoverInfo inHandover) {

        if (inHandover == null) {
            return null;
        }

        HoInfo outHandover = new HoInfo();

        if (inHandover.type != HandoverType.INVALID) {
            outHandover.setType(toHandoverType(inHandover.type));
        }

        if (inHandover.srcTech != RadioTechType.INVALID) {
            outHandover.setSrcTech(toRadioTech(inHandover.srcTech));
        }

        if (inHandover.targetTech != RadioTechType.INVALID) {
            outHandover.setTargetTech(toRadioTech(inHandover.targetTech));
        }

        if (inHandover.hoExtra.type != ExtraType.INVALID) {
            outHandover.setExtra(toExtraType(inHandover.hoExtra.type),
                    inHandover.hoExtra.extraInfo);
        }

        outHandover.setErrorCode(inHandover.errorCode);
        outHandover.setErrorMessage(inHandover.errorMessage);

        return outHandover;
    }

    public static int toHandoverType(int inType) {
        switch (inType) {
            case HandoverType.START:
                return HoInfo.START;
            case HandoverType.COMPLETE_SUCCESS:
                return HoInfo.COMPLETE_SUCCESS;
            case HandoverType.COMPLETE_FAIL:
                return HoInfo.COMPLETE_FAIL;
            case HandoverType.CANCEL:
                return HoInfo.CANCEL;
            case HandoverType.NOT_TRIGGERED_MOBILE_DATA_OFF:
                return HoInfo.NOT_TRIGGERED_MOBILE_DATA_OFF;
            case HandoverType.NOT_TRIGGERED:
                return HoInfo.NOT_TRIGGERED;
            default:
                return HoInfo.INVALID;
        }
    }

    private static int toExtraType(int extraHo) {
        switch (extraHo) {
            case ExtraType.LTE_TO_IWLAN_HO_FAIL:
                return ExtraInfo.LTE_TO_IWLAN_HO_FAIL;
            default:
                return ExtraInfo.INVALID;
        }
    }

    public static CallComposerInfo toCallComposerInfo(
            vendor.qti.hardware.radio.ims.CallComposerInfo from) {
        if (from == null) {
            return null;
        }

        int priority = toPriority(from.priority);
        String subject = from.subject.length > 0 ? String.valueOf(from.subject) : "";
        CallComposerInfo.Location location = CallComposerInfo.Location.UNKNOWN;
        String organization = (from.organization != null && from.organization.length > 0)
                ? String.valueOf(from.organization) : "";
        if (from.location.radius != CallComposerInfo.Location.LOCATION_NOT_SET) {
            location = toLocation(from.location);
        }
        Uri imageUrl = Uri.parse(from.imageUrl);

        return new CallComposerInfo(priority, subject, imageUrl, location, organization);
    }

    private static CallComposerInfo.Location toLocation(CallLocation from) {

        CallComposerInfo.Location location =
                new CallComposerInfo.Location(from.radius, from.latitude, from.longitude);

        return location;
    }

    private static int toPriority(int priority) {
        switch (priority) {
            case vendor.qti.hardware.radio.ims.CallPriority.URGENT:
                return CallComposerInfo.PRIORITY_URGENT;
            default:
                return CallComposerInfo.PRIORITY_NORMAL;
        }
    }

    private static int toVerificationStatus(int verstatVerificationStatus) {
        switch (verstatVerificationStatus) {
            case VerificationStatus.VALIDATION_PASS:
                return VerstatInfo.VERSTAT_VERIFICATION_PASS;
            case VerificationStatus.VALIDATION_FAIL:
                return VerstatInfo.VERSTAT_VERIFICATION_FAIL;
            case VerificationStatus.VALIDATION_NONE:
            default:
                return VerstatInfo.VERSTAT_VERIFICATION_NONE;
        }
    }

    /**
     * Helper method to create DriverCallIms object with CallComposer and ecnam call info.
     */
    public static DriverCallIms toDriverCallIms(AutoCallRejectionInfo2 autoRejectionInfo) {

        if (autoRejectionInfo == null || autoRejectionInfo.autoCallRejectionInfo == null) {
            return null;
        }
        CallComposerInfo ccInfo = autoRejectionInfo.callComposerInfo == null ? null :
                toCallComposerInfo(autoRejectionInfo.callComposerInfo);
        EcnamInfo ecnamInfo = autoRejectionInfo.ecnamInfo == null ? null :
                toEcnamInfo(autoRejectionInfo.ecnamInfo);

        return toDriverCallIms(ccInfo, ecnamInfo,
                autoRejectionInfo.autoCallRejectionInfo.verificationStatus,
                autoRejectionInfo.autoCallRejectionInfo.autoRejectionCause,
                autoRejectionInfo.autoCallRejectionInfo.sipErrorCode,
                autoRejectionInfo.autoCallRejectionInfo.callType,
                autoRejectionInfo.autoCallRejectionInfo.number,
                autoRejectionInfo.isDcCall,
                autoRejectionInfo.callReason);
    }

    /**
     * Helper method to create DriverCallIms object with CallComposer and AutoReject call info.
     */
    public static DriverCallIms toDriverCallIms(CallComposerAutoRejectionInfo autoRejectionInfo) {

        if (autoRejectionInfo == null || autoRejectionInfo.autoCallRejectionInfo == null) {
            return null;
        }

        CallComposerInfo ccInfo = autoRejectionInfo.callComposerInfo == null ? null :
                toCallComposerInfo(autoRejectionInfo.callComposerInfo);

        return toDriverCallIms(ccInfo, null,
                autoRejectionInfo.autoCallRejectionInfo.verificationStatus,
                autoRejectionInfo.autoCallRejectionInfo.autoRejectionCause,
                autoRejectionInfo.autoCallRejectionInfo.sipErrorCode,
                autoRejectionInfo.autoCallRejectionInfo.callType,
                autoRejectionInfo.autoCallRejectionInfo.number,
                false,
                null);
    }

    private static DriverCallIms toDriverCallIms(CallComposerInfo ccInfo, EcnamInfo ecnamInfo,
            int verstatVerificationStatus, int autoRejectionCause, int SipErrorCode,
            int callType, String number, boolean isDcCall, String callReason) {
        VerstatInfo verstatInfo = new VerstatInfo(false /*canMarkUnwantedCall*/,
                toVerificationStatus(verstatVerificationStatus));
        DriverCallIms dc = new DriverCallIms(ccInfo, verstatInfo, ecnamInfo);

        int imsReasonInfoCode = StableAidlErrorCode.toImsReasonInfoCode(autoRejectionCause);
        imsReasonInfoCode = imsReasonInfoCode == ImsReasonInfo.CODE_UNSPECIFIED ?
                ImsReasonInfo.CODE_REJECT_UNKNOWN : imsReasonInfoCode;
        dc.callFailCause = new ImsReasonInfo(imsReasonInfoCode, SipErrorCode, null);
        dc.callDetails = new org.codeaurora.ims.CallDetails();
        dc.callDetails.call_type = toCallType(callType);
        dc.number = number;
        dc.isDcCall = isDcCall;
        dc.callReason = callReason;
        return dc;
    }

    public static DriverCallIms toDriverCallIms(AutoCallRejectionInfo autoRejectionInfo) {
        if (autoRejectionInfo == null) {
            return null;
        }

        return toDriverCallIms(null, null, autoRejectionInfo.verificationStatus,
                autoRejectionInfo.autoRejectionCause, autoRejectionInfo.sipErrorCode,
                autoRejectionInfo.callType, autoRejectionInfo.number, false, null);
    }

    public static ArrayList<DriverCallIms> toDriverCallImsArray(CallInfo[] callList) {
        ArrayList<DriverCallIms> response = new ArrayList<DriverCallIms>();

        for (int i = 0; i < callList.length; ++i) {
            DriverCallIms dc = toDriverCallIms(callList[i]);
            response.add(dc);
        }
        return response;
    }

    private static DriverCallIms toDriverCallIms(CallInfo call) {
        DriverCallIms dc = new DriverCallIms(new VerstatInfo(call.verstatInfo.canMarkUnwantedCall,
                toVerificationStatus(call.verstatInfo.verificationStatus)));
        if (call.state != CallState.INVALID) {
            dc.state = toCallState(call.state);
        }

        if (call.index != Integer.MAX_VALUE) {
            dc.index = call.index;
        }

        if (call.toa != Integer.MAX_VALUE) {
            dc.TOA = call.toa;
        }

        dc.isMpty = call.isMpty;
        dc.isMT = call.isMT;

        if (call.als != Integer.MAX_VALUE) {
            dc.als = call.als;
        }

        dc.isVoice = call.isVoice;

        dc.isVoicePrivacy = call.isVoicePrivacy;

        dc.numberPresentation = DriverCallIms.presentationFromCLIP(
                call.numberPresentation);
        dc.name = call.name;
        dc.namePresentation = DriverCallIms.presentationFromCLIP(
                call.namePresentation);

        dc.isEncrypted = call.isEncrypted;

        dc.isTirOverwriteAllowed = isTirOverwriteAllowed(call.tirMode);

        toCrsData(call.crsData, dc.crsData);
        dc.isPreparatory = call.isPreparatory;

        dc.historyInfo = call.historyInfo;
        dc.diversionInfo = call.diversionInfo;

        dc.mConfSupported = DriverCallIms.CONF_SUPPORT_INDICATED |
                (call.isVideoConfSupported ? DriverCallIms.CONF_VIDEO_SUPPORTED :
                DriverCallIms.CONF_SUPPORT_NONE);

        dc.callDetails = toCallDetails(call.callDetails);

        dc.number = call.number;
        dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);

        toCallProgressInfo(call.callProgInfo, dc.callProgressInfo);

        int imsReasonCode = ImsReasonInfo.CODE_UNSPECIFIED;
        int qtiReasonCode = QtiCallConstants.CODE_UNSPECIFIED;
        int imsReasonExtraCode = ImsReasonInfo.CODE_UNSPECIFIED;
        String imsReasonExtraMessage = null;
        final int failCause = call.failCause.failCause;

        if (failCause != CallFailCause.INVALID) {
            String networkError = null;
            // Check for an error message from the network.
            if (call.failCause.errorDetails != null &&
                    !call.failCause.errorDetails.errorString.isEmpty()) {
                networkError = call.failCause.errorDetails.errorString;
                Log.v(TAG, "CallFailCauseResponse: error string = " + networkError);
            }

            // Check if the CallFailCauseResponse has an error code.
            imsReasonCode = StableAidlErrorCode.toImsReasonInfoCode(failCause);
            qtiReasonCode = StableAidlErrorCode.toQtiCallFailReasonCode(failCause);

            if (call.failCause.hasErrorDetails &&
                    (call.failCause.errorDetails.errorCode != Integer.MAX_VALUE)) {
                imsReasonExtraCode = call.failCause.errorDetails.errorCode;
            } else {
                Log.d(TAG, "CallFailCauseResponse has no int error code!");
            }

            dc.mCallFailReason = toSipError(failCause);
            // If there is a network error, propagate it through
            // the ImsReasonInfo object.
            if (networkError != null) {
                imsReasonExtraMessage = networkError;
            }
        } else {
            Log.d(TAG, "CallFailCauseResponse failCause is Invalid");
        }

        dc.callFailCause = new ImsReasonInfo(imsReasonCode, imsReasonExtraCode,
                imsReasonExtraMessage);
        dc.mAdditionalCallFailCause = qtiReasonCode == QtiCallConstants.CODE_UNSPECIFIED ?
                                   imsReasonCode : qtiReasonCode;

        Log.v(TAG, "Qti Call fail cause= " + dc.mAdditionalCallFailCause);

        //Multi Line Information
        dc.mMtMultiLineInfo = toMultiIdentityLineInfo(call.mtMultiLineInfo);

        dc.additionalCallInfo.setCode(call.additionalCallInfo != null ?
                StableAidlErrorCode.toMsimAdditionalCallInfoCode(
                call.additionalCallInfo.additionalCode) : QtiCallConstants.CODE_UNSPECIFIED);

        if (call.audioQuality != null) {
            dc.audioQuality = new AudioQuality(toCodec(call.audioQuality.codec),
                    toComputedAudioQuality(call.audioQuality.computedAudioQuality));
        }

        dc.modemCallId = call.modemCallId;
        dc.isCalledPartyRinging = call.isCalledPartyRinging;
        dc.isEmergency = call.isEmergency;
        dc.callReason = call.callReason;
        return dc;
    }

    private static DriverCallIms.State toCallState(int inCallState) {
        switch (inCallState) {
            case CallState.ACTIVE:
                return DriverCallIms.State.ACTIVE;
            case CallState.HOLDING:
                return DriverCallIms.State.HOLDING;
            case CallState.DIALING:
                return DriverCallIms.State.DIALING;
            case CallState.ALERTING:
                return DriverCallIms.State.ALERTING;
            case CallState.INCOMING:
                return DriverCallIms.State.INCOMING;
            case CallState.WAITING:
                return DriverCallIms.State.WAITING;
            case CallState.END:
            default:
                return DriverCallIms.State.END;
        }
    }

    // Map TirMode to a boolean value, where if mode is temporary return true
    // all other values return false
    private static boolean isTirOverwriteAllowed(int tirMode) {
        return tirMode == TirMode.TEMPORARY;
    }

    private static int toCrsType(int type) {
        switch (type) {
            case CrsType.INVALID:
                return QtiCallConstants.CRS_TYPE_INVALID;
            case CrsType.AUDIO:
                return QtiCallConstants.CRS_TYPE_AUDIO;
            case CrsType.VIDEO:
                return QtiCallConstants.CRS_TYPE_VIDEO;
            case CrsType.AUDIO_VIDEO:
                return QtiCallConstants.CRS_TYPE_AUDIO
                    | QtiCallConstants.CRS_TYPE_VIDEO;
            default:
                return QtiCallConstants.CRS_TYPE_INVALID;
        }
    }

    private static void toCrsData(vendor.qti.hardware.radio.ims.CrsData from, CrsData to) {
        to.setCrsType(toCrsType(from.type));
        to.setOriginalCallType(toCallType(from.originalCallType));
    }

    private static int toCallProgressInfoType(int type) {
        switch (type) {
            case CallProgressInfoType.INVALID:
                return QtiCallConstants.CALL_PROGRESS_INFO_TYPE_INVALID;
            case CallProgressInfoType.CALL_REJ_Q850:
                return QtiCallConstants.CALL_PROGRESS_INFO_TYPE_CALL_REJ_Q850;
            case CallProgressInfoType.CALL_WAITING:
                return QtiCallConstants.CALL_PROGRESS_INFO_TYPE_CALL_WAITING;
            case CallProgressInfoType.CALL_FORWARDING:
                return QtiCallConstants.CALL_PROGRESS_INFO_TYPE_CALL_FORWARDING;
            case CallProgressInfoType.REMOTE_AVAILABLE:
                return QtiCallConstants.CALL_PROGRESS_INFO_TYPE_REMOTE_AVAILABLE;
            default:
                return QtiCallConstants.CALL_PROGRESS_INFO_TYPE_INVALID;
        }
    }

    private static void toCallProgressInfo(
            vendor.qti.hardware.radio.ims.CallProgressInfo from, CallProgressInfo to) {
        to.setType(toCallProgressInfoType(from.type));

        if (from.reasonCode != Short.MAX_VALUE) {
            to.setReasonCode(from.reasonCode);
        }

        if (from.reasonText != null && !from.reasonText.isEmpty()) {
            to.setReasonText(from.reasonText);
        }
    }

    private static int toCodec(int codec) {
        switch (codec) {
            case Codec.QCELP13K:
                return ImsStreamMediaProfile.AUDIO_QUALITY_QCELP13K;
            case Codec.EVRC:
                return ImsStreamMediaProfile.AUDIO_QUALITY_EVRC;
            case Codec.EVRC_B:
                return ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_B;
            case Codec.EVRC_WB:
                return ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_WB;
            case Codec.EVRC_NW:
                return ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_NW;
            case Codec.AMR_NB:
                return ImsStreamMediaProfile.AUDIO_QUALITY_AMR;
            case Codec.AMR_WB:
                return ImsStreamMediaProfile.AUDIO_QUALITY_AMR_WB;
            case Codec.GSM_EFR:
                return ImsStreamMediaProfile.AUDIO_QUALITY_GSM_EFR;
            case Codec.GSM_FR:
                return ImsStreamMediaProfile.AUDIO_QUALITY_GSM_FR;
            case Codec.GSM_HR:
                return ImsStreamMediaProfile.AUDIO_QUALITY_GSM_HR;
            case Codec.G711U:
                return ImsStreamMediaProfile.AUDIO_QUALITY_G711U;
            case Codec.G723:
                return ImsStreamMediaProfile.AUDIO_QUALITY_G723;
            case Codec.G711A:
                return ImsStreamMediaProfile.AUDIO_QUALITY_G711A;
            case Codec.G722:
                return ImsStreamMediaProfile.AUDIO_QUALITY_G722;
            case Codec.G711AB:
                return ImsStreamMediaProfile.AUDIO_QUALITY_G711AB;
            case Codec.G729:
                return ImsStreamMediaProfile.AUDIO_QUALITY_G729;
            case Codec.EVS_NB:
                return ImsStreamMediaProfile.AUDIO_QUALITY_EVS_NB;
            case Codec.EVS_WB:
                return ImsStreamMediaProfile.AUDIO_QUALITY_EVS_WB;
            case Codec.EVS_SWB:
                return ImsStreamMediaProfile.AUDIO_QUALITY_EVS_SWB;
            case Codec.EVS_FB:
                return ImsStreamMediaProfile.AUDIO_QUALITY_EVS_FB;
            case Codec.INVALID:
            default:
                return ImsStreamMediaProfile.AUDIO_QUALITY_NONE;
        }
    }

    private static int toComputedAudioQuality(int computedAudioQuality) {
        switch (computedAudioQuality) {
            case ComputedAudioQuality.NO_HD:
                return QtiCallConstants.CALL_AUDIO_QUALITY_NO_HD;
            case ComputedAudioQuality.HD:
                return QtiCallConstants.CALL_AUDIO_QUALITY_HD;
            case ComputedAudioQuality.HD_PLUS:
                return QtiCallConstants.CALL_AUDIO_QUALITY_HD_PLUS;
            case ComputedAudioQuality.INVALID:
            default:
                return AudioQuality.INVALID;
        }
    }

    public static int toSipError(int callFailCause) {
        switch (callFailCause) {
            case CallFailCause.SIP_FORBIDDEN:
                return ImsUtils.SIP_FORBIDDEN;
            case CallFailCause.SIP_REQUEST_TIMEOUT:
                return ImsUtils.SIP_REQUEST_TIMEOUT;
            case CallFailCause.SIP_TEMPORARILY_UNAVAILABLE:
                return ImsUtils.SIP_TEMPORARILY_UNAVAILABLE;
            case CallFailCause.SIP_SERVER_INTERNAL_ERROR:
                return ImsUtils.SIP_SERVER_INTERNAL_ERROR;
            case CallFailCause.SIP_SERVER_NOT_IMPLEMENTED:
                return ImsUtils.SIP_SERVER_NOT_IMPLEMENTED;
            case CallFailCause.SIP_SERVER_BAD_GATEWAY:
                return ImsUtils.SIP_SERVER_BAD_GATEWAY;
            case CallFailCause.SIP_SERVICE_UNAVAILABLE:
                return ImsUtils.SIP_SERVICE_UNAVAILABLE;
            case CallFailCause.SIP_SERVER_VERSION_UNSUPPORTED:
                return ImsUtils.SIP_SERVER_VERSION_UNSUPPORTED;
            case CallFailCause.SIP_SERVER_MESSAGE_TOOLARGE:
                return ImsUtils.SIP_SERVER_MESSAGE_TOOLARGE;
            case CallFailCause.SIP_SERVER_PRECONDITION_FAILURE:
                return ImsUtils.SIP_SERVER_PRECONDITION_FAILURE;
            default:
                return ImsUtils.SIP_UNKNOWN;
        }
    }

    private static int fromTtyMode(int ttyMode) {
        switch(ttyMode) {
            case CallDetails.TTY_MODE_FULL:
                return TtyMode.FULL;
            case CallDetails.TTY_MODE_HCO:
                return TtyMode.HCO;
            case CallDetails.TTY_MODE_VCO:
                return TtyMode.VCO;
            case CallDetails.TTY_MODE_OFF:
                return TtyMode.OFF;
            default:
                 return TtyMode.INVALID;
        }
    }

    public static CallForwardInfo fromImsCallForwardTimerInfo(int cfReason, int serviceClass,
            String number, int action, int timeSeconds, int startHour, int startMinute,
            int endHour, int endMinute) {
        return fromImsCallForwardTimerInfo(cfReason, serviceClass, number, action, timeSeconds,
                startHour, startMinute, endHour, endMinute, false /*expectMore*/);
    }

    public static CallForwardInfo fromImsCallForwardTimerInfo(int cfReason, int serviceClass,
           String number, int action, int timeSeconds, boolean expectMore) {
        return fromImsCallForwardTimerInfo(cfReason, serviceClass, number, action, timeSeconds,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                expectMore);
    }

    public static CallForwardInfo fromImsCallForwardTimerInfo(int cfReason, int serviceClass,
            String number, int action, int timeSeconds, int startHour, int startMinute,
            int endHour, int endMinute, boolean expectMore) {
        CallForwardInfo cfInfo = new CallForwardInfo();
        /* Although the field is called status but incase of set call forward request we use it
           to convey action. In case of query call forward response, the field conveys status. */
        cfInfo.status = action;
        cfInfo.reason = cfReason;
        cfInfo.serviceClass = serviceClass;
        cfInfo.toa = PhoneNumberUtils.toaFromString(number);
        cfInfo.number = "";
        if (number != null) {
            cfInfo.number = number;
        }
        cfInfo.timeSeconds = timeSeconds;
        cfInfo.expectMore = expectMore;
        cfInfo.callFwdTimerStart = new CallFwdTimerInfo();
        cfInfo.callFwdTimerEnd = new CallFwdTimerInfo();
        fromCallForwardTimerInfo(cfInfo.callFwdTimerStart, startHour, startMinute);
        fromCallForwardTimerInfo(cfInfo.callFwdTimerEnd, endHour, endMinute);
        return cfInfo;
    }

    public static ImsCallForwardTimerInfo[] toImsCallForwardTimerInfo(
            CallForwardInfo[] inCfInfoList) {
        ImsCallForwardTimerInfo outCfInfoList[] = null;
        if (inCfInfoList != null) {
            int infoListSize = inCfInfoList.length;
            outCfInfoList = new  ImsCallForwardTimerInfo[infoListSize];
            for (int i = 0; i < infoListSize; i++) {
                outCfInfoList[i] = new ImsCallForwardTimerInfo();
                CallForwardInfo inCfInfo = inCfInfoList[i];
                if (inCfInfo.status != Integer.MAX_VALUE) {
                    outCfInfoList[i].status = inCfInfo.status;
                }
                if (inCfInfo.reason != Integer.MAX_VALUE) {
                    outCfInfoList[i].reason = inCfInfo.reason;
                }
                if (inCfInfo.serviceClass != Integer.MAX_VALUE) {
                    outCfInfoList[i].serviceClass = inCfInfo.serviceClass;
                }
                if (inCfInfo.toa != Integer.MAX_VALUE) {
                    outCfInfoList[i].toa = inCfInfo.toa;
                }
                outCfInfoList[i].number = inCfInfo.number;
                if (inCfInfo.timeSeconds != Integer.MAX_VALUE) {
                    outCfInfoList[i].timeSeconds = inCfInfo.timeSeconds;
                }
                CallFwdTimerInfo startCallTimerInfo = inCfInfo.callFwdTimerStart;
                if (startCallTimerInfo.hour != Integer.MAX_VALUE) {
                    outCfInfoList[i].startHour = startCallTimerInfo.hour;
                }
                if (startCallTimerInfo.minute != Integer.MAX_VALUE) {
                    outCfInfoList[i].startMinute = startCallTimerInfo.minute;
                }
                CallFwdTimerInfo endCallTimerInfo = inCfInfo.callFwdTimerEnd;
                if (endCallTimerInfo.hour != Integer.MAX_VALUE) {
                    outCfInfoList[i].endHour = endCallTimerInfo.hour;
                }
                if (endCallTimerInfo.minute != Integer.MAX_VALUE) {
                    outCfInfoList[i].endMinute = endCallTimerInfo.minute;
                }
            }
        } else {
            outCfInfoList = new ImsCallForwardTimerInfo[0];
            Log.v(StableAidl.class, "inCfInfoList is null.");
        }
        return outCfInfoList;
    }

    public static void fromCallForwardTimerInfo(CallFwdTimerInfo callFwdTimerInfo, int hour,
            int minute) {
        callFwdTimerInfo.year = Integer.MAX_VALUE;
        callFwdTimerInfo.month = Integer.MAX_VALUE;
        callFwdTimerInfo.day = Integer.MAX_VALUE;
        callFwdTimerInfo.hour = hour;
        callFwdTimerInfo.minute = minute;
        callFwdTimerInfo.second = Integer.MAX_VALUE;
        callFwdTimerInfo.timezone = Integer.MAX_VALUE;
    }

    public static CallForwardStatusInfo toCallForwardStatusInfo(
            vendor.qti.hardware.radio.ims.CallForwardStatusInfo imsRadioCFStatusInfo) {
        return new CallForwardStatusInfo(toSipError(
                imsRadioCFStatusInfo.errorDetails),
                toCallForwardStatus(imsRadioCFStatusInfo.status));
    }

    private static int toCfStatus(int status) {
        switch(status) {
            case Result.SUCCESS:
               return CallForwardStatus.SUCCESS;
            case Result.FAILURE:
            default:
               return CallForwardStatus.FAILED;
        }
    }

    private static CallForwardStatus[] toCallForwardStatus(
        vendor.qti.hardware.radio.ims.CallForwardStatus[] fromList) {
        // CallForwardStatus will be valid only for CF_All and CF_ALL_CONDITIONAL
        // other CF_* reasons lower layers will set size of CallForwardStatus array as 0.
        if (fromList == null) {
            return null;
        }

        int size = fromList.length;
        CallForwardStatus[] toList = new CallForwardStatus[size];

        for (int i = 0; i < size; i++) {
            vendor.qti.hardware.radio.ims.CallForwardStatus fromCfStatus = fromList[i];
            toList[i] = new CallForwardStatus(fromCfStatus.reason, toCfStatus(fromCfStatus.status),
                    toSipError(fromCfStatus.errorDetails));
        }
        return toList;
    }

    public static int toSuppSvcStatus(int inServiceStatus) {
        switch(inServiceStatus) {
            case ServiceClassStatus.ENABLED:
                return SuppSvcResponse.ENABLED;
            case ServiceClassStatus.DISABLED:
                return SuppSvcResponse.DISABLED;
            default:
                return SuppSvcResponse.INVALID;
        }
    }

    public static ExplicitCallTransferInfo fromEctInfo(int srcCallId, int type,
            String number, int destCallId) {
        ExplicitCallTransferInfo ectInfo = new ExplicitCallTransferInfo();
        ectInfo.callId = srcCallId;
        ectInfo.ectType = fromEctTypeInfo(type);
        ectInfo.targetAddress = "";
        if (number != null) {
            ectInfo.targetAddress = number;
        }
        ectInfo.targetCallId = destCallId > 0 ? destCallId : Integer.MAX_VALUE;
        return ectInfo;
    }

    private static int fromEctTypeInfo(int type) {
        switch (type) {
            case EctTypeInfo.BLIND_TRANSFER:
                return EctType.BLIND_TRANSFER;
            case EctTypeInfo.ASSURED_TRANSFER:
                return EctType.ASSURED_TRANSFER;
            case EctTypeInfo.CONSULTATIVE_TRANSFER:
                return EctType.CONSULTATIVE_TRANSFER;
            default:
                return EctType.INVALID;
        }
    }

    private static  CbNumListInfo fromCbNumList(String[] inCbNumList, int serviceClass) {
        CbNumListInfo outCbNumListInfo = new CbNumListInfo();
        outCbNumListInfo.cbNumInfo = new CbNumInfo[1];
        outCbNumListInfo.cbNumInfo[0] = new CbNumInfo();
        outCbNumListInfo.cbNumInfo[0].number = "";
        if (serviceClass != Integer.MAX_VALUE) {
            outCbNumListInfo.serviceClass = serviceClass;
        }
        if (inCbNumList == null) {
            return outCbNumListInfo;
        }
        outCbNumListInfo.cbNumInfo = new CbNumInfo[inCbNumList.length];
        for (int i = 0; i < inCbNumList.length; ++i) {
            CbNumInfo cbNumInfo = new CbNumInfo();
            cbNumInfo.status = ServiceClassStatus.INVALID;
            if (inCbNumList[i] != null) {
                cbNumInfo.number = inCbNumList[i];
            }
            outCbNumListInfo.cbNumInfo[i] = cbNumInfo;
        }
        return outCbNumListInfo;
    }

    private static int fromFacility(int facilityType) {
        switch (facilityType) {
            case SuppSvcResponse.FACILITY_CLIP:
                return  FacilityType.CLIP;
            case SuppSvcResponse.FACILITY_COLP:
                return  FacilityType.COLP;
            case SuppSvcResponse.FACILITY_BAOC:
                return  FacilityType.BAOC;
            case SuppSvcResponse.FACILITY_BAOIC:
                return  FacilityType.BAOIC;
            case SuppSvcResponse.FACILITY_BAOICxH:
                return  FacilityType.BAOICxH;
            case SuppSvcResponse.FACILITY_BAIC:
                return  FacilityType.BAIC;
            case SuppSvcResponse.FACILITY_BAICr:
                return  FacilityType.BAICr;
            case SuppSvcResponse.FACILITY_BA_ALL:
                return  FacilityType.BA_ALL;
            case SuppSvcResponse.FACILITY_BA_MO:
                return  FacilityType.BA_MO;
            case SuppSvcResponse.FACILITY_BA_MT:
                return  FacilityType.BA_MT;
            case SuppSvcResponse.FACILITY_BS_MT:
                return  FacilityType.BS_MT;
            case SuppSvcResponse.FACILITY_BAICa:
                return  FacilityType.BAICa;
            default:
                return  FacilityType.INVALID;
        }
    }

    public static SuppSvcResponse toSuppSvcResponse(SuppServiceStatus suppServiceStatus) {
        SuppSvcResponse suppSvcResponse = new SuppSvcResponse();
        suppSvcResponse.setStatus(toServiceClassStatus(suppServiceStatus.status));
        suppSvcResponse.setProvisionStatus(
                toServiceClassProvisionStatus(suppServiceStatus.provisionStatus));
        if (suppServiceStatus.facilityType !=  FacilityType.INVALID) {
            suppSvcResponse.setFacilityType(toFacility(suppServiceStatus.facilityType));
        }
        if (suppServiceStatus.failureCause != null &&
                !suppServiceStatus.failureCause.isEmpty()) {
            suppSvcResponse.setFailureCause(suppServiceStatus.failureCause);
        }
        for (CbNumListInfo numList : suppServiceStatus.cbNumListInfo) {
            if (numList.serviceClass == Integer.MAX_VALUE) {
                continue;
            }
            SuppSvcResponse.BarredLines lines =
                    new SuppSvcResponse.BarredLines(numList.serviceClass);

            if (numList.cbNumInfo != null) {
                for (CbNumInfo numInfo : numList.cbNumInfo) {
                    SuppSvcResponse.LineStatus lineStatus =
                        new SuppSvcResponse.LineStatus(numInfo.status,
                        numInfo.number);
                    lines.addLine(lineStatus);
                }
            }
            suppSvcResponse.addBarredLines(lines);
        }

        if (suppServiceStatus.hasErrorDetails) {
            suppSvcResponse.setErrorDetails(toSipError(suppServiceStatus.errorDetails));
        }
        suppSvcResponse.setPasswordRequired(suppServiceStatus.isPasswordRequired);
        return suppSvcResponse;
    }

    private static int toFacility(int facilityType) {
        switch (facilityType) {
            case  FacilityType.CLIP:
                return SuppSvcResponse.FACILITY_CLIP;
            case  FacilityType.COLP:
                return SuppSvcResponse.FACILITY_COLP;
            case  FacilityType.BAOC:
                return SuppSvcResponse.FACILITY_BAOC;
            case  FacilityType.BAOICxH:
                return SuppSvcResponse.FACILITY_BAOICxH;
            case  FacilityType.BAIC:
                return SuppSvcResponse.FACILITY_BAIC;
            case  FacilityType.BAICr:
                return SuppSvcResponse.FACILITY_BAICr;
            case  FacilityType.BA_ALL:
                return SuppSvcResponse.FACILITY_BA_ALL;
            case  FacilityType.BA_MO:
                return SuppSvcResponse.FACILITY_BA_MO;
            case  FacilityType.BA_MT:
                return SuppSvcResponse.FACILITY_BA_MT;
            case  FacilityType.BS_MT:
                return SuppSvcResponse.FACILITY_BS_MT;
            case  FacilityType.BAICa:
                return SuppSvcResponse.FACILITY_BAICa;
            case  FacilityType.BAOIC:
                return SuppSvcResponse.FACILITY_BAOIC;
            case  FacilityType.INVALID:
            default:
                return SuppSvcResponse.FACILITY_BA_ALL;
        }
    }

    private static AddressInfo fromAddress(double lat, double lon, Address address) {
        String info;
        AddressInfo addressInfo = new AddressInfo();
        addressInfo.city = "";
        addressInfo.state = "";
        addressInfo.country = "";
        addressInfo.postalCode = "";
        addressInfo.countryCode = "";
        addressInfo.street = "";
        addressInfo.houseNumber = "";
        if (address == null) {
            Log.v(StableAidl.class, "Null Address!");
            return addressInfo;
        }
        info = address.getLocality();
        if (info != null) {
            addressInfo.city = info;
        }
        info = address.getAdminArea();
        if (info != null) {
            addressInfo.state = info;
        }
        info = address.getCountryName();
        if (info != null) {
            addressInfo.country = info;
        }
        info = address.getPostalCode();
        if (info != null) {
            addressInfo.postalCode = info;
        }
        info = address.getCountryCode();
        if (info != null) {
            addressInfo.countryCode = info;
        }
        info = address.getThoroughfare();
        if (info != null) {
            addressInfo.street = info;
        }
        info = address.getSubThoroughfare();
        if (info != null) {
            addressInfo.houseNumber = info;
        }
        Log.v(StableAidl.class, "address=" + address + ",houseNumber=" +
                address.getSubThoroughfare());
        return addressInfo;
    }

    private static int fromSmsDeliverStatus(int status) {
        switch (status){
            case ImsSmsImplBase.DELIVER_STATUS_OK:
                return SmsDeliverStatus.OK;
            case ImsSmsImplBase.DELIVER_STATUS_ERROR_GENERIC:
                return SmsDeliverStatus.ERROR;
            case ImsSmsImplBase.DELIVER_STATUS_ERROR_NO_MEMORY:
                return SmsDeliverStatus.ERROR_NO_MEMORY;
            case ImsSmsImplBase.DELIVER_STATUS_ERROR_REQUEST_NOT_SUPPORTED:
                return SmsDeliverStatus.ERROR_REQUEST_NOT_SUPPORTED;
            default:
                Log.e(StableAidl.class, "unknown deliver status");
                return SmsDeliverStatus.ERROR;
        }
    }

    private static int fromSmsReportStatus(int report) {
        switch (report){
            case ImsSmsImplBase.STATUS_REPORT_STATUS_OK:
                return SmsReportStatus.OK;
            case ImsSmsImplBase.STATUS_REPORT_STATUS_ERROR:
                return SmsReportStatus.ERROR;
            default:
                return SmsReportStatus.ERROR;
        }
    }

    public static int toTtyMode(int ttyMode) {
        switch(ttyMode) {
            case TtyMode.OFF:
                return CallDetails.TTY_MODE_OFF;
            case TtyMode.FULL:
                return CallDetails.TTY_MODE_FULL;
            case TtyMode.HCO:
                return CallDetails.TTY_MODE_HCO;
            case TtyMode.VCO:
                return CallDetails.TTY_MODE_VCO;
            default:
                 return CallDetails.TTY_MODE_OFF;
        }
    }

    public static SuppNotifyInfo toSuppNotifyInfo( SuppServiceNotification inNotification) {
        if (inNotification == null) {
            return null;
        }
        SuppNotifyInfo outNotification = new SuppNotifyInfo();
        if (inNotification.notificationType != NotificationType.INVALID) {
            outNotification.setNotificationType(toSuppNotifyInfo(
                    inNotification.notificationType));
        }
        if (inNotification.code != Integer.MAX_VALUE) {
            outNotification.setCode(inNotification.code);
        }
        if (inNotification.index != Integer.MAX_VALUE) {
            outNotification.setIndex(inNotification.index);
        }
        if (inNotification.type != Integer.MAX_VALUE) {
            outNotification.setType(inNotification.type);
        }
        if (inNotification.connId != Integer.MAX_VALUE) {
            outNotification.setConnId(inNotification.connId);
        }
        outNotification.setNumber(inNotification.number);
        outNotification.setHistoryInfo(inNotification.historyInfo);
        if (inNotification.hasHoldTone) {
            outNotification.setHoldTone(inNotification.holdTone);
        }
        return outNotification;
    }

    private static int toSuppNotifyInfo(int inNotificationType) {
        switch (inNotificationType) {
            case NotificationType.MO:
                return SuppNotifyInfo.MO;
            case NotificationType.MT:
                return SuppNotifyInfo.MT;
            default:
                return Integer.MAX_VALUE;
        }
    }

    public static int[] toVoWiFiQuality(int voWiFiCallQuality) {
        int[] ret = new int[1];
        switch (voWiFiCallQuality) {
            case VoWiFiCallQuality.EXCELLENT:
                ret[0] = VoWiFiQuality.QUALITY_EXCELLENT;
                break;
            case VoWiFiCallQuality.FAIR:
                ret[0] = VoWiFiQuality.QUALITY_FAIR;
                break;
            case VoWiFiCallQuality.BAD:
                ret[0] = VoWiFiQuality.QUALITY_BAD;
                break;
            default:
                ret[0] = VoWiFiQuality.QUALITY_NONE;
                break;
        }
        return ret;
    }

    public static UssdInfo toUssdInfo(int type, String msg,
            SipErrorInfo errorDetails) {
        return new UssdInfo(toUssdModeType(type), msg,
                    errorDetails.errorCode,
                    errorDetails.errorString);
    }

    private static int toUssdModeType(int type) {
        switch (type) {
            case UssdModeType.NOTIFY:
                return ImsPhoneCommandsInterface.USSD_MODE_NOTIFY;
            case UssdModeType.REQUEST:
                return ImsPhoneCommandsInterface.USSD_MODE_REQUEST;
            case UssdModeType.NW_RELEASE:
                return ImsPhoneCommandsInterface.USSD_MODE_NW_RELEASE;
            case UssdModeType.LOCAL_CLIENT:
                return ImsPhoneCommandsInterface.USSD_MODE_LOCAL_CLIENT;
            case UssdModeType.NOT_SUPPORTED:
                return ImsPhoneCommandsInterface.USSD_MODE_NOT_SUPPORTED;
            case UssdModeType.NW_TIMEOUT:
                return ImsPhoneCommandsInterface.USSD_MODE_NW_TIMEOUT;
            default:
                return UssdInfo.INVALID;
        }
    }

    public static int toGeoLocationStatus(int geoLocationDataStatus) {
        switch(geoLocationDataStatus) {
            case GeoLocationDataStatus.TIMEOUT:
                return QtiCallConstants.REG_ERROR_GEO_LOCATION_STATUS_TIMEOUT;
            case GeoLocationDataStatus.NO_CIVIC_ADDRESS:
                return QtiCallConstants.REG_ERROR_GEO_LOCATION_STATUS_NO_CIVIC_ADDRESS;
            case GeoLocationDataStatus.ENGINE_LOCK:
                return QtiCallConstants.REG_ERROR_GEO_LOCATION_STATUS_ENGINE_LOCK;
            case GeoLocationDataStatus.RESOLVED:
                return QtiCallConstants.REG_ERROR_GEO_LOCATION_STATUS_RESOLVED;
            default:
                return QtiCallConstants.REG_ERROR_GEO_LOCATION_STATUS_RESOLVED;
        }
    }

    public static int toServiceType(int serviceType) {
        switch (serviceType) {
            case SsServiceType.CFU:
                return ImsSsData.SS_CFU;
            case SsServiceType.CF_BUSY:
                return ImsSsData.SS_CF_BUSY;
            case SsServiceType.CF_NO_REPLY:
                return ImsSsData.SS_CF_NO_REPLY;
            case SsServiceType.CF_NOT_REACHABLE:
                return ImsSsData.SS_CF_NOT_REACHABLE;
            case SsServiceType.CF_ALL:
                return ImsSsData.SS_CF_ALL;
            case SsServiceType.CF_ALL_CONDITIONAL:
                return ImsSsData.SS_CF_ALL_CONDITIONAL;
            case SsServiceType.CFUT:
                return ImsSsData.SS_CFUT;
            case SsServiceType.CLIP:
                return ImsSsData.SS_CLIP;
            case SsServiceType.CLIR:
                return ImsSsData.SS_CLIR;
            case SsServiceType.COLP:
                return ImsSsData.SS_COLP;
            case SsServiceType.COLR:
                return ImsSsData.SS_COLR;
            case SsServiceType.CNAP:
                return ImsSsData.SS_CNAP;
            case SsServiceType.WAIT:
                return ImsSsData.SS_WAIT;
            case SsServiceType.BAOC:
                return ImsSsData.SS_BAOC;
            case SsServiceType.BAOIC:
                return ImsSsData.SS_BAOIC;
            case SsServiceType.BAOIC_EXC_HOME:
                return ImsSsData.SS_BAOIC_EXC_HOME;
            case SsServiceType.BAIC:
                return ImsSsData.SS_BAIC;
            case SsServiceType.BAIC_ROAMING:
                return ImsSsData.SS_BAIC_ROAMING;
            case SsServiceType.ALL_BARRING:
                return ImsSsData.SS_ALL_BARRING;
            case SsServiceType.OUTGOING_BARRING:
                return ImsSsData.SS_OUTGOING_BARRING;
            case SsServiceType.INCOMING_BARRING:
                return ImsSsData.SS_INCOMING_BARRING;
            case SsServiceType.INCOMING_BARRING_DN:
                return ImsSsData.SS_INCOMING_BARRING_DN;
            case SsServiceType.INCOMING_BARRING_ANONYMOUS:
                return ImsSsData.SS_INCOMING_BARRING_ANONYMOUS;
            default:
        }
        return ImsUtImplBase.INVALID_RESULT;
    }

    public static int toRequestType(int requestType) {
        switch (requestType) {
            case SsRequestType.ACTIVATION:
                return ImsSsData.SS_ACTIVATION;
            case SsRequestType.DEACTIVATION:
                return ImsSsData.SS_DEACTIVATION;
            case SsRequestType.INTERROGATION:
                return ImsSsData.SS_INTERROGATION;
            case SsRequestType.REGISTRATION:
                return ImsSsData.SS_REGISTRATION;
            case SsRequestType.ERASURE:
                return ImsSsData.SS_ERASURE;
            default:
        }
        return ImsUtImplBase.INVALID_RESULT;
    }

    public static int toTeleserviceType(int teleservice) {
        switch (teleservice) {
            case SsTeleserviceType.ALL_TELE_AND_BEARER_SERVICES:
                return ImsSsData.SS_ALL_TELE_AND_BEARER_SERVICES;
            case SsTeleserviceType.ALL_TELESEVICES:
                return ImsSsData.SS_ALL_TELESEVICES;
            case SsTeleserviceType.TELEPHONY:
                return ImsSsData.SS_TELEPHONY;
            case SsTeleserviceType.ALL_DATA_TELESERVICES:
                return ImsSsData.SS_ALL_DATA_TELESERVICES;
            case SsTeleserviceType.SMS_SERVICES:
                return ImsSsData.SS_SMS_SERVICES;
            case SsTeleserviceType.ALL_TELESERVICES_EXCEPT_SMS:
                return ImsSsData.SS_ALL_TELESERVICES_EXCEPT_SMS;
            default:
        }
        return ImsUtImplBase.INVALID_RESULT;
    }

    public static int toImsCallForwardInfo(int reason) {
        switch(reason) {
            case ImsSsData.SS_CFU:
                return ImsCallForwardInfo.CDIV_CF_REASON_UNCONDITIONAL;
            case ImsSsData.SS_CF_BUSY:
                return ImsCallForwardInfo.CDIV_CF_REASON_BUSY;
            case ImsSsData.SS_CF_NO_REPLY:
                return ImsCallForwardInfo.CDIV_CF_REASON_NO_REPLY;
            case ImsSsData.SS_CF_NOT_REACHABLE:
                return ImsCallForwardInfo.CDIV_CF_REASON_NOT_REACHABLE;
            case ImsSsData.SS_CF_ALL:
                return ImsCallForwardInfo.CDIV_CF_REASON_ALL;
            case ImsSsData.SS_CF_ALL_CONDITIONAL:
                return ImsCallForwardInfo.CDIV_CF_REASON_ALL_CONDITIONAL;
            default:
                break;
        }
        return ImsUtImplBase.INVALID_RESULT;
    }

    public static StatusReport toStatusReport(SmsSendStatusReport report) {
        return new StatusReport(report.messageRef, report.format, report.pdu);
    }

    private static int toFrameworkVerstat(int verstat){
        switch(verstat){
            case VerificationStatus.VALIDATION_NONE:
                return ImsSmsImpl.MT_IMS_STATUS_VALIDATION_NONE;
            case VerificationStatus.VALIDATION_PASS:
                return ImsSmsImpl.MT_IMS_STATUS_VALIDATION_PASS;
            case VerificationStatus.VALIDATION_FAIL:
                return ImsSmsImpl.MT_IMS_STATUS_VALIDATION_FAIL;
            default:
                return ImsSmsImpl.MT_IMS_STATUS_VALIDATION_NONE;
        }
    }

    public static IncomingSms toIncomingSms(vendor.qti.hardware.radio.ims.IncomingSms imsSms) {
        int verstat = toFrameworkVerstat(imsSms.verstat);
        return new IncomingSms(imsSms.format, imsSms.pdu, verstat);
    }

    public static DeflectRequestInfo fromDeflectCall(int index, String number) {
        DeflectRequestInfo deflectRequestInfo = new DeflectRequestInfo();
        deflectRequestInfo.connIndex = index;
        deflectRequestInfo.number = number;
        return deflectRequestInfo;
    }

    public static AcknowledgeSmsInfo fromAcknowledgeSms(int messageRef, int deliverStatus) {
        AcknowledgeSmsInfo smsInfo = new AcknowledgeSmsInfo();
        smsInfo.messageRef = messageRef;
        smsInfo.smsDeliverStatus = fromSmsDeliverStatus(deliverStatus);
        return smsInfo;
    }

    public static GeoLocationInfo fromGeoLocation(double lat, double lon, Address address) {
        GeoLocationInfo geoLocationInfo = new GeoLocationInfo();
        geoLocationInfo.lat = lat;
        geoLocationInfo.lon = lon;
        geoLocationInfo.addressInfo = fromAddress(lat, lon, address);
        return geoLocationInfo;
    }

    public static DtmfInfo fromDtmf(int callId, char c) {
        DtmfInfo dtmfValue = new DtmfInfo();
        dtmfValue.dtmf = Character.toString(c);
        dtmfValue.callId = callId;
        return dtmfValue;
    }

    public static ClirInfo fromClir(int clirMode) {
        ClirInfo clirValue = new ClirInfo();
        clirValue.paramN = clirMode;
        return clirValue;
    }

    public static CallWaitingInfo fromCallWaiting(boolean enable, int serviceClass) {
        CallWaitingInfo callWaitingInfo = new CallWaitingInfo();
        callWaitingInfo.serviceStatus = enable ? ServiceClassStatus.ENABLED :
               ServiceClassStatus.DISABLED;
        callWaitingInfo.serviceClass = serviceClass;
        return callWaitingInfo;
    }

    public static TtyInfo fromTty(int uiTtyMode) {
        TtyInfo info = new TtyInfo();
        info.mode = fromTtyMode(uiTtyMode);
        info.userData = new byte[0];
        return info;
    }

    public static SuppServiceStatusRequest fromSuppServiceStatus(int operationType, int facility,
           String[] inCbNumList, String password, int serviceClass, boolean expectMore) {
        SuppServiceStatusRequest suppServiceStatusRequest = new SuppServiceStatusRequest();
        suppServiceStatusRequest.operationType = fromOperationType(operationType);
        suppServiceStatusRequest.facilityType = fromFacility(facility);
        suppServiceStatusRequest.cbNumListInfo = fromCbNumList(inCbNumList, serviceClass);
        suppServiceStatusRequest.password = password != null ? password : "";
        suppServiceStatusRequest.expectMore = expectMore;
        return suppServiceStatusRequest;
    }

    private static int fromOperationType(int operationType) {
        switch (operationType) {
            case SuppSvcResponse.ACTIVATE:
                return  SuppSvcOperationType.ACTIVATE;
            case SuppSvcResponse.DEACTIVATE:
                return  SuppSvcOperationType.DEACTIVATE;
            case SuppSvcResponse.QUERY:
                return  SuppSvcOperationType.QUERY;
            case SuppSvcResponse.REGISTER:
                return  SuppSvcOperationType.REGISTER;
            case SuppSvcResponse.ERASURE:
                return  SuppSvcOperationType.ERASURE;
            default:
                return  SuppSvcOperationType.INVALID;
        }
    }

    public static ViceUriInfo toViceUriInfo(ViceInfo viceInfo) {
        ArrayList<Byte> viceInfoUri = new ArrayList<>();
        for(int i = 0; i < viceInfo.viceInfoUri.length ; i++)
            viceInfoUri.add(viceInfo.viceInfoUri[i]);
        ViceUriInfo info = new ViceUriInfo(viceInfoUri);
        Log.v(TAG, "onRefreshViceInfo: viceUri = " + viceInfoUri);
        return info;
    }

    public static List <ImsCallForwardInfo> toImsCallForwardInfoList(StkCcUnsolSsResult ss,
            ImsSsData ssData) {
        List <ImsCallForwardInfo> cfInfo = new ArrayList<>();
        if (ss.cfData == null) {
            Log.d(TAG, "cfData is null, which is unexpected for: " +
                    ssData.getServiceType());
        } else {
            CfData cfData = ss.cfData[0];
            int num = cfData.cfInfo.length;
            for (int i = 0; i < num; i++) {
                CallForwardInfo rCfInfo = cfData.cfInfo[i];
                cfInfo.add(new ImsCallForwardInfo(StableAidl.
                        toImsCallForwardInfo(rCfInfo.reason), rCfInfo.status,
                        rCfInfo.toa, rCfInfo.serviceClass, rCfInfo.number,
                        rCfInfo.timeSeconds));

                Log.d(TAG, "[SS Data] CF Info " + i + " : " +  cfInfo.get(i));
            }
        }
        return cfInfo;
    }

    public static List <ImsSsInfo> toImsSsInfoListCb(StkCcUnsolSsResult ss,
            ImsSsData ssData) {
        List <ImsSsInfo> imsSsInfo = new ArrayList<>();
        if (ss.cbNumInfo == null) {
            Log.d(TAG, "cbNumInfo is null, which is unexpected for: " +
                        ssData.getServiceType());
        } else {
            int num = ss.cbNumInfo.length;
            for (int i = 0; i < num; i++) {
                ImsSsInfo.Builder imsSsInfoBuilder =
                        new ImsSsInfo.Builder(ss.cbNumInfo[i].status);

                imsSsInfoBuilder.setIncomingCommunicationBarringNumber(
                        ss.cbNumInfo[i].number);
                imsSsInfo.add(imsSsInfoBuilder.build());
                Log.d(TAG, "[SS Data] ICB Info " + i + " : " +  imsSsInfo.get(i));
            }
        }
        return imsSsInfo;
    }

    public static List <ImsSsInfo> toImsSsInfoList(StkCcUnsolSsResult ss,
            ImsSsData ssData) {
        List <ImsSsInfo> imsSsInfo = new ArrayList<>();
        if (ss.ssInfoData == null) {
            Log.d(TAG, "imsSsInfo is null, which is unexpected for: " +
                        ssData.getServiceType());
        } else {
            SsInfoData ssInfoData = ss.ssInfoData[0];
            int num = ssInfoData.ssInfo.length;
            if (num > 0) {
                ImsSsInfo.Builder imsSsInfoBuilder =
                        new ImsSsInfo.Builder(ssInfoData.ssInfo[0]);
                if (ssData.isTypeClir() && ssData.isTypeInterrogation()) {
                    // creating ImsSsInfoBuilder with first int as status not used
                    imsSsInfoBuilder.setClirOutgoingState(ssInfoData.ssInfo[0]);
                    if (num > 1) {
                        imsSsInfoBuilder.setClirInterrogationStatus(ssInfoData.ssInfo[1]);
                    }
                } else if (num > 1) {
                    imsSsInfoBuilder.setProvisionStatus(ssInfoData.ssInfo[1]);
                }
                imsSsInfo.add(imsSsInfoBuilder.build());
            }
        }
        return imsSsInfo;
    }

    public static AcknowledgeSmsReportInfo fromAcknowledgeSmsReport(int messageRef,
            int statusReportStatus) {
        AcknowledgeSmsReportInfo smsReportInfo = new AcknowledgeSmsReportInfo();
        smsReportInfo.messageRef = messageRef;
        smsReportInfo.smsReportStatus = fromSmsReportStatus(statusReportStatus);
        return smsReportInfo;
    }

    public static ImsSsData toImsSsData(StkCcUnsolSsResult ss) {
        ImsSsData.Builder ssDataBuilder  = new ImsSsData.Builder(
            toServiceType(ss.serviceType),
            toRequestType(ss.requestType),
            toTeleserviceType(ss.teleserviceType),
            ss.serviceClass, ss.result);
        ImsSsData ssData = ssDataBuilder.build();

        if (ssData.isTypeCf() && ssData.isTypeInterrogation())  {
            List <ImsCallForwardInfo> cfInfo = toImsCallForwardInfoList(ss, ssData);
            if (cfInfo !=null) {
                ssDataBuilder.setCallForwardingInfo(cfInfo);
            }
        } else if (ssData.isTypeIcb() && ssData.isTypeInterrogation()) {
            List <ImsSsInfo> imsSsInfo = toImsSsInfoListCb(ss, ssData);
            if (imsSsInfo !=null) {
                ssDataBuilder.setSuppServiceInfo(imsSsInfo);
            }
        } else {
            /** Handling for SS_CLIP/SS_CLIR/SS_COLP/SS_WAIT
                Currently, max size of the array sent is 2.
                Expected format for all except SS_CLIR is:
                status - ssInfo[0]
                provision status - (Optional) ssInfo[1]
             */
            List <ImsSsInfo> imsSsInfo = toImsSsInfoList(ss, ssData);
            if (imsSsInfo !=null) {
                ssDataBuilder.setSuppServiceInfo(imsSsInfo);
            }
        }
        return ssData;
    }

    public static int toVoiceInfo(int voiceInfo) {
        switch (voiceInfo) {
            case VoiceInfo.SILENT:
                return ImsUtils.VOICE_INFO_SILENT;
            case VoiceInfo.SPEECH:
                return ImsUtils.VOICE_INFO_SPEECH;
            case VoiceInfo.UNKNOWN:
            default:
                return ImsUtils.VOICE_INFO_UNKNOWN;
        }
    }

    public static int toServiceDomain(int domain) {
        switch(domain) {
            case SystemServiceDomain.CS_ONLY:
                return ImsRegistrationUtils.CS_ONLY;
            case SystemServiceDomain.PS_ONLY:
                return ImsRegistrationUtils.PS_ONLY;
            case SystemServiceDomain.CS_PS:
                return ImsRegistrationUtils.CS_PS;
            case SystemServiceDomain.CAMPED:
                return ImsRegistrationUtils.CAMPED;
            default:
                return ImsRegistrationUtils.NO_SRV;
        }
    }

    public static int toSmsCallBackMode(int mode) {
        switch(mode) {
            case SmsCallBackMode.ENTER:
                return QtiCallConstants.SCBM_STATUS_ENTER;
            case SmsCallBackMode.EXIT:
            default:
                return QtiCallConstants.SCBM_STATUS_EXIT;
        }
    }

    public static Mwi toMessageWaitingIndication(MessageWaitingIndication
            messageWaitingIndication) {
        if (messageWaitingIndication == null) {
            return null;
        }

        Mwi mwi = new Mwi();
        if (messageWaitingIndication.messageSummary != null) {
            Log.d(StableAidl.class, "toMessageWaitingIndication summaryLength = " +
                    messageWaitingIndication.messageSummary.length);
            for (MessageSummary summary : messageWaitingIndication.messageSummary) {
                if (summary != null) {
                    mwi.mwiMsgSummary.add(toMessageSummary(summary));
                }
            }
        }
        if (messageWaitingIndication.ueAddress != null &&
                !messageWaitingIndication.ueAddress.isEmpty()) {
            mwi.mUeAddress = messageWaitingIndication.ueAddress;
        }
        if (messageWaitingIndication.messageDetails != null) {
            Log.d(StableAidl.class, "toMessageWaitingIndication detailsLength = " +
                    messageWaitingIndication.messageDetails.length);
            for (MessageDetails details : messageWaitingIndication.messageDetails) {
                if (details != null) {
                    mwi.mwiMsgDetails.add(toMessageDetails(details));
                }
            }
        }
        return mwi;
    }

    private static Mwi.MwiMessageSummary toMessageSummary(MessageSummary summary) {
        if (summary == null) {
            return null;
        }

        Mwi.MwiMessageSummary mwiMessageSummary = new Mwi.MwiMessageSummary();

        if (summary.type != MwiMessageType.INVALID) {
            mwiMessageSummary.mMessageType = toMwiMessageType(summary.type);
        }

        if (summary.newMessageCount != Integer.MAX_VALUE) {
            mwiMessageSummary.mNewMessage = summary.newMessageCount;
        }

        if (summary.oldMessageCount != Integer.MAX_VALUE) {
            mwiMessageSummary.mOldMessage = summary.oldMessageCount;
        }

        if (summary.newUrgentMessageCount != Integer.MAX_VALUE) {
            mwiMessageSummary.mNewUrgent = summary.newUrgentMessageCount;
        }

        if (summary.oldUrgentMessageCount != Integer.MAX_VALUE) {
            mwiMessageSummary.mOldUrgent = summary.oldUrgentMessageCount;
        }
        return mwiMessageSummary;
    }

    private static int toMwiMessageType(int type) {
        switch (type) {
            case MwiMessageType.VOICE:
                return Mwi.MWI_MSG_VOICE;
            case MwiMessageType.VIDEO:
                return Mwi.MWI_MSG_VIDEO;
            case MwiMessageType.FAX:
                return Mwi.MWI_MSG_FAX;
            case MwiMessageType.PAGER:
                return Mwi.MWI_MSG_PAGER;
            case MwiMessageType.MULTIMEDIA:
                return Mwi.MWI_MSG_MULTIMEDIA;
            case MwiMessageType.TEXT:
                return Mwi.MWI_MSG_TEXT;
            case MwiMessageType.NONE:
            default:
                return Mwi.MWI_MSG_NONE;
        }
    }

    private static Mwi.MwiMessageDetails toMessageDetails(MessageDetails details) {
        if (details == null) {
            return null;
        }

        Mwi.MwiMessageDetails mwiMessageDetails = new Mwi.MwiMessageDetails();

        if (!details.toAddress.isEmpty()) {
            mwiMessageDetails.mToAddress = details.toAddress;
        }

        if (!details.fromAddress.isEmpty()) {
            mwiMessageDetails.mFromAddress = details.fromAddress;
        }

        if (!details.subject.isEmpty()) {
            mwiMessageDetails.mSubject = details.subject;
        }

        if (!details.date.isEmpty()) {
            mwiMessageDetails.mDate = details.date;
        }

        if (details.priority != MwiMessagePriority.INVALID) {
            mwiMessageDetails.mPriority = toMwiMessagePriority(details.priority);
        }

        if (!details.id.isEmpty()) {
            mwiMessageDetails.mMessageId = details.id;
        }

        if (details.type != MwiMessageType.INVALID) {
            mwiMessageDetails.mMessageType = toMwiMessageType(details.type);
        }
        return mwiMessageDetails;
    }

    private static int toMwiMessagePriority(int type) {
        switch (type) {
            case MwiMessagePriority.LOW:
                return Mwi.MWI_MSG_PRIORITY_LOW;
            case MwiMessagePriority.NORMAL:
                return Mwi.MWI_MSG_PRIORITY_NORMAL;
            case MwiMessagePriority.URGENT:
                return Mwi.MWI_MSG_PRIORITY_URGENT;
            case MwiMessagePriority.UNKNOWN:
            default:
                return Mwi.MWI_MSG_PRIORITY_UNKNOWN;
        }
    }

    public static Object toConfigObject(ConfigInfo configInfo) {
        if (configInfo.hasBoolValue) {
            return configInfo.boolValue;
        } else if (configInfo.intValue != Integer.MAX_VALUE) {
            return configInfo.intValue;
        } else if (configInfo.stringValue != null) {
            return configInfo.stringValue;
        } else if (configInfo.errorCause != ConfigFailureCause.INVALID) {
            return StableAidlErrorCode.toImsConfigErrorCode (configInfo.errorCause);
        }
        return toImsConfig(configInfo);
    }

   public static int[] toClirArray(ClirInfo clirInfo) {
        int[] response = new int[ImsCallUtils.CLIR_RESPONSE_LENGTH];
        if (clirInfo.paramN != Integer.MAX_VALUE) {
            response[0] = clirInfo.paramN;
        }
        if (clirInfo.paramM != Integer.MAX_VALUE) {
            response[1] = clirInfo.paramM;
        }
        Log.v(TAG, "getClirResponse from ImsRadio. param_m - " + clirInfo.paramM +
                "param_n - " + clirInfo.paramN);
        return response;
    }

    public static int[] toCallWaitingArray(CallWaitingInfo callWaitingInfo) {
        int[] response = null;

        int outServiceStatus = StableAidl.toSuppSvcStatus(callWaitingInfo.serviceStatus);
        if (outServiceStatus == SuppSvcResponse.DISABLED) {
            response = new int[1];
            response[0] = SuppSvcResponse.DISABLED;
        } else {
            response = new int[2];
            response[0] = SuppSvcResponse.ENABLED;
            response[1] = callWaitingInfo.serviceClass;
        }
        return response;
    }

    public static MediaConfig fromImsMediaConfig(Point screenSize,
            Point avcSize, Point hevcSize) {
        MediaConfig mediaConfig = new MediaConfig();
        mediaConfig.screenSize = fromSize(screenSize.x, screenSize.y);
        mediaConfig.maxAvcCodecResolution = fromSize(avcSize.x, avcSize.y);
        mediaConfig.maxHevcCodecResolution = fromSize(hevcSize.x, hevcSize.y);
        return mediaConfig;
    }

    private static Size fromSize(int width, int height) {
        Size size = new Size();
        size.width = width;
        size.height = height;
        return size;
    }

    /**
     * The code checks if remote side interface version is supported
     * return true if support, else false.
     */
    public static boolean isVersionSupported(
            vendor.qti.hardware.radio.ims.IImsRadio imsRadio, int version) {
        if (imsRadio == null || version < 0) {
            return false;
        }
        try {
            return imsRadio.getInterfaceVersion() >= version;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to getInterfaceVersion. Exception " + e);
            return false;
        }
    }

    public static int toMultiSimVoiceCapability(int voiceCapability) {
        switch (voiceCapability) {
            case vendor.qti.hardware.radio.ims.MultiSimVoiceCapability.DSDS:
                return MultiSimVoiceCapability.DSDS;
            case vendor.qti.hardware.radio.ims.MultiSimVoiceCapability.PSEUDO_DSDA:
                return MultiSimVoiceCapability.PSEUDO_DSDA;
            case vendor.qti.hardware.radio.ims.MultiSimVoiceCapability.DSDA:
                return MultiSimVoiceCapability.DSDA;
            case vendor.qti.hardware.radio.ims.MultiSimVoiceCapability.DSSS:
            case vendor.qti.hardware.radio.ims.MultiSimVoiceCapability.NONE:
            default:
                Log.e(TAG, "toMultiSimVoiceCapability: Invalid voice capability");
                return MultiSimVoiceCapability.UNKNOWN;
        }
    }

    public static EcnamInfo toEcnamInfo(vendor.qti.hardware.radio.ims.EcnamInfo from) {
        if (from == null) {
            return null;
        }

        String name = from.name;
        Uri iconUrl = from.iconUrl != null ? Uri.parse(from.iconUrl) : null;
        Uri infoUrl = from.infoUrl != null ? Uri.parse(from.infoUrl) : null;
        Uri cardUrl = from.cardUrl != null ? Uri.parse(from.cardUrl) : null;
        return new EcnamInfo(name, iconUrl, infoUrl, cardUrl);
    }

    public static PreAlertingCallInfo toPreAlertingInfo(
            vendor.qti.hardware.radio.ims.PreAlertingCallInfo from) {
        if (from == null) {
            return null;
        }

        return new PreAlertingCallInfo(from.callId, toCallComposerInfo(from.callComposerInfo),
                toEcnamInfo(from.ecnamInfo), from.modemCallId, from.isDcCall);
    }

    public static boolean toCiWlanNotification(int type) {
        switch (type) {
            case CiWlanNotificationInfo.DISABLE_CIWLAN:
                return true;
            case CiWlanNotificationInfo.NONE:
            default:
                return false;
        }
    }

    public static vendor.qti.hardware.radio.ims.VosActionInfo
            fromVosActionInfo(VosActionInfo from) {

        if (from == null) {
            return null;
        }

        vendor.qti.hardware.radio.ims.VosActionInfo to =
                new vendor.qti.hardware.radio.ims.VosActionInfo();
        if (from.getVosMoveInfo() != null) {
            to.vosMoveInfo = fromVosMoveInfo(from.getVosMoveInfo());
        }

        if (from.getVosTouchInfo() != null) {
            to.vosTouchInfo = fromVosTouchInfo(from.getVosTouchInfo());
        }

        if (from.getVosMoveInfo2() != null) {
            to.vosMoveInfo2 = fromVosMoveInfo2(from.getVosMoveInfo2());
        }

        return to;
    }

    public static vendor.qti.hardware.radio.ims.VosMoveInfo fromVosMoveInfo(VosMoveInfo from) {

        vendor.qti.hardware.radio.ims.VosMoveInfo vosMoveInfo =
                new vendor.qti.hardware.radio.ims.VosMoveInfo();
        if (from.getStart() != null) {
            vosMoveInfo.start = fromCoordinate2D(from.getStart());
        }
        if (from.getEnd() != null) {
            vosMoveInfo.end = fromCoordinate2D(from.getEnd());
        }

        return vosMoveInfo;
    }

    public static vendor.qti.hardware.radio.ims.VosTouchInfo fromVosTouchInfo(VosTouchInfo from) {

        vendor.qti.hardware.radio.ims.VosTouchInfo vosTouchInfo =
                new vendor.qti.hardware.radio.ims.VosTouchInfo();
        if (from.getTouch() != null) {
            vosTouchInfo.touch = fromCoordinate2D(from.getTouch());
        }
        vosTouchInfo.touchDuration = from.getTouchDuration();

        return vosTouchInfo;
    }

    public static vendor.qti.hardware.radio.ims.VosMoveInfo2 fromVosMoveInfo2(
            VosMoveInfo2 from) {

        vendor.qti.hardware.radio.ims.VosMoveInfo2 vosMoveInfo2 =
                new vendor.qti.hardware.radio.ims.VosMoveInfo2();
        if (from.getCoordinate2D() != null) {
            vosMoveInfo2.point = fromCoordinate2D(from.getCoordinate2D());
        }
        vosMoveInfo2.index = from.getIndex();
        vosMoveInfo2.timestamp = from.getTimestamp();
        vosMoveInfo2.duration = from.getDuration();

        return vosMoveInfo2;
    }
    public static vendor.qti.hardware.radio.ims.Coordinate2D fromCoordinate2D(Coordinate2D from) {

        vendor.qti.hardware.radio.ims.Coordinate2D coordinate2D =
                new vendor.qti.hardware.radio.ims.Coordinate2D();
        coordinate2D.x = from.getX();
        coordinate2D.y = from.getY();

        return coordinate2D;
    }

    public static ConferenceAbortReasonInfo fromConferenceAbortReasonInfo(
            int conferenceAbortReason) {

        ConferenceAbortReasonInfo conferenceAbortReasonInfo =
                new ConferenceAbortReasonInfo();
        conferenceAbortReasonInfo.conferenceAbortReason =
                fromConferenceAbortReason(conferenceAbortReason);

        return conferenceAbortReasonInfo;
    }

    public static int fromConferenceAbortReason(int conferenceAbortReason) {
        switch(conferenceAbortReason) {
            case QtiCallConstants.PENDING_EMERGENCY_CALL:
                return ConferenceAbortReason.PENDING_EMRGENCY_CALL;
            default:
                return ConferenceAbortReason.INVALID;
        }
    }

    // Convert telephony side feature constants to AIDL feature constants.
    public static int fromFeature(int feature) {
        switch(feature) {
            case Feature.INTERNAL_AIDL_REORDERING:
                return QtiRadioConfigAidl.INTERNAL_AIDL_REORDERING;
            case Feature.DSDS_TRANSITION:
                return QtiRadioConfigAidl.DSDS_TRANSITION;
            case Feature.UVS_CRBT_CALL:
                return QtiRadioConfigAidl.UVS_CRBT_CALL;
            case Feature.GLASSES_FREE_3D_VIDEO:
                return QtiRadioConfigAidl.GLASSES_FREE_3D_VIDEO;
            case Feature.CONCURRENT_CONFERENCE_EMERGENCY_CALL:
                return QtiRadioConfigAidl.CONCURRENT_CONFERENCE_EMERGENCY_CALL;
            default:
                return QtiRadioConfigAidl.INVALID_FEATURE;
        }
    }
}
