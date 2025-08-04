/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.dspasr;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import com.qualcomm.qti.voiceai.dspasr.data.Settings;

public class SettingsActivity extends AppCompatActivity {

    protected final static String TAG = SettingsActivity.class.getSimpleName();
    private static final int UPDATE_ASR_CONFIG = 2000;
    private Switch mContinuousTranscriptionToggle;
    private Switch mPartialTranscriptionToggle;

    private Handler mHandler = null;

    private final String[] mASRSupportedLanguage = {"English", "Chinese"};
    private ImageView mEnableUVBackground;
    private Toast mToast;
    private Switch mEnableLowPowerBufferMode;
    private EditText mEtTimeout;
    private ImageView mLanguageSelectBackground;
    private TextView mCurrentLanguage;
    private Switch mTranslationEnabledSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
            SystemBarStyle.dark(getResources().getColor(R.color.background, getTheme())));
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_settings);
        ClientApplication.getInstance().addActivityInstance(this);
        mHandler = ClientApplication.getInstance().getHandler();
        initializeUI();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        String asrLanguage = Settings.getASRLanguage(this);
        if (TextUtils.isEmpty(asrLanguage)) {
            asrLanguage = mASRSupportedLanguage[0];
        }
        mCurrentLanguage.setText(asrLanguage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ClientApplication.getInstance().removeActivityInstance(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void initializeUI() {
        ImageView back = findViewById(R.id.settings_toolbar_button);
        back.setOnClickListener(view -> finish());
        mContinuousTranscriptionToggle = findViewById(R.id.continuous_transcription_toggle);
        mContinuousTranscriptionToggle.setChecked(Settings.getContinuousTranscriptionEnabled(this));
        mContinuousTranscriptionToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> toggleContinuousTranscription(isChecked));

        mPartialTranscriptionToggle = findViewById(R.id.partial_transcription);
        mPartialTranscriptionToggle.setChecked(Settings.getPartialTranscriptionEnabled(this));
        mPartialTranscriptionToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> togglePartialTranscription(isChecked));

        mEnableLowPowerBufferMode = findViewById(R.id.low_power_buffer_mode);
        mEnableLowPowerBufferMode.setChecked(Settings.getLowPowerBufferModeEnabled(this));
        mEnableLowPowerBufferMode.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> toggleLowPowerBufferMode(isChecked));
        if(Settings.getLowPowerBufferModeEnabled(this)) {
            mContinuousTranscriptionToggle.setChecked(true);
            mPartialTranscriptionToggle.setChecked(false);
            mContinuousTranscriptionToggle.setClickable(false);
            mPartialTranscriptionToggle.setClickable(false);
        }

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

            }
        });

        mEtTimeout = findViewById(R.id.voice_config_timeout);
        mEtTimeout.setText(String.valueOf(Settings.getASRTimeout(this)));
        mEtTimeout.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int value = Integer.valueOf(s.toString());
                    if (value > 10000 || value < 0) {
                        String content = " timeout should be 0-10000";
                        mToast = Toast.makeText(SettingsActivity.this, content, Toast.LENGTH_SHORT);
                        mToast.setText(content);
                        mToast.setDuration(Toast.LENGTH_SHORT);
                        mToast.show();
                        return;
                    }
                    Settings.setASRTimeout(SettingsActivity.this, s.toString());
                    updateConfig();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void setParams(boolean disableUV) {
        Bundle bundle = new Bundle();
        bundle.putString("request.type", "HeySnapdragon_en-US_041.uim");
        bundle.putString("request.locale", "en-US");
        bundle.putBoolean("UV.disable", disableUV);
    }


    private void toggleContinuousTranscription(boolean enable) {
        Log.d(TAG, "toggleContinuousTranscription  enable = " + enable);
//        mContinuousTranscriptionToggle.setClickable(enable);
        Settings.setContinuousTranscriptionEnabled(this, enable);
        updateConfig();
    }

    private void toggleLowPowerBufferMode(boolean enable) {
        Log.d(TAG, "toggleLowPowerBufferMode  enable = " + enable);
        if(enable) {
            mContinuousTranscriptionToggle.setChecked(true);
            mPartialTranscriptionToggle.setChecked(false);
            mContinuousTranscriptionToggle.setClickable(false);
            mPartialTranscriptionToggle.setClickable(false);
        }else{
            mContinuousTranscriptionToggle.setClickable(true);
            mPartialTranscriptionToggle.setClickable(true);
        }
        Settings.setLowPowerBufferModeEnabled(this, enable);
        updateConfig();
    }

    private void togglePartialTranscription(boolean enable) {
        Log.d(TAG, "togglePartialTranscription  enable = " + enable);
//        mContinuousTranscriptionToggle.setClickable(enable);
        Settings.setPartialTranscriptionEnabled(this, enable);
        updateConfig();
    }

    private void updateConfig() {
        mHandler.sendEmptyMessage(UPDATE_ASR_CONFIG);
    }

}
