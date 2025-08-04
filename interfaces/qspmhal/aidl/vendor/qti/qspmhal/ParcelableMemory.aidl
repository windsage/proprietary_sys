/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qspmhal;

import android.hardware.common.MappableFile;

@VintfStability
parcelable ParcelableMemory {
    MappableFile file;
    String name;
}
