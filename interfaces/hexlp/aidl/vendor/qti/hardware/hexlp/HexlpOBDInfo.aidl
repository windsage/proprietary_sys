/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpReturnedBufferInfo;

@VintfStability
parcelable HexlpOBDInfo {
    int flags;
    long timestamp;
    long cookie_in_to_out;
    HexlpReturnedBufferInfo[] returned_buffer_info;
}
