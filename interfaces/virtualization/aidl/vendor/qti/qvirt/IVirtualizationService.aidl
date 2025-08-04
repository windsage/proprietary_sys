/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qvirt;

import vendor.qti.qvirt.IVirtualMachine;
@VintfStability
interface IVirtualizationService{

    /** Returns a handle to the VirtualMachine with name vmName. */
    IVirtualMachine getVm(in @utf8InCpp String vmName);
}