/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import java.util.HashSet;

import android.net.Uri;

import android.telephony.TelephonyManager;

/**
 * This class acts as a util for ImsRegistration
 * related information.
 *
 */
public class ImsRegistrationUtils {

    public static final int NO_SRV = 0;   /* No service */
    public static final int CS_ONLY = 1;  /* Circuit-switched only */
    public static final int PS_ONLY = 2;  /* Packet-switched only */
    public static final int CS_PS = 3;    /* Circuit-switched and packet-switched */
    public static final int CAMPED = 4;   /* Camped */

    //This is used to indicate that UE is PS attached or not.
    public static final int CODE_IS_PS_ATTACHED = 4001;
    public static final int CODE_IS_NOT_PS_ATTACHED = 4002;

    //Conversion util for Radio Tech from vendor scope to AOSP.
    public static int toTelephonManagerRadioTech(int radioTech) {
        switch (radioTech) {
            case RadioTech.RADIO_TECH_NR5G:
            case RadioTech.RADIO_TECH_LTE:
                return TelephonyManager.NETWORK_TYPE_LTE;
            case RadioTech.RADIO_TECH_WIFI:
            case RadioTech.RADIO_TECH_IWLAN:
            case RadioTech.RADIO_TECH_C_IWLAN:
                return TelephonyManager.NETWORK_TYPE_IWLAN;
            default:
               return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    //Util to extract URIs from strings seperated by '|'
    public static Uri[] extractUrisFromPipeSeparatedUriStrings(String combinedUris) {
        if (combinedUris == null || combinedUris.length() <= 1) return null;
        String[] uriStrings = combinedUris.split("\\|");
        Uri[] uris = new Uri[uriStrings.length];
        for (int i = 0; i < uriStrings.length; i++) {
            uris[i] = Uri.parse(uriStrings[i]);
        }
        return uris;
    }

    //Util to check if URIs in set and array are different.
    public static boolean areSelfIdentityUrisDiff(HashSet<Uri> a, Uri[] b) {
        if (a == null) {
            return b != null;
        }

        if (b == null) {
            return true;
        }

        if (b.length != a.size()) {
            return true;
        }

        for (int i = 0; i < b.length; i++) {
            if (!a.contains(b[i])) {
                return true;
            }
        }
        return false;
    }

    public static int convertToPsAttachedCode(int srvDomain) {
        switch(srvDomain) {
            case ImsRegistrationUtils.PS_ONLY:
            case ImsRegistrationUtils.CS_PS:
                return CODE_IS_PS_ATTACHED;
            default:
                return CODE_IS_NOT_PS_ATTACHED;
        }
    }

}
