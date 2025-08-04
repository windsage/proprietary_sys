/* ==============================================================================
 * Uibc_touch_event_type.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

/*
 * UIBC
 */
@VintfStability
@Backing(type="int")
enum Uibc_touch_event_type {
    WFD_UIBC_TOUCH_DOWN,
    WFD_UIBC_TOUCH_UP,
    WFD_UIBC_TOUCH_MOVE,
}
