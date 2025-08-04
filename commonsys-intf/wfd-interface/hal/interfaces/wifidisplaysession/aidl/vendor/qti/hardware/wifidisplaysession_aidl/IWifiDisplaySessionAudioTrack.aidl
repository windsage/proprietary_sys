/* ==============================================================================
 * IWifiDisplaySessionAudioTrack.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.IWifiDisplaySessionAudioTrackCb;
import vendor.qti.hardware.wifidisplaysession_aidl.MediaInfo;

@VintfStability
interface IWifiDisplaySessionAudioTrack {
    // FIXME: AIDL does not allow long to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    // FIXME: AIDL does not allow long to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void createMediaTrack(in long trackObj, in long clientdata,
        in IWifiDisplaySessionAudioTrackCb cb, out long[] status, out long[] instanceId, in String trackValidation);

    void destroyMediaTrack(in long instanceId);

    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void getTrackInfo(in long instanceId, out int[] status, out MediaInfo info);

    void setProxyAvailability(in long instanceId, in boolean bAvail);

    void setProxyInvalid(in long instanceId);
}
