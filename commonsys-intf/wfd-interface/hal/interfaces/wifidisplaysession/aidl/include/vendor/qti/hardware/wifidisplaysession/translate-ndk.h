/* ==============================================================================
 * translate-ndk.h
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */

// FIXME: license file, or use the -l option to generate the files with the header.

// FIXME Remove this file if you don't need to translate types in this backend.

#pragma once

#include "aidl/vendor/qti/hardware/wifidisplaysession/AudioInfo.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/DeviceInfo.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/ImageInfo.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/MediaInfo.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/NetIFType.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/SampleInfo.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_event_parms.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_event_t.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_event_type.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_key_event_parms.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_key_event_type.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_rotate_event_parms.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_scroll_event_parms.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_scroll_event_type.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_touch_event_parms.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_touch_event_type.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/Uibc_zoom_event_parms.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/VideoColorFormatType.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/VideoInfo.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/WFDMediaTrackType.h"
#include "aidl/vendor/qti/hardware/wifidisplaysession/WFDRuntimeCommands.h"
#include "vendor/qti/hardware/wifidisplaysession/1.0/types.h"
#include <limits>

namespace android::h2a {

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::audioInfo& in, aidl::vendor::qti::hardware::wifidisplaysession::AudioInfo* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::videoInfo& in, aidl::vendor::qti::hardware::wifidisplaysession::VideoInfo* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::imageInfo& in, aidl::vendor::qti::hardware::wifidisplaysession::ImageInfo* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::mediaInfo& in, aidl::vendor::qti::hardware::wifidisplaysession::MediaInfo* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::mediaInfo::trackInfo& in, aidl::vendor::qti::hardware::wifidisplaysession::MediaInfo::TrackInfo* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::sampleInfo& in, aidl::vendor::qti::hardware::wifidisplaysession::SampleInfo* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::deviceInfo& in, aidl::vendor::qti::hardware::wifidisplaysession::DeviceInfo* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_touch_event_parms& in, aidl::vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_parms* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_key_event_parms& in, aidl::vendor::qti::hardware::wifidisplaysession::Uibc_key_event_parms* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_zoom_event_parms& in, aidl::vendor::qti::hardware::wifidisplaysession::Uibc_zoom_event_parms* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_scroll_event_parms& in, aidl::vendor::qti::hardware::wifidisplaysession::Uibc_scroll_event_parms* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_rotate_event_parms& in, aidl::vendor::qti::hardware::wifidisplaysession::Uibc_rotate_event_parms* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_parms& in, aidl::vendor::qti::hardware::wifidisplaysession::Uibc_event_parms* out);
__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_t& in, aidl::vendor::qti::hardware::wifidisplaysession::Uibc_event_t* out);

}  // namespace android::h2a
