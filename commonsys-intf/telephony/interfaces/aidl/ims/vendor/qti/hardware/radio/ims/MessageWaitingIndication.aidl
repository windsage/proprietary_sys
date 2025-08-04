/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.MessageDetails;
import vendor.qti.hardware.radio.ims.MessageSummary;

@VintfStability
parcelable MessageWaitingIndication {
    MessageSummary[] messageSummary;
    String ueAddress;
    MessageDetails[] messageDetails;
}

