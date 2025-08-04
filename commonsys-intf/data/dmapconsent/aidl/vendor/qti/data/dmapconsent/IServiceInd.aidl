/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.data.dmapconsent;

import vendor.qti.data.dmapconsent.ServiceStatus;

@VintfStability
interface IServiceInd {
    /**
     * Called with the current service status whenever the service status changes or
     * after registering for service status updates.
     *
     * @param status             Status of the service.
     *
     * @return                   None.
     */
    oneway void consentServiceInd(in ServiceStatus status);
}
