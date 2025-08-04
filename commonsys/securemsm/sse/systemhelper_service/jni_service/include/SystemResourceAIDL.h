/*
 * Copyright (c) 2019, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#ifndef SYSTEMRESOURCEAIDL_H
#define SYSTEMRESOURCEAIDL_H

#include <android/binder_manager.h>
#include <aidl/vendor/qti/hardware/systemhelperaidl/BnSystemResource.h>
#include <aidl/vendor/qti/hardware/trustedui/ITrustedUI.h>

namespace aidl {
namespace vendor {
namespace qti {
namespace hardware {
namespace systemhelperaidl {

using ::android::sp;
using ::aidl::vendor::qti::hardware::trustedui::ITrustedUI;
using ::aidl::android::hardware::common::NativeHandle;
using ndk::ScopedAStatus;

class SystemResource : public BnSystemResource {
    // Methods from ::vendor::qti::hardware::systemhelperaidl::ISystemResource follow.
public:
    SystemResource() = default;
    virtual ~SystemResource() = default;
    ScopedAStatus acquire(SystemResourceType resource,
                          int32_t* _aidl_return) override;
    ScopedAStatus acquireSurface(int32_t width, int32_t height,
                                 NativeHandle* out_Handle,
                                 int32_t* _aidl_return) override;
    ScopedAStatus release(int32_t resourceId) override;

private:
    bool isExist(SystemResourceType resource);
    uint32_t getResourceId(void);
    int32_t releaseSurface(uint32_t resourceId);
    int32_t releaseResource(uint32_t resourceId);
};

}  // namespace systemhelper_aidl
}  // namespace hardware
}  // namespace qti
}  // namespace vendor
}  // namespace aidl
#endif
