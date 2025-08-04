/*
 * Copyright (c) 2022-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import com.qti.extphone.DualDataRecommendation;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import vendor.qti.hardware.radio.qtiradioconfig.SimTypeInfo;

public interface IQtiRadioConfigConnectionCallback {
    /**
     * Response to IQtiRadioConfig.getSecureModeStatus
     *
     * @param token to match request/response. Response must include same token as request.
     * @param error as per RIL_Errno part of hardware/ril/include/telephony/ril.h
     * @param enabled the Secure Mode status - true: enabled, false: disabled
     */
    void getSecureModeStatusResponse(Token token, Status error, boolean enabled);

    /**
     * Unsol msg to inform HLOS that the device has entered/exited Secure Mode
     *
     * @param token with a value of -1 which is reserved for indications
     * @param enabled indicating whether Secure Mode is on or off - true: on, false: off
     */
    void onSecureModeStatusChange(Token token, boolean enabled);

    /**
     * Response to IQtiRadioConfig.setMsimPreference
     *
     * @param token to match request/response. Response must include same serial as request.
     * @param status SUCCESS/FAILURE based on the modem Result code
     */
    void setMsimPreferenceResponse(Token token, Status status);

    /**
     * Response to IQtiRadioConfig.getSimTypeInfo
     *
     * @param token to match request/response. Response must include same serial as request.
     * @param status SUCCESS/FAILURE based on the modem Result code
     * @param simTypeInfo the current active/supported SimType info received from modem
     */
    void getSimTypeInfoResponse(Token token, Status status, SimTypeInfo[] simTypeInfo);

    /**
     * Response to IQtiRadioConfig.setSimType
     *
     * @param token to match request/response. Response must include same serial as request.
     * @param status SUCCESS/FAILURE based on the modem Result code
     */
    void setSimTypeResponse(Token token, Status status);

    /**
     * Received when dual data capability changes.
     *
     * @param token to match request/response. Response must include same token as in request,
     *         otherwise token is set to -1.
     * @param status SUCCESS/FAILURE based on the modem result code
     * @param support True if modem supports dual data feature.
     */
    void onDualDataCapabilityChanged(Token token, Status status, boolean support);

    /**
     * Response to IQtiRadioConfig.setDualDataUserPreference
     *
     * @param token to match request/response. Response must include same serial as request.
     * @param status SUCCESS/FAILURE based on the modem Result code
     */
    void setDualDataUserPreferenceResponse(Token token, Status status);

    /**
     * Received in the following conditions to allow/disallow internet pdn on nDDS
     * after dual data user preference is sent as true
     * to modem through IQtiRadioConfig#setDualDataUserPreference().
     * Condition to send onDualDataRecommendation(NON_DDS and DATA_ALLOW):
     *    1)UE is in DSDA sub-mode and in full concurrent condition
     * Conditions to send onDualDataRecommendation(NON_DDS and DATA_DISALLOW):
     *    1)UE is in DSDS sub-mode
     *    2)UE is in TX sharing condition
     *    3)IRAT is initiated on nDDS when UE is in L+NR RAT combo
     *    4)nDDS is OOS
     *
     * @param token to match request/response. Response must include same token as in request,
     *        otherwise token is set to -1.
     * @param rec <DualDataRecommendation> to allow/disallow internet pdn on nDDS.
     */
    void onDualDataRecommendation(Token token, DualDataRecommendation rec);

    /**
     * Received when CIWLAN capability changes.
     *
     * @param token to match request/response. Response must include same token as in request,
     *         otherwise token is set to -1.
     * @param status SUCCESS/FAILURE based on the modem Result code.
     * @param capability <CiwlanCapability> info of CIWLAN feature support by modem
     *        for a subscription.
     */
    void onCiwlanCapabilityChanged(Token token, Status status, CiwlanCapability capability);

    /**
     * Indicates that modem capability of Smart Temp DDS Switch has changed.
     *
     * Upon receiving this indication, HLOS must inform the modem the user’s preference
     * for enabling temp DDS switch.
     *
     * @param token to match request/response.
     * @param status SUCCESS/FAILURE based on the modem result code
     * @param isCapable true/false based on whether the device is capable of performing
     *        Smart Temp DDS switch
     */
    void onDdsSwitchCapabilityChanged(Token token, Status status, boolean isCapable);

    /**
     * Indicates that Temp DDS Switch criteria has changed.
     *
     * The boolean contained in this indication determines whether the modem-initiated
     * Smart Temp DDS Switch is to be used, or the telephony-initiated legacy Temp DDS
     * Switch logic is to be used. If telephony temp DDS switch logic is disabled, then
     * telephony must wait for modem recommendations to perform the Temp DDS switch.
     *
     * @param token to match request/response.
     * @param telephonyDdsSwitch true/false based on whether telephony temp DDS switch
     *        logic should be enabled or disabled
     */
    void onDdsSwitchCriteriaChanged(Token token, boolean telephonyDdsSwitch);

    /**
     * Indicates the modem's recommendation for the slot on which Temp DDS Switch has to be made.
     *
     * @param token to match request/response.
     * @param recommendedSlotId slot ID to which DDS must be switched
     */
    void onDdsSwitchRecommendation(Token token, int recommendedSlotId);

    /**
     * Response to
     * IQtiRadioConfigConnectionInterface.sendUserPreferenceConfigForDataDuringVoiceCall()
     *
     * @param token to match request/response.
     * @param status SUCCESS/FAILURE based on RIL data module response
     */
    void onSendUserPreferenceForDataDuringVoiceCall(Token token, Status status);
}
