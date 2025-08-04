/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.session;

import android.hardware.soundtrigger.SoundTrigger;

import com.qualcomm.listen.ListenSoundModel;
import com.qualcomm.listen.ListenTypes;
import com.qualcomm.qti.sva.controller.Global;
import com.qualcomm.qti.sva.data.IExtendedSmModel;
import com.qualcomm.qti.sva.data.ISmModel;
import com.qualcomm.qti.sva.utils.FileUtils;
import com.qualcomm.qti.sva.utils.LogUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class OpaqueDataHelper {
    private static final String TAG = OpaqueDataHelper.class.getSimpleName();

    /*
    #define ST_MAX_SOUND_MODELS 10
    #define ST_MAX_CONFIDENCE_KEYWORDS 10
    #define ST_MAX_CONFIDENCE_USERS 10
    */
    private static final int ST_MAX_SOUND_MODELS = 10;
    private static final int ST_MAX_CONFIDENCE_KEYWORDS = 10;
    private static final int ST_MAX_CONFIDENCE_USERS = 10;

    private final int SIZE_OF_ENUM_ST_SOUND_MODEL_ID = 4;
    private final int SIZE_OF_ST_PARAM_KEY = 4;

    private final int ST_SM_ID_NONE = 0x0000;
    private static final int ST_SM_ID_SVA_FIRST_STAGE = 0x0001;   //ST_SM_ID_SVA_GMM
    private static final int ST_SM_ID_SVA_S_STAGE_KEYPHRASE = 0x0002;   //ST_SM_ID_SVA_CNN
    private static final int ST_SM_ID_SVA_S_STAGE_USER = 0x0004;   //ST_SM_ID_SVA_VOP
    private static final int ST_SM_ID_SVA_UDK7_S_STAGE_KEYPHRASE = 0x0040;
    private final int ST_SM_ID_SVA_END = 0x0080;
    private final int ST_SM_ID_CUSTOM_START = 0x0100;
    private final int ST_SM_ID_CUSTOM_END = 0x8000;

    // Param key for detection payload extension
    private static final int ST_PARAM_KEY_KEYWORD_BUFFER = 0x10001;
    private static final int ST_PARAM_KEY_SSTAGE_KW_ENGINE_INFO = 0x10002;
    private static final int ST_PARAM_KEY_SSTAGE_UV_ENGINE_INFO = 0x10003;
    private static final int ST_PARAM_KEY_IS_BARGEIN = 0x10004;

    /*
    // struct size define
    struct st_param_header
    {
        st_param_key_t key_id;
        uint32_t payload_size;
    };
    */
    private final int SIZE_OF_ST_PARAM_HEADER = SIZE_OF_ST_PARAM_KEY + 4;
    /*
    struct st_user_levels
    {
        uint32_t user_id;
        int32_t level;
    };
    */
    private final int SIZE_OF_ST_USER_LEVEL = 8;
    /*
    struct st_keyword_levels
    {
        int32_t kw_level;
        uint32_t num_user_levels;
        struct st_user_levels user_levels[ST_MAX_CONFIDENCE_USERS];
    };
    */
    private final int SIZE_OF_ST_KEYWORD_LEVELS = 8
            + ST_MAX_CONFIDENCE_USERS * SIZE_OF_ST_USER_LEVEL;
    /*
    struct st_sound_model_conf_levels
    {
        st_sound_model_id_t sm_id;
        uint32_t num_kw_levels;
        struct st_keyword_levels kw_levels[ST_MAX_CONFIDENCE_KEYWORDS];
    };
    */
    private final int SIZE_OF_ST_SOUND_MODEL_CONF_LEVELS = SIZE_OF_ENUM_ST_SOUND_MODEL_ID + 4
            + ST_MAX_CONFIDENCE_KEYWORDS * SIZE_OF_ST_KEYWORD_LEVELS;
    /*
    struct st_confidence_levels_info
    {
        uint32_t version;
        uint32_t num_sound_models;
        struct st_sound_model_conf_levels conf_levels[ST_MAX_SOUND_MODELS];
    };
    */
    private final int SIZE_OF_ST_CONFIDENCE_LEVELS_INFO = 8
            + ST_MAX_SOUND_MODELS * SIZE_OF_ST_SOUND_MODEL_CONF_LEVELS;
    /*
    struct st_hist_buffer_info
    {
        uint32_t version;
        uint32_t hist_buffer_duration_msec;
        uint32_t pre_roll_duration_msec;
    };
    */
    private final int SIZE_OF_ST_HIST_BUFFER_INFO = 12;
    /*
    struct st_keyword_indices_info
    {
        uint32_t version;
        uint32_t start_index; // in bytes
        uint32_t end_index;   // in bytes
    };
    */
    private final int SIZE_OF_ST_KEYWORD_INDICES_INFO = 12;
    /*
    public static native long System.currentTimeMillis()
     */
    private final int SIZE_OF_TIME_STAMP_INFO = 8;
    private ListenTypes.SVASoundModelInfo mSmInfo;
    private boolean mHasConfidenceLevelsParam;
    private boolean mHasHistBufferConfigParam;
    private boolean mHasKeywordIndicesParam;
    private boolean mHasTimeStampParam;
    private boolean mHasSecondStageUser = true;
    private boolean mHasFirstStage = true;
    private boolean mHasSecondStageKey= true;
    private int mNumOfModels = 3;
    private String mSmName;

    public OpaqueDataHelper(String smName, boolean hasHistBufferConfigParam,
                            boolean hasKeywordIndicesParam,
                            boolean hasTimeStampParam) {
        mSmName = smName;
        mSmInfo = query(smName);
        mHasHistBufferConfigParam = hasHistBufferConfigParam;
        IExtendedSmModel extendedSmModel = Global.getInstance().getExtendedSmMgr().
                getSoundModel(smName);
        if (null != mSmInfo && extendedSmModel != null
                && extendedSmModel.isSupportExtendedOpaqueData()) {
            mHasConfidenceLevelsParam = true;
        } else {
            mHasConfidenceLevelsParam = false;
        }
        mHasKeywordIndicesParam = hasKeywordIndicesParam;
        mHasTimeStampParam = hasTimeStampParam;
    }

    private int getTotalBufferSize() {
        int totalSize = 0;
        if (mHasConfidenceLevelsParam) {
            totalSize += SIZE_OF_ST_PARAM_HEADER + SIZE_OF_ST_CONFIDENCE_LEVELS_INFO;
        }

        if (mHasHistBufferConfigParam) {
            totalSize += SIZE_OF_ST_PARAM_HEADER + SIZE_OF_ST_HIST_BUFFER_INFO;
        }

        if (mHasKeywordIndicesParam) {
            totalSize += SIZE_OF_ST_PARAM_HEADER + SIZE_OF_ST_KEYWORD_INDICES_INFO;
        }

        if (mHasTimeStampParam) {
            totalSize += SIZE_OF_ST_PARAM_HEADER + SIZE_OF_TIME_STAMP_INFO;
        }

        return totalSize;
    }

    public byte[] fillOpaqueDataByteBuffer(int histDuration, int preRollDuration) {
        int OPAQUE_SIZE = 12;
        int MINOR_VERSION = 2;
        byte[] opaqueByteArray = new byte[OPAQUE_SIZE];

        // fill version
        opaqueByteArray[3] = (byte) (MINOR_VERSION >> 24 & 0xff);
        opaqueByteArray[2] = (byte) (MINOR_VERSION >> 16 & 0xff);
        opaqueByteArray[1] = (byte) (MINOR_VERSION >> 8 & 0xff);
        opaqueByteArray[0] = (byte) (MINOR_VERSION & 0xff);

        // fill histDuration
        opaqueByteArray[7] = (byte) (histDuration >> 24 & 0xff);
        opaqueByteArray[6] = (byte) (histDuration >> 16 & 0xff);
        opaqueByteArray[5] = (byte) (histDuration >> 8 & 0xff);
        opaqueByteArray[4] = (byte) (histDuration & 0xff);

        // fill pre-roll duration
        opaqueByteArray[11] = (byte) (preRollDuration >> 24 & 0xff);
        opaqueByteArray[10] = (byte) (preRollDuration >> 16 & 0xff);
        opaqueByteArray[9] = (byte) (preRollDuration >> 8 & 0xff);
        opaqueByteArray[8] = (byte) (preRollDuration & 0xff);

        return opaqueByteArray;
    }

    public byte[] fillOpaqueDataByteBuffer(int[] confidencelevels, int histBufferDuration, int preRollDuration) {
        byte[] buffer;
        int startPos = 0;
        int size = getTotalBufferSize();
        if (size > 0) {
            buffer = new byte[size];
            if (mHasConfidenceLevelsParam) {
                fillConfidenceLevelParam(buffer, 0, confidencelevels);
                startPos += SIZE_OF_ST_PARAM_HEADER + SIZE_OF_ST_CONFIDENCE_LEVELS_INFO;
            }

            if (mHasHistBufferConfigParam) {
                fillHistBufferConfigParam(buffer, startPos, histBufferDuration, preRollDuration);
            }

            return buffer;
        }

        return null;
    }

    public byte[] fillOpaqueDataByteBuffer(int firstKeyphraseLevel, int firstUserLevel,
                                           int secondKeyphraseLevel, int secondUserLevel,
                                           int histBufferDuration, int preRollDuration,
                                           int indicesStart, int indicesEnd) {
        byte[] buffer;
        int startPos = 0;
        int size = getTotalBufferSize();
        if (size > 0) {
            buffer = new byte[size];
            if (mHasConfidenceLevelsParam) {
                fillConfidenceLevelParam(buffer, 0, firstKeyphraseLevel,
                        firstUserLevel, secondKeyphraseLevel, secondUserLevel);
                startPos += SIZE_OF_ST_PARAM_HEADER + SIZE_OF_ST_CONFIDENCE_LEVELS_INFO;
            }

            if (mHasHistBufferConfigParam) {
                fillHistBufferConfigParam(buffer, startPos, histBufferDuration, preRollDuration);
                startPos += SIZE_OF_ST_PARAM_HEADER + SIZE_OF_ST_HIST_BUFFER_INFO;
            }

            if (mHasKeywordIndicesParam) {
                fillKeywordIndicesParam(buffer, startPos, indicesStart, indicesEnd);
                startPos += SIZE_OF_ST_PARAM_HEADER + SIZE_OF_ST_KEYWORD_INDICES_INFO;
            }

            if (mHasTimeStampParam) {
                fillTimeStampParam(buffer, startPos);
            }

            return buffer;
        }
        return null;
    }

    private void fillKeywordIndicesParam(byte[] buffer, int startPos,
                                         int indicesStart, int indicesEnd) {
        // fill the key id
        int keyId = ST_Param_Key.ST_PARAM_KEY_KEYWORD_INDICES.ordinal();
        int startIndex = startPos;
        fillInt(buffer, startIndex, keyId);

        // fill the payload size
        int payloadSize = SIZE_OF_ST_KEYWORD_INDICES_INFO;
        startIndex = startPos + 4;
        fillInt(buffer, startIndex, payloadSize);

        // fill version
        int version = 1;
        startIndex = startPos + 8;
        fillInt(buffer, startIndex, version);

        // fill indices start index
        startIndex = startPos + 12;
        fillInt(buffer, startIndex, indicesStart);

        // fill indices end index
        startIndex = startPos + 16;
        fillInt(buffer, startIndex, indicesEnd);
    }

    private void fillTimeStampParam(byte[] buffer, int startPos) {
        // fill the key id
        int keyId = ST_Param_Key.ST_PARAM_KEY_TIMESTAMP.ordinal();
        int startIndex = startPos;
        fillInt(buffer, startIndex, keyId);

        // fill the payload size
        int payloadSize = SIZE_OF_TIME_STAMP_INFO;
        startIndex = startPos + 4;
        fillInt(buffer, startIndex, payloadSize);

        long timeStamp = System.currentTimeMillis();
        startIndex = startPos + 8;
        fillLong(buffer, startIndex, timeStamp);
    }

    private void fillHistBufferConfigParam(byte[] buffer, int startPos,
                                           int histBufferDuration, int preRollDuration) {
        // fill the key id
        int keyId = ST_Param_Key.ST_PARAM_KEY_HISTORY_BUFFER_CONFIG.ordinal();
        int startIndex = startPos;
        fillInt(buffer, startIndex, keyId);

        // fill the payload size
        int payloadSize = SIZE_OF_ST_HIST_BUFFER_INFO;
        startIndex = startPos + 4;
        fillInt(buffer, startIndex, payloadSize);

        // fill the version
        int histVersion = 2;
        startIndex = startPos + 8;
        fillInt(buffer, startIndex, histVersion);

        // fill the hist buffer duration
        startIndex = startPos + 12;
        fillInt(buffer, startIndex, histBufferDuration);

        // fill the pre-roll duration
        startIndex = startPos + 16;
        fillInt(buffer, startIndex, preRollDuration);
    }

    private void fillConfidenceLevelParam(byte[] buffer, int startPos,
                                          int firstKeyphraseLevel, int firstUserLevel,
                                          int secondKeyphraseLevel, int secondUserLevel) {
        // fill the key id
        int keyId = ST_Param_Key.ST_PARAM_KEY_CONFIDENCE_LEVELS.ordinal();
        int startIndex = startPos;
        fillInt(buffer, startIndex, keyId);

        // fill the payload size
        int payloadSize = SIZE_OF_ST_CONFIDENCE_LEVELS_INFO;
        startIndex = startPos + 4;
        fillInt(buffer, startIndex, payloadSize);

        // fill the version
        int version = 0x02;
        startIndex = startPos + 8;
        fillInt(buffer, startIndex, version);

        // fill the sound model count
        int modelCount = mNumOfModels;
        startIndex = startPos + 12;
        fillInt(buffer, startIndex, modelCount);

        // match the userId with keyphraseRecognitionExtra in priority
        IExtendedSmModel extendedSmModel = Global.getInstance().getExtendedSmMgr()
                .getSoundModel(mSmName);
        SoundTrigger.KeyphraseRecognitionExtra[] extraArray
                = extendedSmModel.getKeyphraseRecognitionExtra();
        boolean bMatchWithExtra = false;
        if (null != extraArray && extraArray.length > 0) {
            bMatchWithExtra = true;
        }
        LogUtils.d(TAG, "fillConfidenceLevelParam: bMatchWithExtra = "
                + bMatchWithExtra);

        // fill sound model GMM confidence level
        if (mHasFirstStage) {
            int gmmSmId = ST_SM_ID_SVA_FIRST_STAGE;
            startIndex = startPos + 16;
            fillInt(buffer, startIndex, gmmSmId);

            // fill the keyphrase count
            int keyphraseCount = null != mSmInfo ? mSmInfo.keywordInfo.length : 0;
            if (bMatchWithExtra) {
                keyphraseCount = extraArray.length;
            }
            LogUtils.d(TAG, "fillConfidenceLevelParam: GMM keyphraseCount = "
                    + keyphraseCount);

            startIndex = startPos + 20;
            fillInt(buffer, startIndex, keyphraseCount);

            startIndex = startPos + 24;
            for (int ii = 0; ii < keyphraseCount; ii++) {
                int userCount = (null != mSmInfo && null != mSmInfo.keywordInfo[ii] &&
                                 mSmInfo.keywordInfo[ii].activeUsers != null)
                                 ? mSmInfo.keywordInfo[ii].activeUsers.length : 0;
                LogUtils.d(TAG, "fillConfidenceLevelParam: GMM query userCount = "
                        + userCount);
                if (bMatchWithExtra) {
                    userCount = extraArray[ii].confidenceLevels.length;
                }
                LogUtils.d(TAG, "fillConfidenceLevelParam: GMM userCount = " + userCount);

                // fill the ii keyphrase level
                int pos = startIndex + ii * (4 + 4 + ST_MAX_CONFIDENCE_USERS * SIZE_OF_ST_USER_LEVEL);
                //buffer[pos] = (byte) (gmmKeyphraseLevel & 0xff);
                fillInt(buffer, pos, firstKeyphraseLevel);


                // fill the user count
                pos = pos + 4;
                fillInt(buffer, pos, userCount);

                // fill the ii user level
                int jj = 0;
                if (bMatchWithExtra) {
                    for (SoundTrigger.ConfidenceLevel level : extraArray[ii].confidenceLevels) {
                        pos = pos + 4 + jj * SIZE_OF_ST_USER_LEVEL;

                        // fill user id
                        fillInt(buffer, pos, level.userId);

                        // fill user level
                        pos = pos + 4;
                        fillInt(buffer, pos, firstUserLevel);
                        jj++;
                    }
                } else {
                    for (jj = 0; jj < userCount; jj++) {
                        pos = pos + 4 + jj * SIZE_OF_ST_USER_LEVEL;

                        // fill user id
                        fillInt(buffer, pos, jj);

                        // fill user level
                        pos = pos + 4;
                        fillInt(buffer, pos, firstUserLevel);
                    }
                }
            }
        }

        if (mHasSecondStageKey) {
            int smId;
            if (extendedSmModel.isUdkSm() &&
                    extendedSmModel.getSoundModelVersion().getVersionNumber()
                            > ISmModel.ModelVersion.VERSION_6_0.getVersionNumber()) {
                smId = ST_SM_ID_SVA_UDK7_S_STAGE_KEYPHRASE;
            } else {
                smId = ST_SM_ID_SVA_S_STAGE_KEYPHRASE;
            }
            int newStartPos;
            if (mHasFirstStage) {
                newStartPos = startPos + SIZE_OF_ST_SOUND_MODEL_CONF_LEVELS;
            } else {
                newStartPos = startPos;
            }
            // fill model id
            startIndex = newStartPos + 16;
            fillInt(buffer, startIndex, smId);

            // fill the keyphrase count
            int secondKeyphraseCount = null != mSmInfo ? mSmInfo.keywordInfo.length : 0;
            startIndex = newStartPos + 20;
            fillInt(buffer, startIndex, secondKeyphraseCount);

            startIndex = newStartPos + 24;
            for (int iii = 0; iii < secondKeyphraseCount; iii++) {
                int userCount = mSmInfo.keywordInfo[iii].activeUsers != null
                                ? mSmInfo.keywordInfo[iii].activeUsers.length : 0;

                // fill the ii keyphrase level
                int pos = startIndex + iii * (4 + ST_MAX_CONFIDENCE_USERS*SIZE_OF_ST_USER_LEVEL);
                //buffer[pos] = (byte) (cnnKeyphraseLevel & 0xff);
                fillInt(buffer, pos, secondKeyphraseLevel);

                // fill the user count
                pos = pos + 4;
                fillInt(buffer, pos, userCount);

                // fill the iii user level
                for (int jjj = 0; jjj < userCount; jjj++) {
                    pos = pos + 4 + jjj * SIZE_OF_ST_USER_LEVEL;

                    // fill user id
                    fillInt(buffer, pos, jjj);

                    // fill user level
                    pos = pos + 4;
                    fillInt(buffer, pos, secondUserLevel);
                }
            }
        }

        if (mHasSecondStageUser) {
            int smId = ST_SM_ID_SVA_S_STAGE_USER;
            int newStartPos;
            if (mHasFirstStage) {
                if (mHasSecondStageKey) {
                    newStartPos = startPos + 2*SIZE_OF_ST_SOUND_MODEL_CONF_LEVELS;
                } else {
                    newStartPos = startPos + SIZE_OF_ST_SOUND_MODEL_CONF_LEVELS;
                }
            } else {
                if (mHasSecondStageKey) {
                    newStartPos = startPos + SIZE_OF_ST_SOUND_MODEL_CONF_LEVELS;
                } else {
                    newStartPos = startPos;
                }
            }

            // fill model id
            startIndex = newStartPos + 16;
            fillInt(buffer, startIndex, smId);

            // fill the keyphrase count
            int keyphraseCount = null != mSmInfo ? mSmInfo.keywordInfo.length : 0;
            startIndex = newStartPos + 20;
            fillInt(buffer, startIndex, keyphraseCount);

            startIndex = newStartPos + 24;
            for (int iii = 0; iii < keyphraseCount; iii++) {
                int userCount = mSmInfo.keywordInfo[iii].activeUsers != null
                        ? mSmInfo.keywordInfo[iii].activeUsers.length : 0;
                LogUtils.d(TAG, "fillConfidenceLevelParam: VOP query userCount = "
                        + userCount);
                if (bMatchWithExtra) {
                    userCount = extraArray[iii].confidenceLevels.length;
                }
                LogUtils.d(TAG, "fillConfidenceLevelParam: VOP userCount = " + userCount);

                // fill the iii keyphrase level
                int pos = startIndex + iii * (4 + ST_MAX_CONFIDENCE_USERS*SIZE_OF_ST_USER_LEVEL);
                //buffer[pos] = (byte) (cnnKeyphraseLevel & 0xff);
                fillInt(buffer, pos, secondKeyphraseLevel);

                // fill the user count
                pos = pos + 4;
                fillInt(buffer, pos, userCount);

                // fill the iii user level
                for (int jjj = 0; jjj < userCount; jjj++) {
                    pos = pos + 4 + jjj * SIZE_OF_ST_USER_LEVEL;

                    // fill user id
                    fillInt(buffer, pos, jjj);

                    // fill user level
                    pos = pos + 4;
                    fillInt(buffer, pos, secondUserLevel);
                }
            }
        }
    }

    //this is a MFCN model, which only include first stage model with 5 kw in it, no usr level
    //thus, we only have to fill the keyphrase count and keyphrase level
    private void fillConfidenceLevelParam(byte[] buffer, int startPos,
                                          int[] firstKeyphraseLevels) {
        // fill the key id
        int keyId = ST_Param_Key.ST_PARAM_KEY_CONFIDENCE_LEVELS.ordinal();
        int startIndex = startPos;
        fillInt(buffer, startIndex, keyId);

        // fill the payload size
        int payloadSize = SIZE_OF_ST_CONFIDENCE_LEVELS_INFO;
        startIndex = startPos + 4;
        fillInt(buffer, startIndex, payloadSize);

        // fill the version
        int version = 0x02;
        startIndex = startPos + 8;
        fillInt(buffer, startIndex, version);

        // fill the sound model count, for MFCN model, only has 1st stage
        int modelCount = 1;//mNumOfModels;
        startIndex = startPos + 12;
        fillInt(buffer, startIndex, modelCount);

        // fill sound model GMM confidence level

        int gmmSmId = ST_SM_ID_SVA_FIRST_STAGE;
        startIndex = startPos + 16;
        fillInt(buffer, startIndex, gmmSmId);

        // fill the keyphrase count
        int keyphraseCount = firstKeyphraseLevels.length;
        LogUtils.d(TAG, "fillConfidenceLevelParam: GMM keyphraseCount = "
                + keyphraseCount);

        startIndex = startPos + 20;
        fillInt(buffer, startIndex, keyphraseCount);

        startIndex = startPos + 24;
        for (int ii = 0; ii < keyphraseCount; ii++) {
            // fill the ii keyphrase level
            /*
            struct __attribute__((__packed__)) st_keyword_levels_v2
            {
                int32_t kw_level;
                uint32_t num_user_levels;
                struct st_user_levels_v2 user_levels[ST_MAX_USERS];
            };
            */
            int pos = startIndex + ii * (4 + 4 +  ST_MAX_CONFIDENCE_USERS * SIZE_OF_ST_USER_LEVEL);
            //buffer[pos] = (byte) (gmmKeyphraseLevel & 0xff);
            LogUtils.d(TAG, "fillConfidenceLevelParam: " +
                    "GMM firstKeyphraseLevels["+ii+"] = " + firstKeyphraseLevels[ii] +" pos  ="+ pos);
            fillInt(buffer, pos, firstKeyphraseLevels[ii]);
        }

    }

    private void fillInt(byte[] buffer, final int startPos, int value) {
        int startIndex = startPos;
        buffer[startIndex] = (byte) (value & 0xff);
        buffer[++startIndex] = (byte) (value >> 8 & 0xff);
        buffer[++startIndex] = (byte) (value >> 16 & 0xff);
        buffer[++startIndex] = (byte) (value >> 24 & 0xff);
    }

    private void fillLong(byte[] buffer, final int startPos, long value) {
        int startIndex = startPos;
        buffer[startIndex] = (byte) (value & 0xff);
        buffer[++startIndex] = (byte) (value >> 8 & 0xff);
        buffer[++startIndex] = (byte) (value >> 16 & 0xff);
        buffer[++startIndex] = (byte) (value >> 24 & 0xff);
        buffer[++startIndex] = (byte) (value >> 32 & 0xff);
        buffer[++startIndex] = (byte) (value >> 40 & 0xff);
        buffer[++startIndex] = (byte) (value >> 48 & 0xff);
        buffer[++startIndex] = (byte) (value >> 56 & 0xff);
    }

    private ListenTypes.SVASoundModelInfo query(String smFullName) {
        LogUtils.d(TAG, "query: smFullName = " + smFullName);
        if (null == smFullName) {
            LogUtils.d(TAG, "query: invalid input param");
            return null;
        }

        String filePath = Global.PATH_ROOT + "/" + smFullName;
        if (FileUtils.isExist(filePath)) {
            try {
                ByteBuffer smBuffer;
                smBuffer = FileUtils.readFileToByteBuffer(filePath);
                return (ListenTypes.SVASoundModelInfo) ListenSoundModel.query(smBuffer);
            } catch (IOException e) {
                LogUtils.d(TAG, "query: file IO exception");
                e.printStackTrace();
                return null;
            }
        } else {
            LogUtils.d(TAG, "query: error file not exists");
            return null;
        }
    }

    enum ST_Param_Key {
        ST_PARAM_KEY_CONFIDENCE_LEVELS,
        ST_PARAM_KEY_HISTORY_BUFFER_CONFIG,
        ST_PARAM_KEY_KEYWORD_INDICES,
        ST_PARAM_KEY_TIMESTAMP,
    }

    private static class ConfidenceLevelInfo {
        private int mVersion;
        private List<ConfidenceLevel> mConfidenceLevels = new ArrayList<>();
    }

    private static class ConfidenceLevel {
        private int mModeId;
        private List<KeywordLevel> mKeywordLevels = new ArrayList<>();
    }

    private static class KeywordLevel {
        private int mLevel;
        private List<UserLevel> mUserLevels = new ArrayList<>();
    }

    private static class UserLevel {
        private int mUserId;
        private int mLevel;
    }

    public static class DetectionConfLevel {
        private int mFKeywordLevel;
        private int mFUserLevel;
        private int mSKeywordLevel;
        private int mSUserLevel;
        private int mFKeywordID;

        public int getFKeywordLevel() {
            return mFKeywordLevel;
        }

        public int getFKeywordID() {
            return mFKeywordID;
        }

        public int getFUserLevel() {
            return mFUserLevel;
        }

        public int getSKeywordLevel() {return mSKeywordLevel;}

        public int getSUserLevel() {return mSUserLevel;}

        public String toString() {
            return "DetectionConfLevel{1st_kw_id=" + mFKeywordID +
                                    "1st_kw=" + mFKeywordLevel +
                                    ",1st_user=" + mFUserLevel +
                                    ",2nd_kw=" + mSKeywordLevel +
                                    ",2nd_user=" + mSUserLevel + "}";
        }
    }

    public static DetectionConfLevel parse(byte[] opaqueData, int[] firstKeyphraseLevels) {
        LogUtils.d(TAG, "parse opaqueData.length = " + opaqueData.length);
        StringBuilder sb = new StringBuilder();
        for (int start = 0; start < opaqueData.length; start += 4) {
            sb.append(byte2Int(opaqueData, start)).append(",");
        }
        LogUtils.d(TAG, "parse opaqueData = " + sb.toString());
        if (opaqueData.length == 1) return null;
        ConfidenceLevelInfo confidenceLevelInfo = new ConfidenceLevelInfo();
        int index = 0;
        while (index < opaqueData.length) {
            if (index + 8 >= opaqueData.length) break;

            int tag = byte2Int(opaqueData, index);
            index += 4;
            int payloadSize = byte2Int(opaqueData, index);
            index += 4;

            LogUtils.d(TAG, "tag = " + tag);
            LogUtils.d(TAG, "payloadSize = " + payloadSize);
            if (tag == ST_Param_Key.ST_PARAM_KEY_KEYWORD_INDICES.ordinal()
                    || tag == ST_Param_Key.ST_PARAM_KEY_HISTORY_BUFFER_CONFIG.ordinal()
                    || tag == ST_Param_Key.ST_PARAM_KEY_TIMESTAMP.ordinal()) {
                index += payloadSize;
            } else if(tag == ST_Param_Key.ST_PARAM_KEY_CONFIDENCE_LEVELS.ordinal()) {
                LogUtils.d(TAG, "ST_PARAM_KEY_CONFIDENCE_LEVELS = " + tag);
                if (index + payloadSize >= opaqueData.length) break;
                int localIndex = index;
                int version = byte2Int(opaqueData, localIndex);
                localIndex += 4;
                int confidenceLevelsSize = byte2Int(opaqueData, localIndex);
                localIndex += 4;
                LogUtils.d(TAG, "version = " + version + "," +
                                        "confidenceLevelsSize=" + confidenceLevelsSize);
                for (int i = 0; i < ST_MAX_SOUND_MODELS; i++) {
                    ConfidenceLevel confidenceLevel = new ConfidenceLevel();
                    confidenceLevel.mModeId = byte2Int(opaqueData, localIndex);
                    localIndex += 4;
                    int keywordLevelsSize = byte2Int(opaqueData, localIndex);
                    localIndex += 4;
                    LogUtils.d(TAG, "confidenceLevel.mModeId = " + confidenceLevel.mModeId
                            + "," + "keywordLevelsSize=" + keywordLevelsSize);
                    for (int j = 0; j < ST_MAX_CONFIDENCE_KEYWORDS; j++) {
                        KeywordLevel keywordLevel = new KeywordLevel();
                        keywordLevel.mLevel = byte2Int(opaqueData, localIndex);
                        localIndex += 4;
                        int userLevelsSize = byte2Int(opaqueData, localIndex);
                        localIndex += 4;
                        for (int k = 0; k < ST_MAX_CONFIDENCE_USERS; k++) {
                            UserLevel userLevel = new UserLevel();
                            userLevel.mUserId = byte2Int(opaqueData, localIndex);
                            localIndex += 4;
                            userLevel.mLevel = byte2Int(opaqueData, localIndex);
                            localIndex += 4;
                            keywordLevel.mUserLevels.add(userLevel);
                        }
                        confidenceLevel.mKeywordLevels.add(keywordLevel);
                    }
                    confidenceLevelInfo.mConfidenceLevels.add(confidenceLevel);
                }
                index += payloadSize;
            } else if (tag == ST_PARAM_KEY_KEYWORD_BUFFER) {
                LogUtils.d(TAG, "ST_PARAM_KEY_KEYWORD_BUFFER = " + tag);
                if (index + payloadSize >= opaqueData.length) break;
                byte[] buffer = new byte[payloadSize];
                for (int i = 0; i < payloadSize; i++) {
                    buffer[i] = opaqueData[i + index];
                }
                index += payloadSize;
                saveRecordingFile(buffer, payloadSize);
            } else if (tag == ST_PARAM_KEY_SSTAGE_KW_ENGINE_INFO
                    || tag == ST_PARAM_KEY_SSTAGE_UV_ENGINE_INFO) {
                if (index + payloadSize >= opaqueData.length) break;
                StringBuilder stringBuilder = new StringBuilder();
                if (tag == ST_PARAM_KEY_SSTAGE_KW_ENGINE_INFO) {
                    stringBuilder.append("ST_PARAM_KEY_SSTAGE_KW_ENGINE_INFO ");
                } else {
                    stringBuilder.append("ST_PARAM_KEY_SSTAGE_UV_ENGINE_INFO ");
                }
                int localIndex = index;
                int version = byte2Int(opaqueData, localIndex);
                stringBuilder.append("version = " + version);
                localIndex += 4;
                int detectionState = byte2Int(opaqueData, localIndex);
                stringBuilder.append(", detection_state = " + detectionState);
                localIndex += 4;
                int processedLength = byte2Int(opaqueData, localIndex);
                stringBuilder.append(", processed_length = " + processedLength);
                localIndex += 4;
                int totalProcessDuration = byte2Int(opaqueData, localIndex);
                stringBuilder.append(", total_process_duration = " + totalProcessDuration);
                localIndex += 4;
                int totalCapiProcessDuration = byte2Int(opaqueData, localIndex);
                stringBuilder.append(", total_capi_process_duration = "
                        + totalCapiProcessDuration);
                localIndex += 4;
                int totalCapiGetParamDuration = byte2Int(opaqueData, localIndex);
                stringBuilder.append(", total_capi_get_param_duration = "
                        + totalCapiGetParamDuration);
                LogUtils.d(TAG, stringBuilder.toString());
                index += payloadSize;
            } else if (tag == ST_PARAM_KEY_IS_BARGEIN) {
                LogUtils.d(TAG, "ST_PARAM_KEY_IS_BARGEIN version = " + byte2Int(opaqueData, index)
                        + ", mode = " + byte2Int(opaqueData, index + 4));
                index += payloadSize;
            }
        }
        return convert2ConfLevel(confidenceLevelInfo, firstKeyphraseLevels);
    }

    private static void saveRecordingFile(byte[] buffer, int bufferSize) {
        String time = String.valueOf(System.currentTimeMillis());
        FileUtils.createDirIfNotExists(Global.PATH_SECOND_STAGE_RECORD);
        String filePath = Global.PATH_SECOND_STAGE_RECORD + "/" + time + Global.SUFFIX_WAV_FILE;
        LogUtils.d(TAG, "saveRecordingFile filePath = " + filePath);
        Global.getInstance().getRecordingsMgr().writeBufferToWavFile(
                buffer, bufferSize, filePath, false);
    }
    private static DetectionConfLevel convert2ConfLevel(ConfidenceLevelInfo confidenceLevelInfo,
                                                        int[] firstKeyphraseLevels) {
        DetectionConfLevel detectionConfLevel = new DetectionConfLevel();
        for (ConfidenceLevel confidenceLevel : confidenceLevelInfo.mConfidenceLevels) {
            int modeId = confidenceLevel.mModeId;
            LogUtils.d(TAG, "modeId = " + modeId);
            if (ST_SM_ID_SVA_FIRST_STAGE == modeId) {
                if (!confidenceLevel.mKeywordLevels.isEmpty()) {
                    if(firstKeyphraseLevels != null) {
                        for (int i = 0; i < firstKeyphraseLevels.length; i++) {
                            KeywordLevel keywordLevel = confidenceLevel.mKeywordLevels.get(i);
                            LogUtils.d(TAG,
                                    "fStage keyword level[" + i + "] = "
                                            + keywordLevel.mLevel);
                            if(keywordLevel.mLevel > 0/*firstKeyphraseLevels[i]*/){
                                detectionConfLevel.mFKeywordID = i;
                                LogUtils.d(TAG, "fStage keyword ID = " + i);
                                detectionConfLevel.mFKeywordLevel = keywordLevel.mLevel;
                                if (!keywordLevel.mUserLevels.isEmpty()) {
                                    UserLevel userLevel = keywordLevel.mUserLevels.get(0);
                                    LogUtils.d(TAG, "fStage user level = "
                                            + userLevel.mLevel);
                                    detectionConfLevel.mFUserLevel = userLevel.mLevel;
                                } else {
                                    LogUtils.e(TAG, "fStage userLevels is empty");
                                }
                                break;
                            }
                        }
                    }else{
                        KeywordLevel keywordLevel = confidenceLevel.mKeywordLevels.get(0);
                        LogUtils.d(TAG,
                                "fStage keyword level = " + keywordLevel.mLevel);
                        detectionConfLevel.mFKeywordLevel = keywordLevel.mLevel;
                        if (!keywordLevel.mUserLevels.isEmpty()) {
                            UserLevel userLevel = keywordLevel.mUserLevels.get(0);
                            LogUtils.d(TAG, "fStage user level = " + userLevel.mLevel);
                            detectionConfLevel.mFUserLevel = userLevel.mLevel;
                        } else {
                            LogUtils.e(TAG, "fStage userLevels is empty");
                        }
                    }

                    LogUtils.d(TAG, "fStage keyword level = "
                            + detectionConfLevel.mFKeywordLevel
                            + " user level = " + detectionConfLevel.mFUserLevel);
                } else {
                    LogUtils.e(TAG, "fStage keywordLevels is empty");
                }

            } else if (ST_SM_ID_SVA_S_STAGE_KEYPHRASE == modeId
                        || ST_SM_ID_SVA_UDK7_S_STAGE_KEYPHRASE == modeId) {
                if (!confidenceLevel.mKeywordLevels.isEmpty()) {
                    KeywordLevel keywordLevel = confidenceLevel.mKeywordLevels.get(0);
                    detectionConfLevel.mSKeywordLevel = keywordLevel.mLevel;
                    LogUtils.d(TAG, "sStage keyword level = " + detectionConfLevel.mSKeywordLevel);
                } else {
                    LogUtils.e(TAG, "sStage keywordLevels is empty");
                }
            } else if (ST_SM_ID_SVA_S_STAGE_USER == modeId) {
                if (!confidenceLevel.mKeywordLevels.isEmpty()) {
                    KeywordLevel keywordLevel = confidenceLevel.mKeywordLevels.get(0);
                    if (!keywordLevel.mUserLevels.isEmpty()) {
                        UserLevel userLevel = keywordLevel.mUserLevels.get(0);
                        detectionConfLevel.mSUserLevel = userLevel.mLevel;
                        LogUtils.d(TAG, "sStage user level = " + detectionConfLevel.mSUserLevel);
                    } else {
                        LogUtils.e(TAG, "sStage userLevels is empty");
                    }
                } else {
                    LogUtils.e(TAG, "sStage keywordLevels is empty");
                }
            }
        }
        return detectionConfLevel;
    }

    private static int byte2Int(byte[] bytes, int start) {
        if (start + 4 > bytes.length) {
            LogUtils.d("byte2Int start + 4 out of array");
            return 0;
        }
        return (bytes[start++] & 0xff)
                | (bytes[start++] & 0xff) << 8
                | (bytes[start++] & 0xff) << 16
                | (bytes[start] & 0xff) << 24;
    }
}
