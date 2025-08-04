/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qvirt;

@VintfStability
enum VirtualMachineClientTask {

    /** Client is using the VM. Hence they are voting */
    VOTE = 0,

    /** Client is not using the VM. Hence they are unvoting */
    UNVOTE = 1,

}
