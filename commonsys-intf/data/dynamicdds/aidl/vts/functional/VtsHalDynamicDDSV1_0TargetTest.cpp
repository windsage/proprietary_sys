/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#define LOG_TAG "dynamicdds_vts"

#include <log/log.h>
#include <aidl/vendor/qti/data/factoryservice/IFactory.h>
#include <aidl/vendor/qti/data/factoryservice/Result.h>
#include <aidl/vendor/qti/data/factoryservice/StatusCode.h>
#include <aidl/vendor/qti/hardware/data/dynamicddsaidlservice/ISubscriptionManager.h>
#include <aidl/vendor/qti/hardware/data/dynamicddsaidlservice/IToken.h>

#include <android/binder_manager.h>
#include <android/binder_process.h>

using ::aidl::vendor::qti::data::factoryservice::IFactory;
using ::aidl::vendor::qti::data::factoryservice::Result;
using ::aidl::vendor::qti::data::factoryservice::StatusCode;
using ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::ISubscriptionManager;
using ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::IToken;
using ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::ISetAppPreferencesCallback;
using ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::IGetAppPreferencesCallback;
using ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::IDddsCallback;
using ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::Carrier;
using ddsStatusCode = ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::StatusCode;
using ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::SubscriptionConfig;

// using ddsStatusCode = ::aidl::vendor::qti::hardware::data::dynamicddsaidlservice::StatusCode;
#define NUM_OF_STATUS_CODE 4

//Need to sync with types.hal
std::string statusCode[NUM_OF_STATUS_CODE] = {
    "OK",
    "INVALID_ARGUMENTS",
    "NOT_SUPPORTED",
    "FAILED",
};

class Token : public IToken {
    ::ndk::ScopedAStatus getInterfaceVersion(int32_t* _aidl_return) {
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus getInterfaceHash(std::string* _aidl_return) {
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::SpAIBinder asBinder() {
        return ::ndk::SpAIBinder();
    }
    bool isRemote() {
       return false;
    }
};

class DataFactoryTestBase : public testing::TestWithParam<std::string> {
public:
    virtual void SetUp() override {
        setUpFactory();
    }

    void setUpFactory() {
        //Get the AIDL service
        static const std::string instance = std::string() + IFactory::descriptor + "/default";
        ndk::SpAIBinder binder(AServiceManager_waitForService(instance.c_str()));
        factory = IFactory::fromBinder(binder);
        ASSERT_NE(nullptr, factory.get()) << "Could not get Factory";
    }

    std::shared_ptr<ISubscriptionManager> createManager
        (const ::aidl::vendor::qti::data::factoryservice::StatusCode expected_result) {


        // auto cb = [&](::aidl::vendor::qti::data::factoryservice::IFactory::StatusCode status,
        //               std::shared_ptr<ISubscriptionManager> manager) {
        //     ASSERT_EQ(expected_result, status);

        //     subManager = manager;
        //     ASSERT_NE(nullptr, subManager.get()) << "Could not create ISubscriptionManager";
        // };

//        
         std::shared_ptr<ISubscriptionManager> subManager;
         std::shared_ptr<IToken> token = ndk::SharedRefBase::make<Token>();
         Result* result = new Result();
         ALOGI("Factory VTS getting subsctiption Manger");
         factory->createDynamicddsISubscriptionManager(token,result,&subManager);

         ALOGI("Factory VTS getting subsctiption Manger %p",subManager.get());
         //?TODO
         // ASSERT_NE(subManager.get(),nullptr) << "Could not create ISubscriptionManager";
         return subManager;
    }

    std::shared_ptr<IFactory> factory;
};

// positive test. Ensure IFactory can request an ISubscriptionManager
TEST_F(DataFactoryTestBase, TestCreateSubscriptionManager) {
    std::shared_ptr<ISubscriptionManager> manager = createManager(
       ::aidl::vendor::qti::data::factoryservice::StatusCode::OK);
    ASSERT_NE(nullptr, manager.get()) << "Could not get Subscription Manager instance";
}

class DynamicDDSTestBase : public DataFactoryTestBase {
public:
    virtual void SetUp() override {
        setUpFactory();
        manager = createManager(
            StatusCode::OK);
    }

    std::shared_ptr<ISubscriptionManager> manager;
};

/* ---------------------------- getAppPreferences test elements------------------------------ */
class GetAppPreferencesCallbackArgs {
public:
    GetAppPreferencesCallbackArgs(ddsStatusCode setStatus,
                                  const std::vector<SubscriptionConfig>& setPreferences) :
        status(setStatus), preferences(setPreferences) {}

    ddsStatusCode getStatus() {return status;};
    const std::vector<SubscriptionConfig>& getPreferences() {return preferences;};

private:
    ddsStatusCode status;
    const std::vector<SubscriptionConfig>& preferences;
};

class MockGetAppPreferencesCallback : public IGetAppPreferencesCallback,
    public ::testing::VtsHalHidlTargetCallbackBase<GetAppPreferencesCallbackArgs> {

public:
    MockGetAppPreferencesCallback() {};
    virtual ~MockGetAppPreferencesCallback() {};

        ndk::ScopedAStatus onResult(ddsStatusCode status,
                          const std::vector<SubscriptionConfig>& preferences) override {
        ALOGI("GetAppPreferencesCallback arrived");
        dumpStatusCode(status);
        if (status == ddsStatusCode::OK) {
            for (SubscriptionConfig subConfig : preferences) {
                dumpSubscriptionConfig(subConfig);
            }
        }
        GetAppPreferencesCallbackArgs args(status, preferences);
        NotifyFromCallback("onResult", args);
        return ::ndk::ScopedAStatus::ok();
    };

private:
    //Helper function to dump status code
    static void dumpStatusCode(const ddsStatusCode& status) {
        ALOGI("Status = %s", statusCode[(int)status].c_str());
    }

    //Helper function to dump application preference
    static void dumpSubscriptionConfig(const SubscriptionConfig& subconfig) {
        ALOGI("appName = %s", subconfig.appName.c_str());
        ALOGI("priority = %d", subconfig.priority);
        ALOGI("Carriers = ");
        for (const Carrier carrier : subconfig.carriers) {
            ALOGI("%s, %d;", carrier.iin.c_str(), carrier.preference);
        }
    }
    ::ndk::ScopedAStatus getInterfaceVersion(int32_t* _aidl_return) {
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus getInterfaceHash(std::string* _aidl_return) {
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::SpAIBinder asBinder() {
        return ::ndk::SpAIBinder();
    }
    bool isRemote() {
       return false;
    }
};

// /* ---------------------------- setAppPreferences test elements ----------------------------- */
class SetAppPreferencesCallbackArgs {
public:
    SetAppPreferencesCallbackArgs(ddsStatusCode setStatus, string setReason) :
        status(setStatus), reason(setReason){}

    ddsStatusCode getStatus() {return status;};

private:
    ddsStatusCode status;
    string reason;
};

class MockSetAppPreferencesCallback : public ISetAppPreferencesCallback,
    public ::testing::VtsHalHidlTargetCallbackBase<SetAppPreferencesCallbackArgs> {

public:
    MockSetAppPreferencesCallback() {};
    virtual ~MockSetAppPreferencesCallback() {};

    ::ndk::ScopedAStatus onResult(ddsStatusCode status, const std::string& reason) override {
        ALOGI("SetAppPreferencesCallback arrived");
        dumpStatusCode(status);
        dumpReason(reason);
        SetAppPreferencesCallbackArgs args(status, reason);
        NotifyFromCallback("onResult", args);
        return ::ndk::ScopedAStatus::ok();
    };

private:
    //Helper function to dump status code
    static void dumpStatusCode(const ddsStatusCode& status) {
        ALOGI("Status = %s", statusCode[(int)status].c_str());
    }

    //Helper function to dump reason
    static void dumpReason(const std::string& reason) {
        ALOGI("Detailed Reason = %s", reason.c_str());
    }
    ::ndk::ScopedAStatus getInterfaceVersion(int32_t* _aidl_return) {
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus getInterfaceHash(std::string* _aidl_return) {
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::SpAIBinder asBinder() {
        return ::ndk::SpAIBinder();
    }
    bool isRemote() {
       return false;
    }
};

class AppPreferencesBuilder {
public:
    class SubscriptionConfigType {
    public:

        SubscriptionConfigType(){};
        virtual ~SubscriptionConfigType(){};

        void setAppName(std::string appName) {this->appName = appName;};
        std::string getAppName() {return appName;};

        void addCarrier(std::string iin, uint8_t preference) {
            Carrier carrier;
            carrier.iin = iin;
            carrier.preference = preference;
            carriersList.push_back(carrier);
        };

        void setPriority(uint8_t priority) {this->priority = priority;};
        uint8_t getPriority() {return priority;};

        SubscriptionConfig getSubscriptionConfig() {
            SubscriptionConfig subscriptionConfig;
            subscriptionConfig.appName = this->appName;
            subscriptionConfig.priority = this->priority;
            subscriptionConfig.carriers = this->carriersList;
            return subscriptionConfig;
        };

        void dumpSubscriptionConfig() {
            ALOGI("SubscriptionConfig: appName = %s", appName.c_str());
            ALOGI("SubscriptionConfig: priority = %d", priority);
            ALOGI("SubscriptionConfig: CarrierList = ");
            for (const Carrier carrier : this->carriersList) {
                ALOGI("%s, %d;", carrier.iin.c_str(), carrier.preference);
            }
        }

        void clear() {
            appName = "";
            priority = 0;
            carriersList.clear();
        }
    private:
        std::string appName;
        uint8_t priority;
        std::vector<Carrier> carriersList;

    };

    AppPreferencesBuilder(){};
    virtual ~AppPreferencesBuilder(){};

    void addSubscriptionConfig(SubscriptionConfigType subconfig){
        preferences.push_back(subconfig);
    }

    void dumpInputBox() {
        for (SubscriptionConfigType subConfig : preferences) {
            subConfig.dumpSubscriptionConfig();
        }
    }

    std::vector<SubscriptionConfig> getPreferences() {
        std::vector<SubscriptionConfig> result;
        for (SubscriptionConfigType subConfig : preferences) {
            result.push_back(subConfig.getSubscriptionConfig());
        }
        return result;
    }

private:
    std::vector<SubscriptionConfigType> preferences;
};

// /* ---------------------- registerForDynamicSubChanges test elements------------------------- */
class DddsCallbackArgs {
public:
    DddsCallbackArgs(bool setAvailable) :
        available(setAvailable){}

    bool getIsAvailable() {return available;};

private:
    bool available;
};

class MockDddsCallback : public IDddsCallback,
    public ::testing::VtsHalHidlTargetCallbackBase<DddsCallbackArgs> {

public:
    MockDddsCallback() {};
    virtual ~MockDddsCallback() {};

    ::ndk::ScopedAStatus onFeatureAvailable(bool available) override {
        ALOGI("DddsCallback onFeatureAvailable arrived with available = %d", available);
        DddsCallbackArgs args(available);
        NotifyFromCallback("onFeatureAvailable", args);
        return ::ndk::ScopedAStatus::ok();
    };

    ::ndk::ScopedAStatus onSubChanged(int slotId) override {
        ALOGI("DddsCallback onSubChanged arrived with slotId = %d", slotId);
        return ::ndk::ScopedAStatus::ok();
    };

    ::ndk::ScopedAStatus onRecommendedSubChange(int slotId) override {
       ALOGI("DddsCallback onRecommendedSubChange arrived with slotId = %d", slotId);
       return ::ndk::ScopedAStatus::ok();  
    };

    ::ndk::ScopedAStatus getInterfaceVersion(int32_t* _aidl_return) {
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus getInterfaceHash(std::string* _aidl_return) {
        return ndk::ScopedAStatus::ok();
    }

    ::ndk::SpAIBinder asBinder() {
        return ::ndk::SpAIBinder();
    }
    bool isRemote() {
       return false;
    }

};

/* ---------------------- begin of DynamicDDS HAL Test cases ------------------------------- */
// positive test. setDynamicSubscriptionChange with enable: true
TEST_F(DynamicDDSTestBase, TestSetDynamicSubscriptionChange_On) {
    ddsStatusCode status ;
    manager->setDynamicSubscriptionChange(true,&status);
    ASSERT_EQ(ddsStatusCode::OK, status);
}

// positive test. clearAppPreferences to clear all preconfigured application preferences
TEST_F(DynamicDDSTestBase, TestClearAppPreferences) {
    ddsStatusCode status ;
    manager->clearAppPreferences(&status);
    ASSERT_EQ(ddsStatusCode::OK, status);
}

// negative test. getAppPreferences to fetch modem configured application preferences.
// However, there is no preconfigured application preferences in modem
TEST_F(DynamicDDSTestBase, TestGetAppPreferences_EmptyPreferences) {
    std::shared_ptr<MockGetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockGetAppPreferencesCallback>();

    ddsStatusCode status;
    manager->getAppPreferences(mCb,&status);

    auto res = mCb->WaitForCallback("onResult");

    ASSERT_EQ(ddsStatusCode::OK, status);
    EXPECT_TRUE(res.no_timeout);
    if (res.no_timeout == true) {
        EXPECT_EQ(ddsStatusCode::OK, res.args->getStatus());
        EXPECT_EQ((unsigned int)0, res.args->getPreferences().size());
    }
}

// negative test. setAppPreferences try to set empty preference
TEST_F(DynamicDDSTestBase, TestSetAppPreferences_EmptyPreference) {
    std::shared_ptr<MockSetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockSetAppPreferencesCallback>();
    AppPreferencesBuilder appPreferencesBuilder;
    AppPreferencesBuilder::SubscriptionConfigType subConfig;

    ddsStatusCode status;
    manager->setAppPreferences(appPreferencesBuilder.getPreferences(), mCb,&status);
    ASSERT_EQ(ddsStatusCode::INVALID_ARGUMENTS, status);
}

// negative test. setAppPreferences try to set empty appName
TEST_F(DynamicDDSTestBase, TestSetAppPreferences_EmptyAppName) {
    std::shared_ptr<MockSetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockSetAppPreferencesCallback>();
    AppPreferencesBuilder appPreferencesBuilder;
    AppPreferencesBuilder::SubscriptionConfigType subConfig;

    subConfig.setAppName("");
    subConfig.setPriority(7);
    subConfig.addCarrier("8986003",5);
    subConfig.addCarrier("8986001",4);
    appPreferencesBuilder.addSubscriptionConfig(subConfig);
    subConfig.clear();

    ddsStatusCode status;
    manager->setAppPreferences(appPreferencesBuilder.getPreferences(), mCb,&status);
    ASSERT_EQ(ddsStatusCode::INVALID_ARGUMENTS, status);
}

// negative test. setAppPreferences try to set invalid priority
TEST_F(DynamicDDSTestBase, TestSetAppPreferences_InvalidPriority) {
    std::shared_ptr<MockSetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockSetAppPreferencesCallback>();
    AppPreferencesBuilder appPreferencesBuilder;
    AppPreferencesBuilder::SubscriptionConfigType subConfig;

    subConfig.setAppName("com.android.wechat");
    subConfig.setPriority(12);  //12 is invalid priority
    subConfig.addCarrier("8986003",5);
    subConfig.addCarrier("8986001",4);
    appPreferencesBuilder.addSubscriptionConfig(subConfig);
    subConfig.clear();

    ddsStatusCode status;
    manager->setAppPreferences(appPreferencesBuilder.getPreferences(), mCb,&status);
    ASSERT_EQ(ddsStatusCode::INVALID_ARGUMENTS, status);
}

// negative test. setAppPreferences try to set application with empty carriers
TEST_F(DynamicDDSTestBase, TestSetAppPreferences_EmptyCarrier) {
    std::shared_ptr<MockSetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockSetAppPreferencesCallback>();
    AppPreferencesBuilder appPreferencesBuilder;
    AppPreferencesBuilder::SubscriptionConfigType subConfig;

    subConfig.setAppName("com.android.wechat");
    subConfig.setPriority(7);
    //Carriers is empty
    appPreferencesBuilder.addSubscriptionConfig(subConfig);
    subConfig.clear();

    ddsStatusCode status ;
    manager->setAppPreferences(appPreferencesBuilder.getPreferences(), mCb,&status);
    ASSERT_EQ(ddsStatusCode::INVALID_ARGUMENTS, status);
}

// negative test. setAppPreferences try to set invalid IIN
TEST_F(DynamicDDSTestBase, TestSetAppPreferences_InvalidIIN) {
    std::shared_ptr<MockSetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockSetAppPreferencesCallback>();
    AppPreferencesBuilder appPreferencesBuilder;
    AppPreferencesBuilder::SubscriptionConfigType subConfig;

    subConfig.setAppName("com.android.wechat");
    subConfig.setPriority(7);
    subConfig.addCarrier("8986003",5);
    subConfig.addCarrier("",4); //"" is invalid IIN
    appPreferencesBuilder.addSubscriptionConfig(subConfig);
    subConfig.clear();

    ddsStatusCode status;
    manager->setAppPreferences(appPreferencesBuilder.getPreferences(), mCb,&status);
    ASSERT_EQ(ddsStatusCode::INVALID_ARGUMENTS, status);
}

// negative test. setAppPreferences try to set invalid carrier preference
TEST_F(DynamicDDSTestBase, TestSetAppPreferences_InvalidCarrierPreference) {
    std::shared_ptr<MockSetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockSetAppPreferencesCallback>();
    AppPreferencesBuilder appPreferencesBuilder;
    AppPreferencesBuilder::SubscriptionConfigType subConfig;

    subConfig.setAppName("com.android.wechat");
    subConfig.setPriority(7);
    subConfig.addCarrier("8986003",11); //11 is invalid Carrier Preference
    subConfig.addCarrier("8986001",4);
    appPreferencesBuilder.addSubscriptionConfig(subConfig);
    subConfig.clear();

    ddsStatusCode status ;
    manager->setAppPreferences(appPreferencesBuilder.getPreferences(), mCb,&status);
    ASSERT_EQ(ddsStatusCode::INVALID_ARGUMENTS, status);
}

// negative test. setAppPreferences try to set more than 100 preferences
TEST_F(DynamicDDSTestBase, TestSetAppPreferences_TooManyPreferences) {
    std::shared_ptr<MockSetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockSetAppPreferencesCallback>();
    AppPreferencesBuilder appPreferencesBuilder;
    AppPreferencesBuilder::SubscriptionConfigType subConfig;

    for (unsigned int i = 0; i < 108; ++i) { //108 is more than the max size 100
        subConfig.setAppName("com.android.wechat_" + std::to_string(i));
        subConfig.setPriority(7);
        subConfig.addCarrier("8986003",5);
        subConfig.addCarrier("8986001",4);
        appPreferencesBuilder.addSubscriptionConfig(subConfig);
        subConfig.clear();
    }

    ddsStatusCode status ;
    manager->setAppPreferences(appPreferencesBuilder.getPreferences(), mCb,&status);
    ASSERT_EQ(ddsStatusCode::INVALID_ARGUMENTS, status);
}

// positive test. setAppPreferences to set below application preference in modem
TEST_F(DynamicDDSTestBase, TestSetAppPreferences_ValidPreferences) {
    std::shared_ptr<MockSetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockSetAppPreferencesCallback>();
    AppPreferencesBuilder appPreferencesBuilder;
    AppPreferencesBuilder::SubscriptionConfigType subConfig;

    subConfig.setAppName("com.android.wechat");
    subConfig.setPriority(7);
    subConfig.addCarrier("8986003",5);
    subConfig.addCarrier("8986001",4);
    appPreferencesBuilder.addSubscriptionConfig(subConfig);
    subConfig.clear();

    subConfig.setAppName("com.android.weibo");
    subConfig.setPriority(4);
    subConfig.addCarrier("8986003",8);
    subConfig.addCarrier("8986001",2);
    appPreferencesBuilder.addSubscriptionConfig(subConfig);

    ddsStatusCode status;
    manager->setAppPreferences(appPreferencesBuilder.getPreferences(), mCb,&status);
    auto res = mCb->WaitForCallback("onResult");

    ASSERT_EQ(ddsStatusCode::OK, status);
    EXPECT_TRUE(res.no_timeout);
    if (res.no_timeout == true) {
        EXPECT_EQ(ddsStatusCode::OK, res.args->getStatus());
    }
}

// positive test. getAppPreferences to fetch modem configured application preferences
TEST_F(DynamicDDSTestBase, TestGetAppPreferences_ValidPreferences) {
    std::shared_ptr<MockGetAppPreferencesCallback> mCb = ndk::SharedRefBase::make<MockGetAppPreferencesCallback>();

    ddsStatusCode status ;
    manager->getAppPreferences(mCb,&status);
    auto res = mCb->WaitForCallback("onResult");

    ASSERT_EQ(ddsStatusCode::OK, status);
    EXPECT_TRUE(res.no_timeout);
    if (res.no_timeout == true) {
        EXPECT_EQ(ddsStatusCode::OK, res.args->getStatus());
    }
}

// positive test. setDynamicSubscriptionChange with enable: false
TEST_F(DynamicDDSTestBase, TestSetDynamicSubscriptionChange_Off) {
    ddsStatusCode status;
    manager->setDynamicSubscriptionChange(false,&status);
    ASSERT_EQ(ddsStatusCode::OK, status);
}

// positive test. Test registerForDynamicSubChanges and IDddsCallback.onFeatureAvailable
// Need to be invoke when feature is enable/disable
TEST_F(DynamicDDSTestBase, TestRegisterForDynamicSubChanges) {
    std::shared_ptr<MockDddsCallback> mCb = ndk::SharedRefBase::make<MockDddsCallback>();
    ddsStatusCode status;
    manager->registerForDynamicSubChanges(mCb,&status);
    ASSERT_EQ(ddsStatusCode::OK, status);

    manager->setDynamicSubscriptionChange(true,&status);
    auto res = mCb->WaitForCallback("onFeatureAvailable");

    ASSERT_EQ(ddsStatusCode::OK, status);
    EXPECT_TRUE(res.no_timeout);
    if (res.no_timeout == true) {
        EXPECT_TRUE(res.args->getIsAvailable());
    }

    manager->setDynamicSubscriptionChange(false,&status);
    auto res2 = mCb->WaitForCallback("onFeatureAvailable");

    ASSERT_EQ(ddsStatusCode::OK, status);
    EXPECT_TRUE(res2.no_timeout);
    if (res2.no_timeout == true) {
        EXPECT_FALSE(res2.args->getIsAvailable());
    }
}

int main(int argc, char** argv) {
    ALOGI("Enter DynamicDDS VTS test suite");
    ::testing::InitGoogleTest(&argc, argv);
    ABinderProcess_setThreadPoolMaxThreadCount(1);
    ABinderProcess_startThreadPool();
    int status = RUN_ALL_TESTS();
    ALOGI("Test result with status = %d", status);
    return status;
}
