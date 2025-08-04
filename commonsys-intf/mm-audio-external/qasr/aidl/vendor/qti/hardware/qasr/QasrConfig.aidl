/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.qasr;

@VintfStability
parcelable QasrConfig {
    @Backing(type="int") @VintfStability
    /** @deprecated Language detection code values. */
    enum LanguageCode {
        EN_US = 0,
        ZH_CN = 1,
        HI_IN = 2,
        ES_US = 3,
        KO_KR = 4,
        JA_JP = 5,
    }

    /** @deprecated Input language detection code. */
    LanguageCode input_language_code = LanguageCode.EN_US;
    /** @deprecated Output language detection code. */
    LanguageCode output_language_code = LanguageCode.EN_US;
    /** Enable or disable language detection. */
    boolean enable_language_detection;
    /** Enable or disable launguage translation. */
    boolean enable_translation;
    /**
     * Enable or disable continuous mode. In continuous mode each event contains
     * continuation of text transcriptions, i.e. the continuation of text
     * transcription in next event doesn't include the previous event text
     * transcription.
     */
    boolean enable_continuous_mode;
    /** Enable partial transcription */
    boolean enable_partial_transcription;
    /** Confidence threshold for ASR transcription */
    int threshold;
    /**
     * ASR algo processing timeout in milliseconds if silence is not detected
     * after the speech processing is started. If not set, defaults to no timeout.
     */
    int timeout_duration = 0;
    /**
     * Silence duration in milliseconds at the end of the speech before
     * determining speech has ended. If set to 0, default platform
     * duration will be used
     */
    int silence_detection_duration = 0;

    /**
     * If set to true, the ASR sends text transcrptions only after certain max
     * buffer size supported by platform. i.e. the buffer can have multiple text
     * transcription events with is_final true. This is typically used for
     * transcription usecases that doesn't need immediate user attention, but to
     * store the transcriptions for viewing later. If set to false, output
     * will be provided when ASR generates the text transcription.
     */
    boolean outputBufferMode;
    /** Additional engine specific custom data. */
    byte[] data;
    /** Input language detection code. */
    String input_lang_code = "en-US";
    /** Output language detection code. */
    String output_lang_code = "en-US";
}
