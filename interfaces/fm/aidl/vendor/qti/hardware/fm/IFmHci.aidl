/* ============================================================================
*   Copyright (c) 2023 Qualcomm Technologies, Inc.
*   All Rights Reserved.
*   Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ============================================================================
*/

package vendor.qti.hardware.fm;

import vendor.qti.hardware.fm.IFmHciCallbacks;

/**
 * The Host Controller Interface (HCI) is the layer defined by the Fm
 * specification between the software that runs on the host and the Fm
 * controller chip. This boundary is the natural choice for a Hardware
 * Abstraction Layer (HAL). Dealing only in HCI packets and events simplifies
 * the stack and abstracts away power management, initialization, and other
 * implementation-specific details related to the hardware.
 */
@VintfStability
interface IFmHci {
    /**
     * Close the HCI interface
     */
    void close();

    /**
     * Initialize the underlying HCI interface.
     *
     * This method should be used to initialize any hardware interfaces
     * required to communicate with the Fm hardware in the
     * device.
     *
     * The |oninitializationComplete| callback must be invoked in response
     * to this function to indicate success before any other function
     * (sendHciCommand) is invoked on this interface.
     *
     * @param callback implements IFmHciCallbacks which will
     * receive callbacks when incoming HCI packets are received
     * from the controller to be sent to the host.
     */
    void initialize(in IFmHciCallbacks callback);

    /**
     * Send an HCI command the Fm controller.
     * Commands must be executed in order.
     *
     * @param command is the HCI command to be sent
     */
    void sendHciCommand(in byte[] command);
}
