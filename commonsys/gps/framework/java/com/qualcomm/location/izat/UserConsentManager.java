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
package com.qualcomm.location.izat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.UserManager;
import android.os.Binder;
import android.app.ActivityManager;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.OnPermissionsChangedListener;

import com.qualcomm.location.izat.IzatService.ISsrNotifier;
import com.qualcomm.location.izat.IzatService.SsrHandler;
import com.qualcomm.location.osagent.OsAgent;
import com.qualcomm.location.utils.IZatServiceContext;

import com.qualcomm.location.policy.SessionRequest;
import com.qualcomm.location.policy.SessionRequest.RequestPrecision;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 Purpose: This module would be responsible for receiving, aggregating, and restoring
   the user consent flag received from different modules, such as GtpServiceProvider,
   and GnssConfigService.
 */
public class UserConsentManager implements ISsrNotifier, IzatService.ISystemEventListener,
                                           OnPermissionsChangedListener {

    private static final String TAG = "UserConsentManager";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private static UserConsentManager sInstance = null;

    private final Context mContext;
    private IZatServiceContext mIZatServiceCtx;
    private final OsAgent mOsAgent;

    private boolean mIsIzatNetworkProviderLoaded = false;

    // Class capturing the details of a consent provider
    private static class PackageConsentProvider {
        String mPackageName;
        int mUserId;
        int mCurrentUser;

        public PackageConsentProvider(String packageName, int userId) {
            mPackageName = packageName;
            mUserId = userId;
            mCurrentUser = -1;
        }
        public PackageConsentProvider(String packageName, int userId, int currentUser) {
            mPackageName = packageName;
            mUserId = userId;
            mCurrentUser = currentUser;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof PackageConsentProvider)) return false;
            PackageConsentProvider provider = (PackageConsentProvider)obj;
            return (provider != null && mPackageName != null &&
                    provider.mPackageName.equals(mPackageName) &&
                    (provider.mUserId == mUserId || provider.mCurrentUser == mCurrentUser));
        }
    }
    private final List<PackageConsentProvider> mPackageConsentProviders =
            new ArrayList<PackageConsentProvider>();

    // Class capturing the details of a WWAN app
    public static class WwanAppInfo {
        public String mPackageName;
        public int mUserId;
        public int mCurrentUser;
        public String mCookie;
        public String mAppHash;
        public boolean mHasPreciseLicense;
        public boolean mHasStandardLicense;
        public boolean mHasFineLocationPermission;
        public boolean mHasCoarseLocationPermission;
        public boolean mHasBgLocationPermission;

        public WwanAppInfo(String packageName, int userId, int currentUser, String cookie,
                String appHash, boolean hasPreciseLicense,
                boolean hasStandardLicense, boolean hasFineLocationPermission,
                boolean hasCoarseLocationPermission, boolean hasBgLocationPermission) {
            mPackageName = packageName;
            mUserId = userId;
            mCurrentUser = currentUser;
            mCookie = cookie;
            mAppHash = appHash;
            mHasPreciseLicense = hasPreciseLicense;
            mHasStandardLicense = hasStandardLicense;
            mHasFineLocationPermission = hasFineLocationPermission;
            mHasCoarseLocationPermission = hasCoarseLocationPermission;
            mHasBgLocationPermission = hasBgLocationPermission;
        }
        public WwanAppInfo(WwanAppInfo appInfo) {
            mPackageName = appInfo.mPackageName;
            mUserId = appInfo.mUserId;
            mCurrentUser = appInfo.mCurrentUser;
            mCookie = appInfo.mCookie;
            mAppHash = appInfo.mAppHash;
            mHasPreciseLicense = appInfo.mHasPreciseLicense;
            mHasStandardLicense = appInfo.mHasStandardLicense;
            mHasFineLocationPermission = appInfo.mHasFineLocationPermission;
            mHasCoarseLocationPermission = appInfo.mHasCoarseLocationPermission;
            mHasBgLocationPermission = appInfo.mHasBgLocationPermission;
        }

        public void update(WwanAppInfo info) {
            if (info.mHasPreciseLicense) {
                mHasPreciseLicense = true;
            }
            if (info.mHasStandardLicense) {
                mHasStandardLicense = true;
            }
            mHasFineLocationPermission = info.mHasFineLocationPermission;
            mHasCoarseLocationPermission = info.mHasCoarseLocationPermission;
            mHasBgLocationPermission = info.mHasBgLocationPermission;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof WwanAppInfo)) return false;
            WwanAppInfo appInfo = (WwanAppInfo)obj;
            return (appInfo != null && mPackageName != null &&
                    appInfo.mPackageName.equals(mPackageName) &&
                    (appInfo.mUserId == mUserId || appInfo.mCurrentUser == mCurrentUser));
        }

        public boolean compareAllFields(WwanAppInfo appInfo) {
            return (appInfo != null &&
                appInfo.mPackageName.equals(mPackageName) &&
                appInfo.mUserId == mUserId &&
                appInfo.mCurrentUser == mCurrentUser &&
                appInfo.mCookie.equals(mCookie) &&
                appInfo.mAppHash.equals(mAppHash) &&
                appInfo.mHasPreciseLicense == mHasPreciseLicense &&
                appInfo.mHasStandardLicense == mHasStandardLicense &&
                appInfo.mHasFineLocationPermission == mHasFineLocationPermission &&
                appInfo.mHasCoarseLocationPermission == mHasCoarseLocationPermission &&
                appInfo.mHasBgLocationPermission == mHasBgLocationPermission);
        }

        public boolean comparePermissions(WwanAppInfo appInfo) {
            return (appInfo != null &&
                appInfo.mHasFineLocationPermission == mHasFineLocationPermission &&
                appInfo.mHasBgLocationPermission == mHasBgLocationPermission);
        }
    }

    private final List<WwanAppInfo> mWwanAppInfoList = new ArrayList<WwanAppInfo>();

    // User Consent Listener Interface
    public interface UserConsentListener {
        // User consent is updated for current user, listener can query via getUserConsent
        public void onUserConsentUpdated();
    }
    private final List<UserConsentListener> mUserConsentListeners =
            new ArrayList<UserConsentListener>();

    // Wwan App Info Listener Interface
    public interface WwanAppInfoListener {
        // WWAN App Info list is updated, listener can query via getWwanAppInfoList
        public void onWwanAppInfoListUpdated(List<WwanAppInfo> wwanAppInfoList);
    }
    private final List<WwanAppInfoListener> mWwanAppInfoListeners =
            new ArrayList<WwanAppInfoListener>();

    private void logv(String msg) {
        if (VERBOSE) {
            Log.v(TAG, msg);
        }
    }
    private void logi(String msg) {
        Log.i(TAG, msg);
    }
    private void loge(String msg) {
        Log.e(TAG, msg);
    }

    private UserConsentManager(Context ctx) {

        mContext = ctx;
        mIZatServiceCtx = IZatServiceContext.getInstance(mContext);
        mOsAgent = OsAgent.GetInstance(ctx, mIZatServiceCtx.getLooper());

        if (UserManager.supportsMultipleUsers()) {
            mIZatServiceCtx.registerSystemEventListener(MSG_PKG_REMOVED, this);
            mIZatServiceCtx.registerSystemEventListener(MSG_USER_SWITCH_ACTION_UPDATE, this);
        }

        mContext.getPackageManager().addOnPermissionsChangeListener(this);
    }
    public synchronized static UserConsentManager getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new UserConsentManager(ctx);
        }
        return sInstance;
    }

    @Override
    public void notify(int msgId, Object... args) {

        if (msgId == MSG_PKG_REMOVED) {
            Intent intent = (Intent)args[0];
            Uri uri = intent.getData();
            String packageName = uri != null ? uri.getSchemeSpecificPart() : null;
            if (packageName != null) {
                boolean removed = mPackageConsentProviders.removeIf(
                    p -> p.mPackageName.equals(packageName));
                if (removed) {
                    handleConsentUpdate();
                }

                boolean removedAppInfo = mWwanAppInfoList.removeIf(
                    appInfo -> appInfo.mPackageName.equals(packageName));
                if (removedAppInfo) {
                    handleWwanAppInfoListUpdate();
                }
            } else {
                loge("Got null package name on PKG_REMOVED intent");
            }
        }

        if (msgId == MSG_USER_SWITCH_ACTION_UPDATE) {
            mOsAgent.sendConsolidatedUserConsent(getPackageUserConsent() || getAospUserConsent());
        }
    }

    @Override
    public void bootupAndSsrNotifier(String savedState) {
        logi("bootupAndSsrNotifier: " + savedState);

        restoreState(savedState);
        mOsAgent.sendConsolidatedUserConsent(getPackageUserConsent() || getAospUserConsent());

        notifyWwanAppInfoListListeners();
    }

    @Override
    public void onPermissionsChanged(int uid) {
        logi("onPermissionsChanged: " + uid);
        boolean infoUpdated = false;
        PackageManager packageManager = mContext.getPackageManager();

        for (WwanAppInfo info: mWwanAppInfoList) {
            if (info.mUserId == uid) {

                info.mHasFineLocationPermission = packageManager.checkPermission(
                    "android.permission.ACCESS_FINE_LOCATION", info.mPackageName) ==
                    PackageManager.PERMISSION_GRANTED;

                info.mHasCoarseLocationPermission = packageManager.checkPermission(
                    "android.permission.ACCESS_COARSE_LOCATION", info.mPackageName) ==
                    PackageManager.PERMISSION_GRANTED;

                info.mHasBgLocationPermission = packageManager.checkPermission(
                    "android.permission.ACCESS_BACKGROUND_LOCATION", info.mPackageName) ==
                    PackageManager.PERMISSION_GRANTED;

                logi("pkg: " + info.mPackageName +
                     ", finePerm: " + info.mHasFineLocationPermission +
                     ", coarsePerm: " + info.mHasCoarseLocationPermission +
                     ", bgPerm: " + info.mHasBgLocationPermission);

                infoUpdated = true;
            }
        }

        if (infoUpdated) {
            handleWwanAppInfoListUpdate();
        }
    }

    public void setIsIzatNetworkProviderLoaded(boolean isIzatNetworkProviderLoaded) {
        mIsIzatNetworkProviderLoaded = isIzatNetworkProviderLoaded;
    }

    // User Consent API for controlling AOSP sessions.
    public void setAospUserConsent(boolean consent) {

        if (!mIsIzatNetworkProviderLoaded) {
            loge("Ignoring setAospUserConsent since IzatProvider is not NetworkLocationProvider");
            return;
        }

        boolean existingConsent = getAospUserConsent();
        int currentUser = ActivityManager.getCurrentUser();
        int callingUid = Binder.getCallingUid();

        logi("setAospUserConsent [" + callingUid + "][" + currentUser + "] = " +
                existingConsent + "->" + consent);

        if (consent != existingConsent) {

            PackageConsentProvider provider = new PackageConsentProvider(
                "AOSP", callingUid, currentUser);
            boolean providerInList = mPackageConsentProviders.contains(provider);
            // Add/remove provider from list based on consent
            if (!providerInList && consent) {
                mPackageConsentProviders.add(provider);
            } else if (providerInList && !consent) {
                mPackageConsentProviders.remove(provider);
            }

            handleConsentUpdate();

            // Need to multiplex app info on user consent update
            notifyWwanAppInfoListListeners();
        }
    }

    // Return AOSP consent for current user
    public boolean getAospUserConsent() {

        return mPackageConsentProviders.contains(new PackageConsentProvider(
            "AOSP", Binder.getCallingUid(), ActivityManager.getCurrentUser()));
    }

    // User Consent API for controlling individual application sessions (per application)
    public void setPackageUserConsent(boolean consent, String packageName, int userId) {

        int currentUser = ActivityManager.getCurrentUser();

        logi("setPackageUserConsent [" + packageName + "][" + userId + "][" + currentUser +
                "] = " + consent);

        if (packageName == null || userId == 0 || packageName.trim().length() == 0) {
            loge("Invalid packageName " + packageName + " or user id " + userId);
            return;
        }

        PackageConsentProvider provider = new PackageConsentProvider(
            packageName, userId, currentUser);
        boolean providerInList = mPackageConsentProviders.contains(provider);

        // Add/remove provider from list based on consent
        if (!providerInList && consent) {
            mPackageConsentProviders.add(provider);
            handleConsentUpdate();
        } else if (providerInList && !consent) {
            mPackageConsentProviders.remove(provider);
            handleConsentUpdate();
        }

        // Need to multiplex app info on user consent update
        notifyWwanAppInfoListListeners();
    }

    // Return consolidated package user consent.
    public boolean getPackageUserConsent() {

        int currentUser = ActivityManager.getCurrentUser();
        for (PackageConsentProvider provider: mPackageConsentProviders) {
            if (!provider.mPackageName.equals("AOSP") && provider.mCurrentUser == currentUser) {
                return true;
            }
        }
        return false;
    }

    // Return per package user consent.
    public boolean getPackageUserConsent(String packageName, int userId) {

        return mPackageConsentProviders.contains(new PackageConsentProvider(packageName, userId));
    }

    public void setWwanAppInfo(String packageName, int pid, int uid,
            String appHash, SessionRequest.SessionType sessionType,
            SessionRequest.RequestPrecision sessionPrecision) {

        int currentUser = ActivityManager.getCurrentUser();

        boolean finePermission = mContext.checkPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION, pid, uid) ==
            PackageManager.PERMISSION_GRANTED;

        boolean coarsePermission = mContext.checkPermission(
            android.Manifest.permission.ACCESS_COARSE_LOCATION, pid, uid) ==
            PackageManager.PERMISSION_GRANTED;

        boolean bgPermission = mContext.checkPermission(
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION, pid, uid) ==
            PackageManager.PERMISSION_GRANTED;

        logi("setWwanAppInfo [" + packageName + "][" + uid + "][" + currentUser +
                "][" + sessionType + "][" + sessionPrecision + "] = [fine:" + finePermission +
                "][coarse: " + coarsePermission + "][bg:" + bgPermission + "]");

        if (packageName == null || uid == 0 || packageName.trim().length() == 0) {
            loge("Invalid packageName " + packageName + " or uid " + uid);
            return;
        }

        WwanAppInfo appInfo = new WwanAppInfo(packageName, uid, currentUser,
            generateAppCookie(), appHash,
            (sessionPrecision == RequestPrecision.REQUEST_PRECISION_FINE),
            (sessionPrecision == RequestPrecision.REQUEST_PRECISION_COARSE),
            finePermission, coarsePermission, bgPermission);

        int appInfoIdx = mWwanAppInfoList.indexOf(appInfo);

        // Add/update app info from list
        if (appInfoIdx < 0) {
            mWwanAppInfoList.add(appInfo);
        } else {
            WwanAppInfo existingInfo = mWwanAppInfoList.get(appInfoIdx);
            existingInfo.update(appInfo);
        }

        handleWwanAppInfoListUpdate();
    }

    public void removeWwanAppInfo(String packageName) {

        mWwanAppInfoList.removeIf(
            w -> w.mPackageName.equals(packageName));
    }

    // Register a listener for any user consent update
    public void registerUserConsentListener(UserConsentListener listener) {

        if (!mUserConsentListeners.contains(listener)) {
            mUserConsentListeners.add(listener);
        }
    }
    // Register a listener for any wwan app info update
    public void registerWwanAppInfoListener(WwanAppInfoListener listener) {

        if (!mWwanAppInfoListeners.contains(listener)) {
            mWwanAppInfoListeners.add(listener);
        }
    }

    private String generateAppCookie() {
        return UUID.randomUUID().toString();
    }

    private void handleConsentUpdate() {
        persistState();
        notifyUserConsentListeners();
        mOsAgent.sendConsolidatedUserConsent(getPackageUserConsent() || getAospUserConsent());
    }
    private void handleWwanAppInfoListUpdate() {
        persistState();
        notifyWwanAppInfoListListeners();
    }

    private void notifyUserConsentListeners() {

        for (UserConsentListener listener: mUserConsentListeners) {
            listener.onUserConsentUpdated();
        }
    }
    private void notifyWwanAppInfoListListeners() {

        for (WwanAppInfoListener listener: mWwanAppInfoListeners) {
            listener.onWwanAppInfoListUpdated(mWwanAppInfoList);
        }
    }

    private void persistState() {

        StringBuilder sb = new StringBuilder();
        for (PackageConsentProvider provider: mPackageConsentProviders) {
            sb.append(provider.mPackageName);
            sb.append(":");
            sb.append(provider.mUserId);
            sb.append(":");
            sb.append(provider.mCurrentUser);
            sb.append(" ");
        }

        if (mWwanAppInfoList.size() > 0) {

            sb.append("&&&&");

            for (WwanAppInfo appInfo: mWwanAppInfoList) {
                sb.append(appInfo.mPackageName);
                sb.append(":");
                sb.append(appInfo.mUserId);
                sb.append(":");
                sb.append(appInfo.mCurrentUser);
                sb.append(":");
                sb.append(appInfo.mCookie);
                sb.append(":");
                sb.append(appInfo.mAppHash);
                sb.append(":");
                sb.append(appInfo.mHasPreciseLicense? "1":"0");
                sb.append(":");
                sb.append(appInfo.mHasStandardLicense? "1":"0");
                sb.append(" ");
            }
        }

        String stateStr = sb.toString().trim();
        logi("Saving state: [" + stateStr + "]");
        SsrHandler.get().registerDataForSSREvents(
            mContext, UserConsentManager.class.getName(), stateStr);
    }

    private void restoreState(String persistedState) {

        if (mPackageConsentProviders.size() > 0) {
            loge("restoreState: providers list is not empty, can't restore.");
            return;
        }
        if (mWwanAppInfoList.size() > 0) {
            loge("restoreState: wwan app info list is not empty, can't restore.");
            return;
        }

        if (persistedState != null && persistedState.trim().length() > 0) {

            String[] providersAndWwanAppInfo = persistedState.split("&&&&");
            String providerList = providersAndWwanAppInfo[0];
            String wwanAppInfoList = null;
            if (providersAndWwanAppInfo.length > 1) {
                wwanAppInfoList = providersAndWwanAppInfo[1];
            }

            if (providerList.trim().length() > 0) {

                String[] providers = providerList.split(" ");
                for (String provider: providers) {
                    String[] pNameAndUid = provider.split(":");
                    String packageName = pNameAndUid[0];
                    String userId = pNameAndUid[1];
                    String currentUser = pNameAndUid[2];
                    mPackageConsentProviders.add(new PackageConsentProvider(
                        packageName, Integer.parseInt(userId), Integer.parseInt(currentUser)));
                }
            }

            if (wwanAppInfoList != null && wwanAppInfoList.trim().length() > 0) {

                String[] wwanAppInfoArr = wwanAppInfoList.split(" ");
                for (String appInfo: wwanAppInfoArr) {
                    String[] appInfoElements = appInfo.split(":");
                    String packageName = appInfoElements[0];
                    String userId = appInfoElements[1];
                    String currentUser = appInfoElements[2];
                    String cookie = appInfoElements[3];
                    String appHash = appInfoElements[4];
                    String hasPreciseLicense = appInfoElements[5];
                    String hasStandardLicense = appInfoElements[6];

                    PackageManager packageManager = mContext.getPackageManager();

                    boolean finePermission = packageManager.checkPermission(
                        "android.permission.ACCESS_FINE_LOCATION", packageName) ==
                        PackageManager.PERMISSION_GRANTED;

                    boolean coarsePermission = packageManager.checkPermission(
                        "android.permission.ACCESS_COARSE_LOCATION", packageName) ==
                        PackageManager.PERMISSION_GRANTED;

                    boolean bgPermission = packageManager.checkPermission(
                        "android.permission.ACCESS_BACKGROUND_LOCATION", packageName) ==
                        PackageManager.PERMISSION_GRANTED;

                    logi("wwan app pkg " + packageName + ", fineperm: " + finePermission +
                         ", bgPermission:" + bgPermission);

                    mWwanAppInfoList.add(new WwanAppInfo(
                        packageName, Integer.parseInt(userId), Integer.parseInt(currentUser),
                        cookie, appHash,
                        (Integer.parseInt(hasPreciseLicense) == 1),
                        (Integer.parseInt(hasStandardLicense) == 1),
                        finePermission, coarsePermission, bgPermission));
                }
            }
        }
    }
}
