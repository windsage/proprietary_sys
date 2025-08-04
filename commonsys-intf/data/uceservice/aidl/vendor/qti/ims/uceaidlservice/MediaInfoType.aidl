/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

@VintfStability
@Backing(type="int")
enum MediaInfoType {
    CAP_NONE,
    /**
     * voice capability
     */
    CAP_FULL_AUDIO_ONLY,
    /**
     * voice and video capability
     */
    CAP_FULL_AUDIO_AND_VIDEO,
    CAP_UNKNOWN,
}
