/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.trustedui;

import android.hardware.common.Ashmem;

@VintfStability
parcelable TUICreateParams {
    /**
     * String identifier for the trusted app running on the selected execute engine, can be app_name
     */
    String trustedEE;
    /**
     * Unique display identifier for display index, on which the session is requested.
     * @note  A vector of display ids can be obtained by invoking getPhysicalDisplayIds() API
     * from SurfaceControl (Java) or ISurfaceComposer (JNI).
     */
    int dpyIdx;
    /**
     * This is an _optional_ parameter to request minimum size required for the shared memory
     * buffer in bytes. HAL may override this size with a larger size if the requested size
     * is inadequate.
     * @note The shared buffer should be able to hold both command and response data. Client may
     * set minSharedMemSize = 0 if it does not want to recommend any size.
     */
    int minSharedMemSize;
    /**
     * Ashmem for loading TA from buffer. it's an _optional_ parameter to pass TA image data
     * client to TUI HAL.
     */
    Ashmem appBin;
    /**
     * _Optional_ unique identifier for TUI App.
     */
    int TUI_UID;
}
