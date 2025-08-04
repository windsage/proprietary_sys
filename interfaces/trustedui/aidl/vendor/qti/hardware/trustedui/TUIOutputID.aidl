/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.trustedui;

@VintfStability
parcelable TUIOutputID {
  /**
  * indicates Session identifier
  * Valid in case of success and should be ignored in case of an error
  *
  */
  int sessionId;
  /**
  * indicates TrustedInput device identifier
  *
  */
  int trustedInputID;
}
