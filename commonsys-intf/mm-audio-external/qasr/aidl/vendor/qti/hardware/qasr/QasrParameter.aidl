/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.qasr;

/** Parameters set through {@link #IQasr.setParamater()} method. */

@VintfStability
parcelable QasrParameter {
    @Backing(type="int") @VintfStability
    enum Parameter {
        /**
         * Used as default value in parcelables to indicate that a value was not
         * set. Should never be considered a valid setting.
         */
        INVALID = -1,
        /**
         * Force to get the available ASR output after {@link #IQasr.startListening()}.
         * Followed by setting this parameter, QasrEvent will be sent in the
         * callback with available ASR output. If ASR doesn't have any avaialble
         * output, the text size in the event will be zero.
         */
        FORCE_OUTPUT_EVENT = 0,
        /** custom param, for which the data is interpreted by the Vendor HAL. */
        CUSTOM_EXTRAS = 1,
    }

    vendor.qti.hardware.qasr.QasrParameter.Parameter param = Parameter.INVALID;
    /** Payload associated with the parameter, if applicable. */
    byte[] data;
}
