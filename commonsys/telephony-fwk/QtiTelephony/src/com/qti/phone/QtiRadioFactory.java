/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.content.Context;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.telephony.TelephonyManager;
import android.util.Pair;

import static java.util.Arrays.copyOf;
/*
 * This is the factory class that creates a HIDL or AIDL IQtiRadio HAL instance
 */
public final class QtiRadioFactory {
    private static final String TAG = "QtiRadioFactory";
    private static IQtiRadioConnectionInterface[] mQtiRadioHidl, mQtiRadioAidl,
            mQtiRadioNotSupportedHal;
    private static Context mContext;
    private static int mPhoneCount;
    private static boolean sIsCellularSupported;
    private static final int DEFAULT_PHONE_COUNT = 1;

    public static final IQtiRadioConnectionInterface[] makeQtiRadio(Context context) {
        mContext = context;
        mPhoneCount = getPhoneCount();
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        boolean noRil = SystemProperties.getBoolean("ro.radio.noril", false);
        sIsCellularSupported = !noRil &&
                (tm.isVoiceCapable() || tm.isSmsCapable() || tm.isDataCapable());
        if (!sIsCellularSupported) {
            Log.i(TAG, "RIL is not supported");
            return makeQtiRadioNotSupportedHal();
        } else if (isAidlAvailable()) {
            return makeQtiRadioAidl();
        } else {
            return makeQtiRadioHidl();
        }
    }

    public static final IQtiRadioConnectionInterface makeQtiRadio(Context context, int phoneId) {
        mContext = context;
        mPhoneCount = getPhoneCount();
        if (!sIsCellularSupported) {
            Log.i(TAG, "RIL is not supported");
            return (new QtiRadioNotSupportedHal());
        } else if (isAidlAvailable()) {
            mQtiRadioAidl = copyOf(mQtiRadioAidl, mPhoneCount);
            if (phoneId >= 0 && phoneId < mQtiRadioAidl.length) {
                mQtiRadioAidl[phoneId] = new QtiRadioAidl(phoneId, mContext);
                return mQtiRadioAidl[phoneId];
            }
            return null;
        } else {
            return (new QtiRadioHidl(phoneId, mContext));
        }
    }

    private static IQtiRadioConnectionInterface[] makeQtiRadioNotSupportedHal() {
        if (mPhoneCount == 0) {
            mPhoneCount =  DEFAULT_PHONE_COUNT;
        }
        mQtiRadioNotSupportedHal = new QtiRadioNotSupportedHal[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mQtiRadioNotSupportedHal[i] = new QtiRadioNotSupportedHal();
        }
        return mQtiRadioNotSupportedHal;
    }

    private static IQtiRadioConnectionInterface[] makeQtiRadioAidl() {
        mQtiRadioAidl = new QtiRadioAidl[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mQtiRadioAidl[i] = new QtiRadioAidl(i, mContext);
        }
        return mQtiRadioAidl;
    }

    static void initQtiRadioAidl() {
        for (int i = 0; i < mQtiRadioAidl.length; i++) {
            mQtiRadioAidl[i].initQtiRadio();
        }
    }

    static void initQtiRadioAidl(int phoneId) {
        if (phoneId >= 0 && phoneId < mQtiRadioAidl.length) {
            mQtiRadioAidl[phoneId].initQtiRadio();
        }
    }

    private static IQtiRadioConnectionInterface[] makeQtiRadioHidl() {
        mQtiRadioHidl = new QtiRadioHidl[mPhoneCount];
        for (int i = 0; i < mPhoneCount; i++) {
            mQtiRadioHidl[i] = new QtiRadioHidl(i, mContext);
        }
        return mQtiRadioHidl;
    }

    private static int makeRadioVersion(int major, int minor) {
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private static boolean isAidlHalAvailable() {
        final int RADIO_HAL_VERSION_1_6 = makeRadioVersion(1, 6); // Radio HAL Version S

        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            Log.e(TAG, "TelephonyManger is NULL");
            return false;
        }
        Pair<Integer, Integer> radioVersion = telephonyManager.getRadioHalVersion();
        int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);

        Log.d(TAG, "isAidlHalAvailable: halVersion = " + halVersion);
        if (halVersion >= RADIO_HAL_VERSION_1_6) {
            return true;
        }
        return false;
    }

    static boolean isAidlAvailable() {
        int apiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        int apiProdLevel = SystemProperties.getInt("ro.product.first_api_level", 0);
        if ((apiLevel  >= Build.VERSION_CODES.S) ||
               ((apiLevel == 0) && isAidlHalAvailable())) {
            try {
                return ServiceManager.isDeclared(
                        "vendor.qti.hardware.radio.qtiradio.IQtiRadioStable/slot1");
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception while call into AIDL: "+ e);
            }
        }
        return false;
    }

    private static int getPhoneCount() {
        TelephonyManager tm = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getActiveModemCount();
    }
}
