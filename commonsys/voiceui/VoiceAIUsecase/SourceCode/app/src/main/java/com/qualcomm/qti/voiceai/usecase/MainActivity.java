/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.qualcomm.qti.voiceai.usecase.conversation.ui.ConversationRecordingActivity;
import com.qualcomm.qti.voiceai.usecase.voicecall.ui.VoiceCallSettingsActivity;

public class MainActivity extends AppCompatActivity {
    private ImageView mConversationRecordingBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(getResources().getColor(R.color.background, getTheme())));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.background, getTheme()));
        setContentView(R.layout.activity_main);

        mConversationRecordingBackground = findViewById(R.id.conversation_recording_background);
        mConversationRecordingBackground.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ConversationRecordingActivity.class);
            startActivity(intent);
        });

        ImageView voiceCall = findViewById(R.id.voice_call_translation_background);
        voiceCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activityIntent = new Intent();
                activityIntent.setClassName(getPackageName(), VoiceCallSettingsActivity.class.getName());
                startActivity(activityIntent);
            }
        });

        ActivityResultLauncher launcher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean o) {

            }
        });
        launcher.launch(Manifest.permission.RECORD_AUDIO);
    }
}
