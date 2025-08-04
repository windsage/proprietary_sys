/* ============================================================================
*   Copyright (c) 2023 Qualcomm Technologies, Inc.
*   All Rights Reserved.
*   Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ============================================================================
*/

package vendor.qti.hardware.fm;

import vendor.qti.hardware.fm.Status;

/**
 * The interface from the Fm Controller to the stack.
 */
@VintfStability
interface IFmHciCallbacks {
    /**
     * This function is invoked when an HCI event is received from the
     * FM controller to be forwarded to the FM stack.
     * @param event is the HCI event to be sent to the FM stack.
     */
    void hciEventReceived(in byte[] event);

    /**
     * Invoked when the FM controller initialization has been
     * completed.
     */
    void initializationComplete(in Status status);
}
