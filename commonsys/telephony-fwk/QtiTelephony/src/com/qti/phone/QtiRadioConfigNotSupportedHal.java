/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.os.RemoteException;
import android.util.Log;

import com.qti.extphone.MsimPreference;
import com.qti.extphone.QtiSimType;
import com.qti.extphone.Token;

/*
 * Default HAL class that is invoked when no IQtiRadioConfig HAL is available.
 * Typical use case for this is when the target does not support telephony/ril.
 */
public class QtiRadioConfigNotSupportedHal implements IQtiRadioConfigConnectionInterface {

    private static final String TAG = "QtiRadioConfigNotSupportedHal";

    public static final int QTIRADIOCONFIG_HAL_VERSION_UNKNOWN = -1;

    public int getHalVersion() {
        return QTIRADIOCONFIG_HAL_VERSION_UNKNOWN;
    }

    private void fail() throws RemoteException {
        throw new RemoteException("Radio is not supported");
    }

    // Implementation of IQtiRadioConfig java interface where all methods throw an exception
    public void getSecureModeStatus(Token token) throws RemoteException {
        fail();
        Log.e(TAG, "getSecureModeStatus not supported");
    }

    public void setMsimPreference(Token token, MsimPreference pref) throws RemoteException {
        fail();
        Log.e(TAG, "setMsimPreference not supported");
    }

    /**
     * Request dual data capability.
     *
     * @param token to match request/response. Responses must include the same token as requests.
     */
    @Override
    public void getDualDataCapability(Token token) throws RemoteException {
        fail();
        Log.e(TAG, "getDualDataCapability not supported");
    }

    /**
     * Set dual data user preference.
     * In a multi-SIM device, inform modem if user wants dual data feature or not.
     * Modem will not send any recommendations to HLOS to support dual data
     * if user does not opt in the feature even if UE is dual data capable.
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param enable Dual data selection opted by user. True if preference is enabled.
     */
    @Override
    public void setDualDataUserPreference(Token token, boolean enable) throws RemoteException {
        fail();
        Log.e(TAG, "setDualDataUserPreference not supported");
    }

    /**
     * Request CIWLAN capability.
     *
     * @param token to match request/response. Responses must include the same token as requests.
     */
    @Override
    public void getCiwlanCapability(Token token) throws RemoteException {
        fail();
        Log.e(TAG, "getCiwlanCapability not supported");
    }

    @Override
    public void getDdsSwitchCapability(Token token) throws RemoteException {
        fail();
        Log.e(TAG, "getDdsSwitchCapability not supported");
    }

    @Override
    public void sendUserPreferenceForDataDuringVoiceCall(Token token, boolean[] isAllowedOnSlot)
            throws RemoteException {
        fail();
        Log.e(TAG, "sendUserPreferenceForDataDuringVoiceCall not supported");
    }

    @Override
    public void getSimTypeInfo(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void setSimType(Token token, QtiSimType[] simType) throws RemoteException {
        fail();
    }

    @Override
    public void registerCallback(IQtiRadioConfigConnectionCallback callback) {
        Log.e(TAG, "registerCallback not supported");
    }

    @Override
    public void unregisterCallback(IQtiRadioConfigConnectionCallback callback) {
        Log.e(TAG, "unRegisterCallback not supported");
    }

    @Override
    public boolean isFeatureSupported(int feature) {
        Log.e(TAG, "isFeatureSupported not supported");
        return false;
    }
}
