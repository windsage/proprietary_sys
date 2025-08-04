/********************************************************************
---------------------------------------------------------------------
 Copyright (c) 2017 Qualcomm Technologies, Inc.
 All Rights Reserved.
 Confidential and Proprietary - Qualcomm Technologies, Inc.
----------------------------------------------------------------------
 SMCInvoke testing macros header
*********************************************************************/

#pragma once

#include <utils/RefBase.h>
#include <utils/Log.h>

#include <string>

#define LOGD_PRINT(fmt, ...) \
  do { \
    ALOGD("[%s:%u] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__);\
    printf("[%s:%u] " fmt "\n", __FUNCTION__, __LINE__, ##__VA_ARGS__);  \
  } while(0)

#define LOGE_PRINT(fmt, ...) \
  do { \
    ALOGE("[%s:%u] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__);\
    printf("[%s:%u] " fmt "\n", __FUNCTION__, __LINE__, ##__VA_ARGS__);  \
  } while(0)

#define PRINT LOGD_PRINT

/* Macros for testing */
#define TEST_OK(xx)                                                     \
  do                                                                    \
  {                                                                     \
    if ((xx))                                                           \
    {                                                                   \
      LOGE_PRINT("Failed!");              \
      exit(-1);                                                         \
    }                                                                   \
    LOGD_PRINT("Passed");                 \
  } while(0)

#define SILENT_OK(xx)                                                     \
  do                                                                    \
  {                                                                     \
    if ((xx))                                                           \
    {                                                                   \
      LOGE_PRINT("Failed!");              \
      exit(-1);                                                         \
    }                                                                   \
  } while(0)

#define TEST_FAIL(xx)   TEST_OK(!(xx))
#define TEST_FALSE(xx)  TEST_OK(xx)
#define TEST_TRUE(xx)   TEST_FAIL(xx)

#define SILENT_FAIL(xx)   SILENT_OK(!(xx))
#define SILENT_FALSE(xx)  SILENT_OK(xx)
#define SILENT_TRUE(xx)   SILENT_FAIL(xx)

