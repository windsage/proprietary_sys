/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.quicinc.voice.activation.aidl.IInputReceiverCallback;
import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.activation.aidl.ISpeakProgressCallback;
import com.quicinc.voice.assist.sdk.inputprovider.InputProviderConnector;
import com.quicinc.voice.assist.sdk.tts.TTSServiceConnector;
import com.quicinc.voiceassistant.reference.data.LLMRepository;
import com.quicinc.voiceassistant.reference.data.Settings;
import com.quicinc.voiceassistant.reference.util.ASRResultSaver;
import com.quicinc.voiceassistant.reference.util.AppPermissionUtils;
import com.quicinc.voiceassistant.reference.views.ASRFragment;
import com.quicinc.voiceassistant.reference.views.ASRViewModel;
import com.quicinc.voiceassistant.reference.views.AboutFragment;
import com.quicinc.voiceassistant.reference.views.TTSFragment;
import com.quicinc.voiceassistant.reference.views.TTSViewModel;

import java.lang.ref.WeakReference;


public class ASRTTSActivity extends AppCompatActivity {

    protected final static String TAG = ASRTTSActivity.class.getSimpleName();

    private static final String KEY_START_SOURCE = "isStartFromRecognition";
    public static final String KEY_TEXT_INPUT = "LLMClient.textInput";
    public static final String KEY_FINISH = "LLMClient.finish";
    private boolean mIsStartFromRecognition;

    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mActionBarDrawerToggle;
    protected ActionBar mActionBar;
    protected NavigationView mNavigationView;
    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private ASRFragment mASRFragment;
    private TTSFragment mTTSFragment;

    private ASRViewModel mASRViewModel;
    private Observer<Boolean> mASRListeningObserver;
    private TTSViewModel mTTSViewModel;

    private InputProviderConnector mInputProviderConnector;
    private InputReceiverCallback mInputReceiverCallback = new InputReceiverCallback();

    private TTSServiceConnector mTTSServiceConnector;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> mRequestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean result) {
                            Log.d(TAG, "onActivityResult res=" + result);
                            if (!result) {
                                Toast.makeText(ASRTTSActivity.this, "No RECORD_AUDIO Permission",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                mASRViewModel.setMicPermissionGranted(true);
                                registerASRClientToQVA();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mIsStartFromRecognition = bundle.getBoolean(KEY_START_SOURCE, false);
        }
        initUI();

        mASRViewModel = new ViewModelProvider(this).get(ASRViewModel.class);
        mASRListeningObserver = listening -> {
            if (listening) {
                startRecording();
            } else {
                stopRecording();
            }
        };

        mTTSViewModel = new ViewModelProvider(this).get(TTSViewModel.class);
        mTTSViewModel.getTTSPlaying().observe(this, playing -> {
            if (playing) {
                startPlayTTS(mTTSViewModel.getTextPendingToSpeech().getValue());
            } else {
                stopPlayTTS();
            }
        });

        mInputProviderConnector = new InputProviderConnector(getApplicationContext());
        if (mIsStartFromRecognition) {
            mInputProviderConnector.connect(this::registerCallback);
        } else {
            registerASRClientToQVA();
        }

        mTTSServiceConnector = new TTSServiceConnector(getApplicationContext());
        mTTSServiceConnector.connect(()->{
            mTTSServiceConnector.initTTSEngine("English", null);
        });
        if (!AppPermissionUtils.isRuntimePermissionsGranted(this)) {
            mRequestPermission.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            mASRViewModel.setMicPermissionGranted(true);
        }
        Log.d(TAG, "onCreate finished.");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent.");
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mIsStartFromRecognition = extras.getBoolean(KEY_START_SOURCE, false);
            if (mIsStartFromRecognition) {
                if (Boolean.FALSE.equals(mASRViewModel.getASRListening().getValue())) {
                    registerCallback();
                }
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                }
                mActionBar.setTitle(R.string.app_sub_title);
                mActionBar.setBackgroundDrawable(getDrawable(R.drawable.actionbar_background));
                mViewPager.setCurrentItem(0);
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume.");
        super.onResume();
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            findViewById(R.id.main_activity_relative_layout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.main_activity_relative_layout).setVisibility(View.VISIBLE);
        }

    }

    /**
     * Initializes common UI elements.
     */
    protected void initUI() {
        //draw the layout
        setContentView(R.layout.activity_asr_tts);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(this, R.style.ToolbarText);
        toolbar.setTitle(R.string.app_sub_title);
        setSupportActionBar(toolbar);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mActionBar = getSupportActionBar();
        mNavigationView = findViewById(R.id.navigation_view);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.nav_open, R.string.nav_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        mDrawerLayout.addDrawerListener(mActionBarDrawerToggle);
        mActionBarDrawerToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Log.d(TAG, "Back stack changed");
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                Log.d(TAG, "In Fragment");
                mActionBarDrawerToggle.setDrawerIndicatorEnabled(false);
            } else {
                Log.d(TAG, "Main Activity");
                mActionBarDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        });

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                Fragment newFrag;
                String title;
                if (id == R.id.nav_settings) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    Intent it = new Intent(ASRTTSActivity.this, SettingsActivity.class);
                    startActivity(it, ActivityOptions.makeSceneTransitionAnimation(ASRTTSActivity.this).toBundle());
                    return true;
                } else if (id == R.id.nav_about) {
                    Log.d(TAG, "About Option in Menu");
                    newFrag = new AboutFragment();
                    title = "About";
                } else {
                    return false;
                }
                changeFragmentAndroidx(newFrag, title);
                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        mASRFragment = new ASRFragment();
        mTTSFragment = new TTSFragment();
        mViewPager = findViewById(R.id.asr_tts_pager);
        mViewPager.setAdapter(new ViewPagerAdapter(this));
        mTabLayout = findViewById(R.id.asr_tts_tablayout);
        new TabLayoutMediator(mTabLayout, mViewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("ASR");
                    else if (position == 1) tab.setText("TTS");
                }
        ).attach();
    }

    private void startPlayTTS(String text) {
        Log.d(TAG, "startPlayTTS text=" + text);
        Bundle bundle = new Bundle();
        bundle.putString("TEXT", text);
        WeakReference<ISpeakProgressCallback> callbackWeakReference =
                new WeakReference<ISpeakProgressCallback>(
                        new ISpeakProgressCallback.Stub() {
                            @Override
                            public void onStart(String textId) throws RemoteException {
                                Log.d(TAG, "TTS onStart textId=" + textId);
                            }

                            @Override
                            public void onStop(String textId) throws RemoteException {
                                Log.d(TAG, "TTS onStop textId=" + textId);
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTTSViewModel.setTTSPlaying(false);
                                    }
                                });
                            }

                            @Override
                            public void onComplete(String textId) throws RemoteException {
                                Log.d(TAG, "TTS onComplete textId=" + textId);
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTTSViewModel.setTTSPlaying(false);
                                    }
                                });
                            }

                            @Override
                            public void onError(String textId, int errorCode) throws RemoteException {
                                Log.d(TAG, "TTS onError textId=" + textId + "error=" + errorCode);
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTTSViewModel.setTTSPlaying(false);
                                    }
                                });
                            }
                        });
        mTTSServiceConnector.startSpeak(bundle, callbackWeakReference);
        setTabSwitchEnabled(false);
    }

    private void stopPlayTTS() {
        Log.d(TAG, "stopPlayTTS ");
        mTTSServiceConnector.stopSpeak();
        setTabSwitchEnabled(true);
    }

    private void onCallback(String output, boolean isFinished) {
        mMainHandler.post(() -> {
            mASRViewModel.setASRResult(output);
            mTTSViewModel.setTextPendingToSpeech(output);
            if (isFinished) {
                mASRViewModel.setASRListening(false);
                mIsStartFromRecognition = false;
            }
        });
        if (isFinished) {
            ASRResultSaver.save(ASRTTSActivity.this, output);
        }
    }

    private void startRecording() {
        Log.d(TAG, "startRecording");
        mMainHandler.post(() -> {
            mASRViewModel.setASRResult("");
        });
        if (!mIsStartFromRecognition) {
            Log.d(TAG, "startRecording - mIsStartFromRecognition = " + mIsStartFromRecognition);
            mInputProviderConnector.startRecording(null, mInputReceiverCallback);
        }
        mViewPager.setUserInputEnabled(false);
        setTabSwitchEnabled(false);
    }

    private void stopRecording() {
        Log.d(TAG, "stopRecording - mIsStartFromRecognition = " + mIsStartFromRecognition);
        mInputProviderConnector.stopRecording(null);
        mViewPager.setUserInputEnabled(true);
        setTabSwitchEnabled(true);
        mIsStartFromRecognition = false;
    }

    private void setTabSwitchEnabled(boolean enabled){
        mTabLayout.setEnabled(enabled);
        mTabLayout.getTabAt(0).view.setEnabled(enabled);
        mTabLayout.getTabAt(1).view.setEnabled(enabled);
    }

    public void changeFragmentAndroidx(Fragment frag, String title) {
        mActionBar.setTitle(title);
        mActionBar.setBackgroundDrawable(getDrawable(android.R.color.transparent));
        androidx.fragment.app.FragmentTransaction fragTransact = getSupportFragmentManager().beginTransaction();
        fragTransact.replace(R.id.main_layout, frag);
        findViewById(R.id.main_activity_relative_layout).setVisibility(View.GONE);
        fragTransact.addToBackStack(null);
        fragTransact.commit();
    }

    @Override
    public void onBackPressed() {
        // Reset title name:
        mActionBar.setTitle(R.string.app_sub_title);
        mActionBar.setBackgroundDrawable(getDrawable(R.drawable.actionbar_background));
        findViewById(R.id.main_activity_relative_layout).setVisibility(View.VISIBLE);
        super.onBackPressed();
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    }

    @Override
    protected void onDestroy() {
        ComponentName componentName = LLMRepository.getCurrentLLMIntent();
        Bundle extra = new Bundle();
        extra.putString("LLMClient.packageName",componentName.getPackageName());
        extra.putBoolean("LLMClient.disableASR", true);
        mInputProviderConnector.unregisterClient(extra);

        mInputProviderConnector.disconnect();
        mTTSServiceConnector.deInitTTSEngine();
        mTTSServiceConnector.disconnect();
        mInputReceiverCallback = null;
        mMainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void registerCallback() {
        Log.d(TAG, "registerCallback");
        updateASRConfiguration();
        enableASREngineIfNeeded();
        mInputProviderConnector.registerInputReceiverCallback(mInputReceiverCallback);
        mASRViewModel.setASRListening(Boolean.TRUE);
        mASRViewModel.getASRListening().observe(this, this.mASRListeningObserver);
    }

    private void registerASRClientToQVA() {
        Log.d(TAG, "registerASRClientToQVA");
        mInputProviderConnector.connect(() -> {
            updateASRConfiguration();
            enableASREngineIfNeeded();
            mASRViewModel.getASRListening().observe(this, mASRListeningObserver);
        });
    }

    private void enableASREngineIfNeeded() {
        ComponentName componentName = LLMRepository.getCurrentLLMIntent();
        Bundle extra = new Bundle();
        extra.putString("LLMClient.packageName", componentName.getPackageName());
        extra.putBoolean("LLMClient.enableASR", true);
        mInputProviderConnector.registerClient(extra);
    }

    private void updateASRConfiguration() {
        boolean translationEnabled = Settings.getASRTranslationEnabled(this);
        String asrLanguage = Settings.getASRLanguage(this);
        Bundle bundle = new Bundle();
        bundle.putBoolean("request.translation", translationEnabled);
        bundle.putString("request.language", asrLanguage);
        mInputProviderConnector.setParams(bundle, null);
    }

    private class InputReceiverCallback extends IInputReceiverCallback.Stub {

        public void onInputReceived(Bundle params) throws RemoteException {
            String output = params.getString(KEY_TEXT_INPUT, "");
            boolean isFinished = params.getBoolean(KEY_FINISH, false);
            Log.d(TAG, "received output:" + output + ", isFinished:" + isFinished);
            ASRTTSActivity.this.onCallback(output, isFinished);
        }
    }

    class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return mASRFragment;
            else return mTTSFragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
