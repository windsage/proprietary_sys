/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings;

import static android.telephony.TelephonyManager.CALL_STATE_IDLE;

import static com.qti.extphone.ExtPhoneCallbackListener.EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.ArrayList;

public class CiwlanModePreferenceFragment extends Fragment implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "CiwlanModePreferenceFragment";

    private static final String SYNC_STATE_CACHE_KEY = "CELLULAR_I_WLAN_SYNC_KEY";
    private static final String TELEPHONY_STATE_CACHE_KEY = "CELLULAR_I_WLAN_PREFERENCE_STATE_KEY";

    private static String HOME;
    private static String ROAMING;
    private static String mPackageName;
    private View mView;
    private Context mContext;
    private SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mSubscriptionChangeListener;
    private ExtTelephonyManager mExtTelephonyManager;
    private Client mClient;
    private int mActiveModemCount;
    private int[] mCallState = {CALL_STATE_IDLE, CALL_STATE_IDLE};
    private boolean mServiceConnected = false;
    private boolean mApmEnabled = false;
    private boolean[] mResponseArray = {true, true};
    private ArrayMap<Integer, CallStateListener> mCallStateListeners = new ArrayMap<>();
    private ArrayList<ArrayMap<String, RadioGroup>> mRadioGroupList = new ArrayList<>();
    private ArrayList<Button> mSubmitButtonList = new ArrayList<>();
    private RadioGroup mSlot1HomeRadioGroup;
    private RadioGroup mSlot1RoamRadioGroup;
    private RadioGroup mSlot2HomeRadioGroup;
    private RadioGroup mSlot2RoamRadioGroup;

    private static final int INVALID = CiwlanConfig.INVALID;
    private static final int ONLY = CiwlanConfig.ONLY;
    private static final int PREFERRED = CiwlanConfig.PREFERRED;
    private static final String DEFAULT_PREFS = PREFERRED + "," + ONLY;
    private static final int TRUE = 1;
    private static final int FALSE = 0;
    private static final int MAX_ACTIVE_MODEM_COUNT = 2;

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "ExtTelephonyService connected");
            mServiceConnected = true;
            int[] events = new int[] {EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(mPackageName,
                    mExtPhoneCallbackListener, events);
            Log.d(TAG, "Client = " + mClient);
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

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void setCiwlanModeUserPreferenceResponse(int slotId, Token token, Status status)
                throws RemoteException {
            Log.d(TAG, "setCiwlanModeUserPreferenceResponse: slotId = " + slotId + ", token = " +
                    token + ", status = " + status.get());
            mHandler.sendMessage(mHandler.obtainMessage(
                    EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE,
                    new Result(slotId, status, null)));
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Result result = (Result) msg.obj;
            switch (msg.what) {
                case EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE:
                    Log.d(TAG, "EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE");
                    mResponseArray[result.getSlotId()] = true;
                    updateUI(result.getSlotId(), result.getStatus().get() == Status.SUCCESS);
                    break;
                default:
                    Log.e(TAG, "Unsupported action");
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mContext = getContext();
        HOME = mContext.getResources().getString(R.string.home);
        ROAMING = mContext.getResources().getString(R.string.roaming);
        String title = mContext.getResources().getString(R.string.ciwlan_preference_title);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
        mPackageName = mContext.getPackageName();
        mApmEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        if (tm != null) {
            mActiveModemCount = tm.getActiveModemCount();
        }
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
            int[] activeSubs = mSubscriptionManager.getActiveSubscriptionIdList();
            for (int subId : activeSubs) {
                registerCallStateListener(subId);
            }
        }

        mSubscriptionChangeListener = new SubscriptionsChangeListener(mContext, this);
        mSubscriptionChangeListener.start();
        mExtTelephonyManager = ExtTelephonyManager.getInstance(getContext());
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
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
        mView = inflater.inflate(R.layout.ciwlan_mode_preference_fragment, container, false);
        initViews();
        return mView;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        mApmEnabled = airplaneModeEnabled;
        for (int slot = 0; slot < mActiveModemCount; slot++) {
            evaluateLayoutInteractability(slot);
        }
    }

    @Override
    public void onSubscriptionsChanged() {
        for (int slot = 0; slot < mActiveModemCount; slot++) {
            int subId = mSubscriptionManager.getSubscriptionId(slot);
            if (mSubscriptionManager.isActiveSubId(subId)) {
                registerCallStateListener(subId);
            } else {
                unregisterCallStateListener(subId);
            }
            evaluateLayoutInteractability(slot);
        }
    }

    private void initViews() {
        ArrayMap<String, RadioGroup> slot1Map = new ArrayMap<>();
        ArrayMap<String, RadioGroup> slot2Map = new ArrayMap<>();
        mSlot1HomeRadioGroup = mView.findViewById(R.id.slot1HomeRadioGroup);
        mSlot1RoamRadioGroup = mView.findViewById(R.id.slot1RoamRadioGroup);
        slot1Map.put(HOME, mSlot1HomeRadioGroup);
        slot1Map.put(ROAMING, mSlot1RoamRadioGroup);
        mRadioGroupList.add(0, slot1Map);
        mSlot2HomeRadioGroup = mView.findViewById(R.id.slot2HomeRadioGroup);
        mSlot2RoamRadioGroup = mView.findViewById(R.id.slot2RoamRadioGroup);
        slot2Map.put(HOME, mSlot2HomeRadioGroup);
        slot2Map.put(ROAMING, mSlot2RoamRadioGroup);
        mRadioGroupList.add(1, slot2Map);
        mSubmitButtonList.add(0, mView.findViewById(R.id.slot1SubmitButton));
        mSubmitButtonList.add(1, mView.findViewById(R.id.slot2SubmitButton));
        restoreSavedRadioGroupValues();
        setSubmitButtonListeners();
        for (int slotId = 0; slotId < MAX_ACTIVE_MODEM_COUNT; slotId++) {
            boolean match = ((int) readFromCache(SYNC_STATE_CACHE_KEY, slotId)) == TRUE;
            if (!match) {
                Button button = mSubmitButtonList.get(slotId);
                button.setText(button.getText() + ": " + mContext.getResources().getString(
                        R.string.modem_mismatch_summary));
            }
            evaluateLayoutInteractability(slotId);
        }
    }

    private void restoreSavedRadioGroupValues() {
        for (int i = 0; i < mActiveModemCount; i++) {
            String cachedPrefs = (String) readFromCache(TELEPHONY_STATE_CACHE_KEY, i);
            ArrayMap<String, RadioGroup> map = mRadioGroupList.get(i);
            RadioGroup homeRadioGroup = map.get(HOME);
            RadioGroup roamingRadioGroup = map.get(ROAMING);
            homeRadioGroup.check(getRadioButtonId(HOME, i,
                    Integer.valueOf(cachedPrefs.split(",")[0])));
            roamingRadioGroup.check(getRadioButtonId(ROAMING, i,
                    Integer.valueOf(cachedPrefs.split(",")[1])));
        }
    }

    private int getRadioButtonId(String location, int slotId, int mode) {
        switch (mode) {
            case ONLY:
                if (location == HOME) {
                    if (slotId == 0) { return R.id.slot1HomeOnly; }
                    return R.id.slot2HomeOnly;
                } else {
                    if (slotId == 0) { return R.id.slot1RoamingOnly; }
                    return R.id.slot2RoamingOnly;
                }
            case PREFERRED:
                if (location == HOME) {
                    if (slotId == 0) { return R.id.slot1HomePreferred; }
                    return R.id.slot2HomePreferred;
                } else {
                    if (slotId == 0) { return R.id.slot1RoamingPreferred; }
                    return R.id.slot2RoamingPreferred;
                }
            default:
                Log.e(TAG, "Invalid mode");
        }
        return INVALID;
    }

    private void setSubmitButtonListeners() {
        for (Button button : mSubmitButtonList) {
            button.setOnClickListener((view) -> sendUserSelectionToModem(view.getId()));
        }
    }

    private void sendUserSelectionToModem(int id) {
        int homePref = INVALID;
        int roamPref = INVALID;
        int slotId = -1;
        switch (id) {
            case R.id.slot1SubmitButton:
                slotId = 0;
                homePref = getPrefFromSelection(mSlot1HomeRadioGroup.getCheckedRadioButtonId());
                roamPref = getPrefFromSelection(mSlot1RoamRadioGroup.getCheckedRadioButtonId());
                break;
            case R.id.slot2SubmitButton:
                slotId = 1;
                homePref = getPrefFromSelection(mSlot2HomeRadioGroup.getCheckedRadioButtonId());
                roamPref = getPrefFromSelection(mSlot2RoamRadioGroup.getCheckedRadioButtonId());
                break;
        }
        if (mServiceConnected && mClient != null) {
            try {
                CiwlanConfig pref = new CiwlanConfig(homePref, roamPref);
                mResponseArray[slotId] = false;
                mExtTelephonyManager.setCiwlanModeUserPreference(slotId, mClient, pref);
            } catch (Exception e) {
                Log.e(TAG, "setCiwlanModeUserPreference: exception " + e);
            }
        } else {
            Log.e(TAG, "ExtTelephonyService not connected or client null");
        }
        // Temporarily update the button text and disable it until the response is received
        updateUI(slotId, false);
    }

    private int getPrefFromSelection(int id) {
        String ciwlanOnly = mContext.getResources().getString(R.string.ciwlan_only);
        String selectionText = ((Button) mView.findViewById(id)).getText().toString();
        return selectionText.contains(ciwlanOnly) ? ONLY : PREFERRED;
    }

    private void updateUI(int slotId, boolean result) {
        Log.d(TAG, "updateUI: slotId = " + slotId + ", result = " + result);
        String buttonText = mContext.getResources().getString(R.string.wait_summary);
        buttonText = mContext.getResources().getString(R.string.submit);
        if (result) {
            writeToCache(TELEPHONY_STATE_CACHE_KEY, slotId);
        } else {
            buttonText = mContext.getResources().getString(R.string.modem_error_summary);
        }
        mSubmitButtonList.get(slotId).setText(buttonText);
        evaluateLayoutInteractability(slotId);
    }

    private void evaluateLayoutInteractability(int slotId) {
        boolean enabled = true;
        // If airplane mode is on, gray out the UI
        if (mApmEnabled) {
            enabled = false;
        } else {
            // Only enable the UI for slots whose sub ID corresponds to a known subscription and the
            // SIM providing the subscription is present in a slot and in the "LOADED" state
            int subId = mSubscriptionManager.getSubscriptionId(slotId);
            if (!mSubscriptionManager.isActiveSubId(subId)) {
                enabled = false;
            } else if (mCallState[slotId] != CALL_STATE_IDLE) {
                enabled = false;
            } else if (!mResponseArray[slotId]) {
                enabled = false;
            }
        }
        ArrayMap<String, RadioGroup> map = mRadioGroupList.get(slotId);
        for (RadioGroup group : map.values()) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                child.setEnabled(enabled);
            }
        }
        mSubmitButtonList.get(slotId).setEnabled(enabled);
    }

    private void writeToCache(String id, int slotId) {
        ArrayMap<String, RadioGroup> map = mRadioGroupList.get(slotId);
        int homePrefId = map.get(HOME).getCheckedRadioButtonId();
        int roamPrefId = map.get(ROAMING).getCheckedRadioButtonId();
        String prefs = getCiwlanModeFromText(homePrefId) + "," + getCiwlanModeFromText(roamPrefId);
        switch (id) {
            case TELEPHONY_STATE_CACHE_KEY:
                Settings.Global.putString(mContext.getContentResolver(), id + slotId, prefs);
                Settings.Global.putInt(mContext.getContentResolver(), SYNC_STATE_CACHE_KEY + slotId,
                        TRUE);
                break;
            default:
            Log.e(TAG, "writeToCache: unsupported id");
        }
    }

    private Object readFromCache(String id, int slotId) {
        switch (id) {
            case SYNC_STATE_CACHE_KEY:
                return Settings.Global.getInt(mContext.getContentResolver(), id + slotId, TRUE);
            case TELEPHONY_STATE_CACHE_KEY:
                String cache = Settings.Global.getString(mContext.getContentResolver(),
                        id + slotId);
                cache = (cache == null) ? DEFAULT_PREFS : cache;
                return cache;
            default:
                Log.e(TAG, "readFromCache: unsupported id");
                return null;
        }
    }

    private int getCiwlanModeFromText(int id) {
        String onlyModeText = mContext.getResources().getString(R.string.ciwlan_only);
        String preferredModeText = mContext.getResources().getString(R.string.ciwlan_preferred);
        String text = ((Button) mView.findViewById(id)).getText().toString();
        if (text.contains(onlyModeText)) {
            return ONLY;
        } else if (text.contains(preferredModeText)) {
            return PREFERRED;
        }
        return INVALID;
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
                mTelephonyManager.registerTelephonyCallback(mContext.getMainExecutor(),
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