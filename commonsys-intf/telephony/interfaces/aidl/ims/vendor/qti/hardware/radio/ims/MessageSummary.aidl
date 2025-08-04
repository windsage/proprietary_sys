/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.MwiMessageType;

@VintfStability
parcelable MessageSummary {
    MwiMessageType type = MwiMessageType.INVALID;
    int newMessageCount;
    int oldMessageCount;
    int newUrgentMessageCount;
    int oldUrgentMessageCount;
}

