/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.qasr;

@VintfStability
interface IQasrGlobalCallback {
    /**
     * Callback method called by the ASR service whenever internal conditions
     * have been made available, such that A) a call that would previously have
     * failed with an RESOURCE_CONTENTION status may now succeed if retried by client,
     * B) the previously pre-empted ASR processing, for which an ABORTED event was
     * sent, is ready to process with {@link #IQasr.startLisenting()} method.
     */
    oneway void onResourcesAvailable();
}
