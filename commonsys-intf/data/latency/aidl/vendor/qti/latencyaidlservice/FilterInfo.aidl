/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/


package vendor.qti.latencyaidlservice;

import vendor.qti.latencyaidlservice.FilterStatus;
import vendor.qti.latencyaidlservice.Level;
import vendor.qti.latencyaidlservice.OodStatus;
/**
 *
 * @field filterId Id associated with the given filter.
 * @field status Current FilterStatus for the filter.
 */
@VintfStability
parcelable FilterInfo {
    @VintfStability
    union UplinkLatencyLevel {
        boolean noinit;
        Level value;
    }
    @VintfStability
    union DownlinkLatencyLevel {
        boolean noinit;
        Level value;
    }
    @VintfStability
    union PDCPDiscarTimer {
        boolean noinit;
        int value;
    }
    @VintfStability
    union OOD {
        boolean noinit;
        OodStatus value;
    }
    int filterId;
    FilterStatus status;
    UplinkLatencyLevel uplink_level;
    DownlinkLatencyLevel downlink_level;
    PDCPDiscarTimer pdcp_timer;
    OOD ood;
}
