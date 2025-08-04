/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.ui;


import static com.qualcomm.qti.voiceai.usecase.voicecall.VoiceCallTranslationService.STATE_DEINIT;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.voicecall.QLog;
import com.qualcomm.qti.voiceai.usecase.voicecall.VoiceCallSettings;
import com.qualcomm.qti.voiceai.usecase.voicecall.VoiceCallTranslationService;

public class VoiceCallTranslationActivity extends AppCompatActivity {

    private static final String TAG = VoiceCallTranslationActivity.class.getSimpleName();
    private Switch mMuteMyVoice;
    private Switch mMuteOthersVoice;
    private View mExitTranslation;
    private Spinner mMyLanguages;
    private Spinner mOthersLanguages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(getResources().getColor(R.color.background, getTheme())));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.background, getTheme()));
        setContentView(R.layout.activity_voice_call_translation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initSettingsUI();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        QLog.VCLogD(TAG ,"onNewIntent");
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int callState = getIntent().getIntExtra("call_state", -1);
        QLog.VCLogD(TAG ,"onResume callState=" + callState);
        if (callState == STATE_DEINIT) {
            finish();
        } else {
            updateSettings();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initSettingsUI() {
        mMuteMyVoice = findViewById(R.id.mute_my_voice_switch);
        mMuteMyVoice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                VoiceCallSettings.saveMyVoiceMute(isChecked);
            }
        });
        mMuteOthersVoice = findViewById(R.id.mute_other_voice_switch);
        mMuteOthersVoice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                VoiceCallSettings.saveOtherPersonVoiceMute(isChecked);
            }
        });

        mMyLanguages = findViewById(R.id.my_language_list);
        ArrayAdapter<String> myLanguageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, VoiceCallSettings.getSupportedLanguages());
        mMyLanguages.setAdapter(myLanguageAdapter);
        mMyLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                VoiceCallSettings.saveTxLanguage(VoiceCallSettings.getSupportedLanguages().get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mOthersLanguages = findViewById(R.id.others_language_list);
        ArrayAdapter<String> othersLanguageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, VoiceCallSettings.getSupportedLanguages());
        mOthersLanguages.setAdapter(othersLanguageAdapter);
        mOthersLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                VoiceCallSettings.saveRxLanguage(VoiceCallSettings.getSupportedLanguages().get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mExitTranslation = findViewById(R.id.exit_session_background);
        mExitTranslation.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            VoiceCallTranslationService.enterTranslationMode(this, false);
        });
    }

    private void updateSettings() {
        QLog.VCLogD(TAG ,"updateSettings");
        mMuteMyVoice.setChecked(VoiceCallSettings.getMyVoiceMute());
        mMuteOthersVoice.setChecked(VoiceCallSettings.getOtherPersonVoiceMute());
        String otherPersonLanguage = VoiceCallSettings.getRxLanguage();
        String myLanguage = VoiceCallSettings.getTxLanguage();
        mOthersLanguages.setSelection(VoiceCallSettings.getSupportedLanguages().indexOf(otherPersonLanguage));
        mMyLanguages.setSelection(VoiceCallSettings.getSupportedLanguages().indexOf(myLanguage));
    }
}