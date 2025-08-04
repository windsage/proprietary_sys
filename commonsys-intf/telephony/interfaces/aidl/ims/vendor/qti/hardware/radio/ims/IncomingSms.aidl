/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.VerificationStatus;

/**
 * Data structure contains details regarding the incoming sms over ims.
 * Telephony will process IncomingSms if IncomingSms#pdu and IncomingSms#format is not null.
 */
@VintfStability
parcelable IncomingSms {
    String format;
    byte[] pdu;
    VerificationStatus verstat = VerificationStatus.VALIDATION_NONE;
}

