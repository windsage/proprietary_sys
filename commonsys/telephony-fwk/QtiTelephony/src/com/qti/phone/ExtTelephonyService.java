/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;

import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.IExtPhone;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.IDepersoResCallback;
import com.qti.extphone.MsimPreference;
import com.qti.extphone.NrConfig;
import com.qti.extphone.QtiImeiInfo;
import com.qti.extphone.QtiPersoUnlockStatus;
import com.qti.extphone.QtiSetNetworkSelectionMode;
import com.qti.extphone.QtiSimType;
import com.qti.extphone.Token;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class ExtTelephonyService extends Service {

    private static final String LOG_TAG = "ExtTelephonyService";
    private static Context mContext;
    private static final int SLOT_INVALID = -1;
    private ExtTelephonyServiceImpl mExtTelephonyServiceImpl;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "ExtTelephonyService created..");
        mContext = this;
        mExtTelephonyServiceImpl = new ExtTelephonyServiceImpl(mContext);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        mExtTelephonyServiceImpl.cleanUp();
        mExtTelephonyServiceImpl = null;
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "Service bound with " + this.getClass().getName());
        return mBinder;
    }

    private void enforceModifyPhoneState(String message) {
        Log.d(LOG_TAG, "enforceModifyPhoneState for " + message);
        this.enforceCallingOrSelfPermission(
                android.Manifest.permission.MODIFY_PHONE_STATE, message);
    }

    private void enforceReadPrivilegedPermission(String message) {
        Log.d(LOG_TAG, "enforceReadPrivilegedPermission for " + message);
        this.enforceCallingOrSelfPermission(
                android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE, message);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        mExtTelephonyServiceImpl.dump(fd, printwriter, args);
    }

    /*
     * Implement the methods of the IExtPhone interface in this stub.
     */
    private final IExtPhone.Stub mBinder = new IExtPhone.Stub() {

        @Override
        public int getPropertyValueInt(String property, int def) throws RemoteException {
            enforceReadPrivilegedPermission("getPropertyValueInt");
            Log.d(LOG_TAG, "getPropertyValueInt property=" + property);
            return mExtTelephonyServiceImpl.getPropertyValueInt(property, def);
        }

        @Override
        public boolean getPropertyValueBool(String property, boolean def) throws RemoteException {
            enforceReadPrivilegedPermission("getPropertyValueBool");
            Log.d(LOG_TAG, "getPropertyValueBool property=" + property);
            return mExtTelephonyServiceImpl.getPropertyValueBool(property, def);
        }

        @Override
        public String getPropertyValueString(String property, String def) throws RemoteException {
            enforceReadPrivilegedPermission("getPropertyValueString");
            Log.d(LOG_TAG, "getPropertyValueString property=" + property);
            return mExtTelephonyServiceImpl.getPropertyValueString(property, def);
        }

        @Override
        public int getCurrentPrimaryCardSlotId() {
            enforceReadPrivilegedPermission("getCurrentPrimaryCardSlotId");
            Log.d(LOG_TAG, "getCurrentPrimaryCardSlotId");
            return mExtTelephonyServiceImpl.getCurrentPrimaryCardSlotId();
        }

        @Override
        public boolean isPrimaryCarrierSlotId(int slotId) {
            enforceReadPrivilegedPermission("isPrimaryCarrierSlotId");
            Log.d(LOG_TAG, "isPrimaryCarrierSlotId slotId=" + slotId);
            return mExtTelephonyServiceImpl.isPrimaryCarrierSlotId(slotId);
        }

        @Override
        public void setPrimaryCardOnSlot(int slotId) {
            enforceModifyPhoneState("setPrimaryCardOnSlot");
            // TODO  stub implementation pending
        }

        @Override
        public int getPrimaryCarrierSlotId() {
            enforceReadPrivilegedPermission("getPrimaryCarrierSlotId");
            // TODO  stub implementation pending
            return SLOT_INVALID;
        }

        @Override
        public boolean performIncrementalScan(int slotId) {
            Log.d(LOG_TAG, "performIncrementalScan slotId=" + slotId);
            enforceModifyPhoneState("performIncrementalScan");
            return mExtTelephonyServiceImpl.performIncrementalScan(slotId);
        }

        @Override
        public boolean abortIncrementalScan(int slotId) {
            Log.d(LOG_TAG, "abortIncrementalScan slotId=" + slotId);
            enforceModifyPhoneState("abortIncrementalScan");
            return mExtTelephonyServiceImpl.abortIncrementalScan(slotId);
        }

        @Override
        public boolean isSMSPromptEnabled() {
            enforceReadPrivilegedPermission("isSMSPromptEnabled");
            Log.d(LOG_TAG, "isSMSPromptEnabled");
            return mExtTelephonyServiceImpl.isSMSPromptEnabled();
        }

        @Override
        public void setSMSPromptEnabled(boolean enabled) {
            enforceModifyPhoneState("setSMSPromptEnabled");
            mExtTelephonyServiceImpl.setSMSPromptEnabled(enabled);
        }

        @Override
        public void supplyIccDepersonalization(String netpin, String type,
                IDepersoResCallback callback, int phoneId) {
            enforceModifyPhoneState("supplyIccDepersonalization");
            Log.d(LOG_TAG, "supplyIccDepersonalization phoneId=" + phoneId);
            mExtTelephonyServiceImpl.supplyIccDepersonalization(
                    netpin, type, callback, phoneId);
        }

        @Override
        public Token enableEndc(int slot, boolean enable, Client client) throws RemoteException {
            enforceModifyPhoneState("enableEndc");
            Log.d(LOG_TAG, "enableEndc slot=" + slot);
            return mExtTelephonyServiceImpl.enableEndc(slot, enable, client);
        }

        @Override
        public Token queryNrIconType(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("queryNrIconType");
            Log.d(LOG_TAG, "queryNrIconType slot=" + slot);
            return mExtTelephonyServiceImpl.queryNrIconType(slot, client);
        }

        @Override
        public Token queryEndcStatus(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("queryEndcStatus");
            Log.d(LOG_TAG, "queryEndcStatus slot=" + slot);
            return mExtTelephonyServiceImpl.queryEndcStatus(slot, client);
        }

        @Override
        public Token setNrConfig(int slot, NrConfig config, Client client) throws RemoteException {
            enforceModifyPhoneState("setNrConfig");
            Log.d(LOG_TAG, "setNrConfig slot=" + slot);
            return mExtTelephonyServiceImpl.setNrConfig(slot, config, client);
        }

        @Override
        public Token setNetworkSelectionModeAutomatic(int slot, int accessType, Client client)
                throws RemoteException {
            enforceModifyPhoneState("setNetworkSelectionModeAutomatic");
            Log.d(LOG_TAG, "setNetworkSelectionModeAutomatic slot=" + slot);
            return mExtTelephonyServiceImpl.setNetworkSelectionModeAutomatic(slot, accessType,
                    client);
        }

        @Override
        public Token getNetworkSelectionMode(int slot, Client client)
                throws RemoteException {
            enforceModifyPhoneState("getNetworkSelectionMode");
            Log.d(LOG_TAG, "getNetworkSelectionMode slot=" + slot);
            return mExtTelephonyServiceImpl.getNetworkSelectionMode(slot, client);
        }

        @Override
        public Token queryNrConfig(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("queryNrConfig");
            Log.d(LOG_TAG, "queryNrConfig slot=" + slot);
            return mExtTelephonyServiceImpl.queryNrConfig(slot, client);
        }

        @Override
        public Token sendCdmaSms(int slot, byte[] pdu,
                boolean expectMore, Client client) throws RemoteException {
            enforceModifyPhoneState("sendCdmaSms");
            Log.d(LOG_TAG, "sendCdmaSms slot=" + slot);
            return mExtTelephonyServiceImpl.sendCdmaSms(slot, pdu, expectMore, client);
        }

        @Override
        public Token startNetworkScan(int slot, NetworkScanRequest networkScanRequest,
                Client client) throws RemoteException {
            enforceModifyPhoneState("startNetworkScan");
            Log.d(LOG_TAG, "startNetworkScan slot=" + slot);
            return mExtTelephonyServiceImpl.startNetworkScan(slot, networkScanRequest, client);
        }

        @Override
        public Token stopNetworkScan(int slot, Client client) throws RemoteException {
            enforceModifyPhoneState("stopNetworkScan");
            Log.d(LOG_TAG, "stopNetworkScan slot=" + slot);
            return mExtTelephonyServiceImpl.stopNetworkScan(slot, client);
        }

        @Override
        public Token setNetworkSelectionModeManual(int slot, QtiSetNetworkSelectionMode mode,
                Client client) throws RemoteException {
            enforceModifyPhoneState("setNetworkSelectionModeManual");
            Log.d(LOG_TAG, "setNetworkSelectionModeManual slot=" + slot);
            return mExtTelephonyServiceImpl.setNetworkSelectionModeManual(slot, mode, client);
        }

        @Override
        public Token enable5g(int slot, Client client) throws RemoteException {
            enforceModifyPhoneState("enable5g");
            Log.d(LOG_TAG, "enable5g slot=" + slot);
            return mExtTelephonyServiceImpl.enable5g(slot, client);
        }

        @Override
        public Token disable5g(int slot, Client client) throws RemoteException {
            enforceModifyPhoneState("disable5g");
            Log.d(LOG_TAG, "disable5g slot=" + slot);
            return mExtTelephonyServiceImpl.disable5g(slot, client);
        }

        @Override
        public Token queryNrBearerAllocation(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("queryNrBearerAllocation");
            Log.d(LOG_TAG, "queryNrBearerAllocation slot=" + slot);
            return mExtTelephonyServiceImpl.queryNrBearerAllocation(slot, client);
        }

        @Override
        public Token getQtiRadioCapability(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("getQtiRadioCapability");
            Log.d(LOG_TAG, "getQtiRadioCapability slot=" + slot);
            return mExtTelephonyServiceImpl.getQtiRadioCapability(slot, client);
        }

        @Override
        public Token enable5gOnly(int slot, Client client) throws RemoteException {
            enforceModifyPhoneState("enable5gOnly");
            Log.d(LOG_TAG, "enable5gOnly slot=" + slot);
            return mExtTelephonyServiceImpl.enable5gOnly(slot, client);
        }

        @Override
        public Token query5gStatus(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("query5gStatus");
            Log.d(LOG_TAG, "query5gStatus slot=" + slot);
            return mExtTelephonyServiceImpl.query5gStatus(slot, client);
        }

        @Override
        public Token queryNrDcParam(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("queryNrDcParam");
            Log.d(LOG_TAG, "queryNrDcParam slot=" + slot);
            return mExtTelephonyServiceImpl.queryNrDcParam(slot, client);
        }

        @Override
        public Token queryNrSignalStrength(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("queryNrSignalStrength");
            Log.d(LOG_TAG, "queryNrSignalStrength slot=" + slot);
            return mExtTelephonyServiceImpl.queryNrSignalStrength(slot, client);
        }

        @Override
        public Token queryUpperLayerIndInfo(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("queryUpperLayerIndInfo");
            Log.d(LOG_TAG, "queryUpperLayerIndInfo slot=" + slot);
            return mExtTelephonyServiceImpl.queryUpperLayerIndInfo(slot, client);
        }

        @Override
        public Token query5gConfigInfo(int slot, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("query5gConfigInfo");
            Log.d(LOG_TAG, "query5gConfigInfo slot=" + slot);
            return mExtTelephonyServiceImpl.query5gConfigInfo(slot, client);
        }

        @Override
        public Token setCarrierInfoForImsiEncryption(int slot, ImsiEncryptionInfo info,
                Client client) throws RemoteException {
            enforceModifyPhoneState("setCarrierInfoForImsiEncryption");
            return mExtTelephonyServiceImpl.setCarrierInfoForImsiEncryption(slot, info, client);
        }

        @Override
        public void queryCallForwardStatus(int slotId, int cfReason, int serviceClass,
                String number, boolean expectMore, Client client) throws RemoteException {
            Log.d(LOG_TAG, "queryCallForwardStatus: " +slotId);
            mExtTelephonyServiceImpl.queryCallForwardStatus(slotId, cfReason, serviceClass,
                    number, expectMore, client);
        }

        @Override
        public void getFacilityLockForApp(int slotId, String facility, String password,
                int serviceClass, String appId, boolean expectMore, Client client)
                throws RemoteException {
            Log.d(LOG_TAG, "getFacilityLockForApp: " +slotId);
            mExtTelephonyServiceImpl.
                    getFacilityLockForApp(slotId, facility, password, serviceClass, appId,
                    expectMore, client);
        }

        @Override
        public QtiImeiInfo[] getImeiInfo() throws RemoteException {
            enforceReadPrivilegedPermission("getImeiInfo");
            Log.d(LOG_TAG, "getImeiInfo: ");
            return mExtTelephonyServiceImpl.getImeiInfo();
        }

        @Override
        public boolean isSmartDdsSwitchFeatureAvailable() {
            enforceModifyPhoneState("isSmartDdsSwitchFeatureAvailable");
            // Smart DDS switch feature not available for Subsidy device.
            return mExtTelephonyServiceImpl.isSmartDdsSwitchFeatureAvailable()
                    && !mExtTelephonyServiceImpl.isSubsidyFeatureEnabled();
        }

        @Override
        public void setSmartDdsSwitchToggle(boolean isEnabled, Client client)
                throws RemoteException {
            enforceModifyPhoneState("setSmartDdsSwitchToggle");
            mExtTelephonyServiceImpl.setSmartDdsSwitchToggle(isEnabled, client);
        }

        @Override
        public boolean setAirplaneMode(boolean on) {
            enforceModifyPhoneState("setAirplaneMode");
            return mExtTelephonyServiceImpl.setAirplaneMode(on);
        }

        @Override
        public boolean getAirplaneMode() {
            enforceReadPrivilegedPermission("getAirplaneMode");
            return mExtTelephonyServiceImpl.getAirplaneMode();
        }

        @Override
        public boolean checkSimPinLockStatus(int subId) {
            enforceReadPrivilegedPermission("checkSimPinLockStatus");
            return mExtTelephonyServiceImpl.checkSimPinLockStatus(subId);
        }

        @Override
        public boolean toggleSimPinLock(int subId, boolean enabled, String pin) {
            enforceModifyPhoneState("toggleSimPinLock");
            return mExtTelephonyServiceImpl.toggleSimPinLock(subId, enabled, pin);
        }

        @Override
        public boolean verifySimPin(int subId, String pin) {
            enforceReadPrivilegedPermission("verifySimPin");
            return mExtTelephonyServiceImpl.verifySimPin(subId, pin);
        }

        @Override
        public boolean verifySimPukChangePin(int subId, String puk, String newPin) {
            enforceModifyPhoneState("verifySimPukChangePin");
            return mExtTelephonyServiceImpl.verifySimPukChangePin(subId, puk, newPin);
        }

        @Override
        public boolean isFeatureSupported(int feature) {
            Log.d(LOG_TAG, "isFeatureSupported: " + feature);
            return mExtTelephonyServiceImpl.isFeatureSupported(feature);
        }

        @Override
        public Token sendUserPreferenceForDataDuringVoiceCall(int slotId,
                boolean userPreference, Client client) throws RemoteException {
            return mExtTelephonyServiceImpl.sendUserPreferenceForDataDuringVoiceCall(
                    slotId, userPreference, client);
        }

        @Override
        public Token sendUserPreferenceConfigForDataDuringVoiceCall(boolean[] isAllowedOnSlot,
                Client client) throws RemoteException {
            return mExtTelephonyServiceImpl.sendUserPreferenceConfigForDataDuringVoiceCall(
                isAllowedOnSlot, client);
        }

        @Override
        public Token getDdsSwitchCapability(int slotId, Client client) throws RemoteException {
            return mExtTelephonyServiceImpl.getDdsSwitchCapability(slotId, client);
        }

        @Override
        public Token getDdsSwitchConfigCapability(Client client) throws RemoteException {
            return mExtTelephonyServiceImpl.getDdsSwitchConfigCapability(client);
        }

        public Token getSecureModeStatus(Client client) throws RemoteException {
            enforceReadPrivilegedPermission("getSecureModeStatus");
            return mExtTelephonyServiceImpl.getSecureModeStatus(client);
        }

        @Override
        public boolean isEpdgOverCellularDataSupported(int slotId)
                throws RemoteException {
            return mExtTelephonyServiceImpl.isEpdgOverCellularDataSupported(slotId);
        }

        @Override
        public Token getQosParameters(int slot, int cid, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("getQosParameters");
            return mExtTelephonyServiceImpl.getQosParameters(slot, cid, client);
        }

        @Override
        public Token setMsimPreference(Client client, MsimPreference pref)
                throws RemoteException {
            return mExtTelephonyServiceImpl.setMsimPreference(client, pref);
        }

        @Override
        public QtiSimType[] getCurrentSimType() throws RemoteException {
            enforceReadPrivilegedPermission("getCurrentSimType");
            return mExtTelephonyServiceImpl.getCurrentSimType();
        }

        @Override
        public QtiSimType[] getSupportedSimTypes() throws RemoteException {
            enforceReadPrivilegedPermission("getSupportedSimTypes");
            return mExtTelephonyServiceImpl.getSupportedSimTypes();
        }

        @Override
        public Token setSimType(Client client, QtiSimType[] simType) throws RemoteException {
            enforceModifyPhoneState("setSimType");
            Log.d(LOG_TAG, "setSimType");
            return mExtTelephonyServiceImpl.setSimType(client, simType);
        }

        @Override
        public CiwlanConfig getCiwlanConfig(int slotId) throws RemoteException {
            enforceReadPrivilegedPermission("getCiwlanConfig");
            return mExtTelephonyServiceImpl.getCiwlanConfig(slotId);
        }

        @Override
        public boolean getDualDataCapability() {
            return mExtTelephonyServiceImpl.getDualDataCapability();
        }

        @Override
        public Token setDualDataUserPreference(Client client, boolean preference)
                throws RemoteException {
            return mExtTelephonyServiceImpl.setDualDataUserPreference(client, preference);
        }

        @Override
        public QtiPersoUnlockStatus getSimPersoUnlockStatus(int slotId) throws RemoteException {
            return mExtTelephonyServiceImpl.getSimPersoUnlockStatus(slotId);
        }

        @Override
        public boolean isCiwlanAvailable(int slotId) throws RemoteException {
            return mExtTelephonyServiceImpl.isCiwlanAvailable(slotId);
        }

        @Override
        public Token setCiwlanModeUserPreference(int slotId, Client client,
                CiwlanConfig ciwlanConfig) throws RemoteException {
            return mExtTelephonyServiceImpl.setCiwlanModeUserPreference(slotId,
                    client, ciwlanConfig);
        }

        @Override
        public CiwlanConfig getCiwlanModeUserPreference(int slotId) throws RemoteException {
            return mExtTelephonyServiceImpl.getCiwlanModeUserPreference(slotId);
        }

        public CellularRoamingPreference getCellularRoamingPreference(int slotId)
                throws RemoteException {
            return mExtTelephonyServiceImpl.getCellularRoamingPreference(slotId);
        }

        @Override
        public Token setCellularRoamingPreference(Client client, int slotId,
                CellularRoamingPreference pref) throws RemoteException {
            return mExtTelephonyServiceImpl.setCellularRoamingPreference(client, slotId, pref);
        }

        @Override
        public boolean isEmcSupported(int slotId) throws RemoteException {
            return mExtTelephonyServiceImpl.isEmcSupported(slotId);
        }

        @Override
        public boolean isEmfSupported(int slotId) throws RemoteException {
            return mExtTelephonyServiceImpl.isEmfSupported(slotId);
        }

        @Override
        public Token queryNrIcon(int slotId, Client client) throws RemoteException {
            enforceReadPrivilegedPermission("queryNrIcon");
            return mExtTelephonyServiceImpl.queryNrIcon(slotId, client);
        }

        @Override
        public Client registerCallback(String packageName, IExtPhoneCallback callback)
                throws RemoteException {
            enforceReadPrivilegedPermission("registerCallback");
            Log.d(LOG_TAG, "registerCallback packageName=" + packageName);
            int[] events = new int[]{
                    ExtPhoneCallbackListener.EVENT_ALL};
            return registerCallbackWithEvents(packageName, callback, events);
        }

        @Override
        public Client registerCallbackWithEvents(String packageName, IExtPhoneCallback callback,
                int[] events) throws RemoteException {
            enforceReadPrivilegedPermission("registerCallbackWithEvents");
            Log.d(LOG_TAG, "registerCallbackWithEvents packageName=" + packageName);
            return mExtTelephonyServiceImpl.registerCallbackWithEvents(packageName, callback,
                    events);
        }

        @Override
        public void unRegisterCallback(IExtPhoneCallback callback) throws RemoteException {
            enforceReadPrivilegedPermission("unRegisterCallback");
            Log.d(LOG_TAG, "unRegisterCallback....");
            mExtTelephonyServiceImpl.unregisterCallback(callback);
        }

        @Override
        public Client registerQtiRadioConfigCallback(String packageName, IExtPhoneCallback callback)
                throws RemoteException {
            enforceReadPrivilegedPermission("registerQtiRadioConfigCallback");
            Log.d(LOG_TAG, "registerQtiRadioConfigCallback packageName=" + packageName);
            int[] events = new int[]{
                    ExtPhoneCallbackListener.EVENT_ALL};
            return registerCallbackWithEvents(packageName, callback, events);
        }

        @Override
        public void unregisterQtiRadioConfigCallback(IExtPhoneCallback callback)
                throws RemoteException {
            enforceReadPrivilegedPermission("unregisterQtiRadioConfigCallback");
            Log.d(LOG_TAG, "unregisterQtiRadioConfigCallback...");
            mExtTelephonyServiceImpl.unregisterCallback(callback);
        }
    };
}
