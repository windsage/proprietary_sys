/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.qasr;

/** ASR model information. */

@VintfStability
parcelable QasrModel {
    /** ASR vendor uuid that uniquely identifies the ASR engine. */
    String vendorUuid;
    /**
     * Optional ASR model. If not passed, a predefined default model of the
     * platform will be used.
     */
    @nullable ParcelFileDescriptor data;
    /** ASR model size. Size is zero if no model data is passed. */
    int dataSize;
}
