/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qti.phone;

import android.content.Context;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.os.Binder;
import android.os.Handler;
import android.os.HwBinder;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.qti.extphone.BearerAllocationStatus;
import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.DcParam;
import com.qti.extphone.NrConfig;
import com.qti.extphone.NrConfigType;
import com.qti.extphone.NrIcon;
import com.qti.extphone.NrIconType;
import com.qti.extphone.QRadioResponseInfo;
import com.qti.extphone.Qos;
import com.qti.extphone.QosParametersResult;
import com.qti.extphone.QtiPersoUnlockStatus;
import com.qti.extphone.QtiSetNetworkSelectionMode;
import com.qti.extphone.SignalStrength;
import com.qti.extphone.SmsResult;
import com.qti.extphone.Status;
import com.qti.extphone.Token;
import com.qti.extphone.UpperLayerIndInfo;

import com.qti.phone.powerupoptimization.PowerUpOptimization;

import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandwidthInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTime;
import vendor.qti.hardware.radio.qtiradio.V1_0.IQtiRadio;
import vendor.qti.hardware.radio.qtiradio.V1_0.QtiRadioResponseInfo;
import vendor.qti.hardware.radio.qtiradio.V1_0.QtiRadioError;
import vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadioResponse;
import vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadioIndication;

public class QtiRadioHidl implements IQtiRadioConnectionInterface {

    private static final String LOG_TAG = "QtiRadioHidl";

    static final String[] QTI_HIDL_SERVICE_NAME = {"slot1", "slot2", "slot3"};
    QtiRadioResponse mQtiRadioResponse;
    QtiRadioIndication mQtiRadioIndication;
    private IQtiRadioConnectionCallback mCallback;

    private Context mContext;
    private IQtiRadio mQtiRadio;
    private int mQtiPhoneId = 0;
    private QtiRadioProxyDeathRecipient mDeathRecipient;
    private AtomicLong mQtiRadioProxyCookie = new AtomicLong(0);
    private  QtiRILHandler mQtiRILHandler;
    private static final int EVENT_QTIRADIO_SERVICE_DEAD = 1;
    private static final int DEFAULT_REINIT_TIMEOUT_MS = 3000;

    private final int MAX_SLOTS = 2;

    private final Token UNSOL = new Token(-1);
    private ConcurrentHashMap<Integer, Token> mInflightRequests = new ConcurrentHashMap<Integer,
            Token>();

    /* Overloaded adapters to translate HIDL data types to AIDL data types. */
    private NrIconType convertHidlNrIconType2Aidl(int iconType) {
        return new NrIconType(iconType);
    }

    private NrConfig convertHidlNrConfig2Aidl(int nrConfig) {
        return new NrConfig(nrConfig);
    }

    private Status convertHidl2Aidl(int rilErrorCode) {
        return new Status((rilErrorCode == 0)? Status.SUCCESS : Status.FAILURE);
    }

    private boolean isEnableOrDisableSucess(int errorCode) {
        return errorCode == QtiRadioError.NONE ? true : false;
    }

    private BearerAllocationStatus convertHidlBearerStatus2Aidl(int bearerStatus) {
        return new BearerAllocationStatus(bearerStatus);
    }

    private DcParam convertHidl2Aidl(vendor.qti.hardware.radio.qtiradio.V2_0.DcParam dcParam) {
        return new DcParam(dcParam.endc, dcParam.dcnr);
    }

    private UpperLayerIndInfo convertHidl2Aidl(
            vendor.qti.hardware.radio.qtiradio.V2_1.UpperLayerIndInfo ulInfo) {
        return new UpperLayerIndInfo(ulInfo.plmnInfoList, ulInfo.upplerLayerInd);
    }

    private SignalStrength convertHidl2Aidl(
            vendor.qti.hardware.radio.qtiradio.V2_0.SignalStrength signalStrength) {
        return new SignalStrength(signalStrength.rsrp, signalStrength.snr);
    }

    private NrConfigType convertHidlConfigType2Aidl(int configType) {
        return new NrConfigType(configType);
    }

    private SmsResult convertHidlCdmaSmsResult2Aidl(int messageRef, String ackPDU, int errorCode) {
        return new SmsResult(messageRef, ackPDU, errorCode);
    }

    private QRadioResponseInfo convertHidlRadioResponseInfo2Aidl (
            QtiRadioResponseInfo qtiResponseInfo) {
        return new QRadioResponseInfo(qtiResponseInfo.type,
                qtiResponseInfo.serial, qtiResponseInfo.error);
    }

    public class QtiRadioResponse extends
            vendor.qti.hardware.radio.qtiradio.V2_7.IQtiRadioResponse.Stub {
        static final String TAG = "QtiRadioHidlResponse";

        int mSlotId;

        public QtiRadioResponse(int slotId) {
            Log.d(TAG, "[" + slotId + "] Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public void setCarrierInfoForImsiEncryptionResponse(QtiRadioResponseInfo info) {
            mCallback.setCarrierInfoForImsiEncryptionResponse(mSlotId, new Token(-1), new Status(0),
                    convertHidlRadioResponseInfo2Aidl(info));
        }

        @Override
        public void onEnable5gResponse(int serial, int errorCode, int status) {
            Log.d(TAG,"onEnable5gResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "status = " + status);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                boolean enabled = isEnableOrDisableSucess(errorCode);
                Log.d(TAG, "onEnable5gResponse: enabled = " + enabled);

                mCallback.on5gStatus(mSlotId, token, convertHidl2Aidl(errorCode), enabled);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onEnable5gResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onDisable5gResponse(int serial, int errorCode, int status) {
            Log.d(TAG, "onDisable5gResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "status = " + status);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                boolean enabled = isEnableOrDisableSucess(errorCode);
                Log.d(TAG, "onDisable5gResponse: enabled = " + !enabled);
                mCallback.on5gStatus(mSlotId, token, convertHidl2Aidl(errorCode), !enabled);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onDisable5gResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onEnable5gOnlyResponse(int serial, int errorCode, int status) {
            Log.d(TAG, "onEnable5gOnlyResponse: serial = " + serial + " errorCode = " + errorCode +
                    "" + " " + "status = " + status);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                boolean enabled = isEnableOrDisableSucess(errorCode);
                Log.d(TAG, "onEnable5gOnlyResponse: enabled = " + enabled);
                mCallback.on5gStatus(mSlotId, token, convertHidl2Aidl(errorCode), enabled);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onEnable5gOnlyResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void on5gStatusResponse(int serial, int errorCode, int enabled) {
            Log.d(TAG, "on5gStatusResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "enabled = " + enabled);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                boolean isEnabled = (enabled == vendor.qti.hardware.radio.qtiradio
                        .V2_0.EnableStatus.ENABLED);
                Log.d(TAG, "on5gStatusResponse: enabled = " + isEnabled);
                mCallback.on5gStatus(mSlotId, token, convertHidl2Aidl(errorCode), isEnabled);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "on5gStatusResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onNrDcParamResponse(int serial, int errorCode,
                                        vendor.qti.hardware.radio.qtiradio.V2_0.DcParam dcParam) {
            Log.d(TAG, "onNrDcParamResponse: serial = " + serial + " errorCode = "
                    + errorCode + " dcParam = " + dcParam);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                DcParam aidlDcParam = convertHidl2Aidl(dcParam);
                Log.d(TAG, "onNrDcParamResponse: aidlDcParam = " + aidlDcParam);
                mCallback.onNrDcParam(mSlotId, token, convertHidl2Aidl(errorCode), aidlDcParam);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onNrDcParamResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onNrBearerAllocationResponse_2_1(int serial, int errorCode, int bearerStatus) {
            Log.d(TAG, "onNrBearerAllocationResponse: serial = " + serial + " errorCode = " +
                    errorCode + " bearerStatus = " + bearerStatus);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                BearerAllocationStatus bStatus = convertHidlBearerStatus2Aidl(bearerStatus);

                Log.d(TAG, "onNrBearerAllocationResponse:  allocated = " + bStatus);
                mCallback.onAnyNrBearerAllocation(mSlotId, token, convertHidl2Aidl(errorCode),
                        bStatus);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onNrBearerAllocationResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onNrBearerAllocationResponse(int serial, int errorCode, int bearerStatus) {
            Log.d(TAG, "onNrBearerAllocationResponse: serial = " + serial + " errorCode = " +
                    errorCode + " bearerStatus = " + bearerStatus);
            onNrBearerAllocationResponse_2_1(serial, errorCode, bearerStatus);
        }


        @Override
        public void onUpperLayerIndInfoResponse(int serial, int errorCode,
                vendor.qti.hardware.radio.qtiradio.V2_1.UpperLayerIndInfo uliInfo) {
            Log.d(TAG, "onUpperLayerIndInfoResponse: serial = " + serial + " errorCode = " +
                    errorCode + " UpperLayerIndInfo = " + uliInfo);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                UpperLayerIndInfo upperLayerInfo = convertHidl2Aidl(uliInfo);
                Log.d(TAG, "onUpperLayerIndInfoResponse: upperLayerInfo = " + upperLayerInfo);
                mCallback.onUpperLayerIndInfo(mSlotId, token, convertHidl2Aidl(errorCode),
                        upperLayerInfo);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onUpperLayerIndInfoResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void on5gConfigInfoResponse(int serial, int errorCode, int configType) {
            Log.d(TAG, "on5gConfigInfoResponse: serial = " + serial + " errorCode = " +
                    errorCode + " ConfigType = " + configType);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                NrConfigType nrConfigType = convertHidlConfigType2Aidl(configType);
                Log.d(TAG, "on5gConfigInfoResponse: nrConfigType = " + nrConfigType);
                mCallback.on5gConfigInfo(mSlotId, token, convertHidl2Aidl(errorCode), nrConfigType);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "on5gConfigInfoResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onNrIconTypeResponse(int serial, int errorCode, int iconType) {
            Log.d(TAG, "onNrIconTypeResponse: serial = " + serial + " errorCode = " +
                    errorCode + " iconType = " + iconType);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                NrIconType nrIconType = convertHidlNrIconType2Aidl(iconType);
                mCallback.onNrIconType(mSlotId, token, convertHidl2Aidl(errorCode),
                        nrIconType);
                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onNrIconTypeResponse: No previous request found for serial = " +
                        serial);
            }

        }

        @Override
        public void onSignalStrengthResponse(int serial, int errorCode,
                vendor.qti.hardware.radio.qtiradio.V2_0.SignalStrength signalStrength) {
            Log.d(TAG, "onSignalStrengthResponse: serial = " + serial + " errorCode = " +
                    errorCode + " signalStrength = " + signalStrength);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                SignalStrength aidlSignalStrength = convertHidl2Aidl(signalStrength);
                Log.d(TAG, "onSignalStrengthResponse: aidlSignalStrength = " + aidlSignalStrength);
                mCallback.onSignalStrength(mSlotId, token, convertHidl2Aidl(errorCode),
                        aidlSignalStrength);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onSignalStrengthResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onEnableEndcResponse(int serial, int errorCode, int status) {
            Log.d(TAG, "onEnableEndcResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "status = " + status);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onEnableEndc(mSlotId, token, convertHidl2Aidl(errorCode));

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onEnableEndcResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onEndcStatusResponse(int serial, int errorCode, int enabled) {
            Log.d(TAG, "onEndcStatusResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "enabled = " + enabled);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                boolean isEnabled = (enabled == vendor.qti.hardware.radio.qtiradio
                        .V2_3.EndcStatus.ENABLED);
                mCallback.onEndcStatus(mSlotId, token, convertHidl2Aidl(errorCode), isEnabled);

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onEndcStatusResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void getAtrResponse(QtiRadioResponseInfo qtiResponseInfo, String atr) {
            Log.d(TAG, "getAtrResponse: NOP!!");

        }

        @Override
        public void sendCdmaSmsResponse(QtiRadioResponseInfo qtiResponseInfo, SendSmsResult sms) {
            int serial = qtiResponseInfo.serial;
            Log.d(TAG,"sendCdmaSmsResponse: serial = " + serial + " errorCode = " +
                    qtiResponseInfo.error + " " + "SmsResult = " + sms);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                mCallback.sendCdmaSmsResponse(mSlotId, token,
                        convertHidl2Aidl(qtiResponseInfo.error),
                        convertHidlCdmaSmsResult2Aidl(sms.messageRef, sms.ackPDU, sms.errorCode));

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "sendCdmaSmsResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void setNrConfigResponse(int serial, int errorCode, int status) {
            Log.d(TAG,"setNrConfigResponse: serial = " + serial + " errorCode = " + errorCode +
                    " status = " + status);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                mCallback.onSetNrConfig(mSlotId, token, convertHidl2Aidl(errorCode));

                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "setNrConfigResponse: No previous request found for serial = " +
                        serial);
            }
        }

        @Override
        public void onNrConfigResponse(int serial, int errorCode, int nrConfig) {
            Log.d(TAG, "onNrConfigResponse: serial = " + serial + " errorCode = " + errorCode +
                    " nrConfig = " + nrConfig);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                NrConfig config = convertHidlNrConfig2Aidl(nrConfig);
                mCallback.onNrConfigStatus(mSlotId, token, convertHidl2Aidl(errorCode), config);
                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "onNrConfigResponse: No previous request found for serial = " +
                        serial);
            }
        }

        /**
         * Response to IQtiRadio.getQtiRadioCapability
         * @param responseInfo Response info struct containing response type, serial no. and error
         * @param raf Radio Access Family 32-bit bitmap of
         * vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily
         */
        @Override
        public void getQtiRadioCapabilityResponse(RadioResponseInfo responseInfo, int raf) {
            int serial = responseInfo.serial;
            Log.d(TAG,"getQtiRadioCapabilityResponse: serial = " + serial + " errorCode = " +
                    responseInfo.error + " " + "raf = " + raf);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.getQtiRadioCapabilityResponse(mSlotId, token,
                        convertHidl2Aidl(responseInfo.error),
                        QtiRadioUtils.convertToQtiNetworkTypeBitMask(raf));
                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "getQtiRadioCapabilityResponse: No previous request found for serial = "
                        + serial);
            }
        }

        /**
         * Response to IQtiRadio.getQosParameters
         *
         * @param responseInfo Response info struct containing response type, serial no. and error
         * @param qosParametersResult QosParametersResult defined in types.hal
         */
        @Override
        public void getQosParametersResponse(RadioResponseInfo responseInfo,
                vendor.qti.hardware.radio.qtiradio.V2_7.QosParametersResult qosParametersResult) {
            int serial = responseInfo.serial;
            Log.d(TAG, "getQosParametersResponse: "
                    + "serial = " + serial + " errorCode = " + responseInfo.error);

            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.getQosParametersResponse(mSlotId, token,
                        convertHidl2Aidl(responseInfo.error),
                        QtiRadioUtils.createQosParametersResultFromQtiRadioHalStruct(
                                qosParametersResult));
                mInflightRequests.remove(serial);
            } else {
                Log.d(TAG, "getQosParametersResponse: No previous request found for serial = "
                        + serial);
            }
        }
    }

    public class QtiRadioIndication extends
            vendor.qti.hardware.radio.qtiradio.V2_7.IQtiRadioIndication.Stub {
        static final String TAG = "QtiRadiohidlIndication";

        int mSlotId;

        public QtiRadioIndication(int slotId) {
            Log.d(TAG, "[" + slotId + "]Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public void on5gStatusChange(int enableStatus) {
            Log.d(TAG, "on5gStatusChange: slotId = " + mSlotId + "enabled = " + enableStatus);
            if (mCallback != null) {
                boolean enabled = (enableStatus == vendor.qti.hardware.radio.qtiradio
                        .V2_0.EnableStatus.ENABLED);
                mCallback.on5gStatus(mSlotId, UNSOL, new Status(Status.SUCCESS), enabled);
            }
        }

        @Override
        public void onNrDcParamChange(
                vendor.qti.hardware.radio.qtiradio.V2_0.DcParam dcParam) {
            Log.d(TAG, "onNrDcParamChange: slotId = " + mSlotId);
            if (mCallback != null) {
                DcParam aidlDcParam = convertHidl2Aidl(dcParam);
                mCallback.onNrDcParam(mSlotId, UNSOL, new Status(Status.SUCCESS), aidlDcParam);
            }
        }

        @Override
        public void onNrBearerAllocationChange_2_1(int bearerStatus) {
            Log.d(TAG, "onNrBearerAllocationChange: slotId = " + mSlotId +
                    "bearerStatus = " + bearerStatus);
            BearerAllocationStatus bStatus = convertHidlBearerStatus2Aidl(bearerStatus);
            mCallback.onAnyNrBearerAllocation(mSlotId, UNSOL, new Status(Status.SUCCESS), bStatus);
        }

        @Override
        public void onNrBearerAllocationChange(int bearerStatus) {
            onNrBearerAllocationChange_2_1(bearerStatus);
        }

        @Override
        public void onSignalStrengthChange(
                vendor.qti.hardware.radio.qtiradio.V2_0.SignalStrength signalStrength) {
            Log.d(TAG, "onSignalStrengthChange: slotId = " + mSlotId);
            if (mCallback != null) {
                SignalStrength aidlSignalStrength = convertHidl2Aidl(signalStrength);
                mCallback.onSignalStrength(mSlotId, UNSOL, new Status(Status.SUCCESS),
                        aidlSignalStrength);
            }
        }

        @Override
        public void onUpperLayerIndInfoChange(
                vendor.qti.hardware.radio.qtiradio.V2_1.UpperLayerIndInfo uliInfo) {
            Log.d(TAG, "onUpperLayerIndInfoChange: slotId = " + mSlotId);
            if (mCallback != null) {
                UpperLayerIndInfo upperLayerInfo = convertHidl2Aidl(uliInfo);
                mCallback.onUpperLayerIndInfo(mSlotId, UNSOL, new Status(Status.SUCCESS),
                        upperLayerInfo);
            }
        }

        @Override
        public void on5gConfigInfoChange(int configType) {
            Log.d(TAG, "on5gConfigInfoChange: slotId = " + mSlotId);
            if (mCallback != null) {
                NrConfigType nrConfigType = convertHidlConfigType2Aidl(configType);
                mCallback.on5gConfigInfo(mSlotId, UNSOL, new Status(Status.SUCCESS),
                        nrConfigType);
            }
        }

        @Override
        public void onNrIconTypeChange(int iconType) {
            Log.d(TAG, "onNrIconTypeChange: slotId = " + mSlotId + "NrIconType = " + iconType);
            NrIconType nrIconType = convertHidlNrIconType2Aidl(iconType);
            mCallback.onNrIconType(mSlotId, UNSOL, new Status(Status.SUCCESS),
                    nrIconType);
        }

        @Override
        public void onNrConfigChange(int config) {
        }

        /*
         * Unsol message to indicate changes in QoS parameters for a given cid.
         *
         * @param cid Connection id of the data call for which QoS parameters are received.
         * @param qosParamsResult QosParametersResult defined in types.hal
         */
        @Override
        public void onQosParametersChanged(int cid,
                vendor.qti.hardware.radio.qtiradio.V2_7.QosParametersResult qosParamsResult) {
            Log.d(TAG, "onQosParametersChanged: slotId = " + mSlotId + " cid = " + cid);

            mCallback.onQosParametersChanged(mSlotId,
                    cid,
                    QtiRadioUtils.createQosParametersResultFromQtiRadioHalStruct(qosParamsResult));
        }

        @Override
        public void qtiRadioIndication(int value) {
            Log.d(TAG, "qtiRadioIndication: NOP!!");
        }

    }

    public QtiRadioHidl(int slotId, Context context) {
        mQtiPhoneId = slotId;
        mContext = context;
        mDeathRecipient = new QtiRadioProxyDeathRecipient();
        mQtiRILHandler = new QtiRILHandler();
        initQtiRadioHidl();
    }

    final class QtiRadioProxyDeathRecipient implements HwBinder.DeathRecipient {
        @Override
        public void serviceDied(long cookie) {
            Log.d(LOG_TAG, "serviceDied");
            resetServiceAndRequestList();
            mQtiRILHandler.sendMessageDelayed(mQtiRILHandler.obtainMessage(
                    EVENT_QTIRADIO_SERVICE_DEAD), DEFAULT_REINIT_TIMEOUT_MS);
        }
    }

    private synchronized void initQtiRadioHidl() {
        if (!SubscriptionManager.isValidSlotIndex(mQtiPhoneId)) {
            return;
        }
        try {
            mQtiRadio = IQtiRadio.getService(QTI_HIDL_SERVICE_NAME[mQtiPhoneId]);
            if (mQtiRadio == null) {
                Log.d(LOG_TAG, "initQtiRadioHidl: mQtiRadio is null. Return");
                return;
            }
            Log.d(LOG_TAG, "initQtiRadioHidl: mQtiRadio" + mQtiRadio +
                    " mQtiPhoneId=" + mQtiPhoneId);
            mQtiRadio.linkToDeath(mDeathRecipient,
                    mQtiRadioProxyCookie.incrementAndGet());
            mQtiRadioResponse = new QtiRadioResponse(mQtiPhoneId);
            mQtiRadioIndication = new QtiRadioIndication(mQtiPhoneId);
            mQtiRadio.setCallback(mQtiRadioResponse, mQtiRadioIndication);
        } catch (Exception ex) {
            Log.d(LOG_TAG, "initQtiRadioHidl: Exception: " + ex);
        }
    }

    public class QtiRILHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_QTIRADIO_SERVICE_DEAD:
                    Log.d(LOG_TAG, "EVENT_QTIRADIO_SERVICE_DEAD reinitialize ...");
                    initQtiRadioHidl();
                    PowerUpOptimization powerUpOptimization = PowerUpOptimization.getInstance();
                    if (powerUpOptimization != null) {
                        powerUpOptimization.handleRadioDied(mQtiPhoneId);
                    }
                    break;
            }
        }
    }

    private void resetServiceAndRequestList() {
        mQtiRadio = null;
        mQtiRadioResponse = null;
        mQtiRadioIndication = null;
        mQtiRadioProxyCookie.incrementAndGet();
    }

    @java.lang.Override
    public int getPropertyValueInt(String property, int def) throws RemoteException {
        int propVal = def;
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 =
                vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_3 != null) {
            try {
                propVal = radioProxy2_3.getPropertyValueInt(property, def);
            } catch (RemoteException ex) {
                throw new RemoteException("API Error");
            }
        } else {
            Log.d(LOG_TAG, "getPropertyValueInt HAL API not available");
            propVal = SystemProperties.getInt(property, def);
        }
        return propVal;
    }

    @java.lang.Override
    public boolean getPropertyValueBool(String property, boolean def) throws RemoteException {
        boolean propVal = def;
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 =
                vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_3 != null) {
            try {
                propVal = radioProxy2_3.getPropertyValueBool(property, def);
            } catch (RemoteException ex) {
                throw new RemoteException("API Error");
            }
        } else {
            Log.d(LOG_TAG, "getPropertyValueBool HAL API not available");
            propVal = SystemProperties.getBoolean(property, def);
        }
        return propVal;
    }

    @java.lang.Override
    public String getPropertyValueString(String property, String def) throws RemoteException {
        String propVal = def;
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 =
                vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_3 != null) {
            try {
                propVal = radioProxy2_3.getPropertyValueString(property, def);
            } catch (RemoteException ex) {
                throw new RemoteException("API Error");
            }
        } else {
            Log.d(LOG_TAG, "getPropertyValueString HAL API not available");
            propVal = SystemProperties.get(property, def);
        }
        return propVal;
    }


    @java.lang.Override
    public void queryNrIconType(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_2.IQtiRadio radioProxy2_2 = vendor.qti.hardware
                .radio.qtiradio.V2_2.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_2 != null) {
            Log.d(LOG_TAG, "queryNrIconType: serial = " + serial);
            radioProxy2_2.queryNrIconType(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void enableEndc(boolean enable, Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 = vendor.qti.hardware
                .radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_3 != null) {
            Log.d(LOG_TAG, "enableEndc: serial = " + serial +"enable: "+enable);
            radioProxy2_3.enableEndc(serial, enable);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void queryEndcStatus(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 = vendor.qti.hardware
                .radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_3 != null) {
            Log.d(LOG_TAG, "queryEndcStatus: serial = " + serial);
            radioProxy2_3.queryEndcStatus(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void setNrConfig(NrConfig config, Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_5.IQtiRadio radioProxy2_5 = vendor.qti.hardware
                .radio.qtiradio.V2_5.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_5 != null) {
            Log.d(LOG_TAG,"setNrConfig: serial = " + serial + " config= " + config.get());
            radioProxy2_5.setNrConfig(serial, config.get());
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void queryNrConfig(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_5.IQtiRadio radioProxy2_5 = vendor.qti.hardware
                .radio.qtiradio.V2_5.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_5 != null) {
            Log.d(LOG_TAG,"queryNrConfig: serial = " + serial);
            radioProxy2_5.queryNrConfig(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @Override
    public void setCarrierInfoForImsiEncryption(Token token,
            ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_4.IQtiRadio radioProxy2_4 = vendor.qti.hardware
                .radio.qtiradio.V2_4.IQtiRadio.castFrom(mQtiRadio);
        vendor.qti.hardware.radio.qtiradio.V2_4.ImsiEncryptionInfo halImsiInfo =
                new vendor.qti.hardware.radio.qtiradio.V2_4.ImsiEncryptionInfo();
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_4 != null) {
            Log.d(LOG_TAG,"setCarrierInfoForImsiEncryption: serial = " + serial);
            halImsiInfo.base.mcc = imsiEncryptionInfo.getMcc();
            halImsiInfo.base.mnc = imsiEncryptionInfo.getMnc();
            halImsiInfo.base.keyIdentifier = imsiEncryptionInfo.getKeyIdentifier();
            if (imsiEncryptionInfo.getExpirationTime() != null) {
                halImsiInfo.base.expirationTime =
                        imsiEncryptionInfo.getExpirationTime().getTime();
            }
            for (byte b : imsiEncryptionInfo.getPublicKey().getEncoded()) {
                halImsiInfo.base.carrierKey.add(new Byte(b));
            }
            halImsiInfo.keyType = imsiEncryptionInfo.getKeyType();
            radioProxy2_4.setCarrierInfoForImsiEncryption(serial, halImsiInfo);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void sendCdmaSms(byte[] pdu,
            boolean expectMore, Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 =
                    vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_0 != null) {
            Log.d(LOG_TAG,"sendCdmaSms: serial = " + serial);

             CdmaSmsMessage msg = new CdmaSmsMessage();
             constructCdmaSendSmsRilRequest(msg, pdu);

            radioProxy2_0.sendCdmaSms(serial, msg, expectMore);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    private void constructCdmaSendSmsRilRequest(CdmaSmsMessage msg, byte[] pdu) {
        int addrNbrOfDigits;
        int subaddrNbrOfDigits;
        int bearerDataLength;
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu);
        DataInputStream dis = new DataInputStream(bais);

        try {
            msg.teleserviceId = dis.readInt(); // teleServiceId
            msg.isServicePresent = (byte) dis.readInt() == 1 ? true : false; // servicePresent
            msg.serviceCategory = dis.readInt(); // serviceCategory
            msg.address.digitMode = dis.read();  // address digit mode
            msg.address.numberMode = dis.read(); // address number mode
            msg.address.numberType = dis.read(); // address number type
            msg.address.numberPlan = dis.read(); // address number plan
            addrNbrOfDigits = (byte) dis.read();
            for (int i = 0; i < addrNbrOfDigits; i++) {
                msg.address.digits.add(dis.readByte()); // address_orig_bytes[i]
            }
            msg.subAddress.subaddressType = dis.read(); //subaddressType
            msg.subAddress.odd = (byte) dis.read() == 1 ? true : false; //subaddr odd
            subaddrNbrOfDigits = (byte) dis.read();
            for (int i = 0; i < subaddrNbrOfDigits; i++) {
                msg.subAddress.digits.add(dis.readByte()); //subaddr_orig_bytes[i]
            }

            bearerDataLength = dis.read();
            for (int i = 0; i < bearerDataLength; i++) {
                msg.bearerData.add(dis.readByte()); //bearerData[i]
            }
        } catch (IOException ex) {
             Log.d(LOG_TAG, "sendSmsCdma: conversion from input stream to object failed: " + ex);
        }
    }

    @java.lang.Override
    public void enable5g(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_0 != null) {
            Log.d(LOG_TAG,"enable5g: serial = " + serial);
            radioProxy2_0.enable5g(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void disable5g(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_0 != null) {
            Log.d(LOG_TAG,"disable5g: serial = " + serial);
            radioProxy2_0.disable5g(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void queryNrBearerAllocation(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_0 != null) {
            Log.d(LOG_TAG,"queryNrBearerAllocation: serial = " + serial);
            radioProxy2_0.queryNrBearerAllocation(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void enable5gOnly(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_0 != null) {
            Log.d(LOG_TAG,"enable5gOnly: serial = " + serial);
            radioProxy2_0.enable5gOnly(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void query5gStatus(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_0 != null) {
            Log.d(LOG_TAG,"query5gStatus: serial = " + serial);
            radioProxy2_0.query5gStatus(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void queryNrDcParam(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_0 != null) {
            Log.d(LOG_TAG,"queryNrDcParam: serial = " + serial);
            radioProxy2_0.queryNrDcParam(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void queryNrSignalStrength(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_0 != null) {
            Log.d(LOG_TAG,"queryNrSignalStrength: serial = " + serial);
            radioProxy2_0.queryNrSignalStrength(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void queryUpperLayerIndInfo(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_1.IQtiRadio radioProxy2_1 = vendor.qti.hardware
                .radio.qtiradio.V2_1.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_1 != null) {
            Log.d(LOG_TAG,"queryUpperLayerIndInfo: serial = " + serial);
            radioProxy2_1.queryUpperLayerIndInfo(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void query5gConfigInfo(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_1.IQtiRadio radioProxy2_1 = vendor.qti.hardware
                .radio.qtiradio.V2_1.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_1 != null) {
            Log.d(LOG_TAG,"query5gConfigInfo: serial = " + serial);
            radioProxy2_1.query5gConfigInfo(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void getQtiRadioCapability(Token token) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_6.IQtiRadio radioProxy2_6 =
                vendor.qti.hardware.radio.qtiradio.V2_6.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_6 != null) {
            Log.d(LOG_TAG,"getQtiRadioCapability: serial = " + serial);
            radioProxy2_6.getQtiRadioCapability(serial);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void getQosParameters(Token token, int cid) throws RemoteException {
        Log.d(LOG_TAG, "getQosParameters: cid: " + cid + ", token: " + token);

        vendor.qti.hardware.radio.qtiradio.V2_7.IQtiRadio radioProxy2_7 =
                vendor.qti.hardware.radio.qtiradio.V2_7.IQtiRadio.castFrom(mQtiRadio);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        if (radioProxy2_7 != null) {
            Log.d(LOG_TAG, "getQosParameters: serial = " + serial);
            radioProxy2_7.getQosParameters(serial, cid);
        } else {
            mInflightRequests.remove(serial, token);
            throw new RemoteException("API not available!");
        }
    }

    @java.lang.Override
    public void initQtiRadio() {
        // Feature not supported for HIDL
    }

    @java.lang.Override
    public void getFacilityLockForApp(Token token, String facility, String password,
            int serviceClass, String appId, boolean expectMore) throws RemoteException {
        // Feature not supported for HIDL
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

    @java.lang.Override
    public void queryCallForwardStatus(Token token, int cfReason, int serviceClass,
            String number, boolean expectMore) throws RemoteException {
        // Feature not supported for HIDL
    }

    @Override
    public void getImei(Token token) throws RemoteException {
        // Feature not supported for HIDL
    }

    @java.lang.Override
    public boolean isFeatureSupported(int feature) {
        // Feature not supported for HIDL
        return false;
    }

    public void getDdsSwitchCapability(Token token) {
        Log.e(LOG_TAG, "getDdsSwitchCapability not supported in HIDL");
    }

    public void sendUserPreferenceForDataDuringVoiceCall(Token token,
            boolean userPreference) {
        Log.e(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCall not supported in HIDL");
    }

    @Override
    public boolean isEpdgOverCellularDataSupported() {
        // Feature not supported for HIDL
        return false;
    }

    @Override
    public void setNrUltraWidebandIconConfig(Token token, int sib2Value,
            NrUwbIconBandInfo saBandInfo, NrUwbIconBandInfo nsaBandInfo,
            ArrayList<NrUwbIconRefreshTime> refreshTimes,
            NrUwbIconBandwidthInfo bandwidthInfo) throws RemoteException {
        Log.e(LOG_TAG, "setNrUltraWidebandIconConfig not supported in HIDL");
        throw new RemoteException("not supported by HIDL");
    }

    @Override
    public CiwlanConfig getCiwlanConfig() throws RemoteException {
        Log.e(LOG_TAG, "getCiwlanConfig not supported in HIDL");
        return null;
    }

    @Override
    public QtiPersoUnlockStatus getSimPersoUnlockStatus() throws RemoteException {
        Log.e(LOG_TAG, "getSimPersoUnlockStatus not supported for HIDL");
        return null;
    }

    @Override
    public boolean isCiwlanAvailable() {
        Log.e(LOG_TAG, "isCiwlanAvailable not supported for HIDL");
        return false;
    }

    @Override
    public void setCiwlanModeUserPreference(Token token, CiwlanConfig ciwlanConfig)
            throws RemoteException {
        Log.e(LOG_TAG, "setCiwlanModeUserPreference not supported for HIDL");
    }

    @Override
    public CiwlanConfig getCiwlanModeUserPreference() {
        Log.e(LOG_TAG, "getCiwlanModeUserPreference not supported for HIDL");
        return null;
    }

    @Override
    public CellularRoamingPreference getCellularRoamingPreference() throws RemoteException {
        Log.e(LOG_TAG, "getCellularRoamingPreference not supported in HIDL");
        return null;
    }

    @Override
    public void setCellularRoamingPreference(Token token, CellularRoamingPreference pref)
            throws RemoteException {
        Log.e(LOG_TAG, "setCellularRoamingPreference not supported in HIDL");
    }

    @Override
    public boolean isEmcSupported() {
        Log.e(LOG_TAG, "isEmcSupported not supported for HIDL");
        return false;
    }

    @Override
    public boolean isEmfSupported() {
        Log.e(LOG_TAG, "isEmfSupported not supported for HIDL");
        return false;
    }

    @Override
    public void queryNrIcon(Token token) throws RemoteException {
        Log.e(LOG_TAG, "queryNrIcon not supported in HIDL");
    }

    @Override
    public void sendAllEsimProfiles(Token token, boolean status, int refNum, List<String> iccIds)
            throws RemoteException {
        Log.e(LOG_TAG, "sendAllEsimProfiles not supported in HIDL");
    }

    @Override
    public void notifyEnableProfileStatus(Token token, int refNum, boolean result)
            throws RemoteException {
        Log.e(LOG_TAG, "notifyEnableProfileStatus not supported in HIDL");
    }

    @Override
    public void notifyDisableProfileStatus(Token token, int refNum, boolean result)
            throws RemoteException {
        Log.e(LOG_TAG, "notifyDisableProfileStatus not supported in HIDL");
    }

    @Override
    public void registerCallback(IQtiRadioConnectionCallback callback) {
        Log.d(LOG_TAG, "registerCallback: callback = " + callback);
        mCallback = callback;
    }

    @Override
    public void unRegisterCallback(IQtiRadioConnectionCallback callback) {
        Log.d(LOG_TAG, "unRegisterCallback: callback = " + callback);
        if (mCallback == callback) {
            mCallback = null;
        }
    }
}
