/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.primarycard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.qti.phone.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PrimaryCardUtils {

    private static final String TAG = "PrimaryCardUtils";
    private static Context sDirectBootContext;
    static final boolean VDBG = false;

    /**
     * Set of known ICCID prefixes for CMCC cards.
     */
    private static final Set<String> sCmccIccIdPrefixSet = new HashSet<>();

    /**
     * Map of slotId to subId for the current configuration of the device
     * Entries are added/removed to this Map via
     * {@link #addEntryInCurrentSlotToSubMap(int, int)} and
     * {@link #removeEntryFromCurrentSlotToSubMap(int)}
     */
    private static final Map<Integer, Integer> sCurrentSlotIdToSubIdMap = new HashMap<>();

    /**
     * Set of subIds loaded the last time both SIMs were inserted.
     * Used to check if new cards have been inserted at a later point in time.
     */
    private static Set<Integer> sLastKnownSubIds;

    /**
     * Set of subIds known to have been set as the DDS/PrimaryCard by the user.
     */
    private static Set<Integer> sOldSubIds;

    /**
     * Key in the SharedPreferences that stores {@link #sLastKnownSubIds}.
     */
    private static final String KEY_LAST_KNOWN_SUB_IDS_SET = "key_last_known_sub_ids_set";

    /**
     * Key in the SharedPreferences that keeps track of subIds that have been marked as "old".
     * @see #isNewCardInSlot(Context, int)
     */
    private static final String KEY_OLD_SUB_ID_SET = "key_old_sub_id_set";

    /**
     * Key in Settings database that stores the current DDS.
     */
    public static final String SETTING_USER_PREF_DATA_SUB = "user_preferred_data_sub";

    /**
     * SlotId indicating the primary slot as chosen by the Primary Card selection algorithm.
     * This is sent as an extra to {@link PrimaryCardSelectionActivity}.
     */
    public static final String EXTRA_PRIMARY_SLOT_FROM_ALGORITHM = "primary_slot_from_algorithm";

    private static final long NETWORK_STANDARDS_FAMILY_BITMASK_3GPP =
            TelephonyManager.NETWORK_TYPE_BITMASK_GSM
            | TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
            | TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
            | TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA
            | TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA
            | TelephonyManager.NETWORK_TYPE_BITMASK_HSPA
            | TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP
            | TelephonyManager.NETWORK_TYPE_BITMASK_UMTS
            | TelephonyManager.NETWORK_TYPE_BITMASK_TD_SCDMA
            | TelephonyManager.NETWORK_TYPE_BITMASK_LTE
            | TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA
            | TelephonyManager.NETWORK_TYPE_BITMASK_NR;

    /**
     * Returns the last known subIds from {@link #KEY_LAST_KNOWN_SUB_IDS_SET} of the database.
     * @see #sLastKnownSubIds
     *
     * @param context DirectBoot aware context
     * @return Set of subIds
     */
    static Set<Integer> getLastKnownSubIdsFromDatabase(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_preference_file_key), Context.MODE_PRIVATE);

        Set<String> lastKnownSubIdStringSet = sharedPref.getStringSet(KEY_LAST_KNOWN_SUB_IDS_SET,
                new HashSet<>());

        Set<Integer> lastKnownSubIdSet = new HashSet<>();
        for (String subId : lastKnownSubIdStringSet) {
            lastKnownSubIdSet.add(Integer.parseInt(subId));
        }

        return lastKnownSubIdSet;
    }

    /**
     * Returns the "old" subIds from {@link #KEY_OLD_SUB_ID_SET} of the database.
     *
     * @param context DirectBoot aware context
     * @return Set of subIds
     */
    static Set<Integer> getOldSubIdsFromDatabase(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_preference_file_key), Context.MODE_PRIVATE);

        Set<String> oldSubIdStringSet = sharedPref.getStringSet(KEY_OLD_SUB_ID_SET,
                new HashSet<>());

        Set<Integer> oldSubIdSet = new HashSet<>();
        for (String subId : oldSubIdStringSet) {
            oldSubIdSet.add(Integer.parseInt(subId));
        }

        return oldSubIdSet;
    }

    /**
     * Update the last known subIds database. This is called whenever we encounter a new instance
     * when both SIMs are in {@link TelephonyManager#SIM_STATE_LOADED} state.
     *
     * @param context DirectBoot aware context
     */
    static void updateLastKnownSubIds(Context context) {
        Log.d(TAG, "Update last known subIds. Old set " + getLastKnownSubIdsFromDatabase(context));

        if (sLastKnownSubIds == null) {
            Log.e(TAG, "updateLastKnownSimCards: LastKnownSubIds set is null. Unexpected!");
            sLastKnownSubIds = new HashSet<>();
        }

        sLastKnownSubIds.clear();
        sLastKnownSubIds.addAll(sCurrentSlotIdToSubIdMap.values());

        Set<String> lastKnownSubIdsStringSet = new HashSet<>();
        for (int subId : sLastKnownSubIds) {
            lastKnownSubIdsStringSet.add(Integer.toString(subId));
        }

        // Read the current list from the database
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_preference_file_key), Context.MODE_PRIVATE);

        // Store the modified list back in the database
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(KEY_LAST_KNOWN_SUB_IDS_SET, lastKnownSubIdsStringSet);
        editor.apply();

        Log.d(TAG, "modified last known subIds: " + getLastKnownSubIdsFromDatabase(context));
    }

    /**
     * Sets the subscription corresponding to the given slotId as the primary card.
     * This involves the following:
     *  1. Set the subscription as the DDS
     *  2. Update the DDS key in the Settings Database
     *  3. Set the network mode of the subscription to 5/4/3/2G
     *
     * @param context Context
     * @param slotId slot index
     */
    static void setSlotAsPrimary(Context context, int slotId) {
        if (!isValidSlotIndex(context, slotId) || !sCurrentSlotIdToSubIdMap.containsKey(slotId)) {
            Log.e(TAG, "setSlotAsPrimary: invalid slot index: " + slotId);
            return;
        }

        try {
            int subId = sCurrentSlotIdToSubIdMap.get(slotId);

            // Set the subId corresponding to the given slotId as the DDS
            SubscriptionManager subscriptionManager = (SubscriptionManager)
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (subscriptionManager == null) return;

            subscriptionManager = subscriptionManager.createForAllUserProfiles();
            subscriptionManager.setDefaultDataSubId(subId);

            // Update the DDS config in the Settings database
            setUserPreferredDataSubIdInSettingsDb(context, subId);

            // Add the subId corresponding to the chosen slot to oldSubIdDatabase
            PrimaryCardUtils.addSubIdToOldSubIdDatabase(sDirectBootContext, subId);

            // set network mode to 5/4/3/2G
            TelephonyManager telephonyManagerForSub = ((TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE)).createForSubscriptionId(subId);

            if (telephonyManagerForSub != null) {
                // TODO: Change this to setAllowedNetworkTypesForReason
                telephonyManagerForSub.setPreferredNetworkTypeBitmask(
                        NETWORK_STANDARDS_FAMILY_BITMASK_3GPP);
            }

        } catch (RuntimeException ex) {
            Log.e(TAG, "Unable to set primary card to slotId: " + slotId, ex);
        }
    }

    /**
     * Cache the CMCC ICCID prefix list from the resources.
     *
     * @param context Context
     */
    static void loadCmccIccIdPrefixList(Context context) {
        String[] prefixList = context.getResources().getStringArray(R.array.cmcc_iccid_prefix_list);
        sCmccIccIdPrefixSet.addAll(Arrays.asList(prefixList));
        if (VDBG) Log.d(TAG, "CMCC IccId prefixes: " + sCmccIccIdPrefixSet);
    }

    /**
     * Cache the last known subIds from the database. Called once when {@link PrimaryCardService}
     * is started.
     *
     * @param context Context
     */
    static void loadLastKnowsSubIdsFromDatabase(Context context) {
        sDirectBootContext = context;
        sLastKnownSubIds = getLastKnownSubIdsFromDatabase(context);
        if (VDBG) Log.d(TAG, "last known subIds: " + sLastKnownSubIds);
    }

    static void loadOldSubIdsFromDatabase(Context context) {
        sOldSubIds = getOldSubIdsFromDatabase(context);
        if (VDBG) Log.d(TAG, "Old subIds: " + sOldSubIds);
    }

    /**
     * Checks if the subscription in the given slotId is a CMCC card
     *
     * @param context Context
     * @param slotId slot index
     * @return true if the slotId has a CMCC card, false otherwise.
     */
    static boolean isCmccCardInSlot(Context context, int slotId) {
        SubscriptionManager subscriptionManager = (SubscriptionManager)
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager == null) return false;

        subscriptionManager = subscriptionManager.createForAllUserProfiles();
        SubscriptionInfo subInfo =
                subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slotId);

        if (subInfo != null) {
            String iccId = subInfo.getIccId();
            if (TextUtils.isEmpty(iccId) || iccId.length() < 6) {
                Log.e(TAG, "isCmccCardOnSlot: Invalid iccId for" +
                        " slot: " + slotId +
                        ", iccId : " + ((iccId == null) ? "null" : iccId));
                return false;
            }

            String iccIdPrefix = iccId.substring(0, 6);
            Log.d(TAG, "isCmccCardOnSlot: slot: " + slotId + ", iccIdPrefix: " + iccIdPrefix);
            if (sCmccIccIdPrefixSet.contains(iccIdPrefix)) {
                Log.d(TAG, "isCmccCardOnSlot: slotId " + slotId + " contains a CMCC card");
                return true;
            } else {
                Log.d(TAG, "isCmccCardOnSlot: slotId " + slotId + " contains a non-CMCC card");
                return false;
            }
        } else {
            Log.e(TAG, "isCmccCardOnSlot: Invalid subInfo for slot " + slotId);
            return false;
        }
    }

    /**
     * Checks if the given slotId contains a "new" card.
     *
     * New card:
     *  1. a SIM that has never been inserted in this device before
     *  2. a SIM that has been set as primary automatically, without any user confirmation
     *
     * Old card:
     *  1. a SIM that has been previously set as DDS in the settings application, or
     *  2. a SIM that has been set as primary in the PrimaryCard popup and confirmed by the user.
     *
     * @param context DirectBoot aware context
     * @param slotId slot index
     * @return true if the slotId contains a "new" card, false otherwise.
     */
    static boolean isNewCardInSlot(Context context, int slotId) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_preference_file_key), Context.MODE_PRIVATE);
        Set<String> oldSubIdSet = sharedPref.getStringSet(KEY_OLD_SUB_ID_SET, new HashSet<>());

        if (VDBG) Log.d(TAG, "KEY_OLD_SUB_ID_SET: " + oldSubIdSet);

        if (!sCurrentSlotIdToSubIdMap.containsKey(slotId)) {
            Log.d(TAG, "isNewCardOnSlot: unexpected slotId: " + slotId);
            return false;
        }

        int subId = sCurrentSlotIdToSubIdMap.get(slotId);

        if (oldSubIdSet.contains(Integer.toString(subId))) {
            Log.d(TAG, "isNewCardOnSlot: slotId " + slotId + " contains an old card");
            return false;
        } else {
            Log.d(TAG, "isNewCardOnSlot: slotId " + slotId + " contains a new card");
            return true;
        }
    }

    /**
     * Adds the given subId to the database of "old" subIds, stored in SharedPreferences as
     * {@link #KEY_OLD_SUB_ID_SET}.
     *
     * Add a subId to the database whenever the following events occur:
     *  1. The DDS preference is changed by the user in the Settings app.
     *  2. The user confirms primary card selection from the {@link PrimaryCardSelectionActivity}
     *
     * @param context Context
     * @param subId SubId
     */
    static void addSubIdToOldSubIdDatabase(Context context, int subId) {
        Log.d(TAG, "store " + subId + " in database of old subIds. Old set: "
                + getOldSubIdsFromDatabase(context));

        if (sOldSubIds == null) {
            Log.e(TAG, "addSubIdToOldSubIdDatabase: sOldSubIds set is null. Unexpected!");
            sOldSubIds = new HashSet<>();
        }

        sOldSubIds.add(subId);

        Set<String> oldSubIdsStringSet = new HashSet<>();
        for (int oldSubId : sOldSubIds) {
            oldSubIdsStringSet.add(Integer.toString(oldSubId));
        }

        // Read the current list from the database
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_preference_file_key), Context.MODE_PRIVATE);

        // Store the modified list back in the database
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(KEY_OLD_SUB_ID_SET, oldSubIdsStringSet);
        editor.apply();

        Log.d(TAG, "stored " + subId + " in database of old subIds. New set: "
                + getOldSubIdsFromDatabase(context));
    }

    /**
     * Checks if there has been a change in SIM cards since the last time there were two SIMs
     * present in the device
     *
     * @return true if the SIM cards have changed, false otherwise
     */
    static boolean haveSimCardsChanged() {
        if (sLastKnownSubIds == null) {
            Log.e(TAG, "haveSimCardsChanged, LastKnownSubIds is null. Unexpected.");
            return true;
        }
        return !(sLastKnownSubIds.containsAll(sCurrentSlotIdToSubIdMap.values()));
    }

    /**
     * Add an entry to {@link #sCurrentSlotIdToSubIdMap}
     *
     * @param slotId slot index to be added
     * @param subId subId corresponding to the slotId
     */
    static void addEntryInCurrentSlotToSubMap(int slotId, int subId) {
        sCurrentSlotIdToSubIdMap.put(slotId, subId);
        Log.d(TAG, "Added slot: " + slotId + ", sub: " + subId + " to current slotId-subId map. " +
                "Map: " + sCurrentSlotIdToSubIdMap);
    }

    /**
     * Remove an entry from {@link #sCurrentSlotIdToSubIdMap}
     *
     * @param slotId slot index to be removed
     */
    static void removeEntryFromCurrentSlotToSubMap(int slotId) {
        sCurrentSlotIdToSubIdMap.remove(slotId);
        Log.d(TAG, "Removed slot: " + slotId + " from current slotId-subId map. " +
                "Map: " + sCurrentSlotIdToSubIdMap);
    }

    /**
     * Remove all entries from {@link #sCurrentSlotIdToSubIdMap}
     */
    static void removeAllEntriesFromCurrentSlotToSubMap() {
        sCurrentSlotIdToSubIdMap.clear();
        Log.d(TAG, "Removed all entries from current slotId-subId map. " +
                "Map: " + sCurrentSlotIdToSubIdMap);
    }

    /**
     * Returns the contents of {@link #sCurrentSlotIdToSubIdMap} as a String. Used for
     * debugging purposes.
     *
     * @return String
     */
    static String getCurrentSlotIdToSubIdMapAsString() {
        return sCurrentSlotIdToSubIdMap.toString();
    }

    /**
     * Check if subscriptions for all the slots have been loaded
     *
     * @param context Context
     * @return true or false
     */
    static boolean haveAllSubscriptionsLoaded(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return sCurrentSlotIdToSubIdMap.size() == telephonyManager.getActiveModemCount();
    }

    /**
     * Return the user preferred data subId from the Settings database.
     *
     * @param context Context
     * @return subId
     */
    static int getUserPreferredDataSubIdFromSettingsDb(Context context) {
        return android.provider.Settings.Global.getInt(context.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    /**
     * Update the user preferred data subId in the Settings database to the given subId.
     *
     * @param context Context
     * @param subId subId
     */
    static void setUserPreferredDataSubIdInSettingsDb(Context context, int subId) {
        android.provider.Settings.Global.putInt(context.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, subId);
    }

    /**
     * Check if the given slotId is a valid slot index
     *
     * @param context Context
     * @param slotId slot index
     * @return true or false
     */
    static boolean isValidSlotIndex(Context context, int slotId) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return slotId >= 0 && slotId < telephonyManager.getActiveModemCount();
    }

    public static void setupEdgeToEdge(@NonNull Activity activity) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                                    | WindowInsetsCompat.Type.displayCutout());
                    // Apply the insets paddings to the view.
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    // Return CONSUMED if you don't want the window insets to keep being
                    // passed down to descendant views.
                    return WindowInsetsCompat.CONSUMED;
                });
    }
}
