/******************************************************************************
 * ---------------------------------------------------------------------------
 *  Copyright (c) 2015-2017, 2020-2024 Qualcomm Technologies, Inc.
 *  All Rights Reserved.
 *  Confidential and Proprietary - Qualcomm Technologies, Inc.
 * ---------------------------------------------------------------------------
 *******************************************************************************/
package com.qualcomm.location.osagent;

import android.util.Log;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.os.Build;
import android.os.UserHandle;
import android.os.Binder;
import android.location.LocationManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observer;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Date;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.database.ContentObserver;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.provider.Settings.Global;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.provider.Telephony.Sms.Intents;
import android.app.ActivityManager;
import android.text.TextUtils;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;

import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.izat.IzatService.ISystemEventListener;
import com.qualcomm.location.izatprovider.IzatProvider;

import android.os.RemoteException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.qualcomm.location.idlclient.*;
import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import com.qualcomm.location.GpsNetInitiatedHandler;
import com.qualcomm.location.utils.IZatServiceContext;
import vendor.qti.gnss.ILocAidlGnss;
import vendor.qti.gnss.ILocAidlIzatSubscription;
import vendor.qti.gnss.ILocAidlIzatSubscriptionCallback;
import vendor.qti.gnss.LocAidlBoolDataItem;
import vendor.qti.gnss.LocAidlStringDataItem;
import vendor.qti.gnss.LocAidlTimeZoneChangeDataItem;
import vendor.qti.gnss.LocAidlTimeChangeDataItem;
import vendor.qti.gnss.LocAidlWifiSupplicantStatusDataItem;
import com.qualcomm.location.izat.GTPClientHelper;

public class OsAgent
{
    private static OsAgent mInstance;
    private static final String TAG = "OsAgent";
    private static final Object mLock = new Object();
    private boolean mLastWifiScanAvail = false;

    public static OsAgent GetInstance(Context context, Looper looperObj) {
        synchronized (mLock) {
            if (null == mInstance) {
                mInstance = new OsAgent(context, looperObj);
            }
        }
        return mInstance;
    }
    private OsAgent(Context context, Looper looperObj)
    {
        logv("OSAgent constructor");

        mContext = context;
        // initialize the msg handler
        mHandler = new Handler(looperObj, m_handler_callback);

        mLocationMgr = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSubscriptionMgr = (SubscriptionManager)mContext.getSystemService(
                    Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        // OsAgent init operations
        Message msgInit = Message.obtain(mHandler, MSG_OSAGENT_INIT);
        mHandler.sendMessage(msgInit);
    }

    public void subscribe(int[] dataItemArray)
    {
        logv("OSAgent subscribe.... +");

        if (dataItemArray == null) {
            loge("dataItemArray received is NULL");
            return;
        }

        int[] dataItemsList = (int[]) dataItemArray;
        for (int index = 0; index < dataItemArray.length ; ++index)
        {
            logd("OSAgent subscribe:: " + dataItemsList[index]);
        }

        Message msgObj = Message.obtain(mHandler, MSG_DATAITEM_SUBSCRIBE, dataItemArray);
        mHandler.sendMessage(msgObj);
    }

    public void requestData(int[] dataItemArray)
    {
        logv("OSAgent request data.... +");

        if (dataItemArray == null) {
            loge("dataItemArray received is NULL");
            return;
        }

        int[] dataItemsList = (int[]) dataItemArray;
        for (int index = 0; index < dataItemArray.length ; ++index)
        {
            logd("OSAgent request data:: " + dataItemsList[index]);
        }

        Message msgObj = Message.obtain(mHandler, MSG_DATAITEM_REQUEST_DATA, dataItemArray);
        mHandler.sendMessage(msgObj);
    }

    public void unsubscribe(int[] dataItemArray)
    {
       logv("OSAgent unsubscribe.... +");

        if (dataItemArray == null) {
            loge("dataItemArray received is NULL");
            return;
        }

        int[] dataItemsList = (int[]) dataItemArray;
        for (int index = 0; index < dataItemArray.length ; ++index)
        {
            logd("OSAgent unsubscribe:: " + dataItemsList[index]);
        }

        Message msgObj = Message.obtain(mHandler, MSG_DATAITEM_UNSUBSCRIBE, dataItemArray);
        mHandler.sendMessage(msgObj);
    }

    public void unsubscribeAll()
    {
        logv("OSAgent unsubscribeAll.... +");

        Message msgObj = Message.obtain(mHandler, MSG_DATAITEM_UNSUBSCRIBE_ALL);
        mHandler.sendMessage(msgObj);
    }

    private void updateOptinStatus(boolean status) {
        logd("optin enable status: " + status);
        Settings.Secure.putInt(mContext.getContentResolver(),
            SKYHOOK_LOCATION_ENABLED, status ? 1 : 0);
    }

    // Embargo bit check supported from AIDL V2 impl
    private boolean getNativeEmbargoCheckSupported() {
        IDLServiceVersion ver = mIdlClient.getIDLServiceVersion();
        boolean supported = false;
        if (ver.ordinal() >= IDLServiceVersion.V_AIDL_2_0.ordinal()) {
            supported = true;
        }
        return supported;
    }
    // Handles two opt-in cases
    // System only upgrade to Android U:
    //      Use the GtpHelperClient to send the force-optin to XT app and compute opt-in.
    // System and vendor upgrade to Android U:
    //      Only updates user consent bit to vendor.
    //      and final optin is computed at vendor side and sent back to OsAgent
    public void sendConsolidatedUserConsent(boolean userConsent) {

        logi("consolidated User Consent: existing " + mUserConsent + ", incoming: " + userConsent);

        if (mUserConsent != userConsent) {
            mUserConsent = userConsent;
            if (!getNativeEmbargoCheckSupported()) {
                GTPClientHelper.SetClientRegistrationStatus(mContext,
                        GTPClientHelper.GTP_CLIENT_WIFI_PROVIDER, null, mUserConsent);

            } else {
                List<Integer> dataItemList = new ArrayList<Integer>();
                dataItemList.add(ENH_DATA_ITEM_ID);
                if (dataItemList.size() > 0) {
                    mHandler.obtainMessage(MSG_CONTENT_DATA_CHANGED, dataItemList).sendToTarget();
                }
            }
        }
    }

    public void handleSubscribe(Object dataItemsArray)
    {
        int[] dataItems = (int[]) dataItemsArray;

        for (int index = 0; index < dataItems.length; ++index)
        {
            if (mDataItemList.contains(dataItems[index])) {
                // data item was already registered for
                continue;
            }

            // insert the data item
            mDataItemList.add(dataItems[index]);

            switch (dataItems[index])
            {
                case WIFIHARDWARESTATE_DATA_ITEM_ID:
                    // listen to the new ACTION_WIFI_SCAN_AVAILABILITY_CHANGED
                    logi("register listener on ACTION_WIFI_SCAN_AVAILABILITY_CHANGED");
                    IntentFilter scanFilter = new IntentFilter();
                    scanFilter.addAction(WifiManager.ACTION_WIFI_SCAN_AVAILABILITY_CHANGED);
                    mContext.registerReceiver(mWifiScanAvailChangeReceiver, scanFilter, null, null);
                    // no break here, intentionally
                break;
                case ENH_DATA_ITEM_ID:
                case GPSSTATE_DATA_ITEM_ID:
                    mContentSettingsList.add(dataItems[index]);
                break;
                case MODEL_DATA_ITEM_ID:
                    if (! Build.MODEL.equals(Build.UNKNOWN)) {
                        if (mIdlClient != null) {
                            mIdlClient.string_dataitem_update(MODEL_DATA_ITEM_ID, Build.MODEL);
                        }
                    }
                break;
                case MANUFACTURER_DATA_ITEM_ID:
                    if (! Build.MANUFACTURER.equals(Build.UNKNOWN)) {
                        if (mIdlClient != null) {
                            mIdlClient.string_dataitem_update(MANUFACTURER_DATA_ITEM_ID,
                                    Build.MANUFACTURER);
                        }
                    }
                break;
                case TIMEZONE_CHANGE_DATA_ITEM_ID:
                {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                    mContext.registerReceiver(mTimeZoneChangeReceiver, filter, null, null);
                }
                break;

                case TIME_CHANGE_DATA_ITEM_ID:
                {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Intent.ACTION_TIME_CHANGED);
                    mContext.registerReceiver(mTimeChangeReceiver, filter, null, null);
                }
                break;

                case WIFI_SUPPLICANT_STATUS_DATA_ITEM_ID:
                {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                    mContext.registerReceiver(mWifiSupplicantStateChangeReceiver, filter, null, null);
                }
                break;

                case MCCMNC_DATA_ITEM_ID:
                {
                    if (mSubscriptionsChangedListener == null) {
                        mSubscriptionsChangedListener = new OnSubscriptionsChangedListener(
                                mHandler.getLooper()) {
                            @Override
                            public void onSubscriptionsChanged() {
                               updateMccmnc();
                            }
                        };

                        mSubscriptionMgr.addOnSubscriptionsChangedListener(
                                mSubscriptionsChangedListener);
                    }

                    updateMccmnc();
                }
                break;
            }
        }

        if (mContentSettingsList.size() > 0) {
            subscribeForNewContentData();
        }
    }

    public void handleRequestData(Object dataItemsArray)
    {
        int[] dataItems = (int[]) dataItemsArray;
        for (int index = 0; index < dataItems.length; ++index)
        {
            switch (dataItems[index])
            {
                case TIMEZONE_CHANGE_DATA_ITEM_ID:
                    sendTimeZoneInfo();
                break;

                case TIME_CHANGE_DATA_ITEM_ID:
                    sendTimeInfo();
                break;
            }
        }
    }

    public void handleUnsubscribe(Object dataItemsArray)
    {
        int[] dataItems = (int[]) dataItemsArray;

        // populate the required items in these local arrays
        List<Integer> contentDataItemArray = new ArrayList<Integer> ();

        for (int index = 0; index < dataItems.length; ++index)
        {
            if (!mDataItemList.contains(dataItems[index])) {
                // dataitem was never added
                continue;
            }

            // remove the data item
            mDataItemList.remove((Integer)dataItems[index]);
            switch (dataItems[index])
            {
                case WIFIHARDWARESTATE_DATA_ITEM_ID:
                    mContext.unregisterReceiver(mWifiScanAvailChangeReceiver);
                break;
                case TIMEZONE_CHANGE_DATA_ITEM_ID:
                    mContext.unregisterReceiver(mTimeZoneChangeReceiver);
                break;
                case TIME_CHANGE_DATA_ITEM_ID:
                    mContext.unregisterReceiver(mTimeChangeReceiver);
                break;
                case WIFI_SUPPLICANT_STATUS_DATA_ITEM_ID:
                    mContext.unregisterReceiver(mWifiSupplicantStateChangeReceiver);
                break;
                case MCCMNC_DATA_ITEM_ID:
                    if (mSubscriptionsChangedListener != null) {
                        mSubscriptionMgr.removeOnSubscriptionsChangedListener(
                                mSubscriptionsChangedListener);
                        mSubscriptionsChangedListener = null;
                    }
                break;
            }
        }

        if (contentDataItemArray.size() > 0) {
            removeUpdateForContentData(contentDataItemArray);
        }
    }

    public void handleUnsubscribeAll()
    {
        int size = mDataItemList.size();

        int[] dataItemArray = new int[size];

        for (int i = 0; i < size; i++) {
            dataItemArray[i] = mDataItemList.get(i).intValue();
        }

        handleUnsubscribe(dataItemArray);
        mDataItemList.clear();
        mContentSettingsList.clear();
    }

    private void subscribeForNewContentData()
    {
        List<Integer> dataItemListForUpdate = new ArrayList<Integer>();

        if (mContentObserver == null) {
            mContentObserver = new DataItemContentObserver(null);
        }

        for (Integer newdataItem : mContentSettingsList) {
            switch (newdataItem)
            {
                case ENH_DATA_ITEM_ID:
                       if (!getNativeEmbargoCheckSupported()) {
                    Uri eulaUri = Settings.Secure.getUriFor(ENH_LOCATION_SERVICES_ENABLED);
                    if (eulaUri != null) {
                        mContext.getContentResolver().registerContentObserver(
                                eulaUri, true, mContentObserver, UserHandle.USER_ALL);
                        dataItemListForUpdate.add(ENH_DATA_ITEM_ID);
                    } else {
                        loge("getUriFor(ENH_LOCATION_SERVICES_ENABLED) returned null");
                    }
                 } else {
                     dataItemListForUpdate.add(ENH_DATA_ITEM_ID);
                 }
                break;
                case GPSSTATE_DATA_ITEM_ID:
                default:
                    loge("Unsupported data item");

            }
        }

        if (dataItemListForUpdate.size() > 0) {
            updateContentData(dataItemListForUpdate);
        }
    }

    private void updateContentData(List<Integer> updateDataItemList)
    {
        if (mContentSettingsList.size() == 0) {
            return;
        }

        // if we have received a list of data items to update,
        // then we update only those, else we update all
        List<Integer> list = (updateDataItemList != null) ?
                updateDataItemList : mContentSettingsList;

        int idxDataItem = 0;
        int size = list.size();
        int[] dataItemArray = new int[size];
        boolean[] dataItemValueArray = new boolean[size];

        for(Integer newdataItem : list) {
            if (list != mContentSettingsList) {
                if (!mContentSettingsList.contains(newdataItem)) {
                    continue;
                }
            }

            switch (newdataItem)
            {
                case ENH_DATA_ITEM_ID:
                    // Repurposing this ENH_DATA_ITEM_ID from IDLServiceVersion.V_AIDL_2_0 onwards
                    // It is composite opt in before, but only use as user consent after.
                    if(getNativeEmbargoCheckSupported()) {
                        dataItemArray[idxDataItem] = ENH_DATA_ITEM_ID;
                        dataItemValueArray[idxDataItem] = mUserConsent;
                    } else {
                        int eula_state = Settings.Secure.getIntForUser(
                                mContext.getContentResolver(),
                                ENH_LOCATION_SERVICES_ENABLED, -1, mCurrentUserId);

                        if ( mEulaState != eula_state) {
                            boolean eulaState = (eula_state == 0);
                            dataItemArray[idxDataItem] = ENH_DATA_ITEM_ID;
                            dataItemValueArray[idxDataItem] = eulaState;
                            // call setEnable if change in consent!
                            boolean consentAccepted =
                                    (eula_state & FEATURE_DISABLED_BY_CONSET) == 0;
                            boolean consentChanged = ( (eula_state & FEATURE_DISABLED_BY_CONSET) !=
                                    (mEulaState & FEATURE_DISABLED_BY_CONSET) );
                            if (consentChanged && IzatProvider.hasNetworkProvider()) {
                                IzatProvider.getNetworkProvider(mContext).setUserConsent(
                                        consentAccepted);
                            }
                            mEulaState = eula_state;
                        } else {
                            continue;
                        }
                    }
                break;
                case GPSSTATE_DATA_ITEM_ID:
                    boolean gpsState = mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    logd("Gps state: " + gpsState);
                    dataItemArray[idxDataItem] = GPSSTATE_DATA_ITEM_ID;
                    dataItemValueArray[idxDataItem] = gpsState;
                break;
            }

            idxDataItem++;
        }

        if (idxDataItem > 0) {
            // Note: assumption is that all data items values are boolean
            if (mIdlClient != null) {
                mIdlClient.bool_dataitem_update(dataItemArray, dataItemValueArray);
            }
        }
    }

    private void removeUpdateForContentData(List<Integer> removeDataItemList)
    {
        // remove the items from the content list
        for (Integer dataItemToRemove : removeDataItemList) {
            mContentSettingsList.remove(dataItemToRemove);
        }
        // unregister the ContentObserver
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        mContentObserver = null;

        if (mContentSettingsList.size() > 0) {
            // add back update for remaining data items
            subscribeForNewContentData();
        }
    }

    private final BroadcastReceiver mTimeZoneChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                mHandler.obtainMessage(MSG_TIME_ZONE_CHANGED).sendToTarget();
            }
        }
    };

    private final BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                mHandler.obtainMessage(MSG_TIME_CHANGED).sendToTarget();
            }
        }
    };

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_SWITCHED)) {
                mCurrentUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                        UserHandle.USER_CURRENT);
                logd("Action user switched: " + mCurrentUserId);

                // fetch all the data and send out.
                mHandler.obtainMessage(MSG_CONTENT_DATA_CHANGED, null).sendToTarget();
            }

            mHandler.post(()->broadcastSystemEvent(
                        ISystemEventListener.MSG_USER_SWITCH_ACTION_UPDATE, intent));
        }
    };

    private final BroadcastReceiver mProviderChangedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isLocationSettingsOn =
                    mLocationMgr.isLocationEnabledForUser(Binder.getCallingUserHandle());
            logd("Received PROVIDERS_CHANGED_ACTION intent: " + intent.toString() + ", MODE: " +
                    isLocationSettingsOn);
            mHandler.post(()->broadcastSystemEvent(ISystemEventListener.MSG_LOCATION_MODE_CHANGE,
                    isLocationSettingsOn));
            //Update GPS and NLP state
            List<Integer> dataItemList = new ArrayList<Integer>();
            dataItemList.add(GPSSTATE_DATA_ITEM_ID);
            if (dataItemList.size() > 0) {
                mHandler.obtainMessage(MSG_CONTENT_DATA_CHANGED, dataItemList).sendToTarget();
            }
        }
    };

    private final ActivityManager.OnUidImportanceListener mUidImportanceListener =
            new ActivityManager.OnUidImportanceListener() {
                @Override
                public void onUidImportance(int uid, int importance) {
                    boolean isForegroundUid =
                            importance <= FOREGROUND_IMPORTANCE_CUTOFF;
                    mHandler.post(()->broadcastSystemEvent(
                            ISystemEventListener.MSG_UID_IMPORTANCE_CHANGE, uid, isForegroundUid));
                }
    };

    private final BroadcastReceiver mWifiSupplicantStateChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                mHandler.obtainMessage(MSG_WIFI_STATE_CHANGED, intent).sendToTarget();
            }
        }
    };
    private final BroadcastReceiver mWifiScanAvailChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: " + intent.getAction());
            String action = intent.getAction();
            if (action.equals(WifiManager.ACTION_WIFI_SCAN_AVAILABILITY_CHANGED)) {
                mHandler.obtainMessage(MSG_WIFI_SCAN_AVAIL_CHANGED, intent).sendToTarget();
            }
        }
    };

    // listening for emergency call begins, android.intent.action.NEW_OUTGOING_CALL
    private final BroadcastReceiver mOutgoingCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            mHandler.post(()->broadcastSystemEvent(ISystemEventListener.MSG_OUTGOING_CALL, intent));
        }
    };

    // Used to detect when NI request is received, android.intent.action.NETWORK_INITIATED_VERIFY
    private final BroadcastReceiver mNetInitiatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            mHandler.post(()->broadcastSystemEvent(ISystemEventListener.MSG_NET_INITIATED, intent));
        }
    };

    // listen for package removed intent, android.intent.action.PACKAGE_REMOVED
    private final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context conext, Intent intent) {
            Log.d(TAG, "Package uninstalled, onReceive: " +
                    intent.getData().getSchemeSpecificPart());
            mHandler.post(()->broadcastSystemEvent(ISystemEventListener.MSG_PKG_REMOVED, intent));
        }
    };

    private DataItemContentObserver mContentObserver;
    public class DataItemContentObserver extends ContentObserver {

        DataItemContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            try {
                logd("onChange called for: " + uri.toString() + " user: " + mCurrentUserId);
                List<Integer> dataItemList = new ArrayList<Integer>();


                if (mDataItemList.contains(GPSSTATE_DATA_ITEM_ID)) {
                    dataItemList.add(GPSSTATE_DATA_ITEM_ID);
                }
                if (uri.compareTo(
                    Settings.Secure.getUriFor(ENH_LOCATION_SERVICES_ENABLED)) == 0) {
                    dataItemList.add(ENH_DATA_ITEM_ID);
                }
                if (dataItemList.size() > 0) {
                    mHandler.obtainMessage(MSG_CONTENT_DATA_CHANGED, dataItemList).sendToTarget();
                }
           } catch (NullPointerException e) {
                loge("getUriFor returned a NULL");
                e.printStackTrace();
           }
        }
    };

    private void sendTimeInfo()
    {
        TimeZone tz = TimeZone.getDefault();
        long currentTimeMillis = System.currentTimeMillis();
        int rawOffset = tz.getRawOffset()/1000;
        int dstSavings = tz.getDSTSavings()/1000;

        logd(String.format("Action time changed (%d, %d, %d)",
                           currentTimeMillis, rawOffset, dstSavings));

        if (mIdlClient != null) {
            mIdlClient.time_change_update(currentTimeMillis, rawOffset, dstSavings);
        }
    }

    private void sendTimeZoneInfo()
    {
        TimeZone tz = TimeZone.getDefault();
        long currentTimeMillis = System.currentTimeMillis();
        int rawOffset = tz.getRawOffset()/1000;
        int dstSavings = tz.getDSTSavings()/1000;

        logd(String.format("Action timezone changed (%d, %d, %d)",
                           currentTimeMillis, rawOffset, dstSavings));

        if (mIdlClient != null) {
            mIdlClient.timezone_change_update(currentTimeMillis, rawOffset,
                    dstSavings);
        }
    }

    private void updateWiFiSupplicantState(Intent intent) {
        ConnectivityManager connectivityMgr = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        Network network = connectivityMgr.getActiveNetwork();
        NetworkCapabilities capabilities = connectivityMgr.getNetworkCapabilities(network);
        WifiInfo wifiInfo = null;
        if(capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                capabilities.getTransportInfo() instanceof WifiInfo) {
            wifiInfo = (WifiInfo) capabilities.getTransportInfo();
        }
        SupplicantState supplicantState = SupplicantState.UNINITIALIZED;
        if (wifiInfo != null) {
            supplicantState = wifiInfo.getSupplicantState();
        }
        if (wifiInfo == null || supplicantState == null) {
            loge("wifiInfo/supplicantState returned null");
            return;
        }

        int i = 0;
        Integer val = 0;
        final int MAC_ADDR_LENGTH = 6;
        final int SSID_LENGTH = 32;

        byte apMacAddress[] = new byte[MAC_ADDR_LENGTH];
        int isAPMacAddressValid = 0;
        int isSSIDValid = 0;
        char ssid[] = null;

        logd("SUPPLICANT_STATE:" + supplicantState.name());

        if (supplicantState == (SupplicantState.COMPLETED)) {
            if (wifiInfo != null)
            {
                try {
                    logv("Connection info - BSSID - " + wifiInfo.getBSSID()
                              + " - SSID - " + wifiInfo.getSSID() + "\n");
                    String[] bssid =  wifiInfo.getBSSID().split(":");

                    for(i = 0;i<MAC_ADDR_LENGTH; i++){
                        val = Integer.parseInt(bssid[i],16);
                        apMacAddress[i] = val.byteValue();
                    }
                    isAPMacAddressValid = 1;

                    ssid = wifiInfo.getSSID().toCharArray();
                    if((ssid != null) && (wifiInfo.getSSID().length() <= SSID_LENGTH))
                    {
                       logd("ssid string is valid");
                       isSSIDValid = 1;
                    }
                    else
                    {
                        logd("ssid string is invalid");
                        isSSIDValid = 0;
                    }

                } catch (NumberFormatException e) {
                        loge("Unable to parse mac address");
                }
                catch (NullPointerException e) {
                        loge("Unable to get BSSID/SSID");
                }
            }
        }

        if (mIdlClient != null) {
            mIdlClient.wifi_supplicant_status_update(supplicantState.ordinal(),
                    isAPMacAddressValid, apMacAddress, isSSIDValid, ssid);
        }
    }

    private void updateWiFiScanAvailState(Intent intent) {
        boolean scanAvailable = intent.getBooleanExtra(WifiManager.EXTRA_SCAN_AVAILABLE, false);
        logi("updateWiFiScanAvailState, scanAvailable: " + scanAvailable);
        // Notify only when consolidatedWifiState toggles.
        if (mIdlClient != null && mLastWifiScanAvail != scanAvailable) {
            int[] dataItemArray = {WIFIHARDWARESTATE_DATA_ITEM_ID};
            boolean[] dataItemValueArray = {scanAvailable};
            mIdlClient.bool_dataitem_update(dataItemArray, dataItemValueArray);
        }
        mLastWifiScanAvail = scanAvailable;
    }

    private void installModeChangeReceiver()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        mContext.registerReceiver(mProviderChangedReceiver, intentFilter, null, null);

        logd("Registered for PROVIDERS_CHANGED_ACTION");
    }

    private void installUserSwitchActionReceiver()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_USER_SWITCHED);

        // use sticky broadcast receiver first to get the current user.
        Intent userIntent = mContext.registerReceiverAsUser(null, UserHandle.ALL, intentFilter,
                null, null);
        if (userIntent != null) {
            mCurrentUserId = userIntent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                    UserHandle.USER_CURRENT);
        }

        // register Receiver for ACTION_USER_SWITCHED
        mContext.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, intentFilter,
                null, null);

        logd("Registered for ACTION_USER_SWITCHED CurrentUserId = " + mCurrentUserId);
    }

    private void installOutgoingCallReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        mContext.registerReceiver(mOutgoingCallReceiver, intentFilter, null, null);

        logd("Registered for NEW_OUTGOING_CALL");
    }

    private void installNetInitiatedReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GpsNetInitiatedHandler.ACTION_NI_VERIFY);
        mContext.registerReceiver(mNetInitiatedReceiver, intentFilter, null, null);

        logd("Registered for NETWORK_INITIATED_VERIFY");
    }

    private void installPackageRemovedReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        mContext.registerReceiver(mPackageRemovedReceiver, intentFilter, null, null);

        logd("Registered for ACTION_PACKAGE_REMOVED");
    }

    private void installUidImportanceReceiver() {
        final long origId = Binder.clearCallingIdentity();
        ActivityManager activityMgr =
                (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        activityMgr.addOnUidImportanceListener(mUidImportanceListener,
                FOREGROUND_IMPORTANCE_CUTOFF);
        Binder.restoreCallingIdentity(origId);
    }

    private void updateMccmnc() {
        List<String> mccmnc = new ArrayList<String>();

        TelephonyManager telephonyMgr =
            (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String operator  = telephonyMgr.getNetworkOperator();
        logd("updateMccmnc, operator: " + operator + ", simCountryIso: " +
                 telephonyMgr.getSimCountryIso());
        if (operator == null || operator.isEmpty()) {
            mccmnc.add("-");

        } else {
            // note: first 3 characters are for MCC
            String mccmnc_formatted = String.format("%s|%s|%s", operator.substring(0,3),
                    operator.substring(3), telephonyMgr.getSimCountryIso());

            if (mccmnc_formatted.equals("000|00|")) {
                logd("operator MCCMNC is \"000|00|\". filtered out.");
                mccmnc.add("-");

            } else {
                mccmnc.add(mccmnc_formatted);
            }
        }

        List<SubscriptionInfo> siList = mSubscriptionMgr.getActiveSubscriptionInfoList();
        if (siList != null && siList.size() != 0) {
            for (SubscriptionInfo si : siList) {
                String mccmnc_formatted = String.format("%d|%02d|%s", si.getMcc(), si.getMnc(),
                        si.getCountryIso());
                mccmnc.add(mccmnc_formatted);
            }
        }

        String mccmncStr =  TextUtils.join("+", mccmnc);
        logv("updateMccmnc, mccmnc: " + mccmnc);

        if (mccmncStr != null && !mccmncStr.isEmpty()) {
            if (mIdlClient != null) {
                mIdlClient.string_dataitem_update(MCCMNC_DATA_ITEM_ID, mccmncStr);
            }
        }

    }

    private Handler.Callback m_handler_callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int msgID = msg.what;
            logv("handleMessage what - " + msgID);

            switch(msgID)
            {
                case MSG_OSAGENT_INIT:
                    if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL)
                            >= 0) {
                        mIdlClient = new OsAgentIdlClient();
                        installUserSwitchActionReceiver();
                        // listen for location mode changed intents
                        installModeChangeReceiver();
                    } else {
                        Log.e(TAG, "ILoc AIDL is not supported!");
                    }
                break;
                case MSG_DATAITEM_SUBSCRIBE:
                    handleSubscribe(msg.obj);
                break;
                case MSG_DATAITEM_REQUEST_DATA:
                    handleRequestData(msg.obj);
                break;
                case MSG_DATAITEM_UNSUBSCRIBE:
                    handleUnsubscribe(msg.obj);
                break;
                case MSG_DATAITEM_UNSUBSCRIBE_ALL:
                    handleUnsubscribeAll();
                break;
                case MSG_TIME_ZONE_CHANGED:
                    sendTimeZoneInfo();
                break;
                case MSG_TIME_CHANGED:
                    sendTimeInfo();
                break;
                case MSG_WIFI_STATE_CHANGED:
                    updateWiFiSupplicantState((Intent)msg.obj);
                break;
                case MSG_WIFI_SCAN_AVAIL_CHANGED:
                    updateWiFiScanAvailState((Intent)msg.obj);
                break;
                case MSG_CONTENT_DATA_CHANGED:
                    if (null != msg.obj) {
                        updateContentData((List<Integer>)msg.obj);
                    } else {
                        updateContentData(null);
                    }
                break;
                case MSG_INSTALL_OUTGOING_CALL:
                    installOutgoingCallReceiver();
                break;
                case MSG_INSTALL_PKG_REMOVED:
                    installPackageRemovedReceiver();
                break;
                case MSG_INSTALL_NET_INITIATED:
                    installNetInitiatedReceiver();
                break;
                case MSG_INSTALL_UID_IM_LISTENER:
                    installUidImportanceReceiver();
                break;
                case MSG_IDL_SERVICE_DIED:
                    logi("AIDL restart" + msgID);
                    if (mIdlClient != null) {
                        mIdlClient.resetIDLService();
                    }
                    break;
                default:
                    loge("Unhandled message, message id " + msgID);
                break;
            }
            return true;
        }
    };

    private static final void logi(String msg)
    {
        Log.i(TAG, msg);
    }
    private static final void logv(String msg)
    {
        if (VERBOSE_LOG) {
            Log.v(TAG, msg);
        }
    }
    private static final void logd(String msg)
    {
        if (DEBUG_LOG) {
            Log.d(TAG, msg);
        }
    }
    private static final void loge(String msg)
    {
        if (ERROR_LOG) {
            Log.e(TAG, msg);
        }
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void registerObserver(int sysEventMsgId, ISystemEventListener listener) {
        int osagentMsgId = getOsAgentMsgId(sysEventMsgId);

        mSysEventListenerLock.writeLock().lock();
        Set<ISystemEventListener> observerSet = mObserverMap.get(sysEventMsgId);
        if (observerSet == null) {
            mObserverMap.put(sysEventMsgId,
                    new HashSet<ISystemEventListener>() { {add(listener);} });
            mHandler.sendMessage(Message.obtain(mHandler, osagentMsgId));
        } else {
            observerSet.add(listener);
        }
        mSysEventListenerLock.writeLock().unlock();
    }

    public void unregisterObserver(int sysEventMsgId, ISystemEventListener listener) {
        mSysEventListenerLock.writeLock().lock();
        Set<ISystemEventListener> observerSet = mObserverMap.get(sysEventMsgId);
        if (observerSet != null) {
            observerSet.remove(listener);
            if (observerSet.isEmpty()) {
                mObserverMap.remove(sysEventMsgId);
            }
        }
        mSysEventListenerLock.writeLock().unlock();
    }

    public void broadcastSystemEvent(int sysEventMsgId, Object... args) {
        logv("broadcastSystemEvent - " + sysEventMsgId);
        mSysEventListenerLock.readLock().lock();
        //notify other listeners interested in it
        Set<ISystemEventListener> observerSet = mObserverMap.get(sysEventMsgId);
        if (observerSet != null) {
            for (ISystemEventListener observer: observerSet) {
                observer.notify(sysEventMsgId, args);
            }
        }
        mSysEventListenerLock.readLock().unlock();
    }

    // mirror SystemEventListener msg ID to OsAgent msg ID
    public static int getOsAgentMsgId(int sysEvtMsgId) {
        int osagentMsgId = IZatServiceContext.MSG_OSAGENT_BASE;
        switch(sysEvtMsgId) {
            case ISystemEventListener.MSG_OUTGOING_CALL:
                osagentMsgId = MSG_INSTALL_OUTGOING_CALL;
                break;
            case ISystemEventListener.MSG_NET_INITIATED:
                osagentMsgId = MSG_INSTALL_NET_INITIATED;
                break;
            case ISystemEventListener.MSG_PKG_REMOVED:
                osagentMsgId = MSG_INSTALL_PKG_REMOVED;
                break;
            case ISystemEventListener.MSG_USER_SWITCH_ACTION_UPDATE:
                osagentMsgId = MSG_INSTALL_USER_SWITCH_ACTION_UPDATE;
                break;
            case ISystemEventListener.MSG_UID_IMPORTANCE_CHANGE:
                osagentMsgId = MSG_INSTALL_UID_IM_LISTENER;
                break;
        }
        return osagentMsgId;
    }

    private static final boolean VERBOSE_LOG = Log.isLoggable(TAG, Log.VERBOSE);
    private static final boolean DEBUG_LOG = Log.isLoggable(TAG, Log.DEBUG);
    private static final boolean ERROR_LOG = Log.isLoggable(TAG, Log.ERROR);

    private static final String ENH_LOCATION_SERVICES_ENABLED = "enhLocationServices_on";
    private static final String SKYHOOK_LOCATION_ENABLED = "skyhook_location_enabled";
    private static final String COUNTRY_SELECT_ACTION = "com.android.location.osagent.COUNTRY_SELECT_ACTION";
    private String mCDMAHomeCarrier = "";
    private int mCurrentUserId = UserHandle.USER_CURRENT;

    private Context mContext;
    private Handler mHandler;

    private List<Integer> mDataItemList = new ArrayList<Integer> ();
    private List<Integer> mContentSettingsList = new ArrayList<Integer> ();
    private Map<Integer, Set<ISystemEventListener>> mObserverMap = new HashMap<>();
    private static final ReadWriteLock mSysEventListenerLock = new ReentrantReadWriteLock();

    private LocationManager mLocationMgr;
    private SubscriptionManager mSubscriptionMgr;

    // EULA consent monitoring
    private static final int FEATURE_DISABLED_BY_CONSET = 4;
    private int mEulaState = Integer.MAX_VALUE;

    // Cache the consolidated user consent
    private boolean mUserConsent;

    // UID importance
    private static final int FOREGROUND_IMPORTANCE_CUTOFF =
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE;

    // Register for changes to the list of active SubscriptionInfo records
    private OnSubscriptionsChangedListener mSubscriptionsChangedListener = null;

    private OsAgentIdlClient mIdlClient;

    // OSAgent messages
    public static final int MSG_DATAITEM_SUBSCRIBE =       IZatServiceContext.MSG_OSAGENT_BASE + 1;
    public static final int MSG_DATAITEM_REQUEST_DATA =    IZatServiceContext.MSG_OSAGENT_BASE + 2;
    public static final int MSG_DATAITEM_UNSUBSCRIBE =     IZatServiceContext.MSG_OSAGENT_BASE + 3;
    public static final int MSG_DATAITEM_UNSUBSCRIBE_ALL = IZatServiceContext.MSG_OSAGENT_BASE + 4;
    public static final int MSG_INSTALL_USER_SWITCH_ACTION_UPDATE =
            IZatServiceContext.MSG_OSAGENT_BASE + 6;
    public static final int MSG_OSAGENT_INIT =             IZatServiceContext.MSG_OSAGENT_BASE + 14;
    public static final int MSG_INSTALL_OUTGOING_CALL   =  IZatServiceContext.MSG_OSAGENT_BASE + 15;
    public static final int MSG_INSTALL_NET_INITIATED =    IZatServiceContext.MSG_OSAGENT_BASE + 16;
    public static final int MSG_INSTALL_PKG_REMOVED =      IZatServiceContext.MSG_OSAGENT_BASE + 17;
    public static final int MSG_INSTALL_UID_IM_LISTENER =  IZatServiceContext.MSG_OSAGENT_BASE + 18;
    public static final int MSG_TIME_ZONE_CHANGED =        IZatServiceContext.MSG_OSAGENT_BASE + 24;
    public static final int MSG_TIME_CHANGED =             IZatServiceContext.MSG_OSAGENT_BASE + 25;
    public static final int MSG_WIFI_STATE_CHANGED =       IZatServiceContext.MSG_OSAGENT_BASE + 27;
    public static final int MSG_CONTENT_DATA_CHANGED =     IZatServiceContext.MSG_OSAGENT_BASE + 28;
    public static final int MSG_WIFI_SCAN_AVAIL_CHANGED =  IZatServiceContext.MSG_OSAGENT_BASE + 29;
    public static final int MSG_IDL_SERVICE_DIED =         IZatServiceContext.MSG_OSAGENT_BASE + 30;

    // Data Item Id's
    private static final int ENH_DATA_ITEM_ID = 1;
    private static final int GPSSTATE_DATA_ITEM_ID = 2;
    private static final int WIFIHARDWARESTATE_DATA_ITEM_ID = 4;
    private static final int MODEL_DATA_ITEM_ID = 10;
    private static final int MANUFACTURER_DATA_ITEM_ID = 11;
    private static final int TIMEZONE_CHANGE_DATA_ITEM_ID = 16;
    private static final int TIME_CHANGE_DATA_ITEM_ID = 17;
    private static final int WIFI_SUPPLICANT_STATUS_DATA_ITEM_ID = 18;
    private static final int MCCMNC_DATA_ITEM_ID = 21;

    /* =================================================
     *   AIDL Client
     * =================================================*/
    private class OsAgentIdlClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb {

        private final String TAG = "OsAgentAidlClient";
        private vendor.qti.gnss.ILocAidlIzatSubscription mOsAgentIface;
        private OsAgentCb mOsAgentCb;

        private OsAgentIdlClient() {
            getOsAgentIface();
            if (null != mOsAgentIface) {
                try {
                    mOsAgentCb = new OsAgentCb(OsAgent.this);
                    mOsAgentIface.init(mOsAgentCb);
                    registerServiceDiedCb(this);
                } catch (RemoteException e) {
                }
            }
        }

        private void getOsAgentIface() {
            if (null == mOsAgentIface) {
                ILocAidlGnss service = (ILocAidlGnss)getGnssAidlService();
                if (null != service) {
                    try {
                        mOsAgentIface = service.getExtensionLocAidlIzatSubscription();
                    } catch (RemoteException e) {
                    }
                }
            }
        }

        public void resetIDLService() {
            mOsAgentIface = null;
            getOsAgentIface();
            if (null != mOsAgentIface) {
                try {
                    mOsAgentIface.init(mOsAgentCb);
                } catch (RemoteException e) {
                }
            }
        }
        @Override
        public void onServiceDied() {
            Message msgObj = Message.obtain(mHandler, MSG_IDL_SERVICE_DIED);
            mHandler.sendMessage(msgObj);
        }
        private class OsAgentCb extends ILocAidlIzatSubscriptionCallback.Stub {
            private OsAgent mOsAgent;

            public OsAgentCb(OsAgent osAgent) {
                mOsAgent = osAgent;
            }

            @Override
            public void requestData(int[] l) {
                IDLClientUtils.fromIDLService(TAG);
                OsAgent.this.requestData(l);
            }

            @Override
            public void updateSubscribe(int[] l, boolean subscribe) {
                IDLClientUtils.fromIDLService(TAG);
                if (subscribe) {
                    mOsAgent.subscribe(l);
                } else {
                    mOsAgent.unsubscribe(l);
                }
            }

            @Override
            public void unsubscribeAll() {
                IDLClientUtils.fromIDLService(TAG);
                mOsAgent.unsubscribeAll();
            }

            @Override
            public void turnOnModule(int di, int timeout) {
                IDLClientUtils.fromIDLService(TAG);
            }

            @Override
            public void turnOffModule(int di) {
                IDLClientUtils.fromIDLService(TAG);
            }

            @Override
            public void boolDataItemUpdate(LocAidlBoolDataItem[] dataItemArray) {
                IDLClientUtils.fromIDLService(TAG);
                for (int i = 0; i < dataItemArray.length; i++) {
                    if (ENH_DATA_ITEM_ID == dataItemArray[i].id) {
                        mOsAgent.updateOptinStatus(dataItemArray[i].enabled);
                        break;
                    }
                }
            }
            @Override
            public final int getInterfaceVersion() {
                return ILocAidlIzatSubscriptionCallback.VERSION;
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlIzatSubscriptionCallback.HASH;
            }
        }

        public void subscription_deinit() {
            if (mOsAgentIface!= null) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mOsAgentIface.deinit();
                } catch (RemoteException e) {
                }
            }
        }

        public void bool_dataitem_update(int[] dataItemId, boolean[] updated_value) {
            LocAidlBoolDataItem[] dataItemArray = new LocAidlBoolDataItem[dataItemId.length];
            for (int i = 0; i < dataItemId.length; i++) {
                LocAidlBoolDataItem item = new LocAidlBoolDataItem();
                item.id = dataItemId[i];
                item.enabled = updated_value[i];
                dataItemArray[i] = item;
            }
            if (mOsAgentIface!= null && dataItemArray.length > 0) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mOsAgentIface.boolDataItemUpdate(dataItemArray);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public void string_dataitem_update(int dataItemId, String updated_value) {
            LocAidlStringDataItem item = new LocAidlStringDataItem();
            item.id = dataItemId;
            item.str = updated_value;
            if (mOsAgentIface!= null) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mOsAgentIface.stringDataItemUpdate(item);
                } catch (RemoteException e) {
                }
            }
        }

        public void networkinfo_update(boolean is_connected, int type,
                                       String type_name, String subtype_name,
                                       boolean is_available, boolean is_roaming) {}

        public void screen_status_update(boolean status) {
            // No-op
        }

        public void timezone_change_update(long currTimeMillis, int rawOffset, int dstOffset) {
            LocAidlTimeZoneChangeDataItem item = new LocAidlTimeZoneChangeDataItem();
            item.curTimeMillis = currTimeMillis;
            item.rawOffset = rawOffset;
            item.dstOffset = dstOffset;
            if (mOsAgentIface!= null) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mOsAgentIface.timezoneChangeUpdate(item);
                } catch (RemoteException e) {
                }
            }
        }

        public void time_change_update(long currTimeMillis, int rawOffset, int dstOffset) {
            LocAidlTimeChangeDataItem item = new LocAidlTimeChangeDataItem();
            item.curTimeMillis = currTimeMillis;
            item.rawOffset = rawOffset;
            item.dstOffset = dstOffset;
            if (mOsAgentIface!= null) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mOsAgentIface.timeChangeUpdate(item);
                } catch (RemoteException e) {
                }
            }

        }

        public void shutdown_update() {
            // No-op
        }

        public void wifi_supplicant_status_update(int state,
                                                  int ap_mac_valid,
                                                  byte ap_mac_array[],
                                                  int ssid_valid,
                                                  char ssid_array[]) {
            LocAidlWifiSupplicantStatusDataItem item =
                    new LocAidlWifiSupplicantStatusDataItem();
            item.state = state;
            item.apMacAddressValid = (ap_mac_valid != 0);
            item.apSsidValid = (ssid_valid != 0);
            item.apMacAddress = new byte[ap_mac_array.length];
            item.apSsid = new String();
            if (ap_mac_valid != 0) {
                for (int i = 0; i < ap_mac_array.length; i++) {
                    item.apMacAddress[i] = ap_mac_array[i];
                }
            }
            if (ssid_valid != 0) {
                item.apSsid = String.valueOf(ssid_array);
            }

            if (mOsAgentIface!= null) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mOsAgentIface.wifiSupplicantStatusUpdate(item);
                } catch (RemoteException e) {
                }
            }
        }
    }

}
