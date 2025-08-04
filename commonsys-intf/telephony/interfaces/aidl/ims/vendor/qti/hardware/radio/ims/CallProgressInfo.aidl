/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallProgressInfoType;

/**
 * CallProgressInfo is used to notify call progress information for MO calls during alerting stage.
 * Telephony will consider CallProgressInfo only if CallProgressInfo#CallProgressInfoType is not
 * CallProgressInfoType#INVALID.
 */
@VintfStability
parcelable CallProgressInfo {
    /*
     * Type of call progress info
     */
    CallProgressInfoType type = CallProgressInfoType.INVALID;
    /*
     * Reason code for call rejection, this will be valid only for
     * CallProgressInfoType#CALL_REJ_Q850.
     */
    int reasonCode;
    String reasonText;
}

