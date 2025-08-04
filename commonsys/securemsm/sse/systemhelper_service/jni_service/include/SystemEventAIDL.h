/*
 * Copyright (c) 2019, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#ifndef SYSTEMEVENTAIDL_H
#define SYSTEMEVENTAIDL_H

#include <aidl/vendor/qti/hardware/systemhelperaidl/BnSystemEvent.h>
#include <aidl/vendor/qti/hardware/systemhelperaidl/SystemEventType.h>

namespace aidl {
namespace vendor {
namespace qti {
namespace hardware {
namespace systemhelperaidl {

using ::ndk::ScopedAStatus;
class SystemEvent : public BnSystemEvent {
    // Methods from aidl::vendor::qti::hardware::systemhelperaidl::ISystemEvent follow.
public:
    SystemEvent() = default;
    virtual ~SystemEvent() = default;
    ScopedAStatus registerEvent(int64_t eventIds, const std::shared_ptr<ISystemEventCallback>& cb, int32_t* _aidl_return) override;
    ScopedAStatus deRegisterEvent(int64_t eventIds, const std::shared_ptr<ISystemEventCallback>& cb, int32_t* _aidl_return) override;
};

}  // namespace systemhelperaidl
}  // namespace hardware
}  // namespace qti
}  // namespace vendor
}  // namespace aidl
#endif
