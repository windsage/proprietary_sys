/*
 * Copyright (c) 2019, 2022-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#include "SystemResourceAIDL.h"
#include "SystemEventAIDL.h"
#include <ISurfaceComposer.h>
#include <PixelFormat.h>
#include <Surface.h>
#include <SurfaceComposerClient.h>
#include <errno.h>
#include <iostream>
#include <iterator>
#include <jni.h>
#include <map>
#include <mutex>
#include <stdlib.h>
#include <stringl.h>
#include <time.h>
#include <unistd.h>
#include <utils/Log.h>
#include <aidlcommonsupport/NativeHandle.h>
#include <android-base/scopeguard.h>
#include <gui/bufferqueue/1.0/H2BGraphicBufferProducer.h>
#include <gui/bufferqueue/2.0/B2HGraphicBufferProducer.h>

#define MAX_RETRY 5
#define INVALID_RESOURCEID 0xFFFFFFFF

using namespace std;
using namespace android;
using android::sp;
using android::hardware::Return;
using android::hardware::Void;
using ::android::hardware::graphics::bufferqueue::V2_0::utils::B2HGraphicBufferProducer;
using ::aidl::vendor::qti::hardware::systemhelperaidl::SystemResourceType;
using ::android::IGraphicBufferProducer;

extern "C" {
// these function are defined in svc.cpp
extern void acquireWakeLock();
extern void releaseWakeLock();
extern void lockRotation(jboolean lock);
extern bool isOrientationPortrait();
}

#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "SYS_RESOURCE_AIDL"

namespace aidl {
namespace vendor {
namespace qti {
namespace hardware {
namespace systemhelperaidl {

struct surfaceData {
    sp<SurfaceComposerClient> mSurfaceComposer;
    sp<Surface> mSurface;
    sp<SurfaceControl> mControl;
    sp<B2HGraphicBufferProducer> mHGBP = NULL;
    sp<IGraphicBufferProducer> mIGBP = NULL;

};

std::mutex resourceLock;
map<uint32_t, SystemResourceType> resourceMap;
map<uint32_t, surfaceData *> surfaceMap;

uint32_t SystemResource::getResourceId(void) {
    uint32_t rId = 0;
    int32_t i = 0;

    srand(time(0));
    rId = rand();

    // Following logic does a retry if rand number is repeated
    for (i = 0; i < MAX_RETRY; i++) {
        if (resourceMap.find(rId) != resourceMap.end()) {
            rId = rand();
        } else {
            break;
        }
    }

    if (i == MAX_RETRY) {
        ALOGE("%s: duplicate resource id generated", __func__);
        rId = INVALID_RESOURCEID;
    }
    return rId;
}

bool SystemResource::isExist(SystemResourceType resource) {
    for (auto itr = resourceMap.begin(); itr != resourceMap.end(); ++itr) {
        if (itr->second == resource) {
            return 1;
        }
    }
    return 0;
}

int32_t SystemResource::releaseSurface(uint32_t surfaceId) {
    int32_t result = 0, ret = 0;
    struct surfaceData *sData = nullptr;

    std::lock_guard<std::mutex> lock(resourceLock);
    auto itr = surfaceMap.find(surfaceId);
    if (itr == surfaceMap.end()) {
        ALOGD("%s invalid surface id passed", __func__);
        result = -EINVAL;
        goto EXIT;
    }

    sData = itr->second;
    if (sData->mSurface.get() != NULL) {
        sData->mSurface.clear();
        sData->mSurface = NULL;
    }
    if (sData->mControl.get() != NULL) {
        sData->mControl.clear();
        sData->mControl = NULL;
    }
    if (sData->mSurfaceComposer.get() != NULL) {
        sData->mSurfaceComposer->dispose();
        sData->mSurfaceComposer.clear();
        sData->mSurfaceComposer = NULL;
    }

    ALOGD("%s: SurfaceComposerClient deleted surface ", __func__);

    ret = sData->mIGBP->disconnect(3 /*NATIVE_WINDOW_API_MEDIA*/,
                 IGraphicBufferProducer::DisconnectMode::AllLocal);
    ALOGD("%s disconnect surface returned ret : %d", __func__, ret);

    surfaceMap.erase(surfaceId);
    delete sData;

EXIT:
    return result;
}

int32_t SystemResource::releaseResource(uint32_t resourceId) {
    int32_t result = 0;

    std::lock_guard<std::mutex> lock(resourceLock);
    auto itr = resourceMap.find(resourceId);
    if (itr == resourceMap.end()) {
        ALOGD("%s invalid resource id passed", __func__);
        result = -EINVAL;
        goto EXIT;
    }

    switch (itr->second) {
    case SystemResourceType::WAKE_LOCK:
        resourceMap.erase(resourceId);
        if (!isExist(SystemResourceType::WAKE_LOCK)) {
            releaseWakeLock();
        }
        break;

    case SystemResourceType::ROTATION_LOCK:
        resourceMap.erase(resourceId);
        if (!isExist(SystemResourceType::ROTATION_LOCK)) {
            lockRotation(false);
        }
        break;

    default:
        ALOGE("%s requested resource is invalid, should never come here", __func__);
        result = -EINVAL;
        resourceMap.erase(resourceId);
    }

EXIT:
    return result;
}

// Methods from  ::aidl::vendor::qti::hardware::systemhelper_aidl::ISystemResource
// follow.
ScopedAStatus SystemResource::acquire(SystemResourceType resource,
                                      int32_t* _aidl_return) {
    int32_t result = 0;
    int32_t resourceId = INVALID_RESOURCEID;

    std::lock_guard<std::mutex> lock(resourceLock);
    resourceId = getResourceId();
    if (resourceId == INVALID_RESOURCEID) {
        result = -1;
        goto EXIT;
    }

    switch (resource) {
        case SystemResourceType::WAKE_LOCK:
            if (!isExist(SystemResourceType::WAKE_LOCK)) {
                acquireWakeLock();
            }
            break;

        case SystemResourceType::ROTATION_LOCK:
            if (!isExist(SystemResourceType::ROTATION_LOCK)) {
                lockRotation(true);
            }
            break;

        default:
            result = -EINVAL;
            ALOGE("%s requested resource %ld is invalid", __func__, resource);
    }

    if (!result) {
        resourceMap.insert(
             pair<uint32_t, SystemResourceType>(resourceId, resource));
    }
EXIT:
    *_aidl_return = resourceId;
    if (result == 0) {
        return ScopedAStatus::ok();
    } else {
        return ScopedAStatus(AStatus_fromServiceSpecificError(result));
    }
}

ScopedAStatus SystemResource::acquireSurface(int32_t width, int32_t height,
                                             NativeHandle* out_Handle,
                                             int32_t* _aidl_return) {
    int result = -1;
    int maxRetries = 200;
    struct surfaceData *sData = nullptr;
    SurfaceComposerClient::Transaction t;
    uint32_t resourceId = getResourceId();
    native_handle_t* handle = nullptr;

    if (resourceId == INVALID_RESOURCEID) {
        goto EXIT;
    }

    sData = new surfaceData();
    if (!sData) {
        result = -ENOMEM;
        ALOGE("%s: failed to allocate memory", __func__);
        goto EXIT;
    }

    memset(sData, 0, sizeof(struct surfaceData));
    sData->mSurfaceComposer = new SurfaceComposerClient();
    if (sData->mSurfaceComposer->initCheck() != (::android::status_t) OK) {
        ALOGE("%s: failed to allocate SurfaceComposer", __func__);
        goto ERROR;
    }
    sData->mControl = (sData->mSurfaceComposer)
                        ->createSurface(::android::String8("systemHelperService"),
                                        width, height, PIXEL_FORMAT_OPAQUE);
    if (!sData->mControl) {
        ALOGE("%s: failed to create Surface Control", __func__);
        goto ERROR;
    }

    t.setLayer(sData->mControl, INT_MAX - 1);
    if (t.apply(true  /* synchronous */) != OK) {
        ALOGE("%s: setLayer failed", __func__);
        goto ERROR;
    }

  /* Due to framework delay orientation is taking time to reflect on the display
     which may cause failure while starting new TUI session, waiting for correct
     orientation to reflect on display. */
    if (!isOrientationPortrait()) {
        ALOGD("%s: Waiting for device to move to potrait orientation", __func__);
        //Wait for at max 2 sec for device to move to potrait orientation
        while(!isOrientationPortrait() && maxRetries > 0) {
            usleep(10000); //Sleep for 10ms and recheck if orientation is potrait
            maxRetries--;
        }

        if (maxRetries == 0) {
            // Returning error as not able to move the device to potrait orientation
            ALOGE("%s: Failed to move device to potrait orientation", __func__);
            goto ERROR;
        }

        usleep(50000); //Waiting 50ms for new orientation to take effect on display
        ALOGD("%s: Device moved to potrait orientation", __func__);
    }

    sData->mSurface = (sData->mControl)->getSurface();
    if (!sData->mSurface) {
        ALOGE("%s: failed to create Surface", __func__);
        goto ERROR;
    }

    sData->mIGBP = (sData->mSurface)->getIGraphicBufferProducer();
    if (!sData->mIGBP) {
        ALOGE("%s: failed to get mIGBP interface", __func__);
        goto ERROR;
    }
    sData->mHGBP = new B2HGraphicBufferProducer(sData->mIGBP);
    if (!sData->mHGBP) {
        ALOGE("%s: failed to create mHGBP interface", __func__);
        goto ERROR;
    }


    // Insert the newly created entry to map
    {
        std::lock_guard<std::mutex> lock(resourceLock);
        surfaceMap.insert(pair<uint32_t, surfaceData *>(resourceId, sData));
    }

    result = 0;

    *_aidl_return = resourceId;
    *out_Handle = std::move(::android::dupToAidl(handle));
    return ndk::ScopedAStatus::ok();

ERROR:
    if (sData->mSurface) {
        sData->mSurface.clear();
        sData->mSurface = NULL;
    }
    if (sData->mControl) {
        sData->mControl.clear();
        sData->mControl = NULL;
    }
    if (sData->mSurfaceComposer) {
        sData->mSurfaceComposer->dispose();
        sData->mSurfaceComposer.clear();
        sData->mSurfaceComposer = NULL;
    }
    delete sData;

EXIT:
    resourceId = INVALID_RESOURCEID;
    *_aidl_return = resourceId;
    *out_Handle = std::move(::android::dupToAidl(nullptr));
    return ScopedAStatus(AStatus_fromServiceSpecificError(result));
}

ScopedAStatus SystemResource::release(int32_t resourceId) {
    int32_t result = -EINVAL;

    result = releaseResource(resourceId);
    if (result == -EINVAL) {
        result = releaseSurface(resourceId);
    }

    if (!result) {
        ALOGD("%s: Successfully released resource with Id = 0x%x", __func__,
                resourceId);
        return ScopedAStatus::ok();
    }
    return ScopedAStatus(AStatus_fromServiceSpecificError(result));
}

}   // namespace systemhelperaidl
}   // namespace hardware
}   // namespace qti
}   // namespace vendor
}   // namespace aidl
