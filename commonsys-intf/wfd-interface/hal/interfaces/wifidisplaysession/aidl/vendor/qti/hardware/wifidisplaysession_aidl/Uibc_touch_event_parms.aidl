/* ==============================================================================
 * Uibc_touch_event_parms.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_touch_event_type;

@VintfStability
parcelable Uibc_touch_event_parms {
    Uibc_touch_event_type type;		//Type of touch event
    byte num_pointers;				//Number of active touch points on the screen
    byte[] pointer_id;			    //Id of the pointer
    double[] coordinate_x;			//X-Coordinate with respect to the 
									//negotiated display resolution
    double[] coordinate_y;			//Y-Coordinate with respect to the negotiated 
									//display resolution
}
