/* ==============================================================================
 * IWifiDisplaySessionVideoTrackCb.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.VideoInfo;

@VintfStability
interface IWifiDisplaySessionVideoTrackCb {
    // Adding return type to method instead of out param int status since there is only one return value.
    int resetFrameSkip(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int setFreeBuffer(in long clientdata, in long bufferId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int trackInfoUpdated(in long clientdata, in VideoInfo info);

    // Adding return type to method instead of out param int status since there is only one return value.
    int trackPause(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int trackResume(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int trackStart(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int trackStop(in long clientdata);
}
