/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2012 Code Aurora Forum. All rights reserved.
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
 */

package org.codeaurora.ims;

import android.telephony.ims.ImsReasonInfo;

import android.telecom.TelecomManager;

import java.lang.Comparable;
import java.util.Objects;

import org.codeaurora.ims.CallComposerInfo;
/**
 * {@hide}
 */
public class DriverCallIms implements Comparable<DriverCallIms> {
    static final String LOG_TAG = "DRIVERCALL-IMS";

    /**
     * Bit-mask values to indicate the {@link DriverCallIms} properties which changed during a
     * call to {@link #update(DriverCallIms)}.
     */
    public static final int UPDATE_NONE = 0x00;
    public static final int UPDATE_STATE = 0x01;
    public static final int UPDATE_INDEX = 0x02;
    public static final int UPDATE_NUMBER = 0x04;
    public static final int UPDATE_IS_MT = 0x08;
    public static final int UPDATE_IS_MPTY = 0x10;
    public static final int UPDATE_CALL_DETAILS = 0x20;
    public static final int UPDATE_ENCRYPTION = 0x80;
    public static final int UPDATE_HISTORY_INFO = 0x100;
    public static final int UPDATE_CONF_SUPPORT = 0x200;
    public static final int UPDATE_CRS_INFO = 0x400;
    public static final int UPDATE_CALL_PROGRESS_INFO = 0x800;
    public static final int UPDATE_DIVERSION_INFO = 0x1000;
    public static final int UPDATE_AUDIO_QUALITY = 0x2000;
    public static final int UPDATE_MODEM_CALL_ID = 0x4000;
    public static final int UPDATE_IS_CALLED_PARTY_RINGING = 0x8000;

    public enum State {
        ACTIVE,
        HOLDING,
        DIALING, // MO call only
        ALERTING, // MO call only
        INCOMING, // MT call only
        WAITING, // MT call only
        END;
    }

    /**
     * Bit-mask values to indicate the conference support information
     * for this call instance, as received from lower layers.
     */
    /* Default disable mask */
    public static final int CONF_SUPPORT_NONE = 0x00;
    /* Enabled when there is a valid conference support information from lower layers */
    public static final int CONF_SUPPORT_INDICATED = 0x01;
    /* Enabled when VT conference support is indicated as supported */
    public static final int CONF_VIDEO_SUPPORTED = 0x02;

    public CallDetails callDetails;
    public State state;
    /* callFailCause is set for AOSP supported call fail codes */
    public ImsReasonInfo callFailCause;
    /**
     * mAdditionalCallFailCause is set for primarily QCOM specified codes which are
     * not currently supported by AOSP when call fails. However in case we don't
     * have QC specific mapping, we will fallback to AOSP mapping.
     */
    public int mAdditionalCallFailCause ;
    public boolean isEncrypted;
    public String historyInfo;
    /**
     * mCallFailReason is used to notify HLOS in cases of CS silent redial from modem
     * it won't be a call END from modem in this case.
     */
    public int mCallFailReason;
    public int index;
    public boolean isMT;
    public boolean isMpty;
    public String number;
    public int TOA;
    public boolean isVoice;
    public boolean isVoicePrivacy;
    public int als;
    public int numberPresentation;
    public String name;
    public int namePresentation;
    /* A mask indicating the conference support information of this call instance */
    public int mConfSupported = CONF_SUPPORT_NONE;
    //VerstatInfo Shoudn't be modified once set. No need to check for an update in the
    //update() method
    private final VerstatInfo mVerstatInfo;

    public MultiIdentityLineInfo mMtMultiLineInfo;
    // Used for MT calls to indicate whether to display additional answer options to
    // the user to overwrite TIR presentation
    public boolean isTirOverwriteAllowed;
    // Used for MT calls, which contains the call composer elements
    private CallComposerInfo mCallComposerInfo;
    //Used for MT video CRS
    public CrsData crsData;
    public boolean isPreparatory = false;
    //Used for Call progress info for alerting calls
    public CallProgressInfo callProgressInfo;
    //Diversion information to report call forward info will be valid only for incoming calls.
    public String diversionInfo;
    // Additional call information received.
    public MsimAdditionalCallInfo additionalCallInfo;
    // Audio quality received
    public AudioQuality audioQuality;
    // Used for MT calls, which contains ecnam info
    private EcnamInfo mEcnamInfo;
    // Modem Call ID.
    public int modemCallId;
    // Is Data Channel Call
    public boolean isDcCall;
    // Used for MO calls to indicate whether the called party is alerted and ringing
    public boolean isCalledPartyRinging;
    // Is Emergency Call
    public boolean isEmergency;
    // Call Reason
    public String callReason;
    // Copy Constructor
    public DriverCallIms(DriverCallIms dc) {
        callDetails = new CallDetails(dc.callDetails);
        callFailCause = new ImsReasonInfo(dc.callFailCause.getCode(),
                dc.callFailCause.getExtraCode(),
                dc.callFailCause.getExtraMessage());
        crsData = new CrsData(dc.crsData);
        state = dc.state;
        index = dc.index;
        number = dc.number;
        isMT = dc.isMT;
        TOA = dc.TOA;
        isMpty = dc.isMpty;
        als = dc.als;
        isVoice = dc.isVoice;
        isVoicePrivacy = dc.isVoicePrivacy;
        numberPresentation = dc.numberPresentation;
        name = dc.name;
        namePresentation = dc.namePresentation;
        isEncrypted = dc.isEncrypted;
        historyInfo = dc.historyInfo;
        mConfSupported = dc.mConfSupported;
        mVerstatInfo = dc.getVerstatInfo();
        mMtMultiLineInfo = dc.mMtMultiLineInfo;
        isTirOverwriteAllowed = dc.isTirOverwriteAllowed;
        mCallComposerInfo = dc.getCallComposerInfo();
        isPreparatory = dc.isPreparatory;
        callProgressInfo = new CallProgressInfo(dc.callProgressInfo);
        diversionInfo = dc.diversionInfo;
        additionalCallInfo = new MsimAdditionalCallInfo(dc.additionalCallInfo);
        audioQuality = new AudioQuality(dc.audioQuality);
        mEcnamInfo = dc.getEcnamInfo();
        modemCallId = dc.modemCallId;
        isDcCall = dc.isDcCall;
        isCalledPartyRinging = dc.isCalledPartyRinging;
        isEmergency = dc.isEmergency;
        callReason = dc.callReason;
    }

    public DriverCallIms() {
        callDetails = new CallDetails();
        mVerstatInfo = new VerstatInfo();
        crsData = new CrsData();
        callProgressInfo = new CallProgressInfo();
        additionalCallInfo = new MsimAdditionalCallInfo();
        audioQuality = new AudioQuality();
    }

    public DriverCallIms(VerstatInfo verstatInfo) {
        callDetails = new CallDetails();
        crsData = new CrsData();
        mVerstatInfo = verstatInfo;
        callProgressInfo = new CallProgressInfo();
        additionalCallInfo = new MsimAdditionalCallInfo();
        audioQuality = new AudioQuality();
    }

    public DriverCallIms(CallComposerInfo callComposerInfo, VerstatInfo verstatInfo) {
        this(verstatInfo);
        crsData = new CrsData();
        mCallComposerInfo = callComposerInfo;
        callProgressInfo = new CallProgressInfo();
        additionalCallInfo = new MsimAdditionalCallInfo();
    }

    public DriverCallIms(CallComposerInfo callComposerInfo, VerstatInfo verstatInfo,
            EcnamInfo ecnamInfo) {
        this(callComposerInfo, verstatInfo);
        mEcnamInfo = ecnamInfo;
    }

    /**
     * Updates members of the {@link DriverCallIms} with the update.  Bitmask describes
     * which attributes have changed.
     *
     * @param update The update.
     * @return Bit-mask describing the attributes of the {@link DriverCallIms} which changed.
     */
    public int update(DriverCallIms update) {
        if (update == null) {
            return UPDATE_NONE;
        }
        int changed = UPDATE_NONE;
        if (state != update.state) {
            state = update.state;
            changed |= UPDATE_STATE;
        }
        if (index != update.index) {
            index = update.index;
            changed |= UPDATE_INDEX;
        }
        if (number != update.number) {
            number = update.number;
            changed |= UPDATE_NUMBER;
        }
        if (isMT != update.isMT) {
            isMT = update.isMT;
            changed |= UPDATE_IS_MT;
        }
        if (isMpty != update.isMpty) {
            isMpty = update.isMpty;
            changed |= UPDATE_IS_MPTY;
        }
        if (update.callFailCause != null) {
            if (callFailCause == null) {
                callFailCause = new ImsReasonInfo(update.callFailCause.getCode(),
                        update.callFailCause.getExtraCode(),
                        update.callFailCause.getExtraMessage());
            } else {
                int imsReasonCode = callFailCause.getCode();
                int imsReasonExtraCode = callFailCause.getExtraCode();
                String imsReasonExtraMessage = callFailCause.getExtraMessage();
                if (callFailCause.getCode() != update.callFailCause.getCode()) {
                    imsReasonCode = update.callFailCause.getCode();
                }
                if (callFailCause.getExtraCode() != update.callFailCause.getExtraCode()) {
                    imsReasonExtraCode = update.callFailCause.getExtraCode();
                }
                if (callFailCause.getExtraMessage() != update.callFailCause.getExtraMessage()) {
                    imsReasonExtraMessage = update.callFailCause.getExtraMessage();
                }
                callFailCause = new ImsReasonInfo(imsReasonCode, imsReasonExtraCode,
                        imsReasonExtraMessage);
            }
        }
        if(callDetails.update(update.callDetails)) {
            changed |= UPDATE_CALL_DETAILS;
        }
        if (isEncrypted != update.isEncrypted) {
            isEncrypted = update.isEncrypted;
            changed |= UPDATE_ENCRYPTION;
        }
        if (!Objects.equals(update.historyInfo, historyInfo)) {
            historyInfo = update.historyInfo;
            changed |= UPDATE_HISTORY_INFO;
        }
        if (mConfSupported != update.mConfSupported) {
            mConfSupported = update.mConfSupported;
            changed |= UPDATE_CONF_SUPPORT;
        }
        if (crsData.update(update.crsData)) {
            changed |= UPDATE_CRS_INFO;
        }
        if (isPreparatory != update.isPreparatory) {
            isPreparatory = update.isPreparatory;
            changed |= UPDATE_CRS_INFO;
        }
        if (!Objects.equals(update.callProgressInfo, callProgressInfo)) {
            callProgressInfo = update.callProgressInfo;
            changed |= UPDATE_CALL_PROGRESS_INFO;
        }
        if (!Objects.equals(update.diversionInfo, diversionInfo)) {
            diversionInfo = update.diversionInfo;
            changed |= UPDATE_DIVERSION_INFO;
        }
        if (!Objects.equals(update.audioQuality, audioQuality)) {
            audioQuality = update.audioQuality;
            changed |= UPDATE_AUDIO_QUALITY;
        }
        if (modemCallId != update.modemCallId) {
            modemCallId = update.modemCallId;
            changed |= UPDATE_MODEM_CALL_ID;
        }
        if (isCalledPartyRinging != update.isCalledPartyRinging) {
            isCalledPartyRinging = update.isCalledPartyRinging;
            changed |= UPDATE_IS_CALLED_PARTY_RINGING;
        }
        return changed;
    }

    public VerstatInfo getVerstatInfo() {
        return mVerstatInfo;
    }

    public CallComposerInfo getCallComposerInfo() {
        return mCallComposerInfo;
    }

    public EcnamInfo getEcnamInfo() {
        return mEcnamInfo;
    }

    public int getModemCallId() {
        return modemCallId;
    }

    public boolean getIsDcCall() {
        return isDcCall;
    }

    public boolean getIsCalledPartyRinging() {
        return isCalledPartyRinging;
    }

    public boolean isConfSupportIndicated() {
        return (mConfSupported & CONF_SUPPORT_INDICATED) == CONF_SUPPORT_INDICATED;
    }

    public boolean isVideoConfSupported() {
        return (mConfSupported & CONF_VIDEO_SUPPORTED) == CONF_VIDEO_SUPPORTED;
    }

    public static int
    presentationFromCLIP(int cli) throws RuntimeException
    {
        switch(cli) {
            case 0: return TelecomManager.PRESENTATION_ALLOWED;
            case 1: return TelecomManager.PRESENTATION_RESTRICTED;
            case 2: return TelecomManager.PRESENTATION_UNKNOWN;
            case 3: return TelecomManager.PRESENTATION_PAYPHONE;
            default:
                throw new RuntimeException("illegal presentation " + cli);
        }
    }

    /** For sorting by index */
    @Override
    public int
    compareTo(DriverCallIms dc) {

        if (index < dc.index) {
            return -1;
        } else if (index == dc.index) {
            return 0;
        } else { /*index > dc.index*/
            return 1;
        }
    }

    public String toString() {
        return "id=" + index + "," + state + "," + "toa=" + TOA + ","
                + (isMpty ? "conf" : "norm") + "," + (isMT ? "mt" : "mo") + ","
                + als + "," + (isVoice ? "voc" : "nonvoc") + ","
                + (isVoicePrivacy ? "evp" : "noevp") + ","
                /* + "number=" + number */+ ",cli=" + numberPresentation + ","
                /* + "name="+ name */+ "," + namePresentation
                + "Call Details =" + callDetails + "," + "CallFailCause Code= "
                + callFailCause.getCode() + "," + "CallFailCause String= "
                + callFailCause.getExtraMessage()
                + ", isEncrypted=" + isEncrypted
                + ", historyInfo=" + historyInfo
                + ", Conf. Support=" + mConfSupported
                + ", " + mMtMultiLineInfo
                + ", isTirOverwriteAllowed=" + isTirOverwriteAllowed
                + ", isPreparatory=" + isPreparatory
                + ", CRS data=" + crsData
                + ", Call progress Info = " + callProgressInfo
                + ", diversionInfo =" + diversionInfo
                + ", additional call info =" + additionalCallInfo
                + ", audio quality =" + audioQuality
                + ", modemCallId = " + modemCallId
                + ", isDcCall = " + isDcCall
                + ", isCalledPartyRinging = " + isCalledPartyRinging
                + ", isEmergency = " + isEmergency
                + ", callReason = " + callReason;
    }
}
