/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.UimLpaProfileInfo;
import vendor.qti.hardware.radio.lpa.UimLpaResult;
import vendor.qti.hardware.radio.lpa.UimLpaSrvAddrOpResp;
import vendor.qti.hardware.radio.lpa.UimLpaUserEventId;

@VintfStability
@JavaDerive(toString=true)
parcelable UimLpaUserResp {

    /**
     *
     * UimLpaUser EventId
     */
    UimLpaUserEventId event;

    /**
     *
     * UimLpaResult can be SUCCESS, FAILURE OR CODE_MISSING
     */
    UimLpaResult result;

    /**
     *
     * eid
     */
    byte[] eid;

    /**
     *
     * UimLpa profiles
     */
    UimLpaProfileInfo[] profiles;

    /**
     *
     * UimLpaSrvAddrOpResp
     */
    UimLpaSrvAddrOpResp srvAddr;

    /**
     *
     * euicc_info
     */
    byte[] euicc_info2;
}
