/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*****************************************************************************/

package com.android.bluetooth.apm;

import android.util.Log;

public class ApmConst {
    public static final String TAG = "APM: ApmConst";
    private static boolean setActiveLeMedia = true;

    private static boolean qtiLeAudioEnabled = false;
    private static boolean aospLeaEnabled = false;
    public static final String groupAddress = "9E:8B:00:00:00";

    public static boolean getQtiLeAudioEnabled() {
        Log.i(TAG, " getQtiLeAudioEnabled: " + qtiLeAudioEnabled);
        return qtiLeAudioEnabled;
    }

    public static boolean getAospLeaEnabled() {
        Log.i(TAG, " getAospLeaEnabled: " + aospLeaEnabled);
        return aospLeaEnabled;
    }
    protected static void setQtiLeAudioEnabled(boolean qtiLeAudioSupport) {
        Log.i(TAG, " setQtiLeAudioEnabled: " + qtiLeAudioSupport);
        qtiLeAudioEnabled = qtiLeAudioSupport;
    }

    protected static void setAospLeaEnabled(boolean aospLeaSupport) {
        Log.i(TAG, " setAospLeaEnabled: " + aospLeaSupport);
        aospLeaEnabled = aospLeaSupport;
    }

    /*public static void setActiveLeMedia(boolean isSetactiveLeMedia) {
        setActiveLeMedia = isSetactiveLeMedia;
        Log.i(TAG, " setActiveLeMedia: " + setActiveLeMedia);
    }

    public static boolean getActiveLeMedia() {
        Log.i(TAG, " getActiveLeMedia: " + setActiveLeMedia);
        return setActiveLeMedia;
    }*/

    public static class AudioFeatures {

        public static final int CALL_AUDIO = 0;
        public static final int MEDIA_AUDIO = 1;
        public static final int CALL_CONTROL = 2;
        public static final int MEDIA_CONTROL = 3;
        public static final int MEDIA_VOLUME_CONTROL = 4;
        public static final int CALL_VOLUME_CONTROL = 5;
        public static final int BROADCAST_AUDIO = 6;
        public static final int HEARING_AID = 7;
        public static final int GAME_AUDIO = 8;
        public static final int STEREO_RECORDING = 9;
        public static final int GCP_VBC = 10;
        public static final int MAX_AUDIO_FEATURES = 11;

    }

    public static class AudioProfiles {

        public static final int NONE            = 0x0000;
        public static final int A2DP            = 0x0001;
        public static final int HFP             = 0x0002;
        public static final int AVRCP           = 0x0004;
        public static final int TMAP_MEDIA      = 0x0008;
        public static final int BAP_MEDIA       = 0x0010;
        public static final int MCP             = 0x0020;
        public static final int CCP             = 0x0040;
        public static final int VCP             = 0x0080;
        public static final int HAP_BREDR       = 0x0100;
        public static final int HAP_LE          = 0x0200;
        public static final int BROADCAST_BREDR = 0x0400;
        public static final int BROADCAST_LE    = 0x0800;
        public static final int TMAP_CALL       = 0x1000;
        public static final int BAP_CALL        = 0x2000;
        public static final int BAP_GCP         = 0x4000;
        public static final int BAP_RECORDING   = 0x8000;
        public static final int BAP_GCP_VBC   = 0x010000;

    }
}
