/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.controller;

import android.os.Bundle;

import com.qualcomm.listen.ListenSoundModel;

import com.qualcomm.qti.sva.utils.LogUtils;
import com.qualcomm.qti.sva.utils.Utils;
import com.qualcomm.qti.sva.xmlhelper.Container;
import com.qualcomm.qti.sva.xmlhelper.Container.ADAConfigurationKey;
import com.qualcomm.qti.sva.xmlhelper.Container.ADAKWConfigurationKey;
import com.qualcomm.qti.sva.xmlhelper.Container.ADAUserConfigurationKey;
import com.qualcomm.qti.sva.xmlhelper.Container.EPDConfigurationKey;
import com.qualcomm.qti.sva.xmlhelper.Container.SMLPDKConfigurationKey;
import com.qualcomm.qti.sva.xmlhelper.Container.SMLUDKConfigurationKey;
import com.qualcomm.qti.sva.xmlhelper.Container.ONLINEEPDConfigurationKey;

import com.qualcomm.qti.sva.xmlhelper.ContainerGroup;
import com.qualcomm.qti.sva.xmlhelper.XMLHelper;

import java.nio.ByteBuffer;

public class SMLParametersManager {

    public static final int RECORDING_IN_CLEAN_ENVIRONMENT = 0;
    public static final int RECORDING_IN_NOISY_ENVIRONMENT = 1;
    public static final int DEFAULT_CLEAN_RECORDING_TIMES = 5;

    private static final String SML_PDK_PARAMS_TAG = "sml_pdk";
    private static final String SML_UDK_PARAMS_TAG = "sml_udk";
    private static final String SML_EPD_PARAMS_TAG = "sml_onlineEPD";

    public static final int SVA_KEYWORD_TYPE_PDK = 2;
    private static final int SVA_KEYWORD_TYPE_UDK = 3;

    private static final int CAR_NOISE_INDEX = 0;
    private static final int MUSIC_NOISE_INDEX = 1;
    private static final int PARTY_NOISE_INDEX = 2;
    private static final int RADIO_NOISE_INDEX = 3;

    private final int NUBER_OF_LISTEN_EPD_PARAMS = 16;
    private final int SIZE_OF_LISTEN_EPD_PARAMS_RESERVED = 4;
    private final int SIZE_OF_LISTEN_EPD_PARAMS = 4 * (NUBER_OF_LISTEN_EPD_PARAMS
            + SIZE_OF_LISTEN_EPD_PARAMS_RESERVED);

    private static final int MEMORY_ALIGNMENT_SIZE = 2;
    private static final int ADA_NUM_MAX_SETTING = 10;
    private final int SIZE_OF_ADA_PARAMS_RESERVED = 20;
    private final int SIZE_OF_ADA_PARAMS = 26 + MEMORY_ALIGNMENT_SIZE + 4 * ADA_NUM_MAX_SETTING
            + 4 * ADA_NUM_MAX_SETTING * ADA_NUM_MAX_SETTING + 4 * SIZE_OF_ADA_PARAMS_RESERVED;

    private final int SIZE_OF_SML_PDK_PARAMS_RESERVED = 19;
    private final int NUMBER_OF_SML_PDK_PARAMS = 14;
    private final int SIZE_OF_SML_PDK_PARAMS = SIZE_OF_LISTEN_EPD_PARAMS + SIZE_OF_ADA_PARAMS
            + (NUMBER_OF_SML_PDK_PARAMS + SIZE_OF_SML_PDK_PARAMS_RESERVED) * 4;

    private final int SIZE_OF_SML_UDK_PARAMS_RESERVED = 20;
    private final int NUMBER_OF_SML_UDK_PARAMS = 7;
    private final int NUMBER_OF_SML_UDK7_PARAMS = 9;
    private final int SIZE_OF_TEXT_INPUT = 200;
    private final int SIZE_OF_SML_UDK_PARAMS = 2 * SIZE_OF_ADA_PARAMS + SIZE_OF_LISTEN_EPD_PARAMS
            + (NUMBER_OF_SML_UDK_PARAMS + SIZE_OF_SML_UDK_PARAMS_RESERVED) * 4;
    private final int SIZE_OF_SML_UDK7_PARAMS = 2 * SIZE_OF_ADA_PARAMS + SIZE_OF_LISTEN_EPD_PARAMS
            + (NUMBER_OF_SML_UDK7_PARAMS + SIZE_OF_SML_UDK_PARAMS_RESERVED) * 4 + SIZE_OF_TEXT_INPUT;

    private static final String TAG = SMLParametersManager.class.getSimpleName();
    private static final int DEFAULT_NOISY_RECORDING_TIMES = 3;
    private static SMLParametersManager sInstance;

    private ContainerGroup mSMLConfig;

    private SMLParametersManager() {
        String smlConfigPath = Global.PATH_ROOT + "/" + Global.NAME_SML_CONFIGURATIONS;
        XMLHelper xmlHelper = new XMLHelper(smlConfigPath);
        mSMLConfig = xmlHelper.read();
        LogUtils.d(TAG, "SMLParametersManager mSMLConfig = " + mSMLConfig);
    }

    public static SMLParametersManager getInstance() {
        if (sInstance == null) {
            sInstance = new SMLParametersManager();
        }
        return sInstance;
    }

    public void setSMLPDKParameters() {
        Container pdkConfig = mSMLConfig.getContainer(SML_PDK_PARAMS_TAG);
        if (pdkConfig != null) {
            byte[] payload = fillSMLPDKParams(pdkConfig);
            setSMLParams(SVA_KEYWORD_TYPE_PDK, payload);
        } else {
            LogUtils.e(TAG, "SMLParametersManager fails due to pdk config is null");
        }

    }

    public void setSMLUDKParameters(String textinput) {
        Container udkConfig = mSMLConfig.getContainer(SML_UDK_PARAMS_TAG);
        if (udkConfig != null) {
            byte[] payload = fillSMLUDKParams(udkConfig, textinput);
            setSMLParams(SVA_KEYWORD_TYPE_UDK, payload);
        } else {
            LogUtils.e(TAG, "SMLParametersManager fails due to udk config is null");
        }
    }

    private void setSMLParams(int keywordType, byte[] payload) {
        ByteBuffer tuningPayload = ByteBuffer.allocateDirect(payload.length);
        int result = ListenSoundModel.SetSoundModelTuningParams(keywordType,
                tuningPayload.put(payload));
        LogUtils.d(TAG, "SetSoundModelTuningParams result = " + result);
    }

    public int setSMLParamsForTextInput(String textInput, ByteBuffer langModel) {
        Container udkConfig = mSMLConfig.getContainer(SML_UDK_PARAMS_TAG);
        if (udkConfig != null) {
            byte[] payload = fillSMLUDKParams(udkConfig, textInput);
            ByteBuffer tuningPayload = ByteBuffer.allocateDirect(payload.length);
            int result = ListenSoundModel.SetSoundModelTuningParams(langModel, SVA_KEYWORD_TYPE_UDK,
                    tuningPayload.put(payload));
            LogUtils.d(TAG, "setSMLParamsForTextInput result = " + result);
            return result;
        } else {
            LogUtils.e(TAG, "setSMLParamsForTextInput fails due to udk config is null");
            return -1;
        }
    }
    public int getRecordingTimes(int recordingType) {
        if (recordingType == RECORDING_IN_CLEAN_ENVIRONMENT) {
            return DEFAULT_CLEAN_RECORDING_TIMES;
        } else if (recordingType == RECORDING_IN_NOISY_ENVIRONMENT) {
            return DEFAULT_NOISY_RECORDING_TIMES;
        } else {
            return DEFAULT_CLEAN_RECORDING_TIMES;
        }
    }

    public Bundle getOnlineEDPParams() {
        Container epdContainer = mSMLConfig.getContainer(SML_EPD_PARAMS_TAG);
        if (epdContainer != null) {
            Bundle epdParams = new Bundle();
            for (ONLINEEPDConfigurationKey key : ONLINEEPDConfigurationKey.values()) {
                switch (key.getType()) {
                    case INTEGER:
                        epdParams.putInt(key.getValue(),
                                epdContainer.getInt(key.getValue()));
                        break;
                    case FLOAT:
                        epdParams.putFloat(key.getValue(),
                                epdContainer.getFloat(key.getValue()));
                        break;
                    case BOOLEAN:
                        epdParams.putBoolean(key.getValue(),
                                epdContainer.getBoolean(key.getValue()));
                        break;
                    case STRING:
                        epdParams.putString(key.getValue(),
                                epdContainer.getString(key.getValue()));
                        break;
                    default:
                }
            }
            return epdParams;
        }
        return Bundle.EMPTY;
    }

    private byte[] fillSMLUDKParams(Container container,String textinput) {
        byte[] smlUDKParams =
                new byte[Utils.isSupportUDK7() ? SIZE_OF_SML_UDK7_PARAMS : SIZE_OF_SML_UDK_PARAMS];
        int startPos = 0;
        //fill numGuardFramesForFstageUV
        fillInt(smlUDKParams, startPos, container.getInt(
                SMLUDKConfigurationKey.TRAINING_NUMGUARDFRAMES_FOR_FIRST_STAGE_UV));
        startPos += 4;
        //fill numGuardFramesForSstageUV
        fillInt(smlUDKParams, startPos, container.getInt(
                SMLUDKConfigurationKey.TRAINING_NUMGUARDFRAMES_FOR_SECOND_STAGE_UV));
        startPos += 4;
        //fill minPhonemeLength
        fillInt(smlUDKParams, startPos, container.getInt(
                SMLUDKConfigurationKey.TRAINING_MIN_PHONEME_LENGTH));
        startPos += 4;
        //fill maxPhonemeLength
        fillInt(smlUDKParams, startPos, container.getInt(
                SMLUDKConfigurationKey.TRAINING_MAX_PHONEME_LENGTH));
        startPos += 4;
        //fill doPcmNormalization
        fillInt(smlUDKParams, startPos, container.getInt(
                SMLUDKConfigurationKey.TRAINING_PCM_NORMALIZATION));
        startPos += 4;
        //fill epdParams
        fillEpdParams(smlUDKParams, startPos, container);
        startPos += SIZE_OF_LISTEN_EPD_PARAMS;
        //fill enableAdaForKW
        boolean enableAdaForKW = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE);
        fillInt(smlUDKParams, startPos, enableAdaForKW ? 1 : 0);
        startPos += 4;
        //fill adaParamForKW
        fillAdaParamsforKW(smlUDKParams, startPos, container);
        startPos += SIZE_OF_ADA_PARAMS;
        //fill enableAdaForUser
        boolean enableAdaForUser = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE);
        fillInt(smlUDKParams, startPos, enableAdaForUser ? 1 : 0);
        startPos += 4;
        //fill adaParamForUser
        fillAdaParamsforUser(smlUDKParams, startPos, container);
        startPos += SIZE_OF_ADA_PARAMS;

        if(Utils.isSupportUDK7()) {
            //fill numPreGuardFramesForSstageUDK
            fillInt(smlUDKParams, startPos, container.getInt(
                    SMLUDKConfigurationKey.TRAINING_NUMPREGUARDFRAMES_FOR_SECOND_STAGE_UDK));
            startPos += 4;
            //fill numPostGuardFramesForSstageUDK
            fillInt(smlUDKParams, startPos, container.getInt(
                    SMLUDKConfigurationKey.TRAINING_NUMPOSTGUARDFRAMES_FOR_SECOND_STAGE_UDK));
            startPos += 4;
            //fill text input
            if (textinput != null) {
                byte[] bytes = textinput.getBytes();
                System.arraycopy(bytes, 0, smlUDKParams, startPos, bytes.length);
            }
            startPos += 200;
        }
        if(Utils.isSupportUV72()){
            //fill bEnableEPDForUv72
            boolean bEnableEPDForUv72 = container.getBoolean(
                    SMLPDKConfigurationKey.TRAINING_B_ENABLE_EPD_FOR_UV72);
            fillInt(smlUDKParams, startPos, bEnableEPDForUv72 ? 1 : 0);
            startPos += 4;
        }

        if(Utils.isSMLVersionMoreThan9()){
            //fill qualityCheckThreshold
            fillInt(smlUDKParams, startPos, container.getInt(
                    SMLUDKConfigurationKey.TRAINING_QUALITY_CHECK_THRESHOLD));
            startPos += 4;
        }
        // reserved array
        return smlUDKParams;
    }

    private byte[] fillSMLPDKParams(Container container) {
        byte[] smlPDKParams = new byte[SIZE_OF_SML_PDK_PARAMS];
        int startPos = 0;
        //fill numGuardFramesForEPD
        fillInt(smlPDKParams, startPos, container.getInt(
                SMLPDKConfigurationKey.TRAINING_NUMGUARDFRAMES_FOR_EPD));
        startPos += 4;
        //fill enableFstageEpd
        boolean enableFstageEpd = container.getBoolean(
                SMLPDKConfigurationKey.TRAINING_ENABLE_FIRST_STAGE_EPD);
        fillInt(smlPDKParams, startPos, enableFstageEpd ? 1 : 0);
        startPos += 4;
        //fill numGuardFramesForFStageEPD
        fillInt(smlPDKParams, startPos, container.getInt(
                SMLPDKConfigurationKey.TRAINING_NUMGUARDFRAMES_FOR_FIRST_STAGE_EPD));
        startPos += 4;
        //fill checkChopped
        boolean checkChopped = container.getBoolean(SMLPDKConfigurationKey.TRAINING_CHECK_CHOPPED);
        fillInt(smlPDKParams, startPos, checkChopped ? 1 : 0);
        startPos += 4;
        //fill choppingFrameThreshold
        fillInt(smlPDKParams, startPos, container.getInt(
                SMLPDKConfigurationKey.TRAINING_CHOPPING_FRAME_THRESHOLD));
        startPos += 4;
        //fill snrThresholdForNoisySamples
        fillFloat(smlPDKParams, startPos, container.getFloat(
                SMLPDKConfigurationKey.TRAINING_SNR_THRESHOLD_FOR_NOISY_SAMPLES));
        startPos += 4;
        //fill doPcmNormalization
        boolean doPcmNormalization = container.getBoolean(
                SMLPDKConfigurationKey.TRAINING_PCM_NORMALIZATION);
        fillInt(smlPDKParams, startPos, doPcmNormalization ? 1 : 0);
        startPos += 4;
        //fill epdParams
        fillEpdParams(smlPDKParams, startPos, container);
        startPos += SIZE_OF_LISTEN_EPD_PARAMS;
        //fill enableAda
        boolean enableAda = container.getBoolean(ADAConfigurationKey.ADA_ENABLE);
        fillInt(smlPDKParams, startPos, enableAda ? 1 : 0);
        startPos += 4;
        //fill nCleanRecordings
        fillInt(smlPDKParams, startPos, container.getInt(
                SMLPDKConfigurationKey.TRAINING_NUM_CLEAR_RECORDINGS));
        startPos += 4;
        //fill adaParam
        fillAdaParams(smlPDKParams, startPos, container);
        startPos += SIZE_OF_ADA_PARAMS;
        //fill checkClipped
        boolean checkClipped = container.getBoolean(SMLPDKConfigurationKey.TRAINING_CHECK_CLIPPED);
        fillInt(smlPDKParams, startPos, checkClipped ? 1 : 0);
        startPos += 4;
        //fill nClippingThreshold
        fillInt(smlPDKParams, startPos, container.getInt(
                SMLPDKConfigurationKey.TRAINING_NUM_CLIPPING_THRESHOLD));
        startPos += 4;
        //fill clippingRatioThreshold
        fillFloat(smlPDKParams, startPos, container.getFloat(
                SMLPDKConfigurationKey.TRAINING_CLIPPING_RATIO_THRESHOLD));
        startPos += 4;
        //fill enableZeroProcessingForPdkXS
        boolean enableZeroProcessingForPdkXS = container.getBoolean(
                SMLPDKConfigurationKey.TRAINING_ENABLE_ZERO_PROCESSING_FOR_PDK_XS);
        fillInt(smlPDKParams, startPos, enableZeroProcessingForPdkXS ? 1 : 0);
        startPos += 4;
        //fill nZerosPaddingFrames
        fillInt(smlPDKParams, startPos, container.getInt(
                SMLPDKConfigurationKey.TRAINING_NUM_ZEROS_PADDING_FRAMES));
        startPos += 4;
        if(Utils.isSupportUV72()){
            //fill bEnableEPDForUv72
            boolean bEnableEPDForUv72 = container.getBoolean(
                    SMLPDKConfigurationKey.TRAINING_B_ENABLE_EPD_FOR_UV72);
            fillInt(smlPDKParams, startPos, bEnableEPDForUv72 ? 1 : 0);
            startPos += 4;
        }

        // reserved array
        return smlPDKParams;
    }

    private void fillEpdParams(byte[] buffer, final int startPos, Container container) {
        int startIndex = startPos;
        //fill minSnrOnset
        fillFloat(buffer, startIndex, container.getFloat(EPDConfigurationKey.EPD_MIN_SNR_ONSET));
        startIndex += 4;
        //fill minSnrLeave
        fillFloat(buffer, startIndex, container.getFloat(EPDConfigurationKey.EPD_MIN_SNR_LEAVE));
        startIndex += 4;
        //fill snrFloor
        fillFloat(buffer, startIndex, container.getFloat(EPDConfigurationKey.EPD_SNR_FLOOR));
        startIndex += 4;
        //fill snrThresholds
        fillFloat(buffer, startIndex, container.getFloat(EPDConfigurationKey.EPD_SNR_THRESHOLDS));
        startIndex += 4;
        //fill forgettingFactorNoise
        fillFloat(buffer, startIndex, container.getFloat(
                EPDConfigurationKey.EPD_FORGETTING_FACTOR_NOISE));
        startIndex += 4;
        //fill numFrameTransientFrame
        fillInt(buffer, startIndex, container.getInt(
                EPDConfigurationKey.EPD_NUM_FRAME_TRANSIENT_FRAME));
        startIndex += 4;
        //fill minEnergyFrameRatio
        fillFloat(buffer, startIndex, container.getFloat(
                EPDConfigurationKey.EPD_MIN_ENERGY_FRAME_RATIO));
        startIndex += 4;
        //fill minNoiseEnergy
        fillFloat(buffer, startIndex, container.getFloat(EPDConfigurationKey.EPD_MIN_NOISE_ENERGY));
        startIndex += 4;
        //fill numMinFramesInPhrase
        fillInt(buffer, startIndex, container.getInt(
                EPDConfigurationKey.EPD_NUM_MIN_FRAMES_INPHRASE));
        startIndex += 4;
        //fill numMinFramesInSpeech
        fillInt(buffer, startIndex, container.getInt(
                EPDConfigurationKey.EPD_NUM_MIN_FRAMES_INSPEECH));
        startIndex += 4;
        //fill numMaxFrameInSpeechGap
        fillInt(buffer, startIndex, container.getInt(
                EPDConfigurationKey.EPD_NUM_MIN_FRAMES_INSPEECH_GAP));
        startIndex += 4;
        //fill numFramesInHead
        fillInt(buffer, startIndex, container.getInt(EPDConfigurationKey.EPD_NUM_FRAMES_INHEAD));
        startIndex += 4;
        //fill numFramesInTail
        fillInt(buffer, startIndex, container.getInt(EPDConfigurationKey.EPD_NUM_FRAMES_INTAIL));
        startIndex += 4;

        //fill preEmphasize
        fillInt(buffer, startIndex, container.getInt(EPDConfigurationKey.EPD_PRE_EMPHASIZING));
        startIndex += 4;
        //fill numMaxFrames
        fillInt(buffer, startIndex, container.getInt(EPDConfigurationKey.EPD_NUM_MAX_FRAMES));
        startIndex += 4;
        //fill keyword_threshold
        fillInt(buffer, startIndex, container.getInt(EPDConfigurationKey.EPD_KEYWORD_THRESHOLD));
        startIndex += 4;

        // reserved array
    }

    private void fillAdaParams(byte[] buffer, final int startPos, Container container) {
        int startIndex = startPos;
        //fill support noises
        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            boolean enableNoise = false;
            if (i == CAR_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAConfigurationKey.ADA_ENABLE_CAR_NOISE);
            } else if (i == MUSIC_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAConfigurationKey.ADA_ENABLE_MUSIC_NOISE);
            } else if (i == PARTY_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAConfigurationKey.ADA_ENABLE_PARTY_NOISE);
            } else if (i == RADIO_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAConfigurationKey.ADA_ENABLE_RADIO_NOISE);
            }
            fillInt8(buffer, startIndex++, enableNoise ? 1 : 0);
        }
        //memory alignment
        startIndex += 2;

        //fill SNR settings
        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            int numSnr = 0;
            if (i == CAR_NOISE_INDEX || i == MUSIC_NOISE_INDEX
                    || i == PARTY_NOISE_INDEX || i == RADIO_NOISE_INDEX) {
                numSnr = 2;
            }
            fillInt(buffer, startIndex, numSnr);
            startIndex += 4;
        }

        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            int[] noiseArray = null;
            if (i == CAR_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAConfigurationKey.ADA_CAR_NOISE));
            } else if (i == MUSIC_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAConfigurationKey.ADA_MUSIC_NOISE));
            } else if (i == PARTY_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAConfigurationKey.ADA_PARTY_NOISE));
            } else if (i == RADIO_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAConfigurationKey.ADA_RADIO_NOISE));
            }
            if (noiseArray != null && noiseArray.length > 0) {
                for (int j = 0; j < noiseArray.length; j++) {
                    fillInt(buffer, startIndex, noiseArray[j]);
                    startIndex += 4;
                }
                startIndex += 4 * (ADA_NUM_MAX_SETTING - noiseArray.length);
            } else {
                startIndex += 4 * ADA_NUM_MAX_SETTING;
            }

        }

        //fill numCleanRepeation
        fillInt(buffer, startIndex, container.getInt(ADAConfigurationKey.ADA_NUM_CLEAN_REPETITION));
        startIndex += 4;

        //fill enablePitch
        boolean enablePitch = container.getBoolean(ADAConfigurationKey.ADA_ENABLE_PITCH);
        fillInt8(buffer, startIndex++, enablePitch ? 1 : 0);

        //fill enableTempo
        boolean enableTempo = container.getBoolean(ADAConfigurationKey.ADA_ENABLE_TEMPO);
        fillInt8(buffer, startIndex++, enableTempo ? 1 : 0);

        //fill enableLevel
        boolean enableLevel = container.getBoolean(ADAConfigurationKey.ADA_ENABLE_LEVEL);
        fillInt8(buffer, startIndex++, enableLevel ? 1 : 0);

        //fill enableReverb
        boolean enableReverb = container.getBoolean(ADAConfigurationKey.ADA_ENABLE_REVERB);
        fillInt8(buffer, startIndex++, enableReverb ? 1 : 0);

        //fill noiseTypeForVoP
        fillInt(buffer, startIndex, container.getInt(ADAConfigurationKey.ADA_NOISE_TYPE_VOP));
        startIndex += 4;

        //fill noiseSnrForVoP
        fillFloat(buffer, startPos, container.getFloat(ADAConfigurationKey.ADA_NOISE_SNR_VOP));
        startIndex += 4;

        //reseved array
    }

    private void fillAdaParamsforKW(byte[] buffer, final int startPos, Container container) {
        int startIndex = startPos;
        //fill support noises
        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            boolean enableNoise = false;
            if (i == CAR_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE_CAR_NOISE);
            } else if (i == MUSIC_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE_MUSIC_NOISE);
            } else if (i == PARTY_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE_PARTY_NOISE);
            } else if (i == RADIO_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE_RADIO_NOISE);
            }
            fillInt8(buffer, startIndex++, enableNoise ? 1 : 0);
        }

        //fill SNR settings
        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            int numSnr = 0;
            if (i == CAR_NOISE_INDEX || i == MUSIC_NOISE_INDEX
                    || i == PARTY_NOISE_INDEX || i == RADIO_NOISE_INDEX) {
                numSnr = 2;
            }
            fillInt(buffer, startIndex, numSnr);
            startIndex += 4;
        }

        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            int[] noiseArray = null;
            if (i == CAR_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAKWConfigurationKey.ADA_CAR_NOISE));
            } else if (i == MUSIC_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAKWConfigurationKey.ADA_MUSIC_NOISE));
            } else if (i == PARTY_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAKWConfigurationKey.ADA_PARTY_NOISE));
            } else if (i == RADIO_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAKWConfigurationKey.ADA_RADIO_NOISE));
            }
            if (noiseArray != null && noiseArray.length > 0) {
                for (int j = 0; j < noiseArray.length; j++) {
                    fillInt(buffer, startIndex, noiseArray[j]);
                    startIndex += 4;
                }
                startIndex += 4 * (ADA_NUM_MAX_SETTING - noiseArray.length);
            } else {
                startIndex += 4 * ADA_NUM_MAX_SETTING;
            }

        }

        //fill numCleanRepeation
        fillInt(buffer, startIndex, container.getInt(ADAKWConfigurationKey.ADA_NUM_CLEAN_REPETITION));
        startIndex += 4;

        //fill enablePitch
        boolean enablePitch = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE_PITCH);
        fillInt8(buffer, startIndex++, enablePitch ? 1 : 0);

        //fill enableTempo
        boolean enableTempo = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE_TEMPO);
        fillInt8(buffer, startIndex++, enableTempo ? 1 : 0);

        //fill enableLevel
        boolean enableLevel = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE_LEVEL);
        fillInt8(buffer, startIndex++, enableLevel ? 1 : 0);

        //fill enableReverb
        boolean enableReverb = container.getBoolean(ADAKWConfigurationKey.ADA_ENABLE_REVERB);
        fillInt8(buffer, startIndex++, enableReverb ? 1 : 0);

        //fill noiseTypeForVoP
        fillInt(buffer, startIndex, container.getInt(ADAKWConfigurationKey.ADA_NOISE_TYPE_VOP));
        startIndex += 4;

        //fill noiseSnrForVoP
        fillFloat(buffer, startPos, container.getFloat(ADAKWConfigurationKey.ADA_NOISE_SNR_VOP));
        startIndex += 4;

        //reseved array
    }

    private void fillAdaParamsforUser(byte[] buffer, final int startPos, Container container) {
        int startIndex = startPos;
        //fill support noises
        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            boolean enableNoise = false;
            if (i == CAR_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE_CAR_NOISE);
            } else if (i == MUSIC_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE_MUSIC_NOISE);
            } else if (i == PARTY_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE_PARTY_NOISE);
            } else if (i == RADIO_NOISE_INDEX) {
                enableNoise = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE_RADIO_NOISE);
            }
            fillInt8(buffer, startIndex++, enableNoise ? 1 : 0);
        }

        //fill SNR settings
        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            int numSnr = 0;
            if (i == CAR_NOISE_INDEX || i == MUSIC_NOISE_INDEX
                    || i == PARTY_NOISE_INDEX || i == RADIO_NOISE_INDEX) {
                numSnr = 2;
            }
            fillInt(buffer, startIndex, numSnr);
            startIndex += 4;
        }

        for (int i = 0; i < ADA_NUM_MAX_SETTING; i++) {
            int[] noiseArray = null;
            if (i == CAR_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAUserConfigurationKey.ADA_CAR_NOISE));
            } else if (i == MUSIC_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAUserConfigurationKey.ADA_MUSIC_NOISE));
            } else if (i == PARTY_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAUserConfigurationKey.ADA_PARTY_NOISE));
            } else if (i == RADIO_NOISE_INDEX) {
                noiseArray = stringConvertIntArray(container.
                        getString(ADAUserConfigurationKey.ADA_RADIO_NOISE));
            }
            if (noiseArray != null && noiseArray.length > 0) {
                for (int j = 0; j < noiseArray.length; j++) {
                    fillInt(buffer, startIndex, noiseArray[j]);
                    startIndex += 4;
                }
                startIndex += 4 * (ADA_NUM_MAX_SETTING - noiseArray.length);
            } else {
                startIndex += 4 * ADA_NUM_MAX_SETTING;
            }

        }

        //fill numCleanRepeation
        fillInt(buffer, startIndex, container.getInt(ADAUserConfigurationKey.ADA_NUM_CLEAN_REPETITION));
        startIndex += 4;

        //fill enablePitch
        boolean enablePitch = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE_PITCH);
        fillInt8(buffer, startIndex++, enablePitch ? 1 : 0);

        //fill enableTempo
        boolean enableTempo = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE_TEMPO);
        fillInt8(buffer, startIndex++, enableTempo ? 1 : 0);

        //fill enableLevel
        boolean enableLevel = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE_LEVEL);
        fillInt8(buffer, startIndex++, enableLevel ? 1 : 0);

        //fill enableReverb
        boolean enableReverb = container.getBoolean(ADAUserConfigurationKey.ADA_ENABLE_REVERB);
        fillInt8(buffer, startIndex++, enableReverb ? 1 : 0);

        //fill noiseTypeForVoP
        fillInt(buffer, startIndex, container.getInt(ADAUserConfigurationKey.ADA_NOISE_TYPE_VOP));
        startIndex += 4;

        //fill noiseSnrForVoP
        fillFloat(buffer, startPos, container.getFloat(ADAUserConfigurationKey.ADA_NOISE_SNR_VOP));
        startIndex += 4;

        //reseved array
    }

    private void fillInt8(byte[] buffer, final int startPos, int value) {
        buffer[startPos] = (byte) (value & 0xff);
    }

    private void fillInt(byte[] buffer, final int startPos, int value) {
        int startIndex = startPos;
        buffer[startIndex] = (byte) (value & 0xff);
        buffer[++startIndex] = (byte) (value >> 8 & 0xff);
        buffer[++startIndex] = (byte) (value >> 16 & 0xff);
        buffer[++startIndex] = (byte) (value >> 24 & 0xff);
    }

    private void fillFloat(byte[] buffer, final int startPos, float value) {
        int intbits = Float.floatToIntBits(value);
        fillInt(buffer, startPos, intbits);
    }

    private int[] stringConvertIntArray(String str) {
        String[] split = str.split(",");
        int[] result = new int[split.length];
        try {
            for (int i = 0; i < split.length; i++) {
                result[i] = Integer.parseInt(split[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
