/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

/*
 * Service Status enums
 */
@VintfStability
@Backing(type="int")
enum ServiceStatus {
    STATUS_DEINIT,
    /**
     * < Status is NULL.
     */
    STATUS_INIT_IN_PROGRESS,
    /**
     * < Service is being brought up.
     */
    STATUS_SUCCESS,
    /**
     * < Service initialization was successful.
     */
    STATUS_FAILURE,
    /**
     * < Service initialization failed.
     */
    STATUS_SERVICE_DIED,
    /**
     * < Android native service stopped working.
     */
    STATUS_SERVICE_CLOSING,
    STATUS_SERVICE_CLOSED,
    /**
     * < Android native service is closing state
     */
    STATUS_SERVICE_RESTARTED,
    /**
     * < Android native service is restarted
     */
    STATUS_SERVICE_NOT_SUPPORTED,
    STATUS_SERVICE_UNKNOWN,
}
