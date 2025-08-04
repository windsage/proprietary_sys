/*
 * Copyright (c) 2020-2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class SoundWave extends View {

    private final int mTotalNum = 8;
    private final Paint mPaint;
    private final Random mRandom;
    private final ArrayList<Rect> mList = new ArrayList<>();
    private final ArrayList<Integer> mData = new ArrayList<>();
    private int mCurrentIndex;
    private int mMiddle;
    private int mStart;
    private boolean mIsAlreadyInit;

    public SoundWave(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Style.FILL);
        for (int i = 0; i < mTotalNum; i++) {
            mList.add(new Rect());
        }
        mRandom = new Random();
    }

    public void setData(int volume) {
        if (mMiddle <= 0) {
            return;
        }
        if (mCurrentIndex == mTotalNum) {
            mCurrentIndex = 0;
        }
        if (volume < 0 ) {
            volume = -volume;
        }
        volume = volume % mMiddle;
        mData.set(mCurrentIndex, volume);
        mCurrentIndex++;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mIsAlreadyInit) {
            mData.clear();
            int height = getHeight();
            mMiddle = height / 2;
            int width = getWidth();
            mStart = width / mTotalNum;
            for (int i = 0; i < mTotalNum; i++) {
                int value = mRandom.nextInt(mMiddle);
                mData.add(value);
            }
            mIsAlreadyInit = true;
        }
        for (int i = 0; i < mTotalNum; i++) {
            mList.get(i).set(mStart * i, mMiddle - mData.get(i),
                    mStart * i + 24, mMiddle + mData.get(i));
            canvas.drawRect(mList.get(i), mPaint);
        }
    }
}
