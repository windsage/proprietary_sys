/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.connectionaidl;

import vendor.qti.hardware.data.connectionaidl.IDataConnectionIndication;
import vendor.qti.hardware.data.connectionaidl.IDataConnectionResponse;
import vendor.qti.hardware.data.connectionaidl.StatusCode;

@VintfStability
interface IDataConnection {
    /**
     * Queries for allocated bearers on all established data calls.
     * The caller will receive the response through
     * IDataConnectionResponse.onAllBearerAllocationsResponse()
     *
     * @param response Object which contains the response function
     * @return OK or NOT_SUPPORTED
     */
    StatusCode getAllBearerAllocations(in IDataConnectionResponse response);

    /**
     * Queries for allocated bearers on the specified data call. Data
     * connection must be established. The caller will receive the response
     * through IDataConnectionResponse.onBearerAllocationResponse()
     *
     * @param cid The call id to request bearer information for
     * @param response Object which contains the response function
     * @return OK or NOT_SUPPORTED
     */
    StatusCode getBearerAllocation(in int cid, in IDataConnectionResponse response);

    /**
     * Allows the client to query the vendor property value
     * via key
     *
     * @param key: property name
     * @param currentConfig: default key value
     * @param out result: property value
     */
    String getConfig(in String key, in String defaultValue);

    /**
     * Begin listening for bearer allocation changes on all established data
     * calls. This indication will be invoked whenever UL or DL bearer type
     * changes. When a call is disconnected, it will be removed from the list.
     * The caller will be notified through
     * IDataConnectionIndication.onBearerAllocationUpdate()
     *
     * @param indication Object which will be notified of changes
     * @return OK or NOT_SUPPORTED
     */
    StatusCode registerForAllBearerAllocationUpdates(in IDataConnectionIndication indication);
}
