/* ==============================================================================
 * translate-cpp.cpp
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */

// FIXME: license file, or use the -l option to generate the files with the header.

// FIXME Remove this file if you don't need to translate types in this backend.

#include "vendor/qti/hardware/wifidisplaysession/translate-cpp.h"

namespace android::h2a {

static_assert(vendor::qti::hardware::wifidisplaysession::WFDMediaTrackType::WFD_SESSION_AUDIO_TRACK == static_cast<vendor::qti::hardware::wifidisplaysession::WFDMediaTrackType>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDMediaTrackType::WFD_SESSION_AUDIO_TRACK));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDMediaTrackType::WFD_SESSION_VIDEO_TRACK == static_cast<vendor::qti::hardware::wifidisplaysession::WFDMediaTrackType>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDMediaTrackType::WFD_SESSION_VIDEO_TRACK));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDMediaTrackType::WFD_SESSION_IMAGE_TRACK == static_cast<vendor::qti::hardware::wifidisplaysession::WFDMediaTrackType>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDMediaTrackType::WFD_SESSION_IMAGE_TRACK));

static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_OPEN_AUDIO_PROXY == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_OPEN_AUDIO_PROXY));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_CLOSE_AUDIO_PROXY == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_CLOSE_AUDIO_PROXY));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_ENABLE_BITRATE_ADAPT == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_ENABLE_BITRATE_ADAPT));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_DISABLE_BITRATE_ADAPT == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_DISABLE_BITRATE_ADAPT));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_BLANK_REMOTE_DISPLAY == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_BLANK_REMOTE_DISPLAY));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_ENABLE_STREAMING_FEATURE == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_ENABLE_STREAMING_FEATURE));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_DISABLE_STREAMING_FEATURE == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_DISABLE_STREAMING_FEATURE));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_DISABLE_AUDIO == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_DISABLE_AUDIO));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_ENABLE_AUDIO == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_ENABLE_AUDIO));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_DISABLE_VIDEO == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_DISABLE_VIDEO));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_ENABLE_VIDEO == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_ENABLE_VIDEO));
static_assert(vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands::WFD_SESSION_CMD_INVALID == static_cast<vendor::qti::hardware::wifidisplaysession::WFDRuntimeCommands>(::vendor::qti::hardware::wifidisplaysession::V1_0::WFDRuntimeCommands::WFD_SESSION_CMD_INVALID));

static_assert(vendor::qti::hardware::wifidisplaysession::VideoColorFormatType::WFD_SESSION_VIDEO_FORMAT_YCbCr == static_cast<vendor::qti::hardware::wifidisplaysession::VideoColorFormatType>(::vendor::qti::hardware::wifidisplaysession::V1_0::videoColorFormatType::WFD_SESSION_VIDEO_FORMAT_YCbCr));
static_assert(vendor::qti::hardware::wifidisplaysession::VideoColorFormatType::WFD_SESSION_VIDEO_FORMAT_ARGB32 == static_cast<vendor::qti::hardware::wifidisplaysession::VideoColorFormatType>(::vendor::qti::hardware::wifidisplaysession::V1_0::videoColorFormatType::WFD_SESSION_VIDEO_FORMAT_ARGB32));

static_assert(vendor::qti::hardware::wifidisplaysession::NetIFType::UNKNOWN_NET == static_cast<vendor::qti::hardware::wifidisplaysession::NetIFType>(::vendor::qti::hardware::wifidisplaysession::V1_0::NetIFType::UNKNOWN_NET));
static_assert(vendor::qti::hardware::wifidisplaysession::NetIFType::WIFI_P2P == static_cast<vendor::qti::hardware::wifidisplaysession::NetIFType>(::vendor::qti::hardware::wifidisplaysession::V1_0::NetIFType::WIFI_P2P));
static_assert(vendor::qti::hardware::wifidisplaysession::NetIFType::WIGIG_P2P == static_cast<vendor::qti::hardware::wifidisplaysession::NetIFType>(::vendor::qti::hardware::wifidisplaysession::V1_0::NetIFType::WIGIG_P2P));
static_assert(vendor::qti::hardware::wifidisplaysession::NetIFType::LAN == static_cast<vendor::qti::hardware::wifidisplaysession::NetIFType>(::vendor::qti::hardware::wifidisplaysession::V1_0::NetIFType::LAN));

static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_type::WFD_UIBC_TOUCH_DOWN == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_touch_event_type::WFD_UIBC_TOUCH_DOWN));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_type::WFD_UIBC_TOUCH_UP == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_touch_event_type::WFD_UIBC_TOUCH_UP));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_type::WFD_UIBC_TOUCH_MOVE == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_touch_event_type::WFD_UIBC_TOUCH_MOVE));

static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_key_event_type::WFD_UIBC_KEY_DOWN == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_key_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_key_event_type::WFD_UIBC_KEY_DOWN));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_key_event_type::WFD_UIBC_KEY_UP == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_key_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_key_event_type::WFD_UIBC_KEY_UP));

static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_scroll_event_type::WFD_UIBC_SCROLL_VERTICAL == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_scroll_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_scroll_event_type::WFD_UIBC_SCROLL_VERTICAL));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_scroll_event_type::WFD_UIBC_SCROLL_HORIZONTAL == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_scroll_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_scroll_event_type::WFD_UIBC_SCROLL_HORIZONTAL));

static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_event_type::WFD_UIBC_TOUCH == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_type::WFD_UIBC_TOUCH));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_event_type::WFD_UIBC_KEY == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_type::WFD_UIBC_KEY));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_event_type::WFD_UIBC_ZOOM == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_type::WFD_UIBC_ZOOM));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_event_type::WFD_UIBC_SCROLL == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_type::WFD_UIBC_SCROLL));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_event_type::WFD_UIBC_ROTATE == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_type::WFD_UIBC_ROTATE));
static_assert(vendor::qti::hardware::wifidisplaysession::Uibc_event_type::WFD_UIBC_HID_KEY == static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_event_type>(::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_type::WFD_UIBC_HID_KEY));

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::audioInfo& in, vendor::qti::hardware::wifidisplaysession::AudioInfo* out) {
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nSampleRate > std::numeric_limits<int32_t>::max() || in.nSampleRate < 0) {
        return false;
    }
    out->nSampleRate = static_cast<int32_t>(in.nSampleRate);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nSamplesPerFrame > std::numeric_limits<int32_t>::max() || in.nSamplesPerFrame < 0) {
        return false;
    }
    out->nSamplesPerFrame = static_cast<int32_t>(in.nSamplesPerFrame);
    out->nBitsPerSample = static_cast<char16_t>(in.nBitsPerSample);
    out->nChannels = static_cast<char16_t>(in.nChannels);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::videoInfo& in, vendor::qti::hardware::wifidisplaysession::VideoInfo* out) {
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nHeight > std::numeric_limits<int32_t>::max() || in.nHeight < 0) {
        return false;
    }
    out->nHeight = static_cast<int32_t>(in.nHeight);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nWidth > std::numeric_limits<int32_t>::max() || in.nWidth < 0) {
        return false;
    }
    out->nWidth = static_cast<int32_t>(in.nWidth);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nFrameRate > std::numeric_limits<int32_t>::max() || in.nFrameRate < 0) {
        return false;
    }
    out->nFrameRate = static_cast<int32_t>(in.nFrameRate);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nMinBuffersReq > std::numeric_limits<int32_t>::max() || in.nMinBuffersReq < 0) {
        return false;
    }
    out->nMinBuffersReq = static_cast<int32_t>(in.nMinBuffersReq);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nCanSkipFrames > std::numeric_limits<int32_t>::max() || in.nCanSkipFrames < 0) {
        return false;
    }
    out->nCanSkipFrames = static_cast<int32_t>(in.nCanSkipFrames);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nMaxFrameSkipIntervalMs > std::numeric_limits<int32_t>::max() || in.nMaxFrameSkipIntervalMs < 0) {
        return false;
    }
    out->nMaxFrameSkipIntervalMs = static_cast<int32_t>(in.nMaxFrameSkipIntervalMs);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nIDRIntervalMs > std::numeric_limits<int32_t>::max() || in.nIDRIntervalMs < 0) {
        return false;
    }
    out->nIDRIntervalMs = static_cast<int32_t>(in.nIDRIntervalMs);
    out->eColorFmt = static_cast<vendor::qti::hardware::wifidisplaysession::VideoColorFormatType>(in.eColorFmt);
    out->bEnableUBWC = static_cast<bool>(in.bEnableUBWC);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::imageInfo& in, vendor::qti::hardware::wifidisplaysession::ImageInfo* out) {
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nHeight > std::numeric_limits<int32_t>::max() || in.nHeight < 0) {
        return false;
    }
    out->nHeight = static_cast<int32_t>(in.nHeight);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nWidth > std::numeric_limits<int32_t>::max() || in.nWidth < 0) {
        return false;
    }
    out->nWidth = static_cast<int32_t>(in.nWidth);
    out->bSecure = static_cast<bool>(in.bSecure);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nMaxOverlaySupport > std::numeric_limits<int32_t>::max() || in.nMaxOverlaySupport < 0) {
        return false;
    }
    out->nMaxOverlaySupport = static_cast<int32_t>(in.nMaxOverlaySupport);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::mediaInfo& in, vendor::qti::hardware::wifidisplaysession::MediaInfo* out) {
    out->eTrackType = static_cast<vendor::qti::hardware::wifidisplaysession::WFDMediaTrackType>(in.eTrackType);
    if (!translate(in.sInfo, &out->sInfo)) return false;
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::mediaInfo::trackInfo& in, vendor::qti::hardware::wifidisplaysession::MediaInfo::TrackInfo* out) {
    if (!translate(in.sAudioInfo, &out->sAudioInfo)) return false;
    if (!translate(in.sVideoInfo, &out->sVideoInfo)) return false;
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::sampleInfo& in, vendor::qti::hardware::wifidisplaysession::SampleInfo* out) {
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nTimeStamp > std::numeric_limits<int64_t>::max() || in.nTimeStamp < 0) {
        return false;
    }
    out->nTimeStamp = static_cast<int64_t>(in.nTimeStamp);
    out->nSampleId = static_cast<int64_t>(in.nSampleId);
    #error FIXME Unhandled type: handle
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nHeight > std::numeric_limits<int32_t>::max() || in.nHeight < 0) {
        return false;
    }
    out->nHeight = static_cast<int32_t>(in.nHeight);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nWidth > std::numeric_limits<int32_t>::max() || in.nWidth < 0) {
        return false;
    }
    out->nWidth = static_cast<int32_t>(in.nWidth);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nStride > std::numeric_limits<int32_t>::max() || in.nStride < 0) {
        return false;
    }
    out->nStride = static_cast<int32_t>(in.nStride);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nFrameNo > std::numeric_limits<int64_t>::max() || in.nFrameNo < 0) {
        return false;
    }
    out->nFrameNo = static_cast<int64_t>(in.nFrameNo);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nArrivalTime > std::numeric_limits<int64_t>::max() || in.nArrivalTime < 0) {
        return false;
    }
    out->nArrivalTime = static_cast<int64_t>(in.nArrivalTime);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::deviceInfo& in, vendor::qti::hardware::wifidisplaysession::DeviceInfo* out) {
    out->macAddr = String16(in.macAddr.c_str());
    out->ipAddr = String16(in.ipAddr.c_str());
    out->netType = static_cast<vendor::qti::hardware::wifidisplaysession::NetIFType>(in.netType);
    out->wfdDeviceInfoBitmap = static_cast<char16_t>(in.wfdDeviceInfoBitmap);
    out->sessionMngtControlPort = static_cast<char16_t>(in.sessionMngtControlPort);
    out->decoderLatency = static_cast<int32_t>(in.decoderLatency);
    out->extSupport = static_cast<int32_t>(in.extSupport);
    out->maxThroughput = static_cast<char16_t>(in.maxThroughput);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.coupleSinkStatusBitmap > std::numeric_limits<int8_t>::max() || in.coupleSinkStatusBitmap < 0) {
        return false;
    }
    out->coupleSinkStatusBitmap = static_cast<int8_t>(in.coupleSinkStatusBitmap);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_touch_event_parms& in, vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_parms* out) {
    out->type = static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_touch_event_type>(in.type);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_pointers > std::numeric_limits<int8_t>::max() || in.num_pointers < 0) {
        return false;
    }
    out->num_pointers = static_cast<int8_t>(in.num_pointers);
    {
        size_t size = in.pointer_id.size();
        for (size_t i = 0; i < size; i++) {
            // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
            if (in.pointer_id[i] > std::numeric_limits<int8_t>::max() || in.pointer_id[i] < 0) {
                return false;
            }
            out->pointer_id.push_back(static_cast<int8_t>(in.pointer_id[i]));
        }
    }
    {
        size_t size = in.coordinate_x.size();
        for (size_t i = 0; i < size; i++) {
            out->coordinate_x.push_back(static_cast<double>(in.coordinate_x[i]));
        }
    }
    {
        size_t size = in.coordinate_y.size();
        for (size_t i = 0; i < size; i++) {
            out->coordinate_y.push_back(static_cast<double>(in.coordinate_y[i]));
        }
    }
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_key_event_parms& in, vendor::qti::hardware::wifidisplaysession::Uibc_key_event_parms* out) {
    out->type = static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_key_event_type>(in.type);
    out->key_code_1 = static_cast<char16_t>(in.key_code_1);
    out->key_code_2 = static_cast<char16_t>(in.key_code_2);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_zoom_event_parms& in, vendor::qti::hardware::wifidisplaysession::Uibc_zoom_event_parms* out) {
    out->coordinate_x = static_cast<double>(in.coordinate_x);
    out->coordinate_y = static_cast<double>(in.coordinate_y);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_times_zoom_int > std::numeric_limits<int8_t>::max() || in.num_times_zoom_int < 0) {
        return false;
    }
    out->num_times_zoom_int = static_cast<int8_t>(in.num_times_zoom_int);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_times_zoom_fraction > std::numeric_limits<int8_t>::max() || in.num_times_zoom_fraction < 0) {
        return false;
    }
    out->num_times_zoom_fraction = static_cast<int8_t>(in.num_times_zoom_fraction);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_scroll_event_parms& in, vendor::qti::hardware::wifidisplaysession::Uibc_scroll_event_parms* out) {
    out->type = static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_scroll_event_type>(in.type);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_pixels_scrolled < 0) {
        return false;
    }
    out->num_pixels_scrolled = static_cast<char16_t>(in.num_pixels_scrolled);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_rotate_event_parms& in, vendor::qti::hardware::wifidisplaysession::Uibc_rotate_event_parms* out) {
    out->num_rotate_int = static_cast<int8_t>(in.num_rotate_int);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_rotate_fraction > std::numeric_limits<int8_t>::max() || in.num_rotate_fraction < 0) {
        return false;
    }
    out->num_rotate_fraction = static_cast<int8_t>(in.num_rotate_fraction);
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_parms& in, vendor::qti::hardware::wifidisplaysession::Uibc_event_parms* out) {
    if (!translate(in.touch_event, &out->touch_event)) return false;
    if (!translate(in.key_event, &out->key_event)) return false;
    if (!translate(in.zoom_event, &out->zoom_event)) return false;
    if (!translate(in.scroll_event, &out->scroll_event)) return false;
    if (!translate(in.rotate_event, &out->rotate_event)) return false;
    return true;
}

__attribute__((warn_unused_result)) bool translate(const ::vendor::qti::hardware::wifidisplaysession::V1_0::uibc_event_t& in, vendor::qti::hardware::wifidisplaysession::Uibc_event_t* out) {
    out->type = static_cast<vendor::qti::hardware::wifidisplaysession::Uibc_event_type>(in.type);
    if (!translate(in.parms, &out->parms)) return false;
    out->timestamp = static_cast<char16_t>(in.timestamp);
    return true;
}

}  // namespace android::h2a