/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;
/**
 * Data structure contains information about DTMF tones to send.
 */
@VintfStability
parcelable DtmfInfo {
    String dtmf;
    int callId = -1;
}
