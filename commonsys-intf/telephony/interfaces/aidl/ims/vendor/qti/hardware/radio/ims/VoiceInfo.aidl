/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum VoiceInfo {
    UNKNOWN,
    /**
     * Voice Info silent is sent when remote party is silent on a RTT call
     */
    SILENT,
    /**
     * Voice Info speech is sent when remote party starts speaking on a RTT call
     */
    SPEECH,
}
