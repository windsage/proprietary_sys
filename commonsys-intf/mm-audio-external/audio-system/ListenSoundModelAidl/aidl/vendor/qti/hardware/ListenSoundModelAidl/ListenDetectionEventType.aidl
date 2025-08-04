/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

import vendor.qti.hardware.ListenSoundModelAidl.ListenDetectionTypeEnum;
import vendor.qti.hardware.ListenSoundModelAidl.ListenDetectionEventV1;
import vendor.qti.hardware.ListenSoundModelAidl.ListenDetectionEventV2;

@VintfStability
parcelable ListenDetectionEventType {
    @VintfStability
    union Event {
        ListenDetectionEventV1 EventV1;
        ListenDetectionEventV2 EventV2;
    }
    ListenDetectionTypeEnum DetectionDataType;
    Event event;
}
