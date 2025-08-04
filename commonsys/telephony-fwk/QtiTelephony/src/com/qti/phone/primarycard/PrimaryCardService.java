/*
 * Copyright (c) 2021, 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.primarycard;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;


public class PrimaryCardService extends Service {
    private static final String TAG = "PrimaryCardService";
    private Context mContext;
    private Context mDirectBootContext;
    private int mNumPhones;

    private SettingsUserDataPreferenceContentObserver mSettingsUserDataPrefObserver;

    private static final int SLOT_INDEX_ZERO = 0;
    private static final int SLOT_INDEX_ONE = 1;

    private enum PrimaryCardSelectionReason {
        REASON_BOTH_CMCC_AND_BOTH_OLD_OR_NEW,
        REASON_BOTH_CMCC_AND_ONE_NEW,
        REASON_ONE_CMCC_AND_BOTH_NEW,
        REASON_ONE_CMCC_AND_ONE_NEW,
        REASON_ONE_CMCC_AND_BOTH_OLD,
        REASON_NO_CMCC,
        REASON_UNKNOWN
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED.equals(intent.getAction())) {
                int simStateExtra = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
                int slotIdExtra = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                int subIdExtra = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                        SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
                handleSimApplicationStateChanged(simStateExtra, slotIdExtra, subIdExtra);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        log("PrimaryCardService onCreate +");
        mContext = this;
        mDirectBootContext = getApplicationContext().createDeviceProtectedStorageContext();

        SubscriptionManager subscriptionManager =
                (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager != null) {
            subscriptionManager = subscriptionManager.createForAllUserProfiles();
        }
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mNumPhones = telephonyManager.getActiveModemCount();

        PrimaryCardUtils.loadCmccIccIdPrefixList(this);
        PrimaryCardUtils.loadLastKnowsSubIdsFromDatabase(mDirectBootContext);
        PrimaryCardUtils.loadOldSubIdsFromDatabase(mDirectBootContext);

        // Monitor the contents of the data user preference in the Settings database
        mSettingsUserDataPrefObserver = new SettingsUserDataPreferenceContentObserver(null);
        mSettingsUserDataPrefObserver.
                setOnDataPreferenceChangedListener(this::onUserDataPreferenceChanged);
        mSettingsUserDataPrefObserver.register(this);

        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED));


        // Read SIM application states while starting up. This holds relevance when the
        // PrimaryCardService is implicitly started as a result of a change in multi-SIM
        // configuration
        try {
            for (int slotId = 0; slotId < mNumPhones; slotId++) {
                int simState = telephonyManager.getSimApplicationState(slotId);
                int subId = subscriptionManager.getSubscriptionId(slotId);
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    handleSimApplicationStateChanged(simState, slotId, subId);
                } else {
                    Log.e(TAG, "Received invalid subId for slot " + slotId);
                }
            }
        } catch (NullPointerException ex) {
            // We may end up here if we query for sim states before the objects at telephony
            // side are loaded during boot. The intent ACTION_SIM_APPLICATION_STATE_CHANGED takes
            // care of this.
            if (PrimaryCardUtils.VDBG) Log.e(TAG, "Error reading SIM application states", ex);
            else Log.e(TAG, "Error reading SIM application states");

        }

        log("PrimaryCardService onCreate -");
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        try {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mSettingsUserDataPrefObserver.unregister(this);
            PrimaryCardUtils.removeAllEntriesFromCurrentSlotToSubMap();
        } catch (RuntimeException ex) {
            Log.e(TAG, "Unable to unregister receiver", ex);
        }
        super.onDestroy();
    }

    private void handleSimApplicationStateChanged(int simState, int slotId, int subId) {
        log("SIM state changed for slot: " + slotId + ", state: " + simState);
        log("Previous SIMs: " + PrimaryCardUtils.getCurrentSlotIdToSubIdMapAsString());

        if (isValidSlotIndex(slotId)) {
            if (simState == TelephonyManager.SIM_STATE_LOADED) {
                // Save current configuration
                PrimaryCardUtils.addEntryInCurrentSlotToSubMap(slotId, subId);
                log("sub loaded: slotId: " + slotId + ", subId: " + subId);

                if (PrimaryCardUtils.haveAllSubscriptionsLoaded(this)) {
                    // subscription records for all slots have been loaded
                    // initiate primary card selection algorithm
                    log("all subscriptions loaded.");
                    handleAllSubscriptionsLoaded();
                } else {
                    log("only one subscription loaded.");
                }
            } else if (simState == TelephonyManager.SIM_STATE_ABSENT
                    || simState == TelephonyManager.SIM_STATE_UNKNOWN) {
                // SIM is removed. Remove the entry for the current slot from our cache.
                PrimaryCardUtils.removeEntryFromCurrentSlotToSubMap(slotId);
                log("onReceive: SIM state is absent for slot: " + slotId);
            }
        } else {
            Log.e(TAG, "onReceive: invalid slot index received: " + slotId);
        }
        log("Current SIMs: " + PrimaryCardUtils.getCurrentSlotIdToSubIdMapAsString());
    }

    private void handleAllSubscriptionsLoaded() {
        // Check if SIM cards have really changed. We don't want the primary card selection
        // algorithm to kick in on every reboot with the same cards
        if (PrimaryCardUtils.haveSimCardsChanged()) {
            log("SIM cards changed. Starting primary card selection.");
            // Current SIM combination is different from the last time two SIMs were present.
            // Update the last known subIds.
            PrimaryCardUtils.updateLastKnownSubIds(mDirectBootContext);
            startPrimaryCardSelection();
        } else {
            log("No change in SIM cards since last boot");
        }
    }

    private void startPrimaryCardSelection() {
        int primarySlotFromAlgo = determinePrimarySlot();

        if (isValidSlotIndex(primarySlotFromAlgo)) {
            showPrimaryCardSelectionPopup(primarySlotFromAlgo);
        } else {
            Log.e(TAG, "no usable primary slot could be determined.");
        }
    }

    private int determinePrimarySlot() {
        boolean[] isCmccCard = new boolean[mNumPhones];
        boolean[] isNewCard = new boolean[mNumPhones];

        int primarySlot = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        PrimaryCardSelectionReason reason = PrimaryCardSelectionReason.REASON_UNKNOWN;

        for (int slotId = 0; slotId < mNumPhones; slotId++) {
            isCmccCard[slotId] = PrimaryCardUtils.isCmccCardInSlot(this, slotId);
            isNewCard[slotId]  = PrimaryCardUtils.isNewCardInSlot(mDirectBootContext, slotId);
            log("slot: " + slotId
                    + ", isCmccCard: " + isCmccCard[slotId]
                    + ", isNewCard: " + isNewCard[slotId]);
        }

        // both cards are CMCC
        if (isCmccCard[SLOT_INDEX_ZERO] && isCmccCard[SLOT_INDEX_ONE]) {

            if ((isNewCard[SLOT_INDEX_ZERO] && isNewCard[SLOT_INDEX_ONE]) ||
                    (!isNewCard[SLOT_INDEX_ZERO] && !isNewCard[SLOT_INDEX_ONE])) {

                // CMCC + CMCC, either both cards are new, or both cards are old. Set slot 0 as DDS
                primarySlot = SLOT_INDEX_ZERO;
                reason = PrimaryCardSelectionReason.REASON_BOTH_CMCC_AND_BOTH_OLD_OR_NEW;

            } else {

                // CMCC + CMCC, only one of the cards is new. Set the old card as DDS
                primarySlot = isNewCard[SLOT_INDEX_ZERO] ? SLOT_INDEX_ONE : SLOT_INDEX_ZERO;
                reason = PrimaryCardSelectionReason.REASON_BOTH_CMCC_AND_ONE_NEW;

            }

        } else if (isCmccCard[SLOT_INDEX_ZERO] || isCmccCard[SLOT_INDEX_ONE]) {

            // only one card is CMCC
            int cmccSlot = isCmccCard[SLOT_INDEX_ZERO] ? SLOT_INDEX_ZERO : SLOT_INDEX_ONE;

            if (isNewCard[SLOT_INDEX_ZERO] && isNewCard[SLOT_INDEX_ONE]) {

                // CMCC + non-CMCC, both cards are new. Set CMCC card as DDS
                primarySlot = cmccSlot;
                reason = PrimaryCardSelectionReason.REASON_ONE_CMCC_AND_BOTH_NEW;

            } else if (isNewCard[SLOT_INDEX_ZERO] || isNewCard[SLOT_INDEX_ONE]) {

                int ddsSlot = SubscriptionManager.getSlotIndex(
                        SubscriptionManager.getDefaultDataSubscriptionId());

                if (isValidSlotIndex(ddsSlot) && !isCmccCard[ddsSlot]) {
                    // DDS is currently on the non-CMCC card
                    log("old non-CMCC card is the current DDS. Do not change anything");
                } else {
                    // Set CMCC as the DDS
                    primarySlot = cmccSlot;
                    reason = PrimaryCardSelectionReason.REASON_ONE_CMCC_AND_ONE_NEW;
                }

            } else {

                // both cards are old. Set slot 0 as DDS
                primarySlot = SLOT_INDEX_ZERO;
                reason = PrimaryCardSelectionReason.REASON_ONE_CMCC_AND_BOTH_OLD;

            }

        } else {
            // no cards are CMCC. do nothing.
            log("No CMCC card inserted. Do nothing.");
            reason = PrimaryCardSelectionReason.REASON_NO_CMCC;
        }

        log("primarySlot chosen as " + primarySlot + ", reason: " + reason);
        return primarySlot;
    }


    /**
     * Shows a dialog showing the list of subscriptions, and the current primary card.
     * The user can change the primary card in this dialog.
     */
    private void showPrimaryCardSelectionPopup(int primarySlotFromAlgo) {
        log("showing primary card selection popup, primarySlotFromAlgo: " + primarySlotFromAlgo);

        Intent intent = new Intent(this, PrimaryCardSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // pass current primary card selection as intent extra to the card selection activity
        intent.putExtra(PrimaryCardUtils.EXTRA_PRIMARY_SLOT_FROM_ALGORITHM, primarySlotFromAlgo);

        startActivity(intent);
    }

    private void onUserDataPreferenceChanged() {
        int newDataSubId = PrimaryCardUtils.getUserPreferredDataSubIdFromSettingsDb(this);
        log("Settings user data preference changed to: " + newDataSubId);
        PrimaryCardUtils.addSubIdToOldSubIdDatabase(mDirectBootContext, newDataSubId);
    }

    private boolean isValidSlotIndex(int slotId) {
        return slotId >= 0 && slotId < mNumPhones;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
