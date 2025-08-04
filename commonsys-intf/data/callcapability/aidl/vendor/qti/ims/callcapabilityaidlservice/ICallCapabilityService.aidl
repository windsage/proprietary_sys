/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.callcapabilityaidlservice;

import vendor.qti.ims.callcapabilityaidlservice.CallCapStatusCode;
import vendor.qti.ims.callcapabilityaidlservice.ICallCapabilityListener;

@VintfStability
interface ICallCapabilityService {
    /**
     * Registers a listener to notify peer capabilities received as part of call.
     * Client must delete this instance when it no longer wants to listen.
     *
     * @param callCapabilityListener  instance of listener.
     *
     * @return OK on success else FAIL
     *
     */
    CallCapStatusCode registerForCallCapabilityUpdate(
        in ICallCapabilityListener callCapabilityListener);
}
