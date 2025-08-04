/******************************************************************************
  @file    MemHalTest.cpp
  @brief   Memory Hal test binary which loads hal client to make call to hal

  ---------------------------------------------------------------------------
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
  ---------------------------------------------------------------------------
 ******************************************************************************/
#define LOG_TAG           "MemHalAPI-TESTER-SYSTEM"
#include <dlfcn.h>
#include <errno.h>
#include <stdio.h>
#include <time.h>
#include <string>
#include <cutils/trace.h>
#include <cutils/properties.h>
#include <log/log.h>
#include <vector>
#include <android/log.h>
#define QLOGE(...)    ALOGE(__VA_ARGS__)
#define QLOGW(...)    ALOGW(__VA_ARGS__)
#define QLOGI(...)    ALOGI(__VA_ARGS__)
#define QLOGV(...)    ALOGV(__VA_ARGS__)
#define QLOGD(...)    ALOGV(__VA_ARGS__)
#define QCLOGE(...)   ALOGE(__VA_ARGS__)

#define qcopt_lib_path "/system_ext/lib64/libqti-MemHal-client-system.so"

static const char* (*MemHal_GetProp_extn)(const std::string& in_propname, const std::string& in_defaultVal, const std::vector<int32_t>& in_req_details)=NULL ;
static int  (*MemHal_SetProp_extn)(const std::string& in_propname, const std::string& in_NewVal, const std::vector<int32_t>& in_req_details)=NULL ;
static int  (*MemHal_SubmitRequest_extn)(const std::vector<int32_t>& in_in_list, const std::vector<int32_t>& in_req_details)=NULL ;

static void *libhandle = NULL;

static void initialize(void) {
    const char *rc = NULL;

    QLOGE(LOG_TAG,"initialize called");
    dlerror();
    QLOGE(LOG_TAG,"lib name %s", qcopt_lib_path);
    libhandle = dlopen(qcopt_lib_path, RTLD_NOW);
    if (!libhandle) {
        QLOGE(LOG_TAG,"Unable to open %s: %s", qcopt_lib_path,dlerror());
        //printf("Failed to get qcopt handle from lib %s. Refer logcat for error info", qcopt_lib_path);
        return;
    }
    QLOGE(LOG_TAG,"Opening handle to functions defined in client");

    *(void **) (&MemHal_GetProp_extn) = dlsym(libhandle, "MemHal_GetProp_extn");
    if ((rc = dlerror()) != NULL) {
        QLOGE(LOG_TAG,"Unable to get MemHal_GetProp_extn function handle. %s", dlerror());
        //printf("Unable to get MemHal_GetProp_extn function handle. Refer logcat for more info");
        MemHal_GetProp_extn = NULL;
    }
    if(MemHal_GetProp_extn != NULL)
    {
        QLOGD(LOG_TAG"Succesfully Opened handle to function MemHal_GetProp_extn defined in client.");
    }

    *(void **) (&MemHal_SetProp_extn) = dlsym(libhandle, "MemHal_SetProp_extn");
    if ((rc = dlerror()) != NULL) {
        QLOGE(LOG_TAG,"Unable to get MemHal_SetProp_extn function handle. %s", dlerror());
        //printf("Unable to get MemHal_SetProp_extn function handle. Refer logcat for more info");
        MemHal_SetProp_extn = NULL;
    }
    if(MemHal_SetProp_extn != NULL)
    {
        QLOGD(LOG_TAG"Succesfully Opened handle to function MemHal_SetProp_extn defined in client.");
    }

    *(void **) (&MemHal_SubmitRequest_extn) = dlsym(libhandle, "MemHal_SubmitRequest_extn");
    if ((rc = dlerror()) != NULL) {
        QLOGE(LOG_TAG,"Unable to get MemHal_SubmitRequest_extn function handle. %s", dlerror());
        //printf("Unable to get MemHal_SubmitRequest_extn function handle. Refer logcat for more info");
        MemHal_SubmitRequest_extn = NULL;
    }
    if(MemHal_SubmitRequest_extn != NULL)
    {
        QLOGD(LOG_TAG"Succesfully Opened handle to function MemHal_SubmitRequest_extn defined in client.");
    }
    return;
}

int main(int argc, char *argv[]) {
    //printf("MemHalTest Started with %d args\n", argc);
    QLOGE(LOG_TAG,"main: MemHal MemHalTest Started with %d args.", argc);
    initialize();
    int rc=-1;
    QLOGE(LOG_TAG,"main: Calling API'S defined in MemHal client from MemHalTest");
    std::vector<int32_t> paramList;
    paramList.push_back(getpid());
    paramList.push_back(gettid());

    if(MemHal_GetProp_extn!=NULL){
        const char* ret=NULL;
        ret=MemHal_GetProp_extn("ro.lmk.cache_percent","10",paramList);
        if(ret!=NULL){
            QLOGE(LOG_TAG,"main: MemHal_GetProp_extn call success");
            //printf("main: MemHal_GetProp_extn call success\n");
        }

    }
    if(MemHal_SetProp_extn!=NULL){
        
        rc=MemHal_SetProp_extn("ro.lmk.cache_percent","10",paramList);
        if(rc!=-1){
            QLOGE(LOG_TAG,"main: MemHal_SetProp_extn call success");
            //printf("main: MemHal_SetProp_extn call success\n");
        }
    }
    rc=-1;
    if(MemHal_SubmitRequest_extn!=NULL){
        rc=MemHal_SubmitRequest_extn(paramList,paramList);
        if(rc!=-1){
            QLOGE(LOG_TAG,"main: MemHalAsyncRequest_extn call success");
            //printf("main: MemHalAsyncRequest_extn call success\n");
        }

    }    
    //printf("MemHalhal MemHalTest completed\n");
    return 0;
}