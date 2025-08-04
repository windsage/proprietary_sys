/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.telephony.ims.ImsReasonInfo;
import android.telephony.SmsManager;

import com.qualcomm.ims.utils.Log;

import vendor.qti.hardware.radio.ims.CallFailCause;
import vendor.qti.hardware.radio.ims.ConfigFailureCause;
import vendor.qti.hardware.radio.ims.ErrorCode;
import vendor.qti.hardware.radio.ims.SmsSendFailureReason;
import vendor.qti.hardware.radio.ims.MsimAdditionalInfoCode;

public class StableAidlErrorCode {
    public static int toSmsManagerError(int aidlReason) {
        switch (aidlReason) {
            case SmsSendFailureReason.NONE:
                return SmsManager.RESULT_ERROR_NONE;
            case SmsSendFailureReason.GENERIC_FAILURE:
                return SmsManager.RESULT_ERROR_GENERIC_FAILURE;
            case SmsSendFailureReason.RADIO_OFF:
                return SmsManager.RESULT_ERROR_RADIO_OFF;
            case SmsSendFailureReason.NULL_PDU:
                return SmsManager.RESULT_ERROR_NULL_PDU;
            case SmsSendFailureReason.NO_SERVICE:
                return SmsManager.RESULT_ERROR_NO_SERVICE;
            case SmsSendFailureReason.LIMIT_EXCEEDED:
                return SmsManager.RESULT_ERROR_LIMIT_EXCEEDED;
            case SmsSendFailureReason.SHORT_CODE_NOT_ALLOWED:
                return SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED;
            case SmsSendFailureReason.SHORT_CODE_NEVER_ALLOWED:
                return SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED;
            case SmsSendFailureReason.FDN_CHECK_FAILURE:
                return SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE;
            case SmsSendFailureReason.RADIO_NOT_AVAILABLE:
                return SmsManager.RESULT_RADIO_NOT_AVAILABLE;
            case SmsSendFailureReason.NETWORK_REJECT:
                return SmsManager.RESULT_NETWORK_REJECT;
            case SmsSendFailureReason.INVALID_ARGUMENTS:
                return SmsManager.RESULT_INVALID_ARGUMENTS;
            case SmsSendFailureReason.INVALID_STATE:
                return SmsManager.RESULT_INVALID_STATE;
            case SmsSendFailureReason.NO_MEMORY:
                return SmsManager.RESULT_NO_MEMORY;
            case SmsSendFailureReason.INVALID_SMS_FORMAT:
                return SmsManager.RESULT_INVALID_SMS_FORMAT;
            case SmsSendFailureReason.SYSTEM_ERROR:
                return SmsManager.RESULT_SYSTEM_ERROR;
            case SmsSendFailureReason.MODEM_ERROR:
                return SmsManager.RESULT_MODEM_ERROR;
            case SmsSendFailureReason.NETWORK_ERROR:
                return SmsManager.RESULT_NETWORK_ERROR;
            case SmsSendFailureReason.ENCODING_ERROR:
                return SmsManager.RESULT_ENCODING_ERROR;
            case SmsSendFailureReason.INVALID_SMSC_ADDRESS:
                return SmsManager.RESULT_INVALID_SMSC_ADDRESS;
            case SmsSendFailureReason.OPERATION_NOT_ALLOWED:
                return SmsManager.RESULT_OPERATION_NOT_ALLOWED;
            case SmsSendFailureReason.INTERNAL_ERROR:
                return SmsManager.RESULT_INTERNAL_ERROR;
            case SmsSendFailureReason.NO_RESOURCES:
                return SmsManager.RESULT_NO_RESOURCES;
            case SmsSendFailureReason.CANCELLED:
                return SmsManager.RESULT_CANCELLED;
            case SmsSendFailureReason.REQUEST_NOT_SUPPORTED:
                return SmsManager.RESULT_REQUEST_NOT_SUPPORTED;
            default:
                Log.e(StableAidlErrorCode.class, "Failure reason is unknown");
                return SmsManager.RESULT_ERROR_GENERIC_FAILURE;
        }
    }

    public static int toImsReasonInfoCode(int failCause) {
        Log.i(StableAidlErrorCode.class, "Call fail cause= " + failCause);

        switch (failCause) {
            // SIP Codes
            case CallFailCause.SIP_REDIRECTED:
                return ImsReasonInfo.CODE_SIP_REDIRECTED;
            case CallFailCause.SIP_BAD_REQ_WAIT_INVITE:
            case CallFailCause.SIP_BAD_REQUEST:
                return ImsReasonInfo.CODE_SIP_BAD_REQUEST;
            case CallFailCause.SIP_FORBIDDEN:
                return ImsReasonInfo.CODE_SIP_FORBIDDEN;
            case CallFailCause.SIP_NOT_FOUND:
                return ImsReasonInfo.CODE_SIP_NOT_FOUND;
            case CallFailCause.SIP_NOT_SUPPORTED:
                return ImsReasonInfo.CODE_SIP_NOT_SUPPORTED;
            case CallFailCause.SIP_REQUEST_TIMEOUT:
                return ImsReasonInfo.CODE_SIP_REQUEST_TIMEOUT;
            case CallFailCause.SIP_PEER_NOT_REACHABLE:
                return ImsReasonInfo.CODE_SIP_TEMPRARILY_UNAVAILABLE;
            case CallFailCause.SIP_BAD_ADDRESS:
                return ImsReasonInfo.CODE_SIP_BAD_ADDRESS;
            case CallFailCause.SIP_BUSY:
                return ImsReasonInfo.CODE_SIP_BUSY;
            case CallFailCause.SIP_REQUEST_CANCELLED:
                return ImsReasonInfo.CODE_SIP_REQUEST_CANCELLED;
            case CallFailCause.SIP_NOT_ACCEPTABLE:
                return ImsReasonInfo.CODE_SIP_NOT_ACCEPTABLE;
            case CallFailCause.SIP_NOT_REACHABLE:
                return ImsReasonInfo.CODE_SIP_NOT_REACHABLE;
            case CallFailCause.SIP_SERVER_INTERNAL_ERROR:
                return ImsReasonInfo.CODE_SIP_SERVER_INTERNAL_ERROR;
            case CallFailCause.SIP_SERVER_NOT_IMPLEMENTED:
            case CallFailCause.SIP_SERVER_BAD_GATEWAY:
            case CallFailCause.SIP_SERVER_VERSION_UNSUPPORTED:
            case CallFailCause.SIP_SERVER_MESSAGE_TOOLARGE:
            case CallFailCause.SIP_SERVER_PRECONDITION_FAILURE:
                return ImsReasonInfo.CODE_SIP_SERVER_ERROR;
            case CallFailCause.SIP_SERVICE_UNAVAILABLE:
                return ImsReasonInfo.CODE_SIP_SERVICE_UNAVAILABLE;
            case CallFailCause.SIP_SERVER_TIMEOUT:
                return ImsReasonInfo.CODE_SIP_SERVER_TIMEOUT;
            case CallFailCause.SIP_USER_REJECTED:
                return ImsReasonInfo.CODE_SIP_USER_REJECTED;
            case CallFailCause.SIP_GLOBAL_ERROR:
                return ImsReasonInfo.CODE_SIP_GLOBAL_ERROR;
            case CallFailCause.SIP_ALTERNATE_EMERGENCY_CALL:
                return ImsReasonInfo.CODE_SIP_ALTERNATE_EMERGENCY_CALL;

            // Media Codes
            case CallFailCause.MEDIA_INIT_FAILED:
                return ImsReasonInfo.CODE_MEDIA_INIT_FAILED;
            case CallFailCause.MEDIA_NO_DATA:
                return ImsReasonInfo.CODE_MEDIA_NO_DATA;
            case CallFailCause.MEDIA_NOT_ACCEPTABLE:
                return ImsReasonInfo.CODE_MEDIA_NOT_ACCEPTABLE;
            case CallFailCause.MEDIA_UNSPECIFIED_ERROR:
                return ImsReasonInfo.CODE_MEDIA_UNSPECIFIED;
            case CallFailCause.NORMAL:
                return ImsReasonInfo.CODE_USER_TERMINATED;
            case CallFailCause.BUSY:
                return ImsReasonInfo.CODE_SIP_BUSY;
            case CallFailCause.NETWORK_UNAVAILABLE:
                return ImsReasonInfo.CODE_SIP_TEMPRARILY_UNAVAILABLE;
            case CallFailCause.ANSWERED_ELSEWHERE:
                return ImsReasonInfo.CODE_ANSWERED_ELSEWHERE;
            case CallFailCause.FDN_BLOCKED:
                return ImsReasonInfo.CODE_FDN_BLOCKED;
            case CallFailCause.IMEI_NOT_ACCEPTED:
                return ImsReasonInfo.CODE_IMEI_NOT_ACCEPTED;
            case CallFailCause.CS_RETRY_REQUIRED:
                return ImsReasonInfo.CODE_LOCAL_CALL_CS_RETRY_REQUIRED;
            case CallFailCause.HO_NOT_FEASIBLE:
                return ImsReasonInfo.CODE_LOCAL_HO_NOT_FEASIBLE;
            case CallFailCause.LOW_BATTERY:
                return ImsReasonInfo.CODE_LOW_BATTERY;
            case CallFailCause.EMERGENCY_TEMP_FAILURE:
                return ImsReasonInfo.CODE_EMERGENCY_TEMP_FAILURE;
            case CallFailCause.EMERGENCY_PERM_FAILURE:
                return ImsReasonInfo.CODE_EMERGENCY_PERM_FAILURE;
            case CallFailCause.PULL_OUT_OF_SYNC:
                return ImsReasonInfo.CODE_CALL_PULL_OUT_OF_SYNC;
            case CallFailCause.CAUSE_CALL_PULLED:
                return ImsReasonInfo.CODE_CALL_END_CAUSE_CALL_PULL;
            case CallFailCause.ACCESS_CLASS_BLOCKED:
                return ImsReasonInfo.CODE_ACCESS_CLASS_BLOCKED;
            case CallFailCause.DIAL_MODIFIED_TO_SS:
                return ImsReasonInfo.CODE_DIAL_MODIFIED_TO_SS;
            case CallFailCause.DIAL_MODIFIED_TO_USSD:
                return ImsReasonInfo.CODE_DIAL_MODIFIED_TO_USSD;
            case CallFailCause.DIAL_MODIFIED_TO_DIAL:
                return ImsReasonInfo.CODE_DIAL_MODIFIED_TO_DIAL;
            case CallFailCause.DIAL_MODIFIED_TO_DIAL_VIDEO:
                return ImsReasonInfo.CODE_DIAL_MODIFIED_TO_DIAL_VIDEO;
            case CallFailCause.DIAL_VIDEO_MODIFIED_TO_SS:
                return ImsReasonInfo.CODE_DIAL_VIDEO_MODIFIED_TO_SS;
            case CallFailCause.DIAL_VIDEO_MODIFIED_TO_USSD:
                return ImsReasonInfo.CODE_DIAL_VIDEO_MODIFIED_TO_USSD;
            case CallFailCause.DIAL_VIDEO_MODIFIED_TO_DIAL:
                return ImsReasonInfo.CODE_DIAL_VIDEO_MODIFIED_TO_DIAL;
            case CallFailCause.DIAL_VIDEO_MODIFIED_TO_DIAL_VIDEO:
                return ImsReasonInfo.CODE_DIAL_VIDEO_MODIFIED_TO_DIAL_VIDEO;
            case CallFailCause.RADIO_OFF:
                return ImsReasonInfo.CODE_RADIO_OFF;
            case CallFailCause.OUT_OF_SERVICE:
                return ImsReasonInfo.CODE_LOCAL_NETWORK_NO_SERVICE;
            case CallFailCause.NO_VALID_SIM:
                return ImsReasonInfo.CODE_NO_VALID_SIM;
            case CallFailCause.RADIO_INTERNAL_ERROR:
                return ImsReasonInfo.CODE_RADIO_INTERNAL_ERROR;
            case CallFailCause.NETWORK_RESP_TIMEOUT:
                return ImsReasonInfo.CODE_NETWORK_RESP_TIMEOUT;
            case CallFailCause.NETWORK_REJECT:
                return ImsReasonInfo.CODE_NETWORK_REJECT;
            case CallFailCause.RADIO_ACCESS_FAILURE:
                return ImsReasonInfo.CODE_RADIO_ACCESS_FAILURE;
            case CallFailCause.RADIO_LINK_FAILURE:
                return ImsReasonInfo.CODE_RADIO_LINK_FAILURE;
            case CallFailCause.RADIO_LINK_LOST:
                return ImsReasonInfo.CODE_RADIO_LINK_LOST;
            case CallFailCause.RADIO_UPLINK_FAILURE:
                return ImsReasonInfo.CODE_RADIO_UPLINK_FAILURE;
            case CallFailCause.RADIO_SETUP_FAILURE:
                return ImsReasonInfo.CODE_RADIO_SETUP_FAILURE;
            case CallFailCause.RADIO_RELEASE_NORMAL:
                return ImsReasonInfo.CODE_RADIO_RELEASE_NORMAL;
            case CallFailCause.RADIO_RELEASE_ABNORMAL:
                return ImsReasonInfo.CODE_RADIO_RELEASE_ABNORMAL;
            case CallFailCause.NETWORK_DETACH:
                return ImsReasonInfo.CODE_NETWORK_DETACH;
            //The OEM Call Fail Causes (CALL_FAIL_OEM_CAUSE_1 - 15) are for debugging
            //purpose only. They shall not be used as a part of the business requirements.
            case CallFailCause.OEM_CAUSE_1:
                return ImsReasonInfo.CODE_OEM_CAUSE_1;
            case CallFailCause.OEM_CAUSE_2:
                return ImsReasonInfo.CODE_OEM_CAUSE_2;
            case CallFailCause.OEM_CAUSE_3:
                return ImsReasonInfo.CODE_OEM_CAUSE_3;
            case CallFailCause.OEM_CAUSE_4:
                return ImsReasonInfo.CODE_OEM_CAUSE_4;
            case CallFailCause.OEM_CAUSE_5:
                return ImsReasonInfo.CODE_OEM_CAUSE_5;
            case CallFailCause.OEM_CAUSE_6:
                return ImsReasonInfo.CODE_OEM_CAUSE_6;
            case CallFailCause.OEM_CAUSE_7:
                return ImsReasonInfo.CODE_OEM_CAUSE_7;
            case CallFailCause.OEM_CAUSE_8:
                return ImsReasonInfo.CODE_OEM_CAUSE_8;
            case CallFailCause.OEM_CAUSE_9:
                return ImsReasonInfo.CODE_OEM_CAUSE_9;
            case CallFailCause.OEM_CAUSE_10:
                return ImsReasonInfo.CODE_OEM_CAUSE_10;
            case CallFailCause.OEM_CAUSE_11:
                return ImsReasonInfo.CODE_OEM_CAUSE_11;
            case CallFailCause.OEM_CAUSE_12:
                return ImsReasonInfo.CODE_OEM_CAUSE_12;
            case CallFailCause.OEM_CAUSE_13:
                return ImsReasonInfo.CODE_OEM_CAUSE_13;
            case CallFailCause.OEM_CAUSE_14:
                return ImsReasonInfo.CODE_OEM_CAUSE_14;
            case CallFailCause.OEM_CAUSE_15:
                return ImsReasonInfo.CODE_OEM_CAUSE_15;
            case CallFailCause.NO_CSFB_IN_CS_ROAM:
                return ImsReasonInfo.CODE_NO_CSFB_IN_CS_ROAM;
            case CallFailCause.SRV_NOT_REGISTERED:
                return ImsReasonInfo.CODE_REJECT_SERVICE_NOT_REGISTERED;
            case CallFailCause.CALL_TYPE_NOT_ALLOWED:
                return ImsReasonInfo.CODE_REJECT_CALL_TYPE_NOT_ALLOWED;
            case CallFailCause.EMRG_CALL_ONGOING:
                return ImsReasonInfo.CODE_REJECT_ONGOING_E911_CALL;
            case CallFailCause.CALL_SETUP_ONGOING:
                return ImsReasonInfo.CODE_REJECT_ONGOING_CALL_SETUP;
            case CallFailCause.MAX_CALL_LIMIT_REACHED:
                return ImsReasonInfo.CODE_REJECT_MAX_CALL_LIMIT_REACHED;
            case CallFailCause.UNSUPPORTED_SIP_HDRS:
                return ImsReasonInfo.CODE_REJECT_UNSUPPORTED_SIP_HEADERS;
            case CallFailCause.CALL_TRANSFER_ONGOING:
                return ImsReasonInfo.CODE_REJECT_ONGOING_CALL_TRANSFER;
            case CallFailCause.PRACK_TIMEOUT:
                return ImsReasonInfo.CODE_REJECT_INTERNAL_ERROR;
            case CallFailCause.QOS_FAILURE:
                return ImsReasonInfo.CODE_REJECT_QOS_FAILURE;
            case CallFailCause.ONGOING_HANDOVER:
                return ImsReasonInfo.CODE_REJECT_ONGOING_HANDOVER;
            case CallFailCause.VT_WITH_TTY_NOT_ALLOWED:
                return ImsReasonInfo.CODE_REJECT_VT_TTY_NOT_ALLOWED;
            case CallFailCause.CALL_UPGRADE_ONGOING:
                return ImsReasonInfo.CODE_REJECT_ONGOING_CALL_UPGRADE;
            case CallFailCause.CONFERENCE_WITH_TTY_NOT_ALLOWED:
                return ImsReasonInfo.CODE_REJECT_CONFERENCE_TTY_NOT_ALLOWED;
            case CallFailCause.CALL_CONFERENCE_ONGOING:
                return ImsReasonInfo.CODE_REJECT_ONGOING_CONFERENCE_CALL;
            case CallFailCause.VT_WITH_AVPF_NOT_ALLOWED:
                return ImsReasonInfo.CODE_REJECT_VT_AVPF_NOT_ALLOWED;
            case CallFailCause.ENCRYPTION_CALL_ONGOING:
                return ImsReasonInfo.CODE_REJECT_ONGOING_ENCRYPTED_CALL;
            case CallFailCause.CALL_ONGOING_CW_DISABLED:
                return ImsReasonInfo.CODE_REJECT_ONGOING_CALL_WAITING_DISABLED;
            case CallFailCause.CALL_ON_OTHER_SUB:
                return ImsReasonInfo.CODE_REJECT_CALL_ON_OTHER_SUB;
            case CallFailCause.COLLISION_1X:
                return ImsReasonInfo.CODE_REJECT_1X_COLLISION;
            case CallFailCause.UI_NOT_READY:
                return ImsReasonInfo.CODE_REJECT_INTERNAL_ERROR;
            case CallFailCause.CS_CALL_ONGOING:
                return ImsReasonInfo.CODE_REJECT_ONGOING_CS_CALL;
            case CallFailCause.SIP_METHOD_NOT_ALLOWED:
                return ImsReasonInfo.CODE_SIP_METHOD_NOT_ALLOWED;
            case CallFailCause.SIP_PROXY_AUTHENTICATION_REQUIRED:
                return ImsReasonInfo.CODE_SIP_PROXY_AUTHENTICATION_REQUIRED;
            case CallFailCause.SIP_REQUEST_ENTITY_TOO_LARGE:
                return ImsReasonInfo.CODE_SIP_REQUEST_ENTITY_TOO_LARGE;
            case CallFailCause.SIP_EXTENSION_REQUIRED:
                return ImsReasonInfo.CODE_SIP_EXTENSION_REQUIRED;
            case CallFailCause.SIP_REQUEST_URI_TOO_LARGE:
                return ImsReasonInfo.CODE_SIP_REQUEST_URI_TOO_LARGE;
            case CallFailCause.SIP_INTERVAL_TOO_BRIEF:
                 return ImsReasonInfo.CODE_SIP_INTERVAL_TOO_BRIEF;
            case CallFailCause.SIP_CALL_OR_TRANS_DOES_NOT_EXIST:
                return ImsReasonInfo.CODE_SIP_CALL_OR_TRANS_DOES_NOT_EXIST;
            case CallFailCause.REJECTED_ELSEWHERE:
                return ImsReasonInfo.CODE_REJECTED_ELSEWHERE;
            case CallFailCause.USER_REJECTED_SESSION_MODIFICATION:
                return ImsReasonInfo.CODE_USER_REJECTED_SESSION_MODIFICATION;
            case CallFailCause.USER_CANCELLED_SESSION_MODIFICATION:
                return ImsReasonInfo.CODE_USER_CANCELLED_SESSION_MODIFICATION;
            case CallFailCause.SESSION_MODIFICATION_FAILED:
                return ImsReasonInfo.CODE_SESSION_MODIFICATION_FAILED;
            case CallFailCause.SIP_LOOP_DETECTED:
                return ImsReasonInfo.CODE_SIP_LOOP_DETECTED;
            case CallFailCause.SIP_TOO_MANY_HOPS:
                return ImsReasonInfo.CODE_SIP_TOO_MANY_HOPS;
            case CallFailCause.SIP_AMBIGUOUS:
                return ImsReasonInfo.CODE_SIP_AMBIGUOUS;
            case CallFailCause.SIP_REQUEST_PENDING:
                return ImsReasonInfo.CODE_SIP_REQUEST_PENDING;
            case CallFailCause.SIP_UNDECIPHERABLE:
                return ImsReasonInfo.CODE_SIP_UNDECIPHERABLE;
            case CallFailCause.SIP_ERROR:
            case CallFailCause.UNOBTAINABLE_NUMBER:
            case CallFailCause.CONGESTION:
            case CallFailCause.INCOMPATIBILITY_DESTINATION:
            case CallFailCause.CALL_BARRED:
            case CallFailCause.FEATURE_UNAVAILABLE:
            case CallFailCause.ERROR_UNSPECIFIED:
            default:
                return ImsReasonInfo.CODE_UNSPECIFIED;
        }
    }

    public static int toErrorCode(int errorCode) {
        Log.v(StableAidlErrorCode.class, "Response received with error = " + errorCode);

        switch (errorCode) {
            case ErrorCode.SUCCESS:
                return ImsErrorCode.SUCCESS;
            case ErrorCode.RADIO_NOT_AVAILABLE:
                return ImsErrorCode.RADIO_NOT_AVAILABLE;
            case ErrorCode.GENERIC_FAILURE:
                return ImsErrorCode.GENERIC_FAILURE;
            case ErrorCode.PASSWORD_INCORRECT:
                return ImsErrorCode.PASSWORD_INCORRECT;
            case ErrorCode.REQUEST_NOT_SUPPORTED:
                return ImsErrorCode.REQUEST_NOT_SUPPORTED;
            case ErrorCode.CANCELLED:
                return ImsErrorCode.CANCELLED;
            case ErrorCode.UNUSED:
                return ImsErrorCode.UNUSED;
            case ErrorCode.INVALID_PARAMETER:
                return ImsErrorCode.INVALID_PARAMETER;
            case ErrorCode.REJECTED_BY_REMOTE:
                return ImsErrorCode.REJECTED_BY_REMOTE;
            case ErrorCode.IMS_DEREGISTERED:
                return ImsErrorCode.IMS_DEREGISTERED;
            case ErrorCode.NETWORK_NOT_SUPPORTED:
                return ImsErrorCode.NETWORK_NOT_SUPPORTED;
            case ErrorCode.HOLD_RESUME_FAILED:
                return ImsErrorCode.HOLD_RESUME_FAILED;
            case ErrorCode.HOLD_RESUME_CANCELED:
                return ImsErrorCode.HOLD_RESUME_CANCELED;
            case ErrorCode.REINVITE_COLLISION:
                return ImsErrorCode.REINVITE_COLLISION;
            case ErrorCode.FDN_CHECK_FAILURE:
                return ImsErrorCode.FDN_CHECK_FAILURE;
            case ErrorCode.SS_MODIFIED_TO_DIAL:
                return ImsErrorCode.SS_MODIFIED_TO_DIAL;
            case ErrorCode.SS_MODIFIED_TO_USSD:
                return ImsErrorCode.SS_MODIFIED_TO_USSD;
            case ErrorCode.SS_MODIFIED_TO_SS:
                return ImsErrorCode.SS_MODIFIED_TO_SS;
            case ErrorCode.SS_MODIFIED_TO_DIAL_VIDEO:
                return ImsErrorCode.SS_MODIFIED_TO_DIAL_VIDEO;
            case ErrorCode.DIAL_MODIFIED_TO_USSD:
                return ImsErrorCode.DIAL_MODIFIED_TO_USSD;
            case ErrorCode.DIAL_MODIFIED_TO_SS:
                return ImsErrorCode.DIAL_MODIFIED_TO_SS;
            case ErrorCode.DIAL_MODIFIED_TO_DIAL:
                return ImsErrorCode.DIAL_MODIFIED_TO_DIAL;
            case ErrorCode.DIAL_MODIFIED_TO_DIAL_VIDEO:
                return ImsErrorCode.DIAL_MODIFIED_TO_DIAL_VIDEO;
            case ErrorCode.DIAL_VIDEO_MODIFIED_TO_USSD:
                return ImsErrorCode.DIAL_VIDEO_MODIFIED_TO_USSD;
            case ErrorCode.DIAL_VIDEO_MODIFIED_TO_SS:
                return ImsErrorCode.DIAL_VIDEO_MODIFIED_TO_SS;
            case ErrorCode.DIAL_VIDEO_MODIFIED_TO_DIAL:
                return ImsErrorCode.DIAL_VIDEO_MODIFIED_TO_DIAL;
            case ErrorCode.DIAL_VIDEO_MODIFIED_TO_DIAL_VIDEO:
                return ImsErrorCode.DIAL_VIDEO_MODIFIED_TO_DIAL_VIDEO;
            case ErrorCode.USSD_CS_FALLBACK:
                return ImsErrorCode.USSD_CS_FALLBACK;
            case ErrorCode.CF_SERVICE_NOT_REGISTERED:
                return ImsErrorCode.CF_SERVICE_NOT_REGISTERED;
            case ErrorCode.RIL_INTERNAL_NO_MEMORY:
            case ErrorCode.RIL_INTERNAL_INVALID_STATE:
            case ErrorCode.RIL_INTERNAL_INVALID_ARGUMENTS:
            case ErrorCode.RIL_INTERNAL_GENERIC_FAILURE:
                return ImsErrorCode.RIL_FAILED_INTERNAL;
            case ErrorCode.NO_MEMORY:
            case ErrorCode.INVALID:
            default:
                return ImsErrorCode.GENERIC_FAILURE;
        }
    }
    public static int fromImsConfigErrorCode(int errorCause) {
        switch (errorCause) {
            case ImsConfigItem.NO_ERR:
                return ConfigFailureCause.NO_ERR;
            case ImsConfigItem.IMS_NOT_READY:
                return ConfigFailureCause.IMS_NOT_READY;
            case ImsConfigItem.FILE_NOT_AVAILABLE:
                return ConfigFailureCause.FILE_NOT_AVAILABLE;
            case ImsConfigItem.READ_FAILED:
                return ConfigFailureCause.READ_FAILED;
            case ImsConfigItem.WRITE_FAILED:
                return ConfigFailureCause.WRITE_FAILED;
            case ImsConfigItem.OTHER_INTERNAL_ERR:
                return ConfigFailureCause.OTHER_INTERNAL_ERR;
            default:
                return ConfigFailureCause.INVALID;
        }
    }

    public static int toImsConfigErrorCode(int errorCause) {
        switch (errorCause) {
            case ConfigFailureCause.NO_ERR:
                return ImsConfigItem.NO_ERR;
            case ConfigFailureCause.IMS_NOT_READY:
                return ImsConfigItem.IMS_NOT_READY;
            case ConfigFailureCause.FILE_NOT_AVAILABLE:
                return ImsConfigItem.FILE_NOT_AVAILABLE;
            case ConfigFailureCause.READ_FAILED:
                return ImsConfigItem.READ_FAILED;
            case ConfigFailureCause.WRITE_FAILED:
                return ImsConfigItem.WRITE_FAILED;
            case ConfigFailureCause.OTHER_INTERNAL_ERR:
            default:
                return ImsConfigItem.OTHER_INTERNAL_ERR;
        }
    }

    public static int toMsimAdditionalCallInfoCode(int callInfoCode) {
        switch (callInfoCode) {
            /* use a QtiCallConstant mapping to avoid dependency on AOSP */
            case MsimAdditionalInfoCode.CONCURRENT_CALL_NOT_POSSIBLE:
                return QtiCallConstants.CODE_CONCURRENT_CALLS_NOT_POSSIBLE;
            case MsimAdditionalInfoCode.NONE:
            default:
                return QtiCallConstants.CODE_UNSPECIFIED;
        }
    }

    public static int toQtiCallFailReasonCode(int failCause) {
        Log.i(StableAidlErrorCode.class,
                "toQtiCallFailReasonCode input fail cause = " + failCause);

        switch (failCause) {
            /* use a QtiCallConstant mapping to avoid dependency on AOSP */
            case CallFailCause.RETRY_ON_IMS_WITHOUT_RTT:
                return QtiCallConstants.CODE_RETRY_ON_IMS_WITHOUT_RTT;
            /* use a QtiCallConstant mapping to avoid dependency on AOSP */
            case CallFailCause.DSDA_CONCURRENT_CALL_NOT_POSSIBLE:
                return QtiCallConstants.CODE_CONCURRENT_CALLS_NOT_POSSIBLE;
            case CallFailCause.EPSFB_FAILURE:
                return QtiCallConstants.CODE_EPSFB_FAILURE;
            case CallFailCause.TWAIT_EXPIRED:
                return QtiCallConstants.CODE_TWAIT_EXPIRED;
            case CallFailCause.TCP_CONNECTION_REQ:
                return QtiCallConstants.CODE_TCP_CONNECTION_REQ;
            case CallFailCause.SIP_MOVED_PERMANENTLY:
                return QtiCallConstants.CODE_MOVED_PERMANENTLY;
            case CallFailCause.SIP_UNAUTHORIZED:
                return QtiCallConstants.CODE_UNAUTHORIZED;
            case CallFailCause.SIP_PAYMENT_REQUIRED:
                return QtiCallConstants.CODE_PAYMENT_REQUIRED;
            case CallFailCause.SIP_GONE:
                return QtiCallConstants.CODE_GONE;
            case CallFailCause.SIP_REMOTE_UNSUPP_MEDIA_TYPE:
                return QtiCallConstants.CODE_REMOTE_UNSUPP_MEDIA_TYPE;
            case CallFailCause.SIP_UNSUPPORTED_URI_SCHEME:
                return QtiCallConstants.CODE_UNSUPPORTED_URI_SCHEME;
            case CallFailCause.BAD_EXTENSION:
                return QtiCallConstants.CODE_BAD_EXTENSION;
            case CallFailCause.SIP_PEER_NOT_REACHABLE:
                return QtiCallConstants.CODE_PEER_NOT_REACHABLE;
            case CallFailCause.SIP_NOT_ACCEPTABLE_HERE:
                return QtiCallConstants.CODE_NOT_ACCEPTABLE_HERE;
            case CallFailCause.SIP_SERVER_NOT_IMPLEMENTED:
                return QtiCallConstants.CODE_SERVER_NOT_IMPLEMENTED;
            case CallFailCause.SIP_SERVER_BAD_GATEWAY:
                return QtiCallConstants.CODE_SERVER_BAD_GATEWAY;
            case CallFailCause.SIP_SERVER_VERSION_UNSUPPORTED:
                return QtiCallConstants.CODE_SERVER_VERSION_UNSUPPORTED;
            case CallFailCause.SIP_SERVER_MESSAGE_TOOLARGE:
                return QtiCallConstants.CODE_SERVER_MESSAGE_TOO_LARGE;
            case CallFailCause.SIP_BUSY_EVERYWHERE:
                return QtiCallConstants.CODE_BUSY_EVERYWHERE;
            case CallFailCause.SIP_NOT_ACCEPTABLE_GLOBAL:
                return QtiCallConstants.CODE_NOT_ACCEPTABLE_GLOBAL;
            default:
                return QtiCallConstants.CODE_UNSPECIFIED;
        }
    }
}
