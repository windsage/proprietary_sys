/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallFailCause;
import vendor.qti.hardware.radio.ims.RadioTechType;

/**
 * RedialInfo will be sent when lower layers end the call asking Android Telephony to redial.
 * Lower layers will process the data only if CallFailCause is not equal to
 * CallFailCause.INVALID or RadioTechType is not equal to RadioTechType.INVALID.
 */
@VintfStability
parcelable RedialInfo {
    /*
     * Holds the reason why the call is redialed
     * Default Value: CallFailCause#INVALID
     */
    CallFailCause callFailReason = CallFailCause.INVALID;
    /*
     * Holds the radioTech on which lower layers may
     * attempt redialing the call
     * Default Value: RadioTechType#INVALID
     */
    RadioTechType callFailRadioTech = RadioTechType.INVALID;
}
