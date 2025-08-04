/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.quicinc.voice.assist.sdk.multimodal.MultiModalRecognitionConnector;
import com.quicinc.voiceassistant.reference.controller.SoundModelFilesManager;
import com.quicinc.voiceassistant.reference.data.LLMRepository;
import com.quicinc.voiceassistant.reference.data.Settings;
import com.quicinc.voiceassistant.reference.util.AppPermissionUtils;
import com.quicinc.voiceassistant.reference.util.LogUtils;
import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.assist.sdk.configuration.ConfigurationConnector;
import com.quicinc.voice.assist.sdk.enrollment.EnrollmentConnector;
import com.quicinc.voice.assist.sdk.inputprovider.InputProviderConnector;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;
import com.quicinc.voice.assist.sdk.utility.PermissionUtils;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationConnector;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationFailedReason;
import com.quicinc.voice.assist.sdk.enrollment.Enrollment;
import com.quicinc.voiceassistant.reference.R;

import java.lang.ref.WeakReference;

public class SettingsActivity extends ActivationActivity {
    private Switch mWakeToggle;
    private final Handler mHandler = new Handler();
    private boolean mResumed;
    private EnrollmentConnector mEnrollmentConnector;
    private InputProviderConnector mInputProviderConnector;
    private boolean mReceiverRegistered;
    private ConfigurationConnector mConfigurationConnector;
    private final String[] mASRSupportedLanguage = {"English", "Chinese"};
    private ImageView mEnableUVBackground;
    private TextView mEnableUVText;
    private Switch mEnableUV;
    private TextView mUVEnrollText;
    private ImageView mReEnrollBackground;
    private TextView mReEnrollText;
    private ImageView mReEnrollArrow;
    private ImageView mDeleteEnrollBackground;
    private TextView mDeleteEnrollText;
    private ImageView mLanguageSelectBackground;
    private TextView mCurrentLanguage;
    private Switch mTranslationEnabledSwitch;
    private Switch mLiftAndTalkSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        registerSoundModelsChangeCallback();
        connectToQVA();

        ClientApplication.getInstance().addActivityInstance(this);
        initializeUI();
    }

    private void connectToQVA() {
        mInputProviderConnector = new InputProviderConnector(mContext.getApplicationContext());
        mInputProviderConnector.connect(() -> {
            ComponentName componentName = LLMRepository.getCurrentLLMIntent();
            Bundle bundle = new Bundle();
            bundle.putString("LLMClient.packageName", componentName.getPackageName());
            bundle.putString("LLMClient.className", componentName.getClassName());
            mInputProviderConnector.registerClient(bundle);
            changeTranslationEnabledState(Settings.getASRTranslationEnabled(this));
        });
        mVoiceActivationConnector = new VoiceActivationConnector(mContext);
        mVoiceActivationConnector.connect(new AbstractConnector.ServiceConnectListener() {
            @Override
            public void onServiceConnected() {
                isVoiceActivationServiceConnected = true;
                updateWakeToggleStatus();
            }

            @Override
            public void onServiceDisConnected(ComponentName name) {
                isVoiceActivationServiceConnected = false;
            }
        });
        mEnrollmentConnector = new EnrollmentConnector(mContext);
        mEnrollmentConnector.connect(new AbstractConnector.ServiceConnectListener() {
            @Override
            public void onServiceConnected() {
                updateDeleteItem();
            }

            @Override
            public void onServiceDisConnected(ComponentName name) {
            }
        });
        mMultiModalRecognitionConnector = new MultiModalRecognitionConnector(mContext);
        mMultiModalRecognitionConnector.connect(new AbstractConnector.ServiceConnectListener() {
            @Override
            public void onServiceConnected() {
                isMultiModalRecognitionConnected = true;
                updateLiftAndTalkToggleStatus();
            }

            @Override
            public void onServiceDisConnected(ComponentName name) {
                isMultiModalRecognitionConnected = false;
            }
        });
        AppPermissionUtils.requestRuntimePermissions(SettingsActivity.this);
        if (PermissionUtils.hasQVARecordAudioPermission(this)
                && mSmFilesManager.getAccessibleSoundModels().size() < 1) {
            SoundModelFilesManager.getInstance(getApplicationContext()).initialize();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mResumed = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mResumed = true;
        updateWakeToggleStatus();
        updateLiftAndTalkToggleStatus();
        popupIfNeeded();
    }

    private void updateDeleteItem() {
        boolean isGeneralUVEnrolled = mEnrollmentConnector.isGeneralUVEnrolled();
        if (mEnableUV.isShown()) {
            mEnableUV.setChecked(isGeneralUVEnrolled && Settings.getUVEnabled(this));
        }
        if (isGeneralUVEnrolled) {
            showEnableUVView(View.VISIBLE);
            showUVEnrollView(View.VISIBLE);
        } else {
            if (mWakeToggle.isChecked()) {
                showEnableUVView(View.VISIBLE);
            } else {
                showEnableUVView(View.GONE);
            }
            showUVEnrollView(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ClientApplication.getInstance().removeActivityInstance(this);
        unregisterSoundModelsChangeCallback();
        mHandler.removeCallbacks(mQueryRecognitionStatusTask);
        mHandler.removeCallbacks(mQueryLiftAndTalkStatusTask);
        mMultiModalRecognitionConnector.disconnect();
        mVoiceActivationConnector.disconnect();
        mEnrollmentConnector.disconnect();
        mInputProviderConnector.disconnect();
        if (mConfigurationConnector != null) {
            mConfigurationConnector.disconnect();
        }
        if (mReceiverRegistered) {
            unregisterReceiver(mBroadcastReceiver);
        }
        mMultiModalRecognitionConnector = null;
        mSmFilesManager = null;
        mEnrollmentConnector = null;
        mInputProviderConnector = null;
        mVoiceActivationConnector = null;
        mConfigurationConnector = null;
        mContext = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (AppPermissionUtils.QVA_REQUEST_CODE == requestCode) {
            if (PermissionUtils.hasQVARecordAudioPermission(this)) {
                AppPermissionUtils.requestRuntimePermissions(SettingsActivity.this);
                if (mSmFilesManager.getAccessibleSoundModels().size() < 1) {
                    SoundModelFilesManager.getInstance(getApplicationContext()).initialize();
                }
            }
        }
    }

    @Override
    protected void notifyDataSetChanged() {
        updateWakeToggleStatus();
        updateLiftAndTalkToggleStatus();
    }

    private void initializeUI() {
        ImageView back = findViewById(R.id.settings_toolbar_button);
        back.setOnClickListener(view -> finish());
        mWakeToggle = findViewById(R.id.wake_toggle);
        mWakeToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> toggleDetection(isChecked));

        mEnableUVText = findViewById(R.id.uv_toggle_text);
        mEnableUVBackground = findViewById(R.id.uv_toggle_text_background);
        mEnableUV = findViewById(R.id.uv_toggle);

        mEnableUV.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (mEnrollmentConnector.isGeneralUVEnrolled()) {
                disableUV(!isChecked);
                Settings.setUVEnabled(SettingsActivity.this, isChecked);
            } else {
                if (isChecked) {
                    checkEnableThenStartEnrollment();
                }
            }
        });

        mUVEnrollText = findViewById(R.id.user_voice_enrollment);
        mReEnrollText = findViewById(R.id.re_enroll_text);
        mReEnrollBackground = findViewById(R.id.re_enroll_background);
        mReEnrollArrow = findViewById(R.id.re_enroll_arrow);
        mReEnrollBackground.setOnClickListener(view -> checkEnableThenStartEnrollment());

        mDeleteEnrollBackground = findViewById(R.id.delete_enroll_background);
        mDeleteEnrollText = findViewById(R.id.delete_enroll_text);
        mDeleteEnrollBackground.setOnClickListener(
                view -> mEnrollmentConnector.removeGeneralUV(
                        new IOperationCallback<String, String>() {
                            @Override
                            public void onSuccess(String s) {
                                LogUtils.d(TAG, "removeGeneralUV onSuccess.");
                                disableUV(true);
                                updateDeleteItem();
                                boolean recognitionEnabled = mSmFilesManager.isRecognitionEnabled(
                                        mVoiceActivationConnector);
                                if (recognitionEnabled != mWakeToggle.isChecked()) {
                                    mWakeToggle.setChecked(recognitionEnabled);
                                }
                            }

                            @Override
                            public void onFailure(String s) {
                                LogUtils.e(TAG, "removeGeneralUV onFailure.");
                            }
                        }));

        mCurrentLanguage = findViewById(R.id.current_language);
        String asrLanguage = Settings.getASRLanguage(this);
        if (TextUtils.isEmpty(asrLanguage)) {
            asrLanguage = mASRSupportedLanguage[0];
        }
        mCurrentLanguage.setText(asrLanguage);
        mLanguageSelectBackground = findViewById(R.id.voice_language_background);
        mLanguageSelectBackground.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, LanguageSelectActivity.class);
            startActivity(intent);
        });

        mTranslationEnabledSwitch = findViewById(R.id.translation_enabled);
        mTranslationEnabledSwitch.setChecked(Settings.getASRTranslationEnabled(this));
        mTranslationEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "mTranslationEnabledSwitch onCheckedChanged " + isChecked);
                Settings.setASRTranslationEnabled(SettingsActivity.this, isChecked);
                changeTranslationEnabledState(isChecked);
            }
        });
        mLiftAndTalkSwitch = findViewById(R.id.lift_and_talk_switch);
        mLiftAndTalkSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> toggleLiftAndTalkDetection(isChecked));
    }

    private void disableUV(boolean disableUV) {
        if (mConfigurationConnector == null) {
            mConfigurationConnector = new ConfigurationConnector(this);
            mConfigurationConnector.connect(new AbstractConnector.ServiceConnectListener() {
                @Override
                public void onServiceConnected() {
                    boolean isGeneralUVEnrolled = mEnrollmentConnector.isGeneralUVEnrolled();
                    if (!isGeneralUVEnrolled) {
                        return;
                    }
                    setParams(disableUV);
                }

                @Override
                public void onServiceDisConnected(ComponentName name) {
                    mConfigurationConnector = null;
                }
            });
        } else {
            boolean isGeneralUVEnrolled = mEnrollmentConnector.isGeneralUVEnrolled();
            if (!isGeneralUVEnrolled) {
                return;
            }
            setParams(disableUV);
        }
    }

    private void setParams(boolean disableUV) {
        Bundle bundle = new Bundle();
        bundle.putString("request.type", "HeySnapdragon_en-US_041.uim");
        bundle.putString("request.locale", "en-US");
        bundle.putBoolean("UV.disable", disableUV);
        if (mConfigurationConnector != null) {
            boolean setParams = mConfigurationConnector.setParams(bundle);
        }
    }

    private void changeTranslationEnabledState(boolean enabled) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("request.translation", enabled);
        if(mInputProviderConnector != null) {
            mInputProviderConnector.setParams(bundle, null);
        }
    }

    private final Runnable mQueryRecognitionStatusTask = new Runnable() {
        @Override
        public void run() {
            LogUtils.d(TAG, "updateWakeToggleStatus");
            setASRParams();
            boolean recognitionEnabled
                    = mSmFilesManager.isRecognitionEnabled(mVoiceActivationConnector);
            mWakeToggle.setChecked(recognitionEnabled);
            updateDeleteItem();
        }
    };

    private final Runnable mQueryLiftAndTalkStatusTask = new Runnable() {
        @Override
        public void run() {
            LogUtils.d(TAG, "updateLiftAndTalkToggleStatus");
            setASRParams();
            boolean recognitionEnabled
                    = mSmFilesManager.isMultiModalRecognitionEnabled(
                            mMultiModalRecognitionConnector);
            mLiftAndTalkSwitch.setChecked(recognitionEnabled);
            updateDeleteItem();
        }
    };

    private void updateWakeToggleStatus() {
        enableAllClickableView(false);
        mHandler.removeCallbacks(mQueryRecognitionStatusTask);
        if (!mResumed) {
            LogUtils.d(TAG, "ignore, only update UI when onResumed");
            return;
        }
        if (isVoiceActivationServiceConnected) {
            mQueryRecognitionStatusTask.run();
        } else {
            mHandler.postDelayed(mQueryRecognitionStatusTask,
                    SoundModelFilesManager.DELAY_WAITING_CONNECTED);
        }
    }

    private void updateLiftAndTalkToggleStatus() {
        enableAllClickableView(false);
        mHandler.removeCallbacks(mQueryRecognitionStatusTask);
        if (!mResumed) {
            LogUtils.d(TAG, "ignore , only update Lift and Talk UI when onResumed");
            return;
        }
        if (isVoiceActivationServiceConnected) {
            mQueryLiftAndTalkStatusTask.run();
        } else {
            mHandler.postDelayed(mQueryLiftAndTalkStatusTask,
                    SoundModelFilesManager.DELAY_WAITING_CONNECTED);
        }
    }

    private void toggleDetection(boolean isChecked) {
        mWakeToggle.setClickable(false);
        mEnableUV.setClickable(false);
        mReEnrollBackground.setClickable(false);
        LogUtils.d(TAG, "toggle recognition isChecked = " + isChecked);
        WeakReference<IOperationCallback<Bundle, VoiceActivationFailedReason>> callbackRef =
                new WeakReference<>(new IOperationCallback<Bundle, VoiceActivationFailedReason>() {
            @Override
            public void onSuccess(Bundle bundle) {
                LogUtils.d(TAG, "toggleRecognition(check = " + isChecked + ")" + " onSuccess ");
                updateWakeToggleStatus();
                boolean uvEnabled = Settings.getUVEnabled(SettingsActivity.this);
                disableUV(!uvEnabled);
                mWakeToggle.setClickable(true);
                mEnableUV.setClickable(true);
                mReEnrollBackground.setClickable(true);
            }

            @Override
            public void onFailure(VoiceActivationFailedReason reason) {
                LogUtils.e(TAG, "toggleRecognition(check = " + isChecked + ")"
                        + " onFailure " + reason);
                Toast.makeText(mContext, "The model is not supported.", Toast.LENGTH_SHORT).show();
                updateWakeToggleStatus();
                mWakeToggle.setClickable(true);
                mEnableUV.setClickable(true);
                mReEnrollBackground.setClickable(true);
            }
        });
        toggleRecognition(isChecked, callbackRef);
    }

    private void toggleLiftAndTalkDetection(boolean isChecked) {
        mLiftAndTalkSwitch.setClickable(false);
        LogUtils.d(TAG, "toggle LiftAndTalk recognition isChecked = " + isChecked);
        WeakReference<IOperationCallback<Bundle, VoiceActivationFailedReason>> callbackRef =
                new WeakReference<>(new IOperationCallback<Bundle, VoiceActivationFailedReason>() {
            @Override
            public void onSuccess(Bundle bundle) {
                LogUtils.d(TAG, "toggleLiftAndTalkRecognition(check = " + isChecked + ")"
                        + " onSuccess ");
                updateLiftAndTalkToggleStatus();
                mLiftAndTalkSwitch.setClickable(true);
            }

            @Override
            public void onFailure(VoiceActivationFailedReason reason) {
                LogUtils.e(TAG, "toggleLiftAndTalkRecognition(check = " + isChecked + ")"
                        + " onFailure " + reason);
                Toast.makeText(mContext, "The model is not supported.", Toast.LENGTH_SHORT).show();
                updateLiftAndTalkToggleStatus();
                mLiftAndTalkSwitch.setClickable(true);
            }
        });
        toggleMultiModalRecognition(isChecked, callbackRef);
    }
    private void enableAllClickableView(boolean enable) {
        mWakeToggle.setClickable(enable);
        mEnableUV.setClickable(enable);
        mReEnrollBackground.setClickable(enable);
        mDeleteEnrollBackground.setClickable(enable);
        mLanguageSelectBackground.setClickable(enable);
        mTranslationEnabledSwitch.setClickable(enable);
        mLiftAndTalkSwitch.setClickable(enable);
    }

    private void showEnableUVView(int visible) {
        mEnableUVBackground.setVisibility(visible);
        mEnableUVText.setVisibility(visible);
        mEnableUV.setVisibility(visible);
    }

    private void showUVEnrollView(int visible) {
        mUVEnrollText.setVisibility(visible);
        mReEnrollBackground.setVisibility(visible);
        mReEnrollText.setVisibility(visible);
        mReEnrollArrow.setVisibility(visible);
        mDeleteEnrollBackground.setVisibility(visible);
        mDeleteEnrollText.setVisibility(visible);
    }

    private void checkEnableThenStartEnrollment() {
        boolean recognitionEnabled = SoundModelFilesManager.getInstance(
                getApplicationContext()).isRecognitionEnabled(mVoiceActivationConnector);
        if (recognitionEnabled || mWakeToggle.isChecked()) {
            showTurnOffDetectionConfirmDialog(getString(
                    R.string.turn_off_detection_before_enrollment_message));
        } else {
            startEnrollmentActivity();
        }
    }

    private void showTurnOffDetectionConfirmDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(message);
        builder.setTitle(R.string.friendly_tips);
        builder.setNegativeButton(R.string.cancel,
                (dialogInterface, i) -> {
                    boolean isGeneralUVEnrolled = mEnrollmentConnector.isGeneralUVEnrolled();
                    if (!isGeneralUVEnrolled) {
                        mEnableUV.setChecked(false);
                    }
                });
        builder.setPositiveButton(getString(R.string.confirm), (dialogInterface, i) ->
                disableDetectionThenStartEnrollment());
        builder.create().show();
    }

    private void disableDetectionThenStartEnrollment() {
        SoundModelFilesManager.getInstance(mContext).disableRecognition(
                new WeakReference<>(
                        new IOperationCallback<Bundle, VoiceActivationFailedReason>() {
                            @Override
                            public void onSuccess(Bundle bundle) {
                                LogUtils.d(TAG, "delete detection before enroll onSuccess");
                                mWakeToggle.setChecked(false);
                                startEnrollmentActivity();
                            }

                            @Override
                            public void onFailure(VoiceActivationFailedReason reason) {
                                LogUtils.d(TAG, "delete detection before enroll failure reason = "
                                        + reason);
                                Toast.makeText(mContext, getString(
                                        R.string.voice_adding_failure_message),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }));
    }

    private void startEnrollmentActivity() {
        Intent intent = new Intent(SettingsActivity.this, EnrollmentActivity.class);
        startActivity(intent);
    }

    private boolean isQVAEnabled() {
        boolean state = false;
        try {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo("com.quicinc.voice.activation",
                            PackageManager.MATCH_DISABLED_COMPONENTS);
            state = packageInfo.applicationInfo.enabled;
        } catch (Exception e) {
            LogUtils.e("Stub", "error", e);
        }
        return state;
    }

    private void goToGooglePlay() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id=com.quicinc.voice.activation"));
        intent.setPackage("com.android.vending");
        startActivity(intent);
    }

    private void popupIfNeeded() {
        if (!isQVAEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enable QVA")
                    .setMessage("QVA is disabled, please go to Google Play and enable QVA first.\n")
                    .setCancelable(true)
                    .setPositiveButton("OK", (dialog, id) -> {
                        reconnectWhenQVAisUpdated();
                        goToGooglePlay();
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> {
                        // User cancelled the dialog
                        dialog.cancel();
                    });
            builder.create().show();
        }
    }

    private void reconnectWhenQVAisUpdated() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mBroadcastReceiver, intentFilter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(mBroadcastReceiver, intentFilter);
        }
        mReceiverRegistered = true;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            final String action = intent.getAction();
            if (action == null) return;
            LogUtils.d("action=" + action);
            final Uri uri = intent.getData();
            if (uri == null) return;
            final String packageName = uri.getSchemeSpecificPart();
            if ("com.quicinc.voice.activation".equals(packageName)) {
                if (isQVAEnabled()) {
                    connectToQVA();
                }
            }
        }
    };

    private void setASRParams() {
        enableAllClickableView(true);
        String asrLanguage = Settings.getASRLanguage(SettingsActivity.this);
        if (TextUtils.isEmpty(asrLanguage)) {
            asrLanguage = mASRSupportedLanguage[0];
        }
        mCurrentLanguage.setText(asrLanguage);
        Bundle bundle = new Bundle();
        bundle.putString("request.language", asrLanguage);
        mInputProviderConnector.setParams(bundle, null);
    }
}
