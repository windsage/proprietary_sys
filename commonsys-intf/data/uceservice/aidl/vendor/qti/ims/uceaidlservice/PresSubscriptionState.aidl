/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

@VintfStability
@Backing(type="int")
enum PresSubscriptionState {
    /**
     * subscription state on network active
     */
    ACTIVE,
    /**
     * subscription state on network pending
     */
    PENDING,
    /**
     * subscription state on network terminated
     */
    TERMINATED,
    /**
     * subscription state on network unknown
     */
    UNKNOWN,
}
