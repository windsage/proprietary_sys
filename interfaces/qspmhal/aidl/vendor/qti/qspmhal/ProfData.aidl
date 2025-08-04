/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qspmhal;

import vendor.qti.qspmhal.ParcelableMemory;

@VintfStability
parcelable ProfData {
    /*
     * @param mem HIDL shared buffer which has contents of the profile.
     * @param size Size of the HIDL shared buffer.
     * @param pkg_name Name of the requested profile.
     * @param pkg_ver Version of the requested profile.
     * @param timestamp Last modified time stamp.
     * @param Status Status of the operation.
     */
    ParcelableMemory mem;
    int size;
    String pkg_name;
    @nullable String pkg_ver;
    String timestamp;
}
