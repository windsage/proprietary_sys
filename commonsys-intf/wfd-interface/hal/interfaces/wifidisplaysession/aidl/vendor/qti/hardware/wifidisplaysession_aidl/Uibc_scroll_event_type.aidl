/* ==============================================================================
 * Uibc_scroll_event_type.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

/*
 * -----------------------------------------------------------------------
 * Type definitions for scroll event Parms
 * -----------------------------------------------------------------------
 */
@VintfStability
@Backing(type="int")
enum Uibc_scroll_event_type {
    WFD_UIBC_SCROLL_VERTICAL,
    WFD_UIBC_SCROLL_HORIZONTAL,
}
