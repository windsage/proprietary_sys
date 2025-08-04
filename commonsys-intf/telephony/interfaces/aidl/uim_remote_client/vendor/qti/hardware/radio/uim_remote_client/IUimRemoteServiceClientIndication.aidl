/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientPowerDownMode;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientVoltageClass;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientCardInitStatusType;

@VintfStability
interface IUimRemoteServiceClientIndication {
    /**
     * Uim remote client APDU indication
     *
     * @param apduInd APDU data
     */
    oneway void uimRemoteServiceClientApduInd(in byte[] apduInd);

    /**
     * Uim remote client card initialization status indication
     *
     * @param cardInitStatusInd of type UimRemoteClientCardInitStatusType
     */
    oneway void uimRemoteServiceClientCardInitStatusInd(
        in UimRemoteClientCardInitStatusType cardInitStatusInd);

    /**
     * Uim remote client connect Indication
     *
     */
    oneway void uimRemoteServiceClientConnectInd();

    /**
     * Uim remote client disconnect Indication
     *
     */
    oneway void uimRemoteServiceClientDisconnectInd();

    /**
     * Uim remote client power down indication
     *
     * @param powerDownMode Power down mode
     */
    oneway void uimRemoteServiceClientPowerDownInd(in boolean hasPowerDownMode,
        in UimRemoteClientPowerDownMode powerDownMode);

    /**
     * Uim remote client power up indication
     *
     * @param timeOut Response timeout in msecs
     * @param powerUpVoltageClass Power up Volatage class
     */
    oneway void uimRemoteServiceClientPowerUpInd(in boolean hasTimeOut, in int timeOut,
        in boolean hasVoltageClass, in UimRemoteClientVoltageClass powerUpVoltageClass);

    /**
     * Uim remote client reset Indication
     *
     */
    oneway void uimRemoteServiceClientResetInd();

    /**
     * Uim remote client service indication
     *
     * @param status true - service is up, false - service is down
     */
    oneway void uimRemoteServiceClientServiceInd(in boolean status);
}
