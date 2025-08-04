/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qvirt;

@Backing(type="int")
@VintfStability
enum VirtualMachineState {

    /** The VM has not started yet. */
    NOT_STARTED = 0,

    /**The VM is running. This means the VM has been
        created / Spawned but do not know if VM userspace
        is available to Use. */
    RUNNING = 1,

    /** The VM has stopped after running.
        The VM has shutdown gracefully and has not crashed */
    STOPPED = 2,

    /** VM Userspace is ready to be connected. The VM booted
        succesfully and the VM userpsace was reached and a
        communication link between the VM and HAL was established */
    VM_USERSPACE_READY = 3,

    /** VM TASK (Shutdown / Restart) in progress */
    VM_TASK_PROGRESS = 4,

    /** VM has crashed */
    VM_CRASHED = 5,
}
