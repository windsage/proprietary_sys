/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

@VintfStability
@Backing(type="int")
enum HexlpFenceBackingType {
    DMA_FENCE   =  0,
    CSL_FENCE   =  1,
    SYNX_FENCE  =  2,
}
