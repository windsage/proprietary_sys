/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.dspasr;

import static android.content.Intent.getIntent;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognitionSupport;
import android.speech.RecognitionSupportCallback;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.text.TextUtils;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
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
import com.qualcomm.qti.voiceai.dspasr.data.Settings;
import com.qualcomm.qti.voiceai.dspasr.util.ASRResultSaver;
import com.qualcomm.qti.voiceai.dspasr.util.AppPermissionUtils;
import com.qualcomm.qti.voiceai.dspasr.util.TonePlayerManager;
import com.qualcomm.qti.voiceai.dspasr.views.ASRFragment;
import com.qualcomm.qti.voiceai.dspasr.views.ASRViewModel;
import com.qualcomm.qti.voiceai.dspasr.views.AboutFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ASRTTSActivity extends AppCompatActivity {

    protected final static String TAG = ASRTTSActivity.class.getSimpleName();

    private static final String KEY_START_SOURCE = "isStartFromRecognition";
    private boolean mNeedUpdateConfig;
    private Intent mEnpuASRConfig;

    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mActionBarDrawerToggle;
    protected ActionBar mActionBar;
    protected NavigationView mNavigationView;
    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private ASRFragment mASRFragment;

    private ASRViewModel mASRViewModel;
    private Observer<Boolean> mASRListeningObserver;

    private SpeechRecognizer mSpeechRecognizer = null;
    private boolean needStopASR = false;
    private boolean needPlayStopTone = false;
    private TonePlayerManager mTonePlayerManager = null;

    private final ExecutorService mExecutorService = Executors.newFixedThreadPool(8);

    private Handler mMainHandler;
    private static final String QUALCOMM_SPEECH_SERVICE_PACKAGE_NAME =
            "com.qualcomm.qti.voiceai.speech";

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
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(getResources().getColor(R.color.background, getTheme())));
        super.onCreate(savedInstanceState);
        needStopASR = false;
        mTonePlayerManager = new TonePlayerManager(this);

        initUI();
        initASR(this);

        mASRViewModel = new ViewModelProvider(this).get(ASRViewModel.class);
        mASRListeningObserver = listening -> {
            if (listening) {
                startRecording();
            } else {
                stopRecording();
            }
        };

        mASRViewModel.getASRListening().observe(this, mASRListeningObserver);

        if (!AppPermissionUtils.isRuntimePermissionsGranted(this)) {
            mRequestPermission.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            mASRViewModel.setMicPermissionGranted(true);
        }
        if (mEnpuASRConfig == null) {
            mEnpuASRConfig = getEnpuASRConfig(this);
        }

        mMainHandler = new MyHandler();
        ClientApplication.getInstance().setHandler((MyHandler) mMainHandler);
        Log.d(TAG, "onCreate finished.");
    }

    private Intent getEnpuASRConfig(Context context) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, Settings.getPartialTranscriptionEnabled(this));
        intent.putExtra("android.speech.extra.ENABLE_CONTINUOUS_TRANSCRIPTION",
                Settings.getContinuousTranscriptionEnabled(this));
        intent.putExtra("android.speech.extra.ENABLE_OUTPUT_BUFFER_MODE",
                Settings.getLowPowerBufferModeEnabled(this));
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                Integer.valueOf(Settings.getASRTimeout(this)));
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 0);
        intent.putExtra("android.speech.extra.ENABLE_TRANSLATE", false);//not supported
        intent.putExtra("android.speech.extra.ENABLE_DSP_ASR", true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Settings.getASRLanguage(this));
        Log.d(TAG, "getEnpuASRConfig: " + intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Log.d(TAG, "Key=" + key + ", content=" + bundle.get(key));
            }
        }
        return intent;
    }

    private void initASR(Context context) {
        if (mSpeechRecognizer == null) {
            boolean recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context);
            Log.d(TAG, "recognitionAvailable: " + recognitionAvailable);
            if (!recognitionAvailable) {
                return;
            }

//            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> list = packageManager.queryIntentServices(
                    new Intent(RecognitionService.SERVICE_INTERFACE), 0);
            if (list.size() > 0) {
                for (ResolveInfo item : list) {
                    ServiceInfo serviceInfo = item.serviceInfo;
                    String serviceName = serviceInfo.name;
                    String packageName = serviceInfo.packageName;
                    if (QUALCOMM_SPEECH_SERVICE_PACKAGE_NAME.equals(packageName)) {
                        ComponentName componentName = new ComponentName(packageName, serviceName);
                        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context,
                                componentName);
                        break;
                    }
                }
                if (mSpeechRecognizer == null) {
                    Log.e(TAG, "initASR fail no QC speech asr");
                    return;
                }
            } else {
                return;
            }
            mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "onReadyForSpeech params=" + params);
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    mASRViewModel.setASRListening(false);
                }

                @Override
                public void onError(int error) {
                    Log.d(TAG, "onError error=" + error);
                    needStopASR = false;
                    if (error == SpeechRecognizer.ERROR_SERVER
                        || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                        || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                        || error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                        || error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED) {
                        if(error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED){
                            if (mSpeechRecognizer == null) {
                                Log.d(TAG, "destroy mSpeechRecognizer is null ");
                                return;
                            }
                            mSpeechRecognizer.destroy();
                            mSpeechRecognizer = null;
                            initASR(context);
                        }
                        if (mASRViewModel.getASRListening().getValue()) {
                            mASRViewModel.setASRListening(Boolean.FALSE);
                        }
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    Log.d(TAG, "onResults results=" + results);
                    needStopASR = false;
                    ArrayList<String> arrayList =
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    Log.d(TAG, "onResults  arrayList == null ? " + (arrayList == null));
                    if (arrayList != null) {

                        Log.d(TAG, "onResults  arrayList.size() = " + arrayList.size());
                    }
                    StringBuffer sbResult = new StringBuffer();
                    if ((arrayList != null) && (arrayList.size() != 0)) {
                        for (int i = 0; i < arrayList.size(); i++) {
                            Log.d(TAG, "onResults i=" + i + " content = " + arrayList.get(i));
                            sbResult.append(arrayList.get(i));
                        }
                        if (Settings.getLowPowerBufferModeEnabled(getApplicationContext())) {
                            mMainHandler.post(() -> {
                                mASRViewModel.setASRResult(sbResult.toString());
                                mASRViewModel.setASRListening(false);
                            });
                        }else{
                            mMainHandler.post(() -> {
                                mASRViewModel.setASRResult(sbResult.toString());
                                mASRViewModel.setASRListening(false);
                            });
                        }

                        ASRResultSaver.save(ASRTTSActivity.this, sbResult.toString());
                    } else {
                        Log.e(TAG, "onResults arrayList = null or size = 0");
                    }

                    float[] floatArray = results.getFloatArray(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);
                    if (floatArray != null) {
                        for (float it : floatArray) {
                            Log.d(TAG, " onResults score: " + it);
                        }
                    } else {
                        Log.e(TAG, "onResults floatArray = null");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    Log.d(TAG, "onPartialResults partialResults=" + partialResults);
                    ArrayList<String> arrayList =
                            partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (arrayList != null) {
                        for (int i = 0; i < arrayList.size(); i++) {
                            Log.d(TAG, "onPartialResults i=" + i + " content = " + arrayList.get(i));
                        }
                        StringBuffer sb = new StringBuffer();

                        if (Settings.getLowPowerBufferModeEnabled(getApplicationContext())) {
                            Log.d(TAG, "onPartialResults BufferMode Enabled");

                            for (int j = 0; j < arrayList.size(); j++) {
                                sb.append(String.valueOf(j) + " times asr_result: ");
                                sb.append(arrayList.get(j));
                                sb.append("\n");
                            }
                            mMainHandler.post(() -> {
                                mASRViewModel.setASRResult(sb.toString());
                            });
                        } else {
                            for (int j = 0; j < arrayList.size(); j++) {
                                sb.append(arrayList.get(j));
                            }
                            if(!TextUtils.isEmpty(sb.toString().trim())) {
                                mMainHandler.post(() -> {
                                    mASRViewModel.setASRResult(sb.toString());
                                });
                            }
                        }
                    } else {
                        Log.e(TAG, "onPartialResults arrayList = null");
                    }

                    float[] floatArray = partialResults.getFloatArray(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);
                    if (floatArray != null) {
                        for (float it : floatArray) {
                            Log.d(TAG, " onPartialResults score: " + it);
                        }
                    } else {
                        Log.e(TAG, "onPartialResults floatArray = null");
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    Log.d(TAG, " onEvent eventType:" + eventType + "  params:" + params);
                }
            });
        }
    }

    private void startASR(Context context) {
        if (mSpeechRecognizer == null) {
            Log.d(TAG, "mSpeechRecognizer is null ");
            return;
        }

        mSpeechRecognizer.checkRecognitionSupport(mEnpuASRConfig, mExecutorService
                , mRecognitionSupportCallback);

        mSpeechRecognizer.startListening(mEnpuASRConfig);

    }

    private void stopASR(Context context) {
        if (mSpeechRecognizer == null) {
            Log.d(TAG, "stopASR mSpeechRecognizer is null ");
            return;
        }

        mSpeechRecognizer.stopListening();

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

    private void startRecording() {
        Log.d(TAG, "startRecording");
        needStopASR = true;
        needPlayStopTone = true;

        mMainHandler.post(() -> {
            mASRViewModel.setASRResult("");
        });
        startASR(this);
        mViewPager.setUserInputEnabled(false);
        setTabSwitchEnabled(false);
    }

    private void stopRecording() {
        if(needPlayStopTone){
            mTonePlayerManager.playMayWait(
                TonePlayerManager.SUCCESS_TONE_INDEX);
            needPlayStopTone = false;
         }

        if(needStopASR) {
            stopASR(this);
            needStopASR = false;
        }
        mViewPager.setUserInputEnabled(true);
        setTabSwitchEnabled(true);
    }

    private void setTabSwitchEnabled(boolean enabled) {
        mTabLayout.setEnabled(enabled);
        mTabLayout.getTabAt(0).view.setEnabled(enabled);
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
        Log.d(TAG, "onDestroy.");
        stopASR(this);

        mMainHandler.removeCallbacksAndMessages(null);
        needStopASR = false;
        super.onDestroy();
    }

    private void registerCallback() {
        Log.d(TAG, "registerCallback");
        updateASRConfiguration();
        mASRViewModel.setASRListening(Boolean.TRUE);
        mASRViewModel.getASRListening().observe(this, this.mASRListeningObserver);
    }


    private void updateASRConfiguration() {
        boolean translationEnabled = Settings.getASRTranslationEnabled(this);
        String asrLanguage = Settings.getASRLanguage(this);
        Bundle bundle = new Bundle();
        bundle.putBoolean("request.translation", translationEnabled);
        bundle.putString("request.language", asrLanguage);

    }

    private final RecognitionSupportCallback mRecognitionSupportCallback = new RecognitionSupportCallback() {
        @Override
        public void onSupportResult(RecognitionSupport recognitionSupport) {
            Log.d(TAG, "onSupportResult recognitionSupport:" + recognitionSupport);
            if (recognitionSupport != null) {
                List<String> installedOnDeviceLanguages = recognitionSupport.getInstalledOnDeviceLanguages();
                Log.d(TAG, " installedOnDeviceLanguages: " + installedOnDeviceLanguages);
                List<String> pendingOnDeviceLanguages = recognitionSupport.getPendingOnDeviceLanguages();
                List<String> supportedOnDeviceLanguages = recognitionSupport.getSupportedOnDeviceLanguages();
                List<String> onlineLanguages = recognitionSupport.getOnlineLanguages();
                Log.d(TAG, " pendingOnDeviceLanguages: " + pendingOnDeviceLanguages
                        + ", supportedOnDeviceLanguages: " + supportedOnDeviceLanguages
                        + ", onlineLanguages: " + onlineLanguages);
            }
        }

        @Override
        public void onError(int error) {
            Log.d(TAG, "onError error:" + error);
        }
    };

    class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mASRFragment;
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.d(TAG, "handleMessage msg:" + msg);
            if (msg.what == 2000) {
                mEnpuASRConfig = getEnpuASRConfig(ASRTTSActivity.this);
            }
        }
    }
}
