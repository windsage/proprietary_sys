/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradioconfig;

import vendor.qti.hardware.radio.qtiradioconfig.DataPriorityPreference;
import vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfigResponse;
import vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfigIndication;
import vendor.qti.hardware.radio.qtiradioconfig.MsimPreference;
import vendor.qti.hardware.radio.qtiradioconfig.SimType;

@VintfStability
interface IQtiRadioConfig {

    /**
     * Set callbacks for IQtiRadioConfig requests
     *
     * @param responseCallback Object contains response callback functions
     * @param indicationCallback Object contains indication callback functions
     */
     oneway void setCallbacks(in IQtiRadioConfigResponse responseCallback,
            in IQtiRadioConfigIndication indicationCallback);

    /**
     * Query the status of Secure Mode
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     */
    oneway void getSecureModeStatus(in int serial);

    /**
     * Set MSIM preference to either DSDS or DSDA
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     * @param MsimPreference MsimPreference contains DSDA or DSDS.
     */
    oneway void setMsimPreference(in int serial, in MsimPreference pref);

    /**
     * Get the SIM types supported on each slot, whether Physical, eSIM or Integrated(iUICC),
     * and to get which SIM Type is currently enabled
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     *
     * Response callback is IQtiRadioConfigResponse.getSimTypeInfoResponse()
     */
    oneway void getSimTypeInfo(in int serial);

    /**
     * Change the SIM Type to Physical or Integrated.
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     * @param simType contains the SIM Type to be switched to on each slot
     *
     * Response callback is IQtiRadioConfigResponse.setSimTypeResponse()
     */
    oneway void setSimType(in int serial, in SimType[] simType);

    /**
     * Request dual data capability.
     *
     * @param serial Serial number of request.
     *
     * Response callback is IQtiRadioConfigResponse.getDualDataCapabilityResponse().
     */
    oneway void getDualDataCapability(in int serial);

    /**
     * Set dual data user preference.
     * In a multi-SIM device, inform modem if user wants dual data feature or not.
     * Modem will not send any recommendations to HLOS to support dual data
     * if user does not opt in the feature even if UE is dual data capable.
     *
     * @param serial Serial number of request.
     * @param enable Dual data selection opted by user. True if preference is enabled.
     *
     * Response function is IQtiRadioConfigResponse.setDualDataUserPreferenceResponse()
     */
    oneway void setDualDataUserPreference(in int serial, in boolean enable);

    /**
     * Request CIWLAN capability.
     *
     * @param serial Serial number of request.
     *
     * Response callback is IQtiRadioConfigResponse.getCiwlanCapabilityResponse().
     */
    oneway void getCiwlanCapability(in int serial);

    /**
     * Request for Smart Temp DDS Switch capability from the modem. This determines the overall
     * capability of the Smart Temp DDS switch feature.
     *
     * @param serial serial number of the request.
     *
     * Response function is IQtiRadioConfigResponse.getDdsSwitchCapabilityResponse().
     */
    oneway void getDdsSwitchCapability(in int serial);

    /**
     * Inform modem whether we allow Temp DDS Switch to the individual slots. This takes
     * into account factors like the switch state of ‘Data During Calls’ setting, the
     * current roaming state of the individual subscriptions and their data roaming
     * enabled state.
     * If data during calls is allowed, modem can send recommendations to switch
     * DDS during a voice call on the non-DDS.
     *
     * @param serial serial number of the request.
     * @param isAllowedOnSlot vector containing a boolean per slot that determines whether
     *        we allow temporary DDS switch to that slot.
     *
     * Response function is
     * IQtiRadioConfigResponse.sendUserPreferenceForDataDuringVoiceCallResponse().
     */
    oneway void sendUserPreferenceForDataDuringVoiceCall(in int serial,
            in boolean[] isAllowedOnSlot);

    /**
     * Check if Modem/RIL supports particular feature.
     * When isFeatureSupported(int feature) is used, each int
     * must be mapped to a specific feature. We will keep track
     * of that mapping in the comments here.
     *
     * @param int feature is the integer mapping for a particular feature.
     *
     * @return boolean true if feature is supported by RIL/modem, false otherwise.
     *
     * Mapping of feature to integer is as follows:
     * 1 = INTERNAL_AIDL_REORDERING
     * 2 = DSDS_TRANSITION
     * 3 = SMART_TEMP_DDS_VIA_RADIO_CONFIG
     * 4 = EMERGENCY_ENHANCEMENT
     * 5 = TDSCDMA_SUPPORT
     * 6 = UVS_CRBT_CALL
     * 7 = GLASSES_FREE_3D_VIDEO
     * 8 = CONCURRENT_CONFERENCE_EMERGENCY_CALL
     */
    boolean isFeatureSupported(in int feature);

    /**
     * Sets the data prioritization preference.
     *
     * Sets user/application preference to maximize
     * the aggregated data throughput (SUB1 and SUB2 together).
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     * @param DataPriorityPreference contains user preference.
     *
     * Response callback is IQtiRadioConfigResponse.setDataPriorityPreferenceResponse(
     *         in RadioResponseInfo info)
     */
    oneway void setDataPriorityPreference(in int serial, in DataPriorityPreference dataPreference);
}
