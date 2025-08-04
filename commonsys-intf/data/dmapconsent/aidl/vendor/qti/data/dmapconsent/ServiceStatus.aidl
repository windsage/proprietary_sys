/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.data.dmapconsent;

@VintfStability
@Backing(type="int")
enum ServiceStatus {
  SERVICE_INVALID = -1,
  SERVICE_SUCCESS = 0,
  SERVICE_INVALID_ARG = 1,
  SERVICE_INTERNAL_ERR = 2,
  SERVICE_NOT_SUPPORTED = 3,
  SERVICE_INVALID_OPERATION  = 4,
  SERVICE_UP = 5,
  SERVICE_DOWN = 6,
  DMAP_RESET = 8,
}
