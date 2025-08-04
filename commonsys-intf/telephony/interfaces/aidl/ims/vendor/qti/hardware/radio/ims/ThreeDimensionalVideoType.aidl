/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum ThreeDimensionalVideoType {
    /*
    * None if 3DVideoType is not present.
    */
    NONE = 0,
    /*
    * 3D video and the format is half-width side-by-side.
    */
    SBS = 1,
    /*
    * 3D video and the format is full-width side-by-side.
    */
    SBS_FULL = 2,
    /*
    * 3D video and the format is half-width top-and-bottom.
    */
    TAB = 3,
    /*
    * 3D video and the format is full-width top-and-bottom.
    */
    TAB_FULL = 4,
}