/*
 * Copyright (c) 2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum NrIconType {
    INVALID           = -1,
    TYPE_NONE         = 0,
    TYPE_5G_BASIC     = 1,
    TYPE_5G_UWB       = 2,
    TYPE_5G_PLUS_PLUS = 3,
}
