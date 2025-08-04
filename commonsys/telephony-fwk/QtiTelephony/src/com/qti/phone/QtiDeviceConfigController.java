/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.telephony.UiccSlotMapping;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ProgressBar;

import com.android.internal.telephony.TelephonyIntents;
import com.qti.extphone.ExtTelephonyManager;

import java.util.ArrayList;
import java.util.List;

public class QtiDeviceConfigController {
    private static final String LOG_TAG = "QtiDeviceConfigController";

    private Context mContext;
    private Handler mHandler;
    private TelephonyManager mTm = null;

    private int mPhoneCount;
    private int mCurrentAvailableSimCount = 0;
    private int mDeviceConfigState = STATE_IDLE;
    private int mSwitchStatusCheckRetries;
    private int mSwitchSlotFailedRetries;

    private Dialog mProgressDialog = null;

    private int mDefaultTimer = 2*60*1000; // 2 Minutes
    private int mFeatureConfig;

    /* Event Constants */
    private static final int EVENT_PROCESS_DEVICE_CONFIG_REQ = 1;
    private static final int EVENT_CALL_STATE_ECBM_SCBM_IND = 2;
    private static final int EVENT_MULTI_SIM_CONFIG_CHANGED = 3;
    private static final int EVENT_SLOT_SWITCH_IN_PROGRESS = 4;
    private static final int EVENT_ONGOING_REQUEST_TIME_OUT = 5;

    private static final int STATE_IDLE            = 0;
    private static final int STATE_SWITCH_DELAYED = 1;
    private static final int STATE_SWITCH_CONFIG_IN_PROGRESS = 2;
    private static final int STATE_SWITCH_SLOT_IN_PROGRESS = 3;
    private static final int STATE_SWITCH_SLOT_COMPLETED = 4;
    private static final int STATE_SWITCH_CONFIG_PENDING = 5;

    private static final int CONFIG_FULL = 1;
    private static final int CONFIG_SIM_REMOVAL_ONLY = 2;
    private static final int CONFIG_SWITCH_FOR_SECOND_SLOT_ONLY = 3;

    private static final int SINGLE_SIM = 1;
    private static final int DUAL_SIM = 2;

    // Below constants are to represent the Physical slot numbers on Device
    private static final int PHY_SLOT_ID_ONE = 0;
    private static final int PHY_SLOT_ID_TWO = 1;

    private static final int SIM_COUNT_ZERO = 0;
    private static final int SIM_COUNT_ONE = 1;
    private static final int SIM_COUNT_TWO = 2;
    private static final int MAX_SLOT_SWITCH_STATUS_CHECK_RETRY_COUNT = 10;

    private static final int PROGRESS_DIALOG_TIMEOUT_MILLIS = 50000;
    private static final int DELAY_TIMEOUT_MILLIS = 2000;
    private static final int SWITCH_SLOT_PROGRESS_CHECK_TIMEOUT = 4000;
    private static final int DSDS_TO_SS_SWITCH_MIN_TIMER = 30000; // 30 seconds

    public QtiDeviceConfigController(Context context, int featureConfig, int timer) {
        mContext = context;

        // Restrict the minimum timer value to 30sec
        if (timer >= DSDS_TO_SS_SWITCH_MIN_TIMER) {
            mDefaultTimer = timer;
        }
        mFeatureConfig = featureConfig;

        TelephonyManager tm = getTelephonyManager();
        if (tm != null) {
            mPhoneCount = tm.getActiveModemCount();
        } else {
            loge("Telephony Manager is null");
        }

        HandlerThread handlerThread = new HandlerThread(LOG_TAG);
        handlerThread.start();
        mHandler = new QtiDeviceConfigHandler(handlerThread.getLooper());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED);
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        intentFilter.addAction(ExtTelephonyManager.ACTION_SMS_CALLBACK_MODE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);

        // Invoke device config request, this is to handle qti phone crash scenarios.
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(EVENT_PROCESS_DEVICE_CONFIG_REQ), DELAY_TIMEOUT_MILLIS);

        logd("constructor " + mPhoneCount + " timer=" + timer + " config=" + mFeatureConfig);
    }

    public void destroy() {
        logi("destroy");
        mContext.unregisterReceiver(mBroadcastReceiver);
        mHandler.getLooper().quitSafely();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logd("received " + action);
            if (TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED.equals(action)) {
                mHandler.obtainMessage(EVENT_PROCESS_DEVICE_CONFIG_REQ).sendToTarget();
            } else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action) ||
                    TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED.equals(action) ||
                    ExtTelephonyManager.ACTION_SMS_CALLBACK_MODE_CHANGED.equals(action)) {
                mHandler.obtainMessage(EVENT_CALL_STATE_ECBM_SCBM_IND).sendToTarget();
            }
        }
    };

    private final class QtiDeviceConfigHandler extends Handler {
        QtiDeviceConfigHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            logd("message " + msg.what);
            switch (msg.what) {
                case EVENT_PROCESS_DEVICE_CONFIG_REQ: {
                    performDeviceConfigSwitchIfRequired();
                    break;
                }

                case EVENT_CALL_STATE_ECBM_SCBM_IND: {
                    logd("Is device config request pending=" + mDeviceConfigState);
                    if (mDeviceConfigState == STATE_SWITCH_CONFIG_PENDING) {
                        mHandler.sendMessageDelayed(
                                mHandler.obtainMessage(EVENT_PROCESS_DEVICE_CONFIG_REQ),
                                DELAY_TIMEOUT_MILLIS);
                    }
                    break;
                }

                case EVENT_MULTI_SIM_CONFIG_CHANGED: {
                    handleMultiSimConfigChanged(msg.arg1);
                    break;
                }

                case EVENT_SLOT_SWITCH_IN_PROGRESS: {
                    checkWhetherSlotSwitchOperationCompleted();
                    break;
                }

                case EVENT_ONGOING_REQUEST_TIME_OUT: {
                    // Its expected to complete the operation in 50 seconds, if the
                    // operation not completed in 50 seconds, dismiss the progress dialog
                    // and re-try it after default configured time-out
                    mDeviceConfigState = STATE_IDLE;
                    dismissProgressBar();
                    performDeviceConfigSwitchIfRequired();
                    break;
                }

                default: {
                    logd("invalid message " + msg.what);
                    break;
                }
            }
        }
    }

    private void checkWhetherSlotSwitchOperationCompleted() {
        UiccSlotInfo[] slotsInfo = getUiccSlotsInfo();
        if (slotsInfo == null) {
            loge("slotsInfo is null");
            return;
        }

        mSwitchStatusCheckRetries++;
        int availableSimCount = getCountOfSimsPresentOnDevice(slotsInfo);

        logi("availableSimCount=" + availableSimCount + " retry=" + mSwitchStatusCheckRetries);
        if ((availableSimCount == mCurrentAvailableSimCount) ||
                (mSwitchStatusCheckRetries == MAX_SLOT_SWITCH_STATUS_CHECK_RETRY_COUNT)) {
            mDeviceConfigState = STATE_SWITCH_SLOT_COMPLETED;
            mSwitchStatusCheckRetries = 0;
            mSwitchSlotFailedRetries = 0;
            performDeviceConfigSwitchIfRequired();
            if (mDeviceConfigState == STATE_IDLE) dismissProgressBar();
        } else {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_SLOT_SWITCH_IN_PROGRESS),
                    SWITCH_SLOT_PROGRESS_CHECK_TIMEOUT);
        }
    }

    private UiccSlotInfo[] getUiccSlotsInfo() {
        UiccSlotInfo[] slotsInfo = null;
        TelephonyManager tm = getTelephonyManager();
        if (tm != null) {
            slotsInfo = tm.getUiccSlotsInfo();
        }
        return slotsInfo;
    }

    /**
     * Listen for slot status info, calculate number of SIMs with present state,
     * If the present SIM count is not equal to the current Phone count, change the device
     * configuration to either DSDS or Single SIM
     */
    private void performDeviceConfigSwitchIfRequired() {
        if (mDeviceConfigState == STATE_SWITCH_SLOT_IN_PROGRESS) {
            loge("switch slot in progress, ignore ");
            return;
        }
        UiccSlotInfo[] slotsInfo = getUiccSlotsInfo();
        if (slotsInfo == null) {
            loge("null slotInfo ");
            return;
        }

        logd("slotsInfo length " + slotsInfo.length + " device config state=" + mDeviceConfigState);

        boolean areSlotsSwitched = isDeviceSlotsCrossMapped(slotsInfo);
        int presentSimPhysicalSlotId = 0;

        mCurrentAvailableSimCount = getCountOfSimsPresentOnDevice(slotsInfo);
        if (mCurrentAvailableSimCount == SIM_COUNT_ONE) {
            presentSimPhysicalSlotId = getPresentPhysicalSlotId(slotsInfo);
        }

        logd("phone count=" + mPhoneCount + " No of SIMs present=" + mCurrentAvailableSimCount +
                " areSlotsSwitched=" + areSlotsSwitched + " slotId=" + presentSimPhysicalSlotId);

        // 1. If number of SIMs present is ZERO, let the device be in previous configuration.
        // 2. Do not take any action if number of SIMs present is 2 and device config is DSDS
        //    with slots in normal fashion.
        if ((mCurrentAvailableSimCount == SIM_COUNT_ZERO) ||
                ((mCurrentAvailableSimCount == SIM_COUNT_TWO &&
                isDeviceInDsds()) && !areSlotsSwitched)) {
            // Ignore, two SIMs not present  (OR) device is in DSDS with slots in normal fashion.
            mDeviceConfigState = STATE_IDLE; // RESET the state
            logd("Nothing to be done, ignore");
            return;
        } else if ((mDeviceConfigState == STATE_IDLE) &&
                (mCurrentAvailableSimCount == SIM_COUNT_ONE) && isDeviceInDsds()) {
            // Device configuration can be switched to SS as the avilable SIM count is 1 and
            // Current device configuration is DSDS. Send delayed event to process it after timeout
            logd("Send delayed event");
            mDeviceConfigState = STATE_SWITCH_DELAYED;
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(EVENT_PROCESS_DEVICE_CONFIG_REQ), mDefaultTimer);
            return;
        } else if ((mDeviceConfigState == STATE_SWITCH_DELAYED) &&
                mHandler.hasMessages(EVENT_PROCESS_DEVICE_CONFIG_REQ)) {
            logd("Message already in queue, ignore");
            return;
        }

        // Check if voice call active on any SIM or device is in SCBM/ECBM, if any of these
        // is active, wait for them to end/exit
        if (isDeviceIdle()) {
            // If earlier switch slot in progress and requested mapping not
            // achieved wait for it to complete.

            // If switch slot not performed proceed with device config switch
            if (!invokeSwitchSlotIfRequired(presentSimPhysicalSlotId, areSlotsSwitched)) {
                // If there is no switch slot operation needed, send switch device config request
                switchDeviceConfig();
            }
        } else {
            mDeviceConfigState = STATE_SWITCH_CONFIG_PENDING;
        }
    }

    private boolean invokeSwitchSlotIfRequired(
            int presentSimPhysicalSlotId, boolean areSlotsSwitched) {
        boolean switchSlotToBeInvoked = false;
        boolean isNormalMapNeeded = false;

        if (!isSwitchSlotEnabled()) {
            logd("invokeSwitchSlotIfRequired, slot switch config disabled");
            return switchSlotToBeInvoked;
        }

        if ((mCurrentAvailableSimCount == SIM_COUNT_ONE) &&
                (presentSimPhysicalSlotId == PHY_SLOT_ID_TWO) && !areSlotsSwitched) {
            // If there is only one SIM present in second slot, slots have to be switched.
            switchSlotToBeInvoked = true;
        } else if ((mCurrentAvailableSimCount == SIM_COUNT_TWO ||
                presentSimPhysicalSlotId == PHY_SLOT_ID_ONE) && areSlotsSwitched) {
            // If two SIMs present (or) only one SIM present in first slot, make normal slot mapping
            // if its in cross mapped fashion.
            isNormalMapNeeded = true;
            switchSlotToBeInvoked = true;
        }

        if (switchSlotToBeInvoked && sendSwitchSlotRequest(isNormalMapNeeded)) {
            mDeviceConfigState = STATE_SWITCH_SLOT_IN_PROGRESS;
            // Once slot switch invoked, start a timer and wait until all the current PRESENT SIM
            // cards to UP after slot switch operation completed
            mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_SLOT_SWITCH_IN_PROGRESS),
                    SWITCH_SLOT_PROGRESS_CHECK_TIMEOUT);
        }
        return switchSlotToBeInvoked;
    }

    private boolean sendSwitchSlotRequest(boolean isNormalMapNeeded) {
        logi("sendSwitchSlotRequest, " + mDeviceConfigState);
        TelephonyManager tm = getTelephonyManager();
        UiccSlotInfo[] slotsInfo = getUiccSlotsInfo();
        if (slotsInfo == null || tm == null) {
            loge("sendSwitchSlotRequest, slotInfo null, " + mDeviceConfigState);
            return false;
        }

        List<UiccSlotMapping> slotMappingList = new ArrayList<UiccSlotMapping>();
        int physicalSlotId = 0;

        for (int index = 0; index < slotsInfo.length; index++) {
            // FIXME MEP support to be added, set proper portId
            if (isNormalMapNeeded) {
                physicalSlotId = index;
                slotMappingList.add(new UiccSlotMapping(0/* portId */, physicalSlotId, index));
            } else {
                physicalSlotId = (index == 0) ? PHY_SLOT_ID_TWO : PHY_SLOT_ID_ONE;
                if (slotsInfo[index] == null) {
                    logi("UiccSlotInfo is null for index = " + index);
                    mDeviceConfigState = STATE_IDLE;
                    return false;
                }
                slotMappingList.add(new UiccSlotMapping(0/* portId */, physicalSlotId,
                        slotsInfo[index].getPorts().stream().
                        findFirst().get().getLogicalSlotIndex()));
            }
            logi("sendSwitchSlotRequest, physical slot=" + physicalSlotId + " logical slotId=" +
                    slotsInfo[index].getPorts().stream().findFirst().get().getLogicalSlotIndex());
        }

        showProgressBar();
        try {
            tm.setSimSlotMapping(slotMappingList);
        } catch (IllegalStateException e) {
            loge("sendSwitchSlotRequest failed with " + e);
            handleSwitchSlotException();
            return false;
        }
        return true;
    }

    private void handleSwitchSlotException() {
        if (mSwitchSlotFailedRetries < MAX_SLOT_SWITCH_STATUS_CHECK_RETRY_COUNT) {
            mSwitchSlotFailedRetries++;
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(EVENT_PROCESS_DEVICE_CONFIG_REQ),
                    SWITCH_SLOT_PROGRESS_CHECK_TIMEOUT);
        }
    }

    private void switchDeviceConfig() {
        int configToSwitch = -1;

        logd("switchDeviceConfig, sim count="
                + mCurrentAvailableSimCount + " phone count=" + mPhoneCount);

        if (mCurrentAvailableSimCount == SIM_COUNT_TWO && isDeviceInSingleSim()) {
            configToSwitch = DUAL_SIM;
        } else if (mCurrentAvailableSimCount == SIM_COUNT_ONE && isDeviceInDsds()) {
            configToSwitch = SINGLE_SIM;
        }

        if (configToSwitch != -1) {
            TelephonyManager tm = getTelephonyManager();
            if (tm == null) {
                loge("TelephonyManager is null");
                return;
            }

            mDeviceConfigState = STATE_SWITCH_CONFIG_IN_PROGRESS;
            tm.switchMultiSimConfig(configToSwitch);
            showProgressBar();
        } else {
            // Reset config state if there is no need to switch the configuration
            mDeviceConfigState = STATE_IDLE;
        }
    }

    void onMultiSimConfigChanged(int newCount) {
        logd("onMultiSimConfigChanged, current phone count=" +
                mPhoneCount + " new count=" + newCount);
        mHandler.obtainMessage(EVENT_MULTI_SIM_CONFIG_CHANGED, newCount, -1).sendToTarget();
    }

    private void handleMultiSimConfigChanged(int newPhoneCount) {
        mPhoneCount = newPhoneCount;

        if (mDeviceConfigState == STATE_SWITCH_CONFIG_IN_PROGRESS) {
            // If the received device configuration is not matching with current-available sim count
            // send device config request to switch config. This may happen, if earlier request
            // failed OR if device configuration switched via other mechanism.
            if (mCurrentAvailableSimCount != newPhoneCount) {
                mHandler.obtainMessage(EVENT_PROCESS_DEVICE_CONFIG_REQ).sendToTarget();
            } else {
                mDeviceConfigState = STATE_IDLE;
                dismissProgressBar();
            }
            logd("slotsInfo length " + mCurrentAvailableSimCount + " phone count " + mPhoneCount);
        }
    }

    private void showProgressBar() {
        logi(" display progress dialog ");
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            logi(" Progress dialog already shown ");
            return;
        }

        CharSequence dialogTitle =
                mContext.getResources().getString(R.string.progress_dialog_title);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View dialogTitleView = inflater.inflate(R.layout.progress_dialog, null);
        final TextView titleText = dialogTitleView.findViewById(R.id.summary);
        titleText.setText(dialogTitle);

        final ProgressBar progressBar = new ProgressBar(mContext);
        progressBar.setIndeterminate(true);

        mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_ONGOING_REQUEST_TIME_OUT),
                PROGRESS_DIALOG_TIMEOUT_MILLIS);

        mProgressDialog = new AlertDialog.Builder(mContext)
                .setCustomTitle(dialogTitleView)
                .setView(progressBar)
                .setCancelable(false)
                .create();
        mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        mProgressDialog.show();
    }

    private void dismissProgressBar() {
        logi(" dismiss progress dialog " + mProgressDialog);
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
            mHandler.removeMessages(EVENT_ONGOING_REQUEST_TIME_OUT);
        }
    }

    private boolean isDeviceIdle() {
        if (!isDeviceInCall() && !isDeviceInEcbm() && !isDeviceinScbm()) {
            return true;
        }
        return false;
    }

    private boolean isDeviceInCall() {
        TelecomManager tlcMgr = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        boolean callState = tlcMgr.isInCall();
        logd("isDeviceInCall=" + callState);
        return callState;
    }

    private boolean isDeviceInEcbm() {
        boolean isEcbmEnabled = false;
        SubscriptionManager subMgr = (SubscriptionManager)
                mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subMgr == null) return false;

        subMgr = subMgr.createForAllUserProfiles();

        List<SubscriptionInfo> subInfoList = subMgr.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                // If ECBM enabled on first sub no need to look for other sub
                if (isEcbmEnabled) {
                    break;
                }

                TelephonyManager telMgr = mContext.getSystemService(TelephonyManager.class).
                        createForSubscriptionId(subInfo.getSubscriptionId());
                isEcbmEnabled = telMgr.getEmergencyCallbackMode();
            }
        }
        logd("isDeviceInEcbm=" + isEcbmEnabled);
        return isEcbmEnabled;
    }

    private boolean isDeviceinScbm() {
        boolean isScbmEnabled = QtiRadioUtils.isScbmEnabled();;
        logd("isDeviceinScbm=" + isScbmEnabled);
        return isScbmEnabled;
    }

    private boolean isDeviceSlotsCrossMapped(UiccSlotInfo[] slotsInfo) {
        boolean slotsSwitched = false;

        for (int index = 0; index < slotsInfo.length; index++) {
            UiccSlotInfo slotInfo = slotsInfo[index];
            if (slotInfo == null) {
                logi("slotInfo null, index " + index);
                continue;
            }

            // FIXME MEP support to be added ??
            // FIXME TSDS cases to be handled ??
            if ((slotInfo.getPorts().size() == 1) &&
                    slotInfo.getPorts().stream().findFirst().get().getLogicalSlotIndex() != index) {
                // If logical slot index is not same as Physical slot index,
                // slots might be in flex map fashion
                slotsSwitched = true;
            }
            logv("physical slot=" + index + " logical slotId=" +
                    slotInfo.getPorts().stream().findFirst().get().getLogicalSlotIndex());
        }
        return slotsSwitched;
    }

    private int getCountOfSimsPresentOnDevice(UiccSlotInfo[] slotsInfo) {
        int simCount = 0;

        for (int index = 0; index < slotsInfo.length; index++) {
            UiccSlotInfo slotInfo = slotsInfo[index];
            if (slotInfo == null) {
                logi("slotInfo null, index " + index);
                continue;
            }
            if (slotInfo.getCardStateInfo() == UiccSlotInfo.CARD_STATE_INFO_PRESENT) {
               simCount++;
            }
        }
        return simCount;
    }

    // Utility returns the Physical SlotId of the SIM card that is PRESENT on device.
    private int getPresentPhysicalSlotId(UiccSlotInfo[] slotsInfo) {
        int physicalSlotId = -1;

        for (int index = 0; index < slotsInfo.length; index++) {
            UiccSlotInfo slotInfo = slotsInfo[index];
            if (slotInfo == null) {
                logi("slotInfo null, index " + index);
                continue;
            }
            if (slotInfo.getCardStateInfo() == UiccSlotInfo.CARD_STATE_INFO_PRESENT) {
               physicalSlotId = index;
            }
        }
        return physicalSlotId;
    }

    private TelephonyManager getTelephonyManager() {
        if (mTm == null) {
            mTm = (TelephonyManager) mContext.
                    getSystemService(Context.TELEPHONY_SERVICE);
        }
        return mTm;
    }

    private boolean isDeviceInDsds() {
        if (mPhoneCount == SIM_COUNT_TWO) {
            return true;
        }
        return false;
    }

    private boolean isDeviceInSingleSim() {
        if (mPhoneCount == SIM_COUNT_ONE) {
            return true;
        }
        return false;
    }

    private void logv(String string) {
        Log.v(LOG_TAG, string);
    }

    private void logd(String string) {
        Log.d(LOG_TAG, string);
    }

    private void logi(String string) {
        Log.i(LOG_TAG, string);
    }

    private void loge(String string) {
        Log.e(LOG_TAG, string);
    }

    private boolean isSwitchSlotEnabled() {
        if (mFeatureConfig < CONFIG_SWITCH_FOR_SECOND_SLOT_ONLY) {
            return true;
        }
        return false;
    }
}
