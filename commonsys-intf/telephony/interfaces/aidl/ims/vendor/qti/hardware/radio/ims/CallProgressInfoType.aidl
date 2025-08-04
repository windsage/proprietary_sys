/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum CallProgressInfoType {
    /*
     * Invalid if CallProgressInfo is not present.
     */
    INVALID,
    /*
     * MO call will be rejected with protocol Q850 error
     */
    CALL_REJ_Q850,
    /*
     * There is already an ACTIVE call at remote UE side and this call is a
     * WAITING call from remote UE perspective.
     */
    CALL_WAITING,
    /*
     * Call forwarding is enabled at remote UE and this call will be forwarded
     * from remote UE perspective.
     */
    CALL_FORWARDING,
    /*
     * MT call is alerting from remote UE perspective.
     */
    REMOTE_AVAILABLE,
}
