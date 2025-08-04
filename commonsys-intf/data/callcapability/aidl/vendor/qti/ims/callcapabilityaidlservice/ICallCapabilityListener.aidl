/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.callcapabilityaidlservice;

import vendor.qti.ims.callcapabilityaidlservice.CallCapabilityInfo;

@VintfStability
interface ICallCapabilityListener {
    /**
     *  Callback to notify peer capabilities received as part of call.
     *
     *  @param cbdata Call Capability Info as a key-value
     *                pair vector.
     *
     */
    oneway void onPeerCapabilityUpdate(in CallCapabilityInfo[] cbdata);
}
