/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package vendor.qti.hardware.ListenSoundModelAidl;
@VintfStability
interface IListenSoundModel {
  int LsmCheckRecordingsQuality(in android.hardware.common.Ashmem pLanguageModel, in int langModelSize, in vendor.qti.hardware.ListenSoundModelAidl.ListenEpdParams[] pEpdParameter, in int numUserRecording, in android.hardware.common.Ashmem[] pUserRecordings, in int[] recSizes, out vendor.qti.hardware.ListenSoundModelAidl.FloatValue snr);
  int LsmCheckUserRecording(in android.hardware.common.Ashmem pLanguageModel, in int langModelSize, in vendor.qti.hardware.ListenSoundModelAidl.ListenEpdParams[] pEpdParameter, in android.hardware.common.Ashmem pUserRecording, in int recSize, in int maxPhonemeLength, out vendor.qti.hardware.ListenSoundModelAidl.FloatValue snr);
  int LsmCreateUserDefinedKeywordModel(in android.hardware.common.Ashmem pUserDefinedKeyword, in int modelSize, in String keywordId, in String userId, in vendor.qti.hardware.ListenSoundModelAidl.ListenEpdParams[] pEpdParameter, in int numUserRecording, in android.hardware.common.Ashmem[] pUserRecordings, in int[] recSizes, in android.hardware.common.Ashmem pLanguageModel, in int langModelSize, in int outModelSize, out android.hardware.common.Ashmem pOutputUserDefinedKeyword, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue matchingScore);
  int LsmCreateUserKeywordModel(in android.hardware.common.Ashmem pUserDefinedKeyword, in int modelSize, in String keywordId, in String userId, in vendor.qti.hardware.ListenSoundModelAidl.ListenEpdParams[] pEpdParameter, in int numUserRecording, in android.hardware.common.Ashmem[] pUserRecordings, in int[] recSizes, in int outModelSize, out android.hardware.common.Ashmem pUserKeywordModel, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue userMatchingScore);
  int LsmDeleteFromModel(in android.hardware.common.Ashmem pInputModel, in int modelSize, in String keywordId, in String userId, in int resModelSize, out android.hardware.common.Ashmem pResultModel);
  int LsmFindKeywordEndPosition(in android.hardware.common.Ashmem pKeywordModel, in int modelSize, in String keywordId, in android.hardware.common.Ashmem pUserRecording, in int recSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue pkEndPosition);
  int LsmGetBinaryModelSize(in android.hardware.common.Ashmem pListenModel, in int modelSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue nBinaryModelSize);
  int LsmGetKeywordPhrases(in android.hardware.common.Ashmem pSoundModel, in int modelSize, in char numInpKeywords, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue numKeywords, out vendor.qti.hardware.ListenSoundModelAidl.StringValue[] keywords);
  int LsmGetMergedModelSize(in char numModels, in android.hardware.common.Ashmem[] pModels, in int[] modelSizes, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue nOutputModelSize);
  int LsmGetSizeAfterDeleting(in android.hardware.common.Ashmem pInputModel, in int modelSize, in String keywordId, in String userId, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue nOutputModelSize);
  int LsmGetSortedKeywordStatesUserKeywordModelSize(in android.hardware.common.Ashmem pListenModel, in int modelSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue nSortedModelSize);
  int LsmGetSoundModelHeader(in android.hardware.common.Ashmem pSoundModel, in int modelSize, out vendor.qti.hardware.ListenSoundModelAidl.ListenSoundModelHeader[] pListenSoundModelHeader);
  int LsmGetStrippedUserKeywordModelSize(in android.hardware.common.Ashmem pModel, in int modelSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue nStrippedModelSize);
  int LsmGetUserDefinedKeywordApproxSize(in String keywordId, in String userId, in android.hardware.common.Ashmem pLanguageModel, in int langModelSize, in int maxPhonemeLength, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue pOutputSize);
  int LsmGetUserDefinedKeywordSize(in android.hardware.common.Ashmem pUserDefinedKeyword, in int modelSize, in String keywordId, in String userId, in vendor.qti.hardware.ListenSoundModelAidl.ListenEpdParams[] pEpdParameter, in int numUserRecording, in android.hardware.common.Ashmem[] pUserRecordings, in int[] recSizes, in android.hardware.common.Ashmem pLanguageModel, in int langModelSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue pOutputSize);
  int LsmGetUserKeywordModelSize(in android.hardware.common.Ashmem pKeywordModel, in int modelSize, in String keywordId, in String userId, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue nUserKeywordModelSize);
  int LsmGetUserNames(in android.hardware.common.Ashmem pSoundModel, in int modelSize, in char numInpUsers, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue numUsers, out vendor.qti.hardware.ListenSoundModelAidl.StringValue[] users);
  int LsmLoadConfParams(in byte[] pConfData, in int confDataSize);
  int LsmMergeModels(in char numModels, in android.hardware.common.Ashmem[] pModels, in int[] modelSizes, in int mergedModelSize, out android.hardware.common.Ashmem pMergedModel);
  int LsmParseDetectionEventData(in android.hardware.common.Ashmem pUserKeywordModel, in int modelSize, in vendor.qti.hardware.ListenSoundModelAidl.ListenEventPayload[] pEventPayload, out vendor.qti.hardware.ListenSoundModelAidl.ListenDetectionEventType[] pDetectionEvent);
  int LsmParseFromBigSoundModel(in android.hardware.common.Ashmem pSM3p0Model, in int bigModelSize, out android.hardware.common.Ashmem p1stStageModel, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue i1stStgModelSize, out android.hardware.common.Ashmem p2ndStageKWModel, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue i2ndStgKWModelSize, out android.hardware.common.Ashmem p2stStageVoPModel, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue i2ndStgVOPModelSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue indicator);
  int LsmQuerySoundModel(in android.hardware.common.Ashmem pSoundModel, in int modelSize, out vendor.qti.hardware.ListenSoundModelAidl.ListenSoundModelInfo[] pListenSoundModelInfo);
  int LsmReleaseSoundModelHeader(in vendor.qti.hardware.ListenSoundModelAidl.ListenSoundModelHeader[] pListenSoundModelHeader);
  int LsmSmlGet(in vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel[] pSmlModel, in int configId, in byte[] pConfig, in int configSize, out vendor.qti.hardware.ListenSoundModelAidl.ByteValue[] pOutConfig, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue configFilledSize);
  int LsmSmlGetReqMem(in int memStructId, in byte[] memStruct, in int memStructSize, in int configId, in byte[] configStruct, in int configStructSize);
  int LsmSmlGet_v2(in vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel pSmlModel, in int configId, in byte[] pConfig, in int configSize, out vendor.qti.hardware.ListenSoundModelAidl.ByteValue[] pOutConfig, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue configFilledSize);
  int LsmSmlInit(in int configId, in byte[] pConfig, in int configSize, in byte[] pStaticMemory, in int staticMemorySize, out vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel[] pSmlModel);
  int LsmSmlInit_v2(in int configId, in byte[] pConfig, in int configSize, in android.hardware.common.Ashmem pStaticMemory, in int staticMemorySize, out vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel pSmlModel);
  int LsmSmlProcess(in vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel[] pSmlModel, in byte[] pInputData, in int inputDataSize, in int inputDataId, out vendor.qti.hardware.ListenSoundModelAidl.ByteValue[] pOutputData, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue outputDataSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue outputDataId);
  int LsmSmlProcess_v2(in vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel pSmlModel, in byte[] pInputData, in int inputDataSize, in int inputDataId, out vendor.qti.hardware.ListenSoundModelAidl.ByteValue[] pOutputData, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue outputDataSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue outputDataId);
  int LsmSmlSet(in vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel[] pSmlModel, in int configId, in byte[] pConfig, in int configSize);
  int LsmSmlSet_v2(in vendor.qti.hardware.ListenSoundModelAidl.ListenSmlModel pSmlModel, in int configId, in byte[] pConfig, in int configSize);
  int LsmSortKeywordStatesOfUserKeywordModel(in android.hardware.common.Ashmem pInputModel, in int inpModelSize, in int sortedModelSize, out android.hardware.common.Ashmem pSortedModel);
  int LsmStripUserKeywordModel(in android.hardware.common.Ashmem pModel, in int modelSize, out android.hardware.common.Ashmem pStrippedModel, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue strippedModelSize);
  int LsmTuneUserDefinedKeywordModelThreshold(in android.hardware.common.Ashmem pUserDefinedKeyword, in int modelSize, in String keywordId, in android.hardware.common.Ashmem pUserRecording, in int recSize, out android.hardware.common.Ashmem pOutputUserDefinedKeyword, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue outModelSize);
  int LsmVerifyUserRecording(in android.hardware.common.Ashmem pKeywordModel, in int modelSize, in String keywordId, in vendor.qti.hardware.ListenSoundModelAidl.ListenEpdParams[] pEpdParameter, in android.hardware.common.Ashmem pUserRecording, in int recSize, out vendor.qti.hardware.ListenSoundModelAidl.IntegerValue confidenceLevel);
  int LsmVerifyUserRecordingExt(in android.hardware.common.Ashmem pKeywordModel, in int modelSize, in String keywordId, in vendor.qti.hardware.ListenSoundModelAidl.ListenEpdParams[] pEpdParameter, in android.hardware.common.Ashmem pUserRecording, in int recSize, in int isNoisySample, out vendor.qti.hardware.ListenSoundModelAidl.ListenQualityCheckResult[] pQualityCheckResult);
}
