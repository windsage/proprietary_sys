/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

import vendor.qti.hardware.ListenSoundModelAidl.ListenEpdModule;
import vendor.qti.hardware.ListenSoundModelAidl.ListenModelType;
import vendor.qti.hardware.ListenSoundModelAidl.ListenRemoteHandleType;

@VintfStability
union ListenSmlModel {
    ListenEpdModule epdModule;
    ListenModelType modelType;
    ListenRemoteHandleType remoteHandle;
}
