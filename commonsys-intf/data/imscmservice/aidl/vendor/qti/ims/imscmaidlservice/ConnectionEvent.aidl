/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

/**
 *
 *
 * Connection Data Types
 *
 *
 *
 *
 * Connection Event definitions
 */
@VintfStability
@Backing(type="int")
enum ConnectionEvent {
    /**
     * Service is not registered.
     */
    SERVICE_NOTREGISTERED,
    /**
     * Service was registered successfully.
     */
    SERVICE_REGISTERED,
    /**
     * Service is allowed.
     */
    SERVICE_ALLOWED,
    /**
     * Service is not allowed.
     */
    SERVICE_NOTALLOWED,
    /**
     * Service is forcefully closed.
     */
    SERVICE_FORCEFUL_CLOSE,
    /**
     * Service has received HO Terminate event from HO Manager.
     */
    SERVICE_TERMINATE_CONNECTION,
    /**
     * Service is created successfully.
     */
    SERVICE_CREATED,
}
