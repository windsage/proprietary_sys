/* ==============================================================================
 * Uibc_event_t.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_event_parms;
import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_event_type;

@VintfStability
parcelable Uibc_event_t {
    Uibc_event_type type;		//type of uibc event
    Uibc_event_parms parms;		//parameters of the event
    int timestamp;				//The last 16 bits of the WFD source marked
								//RTP timestamp of the frames that are being displayed
								//when user inputs are applied(from spec 1.22,line1266)
								//otherwise "UINT16_MAX"
}
