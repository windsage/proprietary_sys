/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.annotation.NonNull;
import android.telephony.ims.ImsStreamMediaProfile;

/* This class is responsible to cache the audio quality received as part of call state change
 * indication.
 * Audio quality consists of
 *  Codec: audio codec used for call.
 *  ComputedAudioQuality: Lower layers will calculate the audio quality based on codec and bit rate
 *  used for call and will update computed audio quality to upper layers.
 */

public final class AudioQuality {
    public static final int INVALID = -1;

    // Audio codec used for call
    private int mCodec = ImsStreamMediaProfile.AUDIO_QUALITY_NONE;
    // Computed audio quality based on codec and bit rate
    private int mComputedAudioQuality = INVALID;

    public AudioQuality() {}

    /**
     * Constructor
     *
     * @param audioQuality a Non-Null AudioQuality object
     */
    public AudioQuality(@NonNull AudioQuality audioQuality) {
        this(audioQuality.getCodec(), audioQuality.getComputedAudioQuality());
    }

    public AudioQuality(int codec, int computedAudioQuality) {
        mCodec = codec;
        mComputedAudioQuality = computedAudioQuality;
    }

    /**
     * Method used to return audio codec used.
     *
     * @return codec {@link ImsStreamMediaProfile.AUDIO_QUALITY_*}
     */
    public int getCodec() {
        return mCodec;
    }

    /**
     * Method used to return computed audio quality.
     *
     * @return computedAudioQuality {@link QtiCallConstants#CALL_AUDIO_QUALITY_*}
     */
    public int getComputedAudioQuality() {
        return mComputedAudioQuality;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AudioQuality)) {
            return false;
        }
        AudioQuality in = (AudioQuality) obj;
        return this.mCodec == in.getCodec() &&
                this.mComputedAudioQuality == in.getComputedAudioQuality();
    }

    public String toString() {
        return "AudioQuality codec : " + mCodec + " computed audio quality : "
            + mComputedAudioQuality;
    }
}
