/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony;

import android.telephony.NetworkRegistrationInfo;
import android.util.Log;

import com.android.internal.telephony.CellularNetworkService;

/**
 * This class is used purely for converting an instance of HAL RegStateResult to an instance of
 * {@link NetworkRegistrationInfo}.
 */

public class QtiCellularNetworkService extends CellularNetworkService {
    private static final String TAG = "QtiCellularNetworkService";

    private class QtiCellularNetworkProvider extends CellularNetworkServiceProvider {
        public QtiCellularNetworkProvider(int slotId) {
            super(slotId, false);
        }

        protected NetworkRegistrationInfo getRegistrationStateFromResult(Object result,
                                                                         int domain) {
            NetworkRegistrationInfo info = super.getRegistrationStateFromResult(result, domain);
            Log.d(TAG, "Domain: " + domain + ", NRI: " + info);
            return info;
        }
    }

    protected QtiCellularNetworkProvider mProvider;
    private int mSlotId;

    public QtiCellularNetworkService(int slotId) {
        mSlotId = slotId;
    }

    private QtiCellularNetworkProvider getCellularNetworkServiceProvider() {
        if (mProvider == null) {
            Log.d(TAG, "Initiate cellular network service provider for slotId: " + mSlotId);
            mProvider = new QtiCellularNetworkProvider(mSlotId);
        }
        return mProvider;
    }

    /**
     * Convert HAL RegStateResult, which is received as a response to Voice/Data registration state
     * query from RIL, to {@link NetworkRegistrationInfo}
     *
     * @param result Instance of RegStateResult for IRadio HAL 1.5 and beyond, or of
     *               VoiceRegStateResult/DataRegStateResult for the older versions.
     *
     * @param domain {@link NetworkRegistrationInfo#DOMAIN_CS} if this is for voice,
     *               {@link NetworkRegistrationInfo#DOMAIN_PS} if this is for data
     *
     * @return an instance of {@link NetworkRegistrationInfo}
     */
    NetworkRegistrationInfo getRegistrationStateFromResult(Object result, int domain) {
        Log.d(TAG, "getRegistrationStateFromResult, domain: " + domain);
        QtiCellularNetworkProvider provider = getCellularNetworkServiceProvider();
        return provider == null ? null : provider.getRegistrationStateFromResult(result, domain);
    }
}
