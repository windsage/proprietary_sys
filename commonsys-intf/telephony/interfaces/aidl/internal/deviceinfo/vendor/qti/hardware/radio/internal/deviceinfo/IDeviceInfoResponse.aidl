/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.internal.deviceinfo;

@VintfStability
interface IDeviceInfoResponse {

    /**
     * Response to the sendFeaturesSupported Request
     *
     * @param serial - Serial number of request.
     *
     * @param errorCode - Error code returned from RIL
     */
    oneway void sendFeaturesSupportedResponse(in int serial, in int errorCode);

    /**
     * Response to the sendDevicePowerInfo Request
     *
     * @param serial - Serial number of request.
     *
     * @param errorCode - Error code returned from RIL
     */
    oneway void sendDevicePowerInfoResponse(in int serial, in int errorCode);

    /**
     * Response to the sendDeviceInteractiveInfo Request
     *
     * @param serial - Serial number of request.
     *
     * @param errorCode - Error code returned from RIL
     */
    oneway void sendDeviceInteractiveInfoResponse(in int serial, in int errorCode);
}
