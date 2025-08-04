/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.util;

import android.os.Build;
import android.util.Log;

public class LogUtils {
    //logger is used for generic logging.
    private final static String DEFAULT_LOG_TAG = "QVAClient";
    private final static String LOG_TAG_PREFIX = "QVAClient-";
    private final static Logger LOGGER = new Logger(DEFAULT_LOG_TAG);

    public static void v(String message, Object... args) {
        LOGGER.v(message, args);
    }

    public static void v(String logTag, String message, Object... args) {
        LOGGER.setLogTag(LOG_TAG_PREFIX + logTag);
        LOGGER.v(message, args);
        LOGGER.setLogTag(DEFAULT_LOG_TAG);
    }

    public static void d(String message, Object... args) {
        LOGGER.d(message, args);
    }

    public static void d(String logTag, String message, Object... args) {
        LOGGER.setLogTag(LOG_TAG_PREFIX + logTag);
        LOGGER.d(message, args);
        LOGGER.setLogTag(DEFAULT_LOG_TAG);
    }

    public static void e(String message, Object... args) {
        LOGGER.e(message, args);
    }

    public static void e(String logTag, String message, Object... args) {
        LOGGER.setLogTag(LOG_TAG_PREFIX + logTag);
        LOGGER.e(message, args);
        LOGGER.setLogTag(DEFAULT_LOG_TAG);
    }

    private final static class Logger {
        //Log everything for debug builds or if running on a dev device.
        private final static boolean DEBUG = "eng".equals(Build.TYPE)
                || "userdebug".equals(Build.TYPE);

        private String mTag;

        private Logger(String logTag) {
            mTag = logTag;
        }

        private void setLogTag(String logTag) {
            mTag = logTag;
        }

        private boolean isVerboseLoggable() {
            return DEBUG || Log.isLoggable(mTag, Log.VERBOSE);
        }

        private boolean isDebugLoggable() {
            return DEBUG || Log.isLoggable(mTag, Log.DEBUG);
        }

        private boolean isErrorLoggable() {
            return DEBUG || Log.isLoggable(mTag, Log.ERROR);
        }

        private void v(String message, Object... args) {
            if (isVerboseLoggable()) {
                Log.v(mTag, args == null || args.length == 0
                        ? message : String.format(message, args));
            }
        }

        public void d(String message, Object... args) {
            if (isDebugLoggable()) {
                Log.d(mTag, args == null || args.length == 0 ? message
                        : String.format(message, args));
            }
        }

        public void e(String message, Object... args) {
            if (isErrorLoggable()) {
                Log.e(mTag, args == null || args.length == 0 ? message
                        : String.format(message, args));
            }
        }
    }
}
