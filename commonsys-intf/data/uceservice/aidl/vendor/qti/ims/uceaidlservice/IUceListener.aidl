/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.UceStatus;

@VintfStability
interface IUceListener {
    /**
     *  Notifies Uce Service status for respective iccId/subscription.
     *
     * @param status        Status of the service.
     * @param iccId         iccId related to the subscription.
     *
     *
     */
    oneway void onStatusChange(in UceStatus status, in String iccId);
}
