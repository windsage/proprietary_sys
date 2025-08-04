/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpFenceScope;
import vendor.qti.hardware.hexlp.HexlpFenceBackingType;
import android.hardware.common.NativeHandle;

@VintfStability
parcelable HexlpFence {
    HexlpFenceScope         fence_scope;
    HexlpFenceBackingType   fence_backing_type;
    NativeHandle            fence_fd;
}
