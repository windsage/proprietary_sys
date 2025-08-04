/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.content.res.AppCompatResources;

import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.voicecall.ui.VoiceCallTranslationActivity;

public class VoiceCallFloatWindow {

    static String TAG = "FloatWindow";

    private final Context mContext;
    private final WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mRootView;
    private ImageButton mButton;
    private boolean isShowing;
    private int mWidth;
    private int mHeigh;
    private int mNavigationBarHeight;

    public VoiceCallFloatWindow(Context context) {
        mContext = context;
        mWindowManager =  mContext.getSystemService(WindowManager.class);
    }

    public void show() {
        QLog.VCLogD(TAG, "show() isShowing: " + isShowing);
        if (isShowing) return;
        mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        mWidth = mWindowManager.getCurrentWindowMetrics().getBounds().width();
        mHeigh = mWindowManager.getCurrentWindowMetrics().getBounds().height();
        int resid = mContext.getResources().getIdentifier(
                "navigation_bar_height", "dimen", "android");
        mNavigationBarHeight = mContext.getResources().getDimensionPixelSize(resid);
        mLayoutParams.gravity = Gravity.START | Gravity.TOP;
        mLayoutParams.x = 0;
        mLayoutParams.y = mHeigh/2;
        mRootView = LayoutInflater.from(mContext).inflate(R.layout.float_window_layout, null);
        mRootView.setBackground(AppCompatResources.getDrawable(mContext, R.drawable.float_window_cycle));
        mButton = mRootView.findViewById(R.id.float_button);
        mButton.setOnClickListener(v -> {
            startTranslation();
        });

        mWindowManager.addView(mRootView, mLayoutParams);
        isShowing = true;
        mButton.setOnTouchListener(new View.OnTouchListener() {
            private int mLastX, mLastY;
            private float mTouchStartX, mTouchStartY;
            private boolean mIsViewDragged = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mIsViewDragged = false;
                        mLastX = mLayoutParams.x;
                        mLastY = mLayoutParams.y;
                        mTouchStartX = event.getRawX();
                        mTouchStartY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!mIsViewDragged) {
                            if (Math.abs(event.getRawX() - mTouchStartX) > 5
                                    || Math.abs(event.getRawY() - mTouchStartY) > 5) {
                                mIsViewDragged = true;
                            }
                        }
                        if (mIsViewDragged) {
                            mLayoutParams.x = mLastX + (int) (event.getRawX() - mTouchStartX);
                            mLayoutParams.y = mLastY + (int) (event.getRawY() - mTouchStartY);
                            if (mLayoutParams.x <= 0) mLayoutParams.x = 0;
                            int maxX = mWidth - mButton.getWidth();
                            if (mLayoutParams.x >=  maxX) mLayoutParams.x = maxX;
                            if (mLayoutParams.y <=  0) mLayoutParams.y = 0;
                            int maxY = mHeigh - mButton.getHeight() - mNavigationBarHeight;
                            if (mLayoutParams.y >=  maxY) mLayoutParams.y = maxY;
                            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!mIsViewDragged) {
                            mButton.playSoundEffect(SoundEffectConstants.CLICK);
                            startTranslation();
                        } else {
                            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    public void hide() {
        QLog.VCLogD(TAG, "hide() isShowing: " + isShowing);
        if (isShowing) {
            mWindowManager.removeView(mRootView);
        }
        isShowing = false;
    }

    private void startTranslation() {
        VoiceCallTranslationService.enterTranslationMode(mContext, true);

        Intent activityIntent = new Intent();
        activityIntent.setClassName(mContext.getPackageName(), VoiceCallTranslationActivity.class.getName());
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(activityIntent);

    }
}
