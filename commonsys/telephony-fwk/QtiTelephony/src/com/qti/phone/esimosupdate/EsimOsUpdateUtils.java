/******************************************************************************
 #  @file EsimOsUpdateUtils.java
 #
 #  @brief Class for providing the utility fucntions for EuiccOSUpdate.
 #
 #  ---------------------------------------------------------------------------
 #
 # Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 # All rights reserved.
 # Confidential and Proprietary - Qualcomm Technologies, Inc.
 #  ---------------------------------------------------------------------------*/

package com.qti.phone.esimosupdate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;

import com.qti.phone.QtiRadioProxy;
import com.qti.phone.R;

public class EsimOsUpdateUtils {

    private static final String LOG_TAG = "EsimOsUpdateUtils";
    private static final String ITL_AID = "D2760001180002FF34100089C0026E01";
    private static final String FILE_PATH = "/data/user_de/0/com.qti.phone/cache/";
    /**
     * G+D card can be identified by first 6 characters of its CARDID.
     * For G+D, first 6 characters are 890490
     */
    private static final String GD_CARD_PREFIX = "890490";
    private static EsimOsUpdateUtils sInstance;
    private TelephonyManager mTelMgr;
    private QtiRadioProxy mQtiRadioProxy;
    private EuiccOSInfo mEuiccOSInfo = null;

    SharedPreferences mSharedPref = null;
    private static final String PROPERTY_FILE_NAME = "persist.vendor.esim_os_update_name";
    /**
     * OS_UPDATE_MANIFEST_WRITTEN config will be saved to true when Manifest writing is
     * Success.
     */
    public static final String OS_UPDATE_MANIFEST_WRITTEN = "OS_UPDATE_MANIFEST_WRITTEN";
    /**
     * OS_UPDATE_OS_FILE_WRITTEN config will be saved to true when OS writing is
     * Success.
     */
    public static final String OS_UPDATE_OS_FILE_WRITTEN = "OS_UPDATE_OS_FILE_WRITTEN";
    /**
     * OS_UPDATE_SUCCESS_FILE_NAME config will be storing the file name of last OS Update.
     */
    public static final String OS_UPDATE_SUCCESS_FILE_NAME = "OS_UPDATE_SUCCESS_FILE_NAME";
    /**
     * This config will be used later when actual OS Image will be shared.
     */
    static final String EUICC_OS_BINARY_VERSION = "EUICC_OS_BINARY_VERSION";
    private static final int ITL_STATE_INITIALIZED = 1;
    private static final int ITL_STATE_LOCKED = 4;
    /**
     * Indicates an invalid channel.
     */
    private static final int INVALID_LOGICAL_CHANNEL_ID = -1;
    /**
     * Indicates an invalid ITL State
     */
    public static final int INVALID_ITL_STATE = 0;
    /**
     * If TransactionId(Current or OnGoing) reading fails from the EuiccCard
     * return this INVALID_TRANSACTION_ID.
     */
    public static final int INVALID_TRANSACTION_ID = -1;
    /**
     * 0xDF30 : Data TAG for fetching the ITL(ImageTrustLoader) State
     */
    private static final String GET_ITL_STATE_DATA_TAG = "DF30";
    /**
     * 0xDF31 : Data TAG for fetching the ITL(ImageTrustLoader) & OS Version
     */
    private static final String GET_ITL_AND_OS_VERSION = "DF31";
    /**
     * 0xDF32 : Data TAG for fetching the Current TransactionId
     */
    private static final String GET_ITL_CURRENT_TXID_TAG = "DF32";
    /**
     * 0xDF33 : Data TAG for fetching the Ongoing TransactionId
     */
    private static final String GET_ITL_ONGOING_TXID_TAG = "DF33";
    /**
     * 0x80 : CLA TAG for ITL(ImageTrustLoader) State
     */
    private static final int CLA_TAG = 0x80;
    /**
     * 0xCA : Instruction TAG for ITL(ImageTrustLoader) State.
     */
    private static final int INS_TAG = 0xCA;
    private int mRetryCount = 0;
    private final int MAX_RETRY_VALUE = 3;

    private EsimOsUpdateUtils(Context context, QtiRadioProxy qtiRadioProxy) {
        mQtiRadioProxy = qtiRadioProxy;
        Log.i(LOG_TAG, "EsimOsUpdateUtils initialized ...");
        mTelMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mSharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_preference_file_key),
                context.MODE_PRIVATE);
        boolean isManifestWritten = getBooleanFromSharedPrefs(
                EsimOsUpdateUtils.OS_UPDATE_MANIFEST_WRITTEN);
        boolean isOsFileWritten = getBooleanFromSharedPrefs(
                EsimOsUpdateUtils.OS_UPDATE_OS_FILE_WRITTEN);
        logd("isManifestWritten = " + isManifestWritten + " isOsFileWritten = " + isOsFileWritten);
    }

    static EsimOsUpdateUtils getInstance(Context context, QtiRadioProxy qtiRadioProxy) {
        if (sInstance == null) {
            sInstance = new EsimOsUpdateUtils(context, qtiRadioProxy);
        }
        return sInstance;
    }

    /**
     * Function to read the ITL Properties and store in EuiccOSInfo Cache.
     *
     * 1. ITL State
     * 2. ITL & OS Version
     * 3. Current Transaction Id
     * 4. OnGoing Transaction Id
     */
    void readITLAndOSProperties(int slotIndex, int portIndex) {
        int itlChannelId = openITLChannel(slotIndex, portIndex);
        logd("itlChannelId received = " + itlChannelId);
        if (itlChannelId == INVALID_LOGICAL_CHANNEL_ID) {
            return;
        }
        int itlState = readItlState(itlChannelId, slotIndex, portIndex);
        int currentTransactionId = readItlCurrentTxId(itlChannelId, slotIndex, portIndex);
        int ongoingTransactionId = readItlOngoingTxId(itlChannelId, slotIndex, portIndex);
        String itlOSVersion = getItlAndOsVersion(itlChannelId, slotIndex, portIndex);
        closeChannel(slotIndex, portIndex, itlChannelId);
        if (isITLAndOSDetailsProper(currentTransactionId, ongoingTransactionId, itlState,
                itlOSVersion)) {
            fillEuiccOsInfo(currentTransactionId, ongoingTransactionId, itlState, itlOSVersion);
        }
    }

    private boolean isITLAndOSDetailsProper(int currentTxId, int ongoingTxId, int itlState,
            String itlOSVersion) {
        boolean isITLDetailsProper = true;
        if ((currentTxId == INVALID_TRANSACTION_ID) || (ongoingTxId == INVALID_TRANSACTION_ID) ||
                (itlState == INVALID_ITL_STATE) || (itlOSVersion == null) ||
                (itlOSVersion.length() < 48)) {
            isITLDetailsProper = false;
        }
        Log.i(LOG_TAG, "isITLAndOSDetailsProper = " + isITLDetailsProper);
        return isITLDetailsProper;
    }

    private void fillEuiccOsInfo(int currentTxId, int ongoingTxId, int itlState,
            String itlOSVersion) {
        mEuiccOSInfo = null;
        mEuiccOSInfo = new EuiccOSInfo();
        mEuiccOSInfo.setCurrentTransactionId(currentTxId);
        mEuiccOSInfo.setOngoingTransactionId(ongoingTxId);
        mEuiccOSInfo.setItlState(itlState);
        logd("readITL itlOSVersion = " + itlOSVersion + ", length = " + itlOSVersion.length());
        if (itlOSVersion != null && itlOSVersion.length() > 47) {
            String osVersion = itlOSVersion.substring(14,16);
            String binaryVersion = itlOSVersion.substring(30,32);
            String itlVersion = itlOSVersion.substring(32,44);
            logd("osVersion = " + osVersion + ", binaryVersion = " + binaryVersion
                    + ", itlVersion = " + itlVersion);
            mEuiccOSInfo.setOsVersion(Integer.parseInt(osVersion));;
            mEuiccOSInfo.setBinaryVersion(Integer.parseInt(binaryVersion));
            mEuiccOSInfo.setItlVersion(itlVersion);
            saveIntInSharedPrefs(EUICC_OS_BINARY_VERSION, mEuiccOSInfo.getBinaryVersion());
        }
        logd("EuiccOsInfo initiated : " + mEuiccOSInfo);
    }

    EuiccOSInfo getCurrentEuiccOsInfo() {
        return mEuiccOSInfo;
    }

    int openITLChannel(int slotIndex, int portIndex) {
        int channelId = INVALID_LOGICAL_CHANNEL_ID;

        if (mTelMgr == null) {
            return channelId;
        }
        IccOpenLogicalChannelResponse rsp = null;
        while(mRetryCount < MAX_RETRY_VALUE) {

            rsp = mTelMgr.iccOpenLogicalChannelByPort(slotIndex, portIndex, ITL_AID, 0);
            Log.i(LOG_TAG, "Open Channel response = " + rsp + " count = " + mRetryCount);
            if (rsp.getStatus() == IccOpenLogicalChannelResponse.STATUS_NO_ERROR) {
                mRetryCount = 0;
                return rsp.getChannel();
            } else {
                mRetryCount++;
            }
        }
        logd("ITL OPEN_CHANNEL failed");
        mRetryCount = 0;
        return channelId;
    }

    void closeChannel(int slotIndex, int portIndex, int channelId) {
        Log.i(LOG_TAG, "close channel : channelId = " + channelId + ", slotIndex = "
                + slotIndex + ", portIndex = " + portIndex);
        if (mTelMgr != null) {
            try {
                mTelMgr.iccCloseLogicalChannelByPort(slotIndex, portIndex, channelId);
            } catch (IllegalArgumentException ex) {
                loge("Exception: " + ex);
            }
        }
    }

    String transmitApdu(int slotIndex, int portIndex, int channel, int cla,
            int instruction, int p1, int p2, int p3, String data, String TAG) {

        logd("cla = " + cla + " instruction = " + instruction + " p1 = " + p1 +" p2 = " + p2 +
                " p3 = " + p3 + " TAG = " + TAG);
        String rsp = mTelMgr.iccTransmitApduLogicalChannelByPort(slotIndex, portIndex, channel, cla,
                instruction, p1, p2, p3, data);
        logd("transmit apdu " + rsp);
        return rsp;
    }

    /**
     * int readItlState : function to fetch the current ITL(ImageTrustLoader State)
     *
     * There are 4 States of ITL
     * ITL_INITIALIZED : 01
     * ITL_SECURED : 02
     * ITL_SECURED_OS_INSTALLED : 03
     * ITL_LOCKED : 04
     */
    private int readItlState(int logicalChannelId, int slotIndex, int portIndex) {

        int itlState = INVALID_ITL_STATE;
        String state = null;
        state = transmitApdu(slotIndex, portIndex, logicalChannelId, CLA_TAG,
                INS_TAG, 0x00, 0xFE, 0x02, GET_ITL_STATE_DATA_TAG,
                "GET_INITIAL_ITL_STATE");
        /**
        * In case of Success Response for ITL State
        * Response String : 039000
        */
        if (state.length() > 2) {
            itlState = Integer.parseInt(state.substring(0,2));
        }
        logd("itlState = " + itlState);
        return itlState;
    }

    /**
     * Returns the current TransactionId from EuiccCard.
     *
     * currentTransactionid refers to number of times the EuiccCard OS Update is performed.
     * If it returns 0, means it is a fresh card and no update is performed on this card.
     *
     * If it returns 1, means 1 times OS upgrade was performed and it was successfull.
     *
     * If OS Update failed in between, this CurrentTransactionId will not increase.
     *
     * return INVALID_TRANSACTION_ID in case of failure.
     */
    private int readItlCurrentTxId(int logicalChannelId, int slotIndex, int portIndex) {
        int currentTransId = INVALID_TRANSACTION_ID;
        String currentTxId = null;

        currentTxId = transmitApdu(slotIndex, portIndex, logicalChannelId, CLA_TAG, INS_TAG, 0x00,
                0xFE, 0x02, GET_ITL_CURRENT_TXID_TAG, "CURRENT_TRANSACTION_ID");
        if (currentTxId.length() > 32) {
            currentTransId = Integer.parseInt(currentTxId.substring(0,32));
        }
        logd("currentTxId = " + currentTransId);
        return currentTransId;
    }

    /**
     * Returns the Ongoing TransactionId from EuiccCard.
     *
     * OngoingTransactionId is always same to CurrentTransactionId. Only when OS Update
     * procedure is performed and if Manifest data is accepted then OngoingTransactionId
     * will increase by 2.
     *
     * Otherwise during the Initial Stage, Current and Ongoing TransactionId will be same.
     *
     * E.g 1. if Current and Ongoing TransactionId is 0, 0. Then EuiccCard is in initial State.
     *
     * E.g 2. If Current is 0 and Ongoing is 1, means only Manifest data is written.
     *
     * return INVALID_TRANSACTION_ID in case of failure.
     */
    private int readItlOngoingTxId(int logicalChannelId, int slotIndex, int portIndex) {

        String onGoingTxId = null;
        int ongoingTransId = INVALID_TRANSACTION_ID;

        onGoingTxId = transmitApdu(slotIndex, portIndex, logicalChannelId, CLA_TAG, INS_TAG,
                0x00, 0xFE, 0x02, GET_ITL_ONGOING_TXID_TAG, "ONGOING_TRANSACTION_ID");
        if (onGoingTxId.length() > 32) {
            ongoingTransId = Integer.parseInt(onGoingTxId.substring(0,32));
        }
        logd("onGoingTxId = " + ongoingTransId);
        return ongoingTransId;
    }

    private String getItlAndOsVersion(int logicalChannelId, int slotIndex, int portIndex) {
        String version = null;
        version = transmitApdu(slotIndex, portIndex, logicalChannelId, CLA_TAG, INS_TAG, 0x00,
                0xFE, 0x02, GET_ITL_AND_OS_VERSION, "NULL");
        return version;
    }

    // OS update is supported on G+D carrier only, validate whethere current cardId belongs to G+D
    boolean isOsUpdateSupported(String cardId) {
        boolean isCardSupported = false;
        if ((cardId.substring(0, 6)).equalsIgnoreCase(GD_CARD_PREFIX)) {
            isCardSupported = true;
        }
        return isCardSupported;
    }

    /**
     * Utility function to check if ITL Application is in
     * Locked or INITIALIZED state.
     *
     * Locked State means CARD is Locked and is not able to
     * accept any kind of requests.
     *
     * INITIALIZED state means the card is in factory state,
     * which can further be measured as INITIAL state.
     */
    boolean isITLLockedOrInitialized(int itlState) {
        return ((itlState == ITL_STATE_LOCKED) || (itlState == ITL_STATE_INITIALIZED));
    }

    String getFileName() {
        String name = "";
        try {
            name = mQtiRadioProxy.getPropertyValueString(PROPERTY_FILE_NAME, name);
        } catch (RemoteException ex) {
            loge("Exception: " + ex);
        }
        Log.i(LOG_TAG, "OS Image filename = " + name);
        return name;
    }

    File getOsImageFile() {
        String fileName = getFileName();
        if (fileName.length() == 0) {
            return null;
        }
        fileName = FILE_PATH + fileName;
        File file = new File(fileName);
        if (file.isFile() && file.exists()) {
            return file;
        }
        return null;
    }

    boolean isValidLogicalChannel(int logicalChannelId) {
        return (logicalChannelId != INVALID_LOGICAL_CHANNEL_ID);
    }

    /**
     * Parse binary version from Image file name.
     *
     * Image file name syntax below :
     * ST33J2M0t1_SMSV4MEP_Deploy_prod_ROM_1.itlc
     *
     * fixme : when Actual OSImage comes, need to re-use this.
     */
    int getOsBinaryVersionFromImageFileName() {
        int binaryVersion = -1;
        String fileName = getFileName();
        if (fileName.length() > 36) {
            String strBinaryVersion = fileName.substring(36, 37);
            binaryVersion = Integer.parseInt(strBinaryVersion);
        }
        logd("Binary version = " + binaryVersion);
        return binaryVersion;
    }

    private void logd(String string) {
        Log.d(LOG_TAG, string);
    }

    private void loge(String string) {
        Log.e(LOG_TAG, string);
    }

    void saveIntInSharedPrefs(String key, int value) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    int getIntFromSharedPrefs(String key) {
        return mSharedPref.getInt(key, -1);
    }

    void saveBooleanInSharedPrefs(String key, boolean value) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    boolean getBooleanFromSharedPrefs(String key) {
        return mSharedPref.getBoolean(key, false);
    }

    void saveStringInSharedPrefs(String key, String value) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    String getStringFromSharedPrefs(String key) {
        return mSharedPref.getString(key, "");
    }

    void setManifestWrittenStatus(boolean isManifestWritten) {
        saveBooleanInSharedPrefs(OS_UPDATE_MANIFEST_WRITTEN, isManifestWritten);
    }

    public class EuiccOSInfo {
        int currentTransactionId;
        int ongoingTransactionId;
        int itlState;
        String itlVersion;
        int osVersion;
        int binaryVersion;

        public EuiccOSInfo() {
        }

        public void setCurrentTransactionId(int currentTransactionId) {
            this.currentTransactionId = currentTransactionId;
        }

        public int getCurrentTransactionId() {
            return currentTransactionId;
        }

        public void setOngoingTransactionId(int ongoingTransactionId) {
            this.ongoingTransactionId = ongoingTransactionId;
        }

        public int getOngoingTransactionId() {
            return ongoingTransactionId;
        }

        public void setItlState(int itlState) {
            this.itlState = itlState;
        }

        public int getItlState() {
            return itlState;
        }

        public void setItlVersion(String itlVersion) {
            this.itlVersion = itlVersion;
        }

        public String getItlVersion() {
            return itlVersion;
        }

        public void setOsVersion(int osVersion) {
            this.osVersion = osVersion;
        }

        public int getOsVersion() {
            return osVersion;
        }

        public void setBinaryVersion(int binaryVersion) {
            this.binaryVersion = binaryVersion;
        }

        public int getBinaryVersion() {
            return binaryVersion;
        }

        public String toString() {
            return "EuiccOSInfo : currentTxId = " + currentTransactionId + ", ongoingTxId = " +
                    ongoingTransactionId + ", itlState = " + itlState + ", itlVersion = " +
                    itlVersion + ", osVersion = " + osVersion + ", binaryVersion = " +
                    binaryVersion;
        }
    }
}
