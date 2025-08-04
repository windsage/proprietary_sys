/* ==============================================================================
 * Uibc_key_event_parms.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_key_event_type;

@VintfStability
parcelable Uibc_key_event_parms {
    Uibc_key_event_type type;		//Type of key event
    int key_code_1;				//The key code of the first key event 
									//in the format specified in WFD spec
    int key_code_2;				//The key code of the second key event
									//in the format specified in WFD spec
}
