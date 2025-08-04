/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.AddressInfo;

/**
 * Data structure to store geo location information
 * such as latitude, longtitude and address.
 */
@VintfStability
parcelable GeoLocationInfo {
    double lat;
    double lon;
    AddressInfo addressInfo;
}
