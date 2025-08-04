/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.ims.vt;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.codeaurora.telephony.utils.SomeArgs;
import org.codeaurora.ims.QtiCallConstants;
import org.codeaurora.ims.QtiVideoCallDataUsage;
import org.codeaurora.ims.ImsConferenceController;
import org.codeaurora.ims.ImsConferenceController.ConferenceState;

/**
 * Provides an interface to handle the media part of the video telephony call
 */
public class ImsMedia extends Handler
        implements ImsConferenceController.Listener {
    private static final String TAG = "VideoCall_ImsMedia";

    // DPL library's error codes.
    public static final int DPL_INIT_SUCCESSFUL = 0;
    public static final int DPL_INIT_FAILURE = -1;
    public static final int DPL_INIT_MULTIPLE = -2;

    public static final int PLAYER_STATE_STARTED = 0;
    public static final int PLAYER_STATE_STOPPED = 1;

    private static final int LOOPBACK_MODE_HEIGHT = 144;
    private static final int LOOPBACK_MODE_WIDTH = 176;
    private static final int LOOPBACK_MODE_FPS = 20;

    //Screen Share Status
    private static final int SCREEN_SHARE_DISABLED = 0;
    private static final int SCREEN_SHARE_ENABLED = 1;
    private static final int SCREEN_SHARE_PENDING = 2;

    private static boolean mInitCalledFlag = false;

    //AR Call Status
    private static final int AR_DISABLED = 0;
    private static final int AR_ENABLED = 1;
    private static final int AR_PENDING = 2;
    public static final int AR_REQUEST_SUCCESS = 0;

    // Native functions.
    private static native int nativeInit();
    private static native void nativeDeInit();
    private static native void nativeHandleRawFrame(byte[] frame);
    private static native int nativeSetSurface(Surface st);
    private static native void nativeSetDeviceOrientation(int orientation);
    private static native void nativeSetCameraFacing(int facing);
    private static native void nativeSetCameraInfo(int facing, int mount);
    private static native int nativeGetNegotiatedFPS();
    private static native int nativeGetNegotiatedHeight();
    private static native int nativeGetNegotiatedWidth();
    private static native int nativeGetUIOrientationMode();
    private static native int nativeGetPeerHeight();
    private static native int nativeGetPeerWidth();
    private static native int nativeGetVideoQualityIndication();
    private static native int nativeRequestRtpDataUsage(int mediaId);
    private static native void nativeRegisterForMediaEvents(ImsMedia instance);
    private static native Surface nativeGetRecordingSurface();
    private static native int nativeGetRecorderFrameRate();
    private static native int nativeSetSharedDisplayParameters(int width, int height);
    private static native int nativeSetLocalRenderingParameters(int width, int height);
    private static native int nativesetLocalRenderingDelay(int delay);

    private static native boolean nativeHaveSameParent(Surface a, Surface b);
    private static native int nativeSetVideoImageBuffer(int[] pixels, int width,
            int height);
    private static native int nativeGetSurfaceWidth(Surface surface);
    private static native int nativeGetSurfaceHeight(Surface surface);
    private static native int nativeGetDownscaledHeight();
    private static native int nativeGetDownscaledWidth();

    public static final int MEDIA_EVENT = 0;

    // Following values are from the IMS VT API documentation
    public static final int CAMERA_PARAM_READY_EVT = 1;
    public static final int START_READY_EVT = 2;
    public static final int PLAYER_START_EVENT = 3;
    public static final int PLAYER_STOP_EVENT = 4;
    public static final int DISPLAY_MODE_EVT = 5;
    public static final int PEER_RESOLUTION_CHANGE_EVT = 6;
    public static final int VIDEO_QUALITY_EVT = 7;
    public static final int DATA_USAGE_EVT = 8;
    public static final int STOP_READY_EVT = 9;
    public static final int CAMERA_FRAME_RATE_CHANGE_EVT = 10;
    public static final int DEVICE_READY_EVENT = 11;
    public static final int DOWN_SCALE_EVENT = 12;
    public static final int CACHED_MEDIA_EVENT = 100;

    /*
     * Initializing default negotiated parameters to a working set of values so that the application
     * does not crash in case we do not get the Param ready event
     */
    private int mNegotiatedHeight = ImsMediaConstants.DEFAULT_WIDTH;
    private int mNegotiatedWidth = ImsMediaConstants.DEFAULT_HEIGHT;
    private int mUIOrientationMode = ImsMediaConstants.PORTRAIT_MODE;
    private int mNegotiatedFps = ImsMediaConstants.DEFAULT_FPS;
    private int mCalculatedSharedDisplayWidth = ImsMediaConstants.DEFAULT_WIDTH;
    private int mCalculatedSharedDisplayHeight = ImsMediaConstants.DEFAULT_HEIGHT;
    private int mDownscaledWidth = ImsMediaConstants.INVALID_WIDTH;
    private int mDownscaledHeight = ImsMediaConstants.INVALID_HEIGHT;
    private int mPixelsWidth = ImsMediaConstants.DEFAULT_WIDTH;
    private int mPixelsHeight = ImsMediaConstants.DEFAULT_HEIGHT;

    private int mPeerHeight = ImsMediaConstants.DEFAULT_HEIGHT;
    private int mPeerWidth = ImsMediaConstants.DEFAULT_WIDTH;
    private int mVideoQualityLevel = ImsMediaConstants.VIDEO_QUALITY_UNKNOWN;

    // This variable holds TRUE when media event caching is required else holds FALSE
    private boolean mShouldCacheMediaEvents = false;
    private List<Pair<Integer, Integer>> mCachedMediaEvents = new CopyOnWriteArrayList<>();

    private boolean mIsMediaLoopback = false;
    private int mScreenShareStatus = SCREEN_SHARE_DISABLED;
    private boolean mIsParamReady = false;
    private Surface mSurface = null;
    private Surface mRecordingSurface = null;
    private int mArStatus = AR_DISABLED;

    private IMediaListener mMediaListener;
    private final List<MediaStateListener> mMediaStateListeners
            = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<CameraListener> mCameraListener
            = new CopyOnWriteArrayList<>();
    // Use a singleton
    private static ImsMedia mInstance;

    /**
     * This method returns the single instance of ImsMedia object *
     */
    public static synchronized ImsMedia getInstance() {
        if (mInstance == null) {
            mInstance = new ImsMedia();
        }
        return mInstance;
    }

    /**
     * Private constructor for ImsMedia
     */
    private ImsMedia() {
        this(Looper.getMainLooper());
    }

    private ImsMedia(Looper looper) {
        super(looper);
        initializemIsMediaLoopback();
    }

    /**
     * Adds a new media state listener
     */
    public void addMediaStateListener(MediaStateListener listener) {
        if (listener != null) {
            mMediaStateListeners.add(listener);
        }
    }

    /**
     * Removes an existing media state listener
     */
    public void removeMediaStateListener(MediaStateListener listener) {
        mMediaStateListeners.remove(listener);
    }

    // TODO create CameraParams class and pass it as an argument to onParamReady
    public interface IMediaListener {

        void onPeerResolutionChanged(int mediaId, int width, int height);

        void onPlayerStateChanged(int mediaId, int state);

        void onVideoQualityEvent(int mediaId, int videoQuality);

        void onDataUsageChanged(int mediaId, QtiVideoCallDataUsage dataUsage);

        void onOrientationModeChanged(int mediaId, int orientationMode);

        void onRecordingSurfaceChanged(int mediaId, Surface recordingSurface, int width,
                int height);
    };

    public interface CameraListener {
        void onCameraConfigChanged(int mediaId, int width, int height, int fps, Surface surface,
                int orientationMode);
        void onUpdateRecorderFrameRate(int mediaId, int rate);
        void onRecordingEnabled(int mediaId);
        void onRecordingDisabled(int mediaId);
    };

    public interface MediaStateListener {
        void onMediaInitialized();
        void onMediaDeinitialized();
    };

    /*
     * public abstract CameraRelatedEventListener implements ICameraRelatedEventListener { void
     * onParamReadyEvent() {} void onStartReadyEvent(){} void onStopReadyEvent(){} };
     */
    static {
        System.loadLibrary("imsmedia_jni");
    }

    /*
     * Initialize Media
     * @return DPL_INIT_SUCCESSFUL 0 initialization is successful. DPL_INIT_FAILURE -1 error in
     * initialization of QMI or other components. DPL_INIT_MULTIPLE -2 trying to initialize an
     * already initialized library.
     */
    /* package */int init() {
        if (!mInitCalledFlag) {
            int status = nativeInit();
            log("init called error = " + status);
            switch (status) {
                case DPL_INIT_SUCCESSFUL:
                    mInitCalledFlag = true;
                    registerForMediaEvents(this);
                    break;
                case DPL_INIT_FAILURE:
                    mInitCalledFlag = false;
                    break;
                case DPL_INIT_MULTIPLE:
                    mInitCalledFlag = true;
                    loge("Dpl init is called multiple times");
                    status = DPL_INIT_SUCCESSFUL;
                    break;
            }
            if (status == DPL_INIT_SUCCESSFUL) {
                notifyOnMediaInitialized();
            }
            return status;
        }

        // Dpl is already initialized. So return success
        return DPL_INIT_SUCCESSFUL;
    }

    /*
     * Deinitialize Media
     */
    /* package */void deInit() {
        log("deInit called");
        mSurface = null;
        notifyOnMediaDeinitialized();
        nativeDeInit();
        mInitCalledFlag = false;
        mScreenShareStatus  = SCREEN_SHARE_DISABLED;
        mIsParamReady = false;
        mArStatus  = AR_DISABLED;
        mDownscaledWidth = ImsMediaConstants.INVALID_WIDTH;
        mDownscaledHeight = ImsMediaConstants.INVALID_HEIGHT;
        clearMediaEventCache();
    }

    private void notifyOnMediaDeinitialized() {
        for (MediaStateListener listener : mMediaStateListeners) {
            try {
                listener.onMediaDeinitialized();
            } catch (Exception e) {
                loge("notifyOnMediaDeinitialized: Error=" + e);
            }
        }
    }

    private void notifyOnMediaInitialized() {
        for (MediaStateListener listener : mMediaStateListeners) {
            try {
                listener.onMediaInitialized();
            } catch (Exception e) {
                loge("notifyOnMediaInitialized: Error=" + e);
            }
        }
    }

    private void initializemIsMediaLoopback() {
        // Check the Media loopback property
        int property = SystemProperties.getInt("net.lte.VT_LOOPBACK_ENABLE", 0);
        mIsMediaLoopback = (property == 1) ? true : false;
    }

    public void sendCvoInfo(int orientation) {
        log("sendCvoInfo orientation=" + orientation);
        nativeSetDeviceOrientation(orientation);
    }

    private void doOnCachedMediaEvent() {
       log("doOnCachedMediaEvent: scheduling the cache");
       if (!mInitCalledFlag) {
          logw("VT lib deinitialized. Do not process cached events");
          clearMediaEventCache();
          return;
       }

       synchronized(this) {
           for (Pair event : mCachedMediaEvents) {
               int eventId = (int) event.first;
               int mediaId = (int) event.second;
               log("doOnCachedMediaEvent: scheduling eventId : " + eventId
                       + " mediaId : " + mediaId);
               doOnMediaEvent(eventId, mediaId);
           }
           clearMediaEventCache();
       }
    }

    private void clearMediaEventCache() {
        mShouldCacheMediaEvents = false;
        mCachedMediaEvents.clear();
    }

    @Override
    public void onConferenceStateChanged(ConferenceState confState, final boolean isSuccess) {
        synchronized(this) {
            log("onConferenceStateChanged ConferenceState: " + confState + " isSuccess: "
                    + isSuccess);

            switch (confState) {
                case PROGRESS:
                    mShouldCacheMediaEvents = true;
                    return;
                case COMPLETED:
                    processConferenceStateCompleted(isSuccess);
                    return;
                default:
                    return;
            }
        }
    }

    @Override
    public void onConferenceParticipantStateChanged(final boolean isMultiParty) {
        if (!isMultiParty) {
            return;
        }

        synchronized(this) {
            scheduleCacheMediaEvents();
        }
    }

    @Override
    public void onAbortConferenceCompleted(final boolean shouldAllowPendingRequest) {
        log("onAbortConferenceCompleted : no-op");
    }

    private void processConferenceStateCompleted(final boolean isSuccess) {
        if (!isSuccess) {
            //Conference failure
            clearMediaEventCache();
            return;
        }

        //Conference success
        scheduleCacheMediaEvents();
    }

    private void scheduleCacheMediaEvents() {
        // In case of conference participant side, caching need to start now
        mShouldCacheMediaEvents = true;
        // Controls the delay after which cached media events are to be processed
        final String PROPERTY_SCHEDULE_MEDIA_CACHE = "persist.vendor.radio.schd.cache";
        final int MEDIA_CACHE_DELAY = 2300;
        /* In order to ensure that event caching continues until the cache is scheduled,
           defer updating the cacheStatus on merge complete */
        final Message msg = obtainMessage(CACHED_MEDIA_EVENT);
        int delay = SystemProperties.getInt(PROPERTY_SCHEDULE_MEDIA_CACHE, MEDIA_CACHE_DELAY);
        log("scheduling the cache with delay =" + delay);
        sendMessageDelayed(msg, delay);
    }

    /**
     * Config and set screen share status.
     * This function will be invokded while starting screen share
     * or PARAM_READY_EVT/DOWN_SCALE_EVENT changed during screen share.
     */
    private void configScreenShare() {
        calculateSharedDisplayParams(mPixelsWidth, mPixelsHeight,
                getSupportedWidth(), getSupportedHeight());
        mScreenShareStatus = nativeSetSharedDisplayParameters(
            mCalculatedSharedDisplayWidth, mCalculatedSharedDisplayHeight) == 0 ?
                SCREEN_SHARE_ENABLED : SCREEN_SHARE_DISABLED;
    }

    /**
     * Request recording surface from VTLib with calculated
     * width and height for share screen mode.
     */
    public void setSharedDisplayParams(int width, int height) {
        log("setSharedDisplayParams width=" + width +
            " height=" + height);
        mPixelsWidth = width;
        mPixelsHeight = height;
        mScreenShareStatus = SCREEN_SHARE_PENDING;
        if (mIsParamReady) {
            configScreenShare();
        }
    }

    /**
     * Request to VTLib to stop screen share.
     * passing nativeSetSharedDisplayParameters(-1, -1)
     * is to inform VTLib that we intend to stop screen share.
     */
    public void stopScreenShare() {
        mScreenShareStatus = nativeSetSharedDisplayParameters(-1, -1) == 0 ?
            SCREEN_SHARE_DISABLED : SCREEN_SHARE_ENABLED;
        mPixelsWidth = ImsMediaConstants.DEFAULT_WIDTH;
        mPixelsHeight = ImsMediaConstants.DEFAULT_HEIGHT;
        log("Screen Share status after stopScreenShare attempt = " + mScreenShareStatus);
    }

    private int getSupportedWidth() {
        return  mDownscaledWidth !=
          ImsMediaConstants.INVALID_WIDTH ? mDownscaledWidth : mNegotiatedWidth;
    }

    private int getSupportedHeight() {
        return mDownscaledHeight !=
          ImsMediaConstants.INVALID_HEIGHT ? mDownscaledHeight : mNegotiatedHeight;
    }

    /**
     * As recorder is capable to scale upto 8:1 ratio hence
     * calculate appropriate width and height for shared display.
     */
    private void calculateSharedDisplayParams(int pixelsWidth, int pixelsHeight,
            int supportedWidth, int supportedHeight) {
        int scalingFactor = (int) Math.max(Math.ceil(((double) pixelsWidth)/supportedWidth),
                                           Math.ceil(((double) pixelsHeight)/supportedHeight));
        if (scalingFactor > QtiCallConstants.RECORDER_SCALING_FACTOR) {
            scalingFactor = QtiCallConstants.RECORDER_SCALING_FACTOR;
        }
        mCalculatedSharedDisplayWidth = supportedWidth * scalingFactor;
        mCalculatedSharedDisplayHeight = supportedHeight * scalingFactor;
        mayBeTrimSharedDisplaySize(scalingFactor, pixelsWidth < pixelsHeight);
        log("calculateSharedDisplayParams mCalculatedSharedDisplayWidth=" +
                mCalculatedSharedDisplayWidth + " mCalculatedSharedDisplayHeight=" +
                mCalculatedSharedDisplayHeight);
    }

    /**
     * Trim shared display width and height based on decoder and encoder supported width and height
     * to make sure we are setting the width and height in expected limits.
     *
     */
    private void mayBeTrimSharedDisplaySize(int scalingFactor, boolean isPortrait) {
        // Get media codec.
        MediaCodec decoder;
        MediaCodec encoder;
        String videoType = MediaFormat.MIMETYPE_VIDEO_AVC;
        try {
            decoder = MediaCodec.createDecoderByType(videoType);
            encoder = MediaCodec.createEncoderByType(videoType);
        } catch (Exception e) {
           loge("mayBeTrimSharedDisplaySize: Error=" + e);
           return;
        }

        // Get video capabilities of decoder and encoder.
        MediaCodecInfo.VideoCapabilities decoderVc = decoder.getCodecInfo().
                getCapabilitiesForType(videoType).getVideoCapabilities();
        MediaCodecInfo.VideoCapabilities encoderVc = encoder.getCodecInfo().
                getCapabilitiesForType(videoType).getVideoCapabilities();
        decoder.release();
        encoder.release();

        // Get maximum supported width and height of decoder and encoder.
        // If decoder and encoder both supporting 1920x1080 or 1080x1920 then will get decoder
        // height/width and encoder height/width all same which is 1920 because this is the upper
        // limit can support across width and height.
        int decoderWidth = decoderVc.getSupportedWidths().getUpper();
        int decoderHeight = decoderVc.getSupportedHeights().getUpper();
        int encoderWidth = encoderVc.getSupportedWidths().getUpper();
        int encoderHeight = encoderVc.getSupportedHeights().getUpper();

        // Get the upper limit of the smaller resolution supported across width or height.
        // If maximum resolution supported is 1920x1080 or 1080x1920 then will get 1080 as smaller
        // dimention upper limit.
        int encoderSmallerDimension = encoderVc.getSmallerDimensionUpperLimit();
        int decoderSmallerDimension = decoderVc.getSmallerDimensionUpperLimit();

        // Get smaller dimension across encoder and decoder which can be used.
        int smallerCodecDimension = (decoderSmallerDimension > encoderSmallerDimension)
                ? encoderSmallerDimension : decoderSmallerDimension;
        log("mayBeTrimSharedDisplaySize: decoderWidth = " + decoderWidth + " decoderHeight = " +
                decoderHeight + " encoderWidth = " + encoderWidth + " encoderHeight = " +
                encoderHeight + " smallerCodecDimension = " + smallerCodecDimension +
                " scalingFactor = " + scalingFactor + " isPortrait = " + isPortrait);
        // Get smaller width/height across encoder and decoder which can be used.
        int smallerCodecWidth = (decoderWidth > encoderWidth) ? encoderWidth : decoderWidth;
        int smallerCodecHeight = (decoderHeight > encoderHeight) ? encoderHeight : decoderHeight;

        int codecWidth;
        int codecHeight;
        // Define the limits based on portrait, landscape and calculated smaller upper limit,
        // smaller upper limit of width and height.
        if (isPortrait) {
            codecWidth = smallerCodecDimension;
            codecHeight = smallerCodecHeight;
        } else {
            codecWidth = smallerCodecWidth;
            codecHeight = smallerCodecDimension;
        }

        // Make sure that shared display width and height based on scale index are with in the
        // expected range.
        for (int i = 1; i <  scalingFactor; i++) {
            if (mCalculatedSharedDisplayWidth > codecWidth
                    || mCalculatedSharedDisplayHeight > codecHeight) {
                mCalculatedSharedDisplayWidth = getSupportedWidth() * (scalingFactor - i);
                mCalculatedSharedDisplayHeight = getSupportedHeight() * (scalingFactor - i);
            } else {
                break;
            }
        }
    }

    /**
     * Setting the rendering parameters to VTlib
     */
    private void setLocalRenderingParams(boolean enableAr) {
        if (enableAr) {
            mArStatus = nativeSetLocalRenderingParameters(
                    mNegotiatedWidth, mNegotiatedHeight) == AR_REQUEST_SUCCESS ?
                AR_ENABLED : AR_DISABLED;
        } else {
            mArStatus = nativeSetLocalRenderingParameters(ImsMediaConstants.INVALID_WIDTH,
                    ImsMediaConstants.INVALID_HEIGHT) == AR_REQUEST_SUCCESS ?
                AR_DISABLED : AR_ENABLED;
            log("AR status after stopAR attempt = " + mArStatus);
        }
    }

    /**
     * Request to VTLib to start/stop AR Call.
     */
    public void enableArMode(boolean enableAr) {
        log("enableArMode enableAr = " + enableAr);
        if (enableAr) {
            mArStatus = AR_PENDING;
            if (mIsParamReady) {
                setLocalRenderingParams(true);
            }
        } else {
            setLocalRenderingParams(false);
        }
    }

    /**
     * Set the delay of local rendering to VTLib.
     * passing the delay inform VTLib for AV sync.
     */
    public void setLocalRenderingDelay(int delay) {
        log("setLocalRenderingDelay=" + delay);
        nativesetLocalRenderingDelay(delay);
    }

    /**
     * Indicate if AR is enabled
     */
    public boolean isArEnabled() {
        return mArStatus == AR_ENABLED;
    }

    /**
     * Send camera facing info to IMS
     */
    public void setCameraFacing(boolean isFacingFront) {
        final int facing = isFacingFront ? ImsMediaConstants.CAMERA_FACING_FRONT
                : ImsMediaConstants.CAMERA_FACING_BACK;
        log("setCameraFacing isFacingFront=" + isFacingFront);
        nativeSetCameraFacing(facing);
    }

    /**
     * Send camera facing and mount angle info to IMS.
     */
    public void setCameraInfo(int facing, int mount) {
        log("setCameraInfo facing=" + facing + " mount=" + mount);
        nativeSetCameraInfo(facing, mount);
    }

    /**
     * Send the Surface to media module.
     *
     * @param st
     */
    public void setSurface(Surface st) {
        log("setSurface mSurface=" + mSurface + " st=" + st);
        if (!mInitCalledFlag) {
            log("setSurface: init not completed. ignore!");
            return;
        }
        // If surface passed in is not the same as the cached surface,
        // we call nativeSetSurface(null) and then set the new surface.
        if (!haveSameParent(mSurface, st)) {
            if (mSurface != null && st != null) {
                nativeSetSurface(null);
            }
            mSurface = st;
            nativeSetSurface(mSurface);
        }
    }

    /**
     * Get Negotiated Height
     */
    public int getNegotiatedHeight() {
        log("Negotiated Height = " + mNegotiatedHeight);
        return mNegotiatedHeight;
    }

    /**
     * Get Negotiated Width
     */
    public int getNegotiatedWidth() {
        log("Negotiated Width = " + mNegotiatedWidth);
        return mNegotiatedWidth;
    }

    public int getNegotiatedFps() {
        log("Negotiated Fps = " + mNegotiatedFps);
        return mNegotiatedFps;
    }

    /**
     * Get recording surface
     */
    public Surface getRecordingSurface() {
        log("RecordingSurface= " + mRecordingSurface);
        return mRecordingSurface;
    }

    /**
     * Get Negotiated Width
     */
    public int getUIOrientationMode() {
        log("UI Orientation Mode = " + mUIOrientationMode);
        return mUIOrientationMode;
    }

    /**
     * Get Peer Height
     */
    public int getPeerHeight() {
        log("Peer Height = " + mPeerHeight);
        return mPeerHeight;
    }

    /**
     * Get Peer Width
     */
    public int getPeerWidth() {
        log("Peer Width = " + mPeerWidth);
        return mPeerWidth;
    }

    /**
     * Get Downscaled Width
     */
    public int getDownscaledWidth() {
        log("Downscaled Width = " + mDownscaledWidth);
        return mDownscaledWidth;
    }

    /**
     * Get Downscaled height
     */
    public int getDownscaledHeight() {
        log("Downscaled height = " + mDownscaledHeight);
        return mDownscaledHeight;
    }

    /**
     * Get Video Quality level
     */
    public int getVideoQualityLevel() {
        log("Video Quality Level = " + mVideoQualityLevel);
        return mVideoQualityLevel;
    }

    /**
     * Request Call Data Usage
     */
    public void requestCallDataUsage(int mediaId) {
        log("requestCallDataUsage");
        int status = nativeRequestRtpDataUsage(mediaId);
        log("requestCallDataUsage: status = " + status);
    }

    // Gets the bitmap colors from bitmap and sends to lower layers
    public boolean setPreviewImage(Bitmap bitmap) {
        if (!mInitCalledFlag) {
            log("setPreviewImage: VT lib deinitialized so ignore");
            return false;
        }

        log("setPreviewImage: bitmap = " + bitmap);

        int status;
        if (bitmap == null) {
            status = nativeSetVideoImageBuffer(null, -1, -1);
        } else {
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            log("setPreviewImage: bitmap width = " + width + " height = " + height);

            int [] argb = new int[width * height];
            // argb contains bitmap colors
            bitmap.getPixels(argb, 0, width, 0, 0, width, height);
            status = nativeSetVideoImageBuffer(argb, width, height);
        }
        /* status takes below possible values
           0 - Success
          -1 - Generic Failure
          -2 - Buffer size didnt match the required dimension
          -3 - Not matching the required dimensions */
        log("setPreviewImage: status = " + status);
        return (status == 0);
    }

    /**
     * Register for event that will invoke {@link ImsMedia#onMediaEvent(int)}
     */
    private void registerForMediaEvents(ImsMedia instance) {
        log("Registering for Media Callback Events");
        nativeRegisterForMediaEvents(instance);
    }

    public void setMediaListener(IMediaListener listener) {
        log("Registering for Media Listener");
        mMediaListener = listener;
    }

    public void addCameraListener(CameraListener listener) {
        if (listener != null) {
            mCameraListener.addIfAbsent(listener);
        }
    }

    public void removeCameraListener(CameraListener listener) {
            mCameraListener.remove(listener);
    }

    private void doOnMediaEvent(int eventId, int mediaId) {
        switch (eventId) {
            case CAMERA_PARAM_READY_EVT:
                if (updatePreviewParams() &&
                        (mScreenShareStatus == SCREEN_SHARE_DISABLED) &&
                        (mArStatus == AR_DISABLED)) {
                    log("Received PARAM_READY_EVT and not in Screen Share mode" +
                          "Or AR call, Updating negotiated values");
                    onCameraConfigChanged(mediaId);
                } else if (mScreenShareStatus == SCREEN_SHARE_PENDING ||
                        mScreenShareStatus == SCREEN_SHARE_ENABLED) {
                    configScreenShare();
                } else if (mArStatus == AR_PENDING) {
                    setLocalRenderingParams(true);
                }
                mIsParamReady = true;
                mDownscaledWidth = ImsMediaConstants.INVALID_WIDTH;
                mDownscaledHeight = ImsMediaConstants.INVALID_HEIGHT;
                break;
            case DEVICE_READY_EVENT:
                log("Received DEVICE_READY_EVENT.");
                if (mScreenShareStatus == SCREEN_SHARE_ENABLED) {
                    mRecordingSurface = nativeGetRecordingSurface();
                    log("mRecordingSurface = " + mRecordingSurface);
                    if (mMediaListener != null) {
                        mMediaListener.onRecordingSurfaceChanged(mediaId, mRecordingSurface,
                            mCalculatedSharedDisplayWidth, mCalculatedSharedDisplayHeight);
                    }
                } else if (mArStatus == AR_ENABLED) {
                    mRecordingSurface = nativeGetRecordingSurface();
                    log("mRecordingSurface = " + mRecordingSurface);
                    if (mMediaListener != null) {
                        mMediaListener.onRecordingSurfaceChanged(mediaId, mRecordingSurface,
                            mNegotiatedWidth, mNegotiatedHeight);
                    }
                } else if (mScreenShareStatus == SCREEN_SHARE_DISABLED ||
                        mArStatus == AR_DISABLED) {
                    //Inform UI that Screen Share / AR call was successfully disabled.
                    if (mMediaListener != null) {
                        mMediaListener.onRecordingSurfaceChanged(mediaId, null,
                                ImsMediaConstants.INVALID_WIDTH, ImsMediaConstants.INVALID_HEIGHT);
                    }
                    onCameraConfigChanged(mediaId);
                }
                break;
            // Ideally IMS media can handle downscaling gracefully and don't need to notify
            // ATEL to adjust the input and output resolution for encoder as both input and
            // output parameters are provided by IMS media, media can adjust output parameter
            // within resolution family (e.g. 720*1280, 360*640, 240*432) and not exceed the
            // VT call highest capability during downscaling. However for screen share, the
            // input parameter is calculated according to negotiated value and screen size
            // from upper layer. This result may exceed the VT call highest capability of
            // encoder (scale > 8:1), hence introduce DOWN_SCALE_EVENT and notify
            // ATEL to calculate input value properly for screen share (scale upto 8:1 ratio).
            // This event can be received during legacy VT call, ATEL just caches the downscaled
            // values and do nothing. The cached values can be used for screen share if upper
            // layer triggers it later during the same VT life.
            case DOWN_SCALE_EVENT:
                log("Received DOWN_SCALE_EVENT.");
                mDownscaledWidth = nativeGetDownscaledWidth();
                mDownscaledHeight = nativeGetDownscaledHeight();
                if (mScreenShareStatus == SCREEN_SHARE_ENABLED ||
                        mScreenShareStatus == SCREEN_SHARE_PENDING) {
                    configScreenShare();
                }
                break;
            case PEER_RESOLUTION_CHANGE_EVT:
                mPeerHeight = nativeGetPeerHeight();
                mPeerWidth = nativeGetPeerWidth();
                log("Received PEER_RESOLUTION_CHANGE_EVENT. Updating peer values"
                        + " mPeerHeight=" + mPeerHeight + " mPeerWidth=" + mPeerWidth);
                if (mMediaListener != null) {
                    mMediaListener.onPeerResolutionChanged(mediaId, mPeerWidth, mPeerHeight);
                }
                break;
            case START_READY_EVT:
                log("Received START_READY_EVT. Camera recording can be started");
                for (CameraListener listener : mCameraListener) {
                    listener.onRecordingEnabled(mediaId);
                }
                break;

            case STOP_READY_EVT:
                log("Received STOP_READY_EVT");
                for (CameraListener listener : mCameraListener) {
                    listener.onRecordingDisabled(mediaId);
                }
                break;
            case DISPLAY_MODE_EVT:
                log("Received DISPLAY_MODE_EVT");
                mUIOrientationMode = nativeGetUIOrientationMode();
                log("Received DISPLAY_MODE_EVT mUIOrientationMode=" + mUIOrientationMode);
                if (mMediaListener != null) {
                    mMediaListener.onOrientationModeChanged(mediaId, mUIOrientationMode);
                }
                break;
            case PLAYER_START_EVENT:
                log("Received PLAYER_START_EVT");
                if (mMediaListener != null) {
                    mMediaListener.onPlayerStateChanged(mediaId, PLAYER_STATE_STARTED);

                }
                break;
            case PLAYER_STOP_EVENT:
                log("Received PLAYER_STOP_EVT");
                if (mMediaListener != null) {
                    mMediaListener.onPlayerStateChanged(mediaId, PLAYER_STATE_STOPPED);
                }
                break;
            case VIDEO_QUALITY_EVT:
                mVideoQualityLevel = nativeGetVideoQualityIndication();
                log("Received VIDEO_QUALITY_EVT" + mVideoQualityLevel);
                if (mMediaListener != null) {
                    mMediaListener.onVideoQualityEvent(mediaId, mVideoQualityLevel);
                }
                break;
            case CAMERA_FRAME_RATE_CHANGE_EVT:
                final int rate = nativeGetRecorderFrameRate();
                log("Received CAMERA_FRAME_RATE_CHANGE_EVT, rate=" + rate);
                if (rate > 0) {
                    for (CameraListener listener : mCameraListener) {
                        listener.onUpdateRecorderFrameRate(mediaId, rate);
                    }
                }
                break;
            default:
                loge("Received unknown event id=" + eventId);
        }
    }

    private void onCameraConfigChanged(int mediaId) {
        log("Negotiated Camera values mNegotiatedWidth = " + mNegotiatedWidth
                + " mNegotiatedHeight = " + mNegotiatedHeight + " mRecordingSurface = "
                + mRecordingSurface + " mediaId = " + mediaId);
        for (CameraListener listener : mCameraListener) {
            listener.onCameraConfigChanged(mediaId, mNegotiatedWidth,
                    mNegotiatedHeight, mNegotiatedFps, mRecordingSurface,
                    mUIOrientationMode);
            if (mArStatus == AR_ENABLED) {
                listener.onUpdateRecorderFrameRate(mediaId, mNegotiatedFps);
            }
        }
        if (mArStatus == AR_ENABLED) {
            if (mMediaListener != null) {
                mMediaListener.onRecordingSurfaceChanged(mediaId, mRecordingSurface,
                        mNegotiatedWidth, mNegotiatedHeight);
            }
        }
    }

    /**
     * Callback method that is invoked when Media events occur
     */
    public void onMediaEvent(int eventId, int mediaId) {
        log("onMediaEvent eventId = " + eventId + " mShouldCacheMediaEvents: "
                + mShouldCacheMediaEvents + " mediaId = " + mediaId);
        if (!mInitCalledFlag) {
            logw("VT lib deinitialized. Do not cache events");
            return;
        }
        synchronized(this) {
            // Do not cache camera START/STOP events
            if (mShouldCacheMediaEvents &&
                    !(eventId == START_READY_EVT || eventId == STOP_READY_EVT)) {
                 // Merge started so cache it
                mCachedMediaEvents.add(new Pair(eventId, mediaId));
            } else {
                SomeArgs args = SomeArgs.obtain();
                args.argi1 = eventId;
                args.argi2 = mediaId;
                final Message msg = obtainMessage(MEDIA_EVENT, args);
                sendMessage(msg);
            }
        }
    }

    /**
     * Callback method that is invoked when Data Usage event occurs
     */
    private void onDataUsageEvent(int mediaId, long[] dataUsage) {
        // Index 0 of dataUsage array holds LTE data usage
        // Index 1 of dataUsage array holds WiFi data usage
        log("onDataUsageEvent mediaId = " + mediaId);
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = mediaId;
        args.arg2 = dataUsage;
        Message msg = obtainMessage(DATA_USAGE_EVT, args);
        sendMessage(msg);
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MEDIA_EVENT:
                SomeArgs args1 = (SomeArgs) msg.obj;
                try {
                    doOnMediaEvent(args1.argi1, args1.argi2);
                } finally {
                    args1.recycle();
                }
                break;
            case DATA_USAGE_EVT:
                SomeArgs args2 = (SomeArgs) msg.obj;
                try {
                    int mediaId = args2.argi1;
                    long[] dataUsage = (long[]) args2.arg2;
                    if (mMediaListener != null) {
                        mMediaListener.onDataUsageChanged(mediaId,
                                new QtiVideoCallDataUsage(dataUsage));
                    }
                } finally {
                    args2.recycle();
                }
                break;
            case CACHED_MEDIA_EVENT:
                doOnCachedMediaEvent();
                break;
            default:
                loge("Received unknown msg id = " + msg.what);
        }
    }

    public static Size getSurfaceSize(Surface surface) {
        if (surface == null) {
            logw("surface is null");
            return new Size(0, 0);
        }
        return new Size(nativeGetSurfaceWidth(surface), nativeGetSurfaceHeight(surface));
    }

    public static boolean haveSameParent(Surface a, Surface b) {
        if (a==null && b == null) {
            return true;
        } else if ( a==null || b==null) {
            return false;
        }
        return nativeHaveSameParent(a, b);
    }

    private synchronized boolean updatePreviewParams() {
        if (mIsMediaLoopback) {
            mNegotiatedHeight = LOOPBACK_MODE_HEIGHT;
            mNegotiatedWidth = LOOPBACK_MODE_WIDTH;
            mNegotiatedFps = LOOPBACK_MODE_FPS;
            return true;
        } else {
            int h = nativeGetNegotiatedHeight();
            int w = nativeGetNegotiatedWidth();
            int fps = nativeGetNegotiatedFPS();
            int mode = nativeGetUIOrientationMode();
            // TODO Check if IMS-VT library will return null for camera1 case.
            // TODO Check if it is OK to update all camera params if new surface is received.
            Surface surface = nativeGetRecordingSurface();
            if (mNegotiatedHeight != h
                    || mNegotiatedWidth != w
                    || mNegotiatedFps != fps
                    || !haveSameParent(mRecordingSurface, surface)
                    || mUIOrientationMode != mode) {
                mNegotiatedHeight = h;
                mNegotiatedWidth = w;
                mNegotiatedFps = fps;
                mRecordingSurface = surface;
                mUIOrientationMode = mode;
                return true;
            }
            return false;
        }
    }

    /* package-private */
    boolean isMediaInitialized() {
        return mInitCalledFlag;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }

    private static void logw(String msg) {
        Log.w(TAG, msg);
    }
}
