/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.nruwbicon;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.CarrierConfigManager.CarrierConfigChangeListener;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.SparseArray;
import com.qti.phone.QtiRadioProxy;
import java.util.ArrayList;
import java.util.Objects;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandwidthInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTime;

public class NrUwbConfigsController extends Handler {
    private final static String TAG = NrUwbConfigsController.class.getSimpleName();
    private final static int EVENT_CARRIER_CONFIG_CHANGED = 1;

    private final Context mContext;
    private final QtiRadioProxy mQtiRadioProxy;
    private final SparseArray<ConfigEntity> mConfigs = new SparseArray<>();
    private final CarrierConfigManager mCarrierConfigManager;

    private static final String KEY_NR_ULTRA_WIDEBAND_ICON_SIB2_VALUE =
            "5g_ultra_wideband_icon_sib2_value";
    private static final String KEY_NR_ULTRA_WIDEBAND_ICON_MIN_BANDWIDTH_MODE =
            "5g_ultra_wideband_icon_min_bandwidth_mode";
    private static final String KEY_NR_ULTRA_WIDEBAND_ICON_MIN_BANDWIDTH_VALUE =
            "5g_ultra_wideband_icon_min_bandwidth_value";
    private static final String KEY_NR_ULTRA_WIDEBAND_ICON_REFRESH_TIMER_MAP =
            "5g_ultra_wideband_icon_refresh_timer_map";
    private static final String KEY_NR_ULTRA_WIDEBAND_ICON_SA_BAND_MODE =
            "5g_ultra_wideband_icon_sa_band_mode";
    private static final String KEY_NR_ULTRA_WIDEBAND_ICON_SA_BAND_ARRAY =
            "5g_ultra_wideband_icon_sa_band_array";
    private static final String KEY_NR_ULTRA_WIDEBAND_ICON_NSA_BAND_MODE =
            "5g_ultra_wideband_icon_nsa_band_mode";
    private static final String KEY_NR_ULTRA_WIDEBAND_ICON_NSA_BAND_ARRAY =
            "5g_ultra_wideband_icon_nsa_band_array";

    private final CarrierConfigChangeListener
            mCarrierConfigChangeListener = new CarrierConfigChangeListener() {
        @Override
        public void onCarrierConfigChanged(int slotIndex, int subscriptionId, int carrierId,
                int specificCarrierId) {
            obtainMessage(EVENT_CARRIER_CONFIG_CHANGED, slotIndex, subscriptionId).sendToTarget();
        }
    };

    public NrUwbConfigsController(Context context, Looper looper, QtiRadioProxy radioProxy) {
        super(looper);
        mContext = context;
        mQtiRadioProxy = radioProxy;
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mCarrierConfigManager.registerCarrierConfigChangeListener(this::post,
                mCarrierConfigChangeListener);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_CARRIER_CONFIG_CHANGED:
                log("EVENT_CARRIER_CONFIG_CHANGED on slot " + msg.arg1);
                handleNrUwbConfigsUpdate(msg.arg1, msg.arg2);
                break;
            default:
                log("Unexpected event: " + msg.what);
        }
    }

    public void dispose() {
        log("Dispose");
        mConfigs.clear();
        mCarrierConfigManager.unregisterCarrierConfigChangeListener(mCarrierConfigChangeListener);
    }

    private void handleNrUwbConfigsUpdate(int slotId, int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            log("handleNrUwbConfigsUpdate invalid sub id on slot " + slotId);
            return;
        }

        ConfigEntity newEntity = createConfigEntityBySubId(subId);
        if (newEntity != null && newEntity.isValid()) {
            ConfigEntity oldEntity = mConfigs.get(slotId);
            if (!newEntity.equals(oldEntity)) {
                mConfigs.put(slotId, newEntity);
                sendConfigs(slotId, newEntity);
            } else {
                log("handleNrUwbConfigsUpdate same entity there already on slot " + slotId);
            }
        }
    }

    private ConfigEntity createConfigEntityBySubId(int subId) {
        if (mCarrierConfigManager != null) {
            PersistableBundle subCarrierConfigMgr = mCarrierConfigManager.getConfigForSubId(subId);
            ConfigEntity entity = new ConfigEntity();
            entity.nrUwbIconSib2Value = getNrUltraWidebandIconSib2Value(subCarrierConfigMgr);
            entity.nrUwbIconNsaBandInfo = getNrUltraWidebandIconBandsForNsa(subCarrierConfigMgr);
            entity.nrUwbIconSaBandInfo = getNrUltraWidebandIconBandsForSa(subCarrierConfigMgr);
            entity.nrUwbIconRefreshTimeArray =
                    getNrUltraWidebandIconRefreshTime(subCarrierConfigMgr);
            entity.nrUwbIconBwInfo = getNrUltraWidebandIconMinBandwidthValue(subCarrierConfigMgr);
            return entity;
        }
        log("createConfigEntityBySubId returns null for sub " + subId);
        return null;
    }

    private int getNrUltraWidebandIconSib2Value(PersistableBundle ccMgr) {
        int nrUwbIconSib2Value = ccMgr.getInt(
                KEY_NR_ULTRA_WIDEBAND_ICON_SIB2_VALUE,
                Integer.MAX_VALUE);
        if (NrUwbIconUtils.isSib2ValueValid(nrUwbIconSib2Value)) {
            log("nrUwbIconSib2Value = " + nrUwbIconSib2Value);
        } else {
            nrUwbIconSib2Value = Integer.MAX_VALUE;
        }
        return nrUwbIconSib2Value;
    }

    private NrUwbIconBandwidthInfo
            getNrUltraWidebandIconMinBandwidthValue(PersistableBundle ccMgr) {
        int minBandwidthMode = ccMgr.getInt(
                KEY_NR_ULTRA_WIDEBAND_ICON_MIN_BANDWIDTH_MODE,
                Integer.MAX_VALUE);
        int minBandwidthValue = ccMgr.getInt(
                KEY_NR_ULTRA_WIDEBAND_ICON_MIN_BANDWIDTH_VALUE,
                Integer.MAX_VALUE);
        if (NrUwbIconUtils.isMinBandwidthValid(minBandwidthMode, minBandwidthValue)) {
            log("minBandwidthMode = " + minBandwidthMode + ", minBandwidthValue = " +
                    minBandwidthValue);
            NrUwbIconBandwidthInfo bwInfo = new NrUwbIconBandwidthInfo();
            bwInfo.enabled = true;
            bwInfo.mode = minBandwidthMode;
            bwInfo.bandwidth = minBandwidthValue;
            return bwInfo;
        }
        return null;
    }

    private ArrayList<NrUwbIconRefreshTime>
            getNrUltraWidebandIconRefreshTime(PersistableBundle ccMgr) {
        PersistableBundle refreshTimerMap = ccMgr.getPersistableBundle(
                KEY_NR_ULTRA_WIDEBAND_ICON_REFRESH_TIMER_MAP);
        ArrayList<NrUwbIconRefreshTime> refreshTimes = new ArrayList<>();
        if (refreshTimerMap != null && !refreshTimerMap.isEmpty()) {
            for (String type : refreshTimerMap.keySet()) {
                NrUwbIconRefreshTime refreshTimeConfig = new NrUwbIconRefreshTime();
                if (!NrUwbIconUtils.isRefreshTimerTypeValid(type)) {
                    continue;
                }
                refreshTimeConfig.timerType = NrUwbIconUtils.getRefreshTimerTypeFromString(type);
                String timeValue = (String) refreshTimerMap.get(type);
                if (!NrUwbIconUtils.isRefreshTimerValueValid(timeValue)) {
                    continue;
                }
                refreshTimeConfig.timeValue = NrUwbIconUtils.convertRefreshTime(timeValue);
                log("Adding refresh timer type = " + type + ", value = " +
                        refreshTimeConfig.timeValue);
                refreshTimes.add(refreshTimeConfig);
            }
        }
        return refreshTimes;
    }

    private NrUwbIconBandInfo getNrUltraWidebandIconBandsForSa(PersistableBundle ccMgr) {
        int saBandMode = ccMgr.getInt(
                KEY_NR_ULTRA_WIDEBAND_ICON_SA_BAND_MODE,
                Integer.MAX_VALUE);
        boolean saBandModeValid = true;
        if (!NrUwbIconUtils.isModeValid(saBandMode)) {
            log("Invalid SA band mode");
            saBandModeValid = false;
        }
        int[] saBandArray = ccMgr.getIntArray(
                KEY_NR_ULTRA_WIDEBAND_ICON_SA_BAND_ARRAY);
        // Check for band value validity
        int[] saValidBands = NrUwbIconUtils.extractValidBands(saBandArray);

        NrUwbIconBandInfo saBandInfo = new NrUwbIconBandInfo();
        saBandInfo.enabled = saBandModeValid && (saValidBands.length != 0);
        saBandInfo.mode = saBandMode;
        saBandInfo.bands = saValidBands;
        return saBandInfo;
    }

    private NrUwbIconBandInfo getNrUltraWidebandIconBandsForNsa(PersistableBundle ccMgr) {
        int nsaBandMode = ccMgr.getInt(
                KEY_NR_ULTRA_WIDEBAND_ICON_NSA_BAND_MODE,
                Integer.MAX_VALUE);
        boolean nsaBandModeValid = true;
        if (!NrUwbIconUtils.isModeValid(nsaBandMode)) {
            log("Invalid NSA band mode");
            nsaBandModeValid = false;
        }
        int[] nsaBandArray = ccMgr.getIntArray(
                KEY_NR_ULTRA_WIDEBAND_ICON_NSA_BAND_ARRAY);
        // Check for band value validity
        int[] nsaValidBands = NrUwbIconUtils.extractValidBands(nsaBandArray);

        NrUwbIconBandInfo nsaBandInfo = new NrUwbIconBandInfo();
        nsaBandInfo.enabled = nsaBandModeValid && (nsaValidBands.length != 0);
        nsaBandInfo.mode = nsaBandMode;
        nsaBandInfo.bands = nsaValidBands;
        return nsaBandInfo;
    }

    private void sendConfigs(int slotId, ConfigEntity entity) {
        if (mQtiRadioProxy != null && entity != null) {
            try {
                mQtiRadioProxy.setNrUltraWidebandIconConfig(slotId, entity.nrUwbIconSib2Value,
                        entity.nrUwbIconSaBandInfo, entity.nrUwbIconNsaBandInfo,
                        entity.nrUwbIconRefreshTimeArray, entity.nrUwbIconBwInfo);
            } catch (RemoteException e) {
               log("sendConfigs failed");
            }
        } else {
            log("sendConfigs mQtiRadioProxy or entity is null");
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }

    private static class ConfigEntity {
        int nrUwbIconSib2Value = Integer.MAX_VALUE;
        NrUwbIconBandInfo nrUwbIconNsaBandInfo;
        NrUwbIconBandInfo nrUwbIconSaBandInfo;
        ArrayList<NrUwbIconRefreshTime> nrUwbIconRefreshTimeArray;
        NrUwbIconBandwidthInfo nrUwbIconBwInfo;

        /**
         * return true if valid, currently, implies that at least SIB2 value is provided.
         */
        public boolean isValid() {
            return hasSib2Value();
        }

        private boolean hasSib2Value() {
            return nrUwbIconSib2Value != Integer.MAX_VALUE;
        }

        @Override
        public int hashCode() {
            int hashCode = nrUwbIconSib2Value;
            hashCode = 31 * hashCode
                    + (nrUwbIconNsaBandInfo != null ? nrUwbIconNsaBandInfo.hashCode() : 0);
            hashCode = 31 * hashCode
                    + (nrUwbIconSaBandInfo != null ? nrUwbIconSaBandInfo.hashCode() : 0);
            hashCode = 31 * hashCode
                    + (nrUwbIconRefreshTimeArray != null
                    ? nrUwbIconRefreshTimeArray.hashCode() : 0);
            hashCode = 31 * hashCode
                    + (nrUwbIconBwInfo != null ? nrUwbIconBwInfo.hashCode() : 0);
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (!(obj instanceof ConfigEntity)) return false;
            ConfigEntity newObj = (ConfigEntity) obj;

            return newObj.nrUwbIconSib2Value == nrUwbIconSib2Value
                    && Objects.equals(newObj.nrUwbIconNsaBandInfo, nrUwbIconNsaBandInfo)
                    && Objects.equals(newObj.nrUwbIconSaBandInfo, nrUwbIconSaBandInfo)
                    && Objects.equals(newObj.nrUwbIconRefreshTimeArray, nrUwbIconRefreshTimeArray)
                    && Objects.equals(newObj.nrUwbIconBwInfo, nrUwbIconBwInfo);
        }
    }
}
