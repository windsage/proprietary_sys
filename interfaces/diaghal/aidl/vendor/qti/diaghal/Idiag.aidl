/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.diaghal;

import vendor.qti.diaghal.Idiagcallback;
import vendor.qti.diaghal.ParcelableMemory;

@VintfStability
interface Idiag {
    /**
     * close the client connection
     *
     * @return indicates whether close is successful or not
     */
    int close();

    /**
     * Performs the operation requested in cmd_code
     *
     * @param cmd_code ioctl command code
     * @param buf input buffer
     * @param len length of buffer
     * @return indicates whether ioctl is successful or not
     */
    int ioctl(in int cmd_code, in ParcelableMemory buf, in int len);

    /**
     * Adds client's callback to the list of clients on diag hal server side
     *
     * @param callback  callback obj of client
     * @return indicates whether the open is success or failure
     */
    int open(in Idiagcallback callback);

    /**
     * Write data from diag client to server
     *
     * @param buf input buffer
     * @param len length of buffer
     * @return indicates the status of write call
     */
    int write(in ParcelableMemory buf, in int len);
}
