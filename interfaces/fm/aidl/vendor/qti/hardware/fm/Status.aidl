/* ============================================================================
*   Copyright (c) 2023 Qualcomm Technologies, Inc.
*   All Rights Reserved.
*   Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ============================================================================
*/

package vendor.qti.hardware.fm;

@VintfStability
@Backing(type="int")
enum Status {
    SUCCESS,
    TRANSPORT_ERROR,
    INITIALIZATION_ERROR,
    UNKNOWN,
}
