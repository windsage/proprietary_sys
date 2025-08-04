/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.qualcomm.qti.sva.controller.Global;
import com.qualcomm.qti.sva.controller.RecordingsMgr;
import com.qualcomm.qti.sva.onlineepd.EPD;
import com.qualcomm.qti.sva.utils.LogUtils;
import com.qualcomm.qti.sva.utils.Utils;

public class RecordingActivity extends Activity {
    private final String TAG = RecordingActivity.class.getSimpleName();

    private RecordingsMgr mRecordingsMgr;
    private TextView mRecordingTextView;
    private Button mCancelRecordingButton;
    private SoundWave mSoundWave;
    private KeyguardManager mKeyguardManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtils.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);
        Utils.setUpEdgeToEdge(this);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mKeyguardManager.requestDismissKeyguard(this,
                new KeyguardManager.KeyguardDismissCallback() {
            @Override
            public void onDismissSucceeded() {
                LogUtils.d(TAG, "onDismissSucceeded");
                super.onDismissSucceeded();
            }

            @Override
            public void onDismissCancelled() {
                LogUtils.d(TAG, "onDismissCancelled");
                super.onDismissCancelled();
            }

            @Override
            public void onDismissError() {
                LogUtils.d(TAG, "onDismissError");
                super.onDismissError();
            }
        });
        mRecordingsMgr = Global.getInstance().getRecordingsMgr();
        initViews();
        if (mRecordingsMgr.isRecordingFailed()) {
            finish();
        } else {
            mRecordingsMgr.registerRecordingCallback(new IRecordingCallback() {
                @Override
                public void onRecordFinished() {
                    LogUtils.d(TAG, "onRecordFinished call finish!");
                    finish();
                }

            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordingsMgr.setAudioDataUpdatedListener(new RecordingsMgr.IAudioDataUpdatedListener() {
            @Override
            public void onAudioCapture(int volume) {
                mSoundWave.setData(volume);
            }

            @Override
            public void onEPDSuccess(EPD.ProcessResult result) {
                LogUtils.d("EPD onEPDSuccess result = " + result);
            }
        });
    }

    @Override
    protected void onStop() {
        boolean isKeyguardLocked = false;
        if (mKeyguardManager != null) {
            isKeyguardLocked = mKeyguardManager.isKeyguardLocked();
            LogUtils.d(TAG, "onStop isKeyguardLocked = " + isKeyguardLocked);
        }
        if (!isKeyguardLocked) {
            mRecordingsMgr.stopHotWordRecording();
        }
        super.onStop();
    }

    private void initViews() {
        mRecordingTextView = (TextView) findViewById(R.id.recording_description);
        mSoundWave = (SoundWave) findViewById(R.id.recording_sound_wave);
        mCancelRecordingButton = (Button) findViewById(R.id.cancel_recording);
        mCancelRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecordingsMgr.stopHotWordRecording();
            }
        });
    }

    public interface IRecordingCallback {
        public void onRecordFinished();
    }
}
