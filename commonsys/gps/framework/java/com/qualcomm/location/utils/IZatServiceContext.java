/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

  Not a Contribution
=============================================================================*/
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qualcomm.location.utils;

import android.util.Log;
import android.location.Location;
import android.os.HandlerThread;
import android.os.Looper;
import android.content.Context;
import com.qualcomm.location.izat.IzatService.ISystemEventListener;
import com.qualcomm.location.osagent.OsAgent;
import com.qualcomm.location.izat.flp.FlpServiceProvider;
import com.qti.flp.IFlpService;

import android.os.SystemClock;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;
import android.location.LocationResult;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;

import android.text.TextUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.nio.ByteBuffer;

public class IZatServiceContext {
    //Handler messages base
    public static final int MSG_FLP_BASE =                  0;
    public static final int MSG_NET_INITIATED_BASE =        100;
    public static final int MSG_LOCATION_SERVICE_BASE =     200;
    public static final int MSG_IZAT_PROVIDER_BASE =        300;
    public static final int MSG_NPPROXY_BASE =              400;
    public static final int MSG_OSAGENT_BASE =              500;
    public static final int MSG_RILINFO_MONITOR_BASE =      600;
    public static final int MSG_GEOCODER_PROXY_BASE =       700;
    public static final int MSG_ALTITUDE_RECEIVER =         800;
    public static final int MSG_POLICY_MANAGER_BASE =       900;

    private static final String TAG = "IZatServiceContext";
    public static final int FEATURE_BIT_PRECISE_LOCATION_IS_SUPPORTED   = 0x100;
    public static final int FEATURE_BIT_QWES_WWAN_STANDARD_IS_SUPPORTED = 0x8000;
    public static final int FEATURE_BIT_QWES_WWAN_PREMIUM_IS_SUPPORTED  = 0x10000;
    private static IZatServiceContext sInstance = null;
    private final OsAgent mOsAgent;
    private final Context mContext;
    //** Static flag to track whether the ondevice logging has been initialized or not
    public static boolean sIsDiagJNILoaded = false;
    private int mFlpFeatureMask = -1;

    private final HandlerThread mHandlerThd =
            new HandlerThread(IZatServiceContext.class.getSimpleName());
    private final Looper mLooper;
    private Clock mClock = SystemClock.elapsedRealtimeClock();
    static final long OFFSET_UPDATE_INTERVAL_MS = 60 * 60 * 1000;
    // the percentage that we change the random offset at every interval. 0.0 indicates the random
    // offset doesn't change. 1.0 indicates the random offset is completely replaced every interval
    private static final double CHANGE_PER_INTERVAL = 0.03;  // 3% change
    // weights used to move the random offset. the goal is to iterate on the previous offset, but
    // keep the resulting standard deviation the same. the variance of two gaussian distributions
    // summed together is equal to the sum of the variance of each distribution. so some quick
    // algebra results in the following sqrt calculation to weight in a new offset while keeping the
    // final standard deviation unchanged.
    private static final double NEW_WEIGHT = CHANGE_PER_INTERVAL;
    private static final double OLD_WEIGHT = Math.sqrt(1 - NEW_WEIGHT * NEW_WEIGHT);
    // this number actually varies because the earth is not round, but 111,000 meters is considered
    // generally acceptable
    private static final int APPROXIMATE_METERS_PER_DEGREE_AT_EQUATOR = 111_000;
    // we pick a value 1 meter away from 90.0 degrees in order to keep cosine(MAX_LATITUDE) to a
    // non-zero value, so that we avoid divide by zero errors
    private static final double MAX_LATITUDE =
            90.0 - (1.0 / APPROXIMATE_METERS_PER_DEGREE_AT_EQUATOR);

    private final float mAccuracyM = 2000;
    private final Random mRandom = new SecureRandom();

    private double mLatitudeOffsetM;
    private double mLongitudeOffsetM;
    private long mNextUpdateRealtimeMs;

    private Location mCachedFineLocation;
    private Location mCachedCoarseLocation;

    private LocationResult mCachedFineLocationResult;
    private LocationResult mCachedCoarseLocationResult;


    static {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            try {
                System.loadLibrary("locsdk_diag_jni");
                sIsDiagJNILoaded = true;
            } catch (Throwable e) {
                Log.e(TAG, "Failed to loadLibrary liblocsdk_diag_jni: " + e);
            }
        }
    }
    private IZatServiceContext(Context ctx) {
        mContext = ctx;
        mHandlerThd.start();
        mLooper = mHandlerThd.getLooper();
        mOsAgent = OsAgent.GetInstance(mContext, mLooper);
    }

    public Looper getLooper() {
        return mLooper;
    }

    public Context getContext() {
        return mContext;
    }

    public int getAllSupportedFeatures() {
        if (mFlpFeatureMask == -1) {
            IFlpService flpBinder = FlpServiceProvider.getInstance(mContext).getFlpBinder();
            try {
                mFlpFeatureMask = flpBinder.getAllSupportedFeatures();
            } catch (Exception e) {
                Log.e(TAG, "Failed to call flpBinder.getAllSupportedFeatures()" + e);
            }
        }
        return mFlpFeatureMask;
    }

    public boolean isPreciseLocationSupported() {
        return (getAllSupportedFeatures() & FEATURE_BIT_PRECISE_LOCATION_IS_SUPPORTED) >= 0;
    }

    public synchronized static IZatServiceContext getInstance(Context ctx) {
        if (null == sInstance) {
            sInstance = new IZatServiceContext(ctx);
        }
        return sInstance;
    }

    public void registerSystemEventListener(int sysEventMsgId, ISystemEventListener listener) {
        mOsAgent.registerObserver(sysEventMsgId, listener);
    }

    public void unregisterSystemEventListener(int sysEventMsgId, ISystemEventListener listener) {
        mOsAgent.unregisterObserver(sysEventMsgId, listener);
    }

    public void diagLogBatchedFixes(Location[] locations) {
        if (sIsDiagJNILoaded) {
            native_diag_log_flp_batch(locations);
        }
    }

    public Location[] createCoarse(Location[] fineLocs) {
        Location[] coarseLocs = new Location[fineLocs.length];
        for (int i = 0; i<fineLocs.length; ++i) {
            coarseLocs[i] = createCoarse(fineLocs[i]);
        }
        return coarseLocs;
    }

    // convert to coarse location using two technique, random offsets and snap-to-grid.
    public Location createCoarse(Location fineLoc) {
        synchronized (this) {
            if (fineLoc == mCachedFineLocation || fineLoc == mCachedCoarseLocation) {
                fineLoc = mCachedCoarseLocation;
            }
        }

        // update the offsets in use
        updateOffsets();

        Location coarseLoc = new Location(fineLoc);
        // clear any fields that could leak more detailed location information
        coarseLoc.removeBearing();
        coarseLoc.removeSpeed();
        coarseLoc.removeAltitude();
        coarseLoc.setExtras(null);

        double latitude = wrapLatitude(coarseLoc.getLatitude());
        double longitude = wrapLongitude(coarseLoc.getLongitude());

        // add offsets - update longitude first using the non-offset latitude
        longitude += wrapLongitude(metersToDegreesLongitude(mLongitudeOffsetM, latitude));
        latitude += wrapLatitude(metersToDegreesLatitude(mLatitudeOffsetM));

        // quantize location by snapping to a grid. this is the primary means of obfuscation. it
        // gives nice consistent results and is very effective at hiding the true location (as long
        // as you are not sitting on a grid boundary, which the random offsets mitigate).
        //
        // note that we quantize the latitude first, since the longitude quantization depends on the
        // latitude value and so leaks information about the latitude
        double latGranularity = metersToDegreesLatitude(mAccuracyM);
        latitude = wrapLatitude(Math.round(latitude / latGranularity) * latGranularity);
        double lonGranularity = metersToDegreesLongitude(mAccuracyM, latitude);
        longitude = wrapLongitude(Math.round(longitude / lonGranularity) * lonGranularity);

        coarseLoc.setLatitude(latitude);
        coarseLoc.setLongitude(longitude);
        coarseLoc.setAccuracy(Math.max(mAccuracyM, coarseLoc.getAccuracy()));

        synchronized (this) {
            mCachedFineLocation = fineLoc;
            mCachedCoarseLocation = coarseLoc;
        }
        return coarseLoc;
    }

    private static double wrapLatitude(double latitude) {
        if (latitude > MAX_LATITUDE) {
            latitude = MAX_LATITUDE;
        }
        if (latitude < -MAX_LATITUDE) {
            latitude = -MAX_LATITUDE;
        }
        return latitude;
    }

    private static double wrapLongitude(double longitude) {
        longitude %= 360.0;  // wraps into range (-360.0, +360.0)
        if (longitude >= 180.0) {
            longitude -= 360.0;
        }
        if (longitude < -180.0) {
            longitude += 360.0;
        }
        return longitude;
    }

    private synchronized void updateOffsets() {
        long now = mClock.millis();
        if (now < mNextUpdateRealtimeMs) {
            return;
        }

        mLatitudeOffsetM = (OLD_WEIGHT * mLatitudeOffsetM) + (NEW_WEIGHT * nextRandomOffset());
        mLongitudeOffsetM = (OLD_WEIGHT * mLongitudeOffsetM) + (NEW_WEIGHT * nextRandomOffset());
        mNextUpdateRealtimeMs = now + OFFSET_UPDATE_INTERVAL_MS;
    }

    private double nextRandomOffset() {
        return (mAccuracyM / 4.0) * mRandom.nextGaussian();
    }

    private static double metersToDegreesLatitude(double distance) {
        return distance / APPROXIMATE_METERS_PER_DEGREE_AT_EQUATOR;
    }

    // requires latitude since longitudinal distances change with distance from equator.
    private static double metersToDegreesLongitude(double distance, double lat) {
        return distance / APPROXIMATE_METERS_PER_DEGREE_AT_EQUATOR / Math.cos(Math.toRadians(lat));
    }

    // native diag interface API
    public native void native_diag_log_flp_batch(Location[] locations);

    public String getLicenseeHash(int pid, int uid) {
        String licenseeHash = "";
        int i = 0, j = 0;
        try {
            Log.i(TAG, "getLicenseeHash()");

            if (uid < 0 || pid < 0) return licenseeHash;
            Log.d(TAG, "get LicenseeHash for uid = " + uid + " pid = " + pid);
            String pkgName = mContext.getPackageManager().getNameForUid(uid);
            PackageInfo pkgInfo;

            if (TextUtils.isEmpty(pkgName)) {
                Log.e(TAG, "No package info found for uid:" + uid);
                return licenseeHash;
            }
            Log.d(TAG, "Package name : " + pkgName);

            CertificateFactory cf = CertificateFactory.getInstance("X509");
            PackageManager pm = mContext.getPackageManager();
            pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
            android.content.pm.Signature[] signatures = pkgInfo.signatures;
            byte[] pkgCertBytes;
            X509Certificate packageCertX509;
            InputStream input;
            for (Signature signature : signatures) {
                pkgCertBytes = signature.toByteArray();
                input = new ByteArrayInputStream(pkgCertBytes);
                packageCertX509 = (X509Certificate) cf.generateCertificate(input);
                PublicKey publicKey = packageCertX509.getPublicKey();
                Log.d(TAG, " key algorithm: " + publicKey.getAlgorithm());
                byte[] encodedKey = publicKey.getEncoded();
                MessageDigest sha256  = MessageDigest.getInstance("SHA-256");
                Log.v(TAG, "public key SHA-256 digest: " + HexEncoding.encode(
                        sha256.digest(encodedKey)));
                licenseeHash = HexEncoding.encode(sha256.digest(packageCertX509.getEncoded()));
                Log.v(TAG, "licenseeHash: " + licenseeHash);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Package name not found: " + e.getMessage());
            return licenseeHash;
        } catch (CertificateException e) {
            Log.e(TAG, "Certificate Exception : " + e.getMessage());
            return licenseeHash;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException : " + e.getMessage());
            return licenseeHash;
        } catch (Exception e) {
            Log.e(TAG, "Exception caught : " + e.getMessage());
            return licenseeHash;
        }
        return licenseeHash;
    }
    /**
     * Hexadecimal encoding where each byte is represented by two hexadecimal digits.
     */
    static class HexEncoding {

        /** Hidden constructor to prevent instantiation. */
        private HexEncoding() {}

        private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

        /**
         * Encodes the provided data as a hexadecimal string.
         */
        public static String encode(byte[] data) {
            return encode(data, 0, data.length);
        }

        /**
         * Encodes the provided data as a hexadecimal string.
         */
        public static String encode(byte[] data, int offset, int len) {
            StringBuilder result = new StringBuilder(len * 2);
            for (int i = 0; i < len; i++) {
                byte b = data[offset + i];
                result.append(HEX_DIGITS[(b >>> 4) & 0x0f]);
                result.append(HEX_DIGITS[b & 0x0f]);
            }
            return result.toString();
        }

        /**
         * Encodes the provided data as a hexadecimal string.
         */
        public static String encode(ByteBuffer buf) {
            return encode(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
        }

        /**
         * Decodes the provided hexadecimal string into an array of bytes.
         */
        public static byte[] decode(String encoded) {
            //IMPLEMENTATION NOTE: Special care is taken to permit odd number of hexadecimal digits.
            int resultLengthBytes = (encoded.length() + 1) / 2;
            byte[] result = new byte[resultLengthBytes];
            int resultOffset = 0;
            int encodedCharOffset = 0;
            if ((encoded.length() % 2) != 0) {
                //Odd number of digits -- first digit is the lower 4 bits of the first result byte.
                result[resultOffset++] =
                    (byte) getHexadecimalDigitValue(encoded.charAt(encodedCharOffset));
                encodedCharOffset++;
            }
            for (int len = encoded.length(); encodedCharOffset < len; encodedCharOffset += 2) {
                result[resultOffset++] = (byte)
                    ((getHexadecimalDigitValue(encoded.charAt(encodedCharOffset)) << 4)
                     | getHexadecimalDigitValue(encoded.charAt(encodedCharOffset + 1)));
            }
            return result;
        }

        private static int getHexadecimalDigitValue(char c) {
            if ((c >= 'a') && (c <= 'f')) {
                return (c - 'a') + 0x0a;
            } else if ((c >= 'A') && (c <= 'F')) {
                return (c - 'A') + 0x0a;
            } else if ((c >= '0') && (c <= '9')) {
                return c - '0';
            } else {
                throw new IllegalArgumentException(
                        "Invalid hexadecimal digit at position : '"
                        + c + "' (0x" + Integer.toHexString(c) + ")");
            }
        }
    }
}
