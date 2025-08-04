/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */


package vendor.qti.hardware.radio.ims;

@VintfStability
parcelable ImsSubConfigInfo {
    int simultStackCount;
    /*
     * Simultaneous IMS stack static
     * support, is 1 for 7+1, 7+5 and
     * 7+7 reduced scope; is 2 for
     * 7+7 full scope
     */
    boolean[] imsStackEnabled;
}

