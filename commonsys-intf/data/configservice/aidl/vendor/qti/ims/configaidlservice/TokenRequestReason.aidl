/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/**
 * Token Request Trigger Reasons
 */
@VintfStability
@Backing(type="int")
enum TokenRequestReason {
    /**
     * No Reason
     */
    REASON_DO_NOT_EXIST = 0,
    /**
     * Token doesn't exist
     */
    REASON_RCS_TOKEN_DO_NOT_EXIST = 1,
    /**
     * Sim swap happened
     */
    REASON_SIM_SWAP = 2,
    /**
     * Token exist but it is invalid
     */
    REASON_INVALID_TOKEN = 3,
    /**
     * Token exist but it expired
     */
    REASON_EXPIRED_TOKEN = 4,
    /**
     * Client has changed
     */
    REASON_CLIENT_CHANGE = 5,
    /**
     * Device upgrade took place
     */
    REASON_DEVICE_UPGRADE = 6,
    /**
     * Factory reset happened
     */
    REASON_FACTORY_RESET = 7,
}
