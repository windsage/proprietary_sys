/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#include "VtsHalDataConnectionAidlTargetTest.h"
#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <log/log.h>
#include <vector>
#include <cutils/properties.h>

#define ASSERT_OK(res) ASSERT_TRUE(res.isOk())

void DataConnectionTest::SetUp()
{
    mConnection = IDataConnection::fromBinder(::ndk::SpAIBinder(AServiceManager_waitForService(
                           "vendor.qti.hardware.data.connectionaidl.IDataConnection/slot1")));
    mConnection2 = IDataConnection::fromBinder(::ndk::SpAIBinder(AServiceManager_waitForService(
                           "vendor.qti.hardware.data.connectionaidl.IDataConnection/slot2")));
    ASSERT_NE(nullptr, mConnection.get()) << "Could not get DataConnection instance slot1";
    count = 0;
}

// get bearer allocation request/response
TEST_F(DataConnectionTest, TestGetBearerAllocation) {
    StatusCode status = StatusCode::OK;
    int32_t cid = 0;
    std::shared_ptr<BearerAllocationResponse> mResponse = ndk::SharedRefBase::make<BearerAllocationResponse>();
    mResponse->setTest(this);
    ndk::ScopedAStatus ret = mConnection->getBearerAllocation(cid , mResponse, &status);
    ASSERT_OK(ret);
    EXPECT_EQ(std::cv_status::no_timeout, wait());
}

// register bearer allocation indication
TEST_F(DataConnectionTest, TestEnableBearerAllocationUpdates) {
    StatusCode status = StatusCode::OK;
    std::shared_ptr<BearerAllocationIndication> mIndication = ndk::SharedRefBase::make<BearerAllocationIndication>();
    mIndication->setTest(this);
    ndk::ScopedAStatus ret = mConnection->registerForAllBearerAllocationUpdates(mIndication, &status);
    ASSERT_OK(ret);
    EXPECT_EQ(std::cv_status::no_timeout, wait());
}

// get bearer allocation request/response
TEST_F(DataConnectionTest, TestGetAllBearerAllocations) {
    StatusCode status = StatusCode::OK;
    std::shared_ptr<BearerAllocationResponse> mResponse = ndk::SharedRefBase::make<BearerAllocationResponse>();
    mResponse->setTest(this);
    ndk::ScopedAStatus ret = mConnection->getAllBearerAllocations(mResponse, &status);
    ASSERT_OK(ret);
    EXPECT_EQ(std::cv_status::no_timeout, wait());
}

// multisim: get IDataConnection HAL instance on slot2
TEST_F(DataConnectionTest, TestGetConnection2) {
    ASSERT_NE(nullptr, mConnection2.get()) << "Could not get DataConnection instance slot2";
}

// multisim: get bearer allocation request/response on slot2
TEST_F(DataConnectionTest, TestGetBearerAllocation2) {
    StatusCode status = StatusCode::OK;
    int32_t cid = 0;
    ASSERT_NE(nullptr, mConnection2.get()) << "Could not get DataConnection instance";
    std::shared_ptr<BearerAllocationResponse> mResponse = ndk::SharedRefBase::make<BearerAllocationResponse>();
    mResponse->setTest(this);
    ndk::ScopedAStatus ret = mConnection2->getBearerAllocation(cid, mResponse, &status);
    ASSERT_OK(ret);
    EXPECT_EQ(std::cv_status::no_timeout, wait());
}

TEST_F(DataConnectionTest, TestGetXlatPropertyValue1) {
    ASSERT_NE(nullptr, mConnection.get()) << "Could not get DataConnection instance";
    std::string xlatRequired = "persist.vendor.net.doxlat";
    std::string result = "";
    mConnection->getConfig(xlatRequired, "", &result);
    ASSERT_NE("", result.c_str());
}

TEST_F(DataConnectionTest, TestGetXlatPropertyValue2) {
    ASSERT_NE(nullptr, mConnection2.get()) << "Could not get DataConnection instance";
    std::string xlatRequired = "persist.vendor.net.doxlat";
    std::string result = "";
    mConnection2->getConfig(xlatRequired, "", &result);
    ASSERT_NE("", result.c_str());
}

int main(int argc, char** argv) {
    ::testing::InitGoogleTest(&argc, argv);
    ABinderProcess_setThreadPoolMaxThreadCount(1);
    ABinderProcess_startThreadPool();
    return RUN_ALL_TESTS();
}