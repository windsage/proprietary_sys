/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.AccessMode;
import vendor.qti.hardware.radio.qtiradio.CallForwardInfo;
import vendor.qti.hardware.radio.qtiradio.CellularRoamingPreference;
import vendor.qti.hardware.radio.qtiradio.CiwlanConfig;
import vendor.qti.hardware.radio.qtiradio.FacilityLockInfo;
import vendor.qti.hardware.radio.qtiradio.IQtiRadioIndication;
import vendor.qti.hardware.radio.qtiradio.IQtiRadioResponse;
import vendor.qti.hardware.radio.qtiradio.NrConfig;
import vendor.qti.hardware.radio.qtiradio.NrIcon;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandwidthInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTime;
import vendor.qti.hardware.radio.qtiradio.PersoUnlockStatus;
import vendor.qti.hardware.radio.qtiradio.QtiNetworkScanRequest;
import vendor.qti.hardware.radio.qtiradio.SetNetworkSelectionMode;

@VintfStability
interface IQtiRadio {

    /**
     * Set callback for QtiRadio requests
     *
     * @param responseCallback Object contains response callback functions
     * @param indicationCallback Object contains indication callback functions
     */
    oneway void setCallbacks(in IQtiRadioResponse responseCallback,
            in IQtiRadioIndication indicationCallback);
    /**
     * To get 5G icon info to be shown on UI.
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     *
     * Response callback is IQtiRadioResponse.onNrIconTypeResponse()
     */
    oneway void queryNrIconType(in int serial);

    /**
     * To enable/disable ENDC addition in modem to save power consumption.
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     * @param enable set to true/false
     *
     * Response callback is IQtiRadioResponse.onEnableEndcResponse()
     */
    oneway void enableEndc(in int serial, in boolean enable);

    /**
     * To query if ENDC is enabled/disabled.
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     *
     * Response callback is IQtiRadioResponse.onEndcStatusResponse()
     */
    oneway void queryEndcStatus(in int serial);

    /**
     * To get property values
     *
     * @param property name to get value
     *
     * @return string value of property
     *
     */
    String getPropertyValue(in String prop, in String def);

    /**
     * To enable SA or NSA or both(NSA and SA).
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     * @param config option to enable SA or NSA or both(NSA and SA)
     *
     * Response callback is IQtiRadioResponse.setNrConfigResponse()
     */
    oneway void setNrConfig(in int serial, in NrConfig config);


    /**
     * To query which NR configuration is enabled
     *
     * @param serial to match request/response. Responses must include the same token as requests.
     *
     * Response callback is IQtiRadioResponse.onNrConfigResponse()
     */
    oneway void queryNrConfig(in int serial);


    /**
     * To get modem capabilities include 5G SA.
     *
     * @param serial Serial number of request.
     *
     * Response callback is IQtiRadioResponse.getQtiRadioCapabilityResponse()
     */
    oneway void getQtiRadioCapability(in int serial);

    /**
     * Request call forward status.
     *
     * @param serial Serial number of request.
     * @param callForwardInfo CallForwardInfo
     *
     * Response function is IQtiRadioResponse.getCallForwardStatusResponse()
     */
    oneway void getCallForwardStatus(in int serial, in CallForwardInfo callForwardInfo);

    /**
     * Query the status of a facility lock state
     *
     * @param serial Serial number of request.
     * @param facilityLockInfo is FacilityLockInfo
     * Response function is IQtiRadioResponse.getFacilityLockForAppResponse()
     */
    oneway void getFacilityLockForApp(in int serial, in FacilityLockInfo facilityLockInfo);

    /**
     * Query the IMEI and its type, Primary/Secondary
     *
     * @param serial Serial number of request.
     * Response function is IQtiRadioResponse.getImeiResponse()
     */
    oneway void getImei(in int serial);

    /**
     * Request for smart DDS switch capability
     *
     * @param serial Serial number of request.
     * Response function is IQtiRadioResponse.getDdsSwitchCapabilityResponse()
     */
    oneway void getDdsSwitchCapability(in int serial);

    /**
     * Inform modem if user enabled/disabled UI preference for data during voice call.
     * if its enabled then modem can send recommendations to switch DDS during
     * voice call on nonDDS.
     *
     * @param serial Serial number of request.
     * @param userPreference true/false based on UI preference
     * Response function is IQtiRadioResponse.
     * sendUserPreferenceForDataDuringVoiceCallResponse()
     */
    oneway void sendUserPreferenceForDataDuringVoiceCall(in int serial,
            in boolean userPreference);

    /**
     * Request for epdg over cellular data (cellular IWLAN) feature is supported or not.
     *
     * @param serial Serial number of request
     * @return - boolean value indicates if the feature is supported or not.
     */
     boolean isEpdgOverCellularDataSupported();

    /**
     * Request for setting the 5G SA/NSA Ultra Wideband icon requirements. The below configuration
     * parameters are read from the carrier config XML file.
     *
     * @param serial - Serial number of request. The response will have the same serial
     * @param sib2Value - Tells the modem which config file to update on its end with the rest of
     * the parameters. One of 0, 1, or 2 depending on the carrier or -1 if invalid/not provided.
     *
     * @param saBandInfo - The SA 5G bands for which to show the Ultra Wideband 5G icon
     * @param nsaBandInfo - The NSA 5G bands for which to show the Ultra Wideband 5G icon
     * @param refreshTime - The timer to avoid toggling between the 5G and 5G Ultra Wideband icons
     * based on certain network conditions
     *
     * @param minAggregateBwInfo - The minimum aggregate bandwidth for which to show the Ultra
     * Wideband 5G icon
     */
    oneway void setNrUltraWidebandIconConfig(in int serial, in int sib2Value,
            in @nullable NrUwbIconBandInfo saBandInfo, in @nullable NrUwbIconBandInfo nsaBandInfo,
            in @nullable List<NrUwbIconRefreshTime> refreshTime,
            in @nullable NrUwbIconBandwidthInfo minAggregateBwInfo);

    /**
     * Query current network selection mode
     *
     * @param serial Serial number of request.
     *
     * Response function is IQtiRadioResponse.getNetworkSelectionModeResponse()
     */
    oneway void getNetworkSelectionMode(in int serial);

    /**
     * Specify that the network must be selected automatically.
     *
     * @param serial Serial number of request.
     * @param mode PLMN or SNPN.
     * Response function is IQtiRadioResponse.setNetworkSelectionModeAutomaticResponse()
     */
    oneway void setNetworkSelectionModeAutomatic(in int serial, in AccessMode mode);

    /**
     * Manually select a specified network. This request must not respond until the new operator is
     * selected and registered.
     *
     * @param serial Serial number of request.
     * @param setNetworkSelectionMode defines the AccessMode (PLMN or SNPN), Operator MccMnc, RAT,
     * CAG id of the cell, SNPN Network Id.
     *
     * Response function is IQtiRadioResponse.setNetworkSelectionModeManualResponse()
     */
    oneway void setNetworkSelectionModeManual(
            in int serial, in SetNetworkSelectionMode setNetworkSelectionMode);

    /**
     * Starts a network scan.
     *
     * @param serial Serial number of request.
     * @param request Defines the networks/bands/channels which need to be scanned.
     *
     * Response function is IQtiRadioResponse.startNetworkScanResponse()
     */
    oneway void startNetworkScan(in int serial, in QtiNetworkScanRequest request);

    /**
     * Stops ongoing network scan
     *
     * @param serial Serial number of request.
     *
     * Response function is IQtiRadioResponse.stopNetworkScanResponse()
     */
    oneway void stopNetworkScan(in int serial);

    /**
     * Query the C_IWLAN mode
     *
     * @return - The C_IWLAN configuration (only vs preferred) for home and roaming
     */
    CiwlanConfig getCiwlanConfig();

    /**
     * Query SIM PersoSubState Unlock Status
     *
     * @return - PersoSubState unlock status which can be temporary or permanent.
     */
    PersoUnlockStatus getSimPersoUnlockStatus();

    /**
     * Get the cellular roaming preference
     *
     * @return - The international and domestic cellular roaming preference
     */
    CellularRoamingPreference getCellularRoamingPreference();

    /**
     * Set the cellular roaming preference
     *
     * @param serial - The serial number of the request
     * @param pref - The international and domestic cellular roaming preference
     */
    oneway void setCellularRoamingPreference(in int serial, in CellularRoamingPreference pref);

    /**
     * Query C_IWLAN availability.
     * This API returns true or false based on various conditions like internet PDN is established
     * on DDS over LTE/NR RATs, CIWLAN is supported in home/roaming etc..
     * This is different from existing API IQtiRadio#isEpdgOverCellularDataSupported() which
     * returns true if modem supports the CIWLAN feature based on static configuration in modem.
     *
     * @return - boolean TRUE/FALSE based on C_IWLAN availability.
     */
    boolean isCiwlanAvailable();

    /**
     * Set C_IWLAN mode user preference.
     *
     * @param serial Serial number of request.
     * @param CiwlanConfig The C_IWLAN mode user preference (only vs preferred)
     * for home and roaming.
     *
     * Response function is IQtiRadioResponse.setCiwlanModeUserPreferenceResponse()
     */
    oneway void setCiwlanModeUserPreference(in int serial, in CiwlanConfig ciwlanConfig);

    /**
     * Get C_IWLAN mode user preference
     *
     * This function returns C_IWLAN mode user preference set by the user whereas
     * IQtiRadio#getCiwlanConfig() returns actual C_IWLAN mode set by the modem.
     *
     * @return - The C_IWLAN mode user preference (only vs preferred) for home and roaming.
     */
    CiwlanConfig getCiwlanModeUserPreference();

    /**
     * Get the NR icon information to be shown on the UI
     *
     * @param serial - Number to match the request with the response. The response must include the
     *                 same serial number as the request.
     *
     * Response callback is IQtiRadioResponse.onNrIconResponse()
     */
    oneway void queryNrIcon(in int serial);

    /**
     * Send the IccId of all the profiles present in the eSIM card.
     *
     * @param serial - Serial number of the request which should be included in the response.
     * @param status - Indicates whether the fetching of iccids from eSIM profiles was successful
     *                 or not.
     * @param iccIds - List of eSIM Profiles IccId.
     * @param referenceNum - Number sent by the modem while requesting for the eSIM profile iccids.
     */
    oneway void sendAllEsimProfiles(in int serial, in boolean status, in @nullable List<String>
            iccIds, in int referenceNum);

    /**
     * Send the result for eSIM profile activation to modem.
     *
     * @param serial - Serial number of the request which should be included in the response.
     * @param result - Indicates whether the activation of eSIM profile was successful or not.
     * @param referenceNum - Number sent by the modem while requesting for eSIM profile activation.
     */
    oneway void notifyEnableProfileStatus(in int serial, in boolean result, in int referenceNum);

    /**
     * Send the result for eSIM profile deactivation to modem.
     *
     * @param serial - Serial number of the request which should be included in the response.
     * @param result - Indicates whether the deactivation of eSIM profile was successful or not.
     * @param referenceNum -Number sent by the modem while requesting for eSIM profile deactivation.
     */
    oneway void notifyDisableProfileStatus(in int serial, in boolean result, in int referenceNum);

    /**
     * Query EMC support.
     * EMC supported means that sub supports emergency call natively.
     *
     * @return - boolean TRUE if EMC is supported and FALSE if not supported
     */
    boolean isEmcSupported();

    /**
     * Query EMF support.
     * EMF supported means that sub supports emergency call fallback.
     * This is applicable to PS RATs. For CS RATs, we will return false.
     *
     * @return - boolean TRUE if EMF is supported and FALSE if not supported
     */
    boolean isEmfSupported();
}
