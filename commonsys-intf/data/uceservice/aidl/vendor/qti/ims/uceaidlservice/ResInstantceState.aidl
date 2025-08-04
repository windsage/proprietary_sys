/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

@VintfStability
@Backing(type="int")
enum ResInstantceState {
    /**
     * capability polling state on network active
     */
    ACTIVE,
    /**
     * capability polling state on network pending
     */
    PENDING,
    /**
     * capability polling state on network terminated
     */
    TERMINATED,
    /**
     * capability polling state on network unknown
     */
    UNKNOWN,
}
