/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.connectionaidl;

import vendor.qti.hardware.data.connectionaidl.AllocatedBearers;
import vendor.qti.hardware.data.connectionaidl.ErrorReason;

@VintfStability
interface IDataConnectionResponse {
    /**
     * Response to IDataConnection.getAllBearerAllocations()
     *
     * @param error Failure reason if the query was unsuccessful
     * @param bearersList Current list of bearers for all data calls
     */
    oneway void onAllBearerAllocationsResponse(in ErrorReason error,
        in AllocatedBearers[] bearersList);

    /**
     * Response to IDataConnection.getBearerAllocation()
     *
     * @param error Failure reason if the query was unsuccessful
     * @param bearers Current list of bearers for the requested data call
     */
    oneway void onBearerAllocationResponse(in ErrorReason error, in AllocatedBearers bearers);
}
