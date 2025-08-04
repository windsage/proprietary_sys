/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.ramdumpcopyui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import static com.qualcomm.qti.ramdumpcopyui.Constants.*;

public class FullscreenActivity extends AppCompatActivity {
    private static final String TAG = "RamdumpUI";

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private ProgressBar mBar;
    private TextView mTextPercent, mTextProgress, mTextCopyFile, mTextDummy;
    private boolean mFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        mBar = findViewById(R.id.progressBar);
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        init();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void init() {
        mTextPercent = findViewById(R.id.text_per);
        mTextProgress = findViewById(R.id.text_progress);
        mTextCopyFile = findViewById(R.id.text_copy_file);
        mTextDummy = findViewById(R.id.fullscreen_content);
        Intent intent = new Intent(this, RamdumpService.class);
        startForegroundService(intent);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(RamdumpService.ACTION));
        Log.d(TAG, "registerReceiver");
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "unregisterReceiver");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void autoFinish() {
        Log.d(TAG, "autoFinish");
        Intent service = new Intent(this, RamdumpService.class);
        stopService(service);
        FullscreenActivity.this.finish();
    }

    private void setProressStatus(boolean show) {
        findViewById(R.id.process_context).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateUI(Bundle resultData) {
        final int resultCode = resultData.getInt(RamdumpService.CALL_BACK);
        Log.d(TAG, "updateUI resultCode=" + resultCode);
        switch (resultCode) {
            case CMD_VALIDATED:
                mTextDummy.setText(errorToString(resultData.getInt(RamdumpService.TAG_ERROR)));
                setProressStatus(false);
                autoFinish();
                break;
            case CMD_COPY_UPDATE:
                setProressStatus(true);
                int curIndex = resultData.getInt(RamdumpService.TAG_CURRENT_INDEX);
                int count = resultData.getInt(RamdumpService.TAG_TOTAL_COUNT);
                int percent = curIndex * 100 / count;
                Log.d(TAG, "index:" + curIndex + " count=" + count);
                mTextCopyFile.setText(resultData.getString(RamdumpService.TAG_FILE_NAME));
                mTextPercent.setText(percent + "%");
                mTextProgress.setText(curIndex + "/" + count);
                mBar.setProgress(percent);
                break;
            case CMD_COPY_FINISHED:
                Log.d(TAG, "copy finished");
                setProressStatus(true);
                if (EIO == resultData.getInt(RamdumpService.TAG_ERROR)) {
                    mTextDummy.setText(errorToString(EIO));
                } else {
                    mTextDummy.setText("Copy done: " + resultData.getString(
                            RamdumpService.TAG_PATH));
                }
                mBar.setProgress(100);
                mTextPercent.setText("100%");
                mFinished = true;
                autoFinish();
                break;
            case CMD_TOTAL_SIZE:
                String error = errorToString(resultData.getInt(RamdumpService.TAG_ERROR));
                if (!TextUtils.isEmpty(error)) {
                    setProressStatus(false);
                    mTextDummy.setText(error);
                    Log.d(TAG, "size error:" + error);
                    autoFinish();
                }
                break;
            case CMD_CONNECT_REFUSED:
                Log.d(TAG, errorToString(ECONNREFUSED) + " mFinished=" + mFinished);
                if (!mFinished) {
                    mTextDummy.setText(errorToString(ECONNREFUSED));
                    setProressStatus(false);
                }
                autoFinish();
                break;
            default:
                Log.e(TAG, "updateUI error cmd:" + resultCode);
                break;
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null != intent) {
                updateUI(intent.getExtras());
            } else {
                Log.e(TAG, "NUll intent in receiver!");
            }
        }
    };

}
