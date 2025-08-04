/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client.service.screensharing;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Arrays;

import androidx.annotation.NonNull;

public class WfdSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "WfdSurfaceView";
    private static final String SURFACE_PROP_KEY = "wfd_surface_prop";
    private Callback mCallback;
    private EventListener mEventListener;
    private boolean mCaptureEvents = false;

    public WfdSurfaceView(Context context, Callback callback,
                          EventListener eventListener) {
        super(context);
        mCallback = callback;
        mEventListener = eventListener;
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated() called");
        setFocusable(true);
        setFocusableInTouchMode(true);
        Log.d(TAG, "focus status:" + requestFocus());
        Log.d(TAG, "surfaceHolder.getSurface().isValid() is " +
                getHolder().getSurface().isValid());
        if (mCallback != null) {
            mCallback.surfaceCreated(getHolder().getSurface());
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged() called");
        Log.d(TAG, "width = " + width + " height = " + height);
        Configuration config = getResources().getConfiguration();
        int orientation = config.orientation;
        Bundle surfProp = new Bundle(1);
        int[] loc = new int[2];
        getLocationInWindow(loc);
        getLocationOnScreen(loc);
        Log.d(TAG, "Location on Screen: " + Arrays.toString(loc));
        Log.d(TAG, "Location in Window: " + Arrays.toString(loc));
        //Populate in L,T,R,B format
        int[] data = {loc[0], loc[1], loc[0] + width, loc[1] + height, orientation};
        surfProp.putIntArray(SURFACE_PROP_KEY, data);
        if (mCallback != null) {
            mCallback.surfaceChanged(surfProp);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed() called");
        if (mCallback != null) {
            mCallback.surfaceDestroyed();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent() called");
        if (mCaptureEvents && mEventListener != null) {
            Log.d(TAG, "onTouchEvent()- capturing events");
            return mEventListener.sendEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp() called with keycode: " + event);
        if (mCaptureEvents && !dropKey(keyCode) && mEventListener != null) {
            Log.d(TAG, "onKeyUp() called while capturing events with keycode: " + keyCode);
            mEventListener.sendEvent(event);
        }

        //allow other key receivers to handle the event
        //based on the keyCode
        return dontHandleKeyFramework(keyCode);
    }

    private boolean dropKey(int keyCode) {
        Log.e(TAG, "dropkey called with keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_SLEEP:
            case KeyEvent.KEYCODE_WAKEUP:
            case KeyEvent.KEYCODE_CTRL_LEFT:
            case KeyEvent.KEYCODE_CTRL_RIGHT:
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return true;
        }
        return false;
    }

    private boolean dontHandleKeyFramework(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, "onKeyDown() called with keycode: " + event);
        if (mCaptureEvents && !dropKey(keyCode) && mEventListener != null) {
            mEventListener.sendEvent(event);
        }
        //allow other key receivers to handle the event
        //based on the keycode
        return dontHandleKeyFramework(keyCode);
    }

    public interface EventListener {
        boolean sendEvent(InputEvent event);
    }

    public interface Callback {
        void surfaceCreated(Surface surface);
        void surfaceChanged(Bundle surfProp);
        void surfaceDestroyed();
    }

    public boolean startUIBCEventCapture() {
        mCaptureEvents = true;
        return true;
    }

    public boolean stopUIBCEventCapture() {
        mCaptureEvents = false;
        return true;
    }
}
