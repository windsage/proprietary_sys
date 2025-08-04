/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.SrtpEncryptionCategories;

@VintfStability
parcelable SrtpEncryptionInfo {
    int callId;
    int categories = SrtpEncryptionCategories.UNENCRYPTED;
}

