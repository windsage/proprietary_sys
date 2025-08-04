/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.fingerprint;

import vendor.qti.hardware.fingerprint.CaptureDebugData;

@VintfStability
interface IQfpExtendedSessionCallback {
    /**
     * Callback fired when fingerprint image capture is successfull.
     *
     * @param LUTUsed       LUT used for image capture.
     * @param currSystemLUT current sytem LUT.
     * @param numImages     number of images.
     * @param imagesData    images data.
     */
    void onCaptureImage(in int LUTUsed, in int currSystemLUT, in int numImages, in byte[] imagesData);

    /**
     * Callback fired when debug data is captured.
     *
     * @datas debug data.
     */
    void onCaptureDebugData(in CaptureDebugData[] data);

    /**
     * Callback fired when fingerprint operation returns error.
     *
     * @param error error code.
     */
    void onError(in int error);

    /**
     * Callback fired to propagate status of fingerprint operation.
     *
     * @param status    status info.
     * @param extension additional status info.
     */
    void onStatus(in int status, in byte[] extension);

    /**
     * Callback to provide the Air Image that was captured.
     *
     * @param status    status info.
     * @param flags     flags indicating the capture mode.
     * @param data      Air Image data.
     */
    void onAirImageCapture(in int status, in int flags, in byte[] data);

    /**
     * Callback to propagate the HRM response.
     *
     * @param data      HRM response data.
     */
    void onHrmCapture(in int info, in byte[] data);

    /**
     * Callback to notify Tie or Untie operation status.
     *
     * @param reqType   determines tie or untie request type.
     * @param data      tie/untie fingerprint Ids
     */
    void onTieUntie(in int reqType, in byte[] data);

    /**
     * Callback to provide Tied fingers list.
     *
     * @param fingeprintIds tie/untie fingerprint Ids
     */
    void onEnumerateTieList(in byte[] fingeprintIds);

    /**
     * Callback to provide the status of QFS Setting update.
     *
     * @param reqType   determines the request type.
     * @param data      QFS Setting Id and value.
     */
    void onQfsSetting(in int reqType, in byte[] data);

    /**
     * Callback to notify ForceSense response.
     *
     * @param cmd       determines the command type.
     * @param status    status of the request.
     * @param stage     current ForceSense state or sub-command Id.
     * @param data      other ForceSense data.
     */
    void onForceSense(in int cmd, in int status, in int stage, in byte[] data);

    /**
     * Callback to notify Stylus response.
     *
     * @param data      Stylus response data.
     */
    void onStylus(in byte[] data);
}
