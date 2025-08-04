/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.primarycard;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.qti.phone.R;


public class PrimaryCardSelectionActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "PrimaryCardSelect";
    private RadioGroup mSubscriptionsRadioGroup;
    private Button mOkButton;
    private Context mContext;
    private boolean mIsPrimarySubSelected = false;
    private int mPrimarySlotFromAlgorithm = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrimaryCardUtils.setupEdgeToEdge(this);
        setContentView(R.layout.activity_primary_card_selection);

        mContext = getApplicationContext();
        mSubscriptionsRadioGroup = findViewById(R.id.subscriptions_radio_group);

        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        int phoneCount = telephonyManager.getActiveModemCount();

        for (int slot = 0; slot < phoneCount; slot++) {
            RadioButton radioButton = new RadioButton(this);
            mSubscriptionsRadioGroup.addView(radioButton,
                    new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
                            RadioGroup.LayoutParams.WRAP_CONTENT));

            radioButton.setEnabled(true);
            radioButton.setTag(slot);
            radioButton.setText(getSimName(slot));
            radioButton.setOnClickListener(this);
        }

        mOkButton = findViewById(R.id.select_ok_button);
        mOkButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mContext.registerReceiver(mSimStateReceiver,
                new IntentFilter(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED));

        mPrimarySlotFromAlgorithm =
                getIntent().getIntExtra(PrimaryCardUtils.EXTRA_PRIMARY_SLOT_FROM_ALGORITHM,
                SubscriptionManager.INVALID_SIM_SLOT_INDEX);

        Log.d(TAG, "onResume, primarySlotFromAlgorithm: " + mPrimarySlotFromAlgorithm);

        mSubscriptionsRadioGroup.clearCheck();
        for (int slot = 0; slot < mSubscriptionsRadioGroup.getChildCount(); slot++) {
            RadioButton radioButton = (RadioButton) mSubscriptionsRadioGroup.getChildAt(slot);
            radioButton.setChecked(mPrimarySlotFromAlgorithm == slot);
        }

        mOkButton.setTag(mPrimarySlotFromAlgorithm);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        if (!mIsPrimarySubSelected) {
            // Set primary card to primarySlot which it is from algorithm
            PrimaryCardUtils.setSlotAsPrimary(this, mPrimarySlotFromAlgorithm);
            mIsPrimarySubSelected = true;
        }

        try {
            getApplicationContext().unregisterReceiver(mSimStateReceiver);
        } catch (RuntimeException ex) {
            Log.e(TAG, "Exception in unregisterReceiver", ex);
        }
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if (v instanceof RadioButton) {
            // one of the radio buttons is clicked
            int slot = (Integer) v.getTag();
            mOkButton.setTag(slot);
            Log.d(TAG, "Radio button clicked for slot: " + slot);

        } else if (v == mOkButton) {

            // the OK button is clicked
            int primarySlotChosenByUser = (Integer) mOkButton.getTag();
            Log.d(TAG, "Set primary card to slot: " + primarySlotChosenByUser);

            // Set slot as primary card
            PrimaryCardUtils.setSlotAsPrimary(this, primarySlotChosenByUser);
            mIsPrimarySubSelected = true;

            finish();
        }
    }

    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "Received intent " + action);

            if (TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED.equals(action)) {

                int stateExtra = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
                Log.d(TAG, "SIM application state: " + stateExtra);

                if (stateExtra == TelephonyManager.SIM_STATE_LOADED) {
                    for (int index = 0; index < mSubscriptionsRadioGroup.getChildCount(); index++) {
                        RadioButton radioButton =
                                (RadioButton) mSubscriptionsRadioGroup.getChildAt(index);
                        final String currentSimName = radioButton.getText().toString();
                        final String newSimName = getSimName(index);
                        Log.d(TAG, "currentSim: " + currentSimName + ", newSim: " + newSimName);
                        if (!currentSimName.equals(newSimName)) {
                            radioButton.setText(newSimName);
                        }
                    }

                } else if (stateExtra == TelephonyManager.SIM_STATE_ABSENT
                        || stateExtra == TelephonyManager.SIM_STATE_UNKNOWN) {

                    Log.d(TAG, "a SIM card is removed. Finishing activity");
                    finish();
                }
            }
        }
    };

    private String getSimName(int slot) {
        SubscriptionInfo subInfo = null;
        try {
            SubscriptionManager subscriptionManager = (SubscriptionManager) mContext
                    .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (subscriptionManager != null) {
                subscriptionManager = subscriptionManager.createForAllUserProfiles();
                subInfo = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slot);
            }
        } catch (SecurityException ex) {
            Log.e(TAG, "SecurityException while reading subInfo records", ex);
        }

        return (subInfo == null)
                ? mContext.getResources().getString(R.string.sim_card_number_title, slot + 1)
                : subInfo.getDisplayName().toString();
    }
}
