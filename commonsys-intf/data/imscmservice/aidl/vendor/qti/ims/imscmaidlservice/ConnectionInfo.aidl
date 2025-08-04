/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.IImsCMConnection;

/*
 * ConnectionInfo data type 
 *    connection                      IImsCMConnection instance generated.
 *    connectionHandle                unique ID for connection created.
 *    listenerToken                   unique ID for cmConnListener object.
 */
@VintfStability
parcelable ConnectionInfo {
    IImsCMConnection connection;
    long connectionHandle;
    long listenerToken;
}
