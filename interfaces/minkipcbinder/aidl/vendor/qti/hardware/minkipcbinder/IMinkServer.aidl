/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.minkipcbinder;

import vendor.qti.hardware.minkipcbinder.IMinkTransact;


@VintfStability
interface IMinkServer {
    /* Client will connect to MinkIPCBinder server using connect 
      and receive server's MinkTransact handle on successful connection */
    IMinkTransact connect(in IMinkTransact clientMinkTransact);
    void disconnect(in IMinkTransact serverMinkTransact);
}
