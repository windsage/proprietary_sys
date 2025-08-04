/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qvirt;

import vendor.qti.qvirt.VirtualMachineState;
@VintfStability
oneway interface IVirtualMachineCallback {

    /** Called when the state of the VM changes. */
    void onStatusChanged(in VirtualMachineState vmState);
}