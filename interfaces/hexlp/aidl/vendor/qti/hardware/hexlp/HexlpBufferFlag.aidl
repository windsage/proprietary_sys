/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

@VintfStability
@Backing(type="int")
enum HexlpBufferFlag {
    HEXLP_BUFFER_FLAG_EOS = (1 << 0),
    HEXLP_BUFFER_FLAG_DATACORRUPT = (1 << 1),
    HEXLP_BUFFER_FLAG_READONLY = (1 << 2),
    HEXLP_BUFFER_FLAG_EXTRADATA = (1 << 3),
    HEXLP_BUFFER_FLAG_I_FRAME = (1 << 4),
    HEXLP_BUFFER_FLAG_B_FRAME = (1 << 5),
    HEXLP_BUFFER_FLAG_P_FRAME = (1 << 6),
    HEXLP_BUFFER_FLAG_PENDING_OUTPUT = (1 << 7),
    HEXLP_BUFFER_FLAG_BYPASS = (1 << 8),
    HEXLP_BUFFER_FLAG_CUSTOM = (1 << 9),
}
