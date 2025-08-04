/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpPredefinedPortType;
import vendor.qti.hardware.hexlp.HexlpPortDirection;
import vendor.qti.hardware.hexlp.HexlpPortFormat;

@VintfStability
parcelable HexlpPortParam {
    HexlpPredefinedPortType port_type;
    HexlpPortDirection direction;
    /*
     * ! Actual width needs to process.
     */
    int width;
    /*
     * ! Actual height needs to process.
     */
    int height;
    /*
     * !
     * Reserve this field for cases like buffer width is larger than actual
     * width due to alignment.
     * Client can fill the same value as actual width if no special requirement.
     */
    int aligned_width;
    /*
     * !
     * Reserve this field for cases like buffer height is larger than actual
     * height due to alignment.
     * Client can fill the same value as actual height if no special requirement.
     */
    int aligned_height;
    int stride;
    int scanlines;
    HexlpPortFormat fmt;
}
