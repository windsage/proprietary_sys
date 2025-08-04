/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.os.RemoteException;

import com.qti.extphone.MsimPreference;
import com.qti.extphone.QtiSimType;
import com.qti.extphone.Token;

public interface IQtiRadioConfigConnectionInterface {
    /**
     * Query the status of Secure Mode
     *
     * @param token to match request/response. Responses must include the same token as requests.
     */
    public void getSecureModeStatus(Token token) throws RemoteException;

    /**
     * Set MSIM preference to either DSDS or DSDA
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param pref MSIM prefrence either DSDS or DSDA.
     */
    public void setMsimPreference(Token token, MsimPreference pref) throws RemoteException;

    /**
     * Get the number of SIM types supported on each slot, whether Physical, eSIM or
     * Integrated(iUICC), and also to get which SIM Type is currently enabled
     *
     * @param token to match request/response. Responses must include the same token as requests.
     */
    public void getSimTypeInfo(Token token) throws RemoteException;

    /**
     * Set the SIM Type to Physical or Integrated(iUICC)
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param simType contains the SIM Type to be switched to on each slot
     */
    public void setSimType(Token token, QtiSimType[] simType) throws RemoteException;

    /**
     * Request dual data capability.
     * It is a static modem capability.
     * Response function is IQtiRadioConfigConnectionCallback#onDualDataCapabilityChanged().
     *
     * @param token to match request/response. Responses must include the same token as requests.
     */
    public void getDualDataCapability(Token token) throws RemoteException;

    /**
     * Set dual data user preference.
     * In a multi-SIM device, inform modem if user wants dual data feature or not.
     * Modem will not send any recommendations to HLOS to support dual data
     * if user does not opt in the feature even if UE is dual data capable.
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param enable Dual data selection opted by user. True if preference is enabled.
     */
    public void setDualDataUserPreference(Token token, boolean enable) throws RemoteException;

    /**
     * Request CIWLAN capability.
     * It is a static modem capability.
     * Response function is IQtiRadioConfigConnectionCallback#onCiwlanCapabilityChanged().
     *
     * @param token to match request/response. Responses must include the same token as requests.
     */
    public void getCiwlanCapability(Token token) throws RemoteException;

    /**
     * Request for Smart Temp DDS Switch capability from the modem. This determines the overall
     * capability of the Smart Temp DDS switch feature.
     *
     * This is a slot-agnostic variant of
     * {@link IQtiRadioConnectionInterface#getDdsSwitchCapability}, and should be preferred.
     *
     * @param token to match request/response. Responses must include the same token as requests.
     *
     * Response function is IQtiRadioConfigConnectionCallback.onDdsSwitchCapabilityChange().
     */
    public void getDdsSwitchCapability(Token token) throws RemoteException;

    /**
     * Inform modem whether we allow Temp DDS Switch to the individual slots. This takes
     * into account factors like the switch state of ‘Data During Calls’ setting, the
     * current roaming state of the individual subscriptions and their data roaming
     * enabled state.
     * If data during calls is allowed, modem can send recommendations to switch
     * DDS during a voice call on the non-DDS.
     *
     * This is a slot-agnostic variant of
     * {@link IQtiRadioConnectionInterface#sendUserPreferenceForDataDuringVoiceCall},
     * and should be preferred.
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param isAllowedOnSlot vector containing a boolean per slot that determines whether
     *        we allow temporary DDS switch to that slot.
     *
     * Response function is
     * IQtiRadioConfigConnectionCallback.onSendUserPreferenceForDataDuringVoiceCall().
     */
    public void sendUserPreferenceForDataDuringVoiceCall(Token token, boolean[] isAllowedOnSlot)
            throws RemoteException;

    /**
     * Register callback to be called in response to requests/indications
     *
     * @param callback that gets called when a response to a request or an indication is received
     */
    public void registerCallback(IQtiRadioConfigConnectionCallback callback);

    /**
     * Unregister callback previously registered to be called in response to requests/indications
     *
     * @param callback that needs to be unregistered
     */
    public void unregisterCallback(IQtiRadioConfigConnectionCallback callback);

    /**
     * Returns the version of the underlying HAL.
     */
    public int getHalVersion();

    /**
     * Check if Modem/RIL supports a particular feature.
     *
     * @param int feature is the telephony side integer mapping for a particular feature.
     *
     * @return boolean true if feature is supported by RIL/modem, false otherwise.
     */
    public boolean isFeatureSupported(int feature);
}
