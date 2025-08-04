/* ==============================================================================
 * IWifiDisplaySessionAudioTrackCb.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.MediaInfo;

@VintfStability
interface IWifiDisplaySessionAudioTrackCb {
    // Adding return type to method instead of out param int status since there is only one return value.
    int audioProxyClosed(in long clientdata);

    // Adding return type to method instead of out param int retval since there is only one return value.
    int isProxyAvailable(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int pause(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int resume(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int start(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int stop(in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int trackInfoUpdated(in long clientdata, in MediaInfo info);
}
