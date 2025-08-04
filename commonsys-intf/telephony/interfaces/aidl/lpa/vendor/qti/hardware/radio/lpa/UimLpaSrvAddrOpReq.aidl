/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.UimLpaSrvAddrOp;

@VintfStability
@JavaDerive(toString=true)
parcelable UimLpaSrvAddrOpReq {

    /**
     *
     * opCode can be GET_OPERATION or SET_OPERATION
     */
    UimLpaSrvAddrOp opCode;

    /**
     *
     * smdpAddress
     */
    String smdpAddress;
}
