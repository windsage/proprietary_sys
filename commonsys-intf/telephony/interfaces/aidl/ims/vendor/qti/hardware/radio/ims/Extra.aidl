/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.ExtraType;

/**
 * Extra is used for handover failure information.
 * Telephony will process the Extra only if type is not ExtraType#INVALID.
 */
@VintfStability
parcelable Extra {
    ExtraType type = ExtraType.INVALID;
    byte[] extraInfo;
}

