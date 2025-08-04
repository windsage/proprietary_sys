/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.data.dmapconsent;

@VintfStability
@Backing(type="int")
enum ConsentStatus {
  CONSENT_SUCCESS = 0,
  CONSENT_FAILURE = 1,
  CONSENT_INVALID_LICENSE = 2,
  CONSENT_INVALID_APP_IDENTIFIER = 3,
  CONSENT_ERROR = 4,
  CONSENT_INVALID_ARG = 5,
  CONSENT_DUPLICATE_REQUEST = 6,
  CONSENT_INVALID_TOKEN = 7,
}
