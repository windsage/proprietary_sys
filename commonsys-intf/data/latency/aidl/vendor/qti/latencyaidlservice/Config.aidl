/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

import vendor.qti.latencyaidlservice.Level;
import vendor.qti.latencyaidlservice.Radio;
import vendor.qti.latencyaidlservice.SlotId;

/**
 * Data structure passed to the setLevel() for setting latency config.
 * @field rat Radio type.
 * @field slotId SIM slot Id.
 * @field uplink latency level.
 * @field downlink latency level.
 * @field enableConnectionExtension extension of existing radio connection.
 */
@VintfStability
parcelable Config {
    Radio rat;
    SlotId slotId;
    Level uplink;
    Level downlink;
    boolean enableConnectionExtension;
}
