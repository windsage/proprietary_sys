/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.qasr;

import vendor.qti.hardware.qasr.QasrEvent;

@VintfStability
interface IQasrCallback {
    /**
     * ASR output is sent through this callback as an event.
     *
     * @param handle Session handle
     * @param event ASR event that contains the text transcription and
     *      assoicated meta data.
     */
    oneway void eventCallback(in int handle, in QasrEvent event);
}