/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.qualcomm.datastatusnotification;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Telephony;
import android.telephony.data.ApnSetting;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.qualcomm.qcrilhook.QcRilHook;
import com.qualcomm.qcrilhook.QcRilHookCallback;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataStatusNotificationService extends Service {
    private static final String TAG = "QtiDataStatusNotification";
    private static final boolean DBG = true;
    private static final String ALL_APN_TYPES =
            "default,mms,dun,hipri,fota,ims,cbs,ia,supl,emergency,mcx,xcap,vsim,bip,enterprise";

    private static final String DATA_ROAMING = Settings.Global.DATA_ROAMING;
    private static final String CARRIERS = "carriers";
    private static final String DEVICE_PROVISIONING_MOBILE_DATA_ENABLED =
            Settings.Global.DEVICE_PROVISIONING_MOBILE_DATA_ENABLED;
    private static final Uri CARRIERS_URI = Telephony.Carriers.CONTENT_URI;
    private static final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static final int MOBILE_DATA_DISABLED = 0;
    private static final int MOBILE_DATA_ENABLED = 1;
    private static final int DEVICE_PROVISIONED = 1;
    private static final int INVALID_PHONE_INDEX = -1;

    private boolean mEnableMsimLldd =
            SystemProperties.getBoolean("persist.vendor.radio.msim.lldd", false);
    private QcRilHook mQcRilHook;
    private TelephonyManager mTm;
    private SubscriptionManager mSubscriptionManager;
    private int mPhoneCount;
    private DataSettingsObserver mDataSettingsObserver;
    private ApnCache[] mApnCache;
    private final SparseArray<SubTracker> mSubTrackers = new SparseArray<>();
    private ContentResolver mResolver;
    boolean mIsQcRilHookReady = false;

    private QcRilHookCallback mQcRilHookCb = new QcRilHookCallback() {
        public void onQcRilHookReady() {
            mIsQcRilHookReady = true;
            log("mPhoneCount = " + mPhoneCount);
            // Register content observer for carriers DB for changes in APN
            enableContentObserver(CARRIERS_URI);
            handleTelephonyEventRegisteration();
            registerTelephonyBroadcastReceiver();
        }

        public void onQcRilHookDisconnected() {
            mIsQcRilHookReady = false;
        }
    };

    private BroadcastReceiver mTelephonyActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED)) {
                int activeModemCount = intent.getIntExtra(
                        TelephonyManager.EXTRA_ACTIVE_SIM_SUPPORTED_COUNT, 1);
                handleOnMultiSimConfigChanged(activeModemCount);
            } else if (action.equals(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED)) {
                final int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
                // Suppose MCC-MNC of SubscriptionInfo is initiated,
                // the OnSubscriptionsChangedListener may not be invoked, however, querying APNs
                // is relying on sim operator, so attempts to register Telephony events again
                // after SIM is loaded.
                log("ACTION_SIM_APPLICATION_STATE_CHANGED state = " + simState);
                if (simState == TelephonyManager.SIM_STATE_LOADED) {
                    handleTelephonyEventRegisteration();
                }
            }
        }
    };

    private OnSubscriptionsChangedListener mSubListener =
            new OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            handleTelephonyEventRegisteration();
        }
    };

    /* Register telephony broadcast receiver */
    private void registerTelephonyBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED);
        filter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        registerReceiver(mTelephonyActionReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    private void handleOnMultiSimConfigChanged(int activeModemCount) {
        int prevModemCount = mPhoneCount;
        if (prevModemCount == activeModemCount) {
            return;
        }
        mPhoneCount = mTm.getActiveModemCount();
        mApnCache = Arrays.copyOf(mApnCache, activeModemCount);
    }

    private void triggerOnChange(SubTracker subTracker) {
        if (mDataSettingsObserver != null) {
            mDataSettingsObserver.onChange(false, subTracker.getRoamingUri());
            onApnChanged(subTracker.getSlotId());
        }
    }

    private void enableContentObserver(Uri uri) {
        // Register content observer for URI
        if (mDataSettingsObserver == null) {
            mDataSettingsObserver = new DataSettingsObserver();
        }
        mResolver.registerContentObserver(uri, false, mDataSettingsObserver);
    }

    private void disableContentObserver() {
        if (mDataSettingsObserver != null) {
            mResolver.unregisterContentObserver(mDataSettingsObserver);
        }
    }

    private void onApnChanged(int slotId) {
        Apn newApn = null;
        Apn newImsApn = null;
        Apn preferredApn = null;
        int subId = SubscriptionManager.getSubscriptionId(slotId);
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return;

        // Find operator of the subId where APN has changed
        String operator = mTm.createForSubscriptionId(subId).getSimOperator();
        log("onApnChanged: slot = " + slotId + " subId = "
                + subId + " operator = " + operator);
        // Checking if operator is valid because provider is relying on it.
        if (operator != null && !operator.isEmpty()) {
            ApnCache currentApnCache = new ApnCache();

            String selection = "carrier_enabled = 1";
            final Uri simApnUri = Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI,
                    String.valueOf(subId));
            Cursor cursor = mResolver.query(simApnUri,
                    new String[] {
                        "apn", "type", "carrier_enabled"
                    },
                    selection, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                log("APN change URI is  "
                        + simApnUri.toString()
                        + " count = " + cursor.getCount());
                // Try to get preferredApn if preferred APN is set, which can handle
                // default type and has the correct operator
                // else get first APN that can handle default type
                preferredApn = getPreferredApn(subId, selection);
                if (preferredApn != null) {
                    log("Found preferred APN: " + preferredApn.toString());
                } else {
                    preferredApn = getDefaultApn(cursor);
                    if (preferredApn != null) {
                        log("Found default APN: " + preferredApn.toString());
                    } else {
                        log("Preferred or default APN not found");
                    }
                }
                if (cursor.moveToFirst()) {
                    do {
                        newApn = extractApn(cursor);
                        //Store in new cache instance.
                        currentApnCache.put(newApn.getName(), newApn.getType());
                    } while (cursor.moveToNext());
                }
            } else {
                log("No rows in Carriers DB for current operator");
            }
            if (cursor != null) {
                cursor.close();
            }

            /**
             * Inform modem of apns which are no longer relevant because
             * user removed them.
             */
            ApnCache apnCache = mApnCache[slotId];
            if (apnCache != null) {
                for (String apn : apnCache.getMissingKeys(currentApnCache)) {
                    log("Apn = " + apn + " removed, inform modem.");

                    /** Inform modem about apn removal via oem hook */
                    mQcRilHook.qcRilSendApnInfo("", apn, 0 /* disable */,
                                slotId);
                }
            }

            /**
             * Update apn cache to current snapshot
             */
            mApnCache[slotId] = currentApnCache;

            /**
             * Inform modem of current apns snapshot if L+L property is enabled
             */
            if (!mEnableMsimLldd) {
                informCurrentApnsToModemForPhoneId(slotId, preferredApn);
            } else {
                mQcRilHook.qcRilSendApnInfo(preferredApn.getType(), preferredApn.getName(),
                                        1 /* enable */, slotId);
            }

        } else {
            log("Could not get current operator");
        }

    }

    private Apn getApnFound(Cursor cursor, String type) {
        Apn apn = null;
        String[] typesSupported = parseTypes(cursor.getString(
                cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
        log("getApnFound: typesSupported = " + Arrays.toString(typesSupported) +
            " type requested = " + type);
        for (String t : typesSupported) {
            if (t.equalsIgnoreCase(ApnSetting.TYPE_ALL_STRING) ||
                    t.equalsIgnoreCase(type)) {
                apn = new Apn(
                        type,
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.APN)));
                log("getApnFound: Apn = " + apn.toString());
                break;
            }
        }
        return apn;
    }

    private Apn getPreferredApn(int subId, String selection) {
        Apn preferredApn = null;
        Cursor cursor = mResolver.query(getUriBySubId(PREFERAPN_URI, subId),
                new String[] {
                    "apn", "numeric", "type", "carrier_enabled"
                },
                selection, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            preferredApn = getApnFound(cursor,
                    getApnTypesStringFromBitmask(ApnSetting.TYPE_DEFAULT));
        }
        if(cursor != null) {
            cursor.close();
        }
        return preferredApn;
    }

    private Apn getDefaultApn(Cursor cursor) {
        Apn defaultApn = null;
        while (defaultApn == null && cursor.moveToNext()) {
            defaultApn = getApnFound(cursor,
                    getApnTypesStringFromBitmask(ApnSetting.TYPE_DEFAULT));
        }
        return defaultApn;
    }

    private String getApnTypesStringFromBitmask(int apnTypeBitmask) {
        List<String> types = new ArrayList<>();
        int remainingApnTypes = apnTypeBitmask;
        // special case for DEFAULT since it's not a pure bit
        if ((remainingApnTypes & ApnSetting.TYPE_DEFAULT) == ApnSetting.TYPE_DEFAULT) {
            types.add(ApnSetting.TYPE_DEFAULT_STRING);
            remainingApnTypes &= ~ApnSetting.TYPE_DEFAULT;
        }
        while (remainingApnTypes != 0) {
            int highestApnTypeBit = Integer.highestOneBit(remainingApnTypes);
            String apnString = ApnSetting.getApnTypeString(highestApnTypeBit);
            if (!TextUtils.isEmpty(apnString)) types.add(apnString);
            remainingApnTypes &= ~highestApnTypeBit;
        }
        return TextUtils.join(",", types);
    }

    private void dump(ArrayList<Apn> apns) {
        for (Apn apn : apns) {
            log(apn.toString());
        }
    }

    private void informCurrentApnsToModemForPhoneId(int slotId, Apn preferredApn) {
        log("InformCurrentApnsToModemForPhoneId: " + slotId);
        ApnCache apnCache = mApnCache[slotId];
        if (apnCache != null) {
            ArrayList<Apn> apns= new ArrayList<Apn>();
            for (String apn : apnCache.keySet()) {
                String type = apnCache.get(apn);
                apns.add(new Apn(type, apn));
                if (preferredApn != null && preferredApn.getName().equals(apn)) {
                    log("Adding preferred type to: " + preferredApn.getName());
                    type += ",preferred";
                }

                /*
                * Inform modem about apn name and type via oem hook
                */
                mQcRilHook.qcRilSendApnInfo(type, apn,
                        1 /* enable */, slotId);
            }

            log("***********************");
            log("Current APNs for PhoneId = " + slotId);
            dump(apns);
            log("***********************");
        }
    }

    private Apn extractApn(Cursor cursor) {
        String typesSupported = cursor.getString(cursor
                .getColumnIndexOrThrow(Telephony.Carriers.TYPE));
        if (typesSupported.isEmpty() ||
                typesSupported.equalsIgnoreCase(ApnSetting.TYPE_ALL_STRING)) {
            typesSupported = ALL_APN_TYPES;
        }

        return new Apn(
                typesSupported,
                cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)));
    }

    private String[] parseTypes(String types) {
        String[] result;
        // If unset, set to DEFAULT.
        if (TextUtils.isEmpty(types)) {
            result = new String[1];
            result[0] = ApnSetting.TYPE_ALL_STRING;
        } else {
            result = types.split(",");
        }
        return result;
    }

    private Uri getUriBySubId(Uri uri, int subId) {
        return Uri.withAppendedPath(uri, "subId/" + subId);
    }

    @Override
    public void onCreate() {
        log("onCreate");
        mTm = (TelephonyManager) DataStatusNotificationService.this.
                getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = (SubscriptionManager) DataStatusNotificationService.this.
                getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (mSubscriptionManager != null) {
            mSubscriptionManager.addOnSubscriptionsChangedListener(mSubListener);
        }
        mPhoneCount = mTm.getActiveModemCount();
        mResolver = DataStatusNotificationService.this.getContentResolver();
        mQcRilHook = new QcRilHook(this, mQcRilHookCb);
        mApnCache = new ApnCache[mPhoneCount];
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        disableContentObserver();
        if (mTelephonyActionReceiver != null) {
            unregisterReceiver(mTelephonyActionReceiver);
            mTelephonyActionReceiver = null;
        }
        if (mQcRilHook != null) {
            mQcRilHook.dispose();
            mQcRilHook = null;
        }
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mSubListener);
        unregisterSubTrackers();
        mTm = null;
        mPhoneCount = 0;
        mApnCache = null;
    }

    private void unregisterSubTrackers() {
        log("unregisterSubTrackers");
        for (int i = 0; i < mSubTrackers.size(); i++) {
            mSubTrackers.valueAt(i).cleanup();
        }
        mSubTrackers.clear();
    }

    private void handleTelephonyEventRegisteration() {
        if (!mIsQcRilHookReady) {
            log("handleTelephonyEventRegisteration OEM hook is unready");
            return;
        }
        List<SubscriptionInfo> subscriptions = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptions ==  null) {
            subscriptions = Collections.emptyList();
        }
        SparseArray<SubTracker> staleSubTackers = new SparseArray<SubTracker>();
        for (int i = 0; i < mSubTrackers.size(); i++) {
            staleSubTackers.put(mSubTrackers.keyAt(i), mSubTrackers.valueAt(i));
        }
        mSubTrackers.clear();
        final int subCount = subscriptions.size();
        for (int i = 0; i < subCount; i++) {
            int subId = subscriptions.get(i).getSubscriptionId();
            if (staleSubTackers.indexOfKey(subId) >= 0) {
                // The old one is present.
                mSubTrackers.put(subId, staleSubTackers.get(subId));
                staleSubTackers.remove(subId);
            } else {
                final String operator = mTm.createForSubscriptionId(subId).getSimOperator();
                if (operator == null || operator.isEmpty()) {
                    log("handleTelephonyEventRegisteration operator is unready on SUB " + subId);
                    continue;
                }
                // Create a new one for a new SUB.
                final int slotId = subscriptions.get(i).getSimSlotIndex();
                final SubTracker tracker =
                        new SubTracker(this, slotId, subId, mTm, mQcRilHook);
                enableContentObserver(tracker.getRoamingUri());
                // Explicitly trigger onChange() at start of service
                // since modem needs to know the values at start of service
                triggerOnChange(tracker);
                mSubTrackers.put(subId, tracker);
            }
        }
        for (int i = 0; i < staleSubTackers.size(); i++) {
            staleSubTackers.valueAt(i).cleanup();
        }
    }

    private static final class SubTracker {
        private int mSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        private Uri mRoamingUri;
        private TelephonyManager mTelephonyManager;
        private PhoneStateCallback mPhoneStateCallback;

        public SubTracker(Context context, int slotId, int subId,
                TelephonyManager tm, QcRilHook hook) {
            mSlotId = slotId;
            if (tm.getActiveModemCount() > 1) {
                mRoamingUri = Settings.Global.getUriFor(DATA_ROAMING + subId);
            } else {
                mRoamingUri = Settings.Global.getUriFor(DATA_ROAMING);
            }
            mTelephonyManager = tm.createForSubscriptionId(subId);
            mPhoneStateCallback = new PhoneStateCallback(mSlotId, subId, hook);
            mTelephonyManager.registerTelephonyCallback(context.getMainExecutor(),
                    mPhoneStateCallback);
        }

        public Uri getRoamingUri() {
            return mRoamingUri;
        }

        public int getSlotId() {
            return mSlotId;
        }

        public void cleanup() {
            if (mTelephonyManager != null && mPhoneStateCallback != null) {
                mTelephonyManager.unregisterTelephonyCallback(mPhoneStateCallback);
                mTelephonyManager = null;
                mPhoneStateCallback = null;
            }
        }
    }

    private static final class Apn {
        private String mType;
        private String mApn;

        public Apn(String type, String apn) {
            mType = type;
            mApn = apn;
        }

        public String getType() {
            return mType;
        }

        public String getName() {
            return mApn;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[Type=");
            sb.append(mType);
            sb.append(", Apn=");
            sb.append(mApn);
            sb.append("]");
            return sb.toString();
        }
    }

    private class DataSettingsObserver extends ContentObserver {
        DataSettingsObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (!mIsQcRilHookReady) {
                Log.e(TAG, "QcRilhook not ready. Bail out");
                return;
            }

            if (uri != null) {
                String authority = uri.getAuthority();
                String uriLastSegment = uri.getLastPathSegment();
                int slotId = 0;
                int subId = 0;
                if (authority.equals("settings") && mPhoneCount > 1) { // authority for roaming
                    /*
                    * For multi-sim, the URI last segment contains subId in the format
                    * data_roaming0
                    */
                    String[] lastSegmentParts = uriLastSegment.split("\\d");
                    int uriLength = uriLastSegment.length();
                    int keyLength = lastSegmentParts[0].length();
                    try {
                        subId = Integer.parseInt(uriLastSegment.substring(keyLength, uriLength));
                    } catch (NumberFormatException e) {
                        log("Invalid subId from URI last segment");
                    }
                    slotId = SubscriptionManager.getSlotIndex(subId);
                    uriLastSegment = uriLastSegment.substring(0, keyLength);
                    log("MultiSim onChange(): subId = " + subId);
                }
                log("onChange(): uri=" + uri.toString() + " authority=" + authority
                            + " path=" + uri.getPath() + " segments=" + uri.getPathSegments()
                            + " uriLastSegment=" + uriLastSegment);
                switch (uriLastSegment) {
                    case DATA_ROAMING:
                        int data_roaming_status = 0;
                        try {
                            if (mPhoneCount > 1) {
                                data_roaming_status = Settings.Global.getInt(
                                        mResolver, DATA_ROAMING + subId);
                            } else {
                                data_roaming_status = Settings.Global.getInt(
                                        mResolver, DATA_ROAMING);
                            }
                        } catch (SettingNotFoundException ex) {
                            Log.e(TAG, ex.getMessage());
                        }
                        log("handleMessage: Data Roaming changed to "
                                    + data_roaming_status
                                    + " on slot = " + slotId);
                        mQcRilHook.qcRilSendDataRoamingEnableStatus(data_roaming_status, slotId);
                        break;
                    case CARRIERS:
                        for (int i = 0; i < mPhoneCount; i++) {
                            onApnChanged(i);
                        }
                        break;
                    default:
                        Log.e(TAG, "Received unsupported uri");
                }
            } else {
                Log.e(TAG, "Received uri is null");
            }
        }
    }

    /**
     * When data setting is overridden by data during call, user can't make underlying
     * data disabled by mobile data option, and the final overall data enbled state determines
     * the underlying data state also, e.g. while data is disabled with reason thermal or enabled
     * for provisioning, underlying data should be disabled or enabled meanwhile. Besides, even
     * nDDS's mobile data is off, and no matter whether data during call is off, the current data
     * enabled state should be determined by the callback onDataEnabledChanged also because
     * framework will override the settings and underlying module is relying on this for smart
     * temp DDS recommendation.
     */
    private static class PhoneStateCallback extends TelephonyCallback
            implements TelephonyCallback.DataEnabledListener {

        private int mPhoneId = INVALID_PHONE_INDEX;
        private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        private final QcRilHook mQcRilHook;

        public PhoneStateCallback(int phoneId, int subId, QcRilHook hook) {
            mPhoneId = phoneId;
            mSubId = subId;
            mQcRilHook = hook;
        }

        @Override
        public void onDataEnabledChanged(boolean enabled, int reason) {
            log("onDataEnabledChanged: phone ID = " + mPhoneId + " sub ID = " + mSubId
                    + " enabled = " + enabled + " reason = " + reason);
            if (mQcRilHook == null) {
                 Log.e(TAG, "QcRilhook not ready. Bail out");
                 return;
            }
            // Straight emit data enabled state to lower layer.
            mQcRilHook.qcRilSendDataEnableStatus(enabled ? 1 : 0, mPhoneId);
        }
    }

    private static void log(String str) {
        if (DBG) {
            Log.d(TAG, str);
        }
    }
}
