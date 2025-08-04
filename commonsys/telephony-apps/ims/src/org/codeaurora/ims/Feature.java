/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

/*
 * This class contains feature IDs that clients can use to check if
 * a particular feature is supported or not
 */

public final class Feature {

    private Feature() {}

    public static final int SMS = 0;
    public static final int CONSOLIDATED_SET_SERVICE_STATUS = 1;
    public static final int EMERGENCY_DIAL = 2;
    public static final int CALL_COMPOSER_DIAL = 3;
    public static final int USSD = 4;
    public static final int CRS = 5;
    public static final int SIP_DTMF = 6;
    public static final int CONFERENCE_CALL_STATE_COMPLETED = 7;
    public static final int SET_MEDIA_CONFIG = 8;
    public static final int MULTI_SIM_VOICE_CAPABILITY = 9;
    public static final int EXIT_SCBM = 10;
    public static final int B2C_ENRICHED_CALLING = 11;
    public static final int DATA_CHANNEL = 12;
    public static final int VIDEO_ONLINE_SERVICE = 13;
    public static final int DSDS_TRANSITION = 14;
    public static final int INTERNAL_AIDL_REORDERING = 15;
    public static final int UVS_CRBT_CALL = 16;
    public static final int GLASSES_FREE_3D_VIDEO = 17;
    public static final int CONCURRENT_CONFERENCE_EMERGENCY_CALL = 18;
}
