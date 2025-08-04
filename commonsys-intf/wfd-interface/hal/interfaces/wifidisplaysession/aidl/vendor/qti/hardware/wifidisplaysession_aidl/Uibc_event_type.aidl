/* ==============================================================================
 * Uibc_event_type.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

/*
 * -----------------------------------------------------------------------
 * Type definitions for uibc event
 * -----------------------------------------------------------------------
 *
 *
 * This enumerated type lists the different types of uibc events
 */
@VintfStability
@Backing(type="int")
enum Uibc_event_type {
    WFD_UIBC_TOUCH,
    WFD_UIBC_KEY,
    WFD_UIBC_ZOOM,
    WFD_UIBC_SCROLL,
    WFD_UIBC_ROTATE,
    WFD_UIBC_HID_KEY,
}
