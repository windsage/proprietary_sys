//
// Copyright (c) 2021 Qualcomm Technologies, Inc.
// All Rights Reserved.
// Confidential and Proprietary - Qualcomm Technologies, Inc.
//

package vendor.qti.hardware.qxr;

@VintfStability

interface IQXRCoreService {

    int setFd(in ParcelFileDescriptor fd, in int prot);

}

