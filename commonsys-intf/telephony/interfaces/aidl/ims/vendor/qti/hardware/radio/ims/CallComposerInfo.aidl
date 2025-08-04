/*
 * Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallLocation;
import vendor.qti.hardware.radio.ims.CallPriority;

/**
 * CallComposerInfo is used to indicate call composer info availability.
 * Telephony/Lower layers will process CallComposerInfo based on
 *         individual parameters, subject if not empty, imageUri if not empty etc.
 */
@VintfStability
parcelable CallComposerInfo {
    int callId;
    CallPriority priority = CallPriority.INVALID;
    char[] subject;
    CallLocation location;
    String imageUrl;
    @nullable
    char[] organization;
}

