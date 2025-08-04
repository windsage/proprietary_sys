/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpMemBuffer;

@VintfStability
parcelable HexlpSPPBuffer {
    HexlpMemBuffer[] mem_bufs;
    /*
     * ! Buffer flags, as defined by enum HexlpBufferFlag.
     */
    int flags;
    /*
     * ! Timestamp in microseconds.
     */
    long timestamp;
    /*
     * ! Cookie to be copied from the input buffer to the output buffer.
     */
    long cookie_in_to_out;
}
