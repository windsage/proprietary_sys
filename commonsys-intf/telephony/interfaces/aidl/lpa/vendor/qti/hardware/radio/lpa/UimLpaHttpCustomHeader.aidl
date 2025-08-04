/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

@VintfStability
@JavaDerive(toString=true)
parcelable UimLpaHttpCustomHeader {

    /**
     *
     * String headerName
     */
    String headerName;

    /**
     *
     * String headerValue
     */
    String headerValue;
}
