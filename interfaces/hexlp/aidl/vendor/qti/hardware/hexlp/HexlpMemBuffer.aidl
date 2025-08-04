/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import android.hardware.common.NativeHandle;
import vendor.qti.hardware.hexlp.HexlpFence;

@VintfStability
parcelable HexlpMemBuffer {
    /*
     * ! The destination port this buffer should be sent to.
     */
    int port_id;
    /*
     * ! The unique buffer identifier.
     */
    int cookie;
    /*
     * ! The acutal buffer fd, can be NULL if this buffer has been pre-mapped.
     */
    @nullable NativeHandle buffer_fd;
    int offset;
    int alloc_length;
    int valid_length;
    @nullable HexlpFence[] fence_info;
}
