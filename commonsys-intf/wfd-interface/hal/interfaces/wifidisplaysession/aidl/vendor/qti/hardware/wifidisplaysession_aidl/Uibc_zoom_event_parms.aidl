/* ==============================================================================
 * Uibc_zoom_event_parms.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

/*
 * -----------------------------------------------------------------------
 * Type definitions for Zoom event parms
 * -----------------------------------------------------------------------
 */
@VintfStability
parcelable Uibc_zoom_event_parms {
    double coordinate_x;		//Reference X-Coordinate for zoom with respect
								//to the negotiated display resolution
    double coordinate_y;		//Reference Y-Coordinate for zoom with respect
								
    byte num_times_zoom_int;	//to the negotiated display resolution
								//to zoom
    byte num_times_zoom_fraction;	//Fraction portion of the number of times to zoom
}
