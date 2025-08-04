/* ==============================================================================
 * Uibc_rotate_event_parms.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

/*
 * -----------------------------------------------------------------------
 * Type definitions for Rotate event Parms
 * -----------------------------------------------------------------------
 */
@VintfStability
parcelable Uibc_rotate_event_parms {
    byte num_rotate_int;		//The signed integer portion of the amount units
								//in radians to rotate.
								//A negative number indicates to rotate clockwise
								//a positive number indicates to rotate counter-clockwise
    byte num_rotate_fraction;	//The fraction portion of the amount in units of
								//radians to rotate.
}
