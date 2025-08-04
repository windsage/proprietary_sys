/*
 * Copyright (c) 2021, 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.subsidylock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.preference.PreferenceManager;

import com.qti.phone.R;

/**
 * This class provides utility methods for other subsidy lock
   classes.
 */

public class SubsidyLockUtils {

    private static final String TAG = "SubsidyLockUtils";
    private static final String SETTING_USER_PREF_DATA_SUB = "user_preferred_data_sub";
    private static final String ALLOW_USER_SELECT_DDS = "allow_user_select_dds";
    private static final String SUBSIDY_STOREDICCID_KEY = "subsidy_storediccid";
    private static final String SUBSIDY_STOREDMCCMNC_KEY = "subsidy_storedmccmnc";
    static final String SUBSIDY_PRIMARY_SLOT = "subsidy_primary_slot";

    public static void setPrimaryCardOnSlot(Context context, int slotId) {
        if (!isValidSlotId(context, slotId)) {
            Log.e(TAG, "setPrimaryCardOnSlot: invalid slot index: " + slotId);
            return;
        }

        try {
            SubscriptionManager subscriptionManager = getSubscriptionManager(context);

            int subId = subscriptionManager.getSubscriptionId(slotId);
            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                Log.e(TAG, "setPrimaryCardOnSlot: invalid subId");
                return;
            }

            Log.d(TAG, "setPrimaryCardOnSlot: " + slotId);

            if (SubscriptionManager.getDefaultDataSubscriptionId() != subId) {
                Log.d(TAG, "setPrimaryCardOnSlot: Setting the DDS to subId: " + subId);
                subscriptionManager.setDefaultDataSubId(subId);
            }

            if (getUserPreferredDataSubIdFromDB(context) !=  subId) {
                Log.d(TAG, "Setting the user preferred data subId to " + subId);
                setUserPreferredDataSubIdInDB(context, subId);
            }
            saveIccIdMccMncOfSimCardsInSP(context);
        } catch (RuntimeException ex) {
            Log.e(TAG, "Unable to set primary card to slotId: " + slotId, ex);
        }
    }

    public static void setAllowUsertoSetDDS(Context context, boolean allow) {
        Settings.Global.putInt(context.getContentResolver(),
               ALLOW_USER_SELECT_DDS, allow ? 1 : 0);
    }

    public static boolean isPrimaryCapableSimCard(Context context, int slotId) {
        if (!isValidSlotId(context, slotId)) {
            Log.e(TAG, "isPrimaryCapableSimCard: invalid slot index: " + slotId);
            return false;
        }

        String[] jiomccmnclist = context.getResources().getStringArray(R.array.jio_mccmnc_list);
        if (jiomccmnclist == null) {
           Log.e(TAG, "isPrimaryCapableSimCard: Failed to load mccmnc list");
           return false;
        }

        SubscriptionInfo subInfo =
                getSubscriptionManager(context).getActiveSubscriptionInfoForSimSlotIndex(slotId);

        if (subInfo == null) {
            Log.e(TAG, "isPrimaryCapableSimCard: SubInfo is null for slotId " +slotId);
            return false;
        }

        if (subInfo.getMccString() == null || subInfo.getMncString() == null) {
            Log.e(TAG, "isPrimaryCapableSimCard: mcc/mnc is null for slotId " +slotId);
            return false;
        }

        String mccmnc = subInfo.getMccString() + subInfo.getMncString();
        for (String jiomccmnc : jiomccmnclist) {
            if (mccmnc.equals(jiomccmnc)) {
                Log.d(TAG, "Primary SIM card found with mccmnc:  " + mccmnc);
                return true;
            }
        }
        Log.d(TAG, "Not a primary card mccmnc: " + mccmnc);
        return false;
    }

    static boolean didSimCardsChanged(Context context) {
        for (int slotId = 0; slotId < getTelephonyManager(context).getActiveModemCount();
                slotId++) {
            if (didSimCardsChanged(context, slotId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean didSimCardsChanged(Context context, int slotId) {
        String iccId = getIccId(context, slotId);
        String mccmnc = getMccMnc(context, slotId);
        Context directBootContext = context.getApplicationContext()
                    .createDeviceProtectedStorageContext();

        String iccIdInSP =
                PreferenceManager.getDefaultSharedPreferences(directBootContext)
                .getString(SUBSIDY_STOREDICCID_KEY + slotId, null);
        String mccMncInSP =
                PreferenceManager.getDefaultSharedPreferences(directBootContext)
                .getString(SUBSIDY_STOREDMCCMNC_KEY + slotId, null);

        Log.d(TAG, " slotId " + slotId + " icc id = " + iccId + ", icc id in sp=" + iccIdInSP);
        return !TextUtils.equals(iccId, iccIdInSP) || !TextUtils.equals(mccmnc, mccMncInSP);
    }

    private static String getIccId(Context context, int slotId) {
        SubscriptionInfo subInfo =
                getSubscriptionManager(context).getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (subInfo != null) {
            return subInfo.getIccId();
        }
        return null;
    }

    private static String getMccMnc(Context context, int slotId) {
        SubscriptionInfo subInfo =
                getSubscriptionManager(context).getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (subInfo == null || subInfo.getMccString() == null || subInfo.getMncString() == null) {
            return null;
        }
        return subInfo.getMccString() + subInfo.getMncString();
    }

    public static void saveIccIdMccMncOfSimCardsInSP(Context context) {
        for (int slotId = 0; slotId < getTelephonyManager(context).getActiveModemCount();
                slotId++) {
            String iccId = getIccId(context, slotId);
            String mccmnc = getMccMnc(context, slotId);
            Context directBootContext = context.getApplicationContext()
                    .createDeviceProtectedStorageContext();

            Log.d(TAG, "save IccId: " + iccId + ", on slotId:" + slotId + ", in SP.");
            PreferenceManager.getDefaultSharedPreferences(directBootContext).edit()
                    .putString(SUBSIDY_STOREDICCID_KEY + slotId, iccId).commit();

            Log.d(TAG, "save mccmnc: " + mccmnc + ", on slotId:" + slotId + ", in SP.");
            PreferenceManager.getDefaultSharedPreferences(directBootContext).edit()
                    .putString(SUBSIDY_STOREDMCCMNC_KEY + slotId, mccmnc).commit();
        }
    }

    private static void setUserPreferredDataSubIdInDB(Context context, int subId) {
        android.provider.Settings.Global.putInt(context.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, subId);
    }

    static int getUserPreferredDataSubIdFromDB(Context context) {
        return android.provider.Settings.Global.getInt(context.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    static boolean isValidSlotId(Context context, int slotId) {
        return slotId >= 0 && slotId < getTelephonyManager(context).getActiveModemCount();
    }

    private static SubscriptionManager getSubscriptionManager(Context context) {
        SubscriptionManager subscriptionManager = null;
        subscriptionManager = context.getSystemService(SubscriptionManager.class);
        if (subscriptionManager == null) return null;

        return subscriptionManager.createForAllUserProfiles();
    }

    private static TelephonyManager getTelephonyManager(Context context) {
        return context.getSystemService(TelephonyManager.class);
    }
}