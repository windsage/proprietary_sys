/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.VosMoveInfo;
import vendor.qti.hardware.radio.ims.VosMoveInfo2;
import vendor.qti.hardware.radio.ims.VosTouchInfo;

/**
 * VosActionInfo is used to pass user action(move/touch) to lower layer and network.
 * Lower layers will process VosActionInfo based on individual parameters.
 */
@VintfStability
parcelable VosActionInfo {
    @nullable
    VosMoveInfo vosMoveInfo;
    @nullable
    VosTouchInfo vosTouchInfo;
    @nullable
    VosMoveInfo2 vosMoveInfo2;
}

