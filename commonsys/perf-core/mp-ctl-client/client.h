/******************************************************************************
  @file    client.h
  @brief   Android performance PerfLock library

  DESCRIPTION

  ---------------------------------------------------------------------------

  Copyright (c) 2014,2017 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
  ---------------------------------------------------------------------------
******************************************************************************/
#ifdef __cplusplus
extern "C" {
#endif

#ifndef __PERFCLIENT_H__
#define __PERFCLIENT_H__
#include <cutils/properties.h>

#define QC_DEBUG_ERRORS 1
#ifdef QC_DEBUG_ERRORS
#define QLOGE(...)    ALOGE(__VA_ARGS__)
#else
#define QLOGE(...)
#endif

#if QC_DEBUG
#define QLOGW(...)    ALOGW(__VA_ARGS__)
#define QLOGI(...)    ALOGI(__VA_ARGS__)
#define QLOGV(...)    ALOGV(__VA_ARGS__)
#define QCLOGE(...)   ALOGE(__VA_ARGS__)
#else
#define QLOGW(...)
#define QLOGI(...)
#define QLOGV(...)
#define QCLOGE(...)
#endif
#define PROP_VAL_LENGTH PROP_VALUE_MAX
typedef struct {
    char value[PROP_VAL_LENGTH];
} PropVal;

int perf_lock_acq(int, int, int[], int);
int perf_hint(int, const char *, int, int);
int perf_get_feedback(int, const char *);
int perf_get_feedback_extn(int, const char *, int, int[]);
int perf_lock_rel(int);
int perf_hint_rel(int);
PropVal perf_get_prop(const char *, const char *);
const char* perf_sync_request(int);
int perf_lock_acq_rel(int, int, int[], int, int);
void perf_event(int, const char *, int, int[]);
int perf_hint_acq_rel(int, int, const char *, int, int, int, int[]);
int perf_hint_renew(int, int, const char *, int, int, int, int[]);
double get_perf_hal_ver();

//below are thread based offload API's
int perf_hint_offload(int , const char *, int , int , int, int[]);
int perf_lock_rel_offload(int);
int perf_hint_acq_rel_offload(int, int, const char *, int, int, int, int[]);
int perf_event_offload(int, const char *, int, int[]);

//deprecated API's from hal 2.3
int perf_lock_use_profile(int, int);
void perf_lock_cmd(int);
PropVal perf_wait_get_prop(const char * , const char *);
#endif

#ifdef __cplusplus
}
#endif
