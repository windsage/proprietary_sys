/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

import android.hardware.common.Ashmem;
import vendor.qti.hardware.ListenSoundModelAidl.ListenDetectionEventType;
import vendor.qti.hardware.ListenSoundModelAidl.ListenEpdParams;
import vendor.qti.hardware.ListenSoundModelAidl.ListenEventPayload;
import vendor.qti.hardware.ListenSoundModelAidl.ListenQualityCheckResult;
import vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel;
import vendor.qti.hardware.ListenSoundModelAidl.ListenSoundModelHeader;
import vendor.qti.hardware.ListenSoundModelAidl.ListenSoundModelInfo;
import vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel;
import vendor.qti.hardware.ListenSoundModelAidl.ListenDetectionEventType;
import vendor.qti.hardware.ListenSoundModelAidl.ByteValue;
import vendor.qti.hardware.ListenSoundModelAidl.IntegerValue;
import vendor.qti.hardware.ListenSoundModelAidl.FloatValue;
import vendor.qti.hardware.ListenSoundModelAidl.StringValue;

// Interface inherits from vendor.qti.hardware.ListenSoundModel@1.0::IListenSoundModel but AIDL does not support interface inheritance (methods have been flattened).
@VintfStability
interface IListenSoundModel {

    int LsmCheckRecordingsQuality(in Ashmem pLanguageModel,
        in int langModelSize, in ListenEpdParams[] pEpdParameter, in int numUserRecording,
        in Ashmem[] pUserRecordings, in int[] recSizes, out FloatValue snr);

    int LsmCheckUserRecording(in Ashmem pLanguageModel,
        in int langModelSize, in ListenEpdParams[] pEpdParameter,
        in Ashmem pUserRecording, in int recSize, in int maxPhonemeLength, out FloatValue snr);

    int LsmCreateUserDefinedKeywordModel(in Ashmem pUserDefinedKeyword,
        in int modelSize, in String keywordId, in String userId,
        in ListenEpdParams[] pEpdParameter, in int numUserRecording,
        in Ashmem[] pUserRecordings, in int[] recSizes,
        in Ashmem pLanguageModel, in int langModelSize, in int outModelSize,
        out Ashmem pOutputUserDefinedKeyword,  out IntegerValue matchingScore);

    int LsmCreateUserKeywordModel(in Ashmem pUserDefinedKeyword,
        in int modelSize, in String keywordId, in String userId,
        in ListenEpdParams[] pEpdParameter, in int numUserRecording,
        in Ashmem[] pUserRecordings, in int[] recSizes, in int outModelSize,
        out Ashmem pUserKeywordModel, out IntegerValue userMatchingScore);

    int LsmDeleteFromModel(in Ashmem pInputModel, in int modelSize,
        in String keywordId, in String userId, in int resModelSize, out Ashmem pResultModel);

    int LsmFindKeywordEndPosition(in Ashmem pKeywordModel,
        in int modelSize, in String keywordId, in Ashmem pUserRecording,
        in int recSize, out IntegerValue pkEndPosition);

    int LsmGetBinaryModelSize(in Ashmem pListenModel, in int modelSize, out IntegerValue nBinaryModelSize);

    int LsmGetKeywordPhrases(in Ashmem pSoundModel, in int modelSize,
        in char numInpKeywords, out IntegerValue numKeywords, out StringValue[] keywords);

    int LsmGetMergedModelSize(in char numModels, in Ashmem[] pModels,
        in int[] modelSizes, out IntegerValue nOutputModelSize);

    int LsmGetSizeAfterDeleting(in Ashmem pInputModel, in int modelSize,
        in String keywordId, in String userId, out IntegerValue nOutputModelSize);

    int LsmGetSortedKeywordStatesUserKeywordModelSize(
        in Ashmem pListenModel, in int modelSize, out IntegerValue nSortedModelSize);

    int LsmGetSoundModelHeader(in Ashmem pSoundModel, in int modelSize, out ListenSoundModelHeader[] pListenSoundModelHeader);

    int LsmGetStrippedUserKeywordModelSize(in Ashmem pModel,
        in int modelSize, out IntegerValue nStrippedModelSize);

    int LsmGetUserDefinedKeywordApproxSize(in String keywordId, in String userId,
        in Ashmem pLanguageModel, in int langModelSize,
        in int maxPhonemeLength, out IntegerValue pOutputSize);

    int LsmGetUserDefinedKeywordSize(in Ashmem pUserDefinedKeyword,
        in int modelSize, in String keywordId, in String userId,
        in ListenEpdParams[] pEpdParameter, in int numUserRecording,
        in Ashmem[] pUserRecordings, in int[] recSizes,
        in Ashmem pLanguageModel, in int langModelSize, out IntegerValue pOutputSize);

    int LsmGetUserKeywordModelSize(in Ashmem pKeywordModel,
        in int modelSize, in String keywordId, in String userId, out IntegerValue nUserKeywordModelSize);

    int LsmGetUserNames(in Ashmem pSoundModel, in int modelSize,
        in char numInpUsers, out IntegerValue numUsers, out StringValue[] users);

    // Adding return type to method instead of out param int status since there is only one return value.
    int LsmLoadConfParams(in byte[] pConfData, in int confDataSize);

    int LsmMergeModels(in char numModels, in Ashmem[] pModels,
        in int[] modelSizes, in int mergedModelSize, out Ashmem pMergedModel);

    int LsmParseDetectionEventData(in Ashmem pUserKeywordModel,
        in int modelSize, in ListenEventPayload[] pEventPayload, out ListenDetectionEventType[] pDetectionEvent);

    int LsmParseFromBigSoundModel(in Ashmem pSM3p0Model,
        in int bigModelSize, out Ashmem p1stStageModel, out IntegerValue i1stStgModelSize,
        out Ashmem p2ndStageKWModel, out IntegerValue i2ndStgKWModelSize,	out Ashmem p2stStageVoPModel,
        out IntegerValue i2ndStgVOPModelSize, out IntegerValue indicator);

    int LsmQuerySoundModel(in Ashmem pSoundModel, in int modelSize, out ListenSoundModelInfo[] pListenSoundModelInfo);

    // Adding return type to method instead of out param int status since there is only one return value.
    int LsmReleaseSoundModelHeader(in ListenSoundModelHeader[] pListenSoundModelHeader);

    int LsmSmlGet(in ListenSmlModel[] pSmlModel, in int configId, in byte[] pConfig,
        in int configSize, out ByteValue[] pOutConfig, out IntegerValue configFilledSize);

    // Adding return type to method instead of out param int status since there is only one return value.
    int LsmSmlGetReqMem(in int memStructId, in byte[] memStruct, in int memStructSize,
        in int configId, in byte[] configStruct, in int configStructSize);

    int LsmSmlGet_v2(in ListenSmlModel pSmlModel, in int configId, in byte[] pConfig,
        in int configSize, out ByteValue[] pOutConfig, out IntegerValue configFilledSize);

    int LsmSmlInit(in int configId, in byte[] pConfig, in int configSize,
        in byte[] pStaticMemory, in int staticMemorySize, out ListenSmlModel[] pSmlModel);

    int LsmSmlInit_v2(in int configId, in byte[] pConfig, in int configSize,
        in Ashmem pStaticMemory, in int staticMemorySize, out ListenSmlModel pSmlModel);

    int LsmSmlProcess(in ListenSmlModel[] pSmlModel, in byte[] pInputData,
        in int inputDataSize, in int inputDataId, out ByteValue[] pOutputData,
        out IntegerValue outputDataSize, out IntegerValue outputDataId /*This is always zero */);

    int LsmSmlProcess_v2(in ListenSmlModel pSmlModel, in byte[] pInputData,
        in int inputDataSize, in int inputDataId, out ByteValue[] pOutputData,
        out IntegerValue outputDataSize, out IntegerValue outputDataId /*This is always zero */);

    // Adding return type to method instead of out param int status since there is only one return value.
    int LsmSmlSet(in ListenSmlModel[] pSmlModel, in int configId, in byte[] pConfig,
        in int configSize);

    // Adding return type to method instead of out param int status since there is only one return value.
    int LsmSmlSet_v2(in ListenSmlModel pSmlModel, in int configId, in byte[] pConfig,
        in int configSize);

    int LsmSortKeywordStatesOfUserKeywordModel(in Ashmem pInputModel,
        in int inpModelSize, in int sortedModelSize, out Ashmem pSortedModel);

    int LsmStripUserKeywordModel(in Ashmem pModel, in int modelSize,
        out Ashmem pStrippedModel, out IntegerValue strippedModelSize);

    int LsmTuneUserDefinedKeywordModelThreshold(
        in Ashmem pUserDefinedKeyword, in int modelSize, in String keywordId,
        in Ashmem pUserRecording, in int recSize, out Ashmem pOutputUserDefinedKeyword,
        out IntegerValue outModelSize);

    int LsmVerifyUserRecording(in Ashmem pKeywordModel,
        in int modelSize, in String keywordId, in ListenEpdParams[] pEpdParameter,
        in Ashmem pUserRecording, in int recSize, out IntegerValue confidenceLevel);

    int LsmVerifyUserRecordingExt(in Ashmem pKeywordModel,
        in int modelSize, in String keywordId, in ListenEpdParams[] pEpdParameter,
        in Ashmem pUserRecording, in int recSize, in int isNoisySample,
        out ListenQualityCheckResult[] pQualityCheckResult);
}
