/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.enrollment;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.quicinc.voice.activation.aidl.IEnrollmentService;
import com.quicinc.voice.activation.aidl.IGenerateSoundModelCallback;
import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.assist.sdk.enrollment.Enrollment.EnrollmentType;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voice.assist.sdk.utility.DataUtils;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;
import com.quicinc.voice.assist.sdk.utility.UIThreadExecutor;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A public class which contains all the user voice enrollment API.
 */
public class EnrollmentManager {
    private static final String TAG = EnrollmentManager.class.getSimpleName();
    private static final String KEY_ENROLLMENT_TYPE = "Enrollment.type";
    private static final String KEY_ENROLLMENT_PDK_SOUND_MODEL_NAME =
            "Enrollment.pdkSoundModelName";
    private static final String KEY_ENROLLMENT_USER_ID = "Enrollment.userId";
    private static final String KEY_ENROLLMENT_LOCALE = "Enrollment.locale";
    private static final String KEY_ENROLLMENT_RECORDING_TYPE = "Enrollment.recordingType";
    private static final String KEY_UTTERANCE_INFO_ID = "UtteranceInfo.id";
    private static final String KEY_UTTERANCE_FEEDBACK_PCM_DATA = "UtteranceFeedback.pcmData";
    private static final String KEY_UTTERANCE_FEEDBACK_VOLUME = "UtteranceFeedback.volume";
    private static final String KEY_UTTERANCETRAINING_SNR = "UtteranceTrainingData.SNR";
    private static final String KEY_UTTERANCETRAINING_CONFIDENCE = "UtteranceTrainingData.confidence";
    private final static String KEY_ENROLLMENT_DATA_QUALITY = "UVREnrollment.quality_score";
    /**
     * the current running Enrollment.
     */
    private static Enrollment mRunningEnrollment;
    /**
     * The File to write utterance data.
     */
    private static File mUtteranceFile;

    public static void startUserVoiceEnrollment(EnrollmentConnector connector,
                                                Enrollment enrollment,
                                                WeakReference<IOperationCallback<ArrayList<String>,
                                                EnrollmentFailedReason>> callbackRef) {
        IOperationCallback<ArrayList<String>, EnrollmentFailedReason> callback = callbackRef.get();
        if (callback == null) return;
        IEnrollmentService enrollmentService = checkAndGetEnrollmentService(connector);
        if (enrollmentService == null) {
            UIThreadExecutor
                    .failed(callbackRef, EnrollmentFailedReason.INIT_FAILURE_SERVICE_NOT_CONNECTED);
            return;
        }
        if (isEnrollmentInvalid(enrollment)) {
            UIThreadExecutor.failed(callbackRef, EnrollmentFailedReason.INIT_FAILURE_ILLEGAL_MODEL);
            return;
        }

        Bundle bundle = parseEnrollment(enrollment);
        ParcelFileDescriptor pfd = getSoundModelFileDescriptor(enrollment);
        mRunningEnrollment = enrollment;
        IResultCallback iResultCallback =
                buildStartEnrollmentCallback(enrollmentService, enrollment, callbackRef);

        try {
            Log.d(TAG, "start user voice enrollment");
            enrollmentService.startUserVoiceEnrollment(bundle, pfd, iResultCallback);
        } catch (Exception e) {
            Log.e(TAG,
                    "call startUserVoiceEnrollment error, " + e.getLocalizedMessage());
            mRunningEnrollment = null;
            DataUtils.closeFD(pfd);
            UIThreadExecutor
                    .failed(callbackRef, EnrollmentFailedReason.INIT_FAILURE_SERVICE_NOT_CONNECTED);
        }
    }

    public static void startUtteranceTraining(EnrollmentConnector connector,
                                              String utteranceId,
                                              WeakReference<IUtteranceCallback> callbackRef) {
        IUtteranceCallback callback = callbackRef.get();
        if (callback == null) return;
        IEnrollmentService enrollmentService = checkAndGetEnrollmentService(connector);
        if (enrollmentService == null) {
            UIThreadExecutor.execute(new WeakReference<Runnable>(
                    () -> callback.onFailure(utteranceId, null,
                            "service not connected")));
            return;
        }
        Bundle bundle = generateTrainingInfo(utteranceId);
        WeakReference<com.quicinc.voice.activation.aidl.IUtteranceCallback> iUtteranceCallback
                = new WeakReference<com.quicinc.voice.activation.aidl.IUtteranceCallback>(
                        buildUtteranceCallback(utteranceId, callbackRef));
        try {
            Log.d(TAG, "start utterance training");
            enrollmentService.startUtteranceTraining(iUtteranceCallback.get(), bundle);
        } catch (Exception e) {
            Log.e(TAG,
                    "call startUserVoiceEnrollment error, " + e.getLocalizedMessage());
            UIThreadExecutor.execute(new WeakReference<Runnable>(
                    () -> callback.onFailure(utteranceId, null,
                            "service not connected")));
        }
    }

    public static void cancelUtteranceTraining(EnrollmentConnector connector,
                                               String utteranceId,
                                               IOperationCallback<Bundle, EnrollmentFailedReason> callback) {
        if (callback == null) return;
        IEnrollmentService enrollmentService = checkAndGetEnrollmentService(connector);
        if (enrollmentService == null) {
            UIThreadExecutor
                    .failed(callback, EnrollmentFailedReason.INIT_FAILURE_SERVICE_NOT_CONNECTED);
            return;
        }
        Bundle bundle = generateTrainingInfo(utteranceId);
        try {
            Log.d(TAG, "cancel utterance training");
            enrollmentService.cancelUtteranceTraining(new IResultCallback.Stub() {
                @Override
                public void onSuccess(Bundle returnValues) {
                    UIThreadExecutor.success(callback, returnValues);
                }

                @Override
                public void onFailure(Bundle params) {
                    UIThreadExecutor.failed(callback,
                            EnrollmentFailedReason
                                    .getFailedReason(params.getInt(Constants.KEY_ERROR_CODE, -1)));
                }
            }, bundle);
        } catch (Exception e) {
            Log.e(TAG,
                    "call cancelUtteranceTraining error, " + e.getLocalizedMessage());
            UIThreadExecutor
                    .failed(callback, EnrollmentFailedReason.INIT_FAILURE_SERVICE_NOT_CONNECTED);
        }

    }

    /**
     * Method to finish the voice enrollment process. This method will be called after the user went
     * through all utterances and finished the training. As a result, a voice model must be
     * created.
     *
     * @param connector Used to access QVA correlative services. If no connected, nothing happens.
     * @param callback  If <code>onSuccess</code> the parameter is full path of sound model file,
     *                  if
     *                  <code>onFailure</code>, the error reason with type {@link
     *                  EnrollmentFailedReason} will be
     *                  returned. The methods on this callback are called from the main thread of
     *                  your process.
     */
    public static void commitUserVoiceEnrollment(EnrollmentConnector connector,
                                                   WeakReference<IOperationCallback<
                                                           EnrollmentSuccessInfo,
                                                         EnrollmentFailedReason>> callbackRef) {
        IOperationCallback<EnrollmentSuccessInfo, EnrollmentFailedReason> callback =
                callbackRef.get();
        if (callback == null) return;
        IEnrollmentService enrollmentService = checkAndGetEnrollmentService(connector);
        if (enrollmentService == null) {
            UIThreadExecutor
                    .failed(callbackRef, EnrollmentFailedReason.INIT_FAILURE_SERVICE_NOT_CONNECTED);
            return;
        }
        Log.d(TAG, "commit user voice enrollment");
        transferCleanUtteranceIfNeed(enrollmentService, callback, () -> {
            try {
                enrollmentService.commitUserVoiceEnrollment(
                        buildGenerateCallback(callbackRef), null);
            } catch (Exception e) {
                Log.e(TAG,
                        "call commitUserVoiceEnrollment error, " + e.getLocalizedMessage());
                UIThreadExecutor
                        .failed(callbackRef,
                                EnrollmentFailedReason.INIT_FAILURE_SERVICE_NOT_CONNECTED);
            }
        });
    }

    private static void transferCleanUtteranceIfNeed(IEnrollmentService enrollmentService,
                                                     IOperationCallback<EnrollmentSuccessInfo,
                                                             EnrollmentFailedReason> callback,
                                                     Runnable nextStep) {
        if (mRunningEnrollment.getRecordingType() == Enrollment.ENROLLMENT_RECORDING_TYPE_CLEAN) {
            nextStep.run();
            return;
        }
        try {
            Path utteranceCleanPath = DataUtils.getUtterancePath(
                    mRunningEnrollment.getBaseSoundModelFileName(),
                    mRunningEnrollment.getUserId(),
                    Enrollment.ENROLLMENT_RECORDING_TYPE_CLEAN);
            String[] utteranceClean = DataUtils.getUtterances(utteranceCleanPath);
            int utteranceCleanNumber = utteranceClean.length;
            if (utteranceCleanNumber == 0) {
                UIThreadExecutor.failed(callback,
                        EnrollmentFailedReason.INIT_FAILURE_SERVICE_NOT_CONNECTED);
                return;
            }

            int configCleanTrainNumber = mRunningEnrollment.getCleanUtterances();
            for (int i = 0; i < configCleanTrainNumber; i++) {
                int index = i % utteranceCleanNumber;
                if (TextUtils.isEmpty(utteranceClean[index])) {
                    continue;
                }
                File utterance = new File(utteranceClean[index]);
                byte[] utteranceByte = new byte[0];
                try {
                    utteranceByte = Files.readAllBytes(utterance.toPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Bundle request = new Bundle();
                request.putShortArray(utterance.getName(), bytesToShorts(utteranceByte));
                enrollmentService.transferRecording(request);
            }
            nextStep.run();
        } catch (Exception e) {
            UIThreadExecutor.failed(callback,
                    EnrollmentFailedReason.INIT_FAILURE_SERVICE_NOT_CONNECTED);
        }
    }

    private static short[] bytesToShorts(byte[] bytes) {
        short[] shortOut = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asShortBuffer().get(shortOut);
        return shortOut;
    }

    /**
     * Method to finish the voice enrollment process. This method will be called after the user went
     * through all utterances and finished the training, or user just want to cancel this
     * enrollment.
     *
     * @param connector Used to access QVA correlative services. If no connected, nothing happens.
     */
    public static void finishUserVoiceEnrollment(EnrollmentConnector connector) {
        IEnrollmentService enrollmentService = checkAndGetEnrollmentService(connector);
        if (enrollmentService != null) {
            try {
                if (mRunningEnrollment != null) {
                    Log.d(TAG, "cancel user voice enrollment");
                    enrollmentService.cancelUtteranceTraining(new IResultCallback.Stub() {

                        @Override
                        public void onSuccess(Bundle returnValues) {
                            Log.d(TAG,
                                    "cancelUtteranceTraining success");
                        }

                        @Override
                        public void onFailure(Bundle params) {
                            Log.d(TAG,
                                    "cancelUtteranceTraining failed");
                        }
                    }, Bundle.EMPTY);
                    enrollmentService.cancelUserVoiceEnrollment(new IResultCallback.Stub() {
                        @Override
                        public void onSuccess(Bundle returnValues) {
                            Log.i(TAG,
                                    "cancelUserVoiceEnrollment success");
                        }

                        @Override
                        public void onFailure(Bundle params) {
                            Log.i(TAG,
                                    "cancelUserVoiceEnrollment failed");
                        }
                    }, null);

                    clearUtterancesAfterCancel(mRunningEnrollment);
                } else {
                    Log.d(TAG, "finish user voice enrollment");
                    enrollmentService.finishUserVoiceEnrollment(null);
                }
            } catch (Exception e) {
                Log.e(TAG,
                        "call finishUserVoiceEnrollment error, " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Method to remove the unique platform UV data.
     *
     * @param connector Used to access QVA correlative services. If no connected, nothing happens.
     * @param callback The methods on this callback are called from the main thread of your process.
     */
    public static void removeGeneralUV(EnrollmentConnector connector,
                                       IOperationCallback<String, String> callback) {
        if (callback == null) {
            return;
        }
        IEnrollmentService enrollmentService = checkAndGetEnrollmentService(connector);
        if (enrollmentService != null) {
            try {
                Log.d(TAG, "removeGeneralUV");
                enrollmentService.removeGeneralUV(new IResultCallback.Stub() {
                    @Override
                    public void onSuccess(Bundle returnValues) {
                        Log.i(TAG, "removeGeneralUV success");
                        UIThreadExecutor.execute(
                                () -> callback.onSuccess("removeGeneralUV success"));
                    }

                    @Override
                    public void onFailure(Bundle params) {
                        Log.i(TAG, "removeGeneralUV failed");
                        UIThreadExecutor.execute(
                                () -> callback.onFailure("removeGeneralUV failed"));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG,
                        "call removeGeneralUV error, " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Method to check if there's a platform UV enrolled.
     *
     * @param connector Used to access QVA correlative services. If no connected, nothing happens.
     */
    public static boolean isGeneralUVEnrolled(EnrollmentConnector connector) {
        IEnrollmentService enrollmentService = checkAndGetEnrollmentService(connector);
        if (enrollmentService != null) {
            try {
                boolean result = enrollmentService.isGeneralUVEnrolled();
                Log.d(TAG, "isGeneralUVEnrolled: " + result);
                return result;
            } catch (Exception e) {
                Log.e(TAG,
                        "call isGeneralUVEnrolled error, " + e.getLocalizedMessage());
            }
        }
        return false;
    }

    private static IEnrollmentService checkAndGetEnrollmentService(
            EnrollmentConnector connector) {
        if (connector != null) {
            return connector.getEnrollmentService();
        }
        return null;
    }

    private static Bundle parseEnrollment(Enrollment enrollment) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ENROLLMENT_TYPE, enrollment.getEnrollmentType());
        bundle.putString(KEY_ENROLLMENT_USER_ID, enrollment.getUserId());
        bundle.putString(KEY_ENROLLMENT_PDK_SOUND_MODEL_NAME,
                enrollment.getBaseSoundModelFileName());
        bundle.putString(KEY_ENROLLMENT_LOCALE, enrollment.getLocale());
        bundle.putInt(KEY_ENROLLMENT_RECORDING_TYPE, enrollment.getRecordingType());
        return bundle;
    }


    private static ParcelFileDescriptor getSoundModelFileDescriptor(Enrollment enrollment) {
        ParcelFileDescriptor pfd = null;
        if (enrollment.getEnrollmentType() == EnrollmentType.PDK.value()) {
            pfd = DataUtils.openSoundModel(enrollment.getBaseSoundModelPath());
        }
        return pfd;
    }

    private static IResultCallback buildStartEnrollmentCallback(IEnrollmentService service,
                                                                Enrollment enrollment,
                                                                WeakReference<
                                                                IOperationCallback<ArrayList<String>,
                                                                        EnrollmentFailedReason>> callback) {
        return new IResultCallback.Stub() {

            @Override
            public void onSuccess(Bundle bundle) {
                ArrayList<String> res = new ArrayList<>();
                try {
                    List<Bundle> bundles = service.getUtterancesInfo(Bundle.EMPTY);
                    if (bundles != null) {
                        for (Bundle b : bundles) {
                            String id = b.getString(KEY_UTTERANCE_INFO_ID);
                            res.add(id);
                        }
                    }
                    UIThreadExecutor.success(callback, res);
                } catch (Exception e) {
                    Log.e(TAG,
                            "call getUtterancesInfo error, " + e.getLocalizedMessage());
                    UIThreadExecutor.failed(callback, EnrollmentFailedReason.INIT_FAILURE_UNKNOWN);
                }
            }

            @Override
            public void onFailure(Bundle bundle) {
                if (bundle == null) bundle = Bundle.EMPTY;
                String message = bundle.getString(Constants.KEY_ERROR_MESSAGE);
                mRunningEnrollment = null;
                int errorCode = bundle.getInt(Constants.KEY_ERROR_CODE);
                Log.i(TAG, "failed reason:" + message);
                UIThreadExecutor
                        .failed(callback, EnrollmentFailedReason.getFailedReason(errorCode));
            }

        };
    }

    private static com.quicinc.voice.activation.aidl.IUtteranceCallback buildUtteranceCallback(
            String utteranceId, WeakReference<IUtteranceCallback> callbackRef) {
        return new com.quicinc.voice.activation.aidl.IUtteranceCallback.Stub() {
            @Override
            public void onStartRecording(Bundle params) {
                String path = mRunningEnrollment.getUtterancePath();
                File utterancesDir = new File(path);
                mUtteranceFile = new File(utterancesDir, utteranceId);
                try {
                    if (Files.notExists(utterancesDir.toPath())) {
                        Files.createDirectories(utterancesDir.toPath());
                    }
                    Files.deleteIfExists(mUtteranceFile.toPath());
                    Files.createFile(mUtteranceFile.toPath());
                } catch (IOException e) {
                    Log.e(TAG,
                            "mUtteranceFile create failed, " + Log.getStackTraceString(e));
                }
                UIThreadExecutor.execute(new WeakReference<Runnable>(callbackRef.get()::onStartRecording));
            }

            @Override
            public void onStopRecording(Bundle params) {
                Log.d(TAG, "onStopRecording");
                UIThreadExecutor.execute(new WeakReference<Runnable>(callbackRef.get()::onStopRecording));
            }

            @Override
            public void onStartProcessing(Bundle params) {

            }

            @Override
            public void onStopProcessing(Bundle params) {

            }

            @Override
            public void onSuccess(Bundle params) {
                if (params == null) params = Bundle.EMPTY;
                String utteranceId = params.getString(KEY_UTTERANCE_INFO_ID);
                Log.d(TAG,
                        "enroll success utterance id:" + utteranceId);
                EnrollmentExtras extras = getEnrollmentExtras(params);
                UIThreadExecutor.execute(new WeakReference<Runnable>(
                        () -> callbackRef.get().onSuccess(utteranceId, extras)));
            }

            @Override
            public void onError(Bundle params) {
                if (params == null) params = Bundle.EMPTY;
                String utteranceId = params.getString(KEY_UTTERANCE_INFO_ID);
                String reason = params.getString(Constants.KEY_ERROR_MESSAGE);
                Log.d(TAG,
                        "enroll failed utterance id:" + utteranceId);
                EnrollmentExtras extras = getEnrollmentExtras(params);
                UIThreadExecutor.execute(new WeakReference<Runnable>(() -> callbackRef.get()
                        .onFailure(utteranceId, extras, reason)));
            }

            private EnrollmentExtras getEnrollmentExtras(Bundle params) {
                int snr = params.getInt(KEY_UTTERANCETRAINING_SNR, 0);
                int confidence = params.getInt(KEY_UTTERANCETRAINING_CONFIDENCE, 0);
                return new EnrollmentExtras(snr, confidence);
            }

            @Override
            public void onFeedback(Bundle params) {
                short[] pcm = params.getShortArray(KEY_UTTERANCE_FEEDBACK_PCM_DATA);
                if (pcm != null) {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(pcm.length * 2);
                        buffer.order(ByteOrder.nativeOrder());
                        buffer.asShortBuffer().put(pcm);
                        Files.write(mUtteranceFile.toPath(), buffer.array(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        Log.e(TAG,
                                "save pcm error, " + Log.getStackTraceString(e));
                    }
                }
                int volume = (int) params.getFloat(KEY_UTTERANCE_FEEDBACK_VOLUME);
                Log.d(TAG,
                        "utterance onFeedback volume=" + volume);
                UIThreadExecutor.execute(new WeakReference<Runnable>(
                        () -> callbackRef.get().onFeedback(volume)));
            }
        };
    }

    private static IGenerateSoundModelCallback buildGenerateCallback(WeakReference<
            IOperationCallback<EnrollmentSuccessInfo, EnrollmentFailedReason>> callbackRef) {
        IOperationCallback callback = callbackRef.get();
        return new IGenerateSoundModelCallback.Stub() {
            @Override
            public void onTrainSoundModelSuccess(ParcelFileDescriptor soundModelDescriptor,
                                                 Bundle params) {
                String path = mRunningEnrollment.getSoundModelPath();
                DataUtils.saveSoundModel(path, soundModelDescriptor);
                mRunningEnrollment = null;
                if (params == null) params = Bundle.EMPTY;
                int score = params.getInt(KEY_ENROLLMENT_DATA_QUALITY, 0);
                EnrollmentSuccessInfo result = new EnrollmentSuccessInfo(path, score);
                Log.d(TAG,
                        "onTrainSoundModelSuccess path:" + path + " score =" + score);
                UIThreadExecutor.success(callbackRef, result);
            }

            @Override
            public void onTrainSoundModelFailure(Bundle params) {
                if (params == null) params = Bundle.EMPTY;
                int errorCode = params.getInt(Constants.KEY_ERROR_CODE);
                UIThreadExecutor
                        .failed(callbackRef, EnrollmentFailedReason.getFailedReason(errorCode));
            }
        };
    }

    private static Bundle generateTrainingInfo(String utteranceId) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_UTTERANCE_INFO_ID, utteranceId);
        return bundle;
    }

    private static boolean isEnrollmentInvalid(Enrollment enrollment) {
        if (enrollment != null) {
            if (enrollment.getEnrollmentType() == EnrollmentType.PDK.value()
                    && !TextUtils.isEmpty(enrollment.getUserId())
                    && !TextUtils.isEmpty(enrollment.getBaseSoundModelFileName())) {
                return !enrollment.getBaseSoundModelFileName()
                        .endsWith(Constants.SUFFIX_PRESET_SOUND_MODEL);
            } else if (enrollment.getEnrollmentType() == EnrollmentType.TI.value()) {
                return TextUtils.isEmpty(enrollment.getUserId());
            }
        }
        return true;
    }

    private static void clearUtterancesAfterCancel(Enrollment enrollment) {
        DataUtils.deleteUtterances(Paths.get(enrollment.getUtterancePath()));
    }
}
