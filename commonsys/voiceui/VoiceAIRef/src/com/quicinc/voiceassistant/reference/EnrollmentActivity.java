/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.quicinc.voiceassistant.reference.controller.SoundModelFilesManager;
import com.quicinc.voiceassistant.reference.data.Settings;
import com.quicinc.voiceassistant.reference.util.AppPermissionUtils;
import com.quicinc.voiceassistant.reference.util.LogUtils;
import com.quicinc.voiceassistant.reference.views.EnrollmentViewHolder;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.configuration.ConfigurationConnector;
import com.quicinc.voice.assist.sdk.enrollment.Enrollment;
import com.quicinc.voice.assist.sdk.enrollment.EnrollmentConnector;
import com.quicinc.voice.assist.sdk.enrollment.EnrollmentExtras;
import com.quicinc.voice.assist.sdk.enrollment.EnrollmentFailedReason;
import com.quicinc.voice.assist.sdk.enrollment.EnrollmentSuccessInfo;
import com.quicinc.voice.assist.sdk.enrollment.IUtteranceCallback;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;
import com.quicinc.voice.assist.sdk.utility.PermissionUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.quicinc.voiceassistant.reference.R;

public class EnrollmentActivity extends AppCompatActivity
        implements EnrollmentViewHolder.IEnrollmentListener {
    public static final int SUCCESS_RESULT_CODE = 0x1001;
    private static final String TAG = EnrollmentActivity.class.getSimpleName();
    private static final String ERROR_LOW_SNR = "low snr";
    private static final String ENROLLMENT_UTTERANCES = "Enrollment.utterances";
    public static final String REQUEST_TYPE = "request.type";
    public static final String TYPE_COMMON = "common";
    private static final long DELAY_TIME = 2000L;
    private final List<String> mUtteranceInfo = new ArrayList<>();
    private AlertDialog mProgressDialog;
    private AlertDialog mCreateSmFailDialog;
    SoundModelFilesManager mSmFilesMgr;
    Context mContext;
    private EnrollmentConnector mEnrollmentConnector;
    private boolean mEnrollmentServiceConnected;
    private ConfigurationConnector mConfigurationConnector;
    private boolean mSettingsServiceConnected;
    private int mCleanUtterances;
    private Handler mHandler;
    private static final int MSG_SERVICE_START_ENROLLMENT = 2;
    private EnrollmentViewHolder mEnrollmentViewHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_training);
        mContext = EnrollmentActivity.this;
        mSmFilesMgr = SoundModelFilesManager.getInstance(getApplicationContext());
        mEnrollmentConnector = new EnrollmentConnector(mContext);
        mEnrollmentConnector.connect(new AbstractConnector.ServiceConnectListener() {
            @Override
            public void onServiceConnected() {
                mEnrollmentServiceConnected = true;
            }

            @Override
            public void onServiceDisConnected(ComponentName name) {
                mEnrollmentServiceConnected = false;
            }
        });
        mConfigurationConnector = new ConfigurationConnector(this);
        mConfigurationConnector.connect(new AbstractConnector.ServiceConnectListener() {
            @Override
            public void onServiceConnected() {
                Bundle bundle = new Bundle();
                bundle.putString(REQUEST_TYPE, TYPE_COMMON);
                bundle.putBoolean(ENROLLMENT_UTTERANCES, true);
                Bundle params = mConfigurationConnector.getParams(bundle);
                mCleanUtterances = params.getInt(ENROLLMENT_UTTERANCES);
                mSettingsServiceConnected = true;
            }

            @Override
            public void onServiceDisConnected(ComponentName name) {
                mSettingsServiceConnected = false;
            }
        });
        ClientApplication.getInstance().addActivityInstance(this);
        initializeUI();
        mHandler = new EnrollmentHandler(getMainLooper());
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d(TAG, "onResume");
        if (!AppPermissionUtils.requestRuntimePermissions(EnrollmentActivity.this)) {
            mHandler.sendEmptyMessageDelayed(MSG_SERVICE_START_ENROLLMENT, 50);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LogUtils.d(TAG, "onPause");
        mHandler.removeCallbacksAndMessages(null);
        finishEnrollment();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ClientApplication.getInstance().removeActivityInstance(this);
        dismissProgressDialog();
        dismissFailDialog();
        mEnrollmentConnector.disconnect();
        mConfigurationConnector.disconnect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (AppPermissionUtils.QVA_REQUEST_CODE == requestCode) {
            if (!PermissionUtils.hasQVARecordAudioPermission(this)) {
                ClientApplication.getInstance().finishActivities();
            } else {
                AppPermissionUtils.requestRuntimePermissions(EnrollmentActivity.this);
            }
        } else if (AppPermissionUtils.REQUEST_CODE == requestCode) {
            if (!AppPermissionUtils.isRuntimePermissionsGranted(this)) {
                ClientApplication.getInstance().finishActivities();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void startEnrollWhenReady() {
        if (mEnrollmentServiceConnected & mSettingsServiceConnected) {
            startEnrollment();
        } else {
            mHandler.sendEmptyMessageDelayed(MSG_SERVICE_START_ENROLLMENT, 50);
        }
    }

    class EnrollmentHandler extends Handler {
        public EnrollmentHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SERVICE_START_ENROLLMENT) {
                startEnrollWhenReady();
            }
        }
    }

    private void initializeUI() {
        View enrollmentContainer = findViewById(R.id.enrollment_container);
        mEnrollmentViewHolder = new EnrollmentViewHolder(this, enrollmentContainer);
        mEnrollmentViewHolder.setEnrollmentListener(this);
        ImageView cancelEnroll = findViewById(R.id.enrollment_back);
        cancelEnroll.setOnClickListener(view -> {
            if (mEnrollmentConnector != null) {
                finishEnrollment();
                finish();
            }
        });
    }

    private void finishEnrollment() {
        mEnrollmentConnector.finishUserVoiceEnrollment();
    }

    private void startEnrollment() {
        Enrollment enrollment;

        enrollment = new Enrollment(Enrollment.EnrollmentType.TI, null, "0",
                null, Enrollment.ENROLLMENT_RECORDING_TYPE_CLEAN);
        enrollment.setCleanUtterances(mCleanUtterances);
        WeakReference<IOperationCallback<ArrayList<String>, EnrollmentFailedReason>> callbackRef =
                new WeakReference(
                        new IOperationCallback<ArrayList<String>, EnrollmentFailedReason>() {
                    @Override
                    public void onSuccess(ArrayList<String> list) {
                        LogUtils.d(TAG, "startUserVoiceEnrollment onSuccess");
                        mUtteranceInfo.clear();
                        mUtteranceInfo.addAll(list);
                        mEnrollmentViewHolder.resetEnrollmentState();
                    }

                    @Override
                    public void onFailure(EnrollmentFailedReason reason) {
                        showEnrollmentErrorPrompt();
                        LogUtils.e(TAG, "startUserVoiceEnrollment onFailure " + reason);
                    }
                });
        mEnrollmentConnector.startUserVoiceEnrollment(enrollment, callbackRef);
    }

    private void startUtteranceTraining() {
        WeakReference<IUtteranceCallback> callbackRef = new WeakReference<>(
                new IUtteranceCallback() {
                    @Override
                    public void onStartRecording() {
                        LogUtils.d(TAG, "startUtteranceTraining onStartRecording");
                        mEnrollmentViewHolder.updateUtteranceState(true);
                    }

                    @Override
                    public void onStopRecording() {
                        LogUtils.d(TAG, "startUtteranceTraining onStopRecording");
                        mEnrollmentViewHolder.updateUtteranceState(false);
                    }

                    @Override
                    public void onSuccess(String s, EnrollmentExtras enrollmentExtras) {
                        LogUtils.d(TAG, "startUtteranceTraining onSuccess s = " + s);
                        mEnrollmentViewHolder.upToNextStep();
                    }

                    @Override
                    public void onFailure(String s, EnrollmentExtras enrollmentExtras, String s1) {
                        if (!TextUtils.isEmpty(s1) && ERROR_LOW_SNR.equals(s1)) {
                            Toast.makeText(mContext, "too much noisy in recording",
                                    Toast.LENGTH_SHORT).show();
                        }
                        LogUtils.d(TAG, "startUtteranceTraining onFailure, error : " + s1);
                        mEnrollmentViewHolder.enrollFail();
                        mHandler.postDelayed(EnrollmentActivity.this::startUtteranceTraining, DELAY_TIME);
                    }

                    @Override
                    public void onFeedback(int volume) {
                        LogUtils.d(TAG, "startUtteranceTraining onFeedback volume = " + volume);
                        mEnrollmentViewHolder.updateVoiceEffect(volume);
                    }
                });
        mEnrollmentConnector.startUtteranceTraining(
                mUtteranceInfo.get(mEnrollmentViewHolder.getCurrentStep()), callbackRef);
    }

    @Override
    public void onStartUtteranceTraining() {
        startUtteranceTraining();
    }

    @Override
    public void onGenerateSoundModel() {
        showProgressDialog();
        WeakReference<IOperationCallback<EnrollmentSuccessInfo,
                EnrollmentFailedReason>> callbackRef = new WeakReference<>(
                new IOperationCallback<EnrollmentSuccessInfo, EnrollmentFailedReason>() {
                    @Override
                    public void onSuccess(EnrollmentSuccessInfo enrollmentSuccessInfo) {
                        LogUtils.d(TAG, "commitUserVoiceEnrollment onSuccess");
                        mEnrollmentViewHolder.onEnrollCompleted();
                        dismissProgressDialog();
                    }

                    @Override
                    public void onFailure(EnrollmentFailedReason reason) {
                        LogUtils.e(TAG, "commitUserVoiceEnrollment onFailure " + reason);
                        dismissProgressDialog();
                        showCreateSmFailDialog();
                    }
                }
        );
        mEnrollmentConnector.commitUserVoiceEnrollment(callbackRef);
    }

    @Override
    public void onGenerateFinish() {
        Intent intent = new Intent();
        setResult(SUCCESS_RESULT_CODE, intent);
        Settings.setUVEnabled(EnrollmentActivity.this, true);
        finish();
    }

    private void showEnrollmentErrorPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(getString(R.string.enrollment_error_message));
        builder.setTitle(R.string.enrollment_error_title);
        builder.setNegativeButton(getString(android.R.string.cancel),
                (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.create().show();
    }

    private void showProgressDialog() {
        View container = View.inflate(mContext, R.layout.progress_dialog_layout, null);
        TextView userTips = container.findViewById(R.id.user_tips);
        userTips.setText(R.string.create_sound_model_message);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        mProgressDialog = builder.create();
        mProgressDialog.setView(container);
        if (!isFinishing()) {
            mProgressDialog.show();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showCreateSmFailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.friendly_tips)
                .setMessage(R.string.create_sm_failure)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> mCreateSmFailDialog.dismiss());

        if (!isFinishing()) {
            mCreateSmFailDialog = builder.show();
        }
    }

    private void dismissFailDialog() {
        if (mCreateSmFailDialog != null && mCreateSmFailDialog.isShowing()) {
            mCreateSmFailDialog.dismiss();
        }
    }
}
