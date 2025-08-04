/* Copyright (c) 2015-2016, 2019, 2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.ims.vt;

import android.content.Context;
import android.util.Log;

import org.codeaurora.ims.ImsCallSessionImpl;
import org.codeaurora.ims.ImsServiceSub;
import org.codeaurora.ims.ImsSubController;

import java.util.List;

// TODO Maybe rename to ImsGlobals..
public class ImsVideoGlobals implements ImsSubController.OnMultiSimConfigChanged {
    private static String TAG = "VideoCall_ImsVideoGlobals";

    private static ImsVideoGlobals sInstance = null;
    private List<ImsServiceSub> mServiceSubs;
    private CameraController mCameraController;
    private MediaController mMediaController;
    private LowBatteryHandler mLowBatteryHandler;

    private Context mContext;

    public static void init(List<ImsServiceSub> serviceSubArr, Context context) {
        if (serviceSubArr == null || serviceSubArr.isEmpty() || serviceSubArr.get(0) == null) {
            throw new IllegalArgumentException("Default subscription is null.");
        }

        if (sInstance == null) {
            sInstance = new ImsVideoGlobals(serviceSubArr, context);
        } else {
            throw new RuntimeException("ImsVideoGlobals: Multiple initializaiton.");
        }
    }

    public static ImsVideoGlobals getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("ImsVideoGlobals: getInstance called before init.");
        }
        return sInstance;
    }

    public void dispose() {
        log("dispose()");
        for (ImsServiceSub sub: mServiceSubs) {
            sub.removeListener(mMediaController);
            sub.removeListener(mLowBatteryHandler);
        }
        mCameraController.dispose();
        mMediaController.dispose();
        mLowBatteryHandler.dispose();
        sInstance = null;
    }

    private ImsVideoGlobals(List<ImsServiceSub> serviceSubs, Context context) {
        mServiceSubs = serviceSubs;

        mContext = context;
        CameraController.init(context, ImsMedia.getInstance());
        MediaController.init(context, ImsMedia.getInstance());
        LowBatteryHandler.init(mServiceSubs, context);

        mCameraController = CameraController.getInstance();
        mMediaController = MediaController.getInstance();
        mLowBatteryHandler = LowBatteryHandler.getInstance();
        for (ImsServiceSub sub : mServiceSubs) {
            sub.addListener(mMediaController);
            sub.addListener(mLowBatteryHandler);
        }
    }

    private ImsVideoCallProviderImpl getImsVideoCallProviderImpl(ImsCallSessionImpl session) {
        return session == null ? null : session.getImsVideoCallProviderImpl();
    }

    /* package */
    ImsCallSessionImpl findSessionByMediaId(int mediaId) {
        ImsCallSessionImpl session = null;
        for (ImsServiceSub serviceSub : mServiceSubs) {
            session = serviceSub.findSessionByMediaId(mediaId);
            if (session != null) return session;
        }
        return session;
    }

    /* package */
    ImsVideoCallProviderImpl findVideoCallProviderbyMediaId(int mediaId) {
        return getImsVideoCallProviderImpl(findSessionByMediaId(mediaId));
    }

    /* package */
    CameraController getCameraController() { return mCameraController; }

    /* package */
    MediaController getMediaController() { return mMediaController; }

    @Override
    public void onMultiSimConfigChanged(int prevSimCount, int activeModemCount) {
        if (prevSimCount == activeModemCount) {
            return;
        }
        // Adds a listener for ImsServiceSub in the case of SS -> DSDS
        // In the case of DSDS -> SS, the second ImsServiceSub is disposed
        for (int i = prevSimCount; i < activeModemCount; ++i) {
            mServiceSubs.get(i).addListener(mMediaController);
            mServiceSubs.get(i).addListener(mLowBatteryHandler);
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
