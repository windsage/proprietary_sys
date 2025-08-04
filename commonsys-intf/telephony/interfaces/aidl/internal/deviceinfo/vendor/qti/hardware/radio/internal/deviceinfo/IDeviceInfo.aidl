/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.internal.deviceinfo;

import vendor.qti.hardware.radio.internal.deviceinfo.DevicePowerInfo;
import vendor.qti.hardware.radio.internal.deviceinfo.DeviceState;
import vendor.qti.hardware.radio.internal.deviceinfo.IDeviceInfoIndication;
import vendor.qti.hardware.radio.internal.deviceinfo.IDeviceInfoResponse;

@VintfStability
interface IDeviceInfo {

    /**
     * Set callbacks for device info responses (corresponding to requests) and indications
     *
     * @param deviceInfoResponseCb -  Callback object for responses to Device Info HIDL requests
     *
     * @param deviceInfoIndicationCb - Callback object for Device info related indications
     */
    oneway void setCallbacks(in IDeviceInfoResponse deviceInfoResponseCb,
            in IDeviceInfoIndication deviceInfoIndicationCb);

    /**
     * Notify RIL whether we support feature for reporting device power info and device interactive
     * info so it could be conveyed to any consumers of this data. This is part of a 2-way
     * handshake between the two entities wherein we send if feature is supported to interested
     * consumers which should then send an indication requesting that we start reporting the data
     *
     * @param serial - Serial number of request.
     *
     * @param supportDevicePowerInfo - Boolean indicating if we support reporting device power info
     *
     * @param supportDeviceInteractiveInfo - Boolean indicating if we support reporting device
     * interactive state info
     *
     * Response callback is IDeviceInfoResponse.sendFeaturesSupportedResponse()
     */
    oneway void sendFeaturesSupported(in int serial, in boolean supportDevicePowerInfo,
            in boolean supportDeviceInteractiveInfo);

    /**
     * Send device power info to RIL.
     *
     * @param serial - Serial number of request.
     *
     * @param DevicePowerInfo - the battery statistics – charging state of device, type of charger,
     * battery capacity (current and total), power save mode enabled, etc.
     *
     * Response callback is IDeviceInfoResponse.sendDevicePowerInfoResponse()
     */
    oneway void sendDevicePowerInfo(in int serial, in DevicePowerInfo devicePowerInfo);

    /**
     * Send device interactive state info to RIL
     *
     * @param serial - Serial number of request.
     *
     * @param deviceState - enum DeviceState - INVALID, INTERACTIVE, NON_INTERACTIVE
     * @See PowerManager#isInteractive()
     *
     * Response callback is IDeviceInfoResponse.sendDeviceInteractiveInfoResponse()
     */
    oneway void sendDeviceInteractiveInfo(in int serial, in DeviceState deviceState);
}
