/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.diaghal;

import vendor.qti.diaghal.ParcelableMemory;

@VintfStability
interface Idiagcallback {
    /**
     * send data from diag server to client
     *
     * @param mem input buffer
     * @param len length of buffer
     * @return indicates the status of the call
     */
    int send_data(in ParcelableMemory mem, in int len);
}
