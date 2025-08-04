/**
  * Copyright (c) 2024 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
  */


#include <android/log.h>
#define LOG_TAG "smcinvoke_system_native_client"
#define LOG(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#include <stdio.h>
#include <unistd.h>
#define ANDROID_LOG_DEBUG 1
#define ANDROID_LOG_INFO 2
#define ANDROID_LOG_WARN 3
#define ANDROID_LOG_ERROR 4
#define LOGE LOG
#define LOGD LOG

#define FATAL(...) { LOGE(__VA_ARGS__); exit(-1); }
#define LOGF() LOG("%s:%d\n", __FUNCTION__, __LINE__)
#define CHECK(cond, ...) { if (!(cond)) FATAL(__VA_ARGS__); }
#define ALOGE LOGE
#define ALOG LOG
#define ALOGD LOG
#define ALOGV LOG


static inline int atomic_add(int* pn, int n) {
  return __sync_add_and_fetch(pn, n);  // GCC builtin
}


