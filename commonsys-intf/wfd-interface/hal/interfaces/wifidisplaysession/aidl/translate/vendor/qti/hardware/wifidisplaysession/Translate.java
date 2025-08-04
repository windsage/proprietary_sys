/* ==============================================================================
 * translate.java
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */

// FIXME: license file, or use the -l option to generate the files with the header.

// FIXME Remove this file if you don't need to translate types in this backend.

package vendor.qti.hardware.wifidisplaysession;

public class Translate {
static public vendor.qti.hardware.wifidisplaysession.AudioInfo h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.audioInfo in) {
    vendor.qti.hardware.wifidisplaysession.AudioInfo out = new vendor.qti.hardware.wifidisplaysession.AudioInfo();
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nSampleRate > 2147483647 || in.nSampleRate < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nSampleRate");
    }
    out.nSampleRate = in.nSampleRate;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nSamplesPerFrame > 2147483647 || in.nSamplesPerFrame < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nSamplesPerFrame");
    }
    out.nSamplesPerFrame = in.nSamplesPerFrame;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nBitsPerSample < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nBitsPerSample");
    }
    out.nBitsPerSample = (char) in.nBitsPerSample;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nChannels < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nChannels");
    }
    out.nChannels = (char) in.nChannels;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.VideoInfo h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.videoInfo in) {
    vendor.qti.hardware.wifidisplaysession.VideoInfo out = new vendor.qti.hardware.wifidisplaysession.VideoInfo();
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nHeight > 2147483647 || in.nHeight < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nHeight");
    }
    out.nHeight = in.nHeight;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nWidth > 2147483647 || in.nWidth < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nWidth");
    }
    out.nWidth = in.nWidth;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nFrameRate > 2147483647 || in.nFrameRate < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nFrameRate");
    }
    out.nFrameRate = in.nFrameRate;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nMinBuffersReq > 2147483647 || in.nMinBuffersReq < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nMinBuffersReq");
    }
    out.nMinBuffersReq = in.nMinBuffersReq;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nCanSkipFrames > 2147483647 || in.nCanSkipFrames < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nCanSkipFrames");
    }
    out.nCanSkipFrames = in.nCanSkipFrames;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nMaxFrameSkipIntervalMs > 2147483647 || in.nMaxFrameSkipIntervalMs < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nMaxFrameSkipIntervalMs");
    }
    out.nMaxFrameSkipIntervalMs = in.nMaxFrameSkipIntervalMs;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nIDRIntervalMs > 2147483647 || in.nIDRIntervalMs < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nIDRIntervalMs");
    }
    out.nIDRIntervalMs = in.nIDRIntervalMs;
    out.eColorFmt = in.eColorFmt;
    out.bEnableUBWC = in.bEnableUBWC;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.ImageInfo h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.imageInfo in) {
    vendor.qti.hardware.wifidisplaysession.ImageInfo out = new vendor.qti.hardware.wifidisplaysession.ImageInfo();
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nHeight > 2147483647 || in.nHeight < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nHeight");
    }
    out.nHeight = in.nHeight;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nWidth > 2147483647 || in.nWidth < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nWidth");
    }
    out.nWidth = in.nWidth;
    out.bSecure = in.bSecure;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nMaxOverlaySupport > 2147483647 || in.nMaxOverlaySupport < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nMaxOverlaySupport");
    }
    out.nMaxOverlaySupport = in.nMaxOverlaySupport;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.MediaInfo h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.mediaInfo in) {
    vendor.qti.hardware.wifidisplaysession.MediaInfo out = new vendor.qti.hardware.wifidisplaysession.MediaInfo();
    out.eTrackType = in.eTrackType;
    out.sInfo = h2aTranslate(in.sInfo);
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.MediaInfo.TrackInfo h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.mediaInfo.trackInfo in) {
    vendor.qti.hardware.wifidisplaysession.MediaInfo.TrackInfo out = new vendor.qti.hardware.wifidisplaysession.MediaInfo.TrackInfo();
    out.sAudioInfo = h2aTranslate(in.sAudioInfo);
    out.sVideoInfo = h2aTranslate(in.sVideoInfo);
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.SampleInfo h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.sampleInfo in) {
    vendor.qti.hardware.wifidisplaysession.SampleInfo out = new vendor.qti.hardware.wifidisplaysession.SampleInfo();
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nTimeStamp > 9223372036854775807L || in.nTimeStamp < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nTimeStamp");
    }
    out.nTimeStamp = in.nTimeStamp;
    out.nSampleId = in.nSampleId;
    #error FIXME Unhandled type: handle
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nHeight > 2147483647 || in.nHeight < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nHeight");
    }
    out.nHeight = in.nHeight;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nWidth > 2147483647 || in.nWidth < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nWidth");
    }
    out.nWidth = in.nWidth;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nStride > 2147483647 || in.nStride < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nStride");
    }
    out.nStride = in.nStride;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nFrameNo > 9223372036854775807L || in.nFrameNo < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nFrameNo");
    }
    out.nFrameNo = in.nFrameNo;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.nArrivalTime > 9223372036854775807L || in.nArrivalTime < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.nArrivalTime");
    }
    out.nArrivalTime = in.nArrivalTime;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.DeviceInfo h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.deviceInfo in) {
    vendor.qti.hardware.wifidisplaysession.DeviceInfo out = new vendor.qti.hardware.wifidisplaysession.DeviceInfo();
    out.macAddr = in.macAddr;
    out.ipAddr = in.ipAddr;
    out.netType = in.netType;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.wfdDeviceInfoBitmap < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.wfdDeviceInfoBitmap");
    }
    out.wfdDeviceInfoBitmap = (char) in.wfdDeviceInfoBitmap;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.sessionMngtControlPort < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.sessionMngtControlPort");
    }
    out.sessionMngtControlPort = (char) in.sessionMngtControlPort;
    out.decoderLatency = in.decoderLatency;
    out.extSupport = in.extSupport;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.maxThroughput < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.maxThroughput");
    }
    out.maxThroughput = (char) in.maxThroughput;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.coupleSinkStatusBitmap > 127 || in.coupleSinkStatusBitmap < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.coupleSinkStatusBitmap");
    }
    out.coupleSinkStatusBitmap = in.coupleSinkStatusBitmap;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.Uibc_touch_event_parms h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.uibc_touch_event_parms in) {
    vendor.qti.hardware.wifidisplaysession.Uibc_touch_event_parms out = new vendor.qti.hardware.wifidisplaysession.Uibc_touch_event_parms();
    out.type = in.type;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_pointers > 127 || in.num_pointers < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.num_pointers");
    }
    out.num_pointers = in.num_pointers;
    if (in.pointer_id != null) {
        out.pointer_id = new byte[in.pointer_id.size()];
        for (int i = 0; i < in.pointer_id.size(); i++) {
            // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
            if (in.pointer_id.get(i) > 127 || in.pointer_id.get(i) < 0) {
                throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.pointer_id.get(i)");
            }
            out.pointer_id[i] = in.pointer_id.get(i);
        }
    }
    if (in.coordinate_x != null) {
        out.coordinate_x = new double[in.coordinate_x.size()];
        for (int i = 0; i < in.coordinate_x.size(); i++) {
            out.coordinate_x[i] = in.coordinate_x.get(i);
        }
    }
    if (in.coordinate_y != null) {
        out.coordinate_y = new double[in.coordinate_y.size()];
        for (int i = 0; i < in.coordinate_y.size(); i++) {
            out.coordinate_y[i] = in.coordinate_y.get(i);
        }
    }
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.Uibc_key_event_parms h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.uibc_key_event_parms in) {
    vendor.qti.hardware.wifidisplaysession.Uibc_key_event_parms out = new vendor.qti.hardware.wifidisplaysession.Uibc_key_event_parms();
    out.type = in.type;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.key_code_1 < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.key_code_1");
    }
    out.key_code_1 = (char) in.key_code_1;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.key_code_2 < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.key_code_2");
    }
    out.key_code_2 = (char) in.key_code_2;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.Uibc_zoom_event_parms h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.uibc_zoom_event_parms in) {
    vendor.qti.hardware.wifidisplaysession.Uibc_zoom_event_parms out = new vendor.qti.hardware.wifidisplaysession.Uibc_zoom_event_parms();
    out.coordinate_x = in.coordinate_x;
    out.coordinate_y = in.coordinate_y;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_times_zoom_int > 127 || in.num_times_zoom_int < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.num_times_zoom_int");
    }
    out.num_times_zoom_int = in.num_times_zoom_int;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_times_zoom_fraction > 127 || in.num_times_zoom_fraction < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.num_times_zoom_fraction");
    }
    out.num_times_zoom_fraction = in.num_times_zoom_fraction;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.Uibc_scroll_event_parms h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.uibc_scroll_event_parms in) {
    vendor.qti.hardware.wifidisplaysession.Uibc_scroll_event_parms out = new vendor.qti.hardware.wifidisplaysession.Uibc_scroll_event_parms();
    out.type = in.type;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_pixels_scrolled < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.num_pixels_scrolled");
    }
    out.num_pixels_scrolled = (char) in.num_pixels_scrolled;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.Uibc_rotate_event_parms h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.uibc_rotate_event_parms in) {
    vendor.qti.hardware.wifidisplaysession.Uibc_rotate_event_parms out = new vendor.qti.hardware.wifidisplaysession.Uibc_rotate_event_parms();
    out.num_rotate_int = in.num_rotate_int;
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.num_rotate_fraction > 127 || in.num_rotate_fraction < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.num_rotate_fraction");
    }
    out.num_rotate_fraction = in.num_rotate_fraction;
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.Uibc_event_parms h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.uibc_event_parms in) {
    vendor.qti.hardware.wifidisplaysession.Uibc_event_parms out = new vendor.qti.hardware.wifidisplaysession.Uibc_event_parms();
    out.touch_event = h2aTranslate(in.touch_event);
    out.key_event = h2aTranslate(in.key_event);
    out.zoom_event = h2aTranslate(in.zoom_event);
    out.scroll_event = h2aTranslate(in.scroll_event);
    out.rotate_event = h2aTranslate(in.rotate_event);
    return out;
}

static public vendor.qti.hardware.wifidisplaysession.Uibc_event_t h2aTranslate(vendor.qti.hardware.wifidisplaysession.V1_0.uibc_event_t in) {
    vendor.qti.hardware.wifidisplaysession.Uibc_event_t out = new vendor.qti.hardware.wifidisplaysession.Uibc_event_t();
    out.type = in.type;
    out.parms = h2aTranslate(in.parms);
    // FIXME This requires conversion between signed and unsigned. Change this if it doesn't suit your needs.
    if (in.timestamp < 0) {
        throw new RuntimeException("Unsafe conversion between signed and unsigned scalars for field: in.timestamp");
    }
    out.timestamp = (char) in.timestamp;
    return out;
}

}