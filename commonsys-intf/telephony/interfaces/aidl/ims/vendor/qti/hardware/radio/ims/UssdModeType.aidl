/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum UssdModeType {
    /*
     * Default value
     */
    INVALID,
    /*
     * USSD-Notify, no further user action required
     */
    NOTIFY,
    /*
     * USSD-Request, further user action required
     */
    REQUEST,
    /*
     * Session terminated by network
     */
    NW_RELEASE,
    /*
     * other local client (eg, SIM Toolkit) has responded
     */
    LOCAL_CLIENT,
    /*
     * Operation not supported
     */
    NOT_SUPPORTED,
    /*
     * Network timeout
     */
    NW_TIMEOUT,
}
