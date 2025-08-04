/* Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.net.Uri;
import android.telephony.ims.ImsReasonInfo;

import org.codeaurora.ims.MultiIdentityLineInfo;
import vendor.qti.hardware.radio.ims.V1_3.VerstatInfo;
import vendor.qti.hardware.radio.ims.V1_4.MultiIdentityLineInfoHal;
import vendor.qti.hardware.radio.ims.V1_6.CallType;
import vendor.qti.hardware.radio.ims.V1_6.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.V1_9.CallInfo;
import vendor.qti.hardware.radio.ims.V1_9.CallDetails;

import java.lang.StringBuilder;
import java.util.ArrayList;

public class ImsRadioUtilsV19 {
    private ImsRadioUtilsV19() {
    }

    /**
     * Convert the Call Info from V1_8 to V1_9
     */
    private static CallInfo migrateCallInfoFrom(
            vendor.qti.hardware.radio.ims.V1_8.CallInfo from) {

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
        to.callDetails = migrateCallDetails(from.callDetails);

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

    public static CallDetails migrateCallDetails(
            vendor.qti.hardware.radio.ims.V1_6.CallDetails from) {
        if (from == null) {
            return null;
        }
        CallDetails to = new CallDetails();
        to.callType = from.callType;
        to.callDomain = from.callDomain;
        to.extrasLength = from.extrasLength;

        for(String extra : from.extras) {
            to.extras.add(extra);
        }

        for(ServiceStatusInfo localAbility : from.localAbility) {
            to.localAbility.add(localAbility);
        }

        for(ServiceStatusInfo peerAbility : from.peerAbility) {
            to.peerAbility.add(peerAbility);
        }

        to.callSubstate = from.callSubstate;
        to.mediaId = from.mediaId;
        to.causeCode = from.causeCode;
        to.rttMode = from.rttMode;
        to.sipAlternateUri = from.sipAlternateUri;
        to.isVosSupported = false;

        return to;
    }

    public static ArrayList<CallInfo> migrateCallListFrom(ArrayList<
            vendor.qti.hardware.radio.ims.V1_8.CallInfo> callList) {

        if (callList == null) {
            return null;
        }
        ArrayList<CallInfo> list = new ArrayList<CallInfo>();
        for (vendor.qti.hardware.radio.ims.V1_8.CallInfo from : callList) {
            CallInfo to = migrateCallInfoFrom(from);
            list.add(to);
        }
        return list;
    }
}
