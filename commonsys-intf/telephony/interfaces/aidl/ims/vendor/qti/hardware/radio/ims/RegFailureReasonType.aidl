/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum RegFailureReasonType {
    INVALID,
    UNSPECIFIED,
    MOBILE_IP,
    INTERNAL,
    CALL_MANAGER_DEFINED,
    TYPE_3GPP_SPEC_DEFINED,
    PPP,
    EHRPD,
    IPV6,
    IWLAN,
    HANDOFF,
}
