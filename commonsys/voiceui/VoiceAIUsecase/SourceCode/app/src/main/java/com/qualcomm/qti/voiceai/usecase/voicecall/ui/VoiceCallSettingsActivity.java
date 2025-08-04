/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.translation.TranslationManager;
import com.qualcomm.qti.voiceai.usecase.voicecall.VoiceCallSettings;
import com.qualcomm.qti.voiceai.usecase.voicecall.VoiceCallTranslationService;

public class VoiceCallSettingsActivity extends AppCompatActivity {

    private static final String TAG = VoiceCallSettingsActivity.class.getSimpleName();
    private ActivityResultLauncher<Void> mOverlayPermissionLauncher;
    private Switch mMuteMyVoice;
    private Switch mMuteOthersVoice;
    private Switch mEnableFeature;
    private Spinner mMyLanguages;
    private Spinner mOthersLanguages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(getResources().getColor(R.color.background, getTheme())));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.background, getTheme()));
        setContentView(R.layout.activity_voice_call_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton back = findViewById(R.id.back_button);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        initSettingsUI();
        createOverlayPermissionLauncher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSettings();
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
        mEnableFeature = findViewById(R.id.enable_feature_switch);
        mEnableFeature.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                if (!TranslationManager.isAllModesDownloaded()) {
                    mEnableFeature.setChecked(false);
                    Toast.makeText(this, "Please connect to internet and wait for " +
                                    "all translation models to be downloaded",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (checkOverlayAppPermission()) {
                        VoiceCallTranslationService.enableFeature(VoiceCallSettingsActivity.this, true);
                        VoiceCallSettings.setVoiceCallTranslationEnabled(isChecked);
                    } else {
                        startOverlayAppPermissionActivityForResult();
                    }
                }
            } else {
                if (isChecked == VoiceCallSettings.getVoiceCallTranslationEnabled()) return;
                VoiceCallTranslationService.enableFeature(VoiceCallSettingsActivity.this, false);
                VoiceCallSettings.setVoiceCallTranslationEnabled(isChecked);
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
    }

    private void updateSettings() {
        mMuteMyVoice.setChecked(VoiceCallSettings.getMyVoiceMute());
        mMuteOthersVoice.setChecked(VoiceCallSettings.getOtherPersonVoiceMute());
        mEnableFeature.setChecked(VoiceCallSettings.getVoiceCallTranslationEnabled());
        String otherPersonLanguage = VoiceCallSettings.getRxLanguage();
        String myLanguage = VoiceCallSettings.getTxLanguage();
        mOthersLanguages.setSelection(VoiceCallSettings.getSupportedLanguages().indexOf(otherPersonLanguage));
        mMyLanguages.setSelection(VoiceCallSettings.getSupportedLanguages().indexOf(myLanguage));
    }

    private void createOverlayPermissionLauncher() {
        mOverlayPermissionLauncher = registerForActivityResult(new ActivityResultContract<Void, Boolean>() {
            @Override
            public Boolean parseResult(int i, @Nullable Intent intent) {
                return i == RESULT_OK;
            }

            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Void o) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));

                return intent;
            }
        }, new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean o) {
                //trigger onResume
                if (checkOverlayAppPermission()) {
                    VoiceCallTranslationService.enableFeature(VoiceCallSettingsActivity.this, true);
                    VoiceCallSettings.setVoiceCallTranslationEnabled(true);
                } else {
                    VoiceCallSettings.setVoiceCallTranslationEnabled(false);
                }
            }
        });
    }

    private boolean checkOverlayAppPermission() {
        return Settings.canDrawOverlays(this);
    }

    private void startOverlayAppPermissionActivityForResult() {
        mOverlayPermissionLauncher.launch(null);
    }
}