/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.data.dmapconsent;

@VintfStability
@Backing(type="int")
enum ConsentType {
  CONSENT_FINE_LOCATION = 0,
  CONSENT_COARSE_LOCATION = 1,
  CONSENT_GENERAL = 2,
  CONSENT_DIAG = 3,
}
