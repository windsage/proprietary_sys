/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.os.RemoteException;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.util.Log;

import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.NrConfig;
import com.qti.extphone.NrIcon;
import com.qti.extphone.NrIconType;
import com.qti.extphone.QtiPersoUnlockStatus;
import com.qti.extphone.QtiSetNetworkSelectionMode;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.ArrayList;
import java.util.List;

import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandwidthInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTime;

/*
 * Default HAL class that is invoked when no IQtiRadio HAL is available.
 * Typical use case for this when the target does not support telephony/ril
 */

public class QtiRadioNotSupportedHal implements IQtiRadioConnectionInterface {

    private static final String TAG = "QtiRadioNotSupportedHal";

    private void fail() throws RemoteException {
        throw new RemoteException("Radio is not supported");
    }

    // Implementation of IQtiRadio java interface where all methods throw an exception
    @Override
    public int getPropertyValueInt(String property, int def) throws RemoteException {
        fail();
        return -1;
    }

    @Override
    public boolean getPropertyValueBool(String property, boolean def) throws RemoteException {
        fail();
        return false;
    }

    @Override
    public String getPropertyValueString(String property, String def) throws RemoteException {
        fail();
        return null;
    }

    @Override
    public void enableEndc(boolean enable, Token token) throws RemoteException {
        fail();
    }

    @Override
    public void queryNrIconType(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void queryEndcStatus(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void setNrConfig(NrConfig config, Token token) throws RemoteException {
        fail();
    }

    @Override
    public void queryNrConfig(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void setCarrierInfoForImsiEncryption(Token token,
            ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException {
        fail();
    }

    @Override
    public void sendCdmaSms(byte[] pdu, boolean expectMore, Token token) throws RemoteException {
        fail();
    }

    @Override
    public void enable5g(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void disable5g(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void queryNrBearerAllocation(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void  enable5gOnly(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void  query5gStatus(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void  queryNrDcParam(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void  queryNrSignalStrength(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void  queryUpperLayerIndInfo(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void  query5gConfigInfo(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void getQtiRadioCapability(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void queryCallForwardStatus(Token token, int cfReason, int serviceClass,
            String number, boolean expectMore) throws RemoteException {
        fail();
    }

    @Override
    public void getFacilityLockForApp(Token token, String facility, String password,
            int serviceClass, String appId, boolean expectMore) throws RemoteException {
        fail();
    }

    @Override
    public void getImei(Token token) throws RemoteException {
        fail();
    }

    @Override
    public void initQtiRadio() {
        Log.e(TAG, "initQtiRadio not supported in HIDL");
    }

    @Override
    public boolean isFeatureSupported(int feature) {
        Log.e(TAG, "isFeatureSupported not supported");
        return false;
    }

    @Override
    public void getDdsSwitchCapability(Token token) {
        Log.e(TAG, "getDdsSwitchCapability not supported");
    }

    @Override
    public void sendUserPreferenceForDataDuringVoiceCall(Token token,
            boolean userPreference) {
        Log.e(TAG, "sendUserPreferenceForDataDuringVoiceCall not supported");
    }

    @Override
    public boolean isEpdgOverCellularDataSupported() {
        Log.e(TAG, "isEpdgOverCellularDataSupported not supported");
        return false;
    }

    @Override
    public void getQosParameters(Token token, int cid) {
        Log.e(TAG, "getQosParameters not supported");
    }

    @java.lang.Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest,
            Token token) throws RemoteException {
        // Feature not supported for HIDL
    }

    @java.lang.Override
    public void stopNetworkScan(Token token) throws RemoteException {
        // Feature not supported for HIDL
    }

    @java.lang.Override
    public void setNetworkSelectionModeManual(QtiSetNetworkSelectionMode mode,
            Token token) throws RemoteException {
        // Feature not supported for HIDL
    }

    @java.lang.Override
    public void setNetworkSelectionModeAutomatic(int accessType, Token token)
            throws RemoteException {
        // Feature not supported for HIDL
    }

    @java.lang.Override
    public void getNetworkSelectionMode(Token token)
            throws RemoteException {
        // Feature not supported for HIDL
    }

    @Override
    public void setNrUltraWidebandIconConfig(Token token, int sib2Value,
            NrUwbIconBandInfo saBandInfo, NrUwbIconBandInfo nsaBandInfo,
            ArrayList<NrUwbIconRefreshTime> refreshTimes,
            NrUwbIconBandwidthInfo bandwidthInfo) throws RemoteException {
        Log.e(TAG, "setNrUltraWidebandIconConfig not supported");
    }

    public CiwlanConfig getCiwlanConfig() throws RemoteException {
        Log.e(TAG, "getCiwlanConfig not supported");
        return null;
    }

    @Override
    public QtiPersoUnlockStatus getSimPersoUnlockStatus() throws RemoteException {
        Log.e(TAG, "getSimPersoUnlockStatus not supported for HIDL");
        return null;
    }

    @Override
    public boolean isCiwlanAvailable() {
        Log.e(TAG, "isCiwlanAvailable not supported ");
        return false;
    }

    @Override
    public void setCiwlanModeUserPreference(Token token, CiwlanConfig ciwlanConfig)
            throws RemoteException {
        Log.e(TAG, "setCiwlanModeUserPreference not supported");
    }

    @Override
    public CiwlanConfig getCiwlanModeUserPreference() {
        Log.e(TAG, "getCiwlanModeUserPreference not supported");
        return null;
    }

    @Override
    public CellularRoamingPreference getCellularRoamingPreference() throws RemoteException {
        Log.e(TAG, "getCellularRoamingPreference not supported for HIDL");
        return null;
    }

    @Override
    public void setCellularRoamingPreference(Token token, CellularRoamingPreference pref)
            throws RemoteException {
        Log.e(TAG, "setCellularRoamingPreference not supported for HIDL");
    }

    @Override
    public boolean isEmcSupported() {
        Log.e(TAG, "isEmcSupported not supported ");
        return false;
    }

    @Override
    public boolean isEmfSupported() {
        Log.e(TAG, "isEmfSupported not supported ");
        return false;
    }

    @Override
    public void queryNrIcon(Token token) throws RemoteException {
        Log.e(TAG, "queryNrIcon not supported");
    }

    @Override
    public void sendAllEsimProfiles(Token token, boolean status, int refNum, List<String> iccIds)
            throws RemoteException {
        Log.e(TAG, "sendAllEsimProfiles not supported in HIDL");
    }

    @Override
    public void notifyEnableProfileStatus(Token token, int refNum, boolean result)
            throws RemoteException {
        Log.e(TAG, "notifyEnableProfileStatus not supported in HIDL");
    }

    @Override
    public void notifyDisableProfileStatus(Token token, int refNum, boolean result)
            throws RemoteException {
        Log.e(TAG, "notifyDisableProfileStatus not supported in HIDL");
    }

    @Override
    public void registerCallback(IQtiRadioConnectionCallback callback) {
        Log.e(TAG, "registerCallback not supported");
        return;
    }

    @Override
    public void unRegisterCallback(IQtiRadioConnectionCallback callback) {
        Log.e(TAG, "unRegisterCallback not supported");
        return;
    }
}
