/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.os.RemoteException;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;

import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.NrConfig;
import com.qti.extphone.QtiPersoUnlockStatus;
import com.qti.extphone.QtiSetNetworkSelectionMode;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.ArrayList;
import java.util.List;

import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandwidthInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTime;

public interface IQtiRadioConnectionInterface {
    public void initQtiRadio();

    public int getPropertyValueInt(String property, int def) throws RemoteException;

    public boolean getPropertyValueBool(String property, boolean def) throws RemoteException;

    public String getPropertyValueString(String property, String def) throws RemoteException;

    public void enableEndc(boolean enable, Token token) throws RemoteException;

    public void queryNrIconType(Token token) throws RemoteException;

    public void queryEndcStatus(Token token) throws RemoteException;

    public void setNrConfig(NrConfig config, Token token) throws RemoteException;

    public void setNetworkSelectionModeAutomatic(int accessType,
            Token token) throws RemoteException;

    public void getNetworkSelectionMode(Token token) throws RemoteException;

    public void queryNrConfig(Token token) throws RemoteException;

    public void setCarrierInfoForImsiEncryption(Token token,
            ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException;

    public void sendCdmaSms(byte[] pdu, boolean expectMore, Token token) throws RemoteException;

    public void startNetworkScan(NetworkScanRequest networkScanRequest,
            Token token) throws RemoteException;

    public void stopNetworkScan(Token token) throws RemoteException;

    public void setNetworkSelectionModeManual(QtiSetNetworkSelectionMode mode,
            Token token) throws RemoteException;

    public void enable5g(Token token) throws RemoteException;

    public void disable5g(Token token) throws RemoteException;

    public void queryNrBearerAllocation(Token token) throws RemoteException;

    public void  enable5gOnly(Token token) throws RemoteException;

    public void  query5gStatus(Token token) throws RemoteException;

    public void  queryNrDcParam(Token token) throws RemoteException;

    public void  queryNrSignalStrength(Token token) throws RemoteException;

    public void  queryUpperLayerIndInfo(Token token) throws RemoteException;

    public void  query5gConfigInfo(Token token) throws RemoteException;

    public void getQtiRadioCapability(Token token) throws RemoteException;

    public void queryCallForwardStatus(Token token, int cfReason, int serviceClass,
            String number, boolean expectMore) throws RemoteException;

    public void getImei(Token token) throws RemoteException;

    public void getFacilityLockForApp(Token token, String facility, String password,
                int serviceClass, String appId, boolean expectMore) throws RemoteException;

    public boolean isFeatureSupported(int feature);

    public void getDdsSwitchCapability(Token token);

    public void sendUserPreferenceForDataDuringVoiceCall(Token token,
            boolean userPreference);

    public boolean isEpdgOverCellularDataSupported() throws RemoteException;

    public void setNrUltraWidebandIconConfig(Token token, int sib2Value,
            NrUwbIconBandInfo saBandInfo, NrUwbIconBandInfo nsaBandInfo,
            ArrayList<NrUwbIconRefreshTime> refreshTimes,
            NrUwbIconBandwidthInfo bandwidthInfo) throws RemoteException;

    public void getQosParameters(Token token, int cid) throws RemoteException;

    public CiwlanConfig getCiwlanConfig() throws RemoteException;

    public QtiPersoUnlockStatus getSimPersoUnlockStatus() throws RemoteException;

    public boolean isCiwlanAvailable() throws RemoteException;

    public void setCiwlanModeUserPreference(Token token, CiwlanConfig ciwlanConfig)
            throws RemoteException;

    public CiwlanConfig getCiwlanModeUserPreference() throws RemoteException;

    public CellularRoamingPreference getCellularRoamingPreference() throws RemoteException;

    public void setCellularRoamingPreference(Token token, CellularRoamingPreference pref)
            throws RemoteException;

    public boolean isEmcSupported() throws RemoteException;

    public boolean isEmfSupported() throws RemoteException;

    public void queryNrIcon(Token token) throws RemoteException;

    public void sendAllEsimProfiles(Token token, boolean status, int refNum, List<String> iccIds)
            throws RemoteException;

    public void notifyEnableProfileStatus(Token token, int refNum, boolean result)
            throws RemoteException;

    public void notifyDisableProfileStatus(Token token, int refNum, boolean result)
            throws RemoteException;

    public void registerCallback(IQtiRadioConnectionCallback callback);

    public void unRegisterCallback(IQtiRadioConnectionCallback callback);
}
