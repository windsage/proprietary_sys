/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.net.Uri;
import android.os.Bundle;
import com.qualcomm.ims.utils.Log;
import org.codeaurora.ims.QtiCallConstants;

/* This class is responsible to cache the ecnam information received as part of prealerting
 * indication as per spec 3GPP TS 24.196.
 */

public final class EcnamInfo {
    // Display name
    private String mName;
    // Iconic representation of the callee or caller
    private Uri mIconUrl;
    // Describes the caller or callee through web page
    private Uri mInfoUrl;
    // Business card
    private Uri mCardUrl;

    public EcnamInfo() {}

    public EcnamInfo(String name, Uri iconUrl, Uri infoUrl, Uri cardUrl) {
        mName = name;
        mIconUrl = iconUrl;
        mInfoUrl = infoUrl;
        mCardUrl = cardUrl;
    }

    /**
     * Method used to return display name.
     */
    public String getDisplayName() {
        return mName;
    }

    /**
     * Method used to return url to iconic representation of the callee or caller.
     */
    public Uri getIconUrl() {
        return mIconUrl;
    }

    /**
     * Method used to return url to describes the caller or callee through web page.
     */
    public Uri getInfoUrl() {
        return mInfoUrl;
    }

    /**
     * Method used to return url to business card.
     */
    public Uri getCardUrl() {
        return mCardUrl;
    }

    /**
     * To form bundle from EcnamInfo class using keys QtiCallConstants.EXTRA_CALL_ECNAM*.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (mName != null || !mName.isEmpty()) {
            bundle.putString(QtiCallConstants.EXTRA_CALL_ECNAM_DISPLAY_NAME, mName);
        }
        if (mIconUrl != null) {
            bundle.putParcelable(QtiCallConstants.EXTRA_CALL_ECNAM_ICON, mIconUrl);
        }
        if (mInfoUrl != null) {
            bundle.putParcelable(QtiCallConstants.EXTRA_CALL_ECNAM_INFO, mInfoUrl);
        }
        if (mCardUrl != null) {
            bundle.putParcelable(QtiCallConstants.EXTRA_CALL_ECNAM_CARD, mCardUrl);
        }
        return bundle;
    }

    public String toString() {
        return "EcnamInfo Name: " + Log.pii(mName) + " IconUrl: " + Log.pii(mIconUrl) +
            " InfoUrl: " + Log.pii(mInfoUrl) + " CardUrl: " + Log.pii(mCardUrl);
    }
}
