/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallType;
import vendor.qti.hardware.radio.ims.CrsType;

@VintfStability
parcelable CrsData {
    /*
     * INVALID if CrsData is invalid,
     * AUDIO if only audio will be played,
     * VIDEO if only video will be played,
     * AUDIO_VIDEO if both video and audio will be played.
     */
    CrsType type = CrsType.INVALID;
    /*
     * Valid only if CrsData.type is not INVALID
     * Call type of the actual call received from network.
     */
    CallType originalCallType = CallType.UNKNOWN;
}

