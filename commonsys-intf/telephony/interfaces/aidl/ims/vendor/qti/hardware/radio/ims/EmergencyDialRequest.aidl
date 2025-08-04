/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.DialRequest;
import vendor.qti.hardware.radio.ims.EmergencyCallRoute;
import vendor.qti.hardware.radio.ims.EmergencyServiceCategory;

/**
 * EmergencyDialRequest is used to initiate emergency call.
 * Lower layers will process EmergencyDialRequest if EmergencyDialRequest#DialRequest#address
 * is not empty.
 */
@VintfStability
parcelable EmergencyDialRequest {
    DialRequest dialRequest;
    EmergencyServiceCategory categories = EmergencyServiceCategory.UNSPECIFIED;
    String[] urns;
    EmergencyCallRoute route = EmergencyCallRoute.UNKNOWN;
    boolean hasKnownUserIntentEmergency;
    boolean isTesting;
}
