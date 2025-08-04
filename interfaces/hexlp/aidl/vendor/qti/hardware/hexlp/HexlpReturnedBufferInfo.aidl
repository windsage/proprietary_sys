/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

@VintfStability
parcelable HexlpReturnedBufferInfo {
    /*
     * ! Predefined or custom port id.
     */
    int port_id;
    /*
     * ! The unique buffer identifier.
     */
    int cookie;
    /*
     * ! Actual filled length of a buffer, should be zero if it remains unfilled.
     */
    int filled_length;
}
