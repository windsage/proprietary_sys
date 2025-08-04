/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atfwd;

import vendor.qti.hardware.radio.atfwd.AtCmd;

@VintfStability
interface IAtFwdIndication {

    /**
     * Indicates that an AT command has been forwarded from the modem.
     * Once the AT command is processed, its result should be sent back by calling
     * IAtFwd#sendAtCommandProcessedState(int, AtCmdResponse).
     *
     * @param serial is the serial number of the command
     * @param command is AT command to be processed
     */
    oneway void onAtCommandForwarded(in int serial, in AtCmd command);

}
