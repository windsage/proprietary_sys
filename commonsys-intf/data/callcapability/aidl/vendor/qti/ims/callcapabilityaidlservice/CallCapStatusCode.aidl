/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.callcapabilityaidlservice;

/**
 * IMS Call Capability Status codes
 */
@VintfStability
@Backing(type="int")
enum CallCapStatusCode {
    OK = 0,
    FAIL = 1,
    NOT_SUPPORTED = 2,
}
