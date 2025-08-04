/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.connectionaidl;

import vendor.qti.hardware.data.connectionaidl.AllocatedBearers;

@VintfStability
interface IDataConnectionIndication {
    /**
     * Indicates that the bearer allocation changed.
     *
     * @param bearersList Updated list of bearers for all calls
     */
    oneway void onBearerAllocationUpdate(in AllocatedBearers[] bearersList);
}
