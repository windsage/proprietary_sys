/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.ConferenceAbortReason;

@VintfStability
parcelable ConferenceAbortReasonInfo {
    /*
     * Reason for aborting the conference
     */
    ConferenceAbortReason conferenceAbortReason = ConferenceAbortReason.INVALID;
}

