/*
 * Copyright (c) 2021, 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.subsidylock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class SubsidyDeviceController {

    private static final String TAG = "SubsidyDeviceController";
    private static final int VALUE_ZERO = 0;
    private static final int VALUE_ONE = 1;
    private static final int VALUE_TWO = 2;
    private static final int DEFAULT_SLOT_INDEX = 0;

    private static final int EVENT_SIM_STATES_AVAILABLE = 1;
    private static final int EVENT_DDS_CHANGE = 2;
    private static final int EVENT_PRIMARYCARD_RETRY = 3;

    private static final String EXTRA_STATE = "state";
    private static final String ACTION_RADIO_POWER_STATE_CHANGED =
            "org.codeaurora.intent.action.RADIO_POWER_STATE";

    private Context mContext;
    private HandlerThread mSubsidyDevHandlerThread;
    private Handler mSubsidyDevHandler;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private int mNumPhones;
    private int mRadioState[];
    private boolean mRetrySettingPrimaryCard = false;

    public SubsidyDeviceController(Context context) {
        Log.d(TAG, "Started SubsidyDeviceController");

        mSubsidyDevHandlerThread = new HandlerThread("SubsidyDeviceController");
        mSubsidyDevHandlerThread.start();
        mSubsidyDevHandler = new SubsidyDeviceHandler(mSubsidyDevHandlerThread.getLooper());

        mContext = context;
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mNumPhones = mTelephonyManager.getActiveModemCount();
        mRadioState = new int[mNumPhones];

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
        intentFilter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        intentFilter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        intentFilter.addAction(ACTION_RADIO_POWER_STATE_CHANGED);
        context.registerReceiver(mBroadcastReceiver, intentFilter);

        handleSimStates();
    }

    private void handleSimStates() {
        for (int slotId = 0; slotId < mNumPhones; slotId++) {
            handleSimStateChanged(mTelephonyManager.getSimState(slotId), slotId);
        }
    }

    // Primary card logic for subsidy feature is triggered based on Sim Ready,
    // absent, not ready, loaded state.
    private boolean isPreferredSimState(int simState) {
       return (simState == TelephonyManager.SIM_STATE_READY
           || simState == TelephonyManager.SIM_STATE_LOADED
           || simState == TelephonyManager.SIM_STATE_NOT_READY
           || simState == TelephonyManager.SIM_STATE_ABSENT);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive: " + action);

            if (TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED.equals(action)
                    || TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED.equals(action)) {
                int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
                int slotId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);

                handleSimStateChanged(simState, slotId);
            } else if (TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                mSubsidyDevHandler.sendMessage(
                    mSubsidyDevHandler.obtainMessage(EVENT_DDS_CHANGE, intent.getIntExtra(
                    "subscription", SubscriptionManager.INVALID_SUBSCRIPTION_ID), 0));
            } else if (ACTION_RADIO_POWER_STATE_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                int radioState = intent.getIntExtra(EXTRA_STATE,
                        TelephonyManager.RADIO_POWER_UNAVAILABLE);

                if (SubsidyLockUtils.isValidSlotId(mContext, slotId)) {
                    mRadioState[slotId] = radioState;
                }

                if (isRadioOnForAllSlots() && !isAirplaneModeOn() && mRetrySettingPrimaryCard) {
                    mSubsidyDevHandler.sendMessage(
                            mSubsidyDevHandler.obtainMessage(EVENT_PRIMARYCARD_RETRY));
                    mRetrySettingPrimaryCard = false;
                }
            }
        }
    };

    private boolean isRadioOnForAllSlots() {
        for (int slotId = 0; slotId < mNumPhones; slotId++) {
            if (mRadioState[slotId] != TelephonyManager.RADIO_POWER_ON) {
                return false;
            }
        }
        return true;
    }

    private final class SubsidyDeviceHandler extends Handler {
        SubsidyDeviceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage:" + msg.what);
            switch(msg.what) {
                case EVENT_SIM_STATES_AVAILABLE:
                case EVENT_PRIMARYCARD_RETRY:
                    setPrimaryCardIfRequired();
                    break;

                case EVENT_DDS_CHANGE:
                    decideAndSetPrimarySlot(true /*ddsChanged*/);
                    break;
            }
        }
    }

    private void setPrimaryCardIfRequired() {
        Log.i(TAG, "setPrimaryCardIfRequired");
        if (SubsidyLockUtils.didSimCardsChanged(mContext)) {
            decideAndSetPrimarySlot(false /*ddsChanged*/);
        }
    }

    private void handleSimStateChanged(int simState, int slotId) {
        if (!SubsidyLockUtils.isValidSlotId(mContext, slotId)) {
            Log.e(TAG, "handleSimStateChanged: invalid slot index: " + slotId);
            return;
        }

        if (isPreferredSimState(simState)) {
            mSubsidyDevHandler.sendMessage(
                    mSubsidyDevHandler.obtainMessage(EVENT_SIM_STATES_AVAILABLE));
        }
    }

    private void decideAndSetPrimarySlot(boolean ddsChanged) {
        int primarySimCount = 0;
        int primarySimSlotId = -1;

        Log.i(TAG, "decideAndSetPrimarySlot");

        if (!isRadioOnForAllSlots()) {
            if (isAirplaneModeOn()) mRetrySettingPrimaryCard = true;

            Log.i(TAG, "Radio is not Powered On, mRetrySettingPrimaryCard is "
                    + mRetrySettingPrimaryCard);
            return;
        }

        for (int slotId = 0; slotId < mNumPhones; slotId++) {
            if (SubsidyLockUtils.isPrimaryCapableSimCard(mContext, slotId)) {
                primarySimCount++;
                primarySimSlotId = slotId;
            }
        }

        Log.i(TAG, "primarySimCount: " + primarySimCount + " ddsChanged: " + ddsChanged);

        switch(primarySimCount) {
            case VALUE_TWO:
                // No need to handle DDS change, if both sim cards inserted are primary type.
                if (!ddsChanged) {
                    SubsidyLockUtils.setPrimaryCardOnSlot(mContext, DEFAULT_SLOT_INDEX);
                    showSubsidyCardSelectionPopup(DEFAULT_SLOT_INDEX);
                }
                break;

            case VALUE_ONE:
                SubsidyLockUtils.setPrimaryCardOnSlot(mContext, primarySimSlotId);
                break;

            case VALUE_ZERO:
                SubsidyLockUtils.saveIccIdMccMncOfSimCardsInSP(mContext);

                // When no primary sim cards are there, we don't want DDS to
                // be set to non-primary sim, so set it to invalid subId.
                if (mSubscriptionManager.getDefaultDataSubscriptionId() !=
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    mSubscriptionManager.setDefaultDataSubId
                            (SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                }
                break;

        }
        SubsidyLockUtils.setAllowUsertoSetDDS(mContext, primarySimCount == VALUE_TWO);
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * Shows a dialog showing the list of subscriptions, and the current primary card.
     * The user can change the primary card in this dialog.
     */
    private void showSubsidyCardSelectionPopup(int defaultSlotId) {
        Log.i(TAG, "showing subdisy cards selection popup, defaultSlotId: "
                + defaultSlotId);

        Intent intent = new Intent(mContext, SubsidyCardSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        intent.putExtra(SubsidyLockUtils.SUBSIDY_PRIMARY_SLOT, defaultSlotId);

        mContext.startActivity(intent);
    }

    public void dispose() {
        Log.i(TAG, "dispose");
        try {
            mContext.unregisterReceiver(mBroadcastReceiver);
            if (mSubsidyDevHandlerThread != null) {
                mSubsidyDevHandlerThread.quitSafely();
                mSubsidyDevHandlerThread = null;
                mSubsidyDevHandler = null;
            }
        } catch (RuntimeException ex) {
            Log.e(TAG, "dispose: Exception: ", ex);
        }
    }
}