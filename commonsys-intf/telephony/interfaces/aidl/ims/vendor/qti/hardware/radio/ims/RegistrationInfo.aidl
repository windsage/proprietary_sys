/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.RadioTechType;
import vendor.qti.hardware.radio.ims.RegState;

@VintfStability
parcelable RegistrationInfo {
    RegState state = RegState.INVALID;
    int errorCode;
    String errorMessage;
    RadioTechType radioTech = RadioTechType.INVALID;
    String pAssociatedUris;
}

