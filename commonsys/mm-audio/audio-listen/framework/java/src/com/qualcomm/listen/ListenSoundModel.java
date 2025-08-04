/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.listen;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.annotation.NonNull;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SharedMemory;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.hardware.common.Ashmem;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.ErrnoException;
import android.util.Log;

import vendor.qti.hardware.ListenSoundModelAidl.*;
import vendor.qti.hardware.ListenSoundModelAidl.IListenSoundModel;
import com.qualcomm.listen.ListenTypes.*;


/**
 * ListenSoundModel is a class of helper API to QTI's
 * ListenEngine to be used with SoundModel.
 * <p>
 * No events are generated due to calls to these SoundModel
 * methods so no callback is need for this class.
 */
public class ListenSoundModel {
    private final static String TAG = "ListenSoundModel";
    private static IListenSoundModel mListenAidlService = null;

    // Set within SoundModelInfo
    /** SoundModel type not known */
    private static final int UNKNOWN_SOUNDMODEL_TYPE = 0;
    /** SoundModel is Snapdragon Voice Activation format */
    private static final int SVA_SOUNDMODEL_TYPE = 1;

    /** Version number: 2 MSBytes major number, 2 LSBytes minor number
     *  For non-SVA SoundModels version will be unknown */
    private static final int VERSION_UNKNOWN    = 0;
    /** Version 1.0 */
    private static final int VERSION_0100       = 0x0100;
    /** Version 2.0 */
    private static final int VERSION_0200       = 0x0200;

    /** Indicates Pre defined keyword (PDK)  */
    private static final int SVA_KEYWORD_TYPE_PDK = 2;
    /** Indicates User defined keyword (UDK) */
    private static final int SVA_KEYWORD_TYPE_UDK = 3;

    private static Map<FileDescriptor, SharedMemory> fd_mem =
                               new HashMap<FileDescriptor, SharedMemory>();

    private static final ListenSoundModelProxyDeathRecipient mLSMProxyDeathRecipient =
        new ListenSoundModelProxyDeathRecipient();
    private static final String lsmAidlServiceName =
          "vendor.qti.hardware.ListenSoundModelAidl.IListenSoundModel/default";
// -----------------------------------------------------------------------
// Public Methods
// -----------------------------------------------------------------------

    // load the Listen JNI library
    static {

        if (ServiceManager.isDeclared(lsmAidlServiceName)) {
            connectLSMService();
        } else {
            Log.d(TAG, "LSM Aidl service is not registered, loading liblistenjni.qti");
            System.loadLibrary("listenjni.qti");
        }
    }

    /**
     * Constructor
     */
    public ListenSoundModel() {}

    public static void connectLSMService() {
        Log.i(TAG, "Connecting to default ListenSoundModelAidl service");
        try {
            IBinder binder = ServiceManager.waitForService(lsmAidlServiceName);
            mListenAidlService = vendor.qti.hardware.ListenSoundModelAidl.IListenSoundModel.Stub.asInterface(binder);
            binder.linkToDeath(mLSMProxyDeathRecipient, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to connect to ListenSoundModel AIDL service");
        }
        Log.i(TAG, "Connected to ListenSoundModelAidl service");
    }

    private static class ListenSoundModelProxyDeathRecipient implements IBinder.DeathRecipient {
        public void binderDied() {
            Log.i(TAG, "LSM AIDL service died, connect to new one");
            mListenAidlService = null;
            connectLSMService();
        }
    }

    private static SharedMemory createSharedMemory(int size) {

        SharedMemory sFD = null;

        try {
            sFD = SharedMemory.create("", size);
        } catch (ErrnoException e) {
            Log.e(TAG, "createSharedMemory: ERROR: Failed to create Sharedmemory : ", e);
        }

        if (sFD == null || sFD.getSize() != size) {
            Log.e(TAG, "createSharedMemory: ERROR: Failed to allocate shared memory");
            sFD.close();
            return null;
        }

        return sFD;
    }

    public static Ashmem getDataInAshmemObj(ByteBuffer pData) {

        int ret = 0;
        int rDataSize = 0;
        Ashmem rAshmem;
        SharedMemory sharedFd = null;
        ByteBuffer bbf = null;
        String s;
        byte[] recieveSM;

        rAshmem = new Ashmem();

        Log.i(TAG, "getDataInAshmem:Enter.");
        if (pData == null) {
            Log.e(TAG, "getDataInAshmem: ERROR: Null ptr passed");
            return null;
        }

        rDataSize = pData.array().length - pData.arrayOffset();
        sharedFd = createSharedMemory(rDataSize);
        Log.i(TAG, "SharedFd : " + Integer.toString(sharedFd.getFileDescriptor().getInt$()));
        try {
            bbf = sharedFd.map(OsConstants.PROT_READ|OsConstants.PROT_WRITE, 0, rDataSize);
        } catch (ErrnoException e) {
            Log.e(TAG, "getDataInAshmem: ERROR: Failed to map Sharedmemory : ", e);
            sharedFd.close();
            return null;
        }
        pData.flip();
        pData.position(pData.arrayOffset() + pData.position());
        bbf.put(pData.array(), pData.position(), rDataSize);

        try {
            rAshmem.fd = ParcelFileDescriptor.dup(sharedFd.getFileDescriptor());
            rAshmem.size = rDataSize;
            fd_mem.put(rAshmem.fd.getFileDescriptor(), sharedFd);
            unmapSharedMemory(rAshmem.fd, bbf);
        } catch (IOException e) {
            Log.e(TAG, "getDataInAshmem: ERROR: Failed to get file descriptor : ", e);
            sharedFd.unmap(bbf);
            sharedFd.close();
            fd_mem.remove(rAshmem.fd);
            return null;
        }

        Log.i(TAG, "getDataInAshmem:Exit.");
        return rAshmem;
    }

    private static void unmapSharedMemory(ParcelFileDescriptor pFd, ByteBuffer bBuf) {

        FileDescriptor fd = pFd.getFileDescriptor();
        if (!fd_mem.containsKey(fd)) {
            Log.e(TAG, "unmapSharedMemory: ERROR: FD not found in cached map");
            return;
        }

        fd_mem.get(fd).unmap(bBuf);
    }

    private static void closeSharedMemory(Ashmem aShMem) {

        FileDescriptor fd = aShMem.fd.getFileDescriptor();
        if (!fd_mem.containsKey(fd)) {
            Log.e(TAG, "closeSharedMemory: ERROR: FD not found in cached map");
            return;
        }

        try {
            aShMem.fd.close();
        } catch (IOException e) {
            Log.e(TAG, "closeSharedMemory: ERROR: Failed to close ParcelFileDescriptor's sharedmemory : ", e);
            return;
        }

        aShMem.fd = null;
        aShMem.size = 0;

        fd_mem.get(fd).close();
        fd_mem.remove(fd);
        Log.i(TAG, "closeSharedMemory: Closing shared memory!!");
    }

    public static int extractSMInfoFromByteBuffer(ByteBuffer soundModel, SoundModelInfo soundModelInfo) {

        int ret = 0;
        ListenSoundModelInfo[] pListenSoundModelInfo = new ListenSoundModelInfo[1];
        Ashmem sAshmem;

        pListenSoundModelInfo[0] = new ListenSoundModelInfo();

        Log.i(TAG, "extractSMInfoFromByteArray::Enter.");
        if ((null == soundModel) || (null == soundModelInfo)) {
            Log.e(TAG, "extractSMInfoFromByteArray: ERROR: Null ptr passed");
            return ListenTypes.STATUS_EFAILURE;
        }

        sAshmem = getDataInAshmemObj(soundModel);
        if (sAshmem == null) {
            Log.e(TAG, "extractSMInfoFromByteArray: ERROR: Failed to get shared ashmem for sound model");
            return ListenTypes.STATUS_EFAILURE;
        }

        try {
            ret = mListenAidlService.LsmQuerySoundModel(sAshmem, (int)sAshmem.size, pListenSoundModelInfo);
        } catch (RemoteException e) {
            Log.e(TAG, "extractSMInfoFromByteArray: ERROR: Failure returned from server side");
            closeSharedMemory(sAshmem);
            throw e.rethrowAsRuntimeException();
        }

        soundModelInfo.type = pListenSoundModelInfo[0].type;
        soundModelInfo.version = pListenSoundModelInfo[0].version;
        soundModelInfo.size = pListenSoundModelInfo[0].size;

        closeSharedMemory(sAshmem);

        Log.i(TAG, "extractSMInfoFromByteArray::Exit.");
        return ret;
    }

    public static int getTypeVersion(ByteBuffer soundModel, SoundModelInfo soundModelInfo) {

        if (mListenAidlService == null)
            return nativeGetTypeVersion(soundModel, soundModelInfo);

        int ret = 0;

        Log.i(TAG, "getTypeVersioniAidl::Enter.");
        if ((null == soundModel) || (null == soundModelInfo)) {
            Log.e(TAG, "getTypeVersioniAidl: ERROR: Null ptr passed to ListenSoundModel.getTypeVersion");
            return ListenTypes.STATUS_EFAILURE;
        }
        if (!soundModel.hasArray()) {
            Log.e(TAG, "getTypeVersioniAidl: ERROR: returns false - no array");
            return ListenTypes.STATUS_EFAILURE;
        }

        ret = extractSMInfoFromByteBuffer(soundModel, soundModelInfo);

        if (soundModelInfo.type == 4) {
            soundModelInfo.version = VERSION_0200;
        }

        soundModelInfo.type = SVA_SOUNDMODEL_TYPE;
        Log.i(TAG, "getTypeVersioniAidl::Exit.");
        return ret;
    }

    public static int checkModelCompatibility(ByteBuffer soundModel,
                                   SMLMatchResult matchResult) {

        Log.i(TAG, "checkModelCompatibility: Enter.");

        int status = 0;
        ListenSmlModel[] lsm = new ListenSmlModel[1];
        lsm[0] = new ListenSmlModel();
        ListenModelType modelType_t = new ListenModelType();
        ByteValue[] ret;
        IntegerValue mSize = new IntegerValue();
        mSize.value = 6 * 4; //6 is total data members of SMLMatchResult class, and 4 is size of int
        /* Following params are dummy param, using them just to pass to the API, as this API will replace LsmSmlGet() API,
         * in later QSSI upgrade, hence, making param list intact as original LsmSmlGet
         */
        byte[] config = new byte[mSize.value];

        if (mListenAidlService == null) {
            Log.e(TAG, "checkModelCompatibility: LSM AIDL service is not yet registered, returing!!!");
            return ListenTypes.STATUS_EFAILURE;
        }

        if (soundModel == null) {
            Log.e(TAG, "checkModelCompatibility: ERROR: Null ptr passed to ListenSoundModel.getCompatibilityCheck");
            return ListenTypes.STATUS_EFAILURE;
        }

        if (!soundModel.hasArray()) {
            Log.e(TAG, "checkModelCompatibility: ERROR: returns false - no array");
            return ListenTypes.STATUS_EFAILURE;
        }

        modelType_t.data = getDataInAshmemObj(soundModel);
        if (modelType_t.data == null) {
            Log.e(TAG, "checkModelCompatibility: ERROR: Failed to get sound model in ashmem object");
            return ListenTypes.STATUS_EFAILURE;
        }

        lsm[0].setModelType(modelType_t);

        ret = new ByteValue[mSize.value];
        for (int i = 0; i < mSize.value; i++) {
            ret[i] = new ByteValue();
            ret[i].value = (byte)0x0;
        }

        try {
            status = mListenAidlService.LsmSmlGet(lsm,
                         SmlConfigStructureId.SML_CONFIG_ID_COMPATIBILITY_CHECK,
                         config, config.length, ret, mSize);
        } catch (RemoteException e) {
            Log.e(TAG, "checkModelCompatibility: ERROR: Failure returned from server side");
            closeSharedMemory(modelType_t.data);
            throw e.rethrowAsRuntimeException();
        }

        if (status != 0) {
            Log.e(TAG, "checkModelCompatibility: ERROR: Incompatible sound model!!");
            closeSharedMemory(modelType_t.data);

            matchResult.fSvaMatchResult = ret[0].value;
            matchResult.fSvaInternalMatchResult = ret[1].value;
            matchResult.sPdkMatchResult = ret[2].value;
            matchResult.sUdkMatchResult = ret[3].value;
            matchResult.sRnnMatchResult = ret[4].value;
            matchResult.sUvMatchResult = ret[5].value;

            return ConvertReturnStatus(status);
        }

        closeSharedMemory(modelType_t.data);
        return ListenTypes.STATUS_SUCCESS;
    }

    public static int getSMInfoV2(ByteBuffer soundModel, SVASoundModelInfo soundModelInfo) {

        int numKeyphrasesHeader = 0;
        int numUsersHeader = 0;
        int numKeyphrases = 0;
        int numActivePairs = 0;
        int ret = 0;
        IntegerValue numKeywords;
        IntegerValue numUsers;
        StringValue keywordPhrases[];
        StringValue userNames[];
        Ashmem ashSoundModel;
        KeywordUserCounts userCount;
        KeywordInfo[] keywordInfo;
        String[] retUsesrNames;

        Log.i(TAG, "getSMInfoV2: Enter.");

        SoundModelHeader modelHeader = new SoundModelHeader();
        numKeywords = new IntegerValue();
        numUsers = new IntegerValue();

        ashSoundModel = getDataInAshmemObj(soundModel);
        if (ashSoundModel == null) {
            Log.e(TAG, "getSMInfoV2: ERROR: Failed to get sound model in ashmem object");
            return ListenTypes.STATUS_EFAILURE;
        }

        ret = lsm_getSoundModelHeader(ashSoundModel, modelHeader);

        if (ret != 0) {
            Log.e(TAG, "getSMInfoV2: ERROR: getSoundModelHeader() failed");
            closeSharedMemory(ashSoundModel);
            return ret;
        } else if (modelHeader.numKeywords == 0) {
            Log.e(TAG, "getSMInfoV2: ERROR: Returned 0 numKeywords");
            closeSharedMemory(ashSoundModel);
            return ListenTypes.STATUS_EFAILURE;
        }

        numKeywords.value = modelHeader.numKeywords; //Just to initialize it, not used this initialization anywhere
        numKeyphrasesHeader = modelHeader.numKeywords;
        numUsersHeader = modelHeader.numUsers;
        numActivePairs =  modelHeader.numActiveUserKeywordPairs;
        keywordPhrases = new StringValue[numKeyphrasesHeader];
        userNames = new StringValue[numUsersHeader];

        Log.i(TAG, "getSMInfoV2: numKeyphrasesHeader = " + Integer.toString(numKeyphrasesHeader) +
                   ", numUsersHeader = " + Integer.toString(numUsersHeader) +
                   ",  numPairs = " +numActivePairs);

        try {
            ret = mListenAidlService.LsmGetKeywordPhrases(ashSoundModel, (int)ashSoundModel.size,
                              (char)numKeyphrasesHeader, numKeywords, keywordPhrases);
        } catch(RemoteException e) {
            Log.e(TAG, "getSMInfoV2: ERROR: Failure returned from server");
            closeSharedMemory(ashSoundModel);
            throw e.rethrowAsRuntimeException();
        }

        if (ret != 0) {
            Log.e(TAG, "getSMInfoV2: ERROR getKeywordPhrases() failed");
            closeSharedMemory(ashSoundModel);
            return ListenTypes.STATUS_EFAILURE;
        } else if (numKeyphrasesHeader != numKeywords.value) {
            Log.e(TAG, "getSMInfoV2: ERROR returned numKeyphrases  is not equal to no. returned by the SML header");
            closeSharedMemory(ashSoundModel);
            return ListenTypes.STATUS_EFAILURE;
        }

        numUsers.value = numUsersHeader;
        if (numUsers.value > 0) {
            try {
                ret = mListenAidlService.LsmGetUserNames(ashSoundModel, (int)ashSoundModel.size,
                                  (char)numKeyphrasesHeader, numUsers, userNames);
            } catch(RemoteException e) {
                Log.e(TAG, "getSMInfoV2: ERROR: Failure returned from server");
                closeSharedMemory(ashSoundModel);
                throw e.rethrowAsRuntimeException();
            }

            if (ret != 0) {
                Log.e(TAG, "getSMInfoV2: ERROR getUserNames() failed");
                closeSharedMemory(ashSoundModel);
                return ConvertReturnStatus(ret);
            } else if (numUsersHeader != numUsers.value) {
                Log.e(TAG, "getSMInfoV2: ERROR returned numUsers is not equal to the no. returned by the SML header");
                closeSharedMemory(ashSoundModel);
                return ConvertReturnStatus(ret);
            }
        }

        //Fill soundModelInfo object from returned data of above ipc calls
        userCount = new KeywordUserCounts();
        keywordInfo = new KeywordInfo[numKeyphrasesHeader];
        soundModelInfo.userNames = new String[numUsers.value];

        for (int kw = 0; kw < numKeywords.value; ++kw) {

            keywordInfo[kw] = new KeywordInfo();
            keywordInfo[kw].keywordPhrase = keywordPhrases[kw].value;

            if (modelHeader.numUsersSetPerKw != null &&
                modelHeader.numUsersSetPerKw[kw] > 0 &&
                modelHeader.userKeywordPairFlags != null) {
                keywordInfo[kw].activeUsers = new String[modelHeader.numUsersSetPerKw[kw]];
                for (int u = 0, activeUsersforThisKW = 0; u < numUsers.value &&
                                activeUsersforThisKW <  modelHeader.numUsersSetPerKw[kw]; ++u) {
                    if (modelHeader.userKeywordPairFlags[u] == null) {
                        Log.e(TAG, "getSMInfoV2: Error: modelHeader.userKeywordPairFlags's entry is NULL");
                        closeSharedMemory(ashSoundModel);
                        return ListenTypes.STATUS_EFAILURE;
                    }
                    if (modelHeader.userKeywordPairFlags[u][kw] != 0) {
                        keywordInfo[kw].activeUsers[activeUsersforThisKW++] = userNames[u].value;
                    }
                }
            } else {
                keywordInfo[kw].activeUsers = new String[0];
            }
        }

        userCount.numKeywords = (short)numKeywords.value;
        userCount.numUsers = (short)numUsers.value;
        userCount.numUserKWPairs = (short)numActivePairs;

        soundModelInfo.keywordInfo = keywordInfo;
        soundModelInfo.counts = userCount;
        for (int u = 0; u < numUsers.value; u++)
            soundModelInfo.userNames[u] = userNames[u].value;

        for (int kw = 0; kw < numKeywords.value; ++kw) {
            Log.i(TAG, "Keyphrase ID : " + soundModelInfo.keywordInfo[kw].keywordPhrase);
            if (modelHeader.numUsersSetPerKw != null &&
                    modelHeader.numUsersSetPerKw[kw] > 0 &&
                    modelHeader.userKeywordPairFlags != null) {
                for (int u = 0, activeUsersforThisKW = 0; u < numUsers.value &&
                                 activeUsersforThisKW <  modelHeader.numUsersSetPerKw[kw]; ++u) {
                    if (modelHeader.userKeywordPairFlags[u][kw] != 0) {
                        Log.i(TAG, "Active user ID :" + soundModelInfo.keywordInfo[kw].activeUsers[activeUsersforThisKW++]);
                    }
                }
            }
        }

        ret = lsm_releaseSoundModelHeader(modelHeader);

        if (ret != 0)
            Log.e(TAG, "getSMInfoV2: ERROR: releaseSoundModelHeader() failed");

        closeSharedMemory(ashSoundModel);
        Log.i(TAG, "getSMInfoV2: Exit.");
        return ret;

    }

    public static int lsm_getSoundModelHeader(Ashmem soundModel, SoundModelHeader modelHeader) {

        int ret = 0;
        Log.i(TAG, "lsm_getSoundModelHeader: Enter.");

        ListenSoundModelHeader[] pListenSoundModelHeader = new ListenSoundModelHeader[1];
        pListenSoundModelHeader[0] = new ListenSoundModelHeader();

        try {
            ret = mListenAidlService.LsmGetSoundModelHeader(soundModel, (int)soundModel.size, pListenSoundModelHeader);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }

        modelHeader.numKeywords = pListenSoundModelHeader[0].numKeywords;
        modelHeader.numUsers = pListenSoundModelHeader[0].numUsers;
        modelHeader.numActiveUserKeywordPairs = pListenSoundModelHeader[0].numActiveUserKeywordPairs;
        modelHeader.isStripped = pListenSoundModelHeader[0].isStripped;
        modelHeader.model_indicator = pListenSoundModelHeader[0].modelIndicator;

        modelHeader.langPerKw = new int[pListenSoundModelHeader[0].langPerKw.length];
        for (int i =0; i < pListenSoundModelHeader[0].langPerKw.length; ++i) {
            modelHeader.langPerKw[i] = pListenSoundModelHeader[0].langPerKw[i];
        }

        modelHeader.numUsersSetPerKw = new int[pListenSoundModelHeader[0].numUsersSetPerKw.length];
        for (int i =0; i < pListenSoundModelHeader[0].numUsersSetPerKw.length; ++i) {
            modelHeader.numUsersSetPerKw[i] = pListenSoundModelHeader[0].numUsersSetPerKw[i];
        }

        modelHeader.isUserDefinedKeyword = new boolean[pListenSoundModelHeader[0].isUserDefinedKeyword.length];
        for (int i =0; i < pListenSoundModelHeader[0].isUserDefinedKeyword.length; ++i) {
            modelHeader.isUserDefinedKeyword[i] = pListenSoundModelHeader[0].isUserDefinedKeyword[i];
        }

        modelHeader.userKeywordPairFlags = new int[modelHeader.numUsers][modelHeader.numKeywords];
        for (int u = 0; u < modelHeader.numUsers; ++u) {
            for (int k = 0; k < modelHeader.numKeywords; ++k) {
                modelHeader.userKeywordPairFlags[u][k] = pListenSoundModelHeader[0].userKeywordPairFlags.get(u).charAt(k);
            }
        }
        Log.i(TAG, "lsm_getSoundModelHeader: Exit.");
        return ret;
    }

    public static int lsm_releaseSoundModelHeader(SoundModelHeader pModelHeader) {

        Log.i(TAG, "lsm_releaseSoundModelHeader: Enter.");
        int ret = 0;
        ListenSoundModelHeader[] rListenSoundModelHeader = new ListenSoundModelHeader[1];
        rListenSoundModelHeader[0] = new ListenSoundModelHeader();

        rListenSoundModelHeader[0].numKeywords = (char)(pModelHeader.numKeywords);
        rListenSoundModelHeader[0].numUsers = (char)(pModelHeader.numUsers);
        rListenSoundModelHeader[0].numActiveUserKeywordPairs = (char)(pModelHeader.numActiveUserKeywordPairs);
        rListenSoundModelHeader[0].isStripped = pModelHeader.isStripped;
        rListenSoundModelHeader[0].modelIndicator = (char)(pModelHeader.model_indicator);

        rListenSoundModelHeader[0].langPerKw = new char[pModelHeader.langPerKw.length];
        for (int i =0; i < pModelHeader.langPerKw.length; ++i)
            rListenSoundModelHeader[0].langPerKw[i] = (char)(pModelHeader.langPerKw[i]);

        rListenSoundModelHeader[0].numUsersSetPerKw = new char[pModelHeader.numUsersSetPerKw.length];
        for (int i =0; i < pModelHeader.numUsersSetPerKw.length; ++i)
            rListenSoundModelHeader[0].numUsersSetPerKw[i] = (char)(pModelHeader.numUsersSetPerKw[i]);

        rListenSoundModelHeader[0].isUserDefinedKeyword = new boolean[pModelHeader.isUserDefinedKeyword.length];
        for (int i =0; i < pModelHeader.isUserDefinedKeyword.length; ++i)
            rListenSoundModelHeader[0].isUserDefinedKeyword[i] = pModelHeader.isUserDefinedKeyword[i];

        rListenSoundModelHeader[0].userKeywordPairFlags = new ArrayList<String>();
        for (int u = 0; u < pModelHeader.numUsers; ++u) {
            String temp = "";
            for (int k = 0; k < pModelHeader.numKeywords; ++k) {
                temp +=  (char)(pModelHeader.userKeywordPairFlags[u][k]);
            }
            rListenSoundModelHeader[0].userKeywordPairFlags.add(temp);
        }

        try {
            ret = mListenAidlService.LsmReleaseSoundModelHeader(rListenSoundModelHeader);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }

        Log.i(TAG, "lsm_releaseSoundModelHeader: Exit.");
        return ret;
    }

    /**
     * Provides Sound Model Library (SML) version.
     *
     * @param version [out] version information
     * @return
     *        <br> STATUS_SUCCESS
     *        <br> STATUS_EFUNCTION_NOT_IMPLEMENTED
     *        <br> STATUS_EBAD_PARAM
     *        <br> STATUS_EFAILURE
     */
    public static native int nativeGetSMLVersion(SMLVersion version);

    public static int getSMLVersion(SMLVersion version) {

        Log.i(TAG, "getSMLVersion: Enter.");

        if (mListenAidlService == null)
            return nativeGetSMLVersion(version);

        int status = 0;
        byte[] config = new byte[8];
        ByteValue[] ret = new ByteValue[8];
        IntegerValue mSize = new IntegerValue();
        ListenSmlModel[] lsm = new ListenSmlModel[0];

        ret[0] = new ByteValue();
        mSize.value = 0;

        if (version == null) {
            Log.e(TAG, "getSMLVersion: ERROR: Null input params");
            return ListenTypes.STATUS_EFAILURE;
        }

        try {
            status = mListenAidlService.LsmSmlGet(lsm, SmlConfigStructureId.SML_CONFIG_ID_VERSION,
                                                config, config.length, ret, mSize);
        } catch (RemoteException e) {
            Log.e(TAG, "getSMLVersion: ERROR: Failure returned from server side");
            throw e.rethrowAsRuntimeException();
        }
        version.version = 0;
        if (status == 0 && mSize.value > 0) {
            for (int i = 0; i < mSize.value; ++i) {
                version.version += ((long)(ret[i].value & 0xff) << (i * 8));
            }
            Log.i(TAG, "getSMLVersionAidl: Returning version value :" + version.version + " ");
        }

        Log.i(TAG, "getSMLVersion: Exit.");
        return status;

    }

    /**
     * Verifies that User Recording contains the spoken keyword
     * (as defined in the given User-Independent SVA 1.0 model)
     * and is of good enough quality.
     * <p>
     * A returned confidence level greater than 70 indicates that the
     * phrase in the recording sufficiently matches the keyword
     * defined in given model.
     * Such a recording can be can be used for extending a SoundModel.
     *
     * @param userIndependentModel [in] contains User-Independent
     *        model data
     * @param userRecording [in] a single recording of user speaking
     *        keyword
     *
     * @return percent (0-100) confidence level.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     *
     * @deprecated use {@link verifyUserRecording(ByteBuffer, String, ShortBuffer)}
     * instead.
     */
@Deprecated
     public static int verifyUserRecording(
            ByteBuffer       userIndependentModel,
            ShortBuffer      userRecording)
    {
         // calls SVA 2.0 version of verifyUserRecording() with null Keyword string.
         // This results in UserData being compared to the first Keyword of the SoundModel.
         int status = nativeVerifyUserRecording(userIndependentModel, null, userRecording);
         return status;
     }

    /**
     * Verifies that a User Recording contain a specific keyword phrase in the given
     * SVA 2.0 (and above) SoundModel, and that it is of good-enough quality to be
     * used to when calling {@link extend(ByteBuffer, String, String, int,
     *     ShortBuffer[], ByteBuffer, ConfidenceData)}.
     * <p>
     * A returned confidence level greater than 70 indicates that the
     * phrase in the recording sufficiently matches the keyword
     * defined in given model.
     *
     * @param soundModel [in] contains SoundModel containing predefined keywords
     * @param keywordPhrase [in] name of keyword in SM to be extended
     * @param userRecording [in] a single recording of user speaking keyword
     *
     * @return percent (0-100) confidence level.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EKEYWORD_NOT_IN_SOUNDMODEL
     *        <br>   STATUS_ENO_SPEACH_IN_RECORDING
     *        <br>   STATUS_ETOO_MUCH_NOISE_IN_RECORDING
     */
     public static native int nativeVerifyUserRecording(
            ByteBuffer       soundModel,
            String           keywordPhrase,
            ShortBuffer      userRecording);

     public static int verifyUserRecording(ByteBuffer pSoundModel,
                                           String pKeyphrase,
                                           ShortBuffer pUserRecording) {

         Log.i(TAG, "verifyUserRecording: Enter.");
         if (mListenAidlService == null)
             return nativeVerifyUserRecording(pSoundModel, pKeyphrase,
                                              pUserRecording);

         int status = 0;
         Ashmem rSoundModel;
         Ashmem rUserRecording;
         short[] shortRec;
         IntegerValue conf = new IntegerValue();
         ListenEpdParams[] epd = new ListenEpdParams[0];
         ByteBuffer bBuffer = ByteBuffer.allocate(pUserRecording.array().length * 2);

         if (pKeyphrase == null) {
             Log.e(TAG, "verifyUserecording: ERROR: Null keyPhrase is passed");
             return ListenTypes.STATUS_EBAD_PARAM;
         }
         if (pSoundModel == null) {
             Log.e(TAG, "verifyUserRecording: ERROR: Null Sound model is passed");
             return ListenTypes.STATUS_EBAD_PARAM;
         }

         if (pUserRecording == null || !pUserRecording.hasArray()) {
             Log.e(TAG, "verifyUserRecording: ERROR: Null recordings is passed");
             return ListenTypes.STATUS_EBAD_PARAM;
         }

         rSoundModel = getDataInAshmemObj(pSoundModel);
         if (rSoundModel == null) {
             Log.e(TAG, "verifyUserRecording: ERROR: Failed to get Sound model");
             return ListenTypes.STATUS_EBAD_PARAM;
         }

         shortRec = pUserRecording.array();
         for (int i = 0, j = 0; i < pUserRecording.array().length; ++i) {
             bBuffer.put(j++, (byte)(shortRec[i] & 0xff));
             bBuffer.put(j++, (byte)((shortRec[i] >> 8)& 0xff));
         }

         bBuffer.rewind();
         rUserRecording = getDataInAshmemObj(bBuffer);
         if (rUserRecording == null) {
             Log.e(TAG, "verifyUserRecording: ERROR: Failed to get Sound model");
             return ListenTypes.STATUS_EBAD_PARAM;
         }

         try {
             status = mListenAidlService.LsmVerifyUserRecording(rSoundModel,
                                  (int)rSoundModel.size, pKeyphrase, epd, rUserRecording,
                                  (int)rUserRecording.size, conf);
         } catch (RemoteException e) {
             Log.e(TAG, "verifyUserRecording: ERROR: in LsmVerifyUserRecording call");
             closeSharedMemory(rSoundModel);
             closeSharedMemory(rUserRecording);
             throw e.rethrowAsRuntimeException();
         }
         closeSharedMemory(rSoundModel);
         closeSharedMemory(rUserRecording);
         Log.i(TAG, "verifyUserRecording: Exit, Returning confidence level " +
                     Integer.toString(conf.value));

         return conf.value;
     }


    /**
     * Verifies that a User Recording contain a specific keyword
     * phrase in the given SoundModel, and that it is of good-enough
     * quality to be used to when calling {@link extend(ByteBuffer,
     * String, String, int, ShortBuffer[], ByteBuffer,
     * ConfidenceData)}. Accepts the recordings captured in clean
     * and noisy environnment and provides the feedback on the
     * quality of recording.
     * <p>
     * A returned confidence level {@link QualityCheckInfo} greater
     * than certain threshold based on the sound model indicates
     * that the phrase in the recording sufficiently matches the
     * keyword defined in given model.
     *
     * @param soundModel [in] contains SoundModel containing predefined keywords
     * @param keywordPhrase [in] name of keyword in Sound model to be extended
     * @param userRecording [in] a single recording of user speaking keyword
     * @param isNoisySample [in] input recording is capatured in
     *        noisy (true) or clean (false) environment
     * @param diagnostics [out] contains recording quality check
     *        result information
     *
     * @return STATUS_SUCCESS for success.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EKEYWORD_NOT_IN_SOUNDMODEL
     *        <br>   STATUS_ENO_SPEACH_IN_RECORDING
     *        <br>   STATUS_ETOO_MUCH_NOISE_IN_RECORDING
     *        <br>   STATUS_ECHOPPED_SAMPLE
     *        <br>   STATUS_ECLIPPED_SAMPLE
     *        <br>   STATUS_EFAILURE
     */
    public static native int nativeVerifyUserRecordingQuality(
            ByteBuffer       soundModel,
            String           keywordPhrase,
            ShortBuffer      userRecording,
            boolean          isNoisySample,
            QualityCheckInfo diagnostics);

     public static int verifyUserRecordingQuality(ByteBuffer pSoundModel,
                          String pKeywordPhrase, ShortBuffer pUserRecording,
                          boolean pIsNoisySample, QualityCheckInfo pDiagnostics) {

         Log.i(TAG, "verifyUserRecordingQuality: Enter.");

         if (mListenAidlService == null)
             return nativeVerifyUserRecordingQuality(pSoundModel, pKeywordPhrase,
                               pUserRecording, pIsNoisySample, pDiagnostics);
         int status = 0;
         Ashmem rSoundModel;
         Ashmem rUserRecording;
         short[] shortRec;
         ListenEpdParams[] epd = new ListenEpdParams[0];
         ListenQualityCheckResult[] rQcCheck = new ListenQualityCheckResult[1];
         ByteBuffer bBuffer = ByteBuffer.allocate(pUserRecording.array().length * 2);

         if (pKeywordPhrase == null) {
             Log.e(TAG, "verifyUserecordingQuality: ERROR: Null keyPhrase is passed");
             return ListenTypes.STATUS_EBAD_PARAM;
         }
         if (pSoundModel == null) {
             Log.e(TAG, "verifyUserRecordingQuality: ERROR: Null Sound model is passed");
             return ListenTypes.STATUS_EBAD_PARAM;
         }
         if (pUserRecording == null || !pUserRecording.hasArray()) {
             Log.e(TAG, "verifyUserRecordingQuality: ERROR: Null recordings is passed");
             return ListenTypes.STATUS_EBAD_PARAM;
         }

         rSoundModel = getDataInAshmemObj(pSoundModel);
         if (rSoundModel == null) {
             Log.e(TAG, "verifyUserRecordingQuality: ERROR: Failed to get Sound model");
             return ListenTypes.STATUS_EBAD_PARAM;
         }

         shortRec = pUserRecording.array();
         for (int i = 0, j = 0; i < pUserRecording.array().length; ++i) {
             bBuffer.put(j++, (byte)(shortRec[i] & 0xff));
             bBuffer.put(j++, (byte)((shortRec[i] >> 8)& 0xff));
         }
         bBuffer.rewind();
         rUserRecording = getDataInAshmemObj(bBuffer);
         if (rUserRecording == null) {
             Log.e(TAG, "verifyUserRecordingQuality: ERROR: Failed to get memory for user recording");
             closeSharedMemory(rSoundModel);
             return ListenTypes.STATUS_EBAD_PARAM;
         }

         rQcCheck[0] = new ListenQualityCheckResult();

         try {
             status = mListenAidlService.LsmVerifyUserRecordingExt(rSoundModel,
                         (int)rSoundModel.size, pKeywordPhrase, epd, rUserRecording,
                         (int)rUserRecording.size, pIsNoisySample ? 1 : 0, rQcCheck);
         } catch (RemoteException e) {
             Log.e(TAG, "verifyUserRecordingQuality: ERROR: Failure returned from server side");
             closeSharedMemory(rSoundModel);
             closeSharedMemory(rUserRecording);
             throw e.rethrowAsRuntimeException();
         }

         pDiagnostics.isLowSnrSet             = rQcCheck[0].isLowSnrSet;
         pDiagnostics.epdSnr                  = rQcCheck[0].epdSnr;
         pDiagnostics.epdStart                = rQcCheck[0].epdStart;
         pDiagnostics.epdEnd                  = rQcCheck[0].epdEnd;
         pDiagnostics.exactEpdStart           = rQcCheck[0].exactEpdStart;
         pDiagnostics.exactEpdEnd             = rQcCheck[0].exactEpdEnd;
         pDiagnostics.keywordConfidenceLevel  = (short)rQcCheck[0].keywordConfidenceLevel;
         pDiagnostics.epdPeakLevel            = rQcCheck[0].epdPeakLevel;
         pDiagnostics.epdRmsLevel             = rQcCheck[0].epdRmsLevel;
         pDiagnostics.n_epdSamplesClipping    = rQcCheck[0].n_epdSamplesClipping;
         pDiagnostics.keywordStart            = rQcCheck[0].keywordStart;
         pDiagnostics.keywordEnd              = rQcCheck[0].keywordEnd;

         Log.i(TAG, "Recieved conf level : " + Integer.toString(rQcCheck[0].keywordConfidenceLevel) +
                  ", returning conf level : " + Short.toString(pDiagnostics.keywordConfidenceLevel));

         closeSharedMemory(rSoundModel);
         closeSharedMemory(rUserRecording);
         Log.i(TAG, "verifyUserRecordingQuality: Exit.");
         return status;
     }

    /**
     * Set Algo specific Sound Model configuration tuning parameters
     * prior to training the sound model.
     * <p>
     *
     * @param pLangModel [in] Language model to be used for UDK training,
                for PDK training this can be null, this apk is introduced
                from v15
     * @param keywordType [in] SVA_KEYWORD_TYPE_PDK or
     *          SVA_KEYWORD_TYPE_UDK
     * @param tuningPayload [out] opaque payload interpreted by algo.
     *        Caller need to manage padding between variables of
     *        any structured object formatted into tuning payload.
     *
     * @return STATUS_SUCCESS for success.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     */
     public static native int nativeSetSoundModelTuningParams(
         int keywordType,
         ByteBuffer tuningPayload);

     public static int SetSoundModelTuningParams(int keywordType,
                                                 ByteBuffer tuningPayload) {

         if (mListenAidlService == null)
             return nativeSetSoundModelTuningParams(keywordType, tuningPayload);

         return SetSoundModelTuningParams(null, keywordType, tuningPayload);
     }

     public static int SetSoundModelTuningParams(
                  ByteBuffer pLangModel,int keywordType,
                  ByteBuffer tuningPayload) {

         Log.i(TAG, "SetSoundModelTuningParams: Enter.");

         if (mListenAidlService == null)
             return nativeSetSoundModelTuningParams(keywordType, tuningPayload);

         int status = 0;
         int configId;
         int payloadSize;
         byte[] tPayload;
         ListenSmlModel[] lsm;
         ListenModelType modelType_t = new ListenModelType();
         ByteBuffer tempBuffer;
         Ashmem rLangModel = null;

         if (!tuningPayload.hasArray()) {
             Log.e(TAG, "SetSoundModelTuningParamsAidl: ERROR: no array fo tuningPayload");
             return ListenTypes.STATUS_EBAD_PARAM;
         }
         payloadSize = tuningPayload.array().length - tuningPayload.arrayOffset();
         tPayload = new byte[payloadSize];
         for (int i = 0; i < payloadSize; ++i)
             tPayload[i] = tuningPayload.array()[i + tuningPayload.arrayOffset()];

         if (keywordType == SVA_KEYWORD_TYPE_PDK) {
             configId = SmlConfigStructureId.SML_CONFIG_ID_SET_PDK_PARAMS;
             lsm = new ListenSmlModel[0];
         } else if (keywordType == SVA_KEYWORD_TYPE_UDK) {
             configId = SmlConfigStructureId.SML_CONFIG_ID_SET_UDK_PARAMS;
             lsm = new ListenSmlModel[1];
             lsm[0] = new ListenSmlModel();
             if (pLangModel == null) {
                ByteBuffer dummyModel = ByteBuffer.allocate(8);
                modelType_t.data = getDataInAshmemObj(dummyModel);
             } else {
                modelType_t.data = getDataInAshmemObj(pLangModel);
             }
             if (modelType_t.data == null) {
                 Log.e(TAG, "extend: ERROR: Failed to get language model");
                 return ListenTypes.STATUS_EBAD_PARAM;
             }
             lsm[0].setModelType(modelType_t);
         } else {
             Log.e(TAG, "SetSoundModelTuningParamsAidl: ERROR: invalid keyword ID");
             return ListenTypes.STATUS_EBAD_PARAM;
         }

         try {
             status = mListenAidlService.LsmSmlSet(lsm, configId, tPayload,
                                                  payloadSize);
         } catch (RemoteException e) {
             Log.e(TAG, "SetSoundModelTuningParamsAidl: ERROR: in LsmSmlSet call");
             if (keywordType == SVA_KEYWORD_TYPE_UDK)
                 closeSharedMemory(modelType_t.data);
             throw e.rethrowAsRuntimeException();
         }

         if (keywordType == SVA_KEYWORD_TYPE_UDK) {
             switch(status) {
                 case  ListenTypes.UdkTextInputTooSmallSyllables :
                     Log.e(TAG, "SetSoundModelTuningParamsAidl: ERROR: Too small syllables in text input");
                     status = ListenTypes.STATUS_ETOO_SMALL_SYLLABLES;
                     break;
                 case ListenTypes.UdkTextInputTooManySyllables :
                     Log.e(TAG, "SetSoundModelTuningParamsAidl: ERROR: Too many syllables in text input");
                     status = ListenTypes.STATUS_ETOO_MANY_SYLLABLES;
                     break;
                 case ListenTypes.UdkTextInputFail :
                     Log.e(TAG, "SetSoundModelTuningParamsAidl: ERROR: Locale mismatch in text input");
                     status = ListenTypes.STATUS_ELOCALE_MISMATCH;
                     break;
                 case ListenTypes.UdkTextInputQualityPass :
                     Log.i(TAG, "SetSoundModelTuningParamsAidl: PASS: text input is proper");
                     break;
             }
             closeSharedMemory(modelType_t.data);
         }

         Log.i(TAG, "SetSoundModelTuningParams: Exit.");
         return status;
     }

    /**
     * Initialize EPD module prior to training the sound model.
     * <p>
     *
     *
     * @param epdHandle [out] Structure containing opaque EPD State Data
     * @param epdParams [in] EPD params
     * @param soundModel [in] PDK sound model if training PDK.
     *        Not applicable if training UDK sound model.
     *
     * @return STATUS_SUCCESS for success.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     */
    public static int initEPD(EPDHandle epdHandle,
        ListenEPDParams epdParams, ByteBuffer soundModel) {

        int status = 0;
        boolean bSmlInitComplete  = false;
        byte[] config = new byte[8];
        byte[] epdParamsInBytes = new byte[80];
        Ashmem ashSoundModel;
        ListenSmlModel[] lsm = new ListenSmlModel[1];
        lsm[0] = new ListenSmlModel();
        ListenRemoteHandleType remoteHandle = new ListenRemoteHandleType();
        long ashmemSize = 0;

        Log.i(TAG, "initEPD: enter");

        Log.i(TAG, "initEPD: Allocating memory for EPD module");
        if (mListenAidlService == null) {
            Log.e(TAG, "initEPD: LSM AIDL service is not yet registered, returing!!!");
            return ListenTypes.STATUS_EFAILURE;
        }
        if (epdHandle == null || epdParams == null) {
            Log.e(TAG, "initEPD: NULL object passed in as Argument");
            status = ListenTypes.STATUS_EBAD_PARAM;
            return status;
        }

        Log.i(TAG, "initEPD: Intiailizing SML ONLINE EPD");
        lsm[0].setRemoteHandle(remoteHandle);
        // add dummy model for udk case
        if (soundModel == null) {
            ByteBuffer dummyModel = ByteBuffer.allocate(8);
            ashSoundModel = getDataInAshmemObj(dummyModel);
            ashmemSize = ashSoundModel.size;
            ashSoundModel.size = 0;
        } else {
            ashSoundModel = getDataInAshmemObj(soundModel);
        }
        if (ashSoundModel == null) {
            Log.e(TAG, "Failed to get sound model in ashmem object");
            return ListenTypes.STATUS_EFAILURE;
        }

        try {
            status = mListenAidlService.LsmSmlInit_v2(
                SmlConfigStructureId.SML_CONFIG_ID_ONLINE_VAD_INIT, config,
                config.length, ashSoundModel, (int)ashSoundModel.size, lsm[0]);
            if (ashSoundModel.size == 0)
                ashSoundModel.size = ashmemSize;
        } catch (RemoteException e) {
            Log.e(TAG, "initEPD: ERROR: in LsmSmlInit call");
            closeSharedMemory(ashSoundModel);
            throw e.rethrowAsRuntimeException();
        }

        bSmlInitComplete = true;
        fillEPDParamsInBytes(epdParams, epdParamsInBytes);
        try {
            status = mListenAidlService.LsmSmlSet(
                lsm, SmlConfigStructureId.SML_CONFIG_ID_ONLINE_VAD_SET_PARAM,
                epdParamsInBytes, epdParamsInBytes.length);
        } catch (RemoteException e) {
            Log.e(TAG, "initEPD: ERROR: in LsmSmlSet call");
            closeSharedMemory(ashSoundModel);
            throw e.rethrowAsRuntimeException();
        }

        if (status != 0) {
            Log.i(TAG, "initEPD : Cleanup");
            int temp_status = 0;
            if (bSmlInitComplete) {
                try {
                    temp_status = mListenAidlService.LsmSmlSet(
                        lsm, SmlConfigStructureId.SML_CONFIG_ID_ONLINE_VAD_RELEASE,
                        config, config.length);
                } catch (RemoteException e) {
                    Log.e(TAG, "initEPD: ERROR: in LsmSmlSet call");
                    closeSharedMemory(ashSoundModel);
                    throw e.rethrowAsRuntimeException();
                }
                if (temp_status != 0) {
                    Log.e(TAG, "initEPD : Cleanup : Error while releasing EPD module!");
                }
            }
        } else {
            setEPDHandle(epdHandle, lsm[0]);
        }
        closeSharedMemory(ashSoundModel);
        Log.i(TAG, "initEPD : exit");
        return status;
    }

    /**
     * Reset EPD module prior to training with next recording.
     * <p>
     *
     * @param epdHandle [in] Structure containing EPD Handle State Data
     *
     * @return STATUS_SUCCESS for success.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     */
    public static int reinitEPD(EPDHandle epdHandle) {

        int status = 0;
        ListenSmlModel[] lsm = new ListenSmlModel[1];
        byte[] config = new byte[8];
        Log.i(TAG, "reinitEPD : enter");

        lsm[0] = getEPDHandle(epdHandle);
        if (lsm[0] == null) {
            Log.e(TAG, "reinitEPD: ERROR: Failed to get epd handle");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        try {
            status = mListenAidlService.LsmSmlSet(lsm,
                SmlConfigStructureId.SML_CONFIG_ID_ONLINE_VAD_REINIT,
                config, config.length);
        } catch (RemoteException e) {
            Log.e(TAG, "reinitEPD: ERROR: in LsmSmlSet call");
            throw e.rethrowAsRuntimeException();
        }

        Log.i(TAG, "reinitEPD : exit");
        return status;
    }

    /**
     * Process recording samples to detect keyword end. For each user recording
     * this API shall be called with multiple of 10ms sample size (16KHz, Mono channel, 16bit).
     * <p>
     *
     * @param epdHandle [in] Structure containing EPD Handle State Data
     * @param userRecording [in] recording of a user speaking the keyword
     * @param soundModel [out] EPD result
     *
     * @return STATUS_SUCCESS for success.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     */
    public static int processEPD(EPDHandle epdHandle,
        ShortBuffer userRecording, EPDResult epdResult) {

        int status = 0;
        int epdResultSize = 16;
        byte[] config = new byte[epdResultSize];
        ByteValue[] bEpdResult = new ByteValue[epdResultSize];
        ListenSmlModel[] lsm = new ListenSmlModel[1];
        ByteValue[] pOutputData = new ByteValue[8];
        IntegerValue outputDataId = new IntegerValue();
        IntegerValue outputDataSize = new IntegerValue();
        IntegerValue smlResultGetSize = new IntegerValue();

        Log.i(TAG, "processEPD : enter");

        if (userRecording == null) {
            Log.e(TAG, "processEPD : NULL record buffer passed in");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        lsm[0] = getEPDHandle(epdHandle);
        if (lsm[0] == null) {
            Log.e(TAG, "processEPD: ERROR: Failed to get epd handle");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        ByteBuffer bBuffer = ByteBuffer.allocate(userRecording.array().length * 2);
        for (int i = 0, j = 0; i < userRecording.array().length; ++i) {
            bBuffer.put(j++, (byte)(userRecording.array()[i] & 0xff));
            bBuffer.put(j++, (byte)((userRecording.array()[i] >> 8)& 0xff));
        }
        try {
            status = mListenAidlService.LsmSmlProcess(lsm,
                bBuffer.array(), bBuffer.array().length,
                SmlConfigStructureId.SML_CONFIG_ID_ONLINE_VAD_PROCESS,
                pOutputData, outputDataSize, outputDataId);
        } catch (RemoteException e) {
            Log.e(TAG, "processEPD: ERROR: in LsmSmlProcess call");
            throw e.rethrowAsRuntimeException();
        }

        Log.i(TAG, "processEPD : getting results from sml");
        smlResultGetSize.value = bEpdResult.length;
        try {
            status = mListenAidlService.LsmSmlGet(lsm,
                SmlConfigStructureId.SML_CONFIG_ID_ONLINE_VAD_GET_RESULT,
                config, epdResultSize, bEpdResult, smlResultGetSize);
        } catch (RemoteException e) {
            Log.e(TAG, "processEPD: Failure in lsm_smlGet with SML_CONFIG_ID_ONLINE_VAD_GET_RESULT!");
            throw e.rethrowAsRuntimeException();
        }

        fillEPDResult(bEpdResult, epdResult);
        Log.i(TAG, "processEPD : exit: EPD Result Detected Status :" + epdResult.is_detected);
        return status;
    }

    /**
     * Release the EPD module once all the recordings are verified and ready to generate
     * a trained sound model with these recordings.
     * <p>
     *
     * @param epdHandle [in] Structure containing EPD Handle State Data
     *
     * @return STATUS_SUCCESS for success.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     */
    public static int releaseEPD(EPDHandle epdHandle) {

        int status = 0;
        byte[] config = new byte[8];
        ListenSmlModel[] lsm = new ListenSmlModel[1];

        Log.i(TAG, "releaseEPD : enter");

        lsm[0] = getEPDHandle(epdHandle);
        if (lsm[0] == null) {
            Log.e(TAG, "reinitEPD: ERROR: Failed to get epd handle");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        Log.i(TAG, "releaseEPD : Releasing SML Internal Data");
        try {
            status = mListenAidlService.LsmSmlSet(lsm,
                SmlConfigStructureId.SML_CONFIG_ID_ONLINE_VAD_RELEASE,
                config, config.length);
        } catch (RemoteException e) {
            Log.e(TAG, "releaseEPD: ERROR: in LsmSmlSet call");
            throw e.rethrowAsRuntimeException();
        }

        setEPDHandle(epdHandle, null);
        Log.i(TAG, "releaseEPD : exit");
        return status;
    }

    /**
    * Utility function to get EPDHandle data from the EPDHandle structure
    * Passed in by the jni layer.
    *
    * param [in]  epdHandle - EPDHandle structure from Java layer.
    *
    * Return - ListenSmlModel that contains the internal data
    *          for sml and other dependencies.
    *          nullptr in case of error.
    */
    private static ListenSmlModel getEPDHandle(EPDHandle epdHandle) {
        Log.i(TAG, "getEPDHandle : enter");
        ListenSmlModel lsm = new ListenSmlModel();
        ListenRemoteHandleType remoteHandle = new ListenRemoteHandleType();

        if (epdHandle == null) {
            Log.e(TAG, "getEPDHandle : EPD Handle is null!");
            return null;
        }
        if (epdHandle.epd_data == null) {
            Log.e(TAG, "getEPDHandle : EPD not initialized!");
            return null;
        }

        byte[] bytes = epdHandle.epd_data.array();
        remoteHandle.opaqueHandle = byte2Int(bytes, 0);
        remoteHandle.reserved = byte2Int(bytes, 4);
        lsm.setRemoteHandle(remoteHandle);

        return lsm;
    }

    /**
    * Sets the opaque EPD data into the handle structure provided by the Java layer.
    * if the EPD data and size is null, it will null out the Java layer opaque data as well.
    *
    * param [in/out]  epdHandle - EPDHandle object from Java layer
    * param [in] lsmModel - lsm model data for EPD OR NULL to reset EPDHandle
    *
    * Return - status
    */
    private static int setEPDHandle(EPDHandle epdHandle, ListenSmlModel lsmModel) {
        ListenRemoteHandleType remoteHandle;
        byte[] handleInBytes = new byte[8];

        if (epdHandle == null) {
            Log.e(TAG, "setEPDHandle : EPD Handle is null!");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        if (lsmModel == null) {
            Log.e(TAG, "setEPDHandle : Setting EPD Handle to NULL");
            epdHandle.epd_data = null;
        } else {
            remoteHandle = lsmModel.getRemoteHandle();
            fillInt(handleInBytes, 0, remoteHandle.opaqueHandle);
            fillInt(handleInBytes, 4, remoteHandle.reserved);
            epdHandle.epd_data = ByteBuffer.wrap(handleInBytes);
        }

        return ListenTypes.STATUS_SUCCESS;
    }

    /**
    * Fill EPD params in bytes array and print out the param values
    *
    * param [in]  p_epd_params - ptr for the ListenEPDParams structure
    * param [out] epdParamsInBytes - bytes array with ListenEPDParams filled
    *
    * Return - None
    */
    private static void fillEPDParamsInBytes(ListenEPDParams p_epd_params, byte[] epdParamsInBytes) {
        int index = 0;

        Log.i(TAG, "minSnrOnset = " + p_epd_params.minSnrOnset);
        fillInt(epdParamsInBytes, index,
            Float.floatToIntBits(p_epd_params.minSnrOnset));
        index = index + 4;

        Log.i(TAG, "minSnrLeave = " + p_epd_params.minSnrLeave);
        fillInt(epdParamsInBytes, index,
            Float.floatToIntBits(p_epd_params.minSnrLeave));
        index = index + 4;

        Log.i(TAG, "snrFloor = " + p_epd_params.snrFloor);
        fillInt(epdParamsInBytes, index,
            Float.floatToIntBits(p_epd_params.snrFloor));
        index = index + 4;

        Log.i(TAG, "snrThresolds = " + p_epd_params.snrThresholds);
        fillInt(epdParamsInBytes, index,
            Float.floatToIntBits(p_epd_params.snrThresholds));
        index = index + 4;

        Log.i(TAG, "forgettingFactorNoise = " + p_epd_params.forgettingFactorNoise);
        fillInt(epdParamsInBytes, index,
            Float.floatToIntBits(p_epd_params.forgettingFactorNoise));
        index = index + 4;

        Log.i(TAG, "numFrameTransientFrame = " + p_epd_params.numFrameTransientFrame);
        fillInt(epdParamsInBytes, index, p_epd_params.numFrameTransientFrame);
        index = index + 4;

        Log.i(TAG, "minEnergyFrameRatio = " +  p_epd_params.minEnergyFrameRatio);
        fillInt(epdParamsInBytes, index,
            Float.floatToIntBits(p_epd_params.minEnergyFrameRatio));
        index = index + 4;

        Log.i(TAG, "minNoiseEnergy = " +  p_epd_params.minNoiseEnergy);
        fillInt(epdParamsInBytes, index,
            Float.floatToIntBits(p_epd_params.minNoiseEnergy));
        index = index + 4;

        Log.i(TAG, "numMinFramesInPhrase = " +  p_epd_params.numMinFramesInPhrase);
        fillInt(epdParamsInBytes, index, p_epd_params.numMinFramesInPhrase);
        index = index + 4;

        Log.i(TAG, "numMinFramesInSpeech = " +  p_epd_params.numMinFramesInSpeech);
        fillInt(epdParamsInBytes, index, p_epd_params.numMinFramesInSpeech);
        index = index + 4;

        Log.i(TAG, "numMaxFrameInSpeechGap = " +  p_epd_params.numMaxFrameInSpeechGap);
        fillInt(epdParamsInBytes, index, p_epd_params.numMaxFrameInSpeechGap);
        index = index + 4;

        Log.i(TAG, "numFramesInHead = " +  p_epd_params.numFramesInHead);
        fillInt(epdParamsInBytes, index, p_epd_params.numFramesInHead);
        index = index + 4;

        Log.i(TAG, "numFramesInTail = " + p_epd_params.numFramesInTail);
        fillInt(epdParamsInBytes, index, p_epd_params.numFramesInTail);
        index = index + 4;

        Log.i(TAG, "preEmphasize = " + p_epd_params.preEmphasize);
        fillInt(epdParamsInBytes, index, p_epd_params.preEmphasize);
        index = index + 4;

        Log.i(TAG, "numMaxFrames = " + p_epd_params.numMaxFrames);
        fillInt(epdParamsInBytes, index, p_epd_params.numMaxFrames);
        index = index + 4;

        Log.i(TAG, "keyword_threshold = " + p_epd_params.keyword_threshold);
        fillInt(epdParamsInBytes, index, p_epd_params.keyword_threshold);
    }

    /**
    * Fill and print out EPD result if detected
    *
    * param [in]  bEpdResult - ptr for the EPDResult structure
    *
    * Return - None
    */
    private static void fillEPDResult(ByteValue[] result, EPDResult bEpdResult) {
        byte[] epd_result = new byte[result.length];
        for (int i = 0; i < result.length; i++)
            epd_result[i] = result[i].value;

        bEpdResult.is_detected = byte2Int(epd_result, 0);
        bEpdResult.start_index = byte2Int(epd_result, 4);
        bEpdResult.end_index = byte2Int(epd_result, 8);
        bEpdResult.snr = Float.intBitsToFloat(byte2Int(epd_result, 12));

        if (bEpdResult.is_detected == 1) {
            Log.i(TAG, "EPD Results : is_detected = " + bEpdResult.is_detected);
            Log.i(TAG, "EPD Results : start_index = " + bEpdResult.start_index);
            Log.i(TAG, "EPD Results : end_index = " + bEpdResult.end_index);
            Log.i(TAG, "EPD Results : snr = " + bEpdResult.snr);
        }
    }

    /**
    * Fill int data to byte array
    *
    * param [out] buffer - byte array to be written
    * param [in]  startPos - index to fill byte data
    * param [in]  value - int value to be written to byte array
    *
    * Return - None
    */
    private static void fillInt(byte[] buffer, final int startPos, int value) {
        int startIndex = startPos;
        buffer[startIndex] = (byte) (value & 0xff);
        buffer[++startIndex] = (byte) (value >> 8 & 0xff);
        buffer[++startIndex] = (byte) (value >> 16 & 0xff);
        buffer[++startIndex] = (byte) (value >> 24 & 0xff);
    }

    /**
    * Convert Server return status to Client status
    *
    * param [in]  status  - Server return status
    *
    * Return - Client return status
    */
    private static int ConvertReturnStatus(int status) {
        int client_status = 0;
        switch(status) {
            case LSMServerStatus.kSucess :
                client_status = ListenTypes.STATUS_SUCCESS;
                break;
            case LSMServerStatus.kFailed :
                client_status = ListenTypes.STATUS_EFAILURE;
                break;
            case LSMServerStatus.kBadParam :
                client_status = ListenTypes.STATUS_EBAD_PARAM;
                break;
            case LSMServerStatus.kKeywordNotFound :
                client_status = ListenTypes.STATUS_EKEYWORD_NOT_IN_SOUNDMODEL;
                break;
            case LSMServerStatus.kUserNotFound :
                client_status = ListenTypes.STATUS_EUSER_NOT_IN_SOUNDMODEL ;
                break;
            case LSMServerStatus.kUserKwPairNotActive :
                client_status = ListenTypes.STATUS_EKEYWORD_USER_PAIR_NOT_IN_SOUNDMODEL;
                break;
            case LSMServerStatus.kSMVersionUnsupported :
                client_status = ListenTypes.STATUS_EUNSUPPORTED_SOUNDMODEL;
                break;
            case LSMServerStatus.kUserDataForKwAlreadyPresent :
                client_status = ListenTypes.STATUS_EUSER_NAME_CANNOT_BE_USED;
                break;
            case LSMServerStatus.kDuplicateUserKeywordPair :
                client_status = ListenTypes.STATUS_EUSER_KEYWORD_PAIRING_ALREADY_PRESENT;
                break;
            case LSMServerStatus.kMaxKeywordsExceeded :
                client_status = ListenTypes.STATUS_EMAX_KEYWORDS_EXCEEDED;
                break;
            case LSMServerStatus.kMaxUsersExceeded :
                client_status = ListenTypes.STATUS_EMAX_USERS_EXCEEDED;
                break;
            case LSMServerStatus.kRecordingTooShort :
                client_status = ListenTypes.STATUS_ERECORDING_TOO_SHORT;
                break;
            case LSMServerStatus.kRecordingTooLong :
                client_status = ListenTypes.STATUS_ERECORDING_TOO_LONG;
                break;
            case LSMServerStatus.kChoppedSample :
                client_status = ListenTypes.STATUS_ECHOPPED_SAMPLE;
                break;
            case LSMServerStatus.kClippedSample :
                client_status = ListenTypes.STATUS_ECLIPPED_SAMPLE;
                break;
            case LSMServerStatus.kBadRecordingQualitiy :
                client_status = ListenTypes.STATUS_BAD_UDK_RECORDING_QUALITY;
                break;
            case LSMServerStatus.kDuplicateKeyword :
            case LSMServerStatus.kEventStructUnsupported :
            case LSMServerStatus.kLastKeyword :
            case LSMServerStatus.kNoSignal :
            case LSMServerStatus.kLowSnr :
            case LSMServerStatus.kNeedRetrain :
            case LSMServerStatus.kUserUDKPairNotRemoved :
            case LSMServerStatus.kCannotCreateUserUDK :
            case LSMServerStatus.kOutputArrayTooSmall :
            case LSMServerStatus.kTooManyAbnormalUserScores :
            case LSMServerStatus.kWrongModel :
            case LSMServerStatus.kWrongModelAndIndicator :
            case LSMServerStatus.kDuplicateModel :
            case LSMServerStatus. kSecondStageKeywordNotFound :
                client_status = ListenTypes.STATUS_EFAILURE;
                break;
        }
        return client_status;
    }

    /**
    * read int value from byte array
    *
    * param [in]  bytes - byte array to be read
    * param [in]  start - index where read starts
    *
    * Return - int value read from byte array
    */
    private static int byte2Int(byte[] bytes, int start) {
        if (start + 4 > bytes.length) {
            Log.i(TAG, "byte2Int start + 4 out of array");
            return 0;
        }
        return (bytes[start++] & 0xff)
                | (bytes[start++] & 0xff) << 8
                | (bytes[start++] & 0xff) << 16
                | (bytes[start] & 0xff) << 24;
    }

    /**
     * Get the total size of bytes required to hold a SoundModel that
     * containing both User-Independent and User-Dependent SVA 1.0 model
     * data.
     * <p>
     * The size returned by this method must be used to create a
     * ByteBuffer that can hold the SoundModel created by extend().
     *
     * @param userIndependentModel [in] contains User-Independent
     *        model data.
     *
     * @return total (unsigned) size of extended SoundModel
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     *
     * @deprecated use {@link getSizeWhenExtended(ByteBuffer, String, String)}
     * instead.
     *
     * <p>
     * Refer to {@link extend(ByteBuffer, String, String, int, ShortBuffer[],
     *  ByteBuffer, ConfidenceData)}
     */
@Deprecated
     public static int getSizeWhenExtended(
            ByteBuffer userIndependentModel)
     {
     // calls SVA 2.0 version of getSizeWhenExtended() with null string for
     // Keywords Phrase and User Name.
         int status = nativeGetSizeWhenExtended(userIndependentModel, null, null);
         return status;
     }

    /**
     * Get the total size of bytes required to hold a SoundModel that
     * containing both User-Independent and User-Dependent SVA 2.0 (and above)
     * model data.
     * <p>
     * The size returned by this method must be used to create a
     * ByteBuffer that can hold the SoundModel created by
     * {@link extend(ByteBuffer, String, String, int, ShortBuffer[], ByteBuffer, ConfidenceData)}.
     * <p>
     * SoundModels with User-Defined keyword phrases cannot be extended
     * and thus cannot be passed to this function.
     *
     * @param soundModel [in] contains model data.
     * @param keywordPhrase [in] name of keyword in SM to be extended
     * @param userName [in] name of user created these training recordings
     *
     * @return total (unsigned) size of extended SoundModel
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EKEYWORD_NOT_IN_SOUNDMODEL
     *        <br>   STATUS_EMAX_USERS_EXCEEDED
     * <p>
     * Refer to {@link extend(ByteBuffer, String, String, int, ShortBuffer[],
     *  ByteBuffer, ConfidenceData)}
     */
     public static native int nativeGetSizeWhenExtended(
            ByteBuffer soundModel,
            String     keywordPhrase,
            String     userName    );

     public static int getSizeWhenExtended(ByteBuffer psoundModel,
                                           String pkeywordPhrase,
                                           String puserName) {

        Log.i(TAG, "getSizeWhenExtended: Enter.");

        if (mListenAidlService == null)
            return nativeGetSizeWhenExtended(psoundModel, pkeywordPhrase, puserName);

        int status = 0;
        Ashmem ashSoundModel;
        SoundModelHeader modelHeader = new SoundModelHeader();
        IntegerValue modelSize = new IntegerValue();

        ashSoundModel = getDataInAshmemObj(psoundModel);
        if (ashSoundModel == null) {
            Log.e(TAG, "Failed to get sound model in ashmem object");
            return ListenTypes.STATUS_EFAILURE;
        }

        status = lsm_getSoundModelHeader(ashSoundModel, modelHeader);

        if (status != 0) {
            Log.e(TAG, "getSizeWhenExtended: ERROR: getSoundModelHeader() failed");
            closeSharedMemory(ashSoundModel);
            return status;
        } else if (modelHeader.numKeywords == 0) {
            Log.e(TAG, "ERROR getSoundModelHeader() returned numKeywords zero");
            closeSharedMemory(ashSoundModel);
            return ListenTypes.STATUS_EFAILURE;
        }

        if (modelHeader.isStripped) {
            Log.e(TAG, "getSizeWhenExtended: Stripped models can't be trained");
            closeSharedMemory(ashSoundModel);
            return ListenTypes.STATUS_ENOT_SUPPORTED_FOR_SOUNDMODEL_VERSION;
        }

         try {
             status = mListenAidlService.LsmGetUserKeywordModelSize(ashSoundModel,
                                        (int)ashSoundModel.size, pkeywordPhrase,
                                         puserName, modelSize);
         } catch (RemoteException e) {
             Log.e(TAG, "getSizeWhenExtended: ERROR in LsmGetUserKeywordModelSize call");
             closeSharedMemory(ashSoundModel);
             lsm_releaseSoundModelHeader(modelHeader);
             throw e.rethrowAsRuntimeException();
         }

         if (status != 0) {
             Log.e(TAG, "getSizeWhenExtended: ERROR LsmGetUserKeywordModelSize() returned numKeywords error");
             lsm_releaseSoundModelHeader(modelHeader);
             closeSharedMemory(ashSoundModel);
             return status;
         }

        lsm_releaseSoundModelHeader(modelHeader);
        closeSharedMemory(ashSoundModel);
        Log.i(TAG, "getSizeWhenExtended: Exit.");
        return modelSize.value;
    }

    /**
     * Extends a SoundModel by combining UserIndependentModel with
     * SVA 1.0 UserDependentModel data created from user recordings.
     * <p>
     * Application is responsible for creating a ByteBuffer large
     * enough to hold the SoundModel output by this method.
     * The size of the output SoundModel can be determined calling
     * {@link getSizeWhenExtended(ByteBuffer)}.
     * <p>
     * At least 5 user recordings should be passed to this method.
     * The more user recordings passed as input, the greater the
     * likelihood of getting a higher quality SoundModel made.
     * <p>
     * Confidence level greater than 70 indicates that user's
     * speech characteristics are sufficiently consistent to
     * afford the detection algorithm the ability to do User
     * detection.
     *
     * @param userIndependentModel [in] contains
     *        UserIndependentModel data
     * @param numUserRecordings [in] number of recordings of a
     *        user speaking the keyword
     * @param userRecordings [in] array of N user recordings
     * @param extendedSoundModel [out] extended SoundModel
     * @param confidenceData [out] contains ConfidenceData
     * @return
     *        <br>   STATUS_SUCCESS
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     *
     * @deprecated use {@link extend(ByteBuffer, String, String, int,
     *     ShortBuffer[], ByteBuffer, ConfidenceData)} instead.
     *
     * <p>
     * Refer to {@link getSizeWhenExtended(ByteBuffer)}
     */
@Deprecated
     public static int extend(
             ByteBuffer        userIndependentModel,
             int               numUserRecordings,
             ShortBuffer       userRecordings[],
             ByteBuffer        extendedSoundModel,
             ConfidenceData    confidenceData )
     {
         // calls SVA 2.0 version of extend() with null strings for Keywords Phrase
         // and User Name.
         // This results in UserData being added to the first Keyword of the SoundModel.
         int status = extend( userIndependentModel, null, null,
                              numUserRecordings, userRecordings,
                              extendedSoundModel, confidenceData );
         return status;
     }

    /**
     * Extends a SVA 2.0 (and above) SoundModel by combining User-Independent
     * model data with User-Dependent model data created from user recordings.
     * <p>
     * Application is responsible for creating a ByteBuffer large
     * enough to hold the SoundModel output by this method.
     * The size of the output SoundModel can be determined calling
     * {@link getSizeWhenExtended(ByteBuffer, String, String)}.
     * <p>
     * At least 5 user recordings should be passed to this method.
     * The more user recordings passed as input, the greater the
     * likelihood of getting a higher quality SoundModel made.
     * <p>
     * Confidence level greater than 70 indicates that user's
     * speech characteristics are sufficiently consistent to
     * afford the detection algorithm the ability to do User
     * detection.
     * <p>
     * SoundModels with User-Defined keyword phrases cannot be extended
     * and thus cannot be passed to this function.
     *
     * @param soundModel [in] contains the SoundModel to be extended.
     * @param [in]  keywordPhrase - name of keyword in SM to be extended
     *        Null String can be passed if SM only contains one Keyword.
     * @param [in]  userName - name of user created these training recordings
     * @param numUserRecordings [in] number of recordings of a user speaking the keyword
     * @param userRecordings [in] array of N user recordings
     * @param extendedSoundModel [out] extended SoundModel
     * @param confidenceData [out] contains ConfidenceData
     * @return
     *        <br>   STATUS_SUCCESS
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EKEYWORD_NOT_IN_SOUNDMODEL
     *        <br>   STATUS_EMAX_USERS_EXCEEDED
     *
     * <p>
     * Refer to {@link getSizeWhenExtended(ByteBuffer, String, String)}
     */
     public static native int nativeExtend(
             ByteBuffer        soundModel,
             String            keywordPhrase,
             String            userName,
             int               numUserRecordings,
             ShortBuffer       userRecordings[],
             ByteBuffer        extendedSoundModel,
             ConfidenceData    confidenceData );

     public static int extend(ByteBuffer psoundModel, String pkeywordPhrase,
                              String puserName, int pnumUserRecordings,
                              ShortBuffer puserRecordings[],
                              ByteBuffer pextendedSoundModel,
                              ConfidenceData pconfidenceData) {

        Log.i(TAG, "extend: Enter.");

        if (mListenAidlService == null)
            return nativeExtend(psoundModel, pkeywordPhrase, puserName, pnumUserRecordings,
                                puserRecordings, pextendedSoundModel, pconfidenceData);

        int status = 0;
        int returnedModelSize = getSizeWhenExtended(psoundModel, pkeywordPhrase, puserName);
        int rUserRecordingSize[] = new int[pnumUserRecordings];
        byte placeholder = 0;
        SharedMemory sharedFd = null;
        Ashmem rUserRecordings[] = new Ashmem[pnumUserRecordings];
        Ashmem rSoundModel;
        Ashmem rReturnedModel = new Ashmem();
        ListenEpdParams[] rEpd = new ListenEpdParams[0];
        IntegerValue rMatchingScore = new IntegerValue();
        ByteBuffer bbf;

        if (pextendedSoundModel == null || pextendedSoundModel.capacity() < returnedModelSize) {
            Log.e(TAG, "extend: ERROR: Output ByteBuffer is not large enough to hold extended SM");
            return ListenTypes.STATUS_EBAD_PARAM;
        }
        rReturnedModel = getDataInAshmemObj(pextendedSoundModel);
        Ashmem temp = rReturnedModel;

        rSoundModel = getDataInAshmemObj(psoundModel);
        if (rSoundModel == null) {
            Log.e(TAG, "extend: ERROR: Failed to get language model");
            closeSharedMemory(rReturnedModel);
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        for (int i = 0; i < pnumUserRecordings; ++i) {
             ByteBuffer bBuffer = ByteBuffer.allocate(puserRecordings[i].array().length * 2);
             for (int k = 0, j = 0; k < puserRecordings[i].array().length; ++k) {
                 bBuffer.put(j++, (byte)(puserRecordings[i].array()[k] & 0xff));
                 bBuffer.put(j++, (byte)((puserRecordings[i].array()[k] >> 8)& 0xff));
             }
             bBuffer.rewind();
             rUserRecordings[i] = getDataInAshmemObj(bBuffer);
             if (rUserRecordings[i] == null) {
                 Log.e(TAG, "extend: ERROR: Failed to get user recording");
                 closeSharedMemory(rReturnedModel);
                 closeSharedMemory(rSoundModel);
                 for (int j = 0; j < i; ++j)
                     closeSharedMemory(rUserRecordings[j]);
                 return ListenTypes.STATUS_EBAD_PARAM;
             }
             rUserRecordingSize[i] = (int)rUserRecordings[i].size;
        }

        try {
            // Not passing EPD params.
            status = mListenAidlService.LsmCreateUserKeywordModel(rSoundModel, (int)rSoundModel.size,
                                     pkeywordPhrase, puserName, rEpd, pnumUserRecordings, rUserRecordings,
                                     rUserRecordingSize, (int)rReturnedModel.size, rReturnedModel,
                                     rMatchingScore);
        } catch (RemoteException e) {
            Log.e(TAG, "extend: ERROR: Failure in LsmCreateUserKeywordModel call");
            closeSharedMemory(rReturnedModel);
            closeSharedMemory(rSoundModel);
            for (int i = 0; i < pnumUserRecordings; ++i)
                closeSharedMemory(rUserRecordings[i]);
            throw e.rethrowAsRuntimeException();
        }

        sharedFd = SharedMemory.fromFileDescriptor(rReturnedModel.fd);

        if (status == ListenTypes.STATUS_SUCCESS) {
            pconfidenceData.userMatch = rMatchingScore.value;
            try {
                bbf = sharedFd.map(OsConstants.PROT_READ|OsConstants.PROT_WRITE, 0, (int)rReturnedModel.size);
            } catch (ErrnoException e) {
                Log.e(TAG, "extend: ERROR: Failed to map Sharedmemory : ", e);
                closeSharedMemory(rReturnedModel);
                closeSharedMemory(rSoundModel);
                for (int i = 0; i < pnumUserRecordings; ++i)
                    closeSharedMemory(rUserRecordings[i]);
                return ListenTypes.STATUS_EBAD_PARAM;
            }

            if (bbf == null) {
                Log.e(TAG, "extend: bbf is NULL");
            } else {
                bbf.rewind();
                pextendedSoundModel.limit(pextendedSoundModel.array().length);
                pextendedSoundModel.put(bbf);
            }
            sharedFd.unmap(bbf);
        }

        closeSharedMemory(rReturnedModel);
        closeSharedMemory(rSoundModel);
        for (int i = 0; i < pnumUserRecordings; ++i)
            closeSharedMemory(rUserRecordings[i]);

        closeSharedMemory(temp);
        Log.i(TAG, "extend: Exit.");
        return status;
    }

    /**
     * Verifies that a user recording containing a keyphrase is of good
     * enough quality used for creating a User-Defined SoundModel with
     * {@link createUdkSm(String, String, int, ShortBuffer, ByteBuffer,
     * ByteBuffer, ConfidenceData)}. Checks the recording for problems
     * with Signal to Noise Ratio, signal level, and speech length.
     * <p>
     * A Language Model matching the language the keyphrase is spoken
     * in is used during verification process.
     *
     * @param languageModel [in] buffer containing language specific data
     * @param userRecording [in] the user training recording to be verified
     *
     * @return If positive the return value is the measured Signal-to-Noise ratio of
     *        <br>    the recording.  A value of 12 or higher is acceptable
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_ENO_SPEACH_IN_RECORDING
     *        <br>   STATUS_ETOO_MUCH_NOISE_IN_RECORDING
     */
    public static native int nativeVerifyUdkRecording(
            ByteBuffer      languageModel,
            ShortBuffer     userRecording);

    public static int verifyUdkRecording(ByteBuffer planguageModel,
                                         ShortBuffer puserRecording) {
        Log.i(TAG, "verifyUdkRecording: Enter.");

        if (mListenAidlService == null)
            return nativeVerifyUdkRecording(planguageModel, puserRecording);

        int status = 0;
        Ashmem rlanguageModel;
        Ashmem rUserRecording;
        FloatValue rSnr = new FloatValue();
        ListenEpdParams[] epd = new ListenEpdParams[0];
        ByteBuffer bBuffer = ByteBuffer.allocate(puserRecording.array().length *2);

        if (planguageModel == null) {
            Log.e(TAG, "verifyUdkRecording: ERROR: Null language model is passed");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        rlanguageModel = getDataInAshmemObj(planguageModel);
        if (rlanguageModel == null) {
            Log.e(TAG, "verifyUdkRecording: ERROR: Failed to get language model");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        for (int k = 0, j = 0; k < puserRecording.array().length; ++k) {
            bBuffer.put(j++, (byte)(puserRecording.array()[k] & 0xff));
            bBuffer.put(j++, (byte)((puserRecording.array()[k] >> 8)& 0xff));
        }

        rUserRecording = getDataInAshmemObj(bBuffer);
        if (rUserRecording == null) {
            Log.e(TAG, "verifyUdkRecording: ERROR: Failed to get language model");
            closeSharedMemory(rlanguageModel);
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        try {
            status = mListenAidlService.LsmCheckUserRecording(rlanguageModel,
                                 (int)rlanguageModel.size, epd, rUserRecording,
                                 (int)rUserRecording.size, 0, rSnr);
        } catch (RemoteException e) {
            Log.e(TAG, "verifyUdkRecording: ERROR: Failure in LsmCheckUserRecording call");
            closeSharedMemory(rlanguageModel);
            closeSharedMemory(rUserRecording);
            throw e.rethrowAsRuntimeException();
        }

        closeSharedMemory(rlanguageModel);
        closeSharedMemory(rUserRecording);

        if (status != 0) {
            Log.e(TAG, "verifyUdkRecording: ERROR: Failure in LsmCheckUserRecording call with status " +
                 Integer.toString(status));
            return ConvertReturnStatus(status);
        }

        Log.i(TAG, "verifyUdkRecording: Exit, status " + Integer.toString(status));
        return (int)rSnr.value;
    }

    /**
     * Verify that the last user recording contains a keyphrase is of good
     * enough quality used for creating a User-Defined SoundModel with
     * {@link createUdkSm(String, String, int, ShortBuffer, ByteBuffer,
     * ByteBuffer, ConfidenceData)}. Checks the recording for problems with
     * Signal to Noise Ratio, signal level, and speech length.
     * All the recordings which have been spoken will be compared with
     * one another to ensure the consistency as well.
     * <p>
     * A Language Model matching the language the keyphrase is spoken
     * in is used during verification process.
     *
     * @param languageModel [in] buffer containing language specific data
     * @param userRecordings[] [in] the user training recordings to be verified
     *
     * @return If positive the return value is the measured Signal-to-Noise ratio of
     *        <br>    the recordings.
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_ENO_SPEACH_IN_RECORDING
     *        <br>   STATUS_ETOO_MUCH_NOISE_IN_RECORDING
     */
    public static native int verifyUdkRecording(
            ByteBuffer      languageModel,
            ShortBuffer     userRecordings[]);

    /**
     * Gets the total size in bytes required to hold a User-Defined keyword
     * SoundModel.
     * <p>
     * The size returned by this method can be used to create a ByteBuffer
     * that can hold the user-defined keyphrase SoundModel created by
     * {@link createUdkSm(String, String, int, ShortBuffer, ByteBuffer,
     * ByteBuffer, ConfidenceData)}
     * <p>
     * A Language Model matching the language the keyphrase is spoken
     * in must be supplied as input.
     *
     * @param keywordPhrase [in] name of keyword SM is to be created for
     * @param userName [in] name of user created these training recordings
     * @param userRecordings[] [in] the user training recordings
     * @param languageModel [in] buffer containing language specific data
     *
     * @return total (unsigned) size of UDK SoundModel
     *        <br> Negative error number is returned if error occurred.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_EFAILURE
     *
     * <p>
     * Refer to {@link createUdkSm(String, String, int, ShortBuffer,
     *  ByteBuffer, ByteBuffer, ConfidenceData)}
     */
    public static native int nativeGetUdkSmSize(
            String          keyPhrase,
            String          username,
            ShortBuffer     userRecordings[],
            ByteBuffer      languageModel);

    public static int getUdkSmSize(String pkeyPhrase, String pusername,
                                        ShortBuffer puserRecordings[],
                                        ByteBuffer planguageModel) {
        Log.i(TAG, "getUdkSmSize: Enter, Keyphrase : " + pkeyPhrase + ", usename : " + pusername +
                    ", number of recordings : " + Integer.toString(puserRecordings.length));

        if (mListenAidlService == null)
            return nativeGetUdkSmSize(pkeyPhrase, pusername, puserRecordings,
                                      planguageModel);

        int status = 0;
        int numOfRecordings = puserRecordings.length;
        int dummySize = 0;
        int rUserRecordingSize[] = new int[numOfRecordings];
        Ashmem rUserRecordings[] = new Ashmem[numOfRecordings];
        Ashmem rlanguageModel;
        Ashmem dummy;
        IntegerValue rModelSize = new IntegerValue();
        ListenEpdParams[] rEpd = new ListenEpdParams[0];
        ByteBuffer tempBuffer = ByteBuffer.allocate(1);

        //dummy ashmem is just used as placeholder in API, tempBuffer and dummySize is used for this
        tempBuffer.put((byte)1);
        dummy = getDataInAshmemObj(tempBuffer);
        if (dummy == null) {
            Log.e(TAG, "getUdkSmSize: ERROR: Failed to get memory for dummy ashmem");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        rlanguageModel = getDataInAshmemObj(planguageModel);
        if (rlanguageModel == null) {
            Log.e(TAG, "getUdkSmSize: ERROR: Failed to get language model");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        for (int i = 0; i < numOfRecordings; ++i) {
            ByteBuffer bBuffer = ByteBuffer.allocate(puserRecordings[i].array().length * 2);
            for (int k = 0, j = 0; k < puserRecordings[i].array().length; ++k) {
                bBuffer.put(j++, (byte)(puserRecordings[i].array()[k] & 0xff));
                bBuffer.put(j++, (byte)((puserRecordings[i].array()[k] >> 8)& 0xff));
            }
            rUserRecordings[i] = getDataInAshmemObj(bBuffer);
            if (rUserRecordings[i] == null) {
                Log.e(TAG, "getUdkSmSize: ERROR: Failed to get user recording");
                closeSharedMemory(rlanguageModel);
                for (int l = 0; l < i; ++l)
                    closeSharedMemory(rUserRecordings[l]);
                return ListenTypes.STATUS_EBAD_PARAM;
            }
            rUserRecordingSize[i] = (int)rUserRecordings[i].size;
        }

        try {
             // Not passing pUserDefinedKeyword, nor EPD params.
             status = mListenAidlService.LsmGetUserDefinedKeywordSize(dummy, dummySize,
                                      pkeyPhrase, pusername, rEpd, numOfRecordings,
                                      rUserRecordings, rUserRecordingSize, rlanguageModel,
                                      (int)rlanguageModel.size, rModelSize);
         } catch (RemoteException e) {
             Log.e(TAG, "getUdkSmSize: ERROR: Failure in LsmGetUserDefinedKeywordSize call");
             closeSharedMemory(rlanguageModel);
             for (int i = 0; i < numOfRecordings; ++i)
                 closeSharedMemory(rUserRecordings[i]);
             throw e.rethrowAsRuntimeException();
         }

        closeSharedMemory(rlanguageModel);
        for (int i = 0; i < numOfRecordings; ++i)
            closeSharedMemory(rUserRecordings[i]);
        closeSharedMemory(dummy);

        Log.i(TAG, "getUdkSmSize: Exit.");
        return rModelSize.value;
    }

    /**
     * Creates a User-Defined keyword SoundModel.
     * <p>
     * App is responsible for creating a ByteBuffer large
     * enough to hold the SoundModel output by this method.
     * The size of the output SoundModel can be determined calling
     * {@link getUdkSmSize(String, String, ShortBuffer, ByteBuffer)}.
     * <p>
     * At least 5 user recordings should be passed to this method.
     * The more user recordings passed as input, the greater the
     * likelihood of getting a higher quality SoundModel.
     * <p>
     * A Language Model matching the language the keyphrase is spoken
     * in is used during creation process.
     * <p>
     * A confidence level greater than 70 indicates that user's
     * speech characteristics are sufficiently consistent to
     * afford the detection algorithm the ability to do user
     * detection.
     *
     * @param keywordPhrase [in] name of keyword in SM to be extended
     * @param userName [in] name of user created these training recordings
     * @param numUserRecordings [in] number of recordings
     * @param userRecordings[] [in] the user recordings
     * @param languageModel [in] the model containing language specific data
     * @param userDefinedSoundModel [out] the created SoundModel
     * @param confidenceData [out] contains ConfidenceData
     * @return
     *         STATUS_SUCCESS
     *    <br> STATUS_EBAD_PARAM
     *    <br> STATUS_USER_DATA_OVERWRITTEN
     * <p>
     * Refer to {@link getUdkSmSize(String, String, ShortBuffer, ByteBuffer)}
     */
    public static native int nativeCreateUdkSm(
            String          keyPhrase,
            String          username,
            int             numUserRecordings,
            ShortBuffer     userRecordings[],
            ByteBuffer      languageModel,
            ByteBuffer      userDefinedSoundModel,
            ConfidenceData  confidenceData );

     public static int createUdkSm(String pkeyPhrase, String pusername,
                                   int pnumUserRecordings,
                                   ShortBuffer puserRecordings[],
                                   ByteBuffer planguageModel,
                                   ByteBuffer puserDefinedSoundModel,
                                   ConfidenceData pconfidenceData) {
        Log.i(TAG, "createUdkSm: Enter.");
        if (mListenAidlService == null)
            return nativeCreateUdkSm(pkeyPhrase, pusername, pnumUserRecordings,
                     puserRecordings, planguageModel, puserDefinedSoundModel,
                     pconfidenceData);

        int status = 0;
        int rUserRecordingSize[] = new int[pnumUserRecordings];
        int dummySize = 0;
        SharedMemory sharedFd = null;
        Ashmem rUserRecordings[] = new Ashmem[pnumUserRecordings];
        Ashmem rlanguageModel;
        Ashmem rUdkModel = new Ashmem();
        Ashmem dummy;
        IntegerValue rMatchingScore = new IntegerValue();
        ByteBuffer bbf, tempBuffer;
        ListenEpdParams[] rEpd = new ListenEpdParams[0];

        //dummy ashmem is not used in server side, it has been initialized and passed as placeholder only
        //tempBuffer and dummySize is used for this only
        tempBuffer = ByteBuffer.allocate(1);
        tempBuffer.put((byte)1);
        dummy = getDataInAshmemObj(tempBuffer);
        if (dummy == null) {
            Log.e(TAG, "createUdkSm: ERROR: Failed to get memory for dummy ashmem");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        rlanguageModel = getDataInAshmemObj(planguageModel);
        if (rlanguageModel == null) {
             Log.e(TAG, "createUdkSm: ERROR: Failed to get language model");
             closeSharedMemory(dummy);
             return ListenTypes.STATUS_EBAD_PARAM;
        }

        for (int i = 0; i < pnumUserRecordings; ++i) {
            ByteBuffer bBuffer = ByteBuffer.allocate(puserRecordings[i].array().length * 2);
            for (int k = 0, j = 0; k < puserRecordings[i].array().length; ++k) {
                bBuffer.put(j++, (byte)(puserRecordings[i].array()[k] & 0xff));
                bBuffer.put(j++, (byte)((puserRecordings[i].array()[k] >> 8)& 0xff));
            }
            rUserRecordings[i] = getDataInAshmemObj(bBuffer);
             if (rUserRecordings[i] == null) {
                 Log.e(TAG, "createUdkSm: ERROR: Failed to get user recording");
                 closeSharedMemory(rlanguageModel);
                 for (int j = 0; j < i; ++j)
                     closeSharedMemory(rUserRecordings[j]);
                 return ListenTypes.STATUS_EBAD_PARAM;
             }
             rUserRecordingSize[i] = (int)rUserRecordings[i].size;
        }

        try {
            // Not passing pUserDefinedKeyword, nor EPD params.
            status = mListenAidlService.LsmCreateUserDefinedKeywordModel(dummy, dummySize,
                                     pkeyPhrase, pusername, rEpd, pnumUserRecordings,
                                     rUserRecordings, rUserRecordingSize, rlanguageModel,
                                     (int)rlanguageModel.size, (int)puserDefinedSoundModel.capacity(),
                                     rUdkModel, rMatchingScore);
        } catch (RemoteException e) {
            Log.e(TAG, "createUdkSm: ERROR: Failure in LsmCreateUserDefinedKeywordModel call");
            closeSharedMemory(rUdkModel);
            closeSharedMemory(rlanguageModel);
            for (int i = 0; i < pnumUserRecordings; ++i)
                closeSharedMemory(rUserRecordings[i]);
            throw e.rethrowAsRuntimeException();
        }

        sharedFd = SharedMemory.fromFileDescriptor(rUdkModel.fd);

        if (status == ListenTypes.STATUS_SUCCESS) {
            pconfidenceData.userMatch = rMatchingScore.value;
            try {
                bbf = sharedFd.map(OsConstants.PROT_READ|OsConstants.PROT_WRITE,
                                   0, (int)rUdkModel.size);
            } catch (ErrnoException e) {
                Log.e(TAG, "createUdkSm: ERROR: Failed to map Sharedmemory : ", e);
                closeSharedMemory(rUdkModel);
                closeSharedMemory(rlanguageModel);
                for (int i = 0; i < pnumUserRecordings; ++i)
                    closeSharedMemory(rUserRecordings[i]);
                return ListenTypes.STATUS_EBAD_PARAM;
            }
            puserDefinedSoundModel.put(bbf);
            sharedFd.unmap(bbf);
        }

        closeSharedMemory(rUdkModel);
        closeSharedMemory(rlanguageModel);
        for (int i = 0; i < pnumUserRecordings; ++i)
            closeSharedMemory(rUserRecordings[i]);

        Log.i(TAG, "createUdkSm: Exit.");
        return status;
     }

     /**
      * Parses SVA detection events data (received by
      * IListenerEventProcessor.processEvent()) and fills
      * fields of a child class of DetectionData.
      * <p>
      * The data structure created and returned is determined by
      * the DetectionData.type field.
      * <p>
      * If VWU_EVENT_0100 is output as the type,
      * then a VoiceWakeupDetectionData object will be create
      * and returned. For this example, the application would cast
      * this DetectionData to a VoiceWakeupDetectionData object.
      * <p>
      * If VWU_EVENT_0200 is output as the type,
      * then a VoiceWakeupDetectionDataV2 object will be create
      * and returned. For this example, the application would cast
      * this DetectionData to a VoiceWakeupDetectionData object.
      *
      * @param registeredSoundModel [in] SoundModel that was used
      *        for registration/detection
      * @param eventPayload [in] event payload returned by
      *        ListenEngine
      *
      * @return DetectionData child object created by this method
      *         <br> null returned for EventData if
      *             input SoundModel is not an SVA SM or error occurs
      */
     public static  DetectionData parseDetectionEventData(
              ByteBuffer       registeredSoundModel,
              EventData        eventPayload)
     {
         int              status = ListenTypes.STATUS_SUCCESS;
         DetectionData    detData = null;  // generic parent class for all detection data objs
         SoundModelInfo   soundModelInfo = new SoundModelInfo();
          if ( null == soundModelInfo)  {
             Log.e(TAG, "parseDetectionEventData: Failed to SoundModelInfo object");
             return null;
         }
         status = getTypeVersion(registeredSoundModel, soundModelInfo);
         if ( ListenTypes.STATUS_SUCCESS != status ) {
             Log.e(TAG, "parseDetectionEventData: get SM Info failed with " + status);
             return null;
         }
         if (soundModelInfo.type != SVA_SOUNDMODEL_TYPE) { // check that SM is SVA type
             Log.e(TAG, "parseDetectionEventData: SM type " + soundModelInfo.type + " unsupported!");
             return null;
         }

             // Version_2.0 and above
             Log.d(TAG, "SM type is SVA 2.0");
             // Create object of type VoiceWakeupDetectionData version 2,
             //    which extends VoiceWakeupDetectionData class
             VoiceWakeupDetectionDataV2 vwuDetDataV2 = new VoiceWakeupDetectionDataV2();
             // parse SVA 2.0 and above black box eventPayload data and specifically fill
             //     VoiceWakeupDetectionDataV2 fields
             vwuDetDataV2.status = parseVWUDetectionEventDataV2(registeredSoundModel,
                                    eventPayload, vwuDetDataV2);
             vwuDetDataV2.type = ListenTypes.VWU_EVENT_0200;
             detData = vwuDetDataV2;

         if ( detData == null ) {
            Log.e(TAG, "parseDetectionEventData: returns null ptr ");
         } else if ( detData.status != ListenTypes.STATUS_SUCCESS ) {
            Log.e(TAG, "parseDetectionEventData: returns status " + detData.status);
            detData = null;
         }
         return detData;
    }

     /**
      * Parses SVA detection events data into the more meaningful
      * VoiceWakeupDetectionData data structure for SVA 1.0.
      * <p>
      * VWU_EVENT_0100 is output as the DetectionData.type field value.
      *
      * @param registeredSoundModel [in] SoundModel that was used
      *        for registration/detection
      * @param eventPayload [in] event payload returned by
      *        ListenEngine
      * @param vwuDetData [out] VoiceWakeupDetectionData
      *        structure filled when eventPayload is parsed
      * @return
      *        <br> STATUS_SUCCESS
      *        <br> STATUS_EBAD_PARAM
      *        <br> STATUS_EFAILURE
      *        <br> STATUS_ENO_MEMORY
      */
     private static native int parseVWUDetectionEventData(
                 ByteBuffer               registeredSoundModel,
                 EventData                eventPayload,
                 VoiceWakeupDetectionData vwuDetData);

     /**
      * Parses generic detection events data into the more meaningful
      * VoiceWakeupDetectionDataV2 data structure for SVA 2.0 and above.
      * <p>
      * VWU_EVENT_0200 is output as the DetectionData.type field value.
      *
      * @param registeredSoundModel [in] SoundModel that was used
      *        for registration/detection
      * @param eventPayload [in] event payload returned by
      *        ListenEngine
      * @param vwuV2DetData [out] VoiceWakeupDetectionDataV2
      *        structure filled when eventPayload is parsed
      * @return
      *        <br> STATUS_SUCCESS
      *        <br> STATUS_EBAD_PARAM
      *        <br> STATUS_EFAILURE
      *        <br> STATUS_ENO_MEMORY
      */
     private static native int parseVWUDetectionEventDataV2(
                 ByteBuffer                 registeredSoundModel,
                 EventData                  eventPayload,
                 VoiceWakeupDetectionDataV2 vwuV2DetData);
     /**
      * Query information about SoundModel.
      * <p>
      * The data structure created, filled and returned by this
      *   method is determined by the soundmodel type and version.
      * <p>
      * For example, if type is "VoiceWakeup_*", and
      *   version is "Version_2.0" or above,
      *   then a VWUSoundModelInfoV2 object will be created
      *   and returned.
      *
      * @param soundModel [in] SoundModel to query
      * @param soundModelInfo [out] reference to object that is a
      *     child of SoundModelInfo class, allocated in this method
      *
      * @return SoundModelInfo child object created by this method if successful;
      *        <br> null returned if error occurs
      */
     public static SoundModelInfo query(ByteBuffer soundModel)
     {
         Log.i(TAG, "query: Enter.");
         int status = ListenTypes.STATUS_SUCCESS;
         SoundModelInfo  genSMInfo = new SoundModelInfo();
         SVASoundModelInfo  soundModelInfo = new SVASoundModelInfo();

         // get generic info common to all soundmodel types
         status = getTypeVersion(soundModel, genSMInfo);
         if (status != ListenTypes.STATUS_SUCCESS) {
             Log.e(TAG, "query: ERROR: getTypeVersion failed, returned " + status);
             return null;
         }
         soundModelInfo.type = genSMInfo.type;
         soundModelInfo.version = genSMInfo.version;
         soundModelInfo.size = genSMInfo.size;

         // check that SM is SVA type
         if (soundModelInfo.type != SVA_SOUNDMODEL_TYPE) {
             Log.e(TAG, "query: ERROR: SM type " + genSMInfo.type + " unsupported!");
             return null;
         }

         if (genSMInfo.version == VERSION_0100)
         {
             Log.d(TAG, "query: only returns type and version for SVA 1.0 SoundModel");
             // call new native function to get extra V1 Info
             KeywordUserCounts  counts = new KeywordUserCounts();
             soundModelInfo.counts = counts;
             soundModelInfo.counts.numKeywords = 1;
             soundModelInfo.counts.numUsers = 0;
             soundModelInfo.counts.numUserKWPairs = 1;
             soundModelInfo.keywordInfo = null;
             soundModelInfo.userNames = null;
         }
         else
         {
             if (mListenAidlService != null)
                 status = getSMInfoV2(soundModel, soundModelInfo);
             else
                 status = getInfo(soundModel, soundModelInfo); // get num keys and num users
             if (status != ListenTypes.STATUS_SUCCESS) {
                Log.e(TAG, "query: ERROR: getInfoV2 failed, returned " + status);
                return null;
             }
         }
         Log.i(TAG, "query: Exit.");
         return soundModelInfo;
     }

     /**
      * Gets generic header fields common to all SoundModels.
      * <p>
      * Common SoundModelInfo type, version and size fields are
      * guaranteed to be returned by this query.
      *
      * @param soundModel [in] SoundModel to query
      * @param soundModelInfo [out] SoundModelInfo object filled by this method
      * @return status
      *        <br> STATUS_SUCCESS
      *        <br> STATUS_EBAD_PARAM
      */
     public static native int nativeGetTypeVersion(
                 ByteBuffer       soundModel,
                 SoundModelInfo   soundModelInfo);

     /**
      * Gets more detailed information about a version 2.0 and above SVA SoundModel
      * content.
      *
      * @param soundModel [in] SoundModel to query
      * @param numKeywords [in]  number of string entries allocated in keywords array
      * @param soundModelInfo [out] reference to VWUSoundModelInfo structure
      * @return status
      *        <br> STATUS_SUCCESS
      *        <br> STATUS_EBAD_PARAM
      */
     private static native int getInfo(
                 ByteBuffer          soundModel,
                 SVASoundModelInfo   vwuSMInfo);

    /**
     * Calculates size of soundmodel when array of input SoundModels are merged.
     * <p>
     * The size returned by this method must be used to create a
     * ByteBuffer that can hold the SoundModel created by
     * {@link merge(ByteBuffer[], ByteBuffer)}.
     *
     * @param  soundModels [in] array of soundModels to be merged
     *
     * @return total (unsigned) size of merged SoundModel
     * <br> Negative status value returned if error occurs.
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_ESOUNDMODELS_WITH_SAME_KEYWORD_CANNOT_BE_MERGED
     *        <br>   STATUS_ESOUNDMODELS_WITH_SAME_USER_KEYWORD_PAIR_CANNOT_BE_MERGED
     *        <br>   STATUS_EMAX_KEYWORDS_EXCEEDED
     *        <br>   STATUS_EMAX_USERS_EXCEEDED
     *
     * <p>
     * Refer to {@link merge(ByteBuffer[], ByteBuffer)}
     */
     public static native int nativeGetSizeWhenMerged(
             ByteBuffer        soundModels[]);

     public static int getSizeWhenMerged(ByteBuffer soundModels[]) {

         Log.i(TAG, " getSizeWhenMerged: Enter.");

         if (mListenAidlService == null)
             return nativeGetSizeWhenMerged(soundModels);

         int status = ListenTypes.STATUS_SUCCESS;
         int memCount = 0;
         IntegerValue mergedSize = new IntegerValue();
         Ashmem[] inModels = new Ashmem[soundModels.length];
         int[] inModelsSize = new int[soundModels.length];

         for (int i = 0; i < soundModels.length; i++) {
             if (soundModels[i] == null || !soundModels[i].hasArray()) {
                 Log.e(TAG, " getSizeWhenMerged: ERROR: Invalid sound model for " +
                       Integer.toString(i) + " sound model");
                 for (int j = 0; j < memCount; j++)
                     closeSharedMemory(inModels[j]);
                 return ListenTypes.STATUS_EBAD_PARAM;
             }
             inModels[i] = getDataInAshmemObj(soundModels[i]);
             inModelsSize[i] = (int)inModels[i].size;
             memCount++;
         }

         try {
             status = mListenAidlService.LsmGetMergedModelSize((char)soundModels.length, inModels,
                                                               inModelsSize, mergedSize);
         } catch(RemoteException e) {
             Log.e(TAG, " getSizeWhenMerged: ERROR: Failure returned from sever!!");
             for (int i = 0; i < soundModels.length; i++)
                 closeSharedMemory(inModels[i]);
             throw e.rethrowAsRuntimeException();
         }

         for (int i = 0; i < memCount; i++)
             closeSharedMemory(inModels[i]);

         if (status != 0) {
             Log.e(TAG, " getSizeWhenMerged: Faled to get size from sever, status " +
                   Integer.toString(status));
             return ConvertReturnStatus(status);
         }

         Log.i(TAG, " getSizeWhenMerged: Exit.");
         return mergedSize.value;
    }

    /**
     * Merges two or more SoundModels into a single SM.
     * <p>
     * There can not be more than one SoundModel that has keyword data
     * for the same keyword phrase.
     * <p>
     * Application is responsible for creating a ByteBuffer large
     * enough to hold the SoundModel output by this method.
     * The size of the output SoundModel can be determined calling
     * {@link getSizeWhenMerged(ByteBuffer[])}.
     *
     * @param  soundModels [in] array of soundModels to merge
     * @param  mergedSoundModel [out]  resulting merged SoundModel
     *
     * @return status
     *        <br>   STATUS_SUCCESS
     *        <br>   STATUS_EBAD_PARAM
     *        <br>   STATUS_ESOUNDMODELS_WITH_SAME_KEYWORD_CANNOT_BE_MERGED
     *        <br>   STATUS_ESOUNDMODELS_WITH_SAME_USER_KEYWORD_PAIR_CANNOT_BE_MERGED
     *        <br>   STATUS_EMAX_KEYWORDS_EXCEEDED
     *        <br>   STATUS_EMAX_USERS_EXCEEDED
     *
     * <p>
     * Refer to {@link getSizeWhenMerged(ByteBuffer[])}
     */
     public static native int nativeMerge(
             ByteBuffer        soundModels[],
             ByteBuffer        mergedModel);

     public static int merge(ByteBuffer soundModels[], ByteBuffer mergedModel) {

        if (mListenAidlService == null)
            return nativeMerge(soundModels, mergedModel);

        int status = 0;
        int mergedSize;
        int[] inModelsSize = new int[soundModels.length];
        Ashmem[] inModels = new Ashmem[soundModels.length];
        Ashmem outModel = new Ashmem();
        SharedMemory sharedFd = null;
        ByteBuffer bbf;

        mergedSize = getSizeWhenMerged(soundModels);
        if (mergedSize < 0) {
            Log.e(TAG, " merge: ERROR: Invalid sound models passed");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        if (mergedModel.capacity() < mergedSize) {
            Log.e(TAG, " merge: ERROR: Passed byte buffer is not large enough to hold merged model");
            return ListenTypes.STATUS_EBAD_PARAM;
        }

        for (int i = 0; i < soundModels.length; i++) {
            inModels[i] = getDataInAshmemObj(soundModels[i]);
            inModelsSize[i] = (int)inModels[i].size;
        }

        try {
            status = mListenAidlService.LsmMergeModels((char)soundModels.length, inModels, inModelsSize,
                                                       mergedSize, outModel);
        } catch(RemoteException e) {
            Log.e(TAG, " merge: ERROR: Failure returned from server side");
            for (int i = 0; i < soundModels.length; i++)
                closeSharedMemory(inModels[i]);
            throw e.rethrowAsRuntimeException();
        }

        if (status == 0) {
             sharedFd = SharedMemory.fromFileDescriptor(outModel.fd);
             try {
                 bbf = sharedFd.map(OsConstants.PROT_READ|OsConstants.PROT_WRITE, 0, (int)outModel.size);
             } catch (ErrnoException e) {
                 Log.e(TAG, "merge: ERROR: Failed to map Sharedmemory : ", e);
                 for (int i = 0; i < soundModels.length; i++)
                     closeSharedMemory(inModels[i]);
                 closeSharedMemory(outModel);
                 return ListenTypes.STATUS_EFAILURE;
             }
             bbf.rewind();
             mergedModel.limit(mergedModel.array().length);
             mergedModel.put(bbf);
             sharedFd.unmap(bbf);
         } else {
             Log.e(TAG, "merge: Failed to merge models" + Integer.toString(status));
         }

         for (int i = 0; i < soundModels.length; i++)
             closeSharedMemory(inModels[i]);
         closeSharedMemory(outModel);

         return ConvertReturnStatus(status);
     }

     /**
       * Get the size required to hold SoundModel that would be created by
       * {@link deleteData(ByteBuffer, String, String, ByteBuffer)}
       * executed for a keyword, a user, or a user+keyword pair.
       * <p>
       * The size returned by this method must be used to create a
       * ByteBuffer that can hold the SoundModel created by deleteData().
       *
       * @param inputSoundModel [in] SoundModel data is to be deleted from
       * @param keywordPhrase [in] name of keyword for which all data in SM should be delete
       * @param userName [in] name of user for which all data in SM should be delete
       *
       * @return total (unsigned) size of SoundModel after data is deleted
       * <br> Zero returned if error occurs.
       *        <br>   STATUS_EBAD_PARAM
       *        <br>   STATUS_EKEYWORD_NOT_IN_SOUNDMODEL
       *        <br>   STATUS_EUSER_NOT_IN_SOUNDMODEL
       *        <br>   STATUS_EKEYWORD_USER_PAIR_NOT_IN_SOUNDMODEL
       *        <br>   STATUS_ECANNOT_DELETE_LAST_KEYWORD
       *
       * <p>
       * Refer to {@link deleteData(ByteBuffer, String, String, ByteBuffer)}
       */
      public static native int nativeGetSizeAfterDelete(
             ByteBuffer        inputSoundModel,
             String            keywordPhrase,
             String            userName);

      public static int getSizeAfterDelete(
                  ByteBuffer        inputSoundModel,
                  String            keywordPhrase,
                  String            userName) {

          if (mListenAidlService == null)
              return nativeGetSizeAfterDelete(inputSoundModel, keywordPhrase,
                                              userName);

          if (inputSoundModel == null || !inputSoundModel.hasArray()) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: Invalid sound model recieved");
              return ListenTypes.STATUS_EFAILURE;
          }

          if (keywordPhrase == null && userName == null) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: keyword and user strings can't both be null");
              return ListenTypes.STATUS_EFAILURE;
          }

          int status = 0;
          int numKWs = 0, numUsers = 0;
          SoundModelHeader modelHeader = new SoundModelHeader();
          IntegerValue outSize = new IntegerValue();
          Ashmem ashSoundModel = getDataInAshmemObj(inputSoundModel);
          if (ashSoundModel == null) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: Failed to get sound model in ashmem object");
              return ListenTypes.STATUS_EFAILURE;
          }

          status = lsm_getSoundModelHeader(ashSoundModel, modelHeader);

          if (status != 0) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: getSoundModelHeader() failed");
              closeSharedMemory(ashSoundModel);
              return ListenTypes.STATUS_EFAILURE;
          } else if (modelHeader.numKeywords == 0) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: Number of users is zero");
              closeSharedMemory(ashSoundModel);
              return ListenTypes.STATUS_EFAILURE;
          } else if (modelHeader.numKeywords <= 1 && userName == null) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: keyword can not be deleted from sound model with only one keyword");
              closeSharedMemory(ashSoundModel);
              return ListenTypes.STATUS_EFAILURE;
          } else if (modelHeader.numUsers == 0 && userName != null) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: Number of users is zero, but userName is not null");
              closeSharedMemory(ashSoundModel);
              return ListenTypes.STATUS_EFAILURE;
          }

          numKWs = modelHeader.numKeywords;
          numUsers = modelHeader.numUsers;
          status = isKeywordUserInModel(ashSoundModel, numKWs, numUsers, keywordPhrase, userName);
          if (status != 0) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: Either " + keywordPhrase + " or " + userName + " is not present");
              closeSharedMemory(ashSoundModel);
              return ListenTypes.STATUS_EFAILURE;
          }

          try {
              status = mListenAidlService.LsmGetSizeAfterDeleting(ashSoundModel, (int)ashSoundModel.size, keywordPhrase, userName, outSize);
          } catch(RemoteException e) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: Failure returned from server");
              closeSharedMemory(ashSoundModel);
              throw e.rethrowAsRuntimeException();
          }

          if (status != 0) {
              Log.e(TAG, "getSizeAfterDelete: ERROR: Failure returned from server");
              outSize.value = ConvertReturnStatus(status);
          }

          status = lsm_releaseSoundModelHeader(modelHeader);
          if (status != 0)
              Log.e(TAG, "getSizeAfterDelete: ERROR: releaseSoundModelHeader() failed");

          closeSharedMemory(ashSoundModel);

          return outSize.value;
      }

      public static int isKeywordUserInModel(Ashmem soundModel, int numKWs, int numUsers, String kw, String user) {

          int status = 0;
          boolean inModel = false;
          StringValue keywordPhrases[];
          StringValue userNames[];
          IntegerValue numKeywords = new IntegerValue();
          IntegerValue numOutUsers = new IntegerValue();

          if (numKWs > 0) {
              keywordPhrases = new StringValue[numKWs];
              numKeywords.value = numKWs;
              try {
                  status = mListenAidlService.LsmGetKeywordPhrases(soundModel, (int)soundModel.size,
                                  (char)numKWs, numKeywords, keywordPhrases);
              } catch(RemoteException e) {
                  Log.e(TAG, "isKeywordUserInModel: ERROR: Failure returned from server");
                  throw e.rethrowAsRuntimeException();
              }

              if (status != 0) {
                  Log.e(TAG, "isKeywordUserIdModel: ERROR getKeywordPhrases() failed");
                  return ConvertReturnStatus(status);
              }

              for (int i = 0; i < numKWs; i++) {
                  if (kw.equals(keywordPhrases[i])) {
                      inModel = true;
                      break;
                  }
              }

              if (!inModel) {
                  Log.e(TAG,"isKeywordUserIdModel: ERROR: "+ kw + " keyphrase is not present in the sound model");
                  return ListenTypes.STATUS_EFAILURE;
              }
          }

          if (numUsers > 0) {
              inModel = false;
              userNames = new StringValue[numUsers];
              numOutUsers.value = numUsers;
              try {
                  status = mListenAidlService.LsmGetUserNames(soundModel, (int)soundModel.size,
                                  (char)numKWs, numOutUsers, userNames);
              } catch(RemoteException e) {
                  Log.e(TAG, "isKeywordUserInModel: ERROR: Failure returned from server");
                  throw e.rethrowAsRuntimeException();
              }

              if (status != 0) {
                  Log.e(TAG, "isKeywordUserIdModel: ERROR getKeywordPhrases() failed");
                  return ListenTypes.STATUS_EFAILURE;
              }

              for (int i = 0; i < numUsers; i++) {
                  if (user.equals(userNames[i])) {
                      inModel = true;
                      break;
                  }
              }

              if (!inModel) {
                  Log.e(TAG, user + " user is not present in the sound model");
                  return ConvertReturnStatus(status);
              }
          }

          return status;
      }

     /**
       * Deletes specific data from a given SoundModel
       * <p>
       * Returns a new SoundModel after removing data a keyword, a user,
       * or a user+keyword pair from a given SoundModel.  Exactly which data is
       * deleted depends on combination of input parameter Strings
       * keywordPhrase and userName.
       * <p>
       * If keywordPhrase is non-null, but userName is null then all data associated
       * with a particular keyword (including user+KW pairings) is removed.
       * <p>
       * If keywordPhrase is null, but userName is non-null then all data associated
       * with a particular user (including user+keyword pairings) is removed
       * <p>
       * If both keywordPhrase and userName are non-null then only data for a
       * particular user+keyword pair is removed
       * <p>
       * Application is responsible for creating a ByteBuffer large
       * enough to hold the SoundModel output by this method.
       * The size of the output SoundModel can be determined calling
       * {@link getSizeAfterDelete(ByteBuffer,String, String)}.
       *
       * @param inputSoundModel [in] SoundModel data is to be deleted from
       * @param keywordPhrase [in] name of keyword for which all data in SM should be delete
       * @param userName [in] name of user for which all data in SM should be delete
       * @param outputSoundModel [out] new SoundModel with data removed
       *
       * @return status
       *        <br>   STATUS_SUCCESS
       *        <br>   STATUS_EBAD_PARAM
       *        <br>   STATUS_EKEYWORD_NOT_IN_SOUNDMODEL
       *        <br>   STATUS_EUSER_NOT_IN_SOUNDMODEL
       *        <br>   STATUS_EKEYWORD_USER_PAIR_NOT_IN_SOUNDMODEL
       *        <br>   STATUS_ECANNOT_DELETE_LAST_KEYWORD
       *
       * <p>
       * Refer to {@link getSizeAfterDelete(ByteBuffer,String, String)}
       */
      public static native int nativeDeleteData(
             ByteBuffer        inputSoundModel,
             String            keywordPhrase,
             String            userName,
             ByteBuffer        outputSoundModel);

      public static int deleteData(ByteBuffer inputSoundModel, String keywordPhrase,
                                   String userName, ByteBuffer outputSoundModel) {

          if (mListenAidlService == null)
              return nativeDeleteData(inputSoundModel, keywordPhrase,
                                      userName, outputSoundModel);

          if (inputSoundModel == null || !inputSoundModel.hasArray()) {
              Log.e(TAG, "deleteData: ERROR: Invalid sound model recieved");
              return ListenTypes.STATUS_EFAILURE;
          }

          if (keywordPhrase == null && userName == null) {
              Log.e(TAG, "deleteData: ERROR: keyword and user strings can't both be null");
              return ListenTypes.STATUS_EFAILURE;
          }

          int status = 0;
          int outModelSize = ListenSoundModel.getSizeAfterDelete(inputSoundModel, keywordPhrase, userName);
          Ashmem inModel = getDataInAshmemObj(inputSoundModel);
          Ashmem outModel = new Ashmem();
          SharedMemory sharedFd = null;
          ByteBuffer bbf;

          if (outputSoundModel.capacity() < outModelSize) {
              Log.e(TAG, "deleteData: ERROR: Output Bytebuffer is not large enough to hold updated SM");
              return ListenTypes.STATUS_EBAD_PARAM;
          }

          try {
              status = mListenAidlService.LsmDeleteFromModel(inModel, (int)inModel.size, keywordPhrase,
                                                             userName, outModelSize, outModel);
          } catch(RemoteException e) {
              Log.e(TAG, "deleteData: ERROR: Failure returned from server");
              closeSharedMemory(inModel);
              throw e.rethrowAsRuntimeException();
          }

         if (status == 0) {
             sharedFd = SharedMemory.fromFileDescriptor(outModel.fd);
             try {
                 bbf = sharedFd.map(OsConstants.PROT_READ|OsConstants.PROT_WRITE, 0, (int)outModel.size);
             } catch (ErrnoException e) {
                 Log.e(TAG, "deleteData: ERROR: Failed to map Sharedmemory : ", e);
                 closeSharedMemory(inModel);
                 closeSharedMemory(outModel);
                 return ConvertReturnStatus(status);
             }
             bbf.rewind();
             outputSoundModel.limit(outputSoundModel.array().length);
             outputSoundModel.put(bbf);
             sharedFd.unmap(bbf);
         } else {
             Log.e(TAG, "deleteData: Failed to delete data at server side " + Integer.toString(status));
         }

         closeSharedMemory(inModel);
         closeSharedMemory(outModel);

         return ConvertReturnStatus(status);
      }
}
