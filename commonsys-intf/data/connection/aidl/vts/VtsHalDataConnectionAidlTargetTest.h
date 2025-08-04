/*
 *  * Copyright (C) 2017 The Android Open Source Project
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#include <aidl/vendor/qti/hardware/data/connectionaidl/IDataConnection.h>
#include <aidl/vendor/qti/hardware/data/connectionaidl/BnDataConnectionResponse.h>
#include <aidl/vendor/qti/hardware/data/connectionaidl/BnDataConnectionIndication.h>
#include <aidl/Gtest.h>
#include <aidl/Vintf.h>
#include <log/log.h>

#define TIMEOUT_PERIOD 20

using namespace ::aidl::vendor::qti::hardware::data::connectionaidl;

class DataConnectionTest : public ::testing::Test {
  protected:
    std::mutex mtx;
    std::condition_variable cv;
    int count;
  public:
    virtual void SetUp() override;

    /* radio data service handle */
    std::shared_ptr<IDataConnection> mConnection = nullptr;
    /* radio data service handle for slot2*/
    std::shared_ptr<IDataConnection> mConnection2 = nullptr;

    std::cv_status wait() {
        std::unique_lock<std::mutex> lock(mtx);

        std::cv_status status = std::cv_status::no_timeout;
        auto now = std::chrono::system_clock::now();
        while (count == 0) {
            status = cv.wait_until(lock, now + std::chrono::seconds(TIMEOUT_PERIOD));
            if (status == std::cv_status::timeout) {
              return status;
            }
        }
        count--;
        return status;
    }

    void notify() {
      std::unique_lock<std::mutex> lock(mtx);
        count++;
        cv.notify_one();
    }
};

void printBearers(const AllocatedBearers& bearers) {
    ALOGI("AllocatedBearers for cid=%d apn=%s", bearers.cid, bearers.apn.c_str());
    for (BearerInfo bearer : bearers.bearers) {
        ALOGI("Bearer: id=%d ul=%d dl=%d", bearer.bearerId, bearer.uplink, bearer.downlink);
    }
}

class BearerAllocationResponse : public BnDataConnectionResponse {
protected:
    DataConnectionTest *test = nullptr;
public:
    BearerAllocationResponse() {}
    void setTest (DataConnectionTest* parent) { test = parent;}
    virtual ~BearerAllocationResponse() {};

    ::ndk::ScopedAStatus onAllBearerAllocationsResponse(ErrorReason error, const std::vector<AllocatedBearers>& bearersList) override {
        ALOGI("onAllBearerAllocationsResponse arrived error=%d bearerSize=%d", error, (uint32_t)bearersList.size());
        for (AllocatedBearers bearers : bearersList) {
            printBearers(bearers);
        }
        if (test)
            test->notify();
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus onBearerAllocationResponse(ErrorReason error, const AllocatedBearers& bearers) override {
        ALOGI("onBearerAllocationResponse arrived error=%d", error);
        printBearers(bearers);
        if (test)
            test->notify();
        return ndk::ScopedAStatus::ok();
    }

};

class BearerAllocationIndication : public BnDataConnectionIndication {
protected:
    DataConnectionTest *test = nullptr;
public:
    BearerAllocationIndication() {}
    void setTest (DataConnectionTest* parent) { test = parent;}
    virtual ~BearerAllocationIndication() {}

    ::ndk::ScopedAStatus onBearerAllocationUpdate(const std::vector<AllocatedBearers>& bearersList) override {
        ALOGI("onBearerAllocationUpdate bearerSize=%d", (uint32_t)bearersList.size());
        for (AllocatedBearers bearers : bearersList) {
            printBearers(bearers);
        }
        if (test)
            test->notify();
        return ndk::ScopedAStatus::ok();
    }
};