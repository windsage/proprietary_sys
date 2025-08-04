/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.xmlhelper;

import android.text.TextUtils;
import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Container {
    private final static String TAG = Container.class.getSimpleName();
    private String mContainerName = null;
    private List<Item> mItems = new ArrayList<>();

    public Container() {}

    public Container(String name) {
        mContainerName = name;
    }

    public String getName() {
        return mContainerName;
    }

    public void setName(String name) {
        mContainerName = name;
    }

    public int getInt(String key) {
        Item item = get(key);
        if (item != null && item.getType().equals(Type.INTEGER)) {
            String value = item.getValue().toLowerCase();
            if (value.startsWith("0x")) {
                String hex = value.substring("0x".length());
                return new BigInteger(hex, 16).intValue();
            }
            return Integer.parseInt(item.getValue());
        }
        return Integer.MIN_VALUE;
    }

    public Boolean getBoolean(String key) {
        Item item = get(key);
        if (item != null && item.getType().equals(Type.BOOLEAN)) {
            return Boolean.parseBoolean(item.getValue());
        }
        return null;
    }

    public String getString(String key) {
        Item item = get(key);
        if (item != null && item.getType().equals(Type.STRING)) {
            return item.getValue();
        }
        return null;
    }

    public float getFloat(String key) {
        Item item = get(key);
        if (item != null && item.getType().equals(Type.FLOAT)) {
            return Float.parseFloat(item.getValue());
        }
        return Float.MIN_VALUE;
    }

    public void put(Item item) {
        if (item == null) {
            Log.e(TAG,"item is null");
            return;
        }
        for (Item it : mItems) {
            if(it.getName().equalsIgnoreCase(item.getName())){
                it.setValue(item.getValue());
                return;
            }
        }
        mItems.add(item);
    }

    public Item get(String key) {
        if (key == null) return null;
        for (Item item : mItems) {
            if (item.getName().equalsIgnoreCase(key)) {
                return item;
            }
        }
        return null;
    }

    public List<Item> getItems() {
        return mItems;
    }

    @Override
    public String toString() {
        StringBuilder container = new StringBuilder();
        container.append("Container[name= " + getName() + "]{\n");
        for (Item item : mItems) {
            container.append(item.toString()).append("\n");
        }
        return container.append("}\n").toString();
    }

    public static interface SMLPDKConfigurationKey {
        String TRAINING_NUMGUARDFRAMES_FOR_EPD = "Training.numGuardFramesForEPD";
        String TRAINING_ENABLE_FIRST_STAGE_EPD = "Training.enableFstageEpd";
        String TRAINING_NUMGUARDFRAMES_FOR_FIRST_STAGE_EPD = "Training.numGuardFramesForFStageEPD";
        String TRAINING_CHECK_CHOPPED = "Training.checkChopped";
        String TRAINING_CHOPPING_FRAME_THRESHOLD = "Training.choppingFrameThreshold";
        String TRAINING_SNR_THRESHOLD_FOR_NOISY_SAMPLES = "Training.snrThresholdForNoisySamples";
        String TRAINING_PCM_NORMALIZATION = "Training.doPcmNormalization";
        String TRAINING_NUM_CLEAR_RECORDINGS = "Training.nCleanRecordings";
        String TRAINING_CHECK_CLIPPED = "Training.checkClipped";
        String TRAINING_NUM_CLIPPING_THRESHOLD = "Training.nClippingThreshold";
        String TRAINING_CLIPPING_RATIO_THRESHOLD = "Training.clippingRatioThreshold";
        String TRAINING_ENABLE_ZERO_PROCESSING_FOR_PDK_XS = "Training.enableZeroProcessingForPdkXS";
        String TRAINING_NUM_ZEROS_PADDING_FRAMES = "Training.nZerosPaddingFrames";
        String TRAINING_B_ENABLE_EPD_FOR_UV72 = "Training.bEnableEPDForUv72";
    }

    public static interface SMLUDKConfigurationKey {
        String TRAINING_NUMGUARDFRAMES_FOR_FIRST_STAGE_UV = "Training.numGuardFramesForFstageUV";
        String TRAINING_NUMGUARDFRAMES_FOR_SECOND_STAGE_UV = "Training.numGuardFramesForSstageUV";
        String TRAINING_MIN_PHONEME_LENGTH = "Training.minPhonemeLength";
        String TRAINING_MAX_PHONEME_LENGTH = "Training.maxPhonemeLength";
        String TRAINING_PCM_NORMALIZATION = "Training.doPcmNormalization";
        String TRAINING_NUMPREGUARDFRAMES_FOR_SECOND_STAGE_UDK =
                                                    "Training.numPreGuardFramesForSstageUDK";
        String TRAINING_NUMPOSTGUARDFRAMES_FOR_SECOND_STAGE_UDK =
                                                    "Training.numPostGuardFramesForSstageUDK";
        String TRAINING_B_ENABLE_EPD_FOR_UV72 = "Training.bEnableEPDForUv72";

        String TRAINING_QUALITY_CHECK_THRESHOLD = "Training.qualityCheckingThresholdForSstageUDK";

    }

    public static interface EPDConfigurationKey {
        String EPD_MIN_SNR_ONSET = "EPD.minSnrOnset";
        String EPD_MIN_SNR_LEAVE = "EPD.minSnrLeave";
        String EPD_SNR_FLOOR = "EPD.snrFloor";
        String EPD_SNR_THRESHOLDS = "EPD.snrThresholds";
        String EPD_FORGETTING_FACTOR_NOISE = "EPD.forgettingFactorNoise";
        String EPD_NUM_FRAME_TRANSIENT_FRAME = "EPD.numFrameTransientFrame";
        String EPD_MIN_ENERGY_FRAME_RATIO = "EPD.minEnergyFrameRatio";
        String EPD_MIN_NOISE_ENERGY = "EPD.minNoiseEnergy";
        String EPD_NUM_MIN_FRAMES_INPHRASE = "EPD.numMinFramesInPhrase";
        String EPD_NUM_MIN_FRAMES_INSPEECH = "EPD.numMinFramesInSpeech";
        String EPD_NUM_MIN_FRAMES_INSPEECH_GAP = "EPD.numMaxFrameInSpeechGap";
        String EPD_NUM_FRAMES_INHEAD = "EPD.numFramesInHead";
        String EPD_NUM_FRAMES_INTAIL = "EPD.numFramesInTail";
        String EPD_PRE_EMPHASIZING = "EPD.preEmphasize";
        String EPD_NUM_MAX_FRAMES = "EPD.numMaxFrames";
        String EPD_KEYWORD_THRESHOLD = "EPD.keyword_threshold";
    }

    public enum ONLINEEPDConfigurationKey {
        EPD_MIN_SNR_ONSET("EPD.minSnrOnset", Type.FLOAT),
        EPD_MIN_SNR_LEAVE("EPD.minSnrLeave", Type.FLOAT),
        EPD_SNR_FLOOR("EPD.snrFloor", Type.FLOAT),
        EPD_SNR_THRESHOLDS("EPD.snrThresholds", Type.FLOAT),
        EPD_FORGETTING_FACTOR_NOISE("EPD.forgettingFactorNoise", Type.FLOAT),
        EPD_NUM_FRAME_TRANSIENT_FRAME("EPD.numFrameTransientFrame", Type.INTEGER),
        EPD_MIN_ENERGY_FRAME_RATIO("EPD.minEnergyFrameRatio", Type.FLOAT),
        EPD_MIN_NOISE_ENERGY("EPD.minNoiseEnergy", Type.FLOAT),
        EPD_NUM_MIN_FRAMES_INPHRASE("EPD.numMinFramesInPhrase", Type.INTEGER),
        EPD_NUM_MIN_FRAMES_INSPEECH("EPD.numMinFramesInSpeech", Type.INTEGER),
        EPD_NUM_MAX_FRAMES_INSPEECH_GAP("EPD.numMaxFrameInSpeechGap", Type.INTEGER),
        EPD_NUM_FRAMES_INHEAD("EPD.numFramesInHead", Type.INTEGER),
        EPD_NUM_FRAMES_INTAIL("EPD.numFramesInTail", Type.INTEGER),
        EPD_PRE_EMPHASIZING("EPD.preEmphasize", Type.INTEGER),
        EPD_NUM_MAX_FRAMES("EPD.numMaxFrames", Type.INTEGER);

        private String mValue;
        private Type mType;

        ONLINEEPDConfigurationKey(String value, Type type) {
            mValue = value;
            mType = type;
        }

        public String getValue() {
            return mValue;
        }

        public Type getType() {
            return mType;
        }
    }

    public static interface ADAConfigurationKey {
        String ADA_ENABLE = "Ada.enable";
        String ADA_ENABLE_CAR_NOISE = "Ada.enable_car_noise";
        String ADA_ENABLE_MUSIC_NOISE = "Ada.enable_music_noise";
        String ADA_ENABLE_PARTY_NOISE = "Ada.enable_party_noise";
        String ADA_ENABLE_RADIO_NOISE = "Ada.enable_radio_noise";
        String ADA_CAR_NOISE = "Ada.car_noise";
        String ADA_MUSIC_NOISE = "Ada.music_noise";
        String ADA_PARTY_NOISE = "Ada.party_noise";
        String ADA_RADIO_NOISE = "Ada.radio_noise";
        String ADA_NUM_CLEAN_REPETITION = "Ada.numCleanRepetition";
        String ADA_ENABLE_PITCH = "Ada.enablePitch";
        String ADA_ENABLE_TEMPO = "Ada.enableTempo";
        String ADA_ENABLE_LEVEL = "Ada.enableLevel";
        String ADA_ENABLE_REVERB = "Ada.enableReverb";
        String ADA_NOISE_TYPE_VOP = "Ada.noise_type_vop";
        String ADA_NOISE_SNR_VOP = "Ada.noise_snr_vop";
    }

    public static interface ADAKWConfigurationKey {
        String ADA_ENABLE = "Ada.kw.enable";
        String ADA_ENABLE_CAR_NOISE = "Ada.kw.enable_car_noise";
        String ADA_ENABLE_MUSIC_NOISE = "Ada.kw.enable_music_noise";
        String ADA_ENABLE_PARTY_NOISE = "Ada.kw.enable_party_noise";
        String ADA_ENABLE_RADIO_NOISE = "Ada.kw.enable_radio_noise";
        String ADA_CAR_NOISE = "Ada.kw.car_noise";
        String ADA_MUSIC_NOISE = "Ada.kw.music_noise";
        String ADA_PARTY_NOISE = "Ada.kw.party_noise";
        String ADA_RADIO_NOISE = "Ada.kw.radio_noise";
        String ADA_NUM_CLEAN_REPETITION = "Ada.kw.numCleanRepetition";
        String ADA_ENABLE_PITCH = "Ada.kw.enablePitch";
        String ADA_ENABLE_TEMPO = "Ada.kw.enableTempo";
        String ADA_ENABLE_LEVEL = "Ada.kw.enableLevel";
        String ADA_ENABLE_REVERB = "Ada.kw.enableReverb";
        String ADA_NOISE_TYPE_VOP = "Ada.kw.noise_type_vop";
        String ADA_NOISE_SNR_VOP = "Ada.kw.noise_snr_vop";
    }

    public static interface ADAUserConfigurationKey {
        String ADA_ENABLE = "Ada.user.enable";
        String ADA_ENABLE_CAR_NOISE = "Ada.user.enable_car_noise";
        String ADA_ENABLE_MUSIC_NOISE = "Ada.user.enable_music_noise";
        String ADA_ENABLE_PARTY_NOISE = "Ada.user.enable_party_noise";
        String ADA_ENABLE_RADIO_NOISE = "Ada.user.enable_radio_noise";
        String ADA_CAR_NOISE = "Ada.user.car_noise";
        String ADA_MUSIC_NOISE = "Ada.user.music_noise";
        String ADA_PARTY_NOISE = "Ada.user.party_noise";
        String ADA_RADIO_NOISE = "Ada.user.radio_noise";
        String ADA_NUM_CLEAN_REPETITION = "Ada.user.numCleanRepetition";
        String ADA_ENABLE_PITCH = "Ada.user.enablePitch";
        String ADA_ENABLE_TEMPO = "Ada.user.enableTempo";
        String ADA_ENABLE_LEVEL = "Ada.user.enableLevel";
        String ADA_ENABLE_REVERB = "Ada.user.enableReverb";
        String ADA_NOISE_TYPE_VOP = "Ada.user.noise_type_vop";
        String ADA_NOISE_SNR_VOP = "Ada.user.noise_snr_vop";
    }
}
