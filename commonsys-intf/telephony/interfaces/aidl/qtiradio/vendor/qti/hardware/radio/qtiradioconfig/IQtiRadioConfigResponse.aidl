/*
 * Copyright (c) 2022-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradioconfig;

import vendor.qti.hardware.radio.qtiradioconfig.CiwlanCapability;
import vendor.qti.hardware.radio.qtiradioconfig.SimTypeInfo;
import vendor.qti.hardware.radio.RadioResponseInfo;

/**
 * Interface declaring response functions to solicited radio requests for QtiRadioConfig APIs.
 */
@VintfStability
interface IQtiRadioConfigResponse {
    /**
     * Response to IQtiRadioConfig.getSecureModeStatus
     *
     * @param serial to match request/response. Response must include same serial as request.
     * @param errorCode as per types.hal returned from RIL.
     * @param status the Secure Mode status - true: enabled, false: disabled
     */
    oneway void getSecureModeStatusResponse(in int serial, in int errorCode, in boolean status);

    /**
     * Response to IQtiRadioConfig.setMsimPreference
     *
     * @param serial to match request/response. Response must include same serial as request.
     * @param errorCode as per types.hal returned from RIL.
     */
    oneway void setMsimPreferenceResponse(in int serial, in int errorCode);

    /**
     * Response to IQtiRadioConfig.getSimTypeInfo
     *
     * @param serial to match request/response. Response must include same serial as request.
     * @param errorCode as per types.hal returned from RIL.
     * @param simTypeInfo, the current active/supported SimType info received from modem
     */
    oneway void getSimTypeInfoResponse(in int serial,
            in int errorCode, in SimTypeInfo[] simTypeInfo);

    /**
     * Response to IQtiRadioConfig.setSimType
     *
     * @param serial to match request/response. Response must include same serial as request.
     * @param errorCode as per types.hal returned from RIL.
     */
    oneway void setSimTypeResponse(in int serial, in int errorCode);

    /**
     * Response to IQtiRadioConfig.getDualDataCapability
     *
     * @param info Response info struct containing response type, serial no. and error.
     * @param support True if modem supports dual data feature.
     */
    oneway void getDualDataCapabilityResponse(in RadioResponseInfo info, in boolean support);

    /**
     * Response to IQtiRadioConfig.setDualDataUserPreference
     *
     * @param info Response info struct containing response type, serial no. and error.
     */
    oneway void setDualDataUserPreferenceResponse(in RadioResponseInfo info);

    /**
     * Response to IQtiRadioConfig.getCiwlanCapability
     *
     * @param info Response info struct containing response type, serial no. and error.
     * @param capability <CiwlanCapability> info of CIWLAN feature support by modem
     *        for a subscription.
     */
    oneway void getCiwlanCapabilityResponse(in RadioResponseInfo info,
            in CiwlanCapability capability);

    /**
     * Response to IQtiRadioConfig.getDdsSwitchCapability()
     *
     * @param info Response info struct containing serial number of the request and the error code
     * @param isCapable true/false based on whether Smart Temp DDS switch capability is supported
     *        by the modem or not.
     */
    oneway void getDdsSwitchCapabilityResponse(in RadioResponseInfo info, in boolean isCapable);

    /**
     * Response to IQtiRadioConfig.sendUserPreferenceForDataDuringVoiceCall()
     *
     * @param info Response info struct containing serial number of the request and the error code
     */
    oneway void sendUserPreferenceForDataDuringVoiceCallResponse(in RadioResponseInfo info);

    /**
     * Response to IQtiRadioConfig.setDataPriorityPreference()
     *
     * @param info Response info struct containing serial number of the request and the error code
     */
    oneway void setDataPriorityPreferenceResponse(in RadioResponseInfo info);
}
