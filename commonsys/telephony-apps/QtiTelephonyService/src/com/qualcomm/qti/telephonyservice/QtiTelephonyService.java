/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.telephonyservice;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Service created to handle communication between QCRIL and Audio Manager
 * IQcRilAudio HAL is registered as slot1/slot2
 */

public class QtiTelephonyService extends Service {

    private static final String TAG = "QtiTelephonyService";

    private List<IAudioControllerCallback> mAudioHals = new CopyOnWriteArrayList<>();
    private AudioController mAudioController = null;
    private Context mContext = null;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED)) {
                int activeModemCount = intent.getIntExtra(
                        TelephonyManager.EXTRA_ACTIVE_SIM_SUPPORTED_COUNT, 1);
                handleOnMultiSimConfigChanged(activeModemCount);
            }
        }
    };

    // Helper API that handles the transition for active modem count change
    private void handleOnMultiSimConfigChanged(int activeModemCount) {
        int prevModemCount = mAudioHals.size();
        if (prevModemCount == activeModemCount) {
            return;
        }
        if (activeModemCount > prevModemCount) {
            switchToMultiSim(prevModemCount, activeModemCount);
        } else {
            switchToSingleSim(prevModemCount, activeModemCount);
        }
    }

    // Creates new instances of QcRilAudio HAL for increased modem count
    private void switchToMultiSim(int prevModemCount, int activeModemCount) {
        for (int i = prevModemCount + 1; i <= activeModemCount; ++i) {
            mAudioHals.add(QcRilAudioHalFactory.newQcRilAudioHal(mContext, i, mAudioController));
        }
        // Update audio callbacks in AudioController
        mAudioController.updateAudioCallbacks(mAudioHals);
    }

    // Remove instances of QcRilAudio when decreasing the number of active modem
    private void switchToSingleSim(int prevModemCount, int activeModemCount) {
        for (int i = prevModemCount - 1; i >= activeModemCount; --i) {
            mAudioHals.remove(i);
        }
        // Update audio callbacks in AudioController
        mAudioController.updateAudioCallbacks(mAudioHals);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate:");
        mContext = getApplicationContext();

        UserManager userManager = mContext.getSystemService(UserManager.class);
        // Return if not a primary user
        if (!userManager.isSystemUser()) {
            Log.i(TAG, "Not a System user");
            return;
        }

        // RIL Audio HAL initialization.
        TelephonyManager tm  = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);

        // Creating callback for audio server status
        // TODO: Check if retry logic is required when creating AudioController throws an exception.
        try {
            mAudioController = new AudioController(mContext);
        } catch (Exception ex) {
            Log.e(TAG, "Exception: " + ex);
        }

        final int slotCount = tm.getActiveModemCount() + 1;
        for (int i = 1; i < slotCount; ++i) {
            mAudioHals.add(QcRilAudioHalFactory.newQcRilAudioHal(mContext, i, mAudioController));
        }

        // Update audio callbacks in AudioController
        mAudioController.updateAudioCallbacks(mAudioHals);

        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED),
                        Context.RECEIVER_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mAudioController != null) {
            mAudioController.dispose();
        }
        // RIL Audio HAL de-initialization
        for (IAudioControllerCallback h : mAudioHals) {
            h.onDispose();
        }
        mContext.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }
}
