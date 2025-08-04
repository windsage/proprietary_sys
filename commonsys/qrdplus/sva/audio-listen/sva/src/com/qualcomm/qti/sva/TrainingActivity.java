/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.listen.ListenSoundModel;
import com.qualcomm.listen.ListenTypes;
import com.qualcomm.listen.ListenTypes.QualityCheckInfo;
import com.qualcomm.qti.sva.controller.ExtendedSmMgr;
import com.qualcomm.qti.sva.controller.Global;
import com.qualcomm.qti.sva.controller.RecordingsMgr;
import com.qualcomm.qti.sva.controller.SMLParametersManager;
import com.qualcomm.qti.sva.controller.SMLParametersManager.*;
import com.qualcomm.qti.sva.data.IExtendedSmModel;
import com.qualcomm.qti.sva.data.ISettingsModel;
import com.qualcomm.qti.sva.data.ISmModel;
import com.qualcomm.qti.sva.data.SettingsModel;
import com.qualcomm.qti.sva.data.ISmModel.ModelVersion;
import com.qualcomm.qti.sva.onlineepd.EPD;
import com.qualcomm.qti.sva.utils.FileUtils;
import com.qualcomm.qti.sva.utils.LogUtils;
import com.qualcomm.qti.sva.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class TrainingActivity extends Activity {
    public static final String KEY_EXTRA_PREVIOUS_SM_NAME = "previous_sm_name";
    public static final String KEY_EXTRA_KEYPHRASE_OR_NEW_SM_NAME = "keyphrase_or_new_sm_name";
    public static final String KEY_EXTRA_USER_NAME = "user_name";
    public static final String KEY_EXTRA_IS_ADD_USER_TO_PREVIOUS_MODEL
            = "is_add_user_to_previous_sm";
    public static final String KEY_RECORDING_TYPE = "recording_type";
    public static final String KEY_TEXT_INPUT = "text_input";
    private final static int SNR_THRESHOLD = 16;
    private final static int DURATION_OF_TRAINING_RECORDING = 3000; // 3s
    private static final int START_RECORDING_DELAY = 3000; //3s
    private final static int MSG_STOP_TRAINING_RECORDING = 1;
    private final static int MSG_START_TRAINING_RECORDING = 2;

    private final String TAG = TrainingActivity.class.getSimpleName();

    private ExtendedSmMgr mExtendedSmMgr;
    private RecordingsMgr mRecordingMgr;
    private String mBaseSoundModel;
    private String mPreviousSmName;
    private String mKeyphraseOrNewSmName;
    private String mUserName;
    private String mTextInput = null;
    private boolean mIsAddUserToPreviousSm;
    private int mRecodingType;
    private int mRecordingTimes;
    private ISmModel.ModelVersion mUdkVersion;
    private RecordingCounter mRecordingCounter;
    private ShortBuffer[] mCleanRecordings;

    // view variable define
    private TextView mTvUserName;
    private ImageButton mIbMic;

    private Timer mRecordingTimer;
    private boolean mIsTraining = false;
    private VerifyRecordingTask mVerifyRecordingTask;
    private CreateSmTask mCreateSmTask;
    private AlertDialog mTerminateTrainingDialog;
    private AlertDialog mTrainingDialog;
    private String mTargetSoundModelName;

    private TextView mStepView;
    private TextView mSayKeyword;
    private TextView mDidNotHearClearly;
    private TextView mTrainFinish;
    private ImageView mTrainSuccess;
    private ProgressBar mTrainSuccessGenerate;
    private SoundWave mSoundWave;
    private Button mOk;
    private EPD mEPD = null;
    private volatile boolean mIsEPDSuccess = false;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            LogUtils.d(TAG, "handleMessage: what = " + msg.what);
            switch (msg.what) {
                case MSG_STOP_TRAINING_RECORDING:
                    onReceivedStopTrainingRecordingMsg();
                    break;
                case MSG_START_TRAINING_RECORDING:
                    startRecording();
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    private void onReceivedStopTrainingRecordingMsg() {
        mRecordingMgr.stopTrainingRecording(
                mRecordingMgr.getLastUserRecordingFilePath());

        // verify recording
        mVerifyRecordingTask = new VerifyRecordingTask();
        mVerifyRecordingTask.execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mPreviousSmName = intent.getStringExtra(KEY_EXTRA_PREVIOUS_SM_NAME);
        mKeyphraseOrNewSmName = intent.getStringExtra(KEY_EXTRA_KEYPHRASE_OR_NEW_SM_NAME);
        mUserName = intent.getStringExtra(KEY_EXTRA_USER_NAME);
        mIsAddUserToPreviousSm = intent.getBooleanExtra(KEY_EXTRA_IS_ADD_USER_TO_PREVIOUS_MODEL,
                false);
        mRecodingType = intent.getIntExtra(KEY_RECORDING_TYPE,
                SMLParametersManager.RECORDING_IN_CLEAN_ENVIRONMENT);
        mUdkVersion = Utils.getUdkVersion(this, mPreviousSmName);
        if(mUdkVersion.getVersionNumber() == ISmModel.ModelVersion.VERSION_7_0.getVersionNumber())
        {
            mTextInput = intent.getStringExtra(KEY_TEXT_INPUT);
        }
        LogUtils.d(TAG, "onCreate: mPreviousSmName = " + mPreviousSmName
                + " mKeyphraseOrNewSmName = " + mKeyphraseOrNewSmName
                + " mUserName = " + mUserName
                + " mTextInput = " + mTextInput
                + " mIsAddUserToPreviousSm = " + mIsAddUserToPreviousSm
                + " mRecodingType = " + mRecodingType);

        mExtendedSmMgr = Global.getInstance().getExtendedSmMgr();
        mRecordingMgr = Global.getInstance().getRecordingsMgr();

        updateRecordingTimes();
        setSMLParameters(mTextInput);
        setTrainingRecordingParams();

        setContentView(R.layout.activity_training);
        Utils.setUpEdgeToEdge(this);
        initializeUI();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        LogUtils.d(TAG, "onNewIntent: intent = " + intent.toString());
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // stop ongoing training if has
        stopTraining();

        // Get the new intent
        Intent intent = getIntent();
        mPreviousSmName = intent.getStringExtra(KEY_EXTRA_PREVIOUS_SM_NAME);
        mKeyphraseOrNewSmName = intent.getStringExtra(KEY_EXTRA_KEYPHRASE_OR_NEW_SM_NAME);
        mUserName = intent.getStringExtra(KEY_EXTRA_USER_NAME);
        mIsAddUserToPreviousSm = intent.getBooleanExtra(KEY_EXTRA_IS_ADD_USER_TO_PREVIOUS_MODEL,
                false);
        mRecodingType = intent.getIntExtra(KEY_RECORDING_TYPE,
                SMLParametersManager.RECORDING_IN_CLEAN_ENVIRONMENT);
        mUdkVersion = Utils.getUdkVersion(this, mPreviousSmName);
        if(mUdkVersion.getVersionNumber() == ISmModel.ModelVersion.VERSION_7_0.getVersionNumber())
        {
            mTextInput = intent.getStringExtra(KEY_TEXT_INPUT);
        }
        LogUtils.d(TAG, "onRestart: mPreviousSmName = " + mPreviousSmName
                + " mKeyphraseOrNewSmName = " + mKeyphraseOrNewSmName
                + " mUserName = " + mUserName
                + " mTextInput = " + mTextInput
                + " mIsAddUserToPreviousSm = " + mIsAddUserToPreviousSm
                + " mRecodingType = " + mRecodingType);

        updateRecordingTimes();
        setSMLParameters(mTextInput);
        setTrainingRecordingParams();
        mTvUserName.setText(mUserName);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.d(TAG, "onStop: enter");
        stopTraining();
    }

    @Override
    protected void onDestroy() {
        stopRecordingTimer();

        // clear the user recording mem data
        mRecordingMgr.removeUserRecordings();
        mRecordingMgr.setAudioDataUpdatedListener(null);

        if (null != mVerifyRecordingTask && mVerifyRecordingTask.isCancelled()) {
            mVerifyRecordingTask.cancel(true);
            mVerifyRecordingTask = null;
        }

        if (null != mCreateSmTask && mCreateSmTask.isCancelled()) {
            mCreateSmTask.cancel(true);
            mCreateSmTask = null;
        }

        if (null != mTerminateTrainingDialog && mTerminateTrainingDialog.isShowing()) {
            mTerminateTrainingDialog.dismiss();
            mTerminateTrainingDialog = null;
        }

        if (null != mTrainingDialog && mTrainingDialog.isShowing()) {
            mTrainingDialog.dismiss();
            mTrainingDialog = null;
        }

        if (null != mHandler) {
            mHandler.removeCallbacksAndMessages(null);
        }

        super.onDestroy();
    }

    private void setSMLParameters(String textinput) {
        if (isUdk() && mUdkVersion != ModelVersion.VERSION_7_0) {
            SMLParametersManager.getInstance().setSMLUDKParameters(textinput);
        } else {
            SMLParametersManager.getInstance().setSMLPDKParameters();
        }
    }

    private void setTrainingRecordingParams() {
        if (!isUdk()) {
            if (mRecodingType == SMLParametersManager.RECORDING_IN_CLEAN_ENVIRONMENT) {
                mBaseSoundModel = mPreviousSmName;
            } else {
                ISettingsModel settingsModel = new SettingsModel(getApplicationContext(),
                        mPreviousSmName);
                mBaseSoundModel = settingsModel.getBaseSoundModel();
            }
        }
        mTargetSoundModelName = getTargetSoundModelName();
        ISettingsModel sm = new SettingsModel(getApplicationContext(), null);
        int globalTrainingPath = sm.getGlobalTrainingPath();
        mRecordingMgr.setTrainingRecordingParams(mTargetSoundModelName, mUserName,
                mRecodingType, mRecordingTimes, globalTrainingPath);
        LogUtils.d(TAG, "mBaseSoundModel = " + mBaseSoundModel +
                " mTargetSoundModelName = " + mTargetSoundModelName +
                " globalTrainingPath = " + globalTrainingPath);
        mCleanRecordings = mRecordingMgr.getCleanRecordings();
    }

    private void updateRecordingTimes() {
        if (isUdk()) {
            mRecordingTimes = SMLParametersManager.DEFAULT_CLEAN_RECORDING_TIMES;
        } else {
            ISettingsModel settingsModel = new SettingsModel(getApplicationContext(), null);
            mRecordingTimes = settingsModel.getPDKEnrollmentRecordingTimes();
        }
    }

    private void initializeUI() {
        mTvUserName = findViewById(R.id.tv_user_name);
        mTvUserName.setText(mUserName);

        mIbMic = findViewById(R.id.ib_mic);
        mIbMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtils.d(TAG, "initializeUI:onClick: mic image button is clicked");
                startTraining();
            }
        });
        TextView recordingTip2 = findViewById(R.id.recording_tip2);
        TextView trainingTips = findViewById(R.id.training_tips);
        if (mRecodingType == SMLParametersManager.RECORDING_IN_NOISY_ENVIRONMENT) {
            getActionBar().setTitle(R.string.extended_training);
            recordingTip2.setText(R.string.extended_training_recording_tip2);
            trainingTips.setText(R.string.extended_training_tips);
        }
    }

    private void updateUIVisible(boolean bRecording) {
        if (bRecording) {
            mIbMic.setClickable(false);
            showTrainingDialog();
        } else {
            mIbMic.setClickable(true);
            dismissTrainingDialog();
        }
    }

    private void showTrainingDialog() {
        if (mTrainingDialog == null) {
            inflateTrainingDialog();
        }
        mSoundWave.setVisibility(View.VISIBLE);
        mSayKeyword.setVisibility(View.VISIBLE);
        mDidNotHearClearly.setVisibility(View.GONE);
        mTrainSuccess.setVisibility(View.GONE);
        mTrainFinish.setVisibility(View.GONE);
        mTrainSuccessGenerate.setVisibility(View.GONE);
        mStepView.setText(String.format(getString(R.string.current_progress),
                mRecordingCounter.getCurrentProgress(), mRecordingTimes));
        mSayKeyword.setText(String.format(getString(R.string.say_keyword), getKeyphrase()));
        if (!isFinishing() && !mTrainingDialog.isShowing()) {
            mTrainingDialog.show();
        }
    }

    private void inflateTrainingDialog() {
        View view = View.inflate(this, R.layout.train_model_dialog, null);
        mStepView = view.findViewById(R.id.current_step);
        mSoundWave = view.findViewById(R.id.train_sound_wave);
        mSayKeyword = view.findViewById(R.id.say_keyword);
        mDidNotHearClearly = view.findViewById(R.id.didnt_hear_clearly);
        mTrainSuccess = view.findViewById(R.id.train_success);
        mTrainSuccessGenerate = view.findViewById(R.id.train_success_generate);
        mTrainFinish = view.findViewById(R.id.train_finish);
        mOk = view.findViewById(R.id.ok);
        mOk.setOnClickListener(v -> finish());
        ImageView trainCancel = view.findViewById(R.id.train_cancel);
        trainCancel.setOnClickListener(v -> {
            showTerminateTrainingDialog();
        });
        Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        mTrainingDialog = builder.create();
        mTrainingDialog.setView(view);
    }

    private void dismissTrainingDialog() {
        LogUtils.e(TAG, "dismissTrainingDialog ");
        if (mTrainingDialog != null && mTrainingDialog.isShowing()) {
            mTrainingDialog.dismiss();
            LogUtils.e(TAG, "dismissTrainingDialog execute");
        }
    }

    private void showTrainingSuccessPrompt() {
        mSoundWave.setVisibility(View.GONE);
        mTrainSuccess.setVisibility(View.GONE);
        mTrainSuccessGenerate.setVisibility(View.GONE);
        mDidNotHearClearly.setVisibility(View.GONE);
        mSayKeyword.setVisibility(View.INVISIBLE);
        mTrainFinish.setVisibility(View.VISIBLE);
        mOk.setVisibility(View.VISIBLE);
    }

    private void stopTraining(){
        if (mIsTraining) {
            // reset the training
            mIsTraining = false;
            releaseEPD();
            reset();
            updateUIVisible(false);
            mRecordingMgr.stopTrainingRecording(
                    mRecordingMgr.getLastUserRecordingFilePath());
            stopRecordingTimer();
        }
    }

    private void startTraining() {
        LogUtils.d(TAG, "startTraining: enter");
        mIsTraining = true;
        reset();
        initEPD();
        startRecording();
    }

    private boolean epdEnabled() {
        return Global.isSvaEpdEnabled(this);
    }

    private void initEPD() {
        if (!epdEnabled()) return;
        ByteBuffer soundModel = null;
        if (!isUdk()) {
            soundModel = mExtendedSmMgr.convertModelToBuffer(mPreviousSmName);
        }
        Bundle params = SMLParametersManager.getInstance().getOnlineEDPParams();
        mEPD = new EPD(getApplicationContext(), soundModel, params);
        mEPD.init();
    }

    private void reinitEPD() {
        if (!epdEnabled()) {
            releaseEPD();
            return;
        }
        if (mEPD != null) mEPD.reinit();
    }

    private void releaseEPD() {
        if (mEPD != null) mEPD.release();
        mEPD = null;
    }

    private void startRecording() {
        LogUtils.d(TAG, "startRecording: enter");
        reinitEPD();
        updateUIVisible(true);
        mRecordingMgr.startTrainingRecording(mEPD);
        mRecordingMgr.setAudioDataUpdatedListener(new RecordingsMgr.IAudioDataUpdatedListener() {
            @Override
            public void onAudioCapture(int volume) {
                mSoundWave.setData(volume);
            }

            @Override
            public void onEPDSuccess(EPD.ProcessResult result) {
                LogUtils.d("EPD onEPDSuccess result = " + result);
                stopRecordingTimer();
                if (!mIsEPDSuccess) {
                    mHandler.removeMessages(MSG_STOP_TRAINING_RECORDING);
                    mHandler.sendEmptyMessage(MSG_STOP_TRAINING_RECORDING);
                }
                mIsEPDSuccess = true;
            }
        });
        startRecordingTimer();
    }

    private void reset() {
        mRecordingMgr.removeAllTrainingRecordingFiles();
        mRecordingMgr.removeUserRecordings();
        mRecordingCounter = new RecordingCounter();
        mHandler.removeMessages(MSG_START_TRAINING_RECORDING);
        mHandler.removeMessages(MSG_STOP_TRAINING_RECORDING);
        mRecordingMgr.setAudioDataUpdatedListener(null);
    }

    private void startRecordingTimer() {
        LogUtils.d(TAG, "startRecordingTimer: enter");
        stopRecordingTimer();
        mIsEPDSuccess = false;
        mRecordingTimer = new Timer();
        mRecordingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!mIsEPDSuccess) {
                    mHandler.sendEmptyMessage(MSG_STOP_TRAINING_RECORDING);
                } else {
                    LogUtils.d(TAG, "stop send duplicated message EPD Success = " + mIsEPDSuccess);
                }
            }
        }, DURATION_OF_TRAINING_RECORDING);
    }

    private void stopRecordingTimer() {
        LogUtils.d(TAG, "stopRecordingTimer: enter");
        if (null != mRecordingTimer) {
            mRecordingTimer.cancel();
            mRecordingTimer = null;
        }
    }

    private ByteBuffer readPreviousSmData() {
        String filePath = Global.PATH_ROOT + "/" + mBaseSoundModel;
        if (FileUtils.isExist(filePath)) {
            try {
                ByteBuffer smData = FileUtils.readFileToByteBuffer(filePath);
                return smData;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            LogUtils.d(TAG, "readPreviousSmData: file " + filePath + " not exist");
            return null;
        }
    }

    private String getUserName() {
        return mUserName;
    }

    private boolean isUdk() {
        if (mIsAddUserToPreviousSm) {      //needs to check what this means
            return false;
        }
        return getString(R.string.create_your_own_3_0).equals(mPreviousSmName) 
                || getString(R.string.create_your_own_4_0).equals(mPreviousSmName)
                || getString(R.string.create_your_own_7_0).equals(mPreviousSmName);
    }

    private String getKeyphrase() {
        String keyphrase;
        boolean bUdk = isUdk();
        if (!bUdk) {
            if (mIsAddUserToPreviousSm) {
                keyphrase = mKeyphraseOrNewSmName;
            } else {
                // get keyphrase by previous sm name
                IExtendedSmModel extendedSmModel = mExtendedSmMgr.getSoundModel(mPreviousSmName);
                keyphrase = extendedSmModel.getSoundModelPrettyKeyphrase();
            }
        } else {
            keyphrase = mKeyphraseOrNewSmName;
        }

        LogUtils.d(TAG, "getKeyphrase: keyphrase = " + keyphrase);
        return keyphrase;
    }

    private String getTargetSoundModelName() {
        String soundModelName;
        if (isUdk()) {
            soundModelName = getKeyphrase() + ISmModel.SUFFIX_TRAINED_SOUND_MODEL;
            String filePath = Global.PATH_ROOT + "/" + soundModelName;
            if (FileUtils.isExist(filePath)) {
                long timestamp = System.currentTimeMillis();
                soundModelName = getKeyphrase() + "_" + timestamp
                        + ISmModel.SUFFIX_TRAINED_SOUND_MODEL;
            }
        } else {
            soundModelName = mIsAddUserToPreviousSm
                    || mRecodingType == SMLParametersManager.RECORDING_IN_NOISY_ENVIRONMENT
                    ? mPreviousSmName : mKeyphraseOrNewSmName;
        }
        return soundModelName;
    }

    private void showTerminateTrainingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(TrainingActivity.this);
        builder.setTitle(R.string.friendly_tips)
                .setMessage(R.string.terminate_training)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogUtils.d(TAG, "showTerminateTrainingDialog:onClick: enter");
                        mIsTraining = false;
                        releaseEPD();
                        reset();
                        updateUIVisible(false);
                        mRecordingMgr.stopTrainingRecording(
                                mRecordingMgr.getLastUserRecordingFilePath());
                        stopRecordingTimer();
                        mTerminateTrainingDialog.dismiss();
                        mTerminateTrainingDialog = null;
                    }
                });

        if (!(TrainingActivity.this).isFinishing()) {
            mTerminateTrainingDialog = builder.show();
        }
    }

    private class VerifyRecordingTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... strings) {
            LogUtils.d(TAG, "VerifyRecordingTask: doInBackground enter");
            int result;
            ShortBuffer lastRecordingData;
            try {
                lastRecordingData = mRecordingMgr.readWavFile(
                        mRecordingMgr.getLastUserRecordingFilePath());
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }

            if (isUdk()) {
                int snr = ListenSoundModel.verifyUdkRecording(
                        mExtendedSmMgr.getLanguageModel(getApplicationContext(), mUdkVersion),
                                    lastRecordingData);
                if (snr >= SNR_THRESHOLD) {
                    result = 0;
                } else if(snr >= 0){
                    result = ListenTypes.STATUS_ETOO_MUCH_NOISE_IN_RECORDING;
                }else {
                    result = snr;
                }
            } else {
                int confidenceLevel;
                if (Utils.isSupportImprovedTraining()) {
                    QualityCheckInfo qualityCheckInfo = new QualityCheckInfo();
                    int verifyStatus = ListenSoundModel.verifyUserRecordingQuality(
                            readPreviousSmData(), getKeyphrase(), lastRecordingData,
                            mRecodingType == SMLParametersManager.RECORDING_IN_NOISY_ENVIRONMENT,
                            qualityCheckInfo);
                    if (verifyStatus == 0) {
                        confidenceLevel = qualityCheckInfo.keywordConfidenceLevel;
                    } else {
                        confidenceLevel = verifyStatus;
                    }
                } else {
                    confidenceLevel = ListenSoundModel.verifyUserRecording(
                            readPreviousSmData(), getKeyphrase(), lastRecordingData);
                }
                ISettingsModel settingsModel = new SettingsModel(getApplicationContext(),
                        null);
                if (confidenceLevel >= settingsModel.getGlobalGMMTrainingConfidenceLevel()) {
                    result = 0;
                } else if(confidenceLevel >= 0){
                    result = ListenTypes.STATUS_ETOO_MUCH_NOISE_IN_RECORDING;;
                }else {
                    result = confidenceLevel;
                }
            }
            LogUtils.d(TAG, "VerifyRecordingTask: result = " + result);
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            boolean bSuccess = (result.intValue() == 0);
            if (mIsTraining) {
                mSoundWave.setVisibility(View.GONE);
                mRecordingCounter.updateRecordingResult(bSuccess);
                if (bSuccess) {
                    mTrainSuccess.setVisibility(View.VISIBLE);
                    mSayKeyword.setVisibility(View.VISIBLE);
                    // keep recording data to mem
                    mRecordingMgr.addUserRecording();
                } else {
                    mDidNotHearClearly.setVisibility(View.VISIBLE);
                    mDidNotHearClearly.setText(Utils.getListenErrorMsg(TrainingActivity.this,
                            result.intValue()) + getString(R.string.try_again));
                    mSayKeyword.setVisibility(View.INVISIBLE);
                }

                if (mRecordingCounter.isFinished()) {
                    mIsTraining = false;
                    releaseEPD();
                    mTrainSuccess.setVisibility(View.GONE);
                    mTrainSuccessGenerate.setVisibility(View.VISIBLE);
                    mSayKeyword.setText(getString(R.string.generating_model));
                    //create sound model
                    mCreateSmTask = new CreateSmTask();
                    mCreateSmTask.execute();
                } else {
                    mHandler.removeMessages(MSG_START_TRAINING_RECORDING);
                    mHandler.sendEmptyMessageDelayed(MSG_START_TRAINING_RECORDING,
                            START_RECORDING_DELAY);
                }
            }
        }
    }

    private class CreateSmTask extends AsyncTask<String, Void, Integer> {
        ListenTypes.ConfidenceData mConfidenceData;
        ByteBuffer mSmData;

        @Override
        protected Integer doInBackground(String... strings) {
            int smSize;
            if (isUdk()) {
                smSize = ListenSoundModel.getUdkSmSize(getKeyphrase(), getUserName(),
                        mRecordingMgr.getUserRecordings(),
                        mExtendedSmMgr.getLanguageModel(getApplicationContext(), mUdkVersion));
            } else {
                smSize = ListenSoundModel.getSizeWhenExtended(readPreviousSmData(),
                        getKeyphrase(), getUserName());
            }

            if (smSize <= 0) {
                return -1;
            }

            mSmData = ByteBuffer.allocate(smSize);
            mConfidenceData = new ListenTypes.ConfidenceData();
            int result;
            if (isUdk()) {
                result = ListenSoundModel.createUdkSm(getKeyphrase(), getUserName(),
                        mRecordingTimes,
                        mRecordingMgr.getUserRecordings(),
                        mExtendedSmMgr.getLanguageModel(getApplicationContext(), mUdkVersion),
                        mSmData, mConfidenceData);
            } else {
                if (mRecodingType == SMLParametersManager.RECORDING_IN_NOISY_ENVIRONMENT) {
                    ShortBuffer[] recordings = concat(mCleanRecordings,
                            mRecordingMgr.getUserRecordings());
                    result = ListenSoundModel.extend(readPreviousSmData(), getKeyphrase(),
                            getUserName(), recordings.length, recordings, mSmData, mConfidenceData);
                } else {
                    result = ListenSoundModel.extend(readPreviousSmData(), getKeyphrase(),
                            getUserName(), mRecordingTimes,
                            mRecordingMgr.getUserRecordings(), mSmData, mConfidenceData);
                }
            }

            LogUtils.d(TAG, "CreateSmTask:doInBackground: result = " + result
                    + " userMatch = " + mConfidenceData.userMatch);
            if (ListenTypes.STATUS_SUCCESS == result) {
                return 0;
            }

            return -1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            LogUtils.d(TAG, "CreateSmTask:onPostExecute: result = " + result);

            // remove user recording mem data
            mRecordingMgr.removeUserRecordings();

            if (result.intValue() == 0) {
                // save the sm data to new file
                String smFilePath = Global.PATH_ROOT + "/" + mTargetSoundModelName;
                boolean bSuccess = FileUtils.saveByteBufferToFile(
                        mSmData, smFilePath);
                if (bSuccess) {
                    // update sm mgr
                    String[] strings = smFilePath.split("/");
                    String createdSmName = strings[strings.length - 1];
                    LogUtils.d(TAG, "CreateSmTask:onPostExecute: createdSmName = "
                            + createdSmName);
                    mExtendedSmMgr.addSoundModel(getApplicationContext(), createdSmName);

                    if (!TextUtils.isEmpty(mBaseSoundModel)) {
                        ISettingsModel settingsModel = new SettingsModel(getApplicationContext(),
                                createdSmName);
                        settingsModel.setBaseSoundModel(mBaseSoundModel);
                    }
                    mExtendedSmMgr.getSoundModel(createdSmName).setSessionStatus(
                            IExtendedSmModel.SessionStatus.PENDINGSTARTED);
                    showTrainingSuccessPrompt();
                    return;
                }
            }
            // finish training activity, Regardless of success or failure
            Toast.makeText(getApplicationContext(), getString(R.string.create_sm_failure),
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private class RecordingCounter {
        private final String TAG = RecordingCounter.class.getSimpleName();
        private int mCounter = 0;

        public RecordingCounter() {
            mCounter = 0;
        }

        // Updates the UI if training was successful
        public void updateRecordingResult(boolean isGoodRecording) {
            if (isGoodRecording) {
                mCounter++;
            }
        }

        public boolean isFinished() {
            LogUtils.d(TAG, "isFinished: mCounter = " + mCounter);
            return mRecordingTimes <= mCounter;
        }

        public int getCurrentProgress() {
            return mCounter + 1;
        }
    }

    private static ShortBuffer[] concat(ShortBuffer[] a, ShortBuffer[] b) {
        ShortBuffer[] c= new ShortBuffer[a.length+b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;

    }
}
