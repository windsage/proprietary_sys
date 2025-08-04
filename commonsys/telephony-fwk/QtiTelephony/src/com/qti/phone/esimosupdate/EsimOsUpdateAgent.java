/******************************************************************************
 #  @file EsimOsUpdateAgent.java
 #
 #  @brief Class for handling the eSIM/EUICC card OS update for G+D Card Vendor.
 #
 #  ---------------------------------------------------------------------------
 #
 # Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 # All rights reserved.
 # Confidential and Proprietary - Qualcomm Technologies, Inc.
 #  ---------------------------------------------------------------------------*/

package com.qti.phone.esimosupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;
import android.util.Log;

import com.qti.phone.esimosupdate.EsimOsUpdateUtils.EuiccOSInfo;
import com.qti.phone.QtiRadioProxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EsimOsUpdateAgent {
    private static final String LOG_TAG = "EsimOsUpdateAgent";

    private Context mContext;
    private Handler mHandler;

    /**
     * Utility class instance for Os Update procedure.
     * Refer EsimOsUpdateUtils.java
     */
    private EsimOsUpdateUtils mEsimOsUpdateUtils;
    /**
     * boolean mIsCardStatusVerified. True when CARD STATE intent
     * is received and CARD_STATE is PRESENT other-wise false.
     */
    private boolean mIsCardStatusVerified = false;
    /**
     * Slot index where G+D eSIM card is present.
     * This value indicates the slot index where G+D eSIM card is default soldered in device.
     */
    private int mEsimSlotId = 1;
    /**
     * By default port index is 0. This port index can be overriden based on
     * Slot Status indication.
     */
    private int mPortId = 0;

    private static final int EVENT_SIM_SLOT_CHANGED = 1;
    private static final int EVENT_CARD_STATE_CHANGED = 2;
    private static final int EVENT_PERFORM_WRITE_MANIFEST_CONTENT = 3;
    private static final int EVENT_PERFORM_WRITE_MANIFEST_CONTENT_DONE = 4;
    private static final int EVENT_PERFORM_WRITE_OS_CONTENT = 5;
    private static final int EVENT_PERFORM_WRITE_OS_CONTENT_DONE = 6;

    private static final int ITL_STATE_SECURED = 2;
    private static final int ITL_STATE_SECURED_OS_INSTALLED = 3;

    private static final int DELAY_INTERVAL = 5000;

    // APDU command executed successfully
    private static final String APDU_STATUS_NO_ERROR = "9000";
    // ITL accepted the Manifest that is sent
    private static final String APDU_STATUS_MANIFEST_ACCEPTED = "6301";

    public EsimOsUpdateAgent(Context context, QtiRadioProxy radioProxy) {
        mContext = context.createDeviceProtectedStorageContext();
        Log.i(LOG_TAG, "Constructor");
        mEsimOsUpdateUtils = EsimOsUpdateUtils.getInstance(mContext, radioProxy);
        HandlerThread headlerThread = new HandlerThread(LOG_TAG);
        headlerThread.start();
        mHandler = new EsimOsUpdateAgentHandler(headlerThread.getLooper());
        registerForIntents();
        checkIccCardAndProceed();
    }

    private void registerForIntents() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED);
        intentFilter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);

        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED.equals(action)) {
                logd("received slot status change indication");
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SIM_SLOT_CHANGED));
            } else if (TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                logd("SIM_CARD_STATE_CHANGED: slotId = " + slotId);
                if (slotId == mEsimSlotId) {
                    checkIccCardAndProceed();
                }
            }
        }
    };

    private final class EsimOsUpdateAgentHandler extends Handler {
        EsimOsUpdateAgentHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(LOG_TAG, "handleMessage received " + msg.what);
            switch (msg.what) {
                case EVENT_SIM_SLOT_CHANGED: {
                    handleSlotStatusEvent();
                    break;
                }
                case EVENT_CARD_STATE_CHANGED: {
                    handleCardStateChanged();
                    break;
                }
                case EVENT_PERFORM_WRITE_MANIFEST_CONTENT:
                case EVENT_PERFORM_WRITE_OS_CONTENT: {
                    writeOSFileContentInCard();
                    break;
                }
                case EVENT_PERFORM_WRITE_MANIFEST_CONTENT_DONE: {
                    mEsimOsUpdateUtils.readITLAndOSProperties(mEsimSlotId, mPortId);
                    if (verifyITLDetailsAfterManifestWrite()) {
                        writeOSFileContentInCard();
                    }
                    break;
                }
                case EVENT_PERFORM_WRITE_OS_CONTENT_DONE: {
                    mEsimOsUpdateUtils.readITLAndOSProperties(mEsimSlotId, mPortId);
                    if (verifyITLAndOSDetailsAfterOSWrite()) {
                        notifyOSUpdateCompleted();
                    }
                    break;
                }
                default: {
                    logd("invalid message ");
                    break;
                }
            }
        }
    }

    private void checkIccCardAndProceed() {
        TelephonyManager telMgr = (TelephonyManager) mContext.getSystemService(Context.
                TELEPHONY_SERVICE);
        if ((telMgr != null) && telMgr.hasIccCard(mEsimSlotId)) {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_CARD_STATE_CHANGED));
        }
    }

    private void notifyOSUpdateCompleted() {
        mEsimOsUpdateUtils.setManifestWrittenStatus(false);
        mEsimOsUpdateUtils.saveBooleanInSharedPrefs(EsimOsUpdateUtils.
                OS_UPDATE_OS_FILE_WRITTEN, false);
        String fileName = mEsimOsUpdateUtils.getFileName();
        Log.i(LOG_TAG, " OS Upgrade process success, Image FileName = " + fileName);

        mEsimOsUpdateUtils.saveStringInSharedPrefs(EsimOsUpdateUtils.
                OS_UPDATE_SUCCESS_FILE_NAME, fileName);
    }

    private void handleCardStateChanged() {
        TelephonyManager telMgr =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        /**
         * Proceed OS update only for G+D slotId.
         */
        if ((mEsimSlotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) || ((telMgr == null) ||
                !telMgr.hasIccCard(mEsimSlotId))) {
            Log.i(LOG_TAG,"handleCardStateChanged, error mEsimSlotId = " + mEsimSlotId);
            return;
        }

        mIsCardStatusVerified = false;
        boolean isManifestWritten = mEsimOsUpdateUtils.getBooleanFromSharedPrefs(
                EsimOsUpdateUtils.OS_UPDATE_MANIFEST_WRITTEN);
        boolean isOsFileWritten = mEsimOsUpdateUtils.getBooleanFromSharedPrefs(
                EsimOsUpdateUtils.OS_UPDATE_OS_FILE_WRITTEN);

        Log.i(LOG_TAG,"handleCardStateChanged : mEsimSlotId = " + mEsimSlotId
                + ", isManifestWritten = " + isManifestWritten + ", isOsFileWritten = "
                + isOsFileWritten);
        if (isManifestWritten && !isOsFileWritten) {
            /**
             * Manifest is written successfully, check if the SIM STATE is PRESENT in that case
             * read the ITL State and TransactionIds.
             * If ITL state & TransactionIds are proper then move card to PASS_THROUGH Mode and
             * start writing OS File to the Euicc Card.
             */
            mEsimOsUpdateUtils.readITLAndOSProperties(mEsimSlotId, mPortId);
            if (verifyITLDetailsAfterManifestWrite()) {
                performCardReset(TelephonyManager.CARD_POWER_UP_PASS_THROUGH);
            }
        } else if (isManifestWritten && isOsFileWritten) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage
                        (EVENT_PERFORM_WRITE_OS_CONTENT_DONE), DELAY_INTERVAL);
        } else {
            /**
             * This condition refers to initial state when Manifest is not written.
             * And eSIM Slot Id is detected, so proceed to write the OS Manifest file.
             */
            if (!mIsCardStatusVerified) mEsimOsUpdateUtils.readITLAndOSProperties(mEsimSlotId,
                    mPortId);
            mIsCardStatusVerified = true;

            if (verifyITLAndOSDetailsBeforeManifestWrite()) {
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_PERFORM_WRITE_MANIFEST_CONTENT));
            }
        }
    }

    private void handleSlotStatusEvent() {
        UiccSlotInfo[] slotInfo = null;
        UiccSlotInfo eSimSlotInfo = null;

        /**
         * If Manifest writing is success, don't look for EID and EuiccSlot, just bring the card to
         * PASS_THROUGH MODE and write the OS file to the CARD.
         */
        boolean isManifestWritten = mEsimOsUpdateUtils.getBooleanFromSharedPrefs(
                EsimOsUpdateUtils.OS_UPDATE_MANIFEST_WRITTEN);
        boolean isOsFileWritten = mEsimOsUpdateUtils.getBooleanFromSharedPrefs(
                EsimOsUpdateUtils.OS_UPDATE_OS_FILE_WRITTEN);
        Log.i(LOG_TAG, "handleSlotStatusEvent isManifestWritten = "
                + isManifestWritten + " isOsFileWritten = " + isOsFileWritten);
        TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            slotInfo = tm.getUiccSlotsInfo();
        }

        if (slotInfo == null || slotInfo.length == 0) {
            Log.i(LOG_TAG, "SIM Slot information not available ");
            return;
        }

        for (int i = 0; i < slotInfo.length; i++) {
            UiccSlotInfo info = slotInfo[i];

            if (info != null && info.getCardStateInfo() == UiccSlotInfo.CARD_STATE_INFO_PRESENT) {
                mEsimOsUpdateUtils.readITLAndOSProperties(mEsimSlotId, mPortId);
                if (isManifestWritten && isOsFileWritten) {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage
                            (EVENT_PERFORM_WRITE_OS_CONTENT_DONE), DELAY_INTERVAL);
                    break;
                } else if (isManifestWritten && !isOsFileWritten
                        && verifyITLDetailsAfterManifestWrite()) {
                    /**
                     * If Manifest Writing is Success, don't look for EID.
                     *
                     * Since after Manifest writing , OS is removed. EID and MEP won't
                     * be available in UiccSlotInfo. Hence look for CARD STATE and proceed
                     * to write the OS Image.
                     */
                    performCardReset(TelephonyManager.CARD_POWER_UP_PASS_THROUGH);
                    break;
                } else if (info.getIsEuicc() && info.getCardId() != null &&
                        !(info.getCardId()).isEmpty()) {
                    // Check whether particular slot is eSIM and supports OS Update.
                    if (mEsimOsUpdateUtils.isOsUpdateSupported(info.getCardId())) {
                        eSimSlotInfo = info;
                        mEsimSlotId = getSupportedSlotIdFromPortList(info);
                        Log.i(LOG_TAG, "eSIM SlotId = " + mEsimSlotId);
                        continue;
                    }
                }
            }
        }
        if (eSimSlotInfo == null) {
            Log.i(LOG_TAG, "No supported eSIM present on device ");
            return;
        }
        logd("Slot Status verified ... ");

        /**
         * CardStatus and SlotStatus both are verified, proceed for OS Update.
         */
        if (mIsCardStatusVerified && verifyITLAndOSDetailsBeforeManifestWrite()) {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_PERFORM_WRITE_MANIFEST_CONTENT));
        }
    }

    private int getSupportedSlotIdFromPortList(UiccSlotInfo slotInfo) {
        int supportedSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        for (UiccPortInfo portInfo : slotInfo.getPorts()) {
            if (portInfo.isActive()) {
                supportedSlotIndex = portInfo.getLogicalSlotIndex();
                mPortId = portInfo.getPortIndex();
            }
        }
        Log.i(LOG_TAG, "getSupportedSlotIdFromPortList = " + supportedSlotIndex + " portId = " +
                mPortId);
        return supportedSlotIndex;
    }

    private void performCardReset(int state) {
        TelephonyManager telMgr = (TelephonyManager) mContext.getSystemService(Context.
                TELEPHONY_SERVICE);
        logd("performCardReset state = " + state);

        if (telMgr == null) {
            logd("performCardReset, telMgr null ");
            return;
        }

        Executor executor = Executors.newSingleThreadExecutor();
        Consumer<Integer> callback = result -> {
            Log.i(LOG_TAG, " CardReset result received. result = " + result + " state = " + state);

            if (state == TelephonyManager.CARD_POWER_DOWN &&
                    result == TelephonyManager.SET_SIM_POWER_STATE_SUCCESS) {
                performCardReset(TelephonyManager.CARD_POWER_UP);
            } else if (state == TelephonyManager.CARD_POWER_UP &&
                    result == TelephonyManager.SET_SIM_POWER_STATE_SUCCESS) {
                /**
                 * Wait for CARD_STATE to be PREESENT and perform the operation.
                 */
            } else if (state == TelephonyManager.CARD_POWER_UP_PASS_THROUGH &&
                    result == TelephonyManager.SET_SIM_POWER_STATE_SUCCESS) {
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_PERFORM_WRITE_OS_CONTENT));
            }
        };
        telMgr.setSimPowerStateForSlot(mEsimSlotId, state, executor, callback);
    }

    /**
     * This method will read the OS File from path : /data/user_de/0/com.qti.phone/cache/
     *
     * OS File name will be taken from property value persist.vendor.esim_os_update_name.
     * This property value is the name of the OS File. This value must be correct,
     * otherwise proper OS File name will not be picked.
     *
     * If the initial ITL validation passed, start sending APDU command to card line by line,
     *  - If manifest data is written card would respond with 6301
     *  - If full OS data is written card would respond with 9000
     */
    private void writeOSFileContentInCard() {
        boolean isManifestWritten = mEsimOsUpdateUtils.getBooleanFromSharedPrefs(
                EsimOsUpdateUtils.OS_UPDATE_MANIFEST_WRITTEN);
        File osFile = mEsimOsUpdateUtils.getOsImageFile();
        if (osFile == null) {
            Log.i(LOG_TAG, "writeOSFileContentInCard, osfile does not exists");
            return;
        }
        logd("isManifestWritten = " + isManifestWritten);

        int channelId = mEsimOsUpdateUtils.openITLChannel(mEsimSlotId, mPortId);
        if (!mEsimOsUpdateUtils.isValidLogicalChannel(channelId)) {
            Log.i(LOG_TAG, " Invalid Logical ChannelId = " + channelId);
            return;
        }
        String TAG = null;
        String resp = null;
        try {
            InputStreamReader inputreader = new InputStreamReader(new FileInputStream(osFile));
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line = null;
            int count = 0;
            APDUParams apduParams = null;
            while ((line = buffreader.readLine()) != null) {
                if (line != null && !line.isEmpty()) {
                    String lineWithoutSpaces = line.replace(" ", "");

                    int hexCount = Integer.parseInt(Integer.toHexString(count), 16);
                    logd("mEsimSlotId = " + mEsimSlotId + ", mPortId = " + mPortId
                            + ", count = " + count + " hexCount = " + hexCount
                            + ", lineWithoutSpace  = " + lineWithoutSpaces);

                    apduParams = new APDUParams(lineWithoutSpaces);
                    TAG = isManifestWritten ? "OS_FILE":"MANIFEST_FILE";
                    resp = mEsimOsUpdateUtils.transmitApdu(mEsimSlotId, mPortId, channelId,
                            apduParams.getCla(), apduParams.getIns(), apduParams.getP1(),
                            apduParams.getP2(), apduParams.getLc(),
                            apduParams.getData(), TAG);
                    if (resp.equalsIgnoreCase(APDU_STATUS_MANIFEST_ACCEPTED)) {
                        Log.i(LOG_TAG, " Manifest Data Accepted, Resetting the Card");
                        mEsimOsUpdateUtils.setManifestWrittenStatus(true);

                        /**
                         * At this point Card itself issuing the RESET Command.
                         */
                        break;
                    } else if (!resp.equalsIgnoreCase(APDU_STATUS_NO_ERROR)) {
                        Log.i(LOG_TAG, " OS Writing got Error.. Exit");
                        break;
                    }
                }
                count++;
            }
            inputreader.close();
            buffreader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mEsimOsUpdateUtils.closeChannel(mEsimSlotId, mPortId, channelId);
            if ((resp != null) && resp.equalsIgnoreCase(APDU_STATUS_NO_ERROR)) {
                /**
                 * OS Update Successfull. Perform CARD_DOWN then UP.
                 */
                mEsimOsUpdateUtils.saveBooleanInSharedPrefs(EsimOsUpdateUtils.
                        OS_UPDATE_OS_FILE_WRITTEN, true);
                Log.i(LOG_TAG, "OS Update Successfull, perform card down and up");
                performCardReset(TelephonyManager.CARD_POWER_DOWN);
            } else if ((resp != null) && resp.equalsIgnoreCase(APDU_STATUS_MANIFEST_ACCEPTED)) {
                performCardReset(TelephonyManager.CARD_POWER_DOWN);
            }
            Log.i(LOG_TAG, "writeOSFileContentInCard, final response " + resp);
        }
    }

    /*
     * verifyITLAndOSDetailsBeforeManifestWrite : Function to verify the ITL States,
     * TransactionIds and binary version before Manifest write success.
     */
    private boolean verifyITLAndOSDetailsBeforeManifestWrite() {
        boolean isOSDetailsVerified = false;
        EsimOsUpdateUtils.EuiccOSInfo euiccOsInfo = mEsimOsUpdateUtils.getCurrentEuiccOsInfo();
        File osFile = mEsimOsUpdateUtils.getOsImageFile();
        /**
         * Check if OS File exists and Initial OS state is expected to be in
         * ITL_STATE_SECURED_OS_INSTALLED and both transactionIds shall be same.
         */
        if ((osFile != null) && (euiccOsInfo != null) &&
                (euiccOsInfo.getItlState() == ITL_STATE_SECURED_OS_INSTALLED) &&
                (euiccOsInfo.getOngoingTransactionId() == euiccOsInfo.getCurrentTransactionId())) {
            /**
             * Compare ImageFile name.
             *
             * Compare the previous stored OS Filename with the current OS FileName.
             * Filenames should be different to proceed for OS update.
             */
            String oldOSUpdateSuccessFileName = mEsimOsUpdateUtils.getStringFromSharedPrefs(
                    EsimOsUpdateUtils.OS_UPDATE_SUCCESS_FILE_NAME);
            Log.i(LOG_TAG, " oldOSUpdateSuccessFileName = " + oldOSUpdateSuccessFileName);
            if (!oldOSUpdateSuccessFileName.equalsIgnoreCase
                    (mEsimOsUpdateUtils.getFileName())) {
                isOSDetailsVerified = true;
            }
         }
        Log.i(LOG_TAG, "verifyITLAndOSDetailsBeforeManifestWrite : isOSDetailsVerified = "
                + isOSDetailsVerified + ", euiccOsInfo = " + euiccOsInfo + ", osFile = "
                + osFile);
        return isOSDetailsVerified;
    }

    /*
     * Function to verify the ITL State after Manifest write success.
     *
     * ITL State should be ITL_STATE_SECURED and OnGoing TransactionId
     * should greater then CurrentTransactionId.
     */
    private boolean verifyITLDetailsAfterManifestWrite() {
        boolean isITLDetailsVerified = false;
        EsimOsUpdateUtils.EuiccOSInfo euiccOsInfo = mEsimOsUpdateUtils.getCurrentEuiccOsInfo();
        /**
         * Verify ITL State and TransactionIds.
         */
        if ((euiccOsInfo != null) && (euiccOsInfo.getItlState() == ITL_STATE_SECURED) &&
                (euiccOsInfo.getOngoingTransactionId() > euiccOsInfo.getCurrentTransactionId())) {
            isITLDetailsVerified = true;
        }
        Log.i(LOG_TAG, "verifyITLDetailsAfterManifestWrite : isITLDetailsVerified = "
                + isITLDetailsVerified + ", euiccOsInfo = " + euiccOsInfo);
        return isITLDetailsVerified;
    }

    /*
     * Function to verify the ITL State, Transaction Id
     *
     * ITL State should be ITL_STATE_SECURED_OS_INSTALLED and OnGoing
     * TransactionId should be equal to CurrentTransactionId.
     *
     * Return false if verification fails.
     */
    private boolean verifyITLAndOSDetailsAfterOSWrite() {
        boolean isITLDetailsVerified = false;
        EsimOsUpdateUtils.EuiccOSInfo euiccOsInfo = mEsimOsUpdateUtils.getCurrentEuiccOsInfo();

        if (euiccOsInfo != null && (euiccOsInfo.getItlState() == ITL_STATE_SECURED_OS_INSTALLED) &&
                (euiccOsInfo.getOngoingTransactionId() == euiccOsInfo.getCurrentTransactionId())) {
            isITLDetailsVerified = true;
        }
        int oldOSBinaryVersion = mEsimOsUpdateUtils.getIntFromSharedPrefs(EsimOsUpdateUtils.
                EUICC_OS_BINARY_VERSION);
        logd("verifyITLAndOSDetailsAfterOSWrite : isITLDetailsVerified = " + isITLDetailsVerified +
                ", oldOSBinaryVersion = " + oldOSBinaryVersion + ", euiccOsInfo = " + euiccOsInfo);
        return isITLDetailsVerified;
    }

    private void logd(String string) {
        Log.d(LOG_TAG, string);
    }

    private void loge(String string) {
        Log.e(LOG_TAG, string);
    }

    class APDUParams {
        int cla;
        int ins;
        int p1;
        int p2;
        int lc;
        String data;

        public APDUParams(String input) {
            if(input.length() < 10) {
                throw new IllegalArgumentException("Data length must be 10");
            }
            this.cla = Integer.parseInt(input.substring(0, 2), 16);
            this.ins = Integer.parseInt(input.substring(2, 4), 16);
            this.p1 = Integer.parseInt(input.substring(4, 6), 16);
            this.p2 = Integer.parseInt(input.substring(6, 8), 16);
            this.lc = Integer.parseInt(input.substring(8, 10), 16);
            this.data = input.substring(10, input.length());
        }

        public int getCla() {
            return cla;
        }

        public int getIns() {
            return ins;
        }

        public int getP1() {
            return p1;
        }

        public int getP2() {
            return p2;
        }

        public int getLc() {
            return lc;
        }

        public String getData() {
            return data;
        }
    }
}
