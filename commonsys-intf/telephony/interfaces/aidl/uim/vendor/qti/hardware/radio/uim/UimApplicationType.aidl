/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim;

@VintfStability
@Backing(type="byte")
enum UimApplicationType {
    /**
     * Unknown app
     */
    UIM_APP_TYPE_UNKNOWN = 0x00,
    /**
     * SIM app
     */
    UIM_APP_TYPE_SIM = 0x01,
    /**
     * USIM app
     */
    UIM_APP_TYPE_USIM = 0x02,
    /**
     * RUIM app
     */
    UIM_APP_TYPE_RUIM = 0x03,
    /**
     * CSIM app
     */
    UIM_APP_TYPE_CSIM = 0x04,
    /**
     * ISIM app
     */
    UIM_APP_TYPE_ISIM = 0x05,
}
