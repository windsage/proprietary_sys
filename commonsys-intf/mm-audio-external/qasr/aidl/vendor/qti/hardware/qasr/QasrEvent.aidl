/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.qasr;

/** Output event */

@VintfStability
parcelable QasrEvent {

    @Backing(type="int") @VintfStability
    enum EventStatus {
      /**
       * Used as default value in parcelables to indicate that a value was not
       * set. Should never be considered a valid setting.
       */
      INVALID = -1,
      /** Speech Recognition success. */
      SUCCESS = 0,
      /** Speech Recognition aborted. */
      ABORTED = 1,
    }

    parcelable Event {
        /** Final transcription after end of speech is detected. */
        boolean is_final;
        /** Confidence for the entire transcription. */
        int confidence;
        /** Recognized text transcription. */
        String text;
        /** Additonal text transcription information in JSON format. */
        String result_json;
        /** Additional Engine specific custom data. */
        byte[] data;
    }

    vendor.qti.hardware.qasr.QasrEvent.EventStatus status = EventStatus.INVALID;
    /**
     * One or more ASR output text transcriptions depending on
     * {@link QasrConfig.outputBufferMode} is enabled or not.
     */
    Event[] event;
}
