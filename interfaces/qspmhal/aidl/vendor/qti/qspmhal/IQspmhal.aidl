/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qspmhal;

import vendor.qti.qspmhal.Command;
import vendor.qti.qspmhal.ParcelableMemory;
import vendor.qti.qspmhal.ProfData;

@VintfStability
interface IQspmhal {
    /**
     * Retrieves GPU profiles into a shared HIDL buffer.
     *
     * @param pid PID of the caller.
     */
    ProfData getGpuProf(in long pid);

    /**
     * Retrieves CPU profiles into a shared HIDL buffer.
     *
     * @param pkg_name Name of the requested profile.
     */
    ProfData getCpuProf(in String pkg_name);

    /**
     * Locally caches all the active PIDs and corresponding package name.
     *
     * @param pid PID of the caller.
     * @param pkg_name Name of the requested profile.
     * @param pkg_ver Version of the requested profile.
     */
    void setAppInfoH(in long pid, in String pkg_name, in String pkg_ver);

    /**
     * Receives the profile info in shared buffer and stores it.
     *
     * @param prof_buf HIDL shared buffer which has contents of the profile.
     * @param size Size of the shared HIDL buffer
     * @param prof_name Name of the profile.
     * @param cmd Command to be performed
     *
     * @param out Status Status of the operation.
     */
    void setAppProfile(in ParcelableMemory prof_buf, in long size, in String prof_name, in Command cmd);
}
