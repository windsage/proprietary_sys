/* ==============================================================================
 * WfdEnums.java
 *
 * Data structure for WFD capable device
 *
 * Copyright (c) 2012 - 2020, 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ============================================================================== */

package com.qualcomm.wfd;

public class WfdEnums {
    //Should match with DeviceType enum in Device.h
    public static enum WFDDeviceType {
        SOURCE(0), PRIMARY_SINK(1), SECONDARY_SINK(2), SOURCE_PRIMARY_SINK(3), UNKNOWN(4);

        private final int code;

        private WFDDeviceType(int c) {
            code = c;
        }

        public int getCode() {
            return code;
        }

        public static WFDDeviceType getValue(int c) {
            for (WFDDeviceType e:values()) {
                if (e.code == c)
                    return e;
            }
            return null;
        }

    }

    // Order should match with AVPlaybackModeType enum in WFDDefs.h
    public static enum AVPlaybackMode {
        NO_AUDIO_VIDEO,
        AUDIO_ONLY,
        VIDEO_ONLY,
        AUDIO_VIDEO
    }

    public static enum SessionState {
        INVALID,
        INITIALIZED, /* Session is initalized and ready to connect */
        IDLE, /* Ready to PLAY */
        PLAY,
        PAUSE,
        ESTABLISHED,
        TEARDOWN,
        PLAYING,
        PAUSING,
        STANDBY,
        STANDING_BY,
        TEARING_DOWN
    }

    public static enum ErrorType {
        UNKNOWN(-1),
        INVALID_ARG(-2),
        HDMI_CABLE_CONNECTED(-3),
        OPERATION_TIMED_OUT(-4),
        ALREADY_INITIALIZED(-10), /*Session specific errors*/
        NOT_INITIALIZED(-11),
        SESSION_IN_PROGRESS(-12),
        INCORRECT_STATE_FOR_OPERATION(-13),
        NOT_SINK_DEVICE(-14),
        NOT_SOURCE_DEVICE(-15),
        UIBC_NOT_ENABLED(-20) /*UIBC related errors*/,
        UIBC_ALREADY_ENABLED(-21);

        private final int code;

        private ErrorType(int c) {
            code = c;
        }

        public int getCode() {
            return code;
        }

        public static ErrorType getValue(int c) {
            for (ErrorType e:values()) {
                if (e.code == c)
                    return e;
            }
            return null;
        }
    }

    public static enum NetType {
        UNKNOWN_NET, WIFI_P2P, WIGIG_P2P, LAN;
    }

}
