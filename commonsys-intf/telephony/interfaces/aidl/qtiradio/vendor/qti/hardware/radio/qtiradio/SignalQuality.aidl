/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum SignalQuality {
    /*
     * Invalid quality signal
     */
    SIGNAL_QUALITY_INVALID = -1,

    /*
     * Low quality signal
     */
    SIGNAL_QUALITY_LOW = 0,

    /**
     * High quality signal
     */
    SIGNAL_QUALITY_HIGH = 1,
}
