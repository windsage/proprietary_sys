/*
    * Copyright (c) 2024 Qualcomm Technologies, Inc.
    * All Rights Reserved.
    * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qvirtvendor;

import vendor.qti.qvirtvendor.VendorVMState;
import vendor.qti.qvirtvendor.VMErrorCodes;
import vendor.qti.qvirtvendor.VMInfo;
import vendor.qti.qvirtvendor.VMTasks;
@VintfStability
interface IVendorVM {

    /** Returns the current VM state. */
    VendorVMState getState(in VMInfo vminfo);

    /** Communicates with the userspace of the VM and initiates a connection */
    VMErrorCodes connectvm(in VMInfo vminfo);

    /** Communicates with the userspace of the VM and disconnects the connection */
    VMErrorCodes disconnectvm(in VMInfo vminfo);

    /** Communicates with the userspace of the VM to send a request of a task
    Current support is only for Shutdown to be passed to the VM */
    VMErrorCodes performtaskvm(in VMInfo vminfo, in VMTasks task);
}
