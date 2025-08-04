/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.RadioTechType;
import vendor.qti.hardware.radio.ims.RegistrationInfo;
import vendor.qti.hardware.radio.ims.StatusType;

@VintfStability
parcelable StatusForAccessTech {
    RadioTechType networkMode = RadioTechType.INVALID;
    StatusType status = StatusType.INVALID;
    int restrictCause;
    boolean hasRegistration;
    RegistrationInfo registration;
}
