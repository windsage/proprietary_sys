/*
  * Copyright (c) 2024 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qvirtvendor;

@VintfStability
enum VendorVMState {

    /** The VM has not started yet. */
    VM_NOT_STARTED = 0,

    /**The VM has been created / Spawned. */
    VM_BOOT_READY = 1,

    /** VM has gracefully shutdown. */
    VM_SHUTDOWN = 2,

    /** The VM userpsace is running. The VM got
       created, booted up to userspace and the
       communication link with the userpsace was
       established. */
    VM_USERSPACE_READY = 3,

    /** VM PERFORM TASK IN PROGRESS */
    VM_TASK_INPROGRESS = 4,

    /** VM has crashed */
    VM_CRASHED = 5,
}
