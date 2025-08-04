/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallType;
import vendor.qti.hardware.radio.ims.RttMode;
import vendor.qti.hardware.radio.ims.StatusForAccessTech;
import vendor.qti.hardware.radio.ims.StatusType;

/**
 * ServiceStatusInfo is used to indicate/update the feature tags like VoLTE, VT over LTE/WiFi.
 * Lower layers will process based on CallType, StatusForAccessTech#networkMode and
 * StatusForAccessTech#status.
 */
@VintfStability
parcelable ServiceStatusInfo {
    boolean isValid;
    CallType callType = CallType.UNKNOWN;
    StatusType status = StatusType.INVALID;
    int restrictCause;
    StatusForAccessTech[] accTechStatus;
    RttMode rttMode = RttMode.INVALID;
}

