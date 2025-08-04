/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#define MAIN_C
#include "SystemEvent.h"
#include "SystemEventAIDL.h"
#include "SystemResource.h"
#include "SystemResourceAIDL.h"
#include <android-base/logging.h>
#include <common_log.h>
#include <errno.h>
#include <hidl/HidlTransportSupport.h>
#include <iostream>
#include <jni.h>
#include <mutex>
#include <queue>
#include <semaphore.h>
#include <string>
#include <unistd.h>
#include <android/binder_process.h>

using namespace android;
using android::hardware::configureRpcThreadpool;
using android::hardware::joinRpcThreadpool;
using android::sp;
using vendor::qti::hardware::systemhelper::V1_0::implementation::SystemEvent;
using vendor::qti::hardware::systemhelper::V1_0::implementation::SystemResource;
using SystemEventAIDL = ::aidl::vendor::qti::hardware::systemhelperaidl::SystemEvent;
using SystemResourceAIDL = ::aidl::vendor::qti::hardware::systemhelperaidl::SystemResource;
using namespace std;

std::mutex resourceLock;
static queue<string> msg_q;
static queue<sem_t **> request_q;
sem_t sem;
bool orientationPortrait = true;

extern "C" {

#include "com_qualcomm_qti_services_systemhelper_SysHelperService.h"

extern "C" jint
Java_com_qualcomm_qti_services_systemhelper_SysHelperService_init(
    JNIEnv *env, jclass /*cls*/) {

  jint ret = -1;
  configureRpcThreadpool(1, false /*willJoinThreadpool*/);

  SystemEvent *sysEvent = new SystemEvent();
  LOGD("SystemEvent interface registering.");
  auto status = sysEvent->registerAsService();
  if (status != android::OK) {
    LOGE("Could not register SystemEvent 1.0 interface");
  } else {
    SystemResource *sysResource = new SystemResource();
    LOGD("SystemResource interface registering.");
    auto status1 = sysResource->registerAsService();
    if (status1 != android::OK) {
      LOGE("Could not register SystemResource 1.0 interface");
    } else {
      ret = 0;
      LOGD("Successfully registered for both SystemEvent and SystemResource "
           "1.0 interfaces");
    }
  }
  sem_init(&sem, 0, 0);
  return ret;
}

extern "C" jint
Java_com_qualcomm_qti_services_systemhelper_SysHelperService_initAIDL(
    JNIEnv *env, jclass /*cls*/) {
    jint ret = -1;
    std::shared_ptr<SystemEventAIDL> eventService = ndk::SharedRefBase::make<SystemEventAIDL>();
    std::shared_ptr<SystemResourceAIDL> resourceService = ndk::SharedRefBase::make<SystemResourceAIDL>();
    binder_status_t status = STATUS_OK;

    LOGE("SystemEvent AIDL interface registering ...");
    const std::string instance_event = std::string() + SystemEventAIDL::descriptor + "/default";
    status = AServiceManager_addService(eventService->asBinder().get(), instance_event.c_str());
    if (status == STATUS_OK) {
        LOGE("SystemResource AIDL interface registering...");
        const std::string instance_resource = std::string() + SystemResourceAIDL::descriptor + "/default";
        status = AServiceManager_addService(resourceService->asBinder().get(), instance_resource.c_str());
        if (status == STATUS_OK) {
            ret = 0;
            LOGE("Successfully registered for both SystemEvent and SystemResource interfaces");
            ABinderProcess_startThreadPool();
        } else {
            LOGE("Could not register SystemResource interface");
        }
    } else {
        LOGE("Could not register SystemEvent interface status %d ", status);
    }

    sem_init(&sem, 0, 0);
    return ret;
}

extern "C" jstring
Java_com_qualcomm_qti_services_systemhelper_SysHelperService_getMessage(
    JNIEnv *env, jclass /*cls*/) {
  do {
    sem_wait(&sem);
  } while(msg_q.empty());

  string str = msg_q.front();
  msg_q.pop();

  LOGD("%s: System Helper service processing req :%s", __func__, str.c_str());
  jstring jresource = env->NewStringUTF(str.c_str());
  return jresource;
}

extern "C" void
Java_com_qualcomm_qti_services_systemhelper_SysHelperService_sendConfirmation(
            JNIEnv *env, jclass /*cls*/) {

    LOGD("%s: ++ %d request_q.size: %d", __func__, __LINE__, request_q.size());
    std::lock_guard<std::mutex> lock(resourceLock);
    //request is done processing
    if (!request_q.empty()) {
        sem_t *s  = *(request_q.front());
        request_q.pop();
        LOGD("%s: %d Pending requests: %d", __func__, __LINE__, request_q.size());
        if (s != nullptr) {
            sem_post(s);
        }
    }
}

void processRequest(string resource) {
    sem_t* waitForRequest = (sem_t*) malloc(sizeof(sem_t));
    int32_t ret = 0;
    struct timespec ts;

    if (waitForRequest == nullptr) {
           LOGE("%s: %d Out of system heap !!! ", __func__, __LINE__);
           LOGE("%s: %d Cannot process request : %s ", __func__, __LINE__, resource.c_str());
           return;
    }

    LOGD("%s: ++ %d request for %s", __func__, __LINE__,  resource.c_str());
    {
        std::lock_guard<std::mutex> lock(resourceLock);
        msg_q.push(resource);
        ret = sem_init(waitForRequest, 0, 0);
        LOGD("%s: sem init returned ret: %d (%s)", __func__, ret, strerror(errno));
        if (ret != 0) {
            goto end;
        }
        request_q.push(&waitForRequest);
        ret = sem_post(&sem);
        LOGD("%s: sem post returned ret: %d (%s)", __func__, ret, strerror(errno));
        if (ret != 0) {
            goto end;
        }
    }

    //Initialize
    ts.tv_sec = 0;
    ts.tv_nsec = 0;
    clock_gettime(CLOCK_REALTIME, &ts);

    //Wait until request has been completed or 200ms
    ts.tv_nsec += 200000000;
    ts.tv_sec += (ts.tv_nsec/1000000000);
    ts.tv_nsec %= 1000000000;

    ret = sem_timedwait(waitForRequest, &ts);
    LOGD("%s: wait returned ret: %d (%s)", __func__, ret, strerror(errno));

end:
    {
        std::lock_guard<std::mutex> lock(resourceLock);
        if (waitForRequest != nullptr) {
            LOGD("%s: %d free sem", __func__, __LINE__);
            sem_destroy(waitForRequest);
            free(waitForRequest);
            waitForRequest = nullptr;
        }
    }
}

void acquireWakeLock() {
    processRequest("WL_AQUIRE");
}

void releaseWakeLock() {
    processRequest("WL_RELEASE");
}

void lockRotation(jboolean lock) {
    string str = (lock) ? "ROT_LOCK" : "ROT_UNLOCK";
    processRequest(str);
}

bool isOrientationPortrait() {
    return orientationPortrait;
}

extern "C" jint
Java_com_qualcomm_qti_services_systemhelper_SysHelperService_terminate(
    JNIEnv * /*env*/, jclass /*cls*/) {
    return 0;
}

extern "C" void
Java_com_qualcomm_qti_services_systemhelper_SysHelperService_setOrientationPortrait(
            JNIEnv * /*env*/, jclass /*cls*/, jboolean state) {
    if (state == JNI_TRUE) {
        orientationPortrait = true;
    } else {
        orientationPortrait = false;
    }
}
}
