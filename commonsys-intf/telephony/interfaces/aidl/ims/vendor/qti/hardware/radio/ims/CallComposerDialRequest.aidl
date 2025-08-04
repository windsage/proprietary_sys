/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallComposerInfo;
import vendor.qti.hardware.radio.ims.DialRequest;

@VintfStability
parcelable CallComposerDialRequest {
    DialRequest dialRequest;
    CallComposerInfo callComposerInfo;
}

