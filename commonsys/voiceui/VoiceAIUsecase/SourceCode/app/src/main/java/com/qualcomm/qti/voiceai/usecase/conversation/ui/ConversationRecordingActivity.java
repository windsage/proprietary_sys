/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.conversation.ui;

import android.Manifest;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.qualcomm.qti.voiceai.usecase.Facade;
import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.asr.ConversationMode;
import com.qualcomm.qti.voiceai.usecase.asr.RecognitionResultListener;
import com.qualcomm.qti.voiceai.usecase.conversation.data.Settings;
import com.qualcomm.qti.voiceai.usecase.utils.AppPermissionUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class ConversationRecordingActivity extends AppCompatActivity {

    protected final static String TAG = ConversationRecordingActivity.class.getSimpleName();

    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private ConversationFragment mConversationFragment;
    private SettingsFragment mSettingsFragment;
    private SessionsFragment mSessionsFragment;
    private ConversationViewModel mConversationViewModel;
    private Observer<Boolean> mASRListeningObserver;
    private boolean needsClientStopASR;

    private BufferModeHandlerThread bufferModeHandlerThread = null;
    private static final String EXTRA_IS_FINAL = "is_final";

    private final ActivityResultLauncher<String> mRequestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean result) {
                            Log.d(TAG, "onActivityResult res=" + result);
                            if (!result) {
                                Toast.makeText(ConversationRecordingActivity.this, "No RECORD_AUDIO Permission",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                mConversationViewModel.setMicPermissionGranted(true);
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        bufferModeHandlerThread =
                new BufferModeHandlerThread("bufferModeHandlerThread", this);
        bufferModeHandlerThread.start();

        EdgeToEdge.enable(this,
                SystemBarStyle.dark(getResources().getColor(R.color.background, getTheme())));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.background, getTheme()));
        initUI();
        mConversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        mASRListeningObserver = listening -> {
            if (listening) {
                startRecording();
            } else {
                stopRecording();
            }
        };

        mConversationViewModel.getASRListening().observe(this, mASRListeningObserver);

        if (!AppPermissionUtils.isRuntimePermissionsGranted(this)) {
            mRequestPermission.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            mConversationViewModel.setMicPermissionGranted(true);
        }
        needsClientStopASR = false;

        Log.d(TAG, "onCreate finished.");
    }


    /**
     * Initializes common UI elements.
     */
    protected void initUI() {
        //draw the layout
        setContentView(R.layout.activity_conversation_recording);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(this, R.style.ToolbarText);
        toolbar.setTitle(R.string.conversation_title);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        mConversationFragment = new ConversationFragment();
        mSettingsFragment = new SettingsFragment();
        mSessionsFragment = new SessionsFragment();
        mViewPager = findViewById(R.id.conversation_pager);

        mTabLayout = findViewById(R.id.conversation_tablayout);
        mViewPager.setAdapter(new ViewPagerAdapter(this));
        new TabLayoutMediator(mTabLayout, mViewPager,
                (tab, position) -> {
                         if (position == 0) {
                             tab.setIcon(R.drawable.conversation_mode);
                             tab.setText("Conversation");
                             }
                         else if (position == 1) {
                             tab.setText("Sessions");
                             tab.setIcon(R.drawable.sessions_mode);
                             }
                         else if (position == 2) {
                                 tab.setIcon(R.drawable.settings_mode);
                                 tab.setText("Settings");
                             }

                }
        ).attach();
    }
    private void startRecording() {
        Log.d(TAG, "startRecording");
        int indexLanguage =  Settings.getTranscriptionLanguage(this);
        String[] languages = getResources().getStringArray(R.array.support_transcription_languages);
        Log.d(TAG, "startRecording languages =" + Arrays.toString(languages));
        needsClientStopASR = true;
        ConversationMode conversationMode =  Settings.getRealtimeModeEnabled(this) ?
                ConversationMode.REAL_TIME : ConversationMode.OUTPUT_AT_END_OF_CONVERSATION;
        Facade.getASRManager().startASR(languages[indexLanguage],conversationMode,
                new RecognitionResultListener() {
            @Override
            public void onError(int errorCode) {
                Log.d(TAG, "onError errorCode="+errorCode);
                if(errorCode == SpeechRecognizer.ERROR_SERVER_DISCONNECTED
                    || errorCode == SpeechRecognizer.ERROR_SERVER) {
                    Log.d(TAG,"Server Error or Speech Service Disconnected, Re-connect Later");
                    Facade.getASRManager().destroy();
                }
                needsClientStopASR = false;
                if (Boolean.TRUE.equals(mConversationViewModel.getASRListening().getValue())) {
                    mConversationViewModel.setASRListening(Boolean.FALSE);
                }
            }

            @Override
            public void onResults(Bundle results) {
                Log.d(TAG, "onResults results="+results);
                needsClientStopASR = false;
                ArrayList<String> finals = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (conversationMode != ConversationMode.REAL_TIME) {
                    //TODO: buffer mode
                    BufferModeHandlerThread.addFinalResult(results);
                }
            }

            @Override
            public void onPartialResult(Bundle partialResult) {
                Log.d(TAG, "onPartialResult partialResult="+partialResult);
                ArrayList<String> finals = partialResult.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                boolean isFinal = partialResult.getBoolean(EXTRA_IS_FINAL);
                if(conversationMode == ConversationMode.REAL_TIME) {
                    if(finals != null && finals.size() > 0) {
                        mConversationViewModel.setASRResult(new ConversationViewModel.AsrResult(
                                                            isFinal, finals.get(finals.size() -1)));
                    }
                }else{
                    ArrayList<String> finalsBufferMode = partialResult.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    for(String item : finalsBufferMode) {
                        Log.d(TAG, "onPartialResult BufferMode result=" + item);
                    }
                    BufferModeHandlerThread.addPartialResult(partialResult);
                }
            }
        });

        mViewPager.setUserInputEnabled(false);
        setTabSwitchEnabled(false);
    }

    private void stopRecording() {
        Log.d(TAG, "stopRecording");
        if(needsClientStopASR) {
            Facade.getASRManager().stopASR();
        }
        mViewPager.setUserInputEnabled(true);
        setTabSwitchEnabled(true);
    }

    private void setTabSwitchEnabled(boolean enabled){
        mTabLayout.setEnabled(enabled);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }


    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bufferModeHandlerThread.quitSafely();
        bufferModeHandlerThread = null;
        Facade.getASRManager().destroy();
    }

    class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {

            if (position == 0) {
                return mConversationFragment;
            }else if (position == 1) {
                return mSessionsFragment;
            }else if (position == 2) {
                return mSettingsFragment;
            }
            return mConversationFragment;
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
