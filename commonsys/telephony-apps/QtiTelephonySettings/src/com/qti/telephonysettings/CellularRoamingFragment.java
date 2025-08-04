/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings;

import static android.telephony.TelephonyManager.CALL_STATE_IDLE;

import static com.qti.extphone.CellularRoamingPreference.DISABLED;
import static com.qti.extphone.CellularRoamingPreference.ENABLED;
import static com.qti.extphone.CellularRoamingPreference.INVALID;
import static com.qti.extphone.ExtPhoneCallbackListener.EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

public class CellularRoamingFragment extends Fragment implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "CellularRoamingFragment";
    private static final String SYNC_STATE_CACHE_KEY = "CELLULAR_ROAMING_SYNC_KEY";
    private static final String TELEPHONY_STATE_CACHE_KEY = "CELLULAR_ROAMING_STATE_KEY";
    private static final int TRUE = ENABLED;
    private static final int FALSE = DISABLED;
    private static final int MAX_ACTIVE_MODEM_COUNT = 2;

    private View mView;
    private Context mContext;
    private int[] mCallState = {CALL_STATE_IDLE, CALL_STATE_IDLE};
    private boolean mServiceConnected = false;
    private Client mClient;
    private ExtTelephonyManager mExtTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mSubscriptionChangeListener;
    private int mActiveModemCount;
    private final ArrayMap<Integer, Switch> mSwitchMap = new ArrayMap<>();
    private final ArrayMap<Integer, CallStateListener> mCallStateListeners = new ArrayMap<>();
    private static String sPackageName;

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "ExtTelephonyService connected");
            mServiceConnected = true;
            int[] events = new int[] {EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(sPackageName,
                    mExtPhoneCallbackListener, events);
            Log.d(TAG, "Client = " + mClient);
            initUI();
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "ExtTelephonyService disconnected");
            if (mServiceConnected) {
                mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
                mServiceConnected = false;
                mClient = null;
            }
        }
    };

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener(
            Looper.getMainLooper()) {
        @Override
        public void setCellularRoamingPreferenceResponse(int slotId, Token token, Status status)
                throws RemoteException {
            Log.d(TAG, "setCellularRoamingPreferenceResponse: slotId = " + slotId + ", token = " +
                    token + ", status = " + status.get());
            mHandler.sendMessage(mHandler.obtainMessage(
                    EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE,
                    new Result(slotId, status, null)));
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Result result = (Result) msg.obj;
            switch (msg.what) {
                case EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE:
                    Log.d(TAG, "EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE");
                    updateUI(result.getSlotId(), result.getStatus().get() == Status.SUCCESS);
                    break;
                default:
                    Log.e(TAG, "Unsupported action " + msg.what);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mContext = getContext();
        String title = mContext.getResources().getString(R.string.cellular_roaming);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
        sPackageName = mContext.getPackageName();
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }

        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        if (tm != null) {
            mActiveModemCount = tm.getActiveModemCount();
        }
        int[] activeSubs = mSubscriptionManager.getActiveSubscriptionIdList();
        for (int subId : activeSubs) {
            registerCallStateListener(subId);
        }
        mSubscriptionChangeListener = new SubscriptionsChangeListener(mContext, this);
        mSubscriptionChangeListener.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mSubscriptionChangeListener.stop();
        mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
        for (int slot = 0; slot < mActiveModemCount; slot++) {
            int subId = mSubscriptionManager.getSubscriptionId(slot);
            unregisterCallStateListener(subId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        mView = inflater.inflate(R.layout.cellular_roaming_sim_fragment, container, false);
        mSwitchMap.put(0, mView.findViewById(R.id.slot1));
        mSwitchMap.put(1, mView.findViewById(R.id.slot2));
        mExtTelephonyManager = ExtTelephonyManager.getInstance(getContext());
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
        setSwitchListener();
        return mView;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        return;
    }

    @Override
    public void onSubscriptionsChanged() {
        Log.d(TAG, "onSubscriptionsChanged");
        for (int slotId = 0; slotId < mActiveModemCount; slotId++) {
            int subId = mSubscriptionManager.getSubscriptionId(slotId);
            if (mSubscriptionManager.isActiveSubId(subId)) {
                registerCallStateListener(subId);
            } else {
                unregisterCallStateListener(subId);
            }
            evaluateLayoutInteractability(slotId);
        }
    }

    private void setSwitchListener() {
        for (int slot = 0; slot < mActiveModemCount; slot++) {
            Switch toggle = mSwitchMap.get(slot);
            final int slotId = slot;
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                    // This check is to not proceed with the actions in the callback when the button
                    // is programatically checked using setChecked because otherwise it will end up
                    // in an indefinite loop
                    if (!button.isPressed()) {
                        return;
                    }
                    Log.d(TAG, "setToggleState " + isChecked);
                    setToggleState(slotId, isChecked ? ENABLED : DISABLED);
                }
            });
        }
    }

    private void initUI() {
        // Check if the cached state and the modem states match. If they match, simply set the
        // switch's checked status to the cached state. Otherwise, send a request to the modem to
        // apply the user's previously chosen value
        for (int slotId = 0; slotId < MAX_ACTIVE_MODEM_COUNT; slotId++) {
            Switch toggle = mSwitchMap.get(slotId);
            int cachedState = readFromCache(TELEPHONY_STATE_CACHE_KEY, slotId);
            boolean match = readFromCache(SYNC_STATE_CACHE_KEY, slotId) == TRUE;
            if (match) {
                toggle.setChecked(cachedState == ENABLED);
            } else {
                String summary = toggle.getText() + ": " + mContext.getResources().getString(
                        R.string.modem_mismatch_summary);
                toggle.setText(summary);
            }
            evaluateLayoutInteractability(slotId);
        }
    }

    private void evaluateLayoutInteractability(int slotId) {
        // Only enable the UI for slots whose sub ID corresponds to a known subscription and the
        // SIM providing the subscription is present in a slot and in the "LOADED" state
        boolean isSubActive = mSubscriptionManager.isActiveSubId(
                mSubscriptionManager.getSubscriptionId(slotId));
        boolean show = isSubActive && mCallState[slotId] == CALL_STATE_IDLE;
        mSwitchMap.get(slotId).setEnabled(show);
    }

    private void setToggleState(int slotId, int state) {
        // Temporarily update the summary and disable the switch until the response is received
        Switch toggle = mSwitchMap.get(slotId);
        String summary = mContext.getResources().getString(R.string.slot, slotId) + ": " +
                mContext.getResources().getString(R.string.wait_summary);
        toggle.setText(summary);
        toggle.setEnabled(false);
        if (mServiceConnected && mClient != null) {
            try {
                // Passing INVALID for the domestic cellular roaming pref so as to not modify it on
                // the modem side
                mExtTelephonyManager.setCellularRoamingPreference(mClient, slotId,
                        new CellularRoamingPreference(state, INVALID));
            } catch (Exception e) {
                Log.e(TAG, "setToggleState: exception " + e);
            }
        } else {
            Log.e(TAG, "ExtTelephonyService not connected or client null");
        }
    }

    private void updateUI(int slotId, boolean result) {
        Log.d(TAG, "updateUI: slotId = " + slotId + ", result = " + result);
        Switch toggle = mSwitchMap.get(slotId);
        toggle.setEnabled(true);
        String summary = mContext.getResources().getString(R.string.slot, slotId + 1);
        if (result) {
            writeToCache(slotId, toggle.isChecked() ? ENABLED : DISABLED);
        } else {
            summary += ": " + mContext.getResources().getString(R.string.modem_error_summary);
        }
        // In case of failure, revert the toggle state to its previous one
        toggle.setChecked(result ? toggle.isChecked() : !toggle.isChecked());
        toggle.setText(summary);
    }

    private void writeToCache(int slotId, int state) {
        Settings.Global.putInt(mContext.getContentResolver(), SYNC_STATE_CACHE_KEY + slotId, TRUE);
        Settings.Global.putInt(mContext.getContentResolver(), TELEPHONY_STATE_CACHE_KEY + slotId,
                state);
    }

    private int readFromCache(String key, int slotId) {
        switch (key) {
            case TELEPHONY_STATE_CACHE_KEY:
                return Settings.Global.getInt(mContext.getContentResolver(),
                        TELEPHONY_STATE_CACHE_KEY + slotId, ENABLED);
            case SYNC_STATE_CACHE_KEY:
                return Settings.Global.getInt(mContext.getContentResolver(),
                        SYNC_STATE_CACHE_KEY + slotId, TRUE);
            default:
                Log.e(TAG, "Unsupported key = " + key);
        }
        return Integer.MIN_VALUE;
    }

    private void registerCallStateListener(int subId) {
        // Only register if not already registered
        if (mCallStateListeners.get(subId) == null) {
            CallStateListener callback = new CallStateListener(subId);
            mCallStateListeners.put(subId, callback);
            mCallStateListeners.get(subId).register(mContext);
        }
    }

    private void unregisterCallStateListener(int subId) {
        if (SubscriptionManager.isValidSubscriptionId(subId) && mCallStateListeners != null) {
            CallStateListener callback = mCallStateListeners.get(subId);
            if (callback != null) {
                callback.unregister();
                mCallStateListeners.put(subId, null);
            }
        }
    }

    private class CallStateListener extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {
        private int mSubId;
        private TelephonyManager mTelephonyManager;

        public CallStateListener(int subId) {
            mSubId = subId;
        }

        @Override
        public void onCallStateChanged(int state) {
            int slot = mSubscriptionManager.getSlotIndex(mSubId);
            mCallState[slot] = state;
            evaluateLayoutInteractability(slot);
        }

        public void register(Context context) {
            if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
                mTelephonyManager = context.getSystemService(
                        TelephonyManager.class).createForSubscriptionId(mSubId);
                mTelephonyManager.registerTelephonyCallback(context.getMainExecutor(),
                        mCallStateListeners.get(mSubId));
            } else {
                Log.d(TAG, "register: invalid subId");
            }
        }

        public void unregister() {
            mTelephonyManager.unregisterTelephonyCallback(this);
        }
    }
}