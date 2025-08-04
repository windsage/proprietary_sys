/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.MwiMessagePriority;
import vendor.qti.hardware.radio.ims.MwiMessageType;

@VintfStability
parcelable MessageDetails {
    String toAddress;
    String fromAddress;
    String subject;
    String date;
    MwiMessagePriority priority = MwiMessagePriority.INVALID;
    String id;
    MwiMessageType type = MwiMessageType.INVALID;
}

