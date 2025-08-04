/* ==============================================================================
 * Uibc_event_parms.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_key_event_parms;
import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_rotate_event_parms;
import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_scroll_event_parms;
import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_touch_event_parms;
import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_zoom_event_parms;

@VintfStability
parcelable Uibc_event_parms {
    Uibc_touch_event_parms touch_event;
    Uibc_key_event_parms key_event;
    Uibc_zoom_event_parms zoom_event;
    Uibc_scroll_event_parms scroll_event;
    Uibc_rotate_event_parms rotate_event;
}
