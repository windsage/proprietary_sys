/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.SignalQuality;

@VintfStability
@JavaDerive(toString=true)
parcelable SnpnInfo {

    /**
     * SNPN network ID.
     */
    byte[] nid;

    /**
     * Mobile Country Code.
     */
    String mcc;

    /**
     * Mobile Network Code
     */
    String mnc;

    /**
      * 5 or 6 digit numeric code (MCC + MNC)
      */
    String operatorNumeric;

    /**
     * SNPN Signal Strength
     */
    int signalStrength;

    /**
     * SNPN Signal Quality
     */
    SignalQuality signalQuality;
}
