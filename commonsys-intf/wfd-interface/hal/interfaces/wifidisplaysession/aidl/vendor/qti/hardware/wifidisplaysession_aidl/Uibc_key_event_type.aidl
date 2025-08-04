/* ==============================================================================
 * Uibc_key_event_type.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

/*
 * -----------------------------------------------------------------------
 * Type definitions for key board event Parms
 * -----------------------------------------------------------------------
 */
@VintfStability
@Backing(type="int")
enum Uibc_key_event_type {
    WFD_UIBC_KEY_DOWN,
    WFD_UIBC_KEY_UP,
}
