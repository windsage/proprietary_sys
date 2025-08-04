/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.ServiceClassStatus;

@VintfStability
parcelable CbNumInfo {
    ServiceClassStatus status = ServiceClassStatus.INVALID;
    String number;
}

