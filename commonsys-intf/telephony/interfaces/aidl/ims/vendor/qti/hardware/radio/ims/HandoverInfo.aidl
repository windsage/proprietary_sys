/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.Extra;
import vendor.qti.hardware.radio.ims.HandoverType;
import vendor.qti.hardware.radio.ims.RadioTechType;

@VintfStability
parcelable HandoverInfo {
    HandoverType type = HandoverType.INVALID;
    RadioTechType srcTech = RadioTechType.INVALID;
    RadioTechType targetTech = RadioTechType.INVALID;
    Extra hoExtra;
    String errorCode;
    String errorMessage;
}

