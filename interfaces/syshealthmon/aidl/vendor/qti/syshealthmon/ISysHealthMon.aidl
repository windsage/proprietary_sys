/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.syshealthmon;

@VintfStability
interface ISysHealthMon {

    /**
     * Check the system health
     *
     * @param subsys Specifies subsys name needs to be checked.
     *
     * @return: 0 on success, -1 on failure.
     */
    int checkSystemHealth(in String subsys);
}
