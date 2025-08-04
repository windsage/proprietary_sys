/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.onlineepd;

import android.content.Context;
import android.os.Bundle;

import com.qualcomm.listen.ListenTypes;
import com.qualcomm.listen.ListenTypes.EPDHandle;
import com.qualcomm.listen.ListenTypes.EPDResult;
import com.qualcomm.listen.ListenTypes.ListenEPDParams;
import com.qualcomm.listen.ListenSoundModel;
import com.qualcomm.qti.sva.data.ISettingsModel;
import com.qualcomm.qti.sva.data.SettingsModel;
import com.qualcomm.qti.sva.utils.LogUtils;
import com.qualcomm.qti.sva.xmlhelper.Container.*;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class EPD {
    private EPDResult mEPDResult = new EPDResult();
    private EPDHandle mEPDHandle;
    private ListenEPDParams mParams;
    private ByteBuffer mSoundModel;
    public static ProcessResult EMPTY = createProcessResult(Integer.MIN_VALUE, null);
    public static final String EPD_KEYWORD_THRESHOLD = "EPD.keyword_threshold";

    public EPD(Context context, ByteBuffer soundModel, Bundle params) {
        mSoundModel = soundModel;
        ISettingsModel settingsModel = new SettingsModel(context,null);
        int trainingThreshold = settingsModel.getGlobalGMMTrainingConfidenceLevel();
        LogUtils.d("EPD trainingThreshold=" + trainingThreshold);
        Bundle newParam = new Bundle(params);
        newParam.putInt(EPD.EPD_KEYWORD_THRESHOLD, trainingThreshold);
        LogUtils.d("EPD convertEPDConfiguration params = " + newParam);
        convertEPDConfiguration(newParam);
        mEPDHandle = new EPDHandle();
    }

    private void convertEPDConfiguration(Bundle params) {
        mParams = new ListenEPDParams();
        mParams.minSnrOnset =
                params.getFloat(ONLINEEPDConfigurationKey.EPD_MIN_SNR_ONSET.getValue());
        mParams.minSnrLeave = params.getFloat(
                ONLINEEPDConfigurationKey.EPD_MIN_SNR_LEAVE.getValue());
        mParams.snrFloor = params.getFloat(
                ONLINEEPDConfigurationKey.EPD_SNR_FLOOR.getValue());
        mParams.snrThresholds= params.getFloat(
                ONLINEEPDConfigurationKey.EPD_SNR_THRESHOLDS.getValue());
        mParams.forgettingFactorNoise= params.getFloat(
                ONLINEEPDConfigurationKey.EPD_FORGETTING_FACTOR_NOISE.getValue());
        mParams.numFrameTransientFrame= params.getInt(
                ONLINEEPDConfigurationKey.EPD_NUM_FRAME_TRANSIENT_FRAME.getValue());
        mParams.minEnergyFrameRatio= params.getFloat(
                ONLINEEPDConfigurationKey.EPD_MIN_ENERGY_FRAME_RATIO.getValue());
        mParams.minNoiseEnergy= params.getFloat(
                ONLINEEPDConfigurationKey.EPD_MIN_NOISE_ENERGY.getValue());
        mParams.numMinFramesInPhrase= params.getInt(
                ONLINEEPDConfigurationKey.EPD_NUM_MIN_FRAMES_INPHRASE.getValue());
        mParams.numMinFramesInSpeech= params.getInt(
                ONLINEEPDConfigurationKey.EPD_NUM_MIN_FRAMES_INSPEECH.getValue());
        mParams.numMaxFrameInSpeechGap= params.getInt(
                ONLINEEPDConfigurationKey.EPD_NUM_MAX_FRAMES_INSPEECH_GAP.getValue());
        mParams.numFramesInHead= params.getInt(
                ONLINEEPDConfigurationKey.EPD_NUM_FRAMES_INHEAD.getValue());
        mParams.numFramesInTail= params.getInt(
                ONLINEEPDConfigurationKey.EPD_NUM_FRAMES_INTAIL.getValue());
        mParams.preEmphasize= params.getInt(
                ONLINEEPDConfigurationKey.EPD_PRE_EMPHASIZING.getValue());
        mParams.numMaxFrames= params.getInt(
                ONLINEEPDConfigurationKey.EPD_NUM_MAX_FRAMES.getValue());
        mParams.keyword_threshold= params.getInt(EPD_KEYWORD_THRESHOLD);
    }

    public ProcessResult init() {
        int result = ListenTypes.STATUS_EFAILURE;
        synchronized (this) {
            result = ListenSoundModel.initEPD(mEPDHandle, mParams, mSoundModel);
        }
        LogUtils.d("EPD init result = " + result);
        return createProcessResult(result, null);
    }

    public ProcessResult reinit() {
        int result = ListenTypes.STATUS_EFAILURE;
        synchronized (this) {
            result = ListenSoundModel.reinitEPD(mEPDHandle);
        }
        LogUtils.d("EPD reinit result = " + result);
        return createProcessResult(result, null);
    }

    public ProcessResult process(ShortBuffer userRecording) {
        int result = ListenTypes.STATUS_EFAILURE;
        synchronized (this) {
            result = ListenSoundModel.processEPD(mEPDHandle, userRecording,
                    mEPDResult);
        }
        LogUtils.d("EPD process result = " + result + " mEPDResult = " + mEPDResult);
        return createProcessResult(result, mEPDResult);
    }

    public ProcessResult release() {
        int result = ListenTypes.STATUS_EFAILURE;
        synchronized (this) {
            result = ListenSoundModel.releaseEPD(mEPDHandle);
            mEPDHandle = null;
        }
        LogUtils.d("EPD release result = " + result);
        return createProcessResult(result, null);
    }

    private static ProcessResult createProcessResult(int code, EPDResult epdResult) {
        return new ProcessResult(code, epdResult);
    }

    public static class ProcessResult {
        private int mCode;
        private EPDResult mEDPResult;

        ProcessResult(int code, EPDResult epdResult) {
            mCode = code;
            mEDPResult = epdResult;
        }

        public boolean isSuccess() {
            return mCode >= ListenTypes.STATUS_SUCCESS;
        }

        public boolean isDectected() {
            if (mEDPResult != null)
                return mEDPResult.is_detected == 1;
            return false;
        }

        public float getSNR() {
            if (mEDPResult != null)
                return mEDPResult.snr;
            return Float.MIN_VALUE;
        }

        public int getStartIndex() {
            if (mEDPResult != null)
                return mEDPResult.start_index;
            return Integer.MIN_VALUE;
        }

        public int getEndIndex() {
            if (mEDPResult != null)
                return mEDPResult.end_index;
            return Integer.MIN_VALUE;
        }

        @Override
        public String toString() {
            return "EPD.ProcessResult[" + mCode + "]="
                    + (isSuccess() ? "success" : "fail")
                    + "[detected=" + isDectected()
                    + ",snr=" + getSNR()
                    + ",startIndex=" + getStartIndex()
                    + ",endIndex=" + getEndIndex() + "]";
        }
    }
}