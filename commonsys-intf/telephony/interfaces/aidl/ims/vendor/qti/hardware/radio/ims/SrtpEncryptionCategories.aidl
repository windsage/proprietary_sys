/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;


@VintfStability
@Backing(type="int")
enum SrtpEncryptionCategories {
    UNENCRYPTED = 0,
    VOICE = 1 << 0,
    VIDEO = 1 << 1,
    TEXT = 1 << 2,
}

