/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.snapdragonServices.qape;

@VintfStability
interface IQapeCallback {
    oneway void onNotify(in int gameID, in int pid, in int cmdType, in String[] keys,
        in double[] values);
}
