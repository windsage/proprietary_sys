/*
 * Copyright (c) 2022-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradioconfig;

import vendor.qti.hardware.radio.qtiradioconfig.CiwlanCapability;
import vendor.qti.hardware.radio.qtiradioconfig.DualDataRecommendation;

/**
 * Interface declaring unsolicited radio indications for QtiRadioConfig APIs.
 */
@VintfStability
interface IQtiRadioConfigIndication {

    /**
     * Unsol msg to inform HLOS that the device has entered/exited Secure Mode
     *
     * @param enabled indicating whether Secure Mode is on or off - true: on, false: off
     */
    oneway void onSecureModeStatusChange(in boolean enabled);

    /**
     * Sent when dual data capability changes.
     *
     * @param support True if modem supports dual data feature.
     */
    oneway void onDualDataCapabilityChanged(in boolean support);


    /**
     * Sent in below conditions to allow/disallow internet pdn on nDDS
     * after user enables dual data feature from settings app
     * and the user preference is sent to modem through setDualDataUserPreference().
     *
     * DualDataRecommendation(NON_DDS and DATA_ALLOW):
     *    -UE is in DSDA sub-mode and in full concurrent condition
     * DualDataRecommendation(NON_DDS and DATA_DISALLOW):
     *    -UE is in DSDS sub-mode
     *    -UE is in TX sharing condition
     *    -IRAT is initiated on nDDS when UE is in L+NR RAT combo
     *    -nDDS is OOS
     *
     * @param rec <DualDataRecommendation> to allow/disallow internet pdn on nDDS.
     */
    oneway void onDualDataRecommendation(in DualDataRecommendation rec);

    /**
     * Sent when CIWLAN capability changes.
     *
     * @param capability <CiwlanCapability> info of CIWLAN feature support by modem
     *        for a subscription.
     */
    oneway void onCiwlanCapabilityChanged(in CiwlanCapability capability);

    /**
     * Indicates that modem capability of Smart Temp DDS Switch has changed.
     *
     * Upon receiving this indication, HLOS must inform the modem the user’s preference
     * for enabling temp DDS switch. This is done via the API
     * IQtiRadioConfig.sendUserPreferenceForDataDuringVoiceCall().
     *
     * @param isCapable true/false based on whether the device is capable of performing
     *         Smart Temp DDS switch
     */
    oneway void onDdsSwitchCapabilityChanged(in boolean isCapable);

    /**
     * Indicates that Temp DDS Switch criteria has changed.
     *
     * The boolean contained in this indication determines whether the modem-initiated
     * Smart Temp DDS Switch is to be used, or the telephony-initiated legacy Temp DDS
     * Switch logic is to be used. If telephony temp DDS switch logic is disabled, then
     * telephony must wait for modem recommendations to perform the Temp DDS switch.
     *
     * @param telephonyDdsSwitch true/false based on whether telephony temp DDS switch
     *        logic should be enabled or disabled
     */
    oneway void onDdsSwitchCriteriaChanged(in boolean telephonyDdsSwitch);

    /**
     * Indicates the modem's recommendation for the slot on which Temp DDS Switch has to be made.
     *
     * @param recommendedSlotId slot ID to which DDS must be switched
     */
    oneway void onDdsSwitchRecommendation(in int recommendedSlotId);
}
