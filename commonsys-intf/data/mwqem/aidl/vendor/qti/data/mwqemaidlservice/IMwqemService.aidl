/*===========================================================================
 Copyright (c) 2023 Qualcomm Technologies, Inc.
 All Rights Reserved.
 Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.data.mwqemaidlservice;

import vendor.qti.data.mwqemaidlservice.Preference;
import vendor.qti.data.mwqemaidlservice.ReturnCode;

/**
 * This is the root of the HAL module and is the interface returned when
 * loading an implementation of the IMwqemService HAL.
 */
@VintfStability
interface IMwqemService {
    /**
     * Disables MWQEM for the list of UIDs.
     *
     * @param uids List containing uids for disabling MWQEM
     * @return operation status
     */
    ReturnCode disableMwqem(in int[] uids);

    /**
     * Disables MWQEM for all the UIDs.
     *
     * @param uids None
     * @return operation status
     */
    ReturnCode disableMwqemforAllUids();

    /**
     * Enables MWQEM for the list of UIDs.
     *
     * @param uids List containing uids for enabling MWQEM
     * @param pref Preference which mentions whether the application
     *             requires throughput or latency as preference
     * @return operation status
     */
    ReturnCode enableMwqem(in int[] uids, in Preference pref);
}
