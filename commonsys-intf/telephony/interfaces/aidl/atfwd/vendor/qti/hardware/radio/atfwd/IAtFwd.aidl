/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atfwd;

import vendor.qti.hardware.radio.atfwd.AtCmdResponse;
import vendor.qti.hardware.radio.atfwd.IAtFwdIndication;

/**
 * Interface used to communicate between native and Java modules for forwarding and processing
 * AT commands sent from the modem.
 */
@VintfStability
interface IAtFwd {

    /**
     * Set a callback interface that would receive AT command indications.
     *
     * @param atFwdIndicationCb - Callback object for indications
     */
    oneway void setIndicationCallback(in IAtFwdIndication atFwdIndicationCb);

    /**
     * Send the result after processing the AT command received via
     * IAtFwdIndication#onAtCommandForwarded(int, AtCmd).
     *
     * @param serial is the serial number of the command, for which
     *        the result is being sent
     * @param state is the result of the AT command after being processed
     */
    oneway void sendAtCommandProcessedState(in int serial, in AtCmdResponse state);

}
