/* ==============================================================================
 * IWifiDisplaySessionCb.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.WFDMediaTrackType;
import vendor.qti.hardware.wifidisplaysession_aidl.Uibc_event_t;

@VintfStability
interface IWifiDisplaySessionCb {
    // Adding return type to method instead of out param int status since there is only one return value.
    int notify(in long clientdata, in String eventName, in String[] evtData);

    // Adding return type to method instead of out param int status since there is only one return value.
    int notifyMediaTrackCreated(in long clientdata, in long trackObj,
        in WFDMediaTrackType eTrackType);

    // Adding return type to method instead of out param int status since there is only one return value.
    int notifyMediaTrackDeleted(in long clientdata, in long trackObj);

    // Adding return type to method instead of out param int status since there is only one return value.
    int notifyUIBCGenericEvent(in Uibc_event_t ev, in long clientdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    int notifyUIBCHIDEvent(in byte[] hidpack, in byte len, in byte type);
}
