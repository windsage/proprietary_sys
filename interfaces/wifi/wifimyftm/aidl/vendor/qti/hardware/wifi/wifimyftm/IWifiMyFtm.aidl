/**
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifimyftm;

import vendor.qti.hardware.wifi.wifimyftm.MyFtmStatus;

/**
 * Interface exposed by myFtm AIDL service registered
 * with the service manager. This is the root level object for
 * any of the myFtm interactions.
 */
@VintfStability
interface IWifiMyFtm {
   /**
    * Sends the command to myftm
    *
    * @param arg The command to be sent to myftm
    * @return MyFtmStatus of the command sent, check MyFtmCmdStatus
    * for more information on MyFtmStatus.
    */
    MyFtmStatus myftmCmd(in String arg);
}
