/* Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.net.Uri;
import android.telephony.ims.ImsReasonInfo;

import java.util.ArrayList;
import org.codeaurora.ims.sms.SmsResponse;

import vendor.qti.hardware.radio.ims.V1_0.ConfigFailureCause;
import vendor.qti.hardware.radio.ims.V1_8.CallInfo;
import vendor.qti.hardware.radio.ims.V1_8.ConfigInfo;
import vendor.qti.hardware.radio.ims.V1_8.ConfigItem;
import vendor.qti.hardware.radio.ims.V1_8.SystemServiceDomain;
import vendor.qti.hardware.radio.ims.V1_8.SmsCallBackMode;

public class ImsRadioUtilsV18 {
    private ImsRadioUtilsV18() {
    }

    public static int configInfoItemToHal(int item) {
        switch (item) {
            case ImsConfigItem.VOICE_OVER_WIFI_ENTITLEMENT_ID:
                return ConfigItem.CONFIG_ITEM_VOWIFI_ENTITLEMENT_ID;
            default:
                return ImsRadioUtilsV16.configInfoItemToHal(item);
        }
    }

     public static int configInfoItemFromHal(int item) {
        switch (item) {
            case ConfigItem.CONFIG_ITEM_VOWIFI_ENTITLEMENT_ID:
                return ImsConfigItem.VOICE_OVER_WIFI_ENTITLEMENT_ID;
            default:
                return ImsRadioUtilsV16.configInfoItemFromHal(item);
        }
    }

    public static ConfigInfo buildConfigInfo(int item, boolean boolValue, int intValue,
            String stringValue, int errorCause) {
        ConfigInfo configInfo = new ConfigInfo();

        configInfo.item = configInfoItemToHal(item);
        configInfo.hasBoolValue = true;
        configInfo.boolValue = boolValue;
        configInfo.intValue = intValue;
        if (stringValue != null) {
            configInfo.stringValue = stringValue;
        }
        configInfo.errorCause = ImsRadioUtils.configFailureCauseToHal(errorCause);

        return configInfo;
    }

    public static ImsConfigItem configInfoFromHal(ConfigInfo configInfo) {
        if (configInfo == null) {
            return null;
        }

        ImsConfigItem config = new ImsConfigItem();
        config.setItem(configInfoItemFromHal(configInfo.item));
        if (configInfo.hasBoolValue) {
            config.setBoolValue(configInfo.boolValue);
        }

        if (configInfo.intValue != Integer.MAX_VALUE) {
            config.setIntValue(configInfo.intValue);
        }

        config.setStringValue(configInfo.stringValue);

        if (configInfo.errorCause != ConfigFailureCause.CONFIG_FAILURE_INVALID) {
            config.setErrorCause(ImsRadioUtils.configFailureCauseFromHal(configInfo.errorCause));
        }

        return config;
    }

    /**
     * Convert the ConfigInfo from V1_6 to V1_8
     */
    public static ConfigInfo migrateConfigInfoFromV16(
            vendor.qti.hardware.radio.ims.V1_6.ConfigInfo from) {
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.item = from.item;
        configInfo.hasBoolValue = from.hasBoolValue;
        configInfo.boolValue = from.boolValue;
        configInfo.intValue = from.intValue;
        if (from.stringValue != null) {
            configInfo.stringValue = from.stringValue;
        }
        configInfo.errorCause = from.errorCause;

        return configInfo;
    }

    public static SmsResponse imsSmsResponsefromHidl(int messageRef,
            int smsStatusResult, int hidlReason, int networkErrorCode) {
        int statusResult = ImsRadioUtils.mapHidlToFrameworkResponseResult(smsStatusResult);
        int reason = ImsRadioUtils.mapHidlToFrameworkResponseReason(hidlReason);

        return new SmsResponse(messageRef, statusResult, reason, networkErrorCode);
    }

    public static int serviceDomainFromHal(int domain) {
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

    /**
     * Convert the Call Info from V1_7 to V1_8
     */
    private static CallInfo migrateCallInfoFrom(
            vendor.qti.hardware.radio.ims.V1_7.CallInfo from) {

        CallInfo to = new CallInfo();
        to.state = from.state;
        to.index = from.index;
        to.toa = from.toa;
        to.hasIsMpty = from.hasIsMpty;
        to.isMpty = from.isMpty;
        to.hasIsMT = from.hasIsMT;
        to.isMT = from.isMT;
        to.als = from.als;
        to.hasIsVoice = from.hasIsVoice;
        to.isVoice = from.isVoice;
        to.hasIsVoicePrivacy = from.hasIsVoicePrivacy;
        to.isVoicePrivacy = from.isVoicePrivacy;
        to.number = from.number;
        to.numberPresentation = from.numberPresentation;
        to.name = from.name;
        to.namePresentation = from.namePresentation;

        to.hasCallDetails = from.hasCallDetails;
        to.callDetails = from.callDetails;

        to.hasFailCause = from.hasFailCause;
        to.failCause.failCause = from.failCause.failCause;

        for(Byte errorinfo : from.failCause.errorinfo) {
            to.failCause.errorinfo.add(errorinfo);
        }

        to.failCause.networkErrorString = from.failCause.networkErrorString;
        to.failCause.hasErrorDetails = from.failCause.hasErrorDetails;
        to.failCause.errorDetails.errorCode = from.failCause.errorDetails.errorCode;
        to.failCause.errorDetails.errorString =
                from.failCause.errorDetails.errorString;

        to.hasIsEncrypted = from.hasIsEncrypted;
        to.isEncrypted = from.isEncrypted;
        to.hasIsCalledPartyRinging = from.hasIsCalledPartyRinging;
        to.isCalledPartyRinging = from.isCalledPartyRinging;
        to.historyInfo = from.historyInfo;
        to.hasIsVideoConfSupported = from.hasIsVideoConfSupported;
        to.isVideoConfSupported = from.isVideoConfSupported;

        to.verstatInfo = from.verstatInfo;
        to.mtMultiLineInfo = from.mtMultiLineInfo;
        to.tirMode = from.tirMode;

        to.isPreparatory = from.isPreparatory;
        to.crsData = from.crsData;
        to.callProgInfo = from.callProgInfo;

        return to;
    }

    public static ArrayList<CallInfo> migrateCallListFrom(ArrayList<
            vendor.qti.hardware.radio.ims.V1_7.CallInfo> callList) {

        if (callList == null) {
            return null;
        }
        ArrayList<CallInfo> list = new ArrayList<CallInfo>();
        for (vendor.qti.hardware.radio.ims.V1_7.CallInfo from : callList) {
            CallInfo to = migrateCallInfoFrom(from);
            list.add(to);
        }
        return list;
    }

    public static int scbmStatusFromHal(int mode) {
        if (mode == SmsCallBackMode.SCBM_ENTER) {
            return QtiCallConstants.SCBM_STATUS_ENTER;
        } else {
            return QtiCallConstants.SCBM_STATUS_EXIT;
        }
    }
}
