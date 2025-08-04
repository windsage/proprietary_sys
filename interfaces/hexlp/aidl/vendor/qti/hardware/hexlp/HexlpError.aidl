/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

@VintfStability
@Backing(type="int")
enum HexlpError {
    HEXLP_OK = 0,
    HEXLP_ERR,
    HEXLP_PENDING,
    HEXLP_TIMEOUT,
    HEXLP_ERR_STATE,
    HEXLP_ERR_INVALID_CFG,
    HEXLP_ERR_PARAM,
    HEXLP_ERR_NO_MEM,
    HEXLP_ERR_RESOURCES,
    HEXLP_ERR_HW,
    HEXLP_ERR_FATAL,
    HEXLP_ERR_CUSTOM,
    HEXLP_ERR_MAX,
}
