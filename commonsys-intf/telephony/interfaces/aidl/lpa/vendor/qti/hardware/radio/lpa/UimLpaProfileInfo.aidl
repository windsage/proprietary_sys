/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.UimLpaIconType;
import vendor.qti.hardware.radio.lpa.UimLpaProfileClassType;
import vendor.qti.hardware.radio.lpa.UimLpaProfilePolicyMask;
import vendor.qti.hardware.radio.lpa.UimLpaProfileState;

@VintfStability
@JavaDerive(toString=true)
parcelable UimLpaProfileInfo {

    /**
     *
     * ProfileState can be Active or InActive
     */
    UimLpaProfileState state;

    /**
     *
     * ProfileInfo iccid
     */
    byte[] iccid;

    /**
     *
     * UIMLpa profileName
     */
    String profileName;

    /**
     *
     * UIMLpa nickName
     */
    String nickName;

    /**
     *
     * UIMLpa spName
     */
    String spName;

    /**
     *
     * iconType can be JPEG or PNG
     */
    UimLpaIconType iconType;

    /**
     *
     * ProfileInfo icon
     */
    byte[] icon;

    /**
     *
     * profileClass can be TEST, PROVISIONING or OPERATIONAL
     */
    UimLpaProfileClassType profileClass;

    /**
     *
     * profilePolicy can be DISABLE_NOT_ALLOWED, DELETE_NOT_ALLOWED or DELETE_ON_DISABLE
     */
    UimLpaProfilePolicyMask profilePolicy;
}
