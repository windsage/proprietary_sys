/* ==============================================================================
 * IWifiDisplaySessionVideoTrack.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.IWifiDisplaySessionVideoTrackCb;
import vendor.qti.hardware.wifidisplaysession_aidl.MediaInfo;
import vendor.qti.hardware.wifidisplaysession_aidl.SampleInfo;
import android.hardware.common.NativeHandle;

@VintfStability
interface IWifiDisplaySessionVideoTrack {
    // FIXME: AIDL does not allow long to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    // FIXME: AIDL does not allow long to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void createMediaTrack(in long trackObj, in long clientdata,
        in IWifiDisplaySessionVideoTrackCb cb, out long[] status, out long[] instanceId, in String trackValidation);

    void destroyMediaTrack(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int encode(in long instanceId, in android.hardware.common.NativeHandle input, in SampleInfo info);

    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void getTrackInfo(in long instanceId, out int[] status, out MediaInfo info);
}
