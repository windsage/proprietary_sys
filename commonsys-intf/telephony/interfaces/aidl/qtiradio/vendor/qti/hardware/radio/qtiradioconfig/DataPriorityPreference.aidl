/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradioconfig;

@VintfStability
@Backing(type="int")
enum DataPriorityPreference {
    /*Data priority subscription not specified*/
    DATA_PRIORITY_SUB_DEFAULT = 0,
    /* Possible maximum aggregate data throughput (SUB1 and SUB2 together)*/
    DATA_PRIORITY_SUB_MAX_TPUT = 1 << 0,
}
