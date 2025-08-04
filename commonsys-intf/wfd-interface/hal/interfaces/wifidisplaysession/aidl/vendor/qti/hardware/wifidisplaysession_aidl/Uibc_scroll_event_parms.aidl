/* ==============================================================================
 * Uibc_scroll_event_parms.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_scroll_event_type;

@VintfStability
parcelable Uibc_scroll_event_parms {
    Uibc_scroll_event_type type;	//Type of scroll event
    int num_pixels_scrolled;		//Number of pixels scrolled with respect to
									//the negotiated display resolution
									//For vertical scroll, a negative number
									//indicates to scroll up; a positive number
									//indicates to scroll down
									//For horizontal scroll, a negative number
									//indicates to scroll right;a positive number
									//indicates to scroll left
}
