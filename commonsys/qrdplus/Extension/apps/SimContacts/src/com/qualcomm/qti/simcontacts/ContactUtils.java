/*
* Copyright (c) 2019 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package com.qualcomm.qti.simcontacts;

import android.content.Context;
import android.util.Log;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.uicc.AdnCapacity;

import java.util.Arrays;

/**
 * Shared static SIM contact methods.
 */
public class ContactUtils {

    private static final String TAG = "ContactUtils";

    private static AdnCapacity getAdnRecordsCapacity(int subId) {
        return getAdnRecordsCapacityForSubscriber(subId);
    }

    public static boolean canSaveAdn(int subId) {
        return getAdnRecordsCapacity(subId).getMaxAdnCount() > 0;
    }

    /**
     * Returns the subscription's card can save anr or not.
     */
    public static boolean canSaveAnr(int subId) {
        return getAdnRecordsCapacity(subId).getMaxAnrCount() > 0;
    }

    /**
     * Returns the subscription's card can save email or not.
     */
    public static boolean canSaveEmail(int subId) {
        return getAdnRecordsCapacity(subId).getMaxEmailCount() > 0;
    }

    public static int getOneSimAnrCount(int subId) {
        int count = 0;
        AdnCapacity adnCapacity = getAdnRecordsCapacity(subId);
        int anrCount = adnCapacity.getMaxAnrCount();
        int adnCount = adnCapacity.getMaxAdnCount();
        if (adnCount > 0) {
            count = anrCount % adnCount != 0 ? (anrCount / adnCount + 1)
                    : (anrCount / adnCount);
        }
        return count;
    }

    public static int getOneSimEmailCount(int subId) {
        int count = 0;
        AdnCapacity adnCapacity = getAdnRecordsCapacity(subId);
        int emailCount = adnCapacity.getMaxEmailCount();
        int adnCount = adnCapacity.getMaxAdnCount();
        if (adnCount > 0) {
            count = emailCount % adnCount != 0 ? (emailCount / adnCount + 1)
                    : (emailCount / adnCount);
        }
        return count;
    }

    public static int getSimFreeCount(int subId) {
        AdnCapacity adnCapacity = getAdnRecordsCapacity(subId);
        int count = adnCapacity.getMaxAdnCount()-adnCapacity.getUsedAdnCount();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "spare adn:" + count);
        }
        return count;
    }

    public static int getSpareAnrCount(int subId) {
        AdnCapacity adnCapacity = getAdnRecordsCapacity(subId);
        int spareCount = adnCapacity.getMaxAnrCount()-adnCapacity.getUsedAnrCount();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "spare anr:" + spareCount);
        }
        return spareCount;
    }

    public static int getSpareEmailCount(int subId) {
        AdnCapacity adnCapacity = getAdnRecordsCapacity(subId);
        int spareCount = adnCapacity.getMaxEmailCount()-adnCapacity.getUsedEmailCount();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "spare email:" + spareCount);
        }
        return spareCount;
    }

    private static AdnCapacity getAdnRecordsCapacityForSubscriber(int subId) {
        AdnCapacity defaultCapacity = new AdnCapacity(
            500, 0, 200, 0, 500, 0, 14, 42, 40, 15);
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub
                    .asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                AdnCapacity capacity = iccIpb.getAdnRecordsCapacityForSubscriber(subId);
                if (capacity != null)
                    return capacity;
            }
        }catch (RemoteException e){
            Log.e(TAG," getAdnRecordsCapacityForSubscriber error = "+e.toString());
        }
        return defaultCapacity;
    }
}
