/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxacu;

public class aptxacuALSDefs {

  /** Message definitions */
  // Message type from ACU to ALS
  public static final int MESSAGE_TYPE_ALS = 1;

  // Message to ALS to give it the APTX_AND_APTX_HD_PRIORITY preference value when changed
  public static final String ACTION_ACU_APTX_AND_APTX_HD_PRIORITY =
      "ACTION_ACU_APTX_AND_APTX_HD_PRIORITY";
  // Message to ALS to give it the APTX_ADAPTIVE_96KHZ_SAMPLE_RATE preference value when
  // changed
  public static final String ACTION_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE =
      "ACTION_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE";
  // Message to ALS to give it the AUDIO_PROFILE_OVERRIDE preference value when changed
  public static final String ACTION_ACU_AUDIO_PROFILE_OVERRIDE =
      "ACTION_ACU_AUDIO_PROFILE_OVERRIDE";
  // Message to ALS to give it the APP_AUDIO_PROFILE_PREFERENCE_LIST preference value when changed
  public static final String ACTION_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST =
      "ACTION_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST";

  /** Message Parameter definitions */
  // Used with ACTION_ACU_APTX_AND_APTX_HD_PRIORITY
  public static final String APTX_AND_APTX_HD_PRIORITY = "aptx_and_aptx_hd_priority";

  // Used with ACTION_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE
  public static final String APTX_ADAPTIVE_96KHZ_SAMPLE_RATE = "aptx_adaptive_96khz_sample_rate";
  // Used with ACTION_ACU_AUDIO_PROFILE_OVERRIDE
  public static final String AUDIO_PROFILE_OVERRIDE = "audio_profile_override";
  // Used with ACTION_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST
  public static final String APP_AUDIO_PROFILE_PREFERENCE_LIST =
      "app_audio_profile_preference_list";

  // Message name from ALS to give it the updated preferences values, when changed
  public static final String ACTION_ALS_PREFERENCES_UPDATED = "ACTION_ALS_PREFERENCES_UPDATED";
}
