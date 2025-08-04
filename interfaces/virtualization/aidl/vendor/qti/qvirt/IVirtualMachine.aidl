/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qvirt;

import vendor.qti.qvirt.IVirtualMachineCallback;
import vendor.qti.qvirt.VirtualMachineState;
import vendor.qti.qvirt.VirtualMachineClientTask;
@VintfStability
interface IVirtualMachine {

    /** Error code indicating VM start operation failed. */
    const int ERROR_VM_START = -1;

    /** Returns the current VM state. */
    VirtualMachineState getState();

    /** Registers a binder object to the interface to receive callbacks when the VM state changes */
    void registerCallback(IVirtualMachineCallback callback);

    /** Starts running the VM. */
    void start();

    /** Perform Task on the VM
    The client can use this to inform the virtualization service that the VM is currently being used.
    The option to vote (in-use) and unvote (Not-in-use) is provided to the client */
    void performtask_client(in VirtualMachineClientTask task);

    /** Unregister the VM */
    void unregister();

}