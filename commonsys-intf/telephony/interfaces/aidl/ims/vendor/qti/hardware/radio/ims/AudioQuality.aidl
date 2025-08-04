/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.Codec;
import vendor.qti.hardware.radio.ims.ComputedAudioQuality;

/**
 * AudioQuality is used to indicate audio quality information like codec used and computed audio
 * quality like HD, HD+ for the current call.
 */
@VintfStability
parcelable AudioQuality {
  Codec codec = Codec.INVALID;
  ComputedAudioQuality computedAudioQuality = ComputedAudioQuality.INVALID;
}
