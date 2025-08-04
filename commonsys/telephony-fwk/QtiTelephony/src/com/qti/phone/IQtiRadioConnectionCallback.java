/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import com.android.internal.telephony.NetworkScanResult;

import com.qti.extphone.BearerAllocationStatus;
import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.DcParam;
import com.qti.extphone.NetworkSelectionMode;
import com.qti.extphone.NrConfig;
import com.qti.extphone.NrConfigType;
import com.qti.extphone.NrIcon;
import com.qti.extphone.NrIconType;
import com.qti.extphone.QRadioResponseInfo;
import com.qti.extphone.QosParametersResult;
import com.qti.extphone.QtiCallForwardInfo;
import com.qti.extphone.QtiImeiInfo;
import com.qti.extphone.QtiPersoUnlockStatus;
import com.qti.extphone.SignalStrength;
import com.qti.extphone.SmsResult;
import com.qti.extphone.Status;
import com.qti.extphone.Token;
import com.qti.extphone.UpperLayerIndInfo;

public interface IQtiRadioConnectionCallback {
    void onNrIconType(int slotId, Token token, Status status,
                      NrIconType nrIconType);

    /**
     * Response to enableEndc
     * @param - slotId
     * @param - token is the same token which is recived in enableEndc
     * @param - status SUCCESS/FAILURE based on the modem Result code
     */
    void onEnableEndc(int slotId, Token token, Status status);

    /**
     * Response to queryEndcStatus
     * @param - slotId
     * @param - token is the same token which is recived in queryEndcStatus
     * @param - status SUCCESS/FAILURE based on the modem Result code
     * @param - enableStatus true if endc is enabled otherwise false
     */
    void onEndcStatus(int slotId, Token token, Status status, boolean enableStatus);

    /**
     * Response to setNrConfig
     * @param - slotId
     * @param - token is the same token which is recived in setNrConfig
     * @param - status SUCCESS/FAILURE based on the modem Result code
     */
    void onSetNrConfig(int slotId, Token token, Status status);

    /**
     * Response to queryNrConfig
     * @param - slotId
     * @param - token is the same token which is recived in queryNrConfig
     * @param - status SUCCESS/FAILURE based on the modem Result code
     * @param - nrConfig: NSA + SA/NSA/SA
     */
    void onNrConfigStatus(int slotId, Token token, Status status, NrConfig nrConfig);

    /**
     * Response to setCarrierInfoForImsiEncryption
     * @param - slotId
     * @param - token is the same token which is recived in setNrConfig
     * @param - status SUCCESS/FAILURE based on the modem Result code
     *
     */
    void setCarrierInfoForImsiEncryptionResponse(int slotId, Token token, Status status,
            QRadioResponseInfo info);

    void on5gStatus(int slotId, Token token, Status status, boolean enableStatus);

    void onAnyNrBearerAllocation(int slotId, Token token, Status status,
            BearerAllocationStatus bearerStatus);

    void onNrDcParam(int slotId, Token token, Status status, DcParam dcParam);

    void onUpperLayerIndInfo(int slotId, Token token, Status status,
            UpperLayerIndInfo upperLayerInfo);

    void on5gConfigInfo(int slotId, Token token, Status status,
            NrConfigType nrConfigType);

    void onSignalStrength(int slotId, Token token, Status status,
            SignalStrength signalStrength);

    void getQtiRadioCapabilityResponse(int slotId, Token token, Status status, int raf);

    /**
    * Response to sendCdmaSms
    * @param - slotId
    * @param - token is the same token which is recived in sendCdmaSms
    * @param - status SUCCESS/FAILURE based on the modem Result code
    * @param sms Sms result struct as defined by SmsResult
    *
    */
    void sendCdmaSmsResponse(int slotId, Token token, Status status, SmsResult sms);

    void getCallForwardStatusResponse(int slotId, Token token, Status status,
            QtiCallForwardInfo[] callForwardInfoList);

    void getFacilityLockForAppResponse(int slotId, Token token, Status status, int[] result);

    void getImeiResponse(int slotId, Token token, Status status, QtiImeiInfo imeiInfo);

    void onImeiChange(int slotId, QtiImeiInfo imeiInfo);

    void onSendUserPreferenceForDataDuringVoiceCall(int slotId, Token token,
            Status status);

    void onDdsSwitchCapabilityChange(int slotId, Token token, Status status, boolean support);

    void onDdsSwitchCriteriaChange(int slotId, Token token, boolean telephonyDdsSwitch);

    void onDdsSwitchRecommendation(int slotId, Token token, int recommendedSlotId);

    void onDataDeactivateDelayTime(int slotId, Token token, long delayTimeMilliSecs);

    void onEpdgOverCellularDataSupported(int slotId, Token token, boolean support);

    void onMcfgRefresh(Token token, QtiMcfgRefreshInfo qtiRefreshInfo);

    void onSetNrUltraWidebandIconConfigResponse(int slotId, Token token, Status status);

    void startNetworkScanResponse(int slotId, Token token, int errorCode);

    void stopNetworkScanResponse(int slotId, Token token, int errorCode);

    void setNetworkSelectionModeManualResponse(int slotId, Token token, int errorCode);

    void setNetworkSelectionModeAutomaticResponse(int slotId, Token token, int errorCode);

    void getNetworkSelectionModeResponse(int slotId, Token token, Status status,
            NetworkSelectionMode modes);

    void networkScanResult(int slotId, Token token, NetworkScanResult nsr);

    /**
    * Response to getQosParameters
    *
    * @param - slotId
    * @param - token is the same token which is received in getQosParameters
    * @param - status SUCCESS/FAILURE based on the modem result code
    * @param - result QosParametersResult containing QoS parameters
    */
    void getQosParametersResponse(int slotId, Token token, Status status,
            QosParametersResult result);

    /**
    * Indication received when QoS parameters have changed for a data call
    *
    * @param - slotId
    * @param - cid Connection id of the data call which QoS parameters have changed
    * @param - result QosParametersResult containing QoS parameters
    */
    void onQosParametersChanged(int slotId, int cid, QosParametersResult result);

    /**
     * Indication received when persoSubState is unlocked either temporarily or permanently
     *
     * @param - slotId on which the persoSubState changed
     * @param - persoUnlockStatus which can be generally temporary or permanent.
     */
    void onSimPersoUnlockStatusChange(int slotId, QtiPersoUnlockStatus persoUnlockStatus);

    void onCiwlanAvailable(int slotId, boolean ciwlanAvailable);

    void setCiwlanModeUserPreferenceResponse(int slotId, Token token, Status status);

    void onCiwlanConfigChange(int slotId, CiwlanConfig ciwlanConfig);

    /**
     * Response to setCellularRoamingPreference
     *
     * @param - slotId
     * @param token - This is the same token which is received in setCellularRoamingPreference
     * @param status - SUCCESS/FAILURE based on the modem result code
     */
    void setCellularRoamingPreferenceResponse(int slotId, Token token, Status status);

    /**
     * Unsol msg to indicate changes to the NR icon
     *
     * @param slotId - Slot ID for which this indication is sent
     * @param icon - NR icon type as per NrIconType.aidl and additional information such as the Rx
     *               count
     */
    void onNrIconChange(int slotId, NrIcon icon);

    /**
     * Response to queryNrIcon
     *
     * @param slotId - Slot ID for which this response is sent
     * @param token - This is the same token which is sent from queryNrIcon
     * @param status - SUCCESS/FAILURE based on the modem result code
     * @param icon - NR icon type as per NrIconType.aidl and additional information such as the Rx
     *               count
     */
    void onNrIconResponse(int slotId, Token token, Status status, NrIcon icon);

    /**
     * Indication from modem to telephony to fetch the IccId of the available eSIM profiles.
     *
     * @param slotId - Slot ID on which the modem request is received
     * @param refNum - Reference Number receieved from the modem which needs to be sent in the
     *                 response
     */
    void onAllAvailableProfilesReq(int slotId, int refNum);

    /**
     * Indication from modem to telephony to activate the given eSIM profile.
     *
     * @param slotId - Slot ID on which the modem request is received
     * @param refNum - Reference Number receieved from the modem which needs to be sent in the
     *                 response once activation is done.
     * @param iccId - IccId of the eSIM profile which needs to be activated.
     */
    void onEnableProfileReq(int slotId, int refNum, String iccId);

    /**
     * Indication from modem to telephony to deactivate the given eSIM profile.
     *
     * @param slotId - Slot ID on which the modem request is received
     * @param refNum - Reference Number receieved from the modem which needs to be sent in the
     *                 response once deactivation is done.
     * @param iccId - IccId of the eSIM profile which needs to be deactivated.
     */
    void onDisableProfileReq(int slotId, int refNum, String iccId);

    /**
     * Response to sendAllEsimProfiles
     *
     * @param token - This is the token sent while sending the request sendAllEsimProfiles
     */
    void onSendAllEsimProfilesResponse(Token token);

    /**
     * Response to notifyEnableProfileStatus
     *
     * @param token - This is the token sent while sending the request notifyEnableProfileStatus
     */
    void onNotifyEnableProfileStatusResponse(Token token);

    /**
     * Response to notifyDisableProfileStatus
     *
     * @param token - This is the token sent while sending the request notifyDisableProfileStatus
     */
    void onNotifyDisableProfileStatusResponse(Token token);
}
