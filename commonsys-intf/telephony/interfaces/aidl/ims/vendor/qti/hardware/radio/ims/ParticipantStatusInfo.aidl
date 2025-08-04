/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.ConfParticipantOperation;

/**
 * ParticipantStatusInfo is used to indicate the participant status details.
 * Telephony will validate based on each field callId if not Integer.MAX_VALUE,
 * operation if not ConfParticipantOperation#INVALID, sipStatus if not Integer.MAX_VALUE.
 */
@VintfStability
parcelable ParticipantStatusInfo {
    int callId;
    ConfParticipantOperation operation = ConfParticipantOperation.INVALID;
    /*
     * sip error code as defined in RFC3261
     */
    int sipStatus;
    String participantUri;
    boolean isEct;
}

