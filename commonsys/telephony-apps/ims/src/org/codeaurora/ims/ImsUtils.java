/*
 * Copyright (c) 2021, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import java.util.ArrayList;

public class ImsUtils {

    /* Constants used for the RTT voice info which indicates whether there is speech or
     * silence from remote user */
    public static final int VOICE_INFO_SILENT = 0;
    public static final int VOICE_INFO_SPEECH = 1;
    public static final int VOICE_INFO_UNKNOWN = 2;

    //Constants for sip error codes.
    public static final int SIP_UNKNOWN = 0;
    public static final int SIP_FORBIDDEN = 403;
    public static final int SIP_REQUEST_TIMEOUT = 408;
    public static final int SIP_TEMPORARILY_UNAVAILABLE = 480;
    public static final int SIP_SERVER_INTERNAL_ERROR = 500;
    public static final int SIP_SERVER_NOT_IMPLEMENTED = 501;
    public static final int SIP_SERVER_BAD_GATEWAY = 502;
    public static final int SIP_SERVICE_UNAVAILABLE = 503;
    public static final int SIP_SERVER_VERSION_UNSUPPORTED = 505;
    public static final int SIP_SERVER_MESSAGE_TOOLARGE = 513;
    public static final int SIP_SERVER_PRECONDITION_FAILURE = 580;

    public static byte[] toByteArray(ArrayList<Byte> inList) {

        if (inList == null) {
            return null;
        }

        byte[] outArray = new byte[inList.size()];

        for(int i = 0; i < outArray.length; ++i) {
            outArray[i] = inList.get(i);
        }

        return outArray;
    }

    public static ArrayList<Byte> toByteArrayList(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        final ArrayList<Byte> list = new ArrayList<>();
        for (byte b : byteArray) {
            list.add(b);
        }
        return list;
    }
}
