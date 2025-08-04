/*
 * Copyright (c) 2018, 2022-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 */
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qualcomm.qti.internal.telephony;

import android.content.Context;
import android.hardware.radio.RadioError;
import android.os.Message;
import android.os.AsyncResult;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.NetworkScanResult;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.SmsResponse;

import com.qualcomm.qti.internal.telephony.QtiCarrierInfoManager.QtiCarrierInfoResponse;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.QRadioResponseInfo;
import com.qti.extphone.QosParametersResult;
import com.qti.extphone.QtiSetNetworkSelectionMode;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.SmsResult;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import com.qualcomm.qti.internal.telephony.data.FrameworkQosParameters;
import com.qualcomm.qti.internal.telephony.data.QtiDataServiceManager.QtiCellularDataServiceCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QtiPhoneUtils {
    private static final String TAG = "QtiPhoneUtils";

    private Context mContext;
    private static QtiPhoneUtils sInstance;
    private ExtTelephonyManager mExtTelephonyManager;
    private Client mClient;
    private final HashMap<Integer, Message> mPendingRequests = new HashMap<>();
    private QtiCarrierInfoResponse mQtiCarrierInfoResponse;
    private boolean mIsSmartDdsSwitchFeatureAvailable;
    private final HashMap<Integer, QtiCellularDataServiceCallback> mCellularDataServiceCallbacks =
            new HashMap<>();

    /** List of listeners to be notified when QtiPhoneUtils gets connected to ExtTelephonyService */
    private static List<OnQtiPhoneReadyListener> sReadyListeners = new ArrayList<>();

    private static final String PROPERTY_DSDS_TO_SS = "persist.vendor.radio.dsds_to_ss";
    private static int sDsdsToSsConfigStatus = -1;

    static QtiPhoneUtils init(Context context) {
        synchronized (QtiPhoneUtils.class) {
            if (sInstance == null) {
                sInstance = new QtiPhoneUtils(context);
            }
        }
        return sInstance;
    }

    public static QtiPhoneUtils getInstance() {
        synchronized (QtiPhoneUtils.class) {
            if (sInstance == null) {
                throw new RuntimeException("QtiPhoneUtils was not initialized!");
            }
            return sInstance;
        }
    }

    private QtiPhoneUtils(Context context) {
        mContext = context;
        mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
    }

    /**
     * Add a new listener to {@link sReadyListeners}.
     * The listener will be notified when QtiPhoneUtils gets connected to ExtTelephonyService.
     *
     * @param listener an implementation of {@link OnQtiPhoneReadyListener} interface
     */
    public static void addOnQtiPhoneReadyListener(OnQtiPhoneReadyListener listener) {
        sReadyListeners.add(listener);
    }

    /**
     * Interface to be implemented by clients interested in knowing when QtiPhoneUtils has
     * established a connection to ExtTelephonyService.
     */
    public static interface OnQtiPhoneReadyListener {
        void onQtiPhoneReady();
    }

    public void registerDataServiceCallbackForQos(int phoneId,
                                                  QtiCellularDataServiceCallback callback) {
        Rlog.d(TAG, "registerDataServiceCallbackForQos, phoneId: " + phoneId);
        mCellularDataServiceCallbacks.put(phoneId, callback);
    }

    public boolean getPropertyValueBool(String property, boolean def) {
      return mExtTelephonyManager.getPropertyValueBool(property, def);
    }

    public boolean isDsdsToSsConfigEnabled() {
        return sDsdsToSsConfigStatus > 0;
    }

    private void queryDsdsToSsConfig() {
        if (sDsdsToSsConfigStatus == -1) {
            sDsdsToSsConfigStatus = mExtTelephonyManager.
                    getPropertyValueInt(PROPERTY_DSDS_TO_SS, 0);
        }
        Rlog.d(TAG, "queryDsdsToSsConfig value = " + sDsdsToSsConfigStatus);
    }

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {

        @Override
        public void onConnected() {
            int[] events = new int[] {
                    ExtPhoneCallbackListener.EVENT_NETWORK_SCAN_RESULT,
                    ExtPhoneCallbackListener.EVENT_ON_QOS_PARAMETERS_CHANGED};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mContext.getPackageName(), mExtPhoneCallbackListener, events);
            Rlog.d(TAG, "mExtTelManagerServiceCallback: service connected " + mClient);
            queryDsdsToSsConfig();
            try {
                mIsSmartDdsSwitchFeatureAvailable =
                        mExtTelephonyManager.isSmartDdsSwitchFeatureAvailable();
                Rlog.d(TAG, "isSmartDdsSwitchFeatureAvailable: " +
                        mIsSmartDdsSwitchFeatureAvailable);
            } catch (RemoteException ex) {
                Rlog.e(TAG, "isSmartDdsSwitchFeatureAvailable exception " + ex);
            }
            for (OnQtiPhoneReadyListener listener : sReadyListeners) {
                listener.onQtiPhoneReady();
            }
        }

        @Override
        public void onDisconnected() {
            Rlog.d(TAG, "mExtTelManagerServiceCallback: service disconnected");
        }
    };

    protected ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void sendCdmaSmsResponse(int slotId, Token token, Status status, SmsResult sms) {
            Message msg = null;

            Rlog.d(TAG, "sendCdmaSmsResponse " + token.get());
            synchronized (mPendingRequests) {
                msg =  mPendingRequests.get(token.get());
            }
            SmsResponse ret = new SmsResponse(sms.getMessageRef(),
                    sms.getAckPDU(), sms.getErrorCode());
            if (status.get() == Status.SUCCESS) {
                if (msg != null) {
                    AsyncResult.forMessage(msg, ret, null);
                    msg.sendToTarget();
                }
            }
            synchronized (mPendingRequests) {
                mPendingRequests.remove(token.get());
            }
        }

        @Override
        public void getQtiRadioCapabilityResponse(int slotId, Token token, Status status, int raf) {
            Message msg = null;

            Rlog.d(TAG, "getQtiRadioCapabilityResponse " + token.get());
            synchronized (mPendingRequests) {
                msg =  mPendingRequests.get(token.get());
            }
            if (status.get() == Status.SUCCESS) {
                if (msg != null) {
                    AsyncResult.forMessage(msg, raf, null);
                    msg.sendToTarget();
                }
            }
            synchronized (mPendingRequests) {
                mPendingRequests.remove(token.get());
            }
        }

        @Override
        public void setCarrierInfoForImsiEncryptionResponse(int slotId, Token token,
                QRadioResponseInfo info) {
            mQtiCarrierInfoResponse.setCarrierInfoForImsiEncryptionResponse(info);
        }

        @Override
        public void startNetworkScanResponse(int slotId, Token token, int errorCode) {
            Message msg = null;
            CommandException ex = CommandException.fromRilErrno(errorCode);

            Rlog.d(TAG, "startNetworkScanResponse " + token.get());
            synchronized (mPendingRequests) {
                msg =  mPendingRequests.get(token.get());
            }
            NetworkScanResult nsr = null;
            if (errorCode == 0) {
                nsr = new NetworkScanResult(NetworkScanResult.SCAN_STATUS_PARTIAL,
                        RadioError.NONE, null);
            } else {
                Rlog.d(TAG, "startNetworkScanResponse " + token.get() + "error " +
                        errorCode);
            }
            if (msg != null) {
                AsyncResult.forMessage(msg, nsr, ex);
                msg.sendToTarget();
            }
            synchronized (mPendingRequests) {
                mPendingRequests.remove(token.get());
            }
        }

        @Override
        public void stopNetworkScanResponse(int slotId, Token token, int errorCode) {
            Message msg = null;
            CommandException ex = CommandException.fromRilErrno(errorCode);
            NetworkScanResult nsr = null;

            Rlog.d(TAG, "stopNetworkScanResponse " + token.get());
            synchronized (mPendingRequests) {
                msg =  mPendingRequests.get(token.get());
            }
            if (errorCode == 0) {
                nsr = new NetworkScanResult(NetworkScanResult.SCAN_STATUS_PARTIAL,
                        RadioError.NONE, null);
            } else {
                Rlog.d(TAG, "stopNetworkScanResponse " + token.get() + "error " +
                        errorCode);
            }
            if (msg != null) {
                AsyncResult.forMessage(msg, nsr, ex);
                msg.sendToTarget();
            }
            synchronized (mPendingRequests) {
                mPendingRequests.remove(token.get());
            }
        }

        @Override
        public void setNetworkSelectionModeManualResponse(int slotId, Token token, int errorCode) {
            Message msg = null;
            CommandException ex = CommandException.fromRilErrno(errorCode);
            Object ret = null;

            Rlog.d(TAG, "setNetworkSelectionModeManualResponse " + token.get());
            synchronized (mPendingRequests) {
                msg =  mPendingRequests.get(token.get());
            }
            if (errorCode != 0) {
                Rlog.d(TAG, "setNetworkSelectionModeManualResponse " + token.get() + "error " +
                        errorCode);
            }
            if (msg != null) {
                AsyncResult.forMessage(msg, ret, ex);
                msg.sendToTarget();
            }
            synchronized (mPendingRequests) {
                mPendingRequests.remove(token.get());
            }
        }

        @Override
        public void setNetworkSelectionModeAutomaticResponse(int slotId, Token token,
                int errorCode) {
            Message msg = null;
            CommandException ex = CommandException.fromRilErrno(errorCode);
            Object ret = null;

            Rlog.d(TAG, "setNetworkSelectionModeAutomaticResponse " + token.get());
            synchronized (mPendingRequests) {
                msg =  mPendingRequests.get(token.get());
            }
            if (errorCode != 0) {
                Rlog.d(TAG, "setNetworkSelectionModeAutomaticResponse " + token.get() + "error " +
                        errorCode);
            }
            if (msg != null) {
                AsyncResult.forMessage(msg, ret, ex);
                msg.sendToTarget();
            }
            synchronized (mPendingRequests) {
                mPendingRequests.remove(token.get());
            }
        }

        @Override
        public void networkScanResult(int slotId, Token token, int status, int error,
                List<CellInfo> cellInfos) {

            Rlog.d(TAG, "networkScanResult " + token.get());

            NetworkScanResult nsr = new NetworkScanResult(status, error,
                        cellInfos);
            QtiTelephonyComponentFactory.getInstance().getRil(slotId)
                    .notifyNetworkScanResult(nsr);
        }

        @Override
        public void getQosParametersResponse(int slotId, Token token, Status status,
                                             QosParametersResult qosParams) {
            Message msg = null;
            boolean foundException = false;

            Rlog.d(TAG, "getQosParametersResponse token: " + token.get()
                    + ", slot: " + slotId
                    + ", status: " + status
                    + ", qosParams: " + ((qosParams == null) ? "null" : qosParams));

            FrameworkQosParameters frameworkQosParameters = null;
            try {
                frameworkQosParameters = new FrameworkQosParameters(qosParams);
                Rlog.d(TAG, "getQosParametersResponse, qosParams: " + frameworkQosParameters);
            } catch (NullPointerException e) {
                Rlog.e(TAG, "getQosParametersResponse, exception converting", e);
                foundException = true;
            }

            synchronized (mPendingRequests) {
                msg =  mPendingRequests.get(token.get());
            }

            if (msg != null) {
                if (status.get() == Status.SUCCESS && !foundException) {
                    AsyncResult.forMessage(msg, frameworkQosParameters, null);
                } else {
                    AsyncResult.forMessage(msg, null,
                            new RuntimeException("error while fetching QoS params"));
                }
                msg.sendToTarget();
            }
            synchronized (mPendingRequests) {
                mPendingRequests.remove(token.get());
            }
        }

        @Override
        public void onQosParametersChanged(int slotId, int cid, QosParametersResult qosParams) {
            Rlog.d(TAG, "onQosParametersChanged slot: " + slotId
                    + ", cid: " + cid
                    + ", qosParams: " + ((qosParams == null) ? "null" : qosParams));

            if (!mCellularDataServiceCallbacks.containsKey(slotId)) {
                Rlog.e(TAG, "onQosParametersChanged, no callback for slotId: " + slotId);
                return;
            }

            if (mCellularDataServiceCallbacks.get(slotId) == null) {
                Rlog.e(TAG, "onQosParametersChanged, invalid callback for slotId: " + slotId);
                return;
            }

            FrameworkQosParameters frameworkQosParameters = null;
            try {
                frameworkQosParameters = new FrameworkQosParameters(qosParams);
                Rlog.d(TAG, "onQosParametersChanged, frameworkQosParameters: "
                        + frameworkQosParameters);
            } catch (NullPointerException e) {
                Rlog.e(TAG, "onQosParametersChanged, caught exception in conversion.", e);
                return;
            }

            mCellularDataServiceCallbacks
                    .get(slotId)
                    .onQosParametersChanged(cid, frameworkQosParameters);
        }

    };

    boolean sendCdmaSms(int phoneId, byte[] pdu, Message result, boolean expectMore) {
        if (!mExtTelephonyManager.isServiceConnected()) {
            return false;
        }
        synchronized (mPendingRequests) {
            int serial = 0;
            Rlog.d(TAG, "sendCdmaSms, expectMore=" + expectMore);

            try {
                Token token = mExtTelephonyManager.sendCdmaSms(
                        phoneId,pdu, expectMore, mClient);
                serial = token.get();
            } catch (RuntimeException e) {
                Rlog.e(TAG, "Exception sendCdmaSms " + e);
            }
            mPendingRequests.put(serial, result);
        }
        return true;
    }

    boolean startNetworkScan(int phoneId, NetworkScanRequest networkScanRequest, Message result) {
        if (!mExtTelephonyManager.isServiceConnected()) {
            return false;
        }
        synchronized (mPendingRequests) {
            int serial = 0;

            try {
                Token token = mExtTelephonyManager.startNetworkScan(phoneId, networkScanRequest,
                         mClient);
                serial = token.get();
            } catch (RuntimeException e) {
                Rlog.e(TAG, "Exception startNetworkScan " + e);
            }
            mPendingRequests.put(serial, result);
        }
        return true;
    }

    boolean stopNetworkScan(int phoneId, Message result) {
        if (!mExtTelephonyManager.isServiceConnected()) {
            return false;
        }
        synchronized (mPendingRequests) {
            int serial = 0;

            try {
                Token token = mExtTelephonyManager.stopNetworkScan(phoneId, mClient);
                serial = token.get();
            } catch (RuntimeException e) {
                Rlog.e(TAG, "Exception stopNetworkScan " + e);
            }
            mPendingRequests.put(serial, result);
        }
        return true;
    }

    boolean setNetworkSelectionModeManual(int phoneId, OperatorInfo network, Message result) {
        if (!mExtTelephonyManager.isServiceConnected()) {
            return false;
        }
        long cagId = QtiSetNetworkSelectionMode.CAG_ID_INVALID;
        byte[] nId = null;
        if (network.getCagInfo() != null) {
            cagId = network.getCagInfo().getCagId();
        }
        if (network.getSnpnInfo() != null) {
            nId = network.getSnpnInfo().getNid();
        }
        QtiSetNetworkSelectionMode mode = new QtiSetNetworkSelectionMode(
                network.getOperatorNumeric(), network.getRan(),
                network.getAccessMode(), cagId, nId);

        synchronized (mPendingRequests) {
            int serial = 0;
            try {
                Token token = mExtTelephonyManager.setNetworkSelectionModeManual(phoneId, mode,
                        mClient);
                serial = token.get();
            } catch (RuntimeException e) {
                Rlog.e(TAG, "Exception setNetworkSelectionModeManual " + e);
            }
            mPendingRequests.put(serial, result);
        }
        return true;
    }

    boolean setNetworkSelectionModeAutomatic(int phoneId, int accessType, Message result) {
        if (!mExtTelephonyManager.isServiceConnected()) {
            return false;
        }
        synchronized (mPendingRequests) {
            int serial = 0;

            try {
                Token token = mExtTelephonyManager.setNetworkSelectionModeAutomatic(phoneId,
                        accessType, mClient);
                serial = token.get();
            } catch (RuntimeException e) {
                Rlog.e(TAG, "Exception setNetworkSelectionModeAutomatic " + e);
            }
            mPendingRequests.put(serial, result);
        }
        return true;
    }

    boolean getQtiRadioCapability(int phoneId, Message response) throws RemoteException {
        if (!mExtTelephonyManager.isServiceConnected()) {
            return false;
        }
        Rlog.d(TAG, "getQtiRadioCapability, response=" + response);
        synchronized (mPendingRequests) {
            Token token = mExtTelephonyManager.getQtiRadioCapability(phoneId, mClient);
            mPendingRequests.put(token.get(), response);
        }
        return true;
    }

    void setCarrierInfoForImsiEncryption(int phoneId, ImsiEncryptionInfo imsiEncryptionInfo,
                QtiCarrierInfoResponse response) {
        if (!mExtTelephonyManager.isServiceConnected()) {
            return;
        }
        mQtiCarrierInfoResponse = response;
        int tokenSerial = 0;
        Rlog.d(TAG, "setCarrierInfoForImsiEncryption, phoneId=" + phoneId);
        try {
            Token token = mExtTelephonyManager.setCarrierInfoForImsiEncryption(
                    phoneId, imsiEncryptionInfo, mClient);
            tokenSerial = token.get();
        } catch (RuntimeException e) {
            Rlog.e(TAG, "Exception setCarrierInfoForImsiEncryption " + e);
        }
    }

    public int getPhoneCount() {
        return ((TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE)).getPhoneCount();
    }

    public static int getPhoneId(Context context, int subId) {
        final SubscriptionManager subManager = context.getSystemService(SubscriptionManager.class);
        if (subManager == null) {
            return subManager.INVALID_SIM_SLOT_INDEX;
        }
        final SubscriptionInfo info = subManager.getActiveSubscriptionInfo(subId);
        if (info == null) {
            return subManager.INVALID_SIM_SLOT_INDEX;
        }
        return info.getSimSlotIndex();
    }

    public boolean isValidPhoneId(int phoneId) {
        return phoneId >= 0 && phoneId < getPhoneCount();
    }

    public static boolean putIntAtIndex(android.content.ContentResolver cr,
            String name, int index, int value) {
        String data = "";
        String valArray[] = null;
        String v = android.provider.Settings.Global.getString(cr, name);

        if (index == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("putIntAtIndex index == MAX_VALUE index=" + index);
        }
        if (index < 0) {
            throw new IllegalArgumentException("putIntAtIndex index < 0 index=" + index);
        }
        if (v != null) {
            valArray = v.split(",");
        }

        // Copy the elements from valArray till index
        for (int i = 0; i < index; i++) {
            String str = "";
            if ((valArray != null) && (i < valArray.length)) {
                str = valArray[i];
            }
            data = data + str + ",";
        }

        data = data + value;

        // Copy the remaining elements from valArray if any.
        if (valArray != null) {
            for (int i = index+1; i < valArray.length; i++) {
                data = data + "," + valArray[i];
            }
        }
        return android.provider.Settings.Global.putString(cr, name, data);
    }


    public static int getIntAtIndex(android.content.ContentResolver cr,
            String name, int index)
            throws android.provider.Settings.SettingNotFoundException {
        String v = android.provider.Settings.Global.getString(cr, name);
        if (v != null) {
            String valArray[] = v.split(",");
            if ((index >= 0) && (index < valArray.length) && (valArray[index] != null)) {
                try {
                    return Integer.parseInt(valArray[index]);
                } catch (NumberFormatException e) {
                    //Log.e(TAG, "Exception while parsing Integer: ", e);
                }
            }
        }
        throw new android.provider.Settings.SettingNotFoundException(name);
    }

    public int getCurrentUiccCardProvisioningStatus(int slotId) {
        int currentStatus = QtiUiccCardProvisioner.UiccProvisionStatus.INVALID_STATE;
        if (!isValidPhoneId(slotId)) {
            return currentStatus;
        }

        Phone phone = PhoneFactory.getPhone(slotId);
        if (phone == null)  {
            return currentStatus;
        }

        if (phone.getHalVersion().greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            SubscriptionManager subscriptionManager = (SubscriptionManager) mContext
                    .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            int subId = subscriptionManager.getSubscriptionId(slotId);
            if (subscriptionManager.isValidSubscriptionId(subId)) {
                SubscriptionInfo si = subscriptionManager.getActiveSubscriptionInfo(subId);
                currentStatus = si.areUiccApplicationsEnabled() ?
                        QtiUiccCardProvisioner.UiccProvisionStatus.PROVISIONED :
                        QtiUiccCardProvisioner.UiccProvisionStatus.NOT_PROVISIONED;
            } else {
                currentStatus = QtiUiccCardProvisioner.UiccProvisionStatus.CARD_NOT_PRESENT;
            }
        } else {
            currentStatus =  QtiUiccCardProvisioner.getInstance().
                    getCurrentUiccCardProvisioningStatus(slotId);
        }
        Rlog.d(TAG, " getCurrentUiccCardProvisioningStatus, state[" +
                slotId + "] = " + currentStatus);
        return currentStatus;
    }

    public void getQosParameters(int phoneId, int cid, Message response) throws RemoteException {
        if (!mExtTelephonyManager.isServiceConnected()) {
            return;
        }

        Rlog.d(TAG, "getQosParameters, phoneId: " + phoneId
                + ", cid: " + cid
                + ", response: " + response);

        synchronized (mPendingRequests) {
            Token token = null;
            try {
                token = mExtTelephonyManager.getQosParameters(phoneId, cid, mClient);
            } catch (Exception ex) {
                Rlog.d(TAG, "getQosParameters, caught exception", ex);
            }

            if (token == null) {
                throw new RuntimeException("API is not available");
            } else {
                mPendingRequests.put(token.get(), response);
            }
        }
    }

    public boolean isSmartDdsSwitchFeatureAvailable() {
        return mIsSmartDdsSwitchFeatureAvailable;
    }
}
