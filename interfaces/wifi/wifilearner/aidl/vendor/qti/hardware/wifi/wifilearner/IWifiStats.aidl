/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifilearner;

import vendor.qti.hardware.wifi.wifilearner.ChannelSurveyInfo;
import vendor.qti.hardware.wifi.wifilearner.IfaceStats;
import vendor.qti.hardware.wifi.wifilearner.MacAddress;
import vendor.qti.hardware.wifi.wifilearner.WifiLearnerStatus;

@VintfStability
interface IWifiStats {
    /**
     * Function     : getPeerStats
     * Description  : gets peer stats on given interface.
     * Input params : interface name, peer mac address.
     * Out params   : IfaceStats
     * Return       : WifiLearnerStatus
     *
     */
    void getPeerStats(in String ifaceName, in MacAddress peerMac,
        out WifiLearnerStatus status, out IfaceStats stats);

    /**
     * Function     : getSurveyDumpResults
     * Description  : gets survey dump results.
     * Input params : interface name.
     * Out params   : ChannelSurveyInfo list
     * Return       : WifiLearnerStatus
     *
     */
    void getSurveyDumpResults(in String ifaceName,
        out WifiLearnerStatus status, out ChannelSurveyInfo[] surveyResults);
}
