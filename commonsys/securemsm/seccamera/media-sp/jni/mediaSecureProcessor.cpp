/*
 * Copyright (c) 2019, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 *
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The concept for configuration data marshaling and
 * de-marshaling is borrowed from AOSP Camera MetaData.
 * Reference:
 *   platform/system/media/camera/src/camera_metadata.h[/c]
 *
 */

// #define LOG_NDEBUG 0

#define LOG_TAG "MediaSecureProcessor-JNI"

#include "mediaSecureProcessor.h"
#include <aidlcommonsupport/NativeHandle.h>
#include <android_runtime/AndroidRuntime.h>
#include <android_runtime/Log.h>
#include "jni.h"

#include <aidl/vendor/qti/hardware/secureprocessor/config/ConfigType.h>
#include <android_runtime/android_hardware_HardwareBuffer.h>
#include <binder/IServiceManager.h>
#include <binder/Parcel.h>
#include <cutils/native_handle.h>
#include <cutils/properties.h>
#include <media/stagefright/foundation/ADebug.h>
#include <utils/Log.h>
#include <vndk/hardware_buffer.h>

#include "SecureProcessorConfig.h"
#include "SecureProcessorConfigAidl.h"
#include "SecureProcessorUtils.h"

using vendor::qti::hardware::secureprocessor::common::V1_0::
    SecureProcessorCfgBuf;
using SecureProcessorConfig =
    vendor::qti::hardware::secureprocessor::common::V1_0::SecureProcessorConfig;
using SecureProcessorConfigAidl = vendor::qti::hardware::secureprocessor::
    common::aidl::SecureProcessorConfigAidl;
using ConfigTypeHidl =
    vendor::qti::hardware::secureprocessor::config::V1_0::ConfigType;
using ConfigTypeAidl =
    ::aidl::vendor::qti::hardware::secureprocessor::config::ConfigType;

/**
 * MediaSecureProcessorConfig map:
 *   The map is used for converting configuration strings
 *   to corresponding HAL defined config TAGs.
 */
std::map<std::string, int32_t> MediaSecureProcessorConfigMap = {
    {"secureprocessor.image.config.camera_id",
     ((int32_t)ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_CAMERA_ID)},
    {"secureprocessor.image.config.frame_number",
     ((int32_t)ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_NUMBER)},
    {"secureprocessor.image.config.timestamp",
     ((int32_t)ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_TIMESTAMP)},
    {"secureprocessor.image.config.frame_buffer_width",
     ((int32_t)
          ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_WIDTH)},
    {"secureprocessor.image.config.frame_buffer_height",
     ((int32_t)
          ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_HEIGHT)},
    {"secureprocessor.image.config.frame_buffer_stride",
     ((int32_t)
          ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_STRIDE)},
    {"secureprocessor.image.config.frame_buffer_format",
     ((int32_t)
          ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_FORMAT)},
    {"secureprocessor.session.config.num_sensors",
     ((int32_t)ConfigTagHidl::SECURE_PROCESSOR_SESSION_CONFIG_NUM_SENSOR)},
    {"secureprocessor.session.config.usecase_id",
     ((int32_t)
          ConfigTagHidl::SECURE_PROCESSOR_SESSION_CONFIG_USECASE_IDENTIFIER)}};

std::map<std::string, int32_t> MediaSecureProcessorConfigMapAidl = {
    {"secureprocessor.image.config.camera_id",
     ((int32_t)ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_CAMERA_ID)},
    {"secureprocessor.image.config.frame_number",
     ((int32_t)ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_NUMBER)},
    {"secureprocessor.image.config.timestamp",
     ((int32_t)ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_TIMESTAMP)},
    {"secureprocessor.image.config.frame_buffer_width",
     ((int32_t)
          ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_WIDTH)},
    {"secureprocessor.image.config.frame_buffer_height",
     ((int32_t)
          ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_HEIGHT)},
    {"secureprocessor.image.config.frame_buffer_stride",
     ((int32_t)
          ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_STRIDE)},
    {"secureprocessor.image.config.frame_buffer_format",
     ((int32_t)
          ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_FORMAT)},
    {"secureprocessor.session.config.num_sensors",
     ((int32_t)ConfigTagAidl::SECURE_PROCESSOR_SESSION_CONFIG_NUM_SENSOR)},
    {"secureprocessor.session.config.usecase_id",
     ((int32_t)
          ConfigTagAidl::SECURE_PROCESSOR_SESSION_CONFIG_USECASE_IDENTIFIER)}};

// reinterpret_cast
template <typename To, typename From>
inline std::shared_ptr<To> reinterpret_pointer_cast(
    std::shared_ptr<From> const &ptr) noexcept
{
    return std::shared_ptr<To>(ptr, reinterpret_cast<To *>(ptr.get()));
}

namespace android
{
#define CALL_OBJECT_METHOD(var, obj, mid)  \
    var = env->CallObjectMethod(obj, mid); \
    LOG_FATAL_IF(!(var), "Unable to call object method: " mid);

#define GET_METHOD_ID(var, clazz, fieldName, fieldDescriptor)  \
    var = env->GetMethodID(clazz, fieldName, fieldDescriptor); \
    LOG_FATAL_IF(!(var), "Unable to find method: " fieldName);

#define GET_FIELD_ID(var, clazz, fieldName, fieldDescriptor)  \
    var = env->GetFieldID(clazz, fieldName, fieldDescriptor); \
    LOG_FATAL_IF(!(var), "Unable to find field: " fieldName);

struct MapFields {
    jmethodID entrySet;
    jmethodID iterator;
    jmethodID hasNext;
    jmethodID next;
    jmethodID getKey;
    jmethodID getValue;
    jmethodID stringval;
    jmethodID intval;
    jobject obj_entryset;
    jobject obj_iter;
};

static MapFields mField;
static jfieldID gContext;
static std::shared_ptr<ISecureProcessorAidl> secureProcessorAidl = nullptr;
bool AIDL_flag = false;
::ndk::SpAIBinder secureProcessorBinder;

void serviceDiedAidl(void *cookie)
{
    UNUSED(cookie);
    ALOGI("secureProcessorAidl just died");
    secureProcessorAidl = nullptr;
}

static sp<JMediaSecureProcessor> getMediaSecureProcessor(JNIEnv *env,
                                                         jobject thiz)
{
    return reinterpret_cast<JMediaSecureProcessor *>(
        env->GetLongField(thiz, gContext));
}

JMediaSecureProcessor::JMediaSecureProcessor(JNIEnv *env, jobject thiz,
                                             const char *service)
{
    jclass clazz = env->GetObjectClass(thiz);
    CHECK(clazz != NULL);

    mClass = (jclass)env->NewGlobalRef(clazz);
    mObject = env->NewWeakGlobalRef(thiz);
    std::string instance = std::string() + ISecureProcessorAidl::descriptor + "/" + service;

    if (AServiceManager_isDeclared(instance.c_str())) {
        AIDL_flag = true;
        MakeSecureProcessorAidl(instance);
        if (secureProcessorAidl == nullptr) {
            ALOGE("Failed to get SecureProcessor AIDL HAL service handle.");
        }
    } else {
        secureProcessorHidl = MakeSecureProcessorHidl(service);
        if (secureProcessorHidl == nullptr) {
            ALOGE("Failed to get SecureProcessor HIDL HAL service handle.");
        }
    }
}

JMediaSecureProcessor::~JMediaSecureProcessor()
{
    if (secureProcessorHidl != nullptr) {
        if (death_recepient != NULL) {
            secureProcessorHidl->unlinkToDeath(death_recepient);
            death_recepient.clear();
        }
        secureProcessorHidl.clear();
    }
    if (secureProcessorAidl != nullptr) {
        auto deathRecipient = ::ndk::ScopedAIBinder_DeathRecipient(
            AIBinder_DeathRecipient_new(&serviceDiedAidl));
        auto status = ::ndk::ScopedAStatus::fromStatus(AIBinder_unlinkToDeath(
            secureProcessorBinder.get(), deathRecipient.get(),
            (void *)serviceDiedAidl));
        if (!status.isOk()) {
            ALOGE("Failed to unlink from death recipient%d: %s",
                  status.getStatus(), status.getMessage());
        }
    }
    JNIEnv *env = AndroidRuntime::getJNIEnv();
    if (env != NULL) {
        env->DeleteWeakGlobalRef(mObject);
        mObject = NULL;
        env->DeleteGlobalRef(mClass);
        mClass = NULL;
    }
}

void JMediaSecureProcessor::MakeSecureProcessorAidl(const std::string instance)
{
    if (secureProcessorAidl == nullptr) {
        // The client can be up before the service, and getService will
        // sometimes return null, using
        // AServiceManager_waitForService(instance.c_str()) instead to wait
        // until the service is up
        secureProcessorBinder =
            ::ndk::SpAIBinder(AServiceManager_waitForService(instance.c_str()));

        if (secureProcessorBinder.get() == nullptr) {
            ALOGE("secureproccessor AIDL service doesn't exist");
        }

        auto deathRecipient = ::ndk::ScopedAIBinder_DeathRecipient(
            AIBinder_DeathRecipient_new(&serviceDiedAidl));
        auto status = ::ndk::ScopedAStatus::fromStatus(AIBinder_linkToDeath(
            secureProcessorBinder.get(), deathRecipient.get(),
            (void *)serviceDiedAidl));

        if (!status.isOk()) {
            ALOGE(
                "linking secureproccessor AIDL service to death failed: %d: %s",
                status.getStatus(), status.getMessage());
        }

        secureProcessorAidl =
            ISecureProcessorAidl::fromBinder(secureProcessorBinder);

        if (secureProcessorAidl == nullptr) {
            ALOGE("secureproccessor AIDL service doesn't exist");
        }

        ALOGD("secureproccessor AIDL getService successful");
    }
}

sp<ISecureProcessorHidl> JMediaSecureProcessor::MakeSecureProcessorHidl(
    const char *service)
{
    sp<ISecureProcessorHidl> secureprocessor =
        ISecureProcessorHidl::getService(service);
    if (secureprocessor == NULL) {
        ALOGE("GetService failed for %s", service);
    }

    death_recepient = new serviceDeathRecipient();
    if (death_recepient == NULL) {
        ALOGE("%s : Failed to register death recepient", __func__);
    } else {
        hardware::Return<bool> linked =
            secureprocessor->linkToDeath(death_recepient, 0);
        if (!linked) {
            ALOGE("%s: Unable to link to secure processor death notification",
                  __func__);
        } else if (!linked.isOk()) {
            ALOGE("%s: Transaction error in linking", __func__);
        }
    }

    return secureprocessor;
}

sp<ISecureProcessorHidl> JMediaSecureProcessor::GetSecureProcessor(JNIEnv *env,
                                                                   jobject obj)
{
    jclass clazz =
        env->FindClass("com/qti/media/secureprocessor/MediaSecureProcessor");
    CHECK(clazz != NULL);

    if (!env->IsInstanceOf(obj, clazz)) {
        ALOGE("Cannot find MediaSecureProcessor instance");
        return NULL;
    }

    sp<JMediaSecureProcessor> jMediaSP = getMediaSecureProcessor(env, obj);
    if (jMediaSP == NULL) {
        ALOGE("Cannot get JMediaSecureProcessor instance");
        return NULL;
    }
    return jMediaSP->secureProcessorHidl;
}

static String8 JStringToString8(JNIEnv *env, jstring const &jstr)
{
    String8 result;

    const char *str = env->GetStringUTFChars(jstr, NULL);
    if (str) {
        result = str;
        env->ReleaseStringUTFChars(jstr, str);
    }
    return result;
}

static SecureProcessorConfigAidl *GetNativeHandleAidl(JNIEnv *env, jobject thiz)
{
    jclass cls = env->FindClass(
        "com/qti/media/secureprocessor/MediaSecureProcessorConfig");
    jmethodID nMethodID = NULL;
    GET_METHOD_ID(nMethodID, cls, "getNativehandle", "()J");
    jlong nHandle = env->CallLongMethod(thiz, nMethodID);
    return reinterpret_cast<SecureProcessorConfigAidl *>(nHandle);
}

static SecureProcessorConfig *GetNativeHandleHidl(JNIEnv *env, jobject thiz)
{
    jclass cls = env->FindClass(
        "com/qti/media/secureprocessor/MediaSecureProcessorConfig");
    jmethodID nMethodID = NULL;
    GET_METHOD_ID(nMethodID, cls, "getNativehandle", "()J");
    jlong nHandle = env->CallLongMethod(thiz, nMethodID);
    return reinterpret_cast<SecureProcessorConfig *>(nHandle);
}

static void PopulateMapFields(JNIEnv *env, jobject map)
{
    jclass clazz = env->GetObjectClass(map);
    CHECK(clazz != NULL);
    GET_METHOD_ID(mField.entrySet, clazz, "entrySet", "()Ljava/util/Set;");

    jclass entryset = env->FindClass("java/util/Set");
    GET_METHOD_ID(mField.iterator, entryset, "iterator",
                  "()Ljava/util/Iterator;");

    jclass iter = env->FindClass("java/util/Iterator");
    GET_METHOD_ID(mField.hasNext, iter, "hasNext", "()Z");
    GET_METHOD_ID(mField.next, iter, "next", "()Ljava/lang/Object;");

    jclass centry = env->FindClass("java/util/Map$Entry");
    GET_METHOD_ID(mField.getKey, centry, "getKey", "()Ljava/lang/Object;");
    GET_METHOD_ID(mField.getValue, centry, "getValue", "()Ljava/lang/Object;");

    jclass cstring = env->FindClass("java/lang/String");
    GET_METHOD_ID(mField.stringval, cstring, "toString",
                  "()Ljava/lang/String;");

    jclass cint = env->FindClass("java/lang/Integer");
    GET_METHOD_ID(mField.intval, cint, "intValue", "()I");

    CALL_OBJECT_METHOD(mField.obj_entryset, map, mField.entrySet);
    CALL_OBJECT_METHOD(mField.obj_iter, mField.obj_entryset, mField.iterator);
}

static jobject PopulateReturnConfig(JNIEnv *env,
                                    SecureProcessorCfgBuf outConfig)
{
    SecureProcessorConfig *outConfigBuf = nullptr;

    jclass cls = env->FindClass(
        "com/qti/media/secureprocessor/MediaSecureProcessorConfig");
    jmethodID constructor = NULL;
    GET_METHOD_ID(constructor, cls, "<init>", "()V");
    jobject obj = env->NewObject(cls, constructor);

    convertFromHidl(&outConfig, &outConfigBuf);
    jlong oHandle = reinterpret_cast<jlong>(outConfigBuf);
    jmethodID nMethodID = NULL;
    GET_METHOD_ID(nMethodID, cls, "setNativehandle", "(J)I");
    jint result = env->CallIntMethod(obj, nMethodID, oHandle);
    if (result < 0) {
        ALOGE("Failed to set the native handle on MediaSecureProcessorConfig");
        return nullptr;
    }
    return obj;
}
static jobject PopulateReturnConfigAidl(JNIEnv *env,
                                        std::vector<uint8_t> outConfig)
{
    jclass cls = env->FindClass(
        "com/qti/media/secureprocessor/MediaSecureProcessorConfig");
    jmethodID constructor = NULL;
    GET_METHOD_ID(constructor, cls, "<init>", "()V");
    jobject obj = env->NewObject(cls, constructor);

    // Convert AIDL config to config wrapper class
    SecureProcessorConfigAidl *outConfigBuf =
        new SecureProcessorConfigAidl((void *)outConfig.data());

    jlong oHandle = reinterpret_cast<jlong>(outConfigBuf);
    jmethodID nMethodID = NULL;
    GET_METHOD_ID(nMethodID, cls, "setNativehandle", "(J)I");
    jint result = env->CallIntMethod(obj, nMethodID, oHandle);
    if (result < 0) {
        ALOGE("Failed to set the native handle on MediaSecureProcessorConfig");
        return nullptr;
    }
    return obj;
}

}  // namespace android

using namespace android;

template <typename T>
static jobject updateConfigEntryBuffer(JNIEnv *env, T buffer)
{
    jclass cls = env->FindClass(
        "com/qti/media/secureprocessor/MediaSecureProcessorConfig$ConfigEntry");

    if (buffer.type == (int32_t)ConfigTypeHidl::INT32) {
        jmethodID constructor = NULL;
        GET_METHOD_ID(constructor, cls, "<init>", "(III[I)V");
        jintArray intVal = env->NewIntArray(buffer.count);
        env->SetIntArrayRegion(intVal, 0, (buffer.count), (buffer.data.i32));
        jobject obj = env->NewObject(cls, constructor, (buffer.tag),
                                     (buffer.type), (buffer.count), intVal);
        return obj;
    } else if (buffer.type == (int32_t)ConfigTypeHidl::BYTE) {
        jmethodID constructor = NULL;
        GET_METHOD_ID(constructor, cls, "<init>", "(III[B)V");
        jbyteArray byteVal = env->NewByteArray(buffer.count);
        env->SetByteArrayRegion(byteVal, 0, (buffer.count),
                                (jbyte *)(buffer.data.u8));
        jobject obj = env->NewObject(cls, constructor, (buffer.tag),
                                     (buffer.type), (buffer.count), byteVal);
        return obj;
    } else if (buffer.type == (int32_t)ConfigTypeHidl::INT64) {
        jmethodID constructor = NULL;
        GET_METHOD_ID(constructor, cls, "<init>", "(III[J)V");
        jlongArray longVal = env->NewLongArray(buffer.count);
        env->SetLongArrayRegion(longVal, 0, (buffer.count), (buffer.data.i64));
        jobject obj = env->NewObject(cls, constructor, (buffer.tag),
                                     (buffer.type), (buffer.count), longVal);
        return obj;
    }

    return nullptr;
}

template <typename T>
static jobject updateConfigEntryBufferAidl(JNIEnv *env, T buffer)
{
    jclass cls = env->FindClass(
        "com/qti/media/secureprocessor/MediaSecureProcessorConfig$ConfigEntry");

    if (buffer.type == (int32_t)ConfigTypeAidl::INT32) {
        jmethodID constructor = NULL;
        GET_METHOD_ID(constructor, cls, "<init>", "(III[I)V");
        jintArray intVal = env->NewIntArray(buffer.count);
        env->SetIntArrayRegion(intVal, 0, (buffer.count), (buffer.data.i32));
        jobject obj = env->NewObject(cls, constructor, (buffer.tag),
                                     (buffer.type), (buffer.count), intVal);
        return obj;
    } else if (buffer.type == (int32_t)ConfigTypeAidl::BYTE) {
        jmethodID constructor = NULL;
        GET_METHOD_ID(constructor, cls, "<init>", "(III[B)V");
        jbyteArray byteVal = env->NewByteArray(buffer.count);
        env->SetByteArrayRegion(byteVal, 0, (buffer.count),
                                (jbyte *)(buffer.data.u8));
        jobject obj = env->NewObject(cls, constructor, (buffer.tag),
                                     (buffer.type), (buffer.count), byteVal);
        return obj;
    } else if (buffer.type == (int32_t)ConfigTypeAidl::INT64) {
        jmethodID constructor = NULL;
        GET_METHOD_ID(constructor, cls, "<init>", "(III[J)V");
        jlongArray longVal = env->NewLongArray(buffer.count);
        env->SetLongArrayRegion(longVal, 0, (buffer.count), (buffer.data.i64));
        jobject obj = env->NewObject(cls, constructor, (buffer.tag),
                                     (buffer.type), (buffer.count), longVal);
        return obj;
    }

    return nullptr;
}

static jlong mediaSecureProcessorConfig_createConfigBuffer(JNIEnv *env,
                                                           jobject thiz,
                                                           jint entryLimit,
                                                           jint dataLimit)
{
    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return -1;
    }
    if (AIDL_flag) {
        SecureProcessorConfigAidl *nConfig =
            new SecureProcessorConfigAidl(entryLimit, dataLimit);
        return reinterpret_cast<jlong>(nConfig);
    } else {
        SecureProcessorConfig *nConfig =
            new SecureProcessorConfig(entryLimit, dataLimit);
        return reinterpret_cast<jlong>(nConfig);
    }
}

static jint mediaSecureProcessorConfig_insertCustomTags(JNIEnv *env,
                                                        jobject thiz,
                                                        jobject map)
{
    int result = -1;
    int custom_offset;

    if (env == NULL || thiz == NULL || map == NULL) {
        ALOGE("Invalid parameters");
        return result;
    }

    if (AIDL_flag) {
        custom_offset =
            (int32_t)ConfigTagAidl::SECURE_PROCESSOR_CUSTOM_CONFIG_START;
    } else {
        custom_offset =
            (int32_t)ConfigTagHidl::SECURE_PROCESSOR_CUSTOM_CONFIG_START;
    }

    PopulateMapFields(env, map);

    bool hasNext =
        (bool)env->CallBooleanMethod(mField.obj_iter, mField.hasNext);

    while (hasNext) {
        jobject entry = NULL;
        CALL_OBJECT_METHOD(entry, mField.obj_iter, mField.next);

        jobject key = NULL;
        CALL_OBJECT_METHOD(key, entry, mField.getKey);
        jobject value = NULL;
        CALL_OBJECT_METHOD(value, entry, mField.getValue);

        jstring jstrKey = (jstring)env->CallObjectMethod(key, mField.stringval);

        const char *strKey = env->GetStringUTFChars(jstrKey, 0);
        int Keyvalue = env->CallIntMethod(value, mField.intval);

        if (Keyvalue < custom_offset) {
            ALOGE("Invalid tag value");
            return result;
        }

        MediaSecureProcessorConfigMap.insert({strKey, Keyvalue});

        hasNext = (bool)env->CallBooleanMethod(mField.obj_iter, mField.hasNext);
    }

    return 0;
}

static jint mediaSecureProcessorConfig_clear(JNIEnv *env, jobject thiz)
{
    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return -1;
    }
    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present");
            return -1;
        }

        configBufAidl->clear();
        return 0;
    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present");
            return -1;
        }

        configBufHidl->clear();
        return 0;
    }
}

static jboolean mediaSecureProcessorConfig_isEmpty(JNIEnv *env, jobject thiz)
{
    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return JNI_FALSE;
    }
    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present");
            return JNI_FALSE;
        }
        return configBufAidl->isEmpty();
    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present");
            return JNI_FALSE;
        }
        return configBufHidl->isEmpty();
    }
}

static jint mediaSecureProcessorConfig_getSize(JNIEnv *env, jobject thiz)
{
    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return 0;
    }
    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present");
            return 0;
        }

        return configBufAidl->getSize();
    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present");
            return 0;
        }

        return configBufHidl->getSize();
    }
}

static jboolean mediaSecureProcessorConfig_checkEntry(JNIEnv *env, jobject thiz,
                                                      jstring jtag)
{
    bool result = JNI_FALSE;
    std::map<std::string, int32_t>::iterator ptr;

    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return result;
    }

    std::string tag = (JStringToString8(env, jtag)).c_str();
    ptr = MediaSecureProcessorConfigMap.find(tag);
    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present to check entry");
            return result;
        }

        if (ptr != MediaSecureProcessorConfigMap.end()) {
            auto buffer = configBufAidl->checkEntry(ptr->second);
            if (buffer) {
                result = JNI_TRUE;
            }
        } else {
            ALOGE("Invalid Tag: %s", tag.c_str());
        }
        return result;
    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present to check entry");
            return result;
        }

        if (ptr != MediaSecureProcessorConfigMap.end()) {
            auto buffer = configBufHidl->checkEntry(ptr->second);
            if (buffer) {
                result = JNI_TRUE;
            }
        } else {
            ALOGE("Invalid Tag: %s", tag.c_str());
        }
        return result;
    }
}

static jint mediaSecureProcessorConfig_getEntryCount(JNIEnv *env, jobject thiz)
{
    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return 0;
    }

    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present to get entry count");
            return 0;
        }
        return configBufAidl->entryCount();
    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present to get entry count");
            return 0;
        }
        return configBufHidl->entryCount();
    }
}

static jint mediaSecureProcessorConfig_addEntry__ILjava_lang_String_2(
    JNIEnv *env, jobject thiz, jstring jtag, jstring jdata)
{
    status_t result = -1;
    std::map<std::string, int32_t>::iterator ptr;

    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return result;
    }

    std::string tag = (JStringToString8(env, jtag)).c_str();
    String8 data = JStringToString8(env, jdata);
    ptr = MediaSecureProcessorConfigMap.find(tag);

    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present to add entry");
            return result;
        }
        if (ptr != MediaSecureProcessorConfigMap.end()) {
            result = configBufAidl->addEntry(ptr->second, data);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }
    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present to add entry");
            return result;
        }
        if (ptr != MediaSecureProcessorConfigMap.end()) {
            result = configBufHidl->addEntry(ptr->second, data);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }
    }
    return result;
}

static jint mediaSecureProcessorConfig_addEntry__I_3II(
    JNIEnv *env, jobject thiz, jstring jtag, jintArray jdata, jint jdataCount)
{
    status_t result = -1;
    std::map<std::string, int32_t>::iterator ptr;
    int32_t *data = NULL;

    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return result;
    }

    std::string tag = (JStringToString8(env, jtag)).c_str();
    data = env->GetIntArrayElements(jdata, NULL);
    ptr = MediaSecureProcessorConfigMap.find(tag);
    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present to add entry");
            return result;
        }

        if (ptr != MediaSecureProcessorConfigMap.end()) {
            result = configBufAidl->addEntry(ptr->second, data, jdataCount);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }
    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present to add entry");
            return result;
        }

        ptr = MediaSecureProcessorConfigMap.find(tag);
        if (ptr != MediaSecureProcessorConfigMap.end()) {
            result = configBufHidl->addEntry(ptr->second, data, jdataCount);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }
    }
    return result;
}

static jint mediaSecureProcessorConfig_addEntry__I_3BI(
    JNIEnv *env, jobject thiz, jstring jtag, jbyteArray jdata, jint jdataCount)
{
    status_t result = -1;
    uint8_t *data = NULL;
    jboolean isCopy = JNI_FALSE;
    std::map<std::string, int32_t>::iterator ptr;

    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return result;
    }

    std::string tag = (JStringToString8(env, jtag)).c_str();
    jbyte *const tmpData = env->GetByteArrayElements(jdata, &isCopy);
    data = reinterpret_cast<uint8_t *>(tmpData);

    ptr = MediaSecureProcessorConfigMap.find(tag);

    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present to add entry");
            return result;
        }

        if (ptr != MediaSecureProcessorConfigMap.end()) {
            result = configBufAidl->addEntry(ptr->second, data, jdataCount);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }

    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present to add entry");
            return result;
        }

        if (ptr != MediaSecureProcessorConfigMap.end()) {
            result = configBufHidl->addEntry(ptr->second, data, jdataCount);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }
    }
    return result;
}

static jint mediaSecureProcessorConfig_addEntry__I_3JI(
    JNIEnv *env, jobject thiz, jstring jtag, jlongArray jdata, jint jdataCount)
{
    status_t result = -1;
    int64_t *data = NULL;
    std::map<std::string, int32_t>::iterator ptr;

    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return result;
    }

    std::string tag = (JStringToString8(env, jtag)).c_str();
    data = reinterpret_cast<int64_t *>(env->GetLongArrayElements(jdata, 0));

    ptr = MediaSecureProcessorConfigMap.find(tag);

    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE("No configuration backing buffer present to add entry");
            return result;
        }

        if (ptr != MediaSecureProcessorConfigMap.end()) {
            result = configBufAidl->addEntry(ptr->second, data, jdataCount);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }

    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE("No configuration backing buffer present to add entry");
            return result;
        }
        if (ptr != MediaSecureProcessorConfigMap.end()) {
            result = configBufHidl->addEntry(ptr->second, data, jdataCount);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }
    }
    return result;
}

static jobject mediaSecureProcessorConfig_getEntry(JNIEnv *env, jobject thiz,
                                                   jstring jtag)
{
    std::map<std::string, int32_t>::iterator ptr;

    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return NULL;
    }

    std::string tag = (JStringToString8(env, jtag)).c_str();
    ptr = MediaSecureProcessorConfigMap.find(tag);

    if (AIDL_flag) {
        SecureProcessorConfigAidl *configBufAidl =
            GetNativeHandleAidl(env, thiz);
        if (configBufAidl == NULL) {
            ALOGE(
                "No configuration backing buffer present to get entry "
                "mediaSecureProcessorConfig_getEntry");
            return NULL;
        }

        if (ptr != MediaSecureProcessorConfigMap.end()) {
            auto buffer = configBufAidl->getEntry(ptr->second);
            return updateConfigEntryBuffer(env, buffer);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }

    } else {
        SecureProcessorConfig *configBufHidl = GetNativeHandleHidl(env, thiz);
        if (configBufHidl == NULL) {
            ALOGE(
                "No configuration backing buffer present to get entry "
                "mediaSecureProcessorConfig_getEntry");
            return NULL;
        }

        if (ptr != MediaSecureProcessorConfigMap.end()) {
            auto buffer = configBufHidl->getEntry(ptr->second);
            return updateConfigEntryBuffer(env, buffer);
        } else {
            ALOGE("Not a valid Tag value: %s", tag.c_str());
        }
    }
    return NULL;
}

static jobject mediaSecureProcessorConfig_getEntryByIndex(JNIEnv *env,
                                                          jobject thiz,
                                                          jint index)
{
    if (env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return NULL;
    }

    if (index >= 0) {
        if (AIDL_flag) {
            SecureProcessorConfigAidl *configBufAidl =
                GetNativeHandleAidl(env, thiz);
            if (configBufAidl == NULL) {
                ALOGE(
                    "No configuration backing buffer present to get entry "
                    "mediaSecureProcessorConfig_getEntryByIndex");
                return NULL;
            }

            auto entry = configBufAidl->getEntryByIndex(index);
            return updateConfigEntryBuffer(env, entry);
        } else {
            SecureProcessorConfig *configBufHidl =
                GetNativeHandleHidl(env, thiz);
            if (configBufHidl == NULL) {
                ALOGE(
                    "No configuration backing buffer present to get entry "
                    "mediaSecureProcessorConfig_getEntryByIndex");
                return NULL;
            }

            auto entry = configBufHidl->getEntryByIndex(index);
            return updateConfigEntryBuffer(env, entry);
        }
    }
    return NULL;
}

static sp<JMediaSecureProcessor> setMediaSecureProcessor(
    JNIEnv *env, jobject thiz, const sp<JMediaSecureProcessor> &jMediaSP)
{
    sp<JMediaSecureProcessor> old = reinterpret_cast<JMediaSecureProcessor *>(
        env->GetLongField(thiz, gContext));
    if (jMediaSP != NULL) {
        jMediaSP->incStrong(thiz);
    }

    if (old != NULL) {
        old->decStrong(thiz);
    }

    env->SetLongField(thiz, gContext, reinterpret_cast<jlong>(jMediaSP.get()));
    return old;
}

static void mediaSecureProcessor_native_release(JNIEnv *env, jobject thiz)
{
    setMediaSecureProcessor(env, thiz, NULL);
}

static void mediaSecureProcessor_native_init(JNIEnv *env)
{
    jclass clazz =
        env->FindClass("com/qti/media/secureprocessor/MediaSecureProcessor");
    CHECK(clazz != NULL);
    GET_FIELD_ID(gContext, clazz, "mNativeContext", "J");
}

static void mediaSecureProcessor_native_setup(JNIEnv *env, jobject thiz,
                                              jstring jservice)
{
    std::string tmp = (JStringToString8(env, jservice)).c_str();
    const char *service = tmp.c_str();
    sp<JMediaSecureProcessor> jMediaSP =
        new JMediaSecureProcessor(env, thiz, service);
    if (jMediaSP == NULL) {
        ALOGE("Failed to instantiate JMediaSecureProcessor object.");
        return;
    }

    setMediaSecureProcessor(env, thiz, jMediaSP);
}

static jint mediaSecureProcessor_createSession(JNIEnv *env, jobject thiz)
{
    if(env == NULL || thiz == NULL) {
        ALOGE("Invalid parameters");
        return 0;
    }

    if (AIDL_flag) {
        return JMediaSecureProcessor::mediaSecureProcessor_createSession_AIDL();
    } else {
        return JMediaSecureProcessor::mediaSecureProcessor_createSession_HIDL(
            env, thiz);
    }
}

int JMediaSecureProcessor::mediaSecureProcessor_createSession_AIDL()
{
    sessionIDout sessionId;
    ErrorCodeAidl retCode = ErrorCodeAidl::SECURE_PROCESSOR_OK;
    auto err = secureProcessorAidl->createSession(&sessionId, &retCode);
    int sessID = sessionId.sID;

    if (!err.isOk() || retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
        String8 msg("createSession failed");
        if (retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
            msg += ": retCode.";
        } else {
            msg.appendFormat(": general failure (retCode = %d)", retCode);
        }
        ALOGE("%s", msg.c_str());
        return 0;
    }

    ALOGD("%s: session created with sessionId = %d", __func__, sessID);
    return sessID;
}

uint32_t JMediaSecureProcessor::mediaSecureProcessor_createSession_HIDL(
    JNIEnv *env, jobject thiz)
{
    sp<ISecureProcessorHidl> secureprocessor =
        JMediaSecureProcessor::GetSecureProcessor(env, thiz);
    if (secureprocessor == NULL) {
        ALOGE("%s: failed to get secureproccessor", __func__);
        return 0;
    }

    uint32_t sessionId = 0;
    ErrorCodeHidl retCode = ErrorCodeHidl::SECURE_PROCESSOR_OK;
    auto err = secureprocessor->createSession(
        [&retCode, &sessionId](ErrorCodeHidl code, uint32_t sid) {
            retCode = code;
            sessionId = sid;
        });

    if (!err.isOk() || retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
        String8 msg("createSession failed");
        if (retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
            msg += ": retCode.";
        } else {
            msg.appendFormat(": general failure (retCode = %d)", retCode);
        }
        ALOGE("%s", msg.c_str());
        return 0;
    }

    ALOGD("%s: session created with sessionId = %d", __func__, sessionId);
    return sessionId;
}

static jobject mediaSecureProcessor_getConfig(JNIEnv *env, jobject thiz,
                                              jint jsessionId,
                                              jobject jSessionConfig)
{
    if(env == NULL || thiz == NULL || jSessionConfig == NULL
        || jsessionId == 0) {
        ALOGE("Invalid parameters");
        return NULL;
    }

    if (AIDL_flag) {
        return JMediaSecureProcessor::mediaSecureProcessor_getConfig_AIDL(
            env, jsessionId, jSessionConfig);
    } else {
        return JMediaSecureProcessor::mediaSecureProcessor_getConfig_HIDL(
            env, thiz, jsessionId, jSessionConfig);
    }
}

jobject JMediaSecureProcessor::mediaSecureProcessor_getConfig_AIDL(
    JNIEnv *env, jint jsessionId, jobject jSessionConfig)
{
    ErrorCodeAidl retCode = ErrorCodeAidl::SECURE_PROCESSOR_OK;

    std::vector<uint8_t> outConfig;

    SecureProcessorConfigAidl *inconfigBuf =
        GetNativeHandleAidl(env, jSessionConfig);

    uint32_t size = inconfigBuf->getSize();
    const uint8_t *dummy = (uint8_t *)inconfigBuf->releaseAndGetBuffer();
    std::vector<uint8_t> inConfig(dummy, dummy + size);

    auto err = secureProcessorAidl->getConfig(jsessionId, inConfig, &outConfig,
                                              &retCode);

    if (!err.isOk() || retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
        String8 msg("getConfig failed");
        if (retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
            msg += ": retCode.";
        } else {
            msg.appendFormat(": general failure (retCode = %d)", retCode);
        }
        ALOGE("%s", msg.c_str());
    } else {
        jobject obj = PopulateReturnConfigAidl(env, outConfig);
        return obj;
    }

    return NULL;
}

jobject JMediaSecureProcessor::mediaSecureProcessor_getConfig_HIDL(
    JNIEnv *env, jobject thiz, jint jsessionId, jobject jSessionConfig)
{
    ErrorCodeHidl retCode = ErrorCodeHidl::SECURE_PROCESSOR_OK;
    SecureProcessorCfgBuf inConfig;
    SecureProcessorCfgBuf outConfig;

    sp<ISecureProcessorHidl> secureprocessor =
        JMediaSecureProcessor::GetSecureProcessor(env, thiz);
    if (secureprocessor == NULL) {
        return NULL;
    }

    SecureProcessorConfig *inconfigBuf =
        GetNativeHandleHidl(env, jSessionConfig);
    convertToHidl(inconfigBuf, &inConfig);

    auto err = secureprocessor->getConfig(
        jsessionId, inConfig,
        [&retCode, &outConfig](ErrorCodeHidl code,
                               SecureProcessorCfgBuf outCfg) {
            retCode = code;
            outConfig = outCfg;
        });

    if (!err.isOk() || retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
        String8 msg("getConfig failed");
        if (retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
            msg += ": retCode.";
        } else {
            msg.appendFormat(": general failure (retCode = %d)", retCode);
        }
        ALOGE("%s", msg.c_str());
    } else {
        jobject obj = PopulateReturnConfig(env, outConfig);
        return obj;
    }

    return NULL;
}

static jint mediaSecureProcessor_setConfig(JNIEnv *env, jobject thiz,
                                           jint jsessionId,
                                           jobject jSessionConfig)
{
    if(env == NULL || thiz == NULL || jSessionConfig == NULL
        || jsessionId == 0) {
        ALOGE("Invalid parameters");
        return -1;
    }

    if (AIDL_flag) {
        return JMediaSecureProcessor::mediaSecureProcessor_setConfig_AIDL(
            env, jsessionId, jSessionConfig);
    } else {
        return JMediaSecureProcessor::mediaSecureProcessor_setConfig_HIDL(
            env, thiz, jsessionId, jSessionConfig);
    }
}
jint JMediaSecureProcessor::mediaSecureProcessor_setConfig_HIDL(
    JNIEnv *env, jobject thiz, jint jsessionId, jobject jSessionConfig)
{
    sp<ISecureProcessorHidl> secureprocessor =
        JMediaSecureProcessor::GetSecureProcessor(env, thiz);
    if (secureprocessor == NULL) {
        return -1;
    }

    SecureProcessorConfig *configBuf = GetNativeHandleHidl(env, jSessionConfig);
    SecureProcessorCfgBuf inConfig;

    convertToHidl(configBuf, &inConfig);

    ErrorCodeHidl retCode = secureprocessor->setConfig(jsessionId, inConfig);

    if (retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
        ALOGE("setConfig failed");
        return -1;
    }

    return 0;
}
jint JMediaSecureProcessor::mediaSecureProcessor_setConfig_AIDL(
    JNIEnv *env, jint jsessionId, jobject jSessionConfig)
{
    SecureProcessorConfigAidl *configBuf =
        GetNativeHandleAidl(env, jSessionConfig);
    if (configBuf == nullptr) {
    }
    uint32_t size = configBuf->getSize();
    const uint8_t *dummy = (uint8_t *)configBuf->releaseAndGetBuffer();
    std::vector<uint8_t> inConfig(dummy, dummy + size);

    ErrorCodeAidl retCode = ErrorCodeAidl::SECURE_PROCESSOR_OK;
    auto err = secureProcessorAidl->setConfig(jsessionId, inConfig, &retCode);
    if (!err.isOk() || retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
        ALOGE("setConfig failed");
        return -1;
    }
    return 0;
}

static jint mediaSecureProcessor_startSession(JNIEnv *env, jobject thiz,
                                              jint jsessionId)
{
    if(env == NULL || thiz == NULL || jsessionId == 0) {
        ALOGE("Invalid parameters");
        return -1;
    }

    if (AIDL_flag) {
        return JMediaSecureProcessor::mediaSecureProcessor_startSession_AIDL(
            jsessionId);
    } else {
        return JMediaSecureProcessor::mediaSecureProcessor_startSession_HIDL(
            env, thiz, jsessionId);
    }
}
jint JMediaSecureProcessor::mediaSecureProcessor_startSession_AIDL(
    jint jsessionId)
{
    ErrorCodeAidl retCode = ErrorCodeAidl::SECURE_PROCESSOR_OK;
    auto err = secureProcessorAidl->startSession(jsessionId, &retCode);

    if (!err.isOk() || retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
        String8 msg("startSession failed");
        if (retCode == ErrorCodeAidl::SECURE_PROCESSOR_FAIL) {
            msg += ": session not found";
        } else {
            msg.appendFormat(": general failure (%d)", retCode);
        }
        ALOGE("%s", msg.c_str());
        return -1;
    }
    return 0;
}
jint JMediaSecureProcessor::mediaSecureProcessor_startSession_HIDL(
    JNIEnv *env, jobject thiz, jint jsessionId)
{

    sp<ISecureProcessorHidl> secureprocessor =
        JMediaSecureProcessor::GetSecureProcessor(env, thiz);
    if (secureprocessor == NULL) {
        return -1;
    }

    ErrorCodeHidl retCode = secureprocessor->startSession(jsessionId);
    if (retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
        String8 msg("startSession failed");
        if (retCode == ErrorCodeHidl::SECURE_PROCESSOR_FAIL) {
            msg += ": session not found";
        } else {
            msg.appendFormat(": general failure (%d)", retCode);
        }
        ALOGE("%s", msg.c_str());
        return -1;
    }

    return 0;
}

static status_t _populateImageBufferConfig(AHardwareBuffer *hwBuf,
                                           SecureProcessorConfig *cfg)
{
    if (!hwBuf || !cfg) {
        return BAD_VALUE;
    }

    AHardwareBuffer_Desc bufDesc;
    AHardwareBuffer_describe(hwBuf, &bufDesc);

    // Set frame buffer width
    uint32_t tag = (uint32_t)
        ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_WIDTH;
    int32_t width = bufDesc.width;
    cfg->addEntry(tag, &width, 1);

    // Set frame buffer height
    tag = (uint32_t)
        ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_HEIGHT;
    int32_t height = bufDesc.height;
    cfg->addEntry(tag, &height, 1);

    // Set frame buffer stride
    tag = (uint32_t)
        ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_STRIDE;
    int32_t stride = bufDesc.stride;
    cfg->addEntry(tag, &stride, 1);

    // Set frame buffer height
    tag = (uint32_t)
        ConfigTagHidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_FORMAT;
    int32_t format = bufDesc.format;
    cfg->addEntry(tag, &format, 1);

    return OK;
}
static status_t _populateImageBufferConfigAidl(AHardwareBuffer *hwBuf,
                                               SecureProcessorConfigAidl *cfg)
{
    if (!hwBuf || !cfg) {
        return BAD_VALUE;
    }

    AHardwareBuffer_Desc bufDesc;
    AHardwareBuffer_describe(hwBuf, &bufDesc);

    // Set frame buffer width
    uint32_t tag = (uint32_t)
        ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_WIDTH;
    int32_t width = bufDesc.width;
    cfg->addEntry(tag, &width, 1);

    // Set frame buffer height
    tag = (uint32_t)
        ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_HEIGHT;
    int32_t height = bufDesc.height;
    cfg->addEntry(tag, &height, 1);

    // Set frame buffer stride
    tag = (uint32_t)
        ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_STRIDE;
    int32_t stride = bufDesc.stride;
    cfg->addEntry(tag, &stride, 1);

    // Set frame buffer height
    tag = (uint32_t)
        ConfigTagAidl::SECURE_PROCESSOR_IMAGE_CONFIG_FRAME_BUFFER_FORMAT;
    int32_t format = bufDesc.format;
    cfg->addEntry(tag, &format, 1);

    return OK;
}

static jobject mediaSecureProcessor_processImage(JNIEnv *env, jobject thiz,
                                                 jint jsessionId,
                                                 jobject jhwBuffer,
                                                 jobject jImageConfig)
{
    if(env == NULL || thiz == NULL || jhwBuffer == NULL ||
        jImageConfig == NULL || jsessionId == 0) {
        ALOGE("Invalid parameters");
        return NULL;
    }

    if (AIDL_flag) {
        return JMediaSecureProcessor::mediaSecureProcessor_processImage_AIDL(
            env, jsessionId, jhwBuffer, jImageConfig);
    } else {
        return JMediaSecureProcessor::mediaSecureProcessor_processImage_HIDL(
            env, thiz, jsessionId, jhwBuffer, jImageConfig);
    }
}
jobject JMediaSecureProcessor::mediaSecureProcessor_processImage_AIDL(
    JNIEnv *env, jint jsessionId, jobject jhwBuffer,
    jobject jImageConfig)
{
    ErrorCodeAidl retCode = ErrorCodeAidl::SECURE_PROCESSOR_OK;
    std::vector<uint8_t> AIDLOutCfg;

    SecureProcessorConfigAidl *configBuf =
        GetNativeHandleAidl(env, jImageConfig);

    // Convert HardwareBuffer jobject to AHardwareBuffer
    AHardwareBuffer *hwBuf =
        android_hardware_HardwareBuffer_getNativeHardwareBuffer(env, jhwBuffer);

    // Convert AHardwareBuffer to nativeHandle
    const native_handle_t *nativeHandle =
        AHardwareBuffer_getNativeHandle(hwBuf);

    // Add mandatory Image Frame Buffer configs
    status_t status = _populateImageBufferConfigAidl(hwBuf, configBuf);
    if (status != OK) {
        ALOGE("failed to populate mandatory Image Buffer config");
        return NULL;
    }

    // Convert to AIDL vector
    uint32_t size = configBuf->getSize();
    const uint8_t *dummy = (uint8_t *)configBuf->releaseAndGetBuffer();
    std::vector<uint8_t> AIDLInCfg(dummy, dummy + size);

    // Call the AIDL API
    auto ret = secureProcessorAidl->processImage(
        jsessionId, dupToAidl(nativeHandle), AIDLInCfg, &AIDLOutCfg, &retCode);

    if (!ret.isOk() || retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
        String8 msg("processImage failed");
        if (retCode == ErrorCodeAidl::SECURE_PROCESSOR_FAIL) {
            msg += ": retCode";
        } else {
            msg.appendFormat(": general failure (retCode = %d)", retCode);
        }
        ALOGE("%s", msg.c_str());
    } else {
        jobject obj = PopulateReturnConfigAidl(env, AIDLOutCfg);
        return obj;
    }
    // Delete configBuf object
    delete configBuf;
    return nullptr;
}
jobject JMediaSecureProcessor::mediaSecureProcessor_processImage_HIDL(
    JNIEnv *env, jobject thiz, jint jsessionId, jobject jhwBuffer,
    jobject jImageConfig)
{
    ErrorCodeHidl retCode = ErrorCodeHidl::SECURE_PROCESSOR_OK;
    SecureProcessorCfgBuf HIDLInCfg;
    SecureProcessorCfgBuf HIDLOutCfg;

    sp<ISecureProcessorHidl> secureprocessor =
        JMediaSecureProcessor::GetSecureProcessor(env, thiz);
    if (secureprocessor == NULL) {
        return NULL;
    }

    SecureProcessorConfig *configBuf = GetNativeHandleHidl(env, jImageConfig);

    // Convert HardwareBuffer jobject to AHardwareBuffer
    AHardwareBuffer *hwBuf =
        android_hardware_HardwareBuffer_getNativeHardwareBuffer(env, jhwBuffer);

    // Convert AHardwareBuffer to nativeHandle
    const native_handle_t *nativeHandle =
        AHardwareBuffer_getNativeHandle(hwBuf);

    // Add mandatory Image Frame Buffer configs
    status_t status = _populateImageBufferConfig(hwBuf, configBuf);
    if (status != OK) {
        ALOGE("failed to populate mandatory Image Buffer config");
        return NULL;
    }

    // Convert to HIDL vector
    convertToHidl(configBuf, &HIDLInCfg);

    // Call the HIDL API
    auto ret = secureprocessor->processImage(
        jsessionId, nativeHandle, HIDLInCfg,
        [&retCode, &HIDLOutCfg](ErrorCodeHidl _status,
                                SecureProcessorCfgBuf HIDLOutCfg_) {
            retCode = _status;
            if (retCode == ErrorCodeHidl::SECURE_PROCESSOR_OK) {
                HIDLOutCfg = HIDLOutCfg_;
            }
        });
    if (!ret.isOk() || retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
        String8 msg("processImage failed");
        if (retCode == ErrorCodeHidl::SECURE_PROCESSOR_FAIL) {
            msg += ": retCode";
        } else {
            msg.appendFormat(": general failure (retCode = %d)", retCode);
        }
        ALOGE("%s", msg.c_str());
    } else {
        jobject obj = PopulateReturnConfig(env, HIDLOutCfg);
        return obj;
    }

    // Delete configBuf object
    delete configBuf;

    return nullptr;
}

static jint mediaSecureProcessor_stopSession(JNIEnv *env, jobject thiz,
                                             jint jsessionId)
{
    if(env == NULL || thiz == NULL || jsessionId == 0) {
        ALOGE("Invalid parameters");
        return -1;
    }

    if (AIDL_flag) {
        return JMediaSecureProcessor::mediaSecureProcessor_stopSession_AIDL(
            jsessionId);
    } else {
        return JMediaSecureProcessor::mediaSecureProcessor_stopSession_HIDL(
            env, thiz, jsessionId);
    }
}
jint JMediaSecureProcessor::mediaSecureProcessor_stopSession_AIDL(
    jint jsessionId)
{
    ErrorCodeAidl retCode = ErrorCodeAidl::SECURE_PROCESSOR_OK;
    auto err = secureProcessorAidl->stopSession(jsessionId, &retCode);

    if (!err.isOk() || retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
        String8 msg("stopSession failed");
        if (retCode == ErrorCodeAidl::SECURE_PROCESSOR_FAIL) {
            msg += ": session not found";
        } else {
            msg.appendFormat(": general failure (%d)", retCode);
        }
        ALOGE("%s", msg.c_str());
        return -1;
    }
    return 0;
}
jint JMediaSecureProcessor::mediaSecureProcessor_stopSession_HIDL(
    JNIEnv *env, jobject thiz, jint jsessionId)
{
    sp<ISecureProcessorHidl> secureprocessor =
        JMediaSecureProcessor::GetSecureProcessor(env, thiz);
    if (secureprocessor == NULL) {
        return -1;
    }

    ErrorCodeHidl retCode = secureprocessor->stopSession(jsessionId);
    if (retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
        String8 msg("stopSession failed");
        if (retCode == ErrorCodeHidl::SECURE_PROCESSOR_FAIL) {
            msg += ": session not found";
        } else {
            msg.appendFormat(": general failure (%d)", retCode);
        }
        ALOGE("%s", msg.c_str());
        return -1;
    }

    return 0;
}

static jint mediaSecureProcessor_deleteSession(JNIEnv *env, jobject thiz,
                                               jint jsessionId)
{
    if(env == NULL || thiz == NULL || jsessionId == 0) {
        ALOGE("Invalid parameters");
        return -1;
    }

    if (AIDL_flag) {
        return JMediaSecureProcessor::mediaSecureProcessor_deleteSession_AIDL(
            jsessionId);
    } else {
        return JMediaSecureProcessor::mediaSecureProcessor_deleteSession_HIDL(
            env, thiz, jsessionId);
    }
}
jint JMediaSecureProcessor::mediaSecureProcessor_deleteSession_AIDL(
    jint jsessionId)
{
    ErrorCodeAidl retCode = ErrorCodeAidl::SECURE_PROCESSOR_OK;
    auto err = secureProcessorAidl->deleteSession(jsessionId, &retCode);

    if (!err.isOk() || retCode != ErrorCodeAidl::SECURE_PROCESSOR_OK) {
        String8 msg("deleteSession failed");
        if (retCode == ErrorCodeAidl::SECURE_PROCESSOR_FAIL) {
            msg += ": session not found ";
        } else {
            msg.appendFormat(": general failure (%d)", retCode);
        }
        ALOGE("%s", msg.c_str());
        return -1;
    }
    return 0;
}
jint JMediaSecureProcessor::mediaSecureProcessor_deleteSession_HIDL(
    JNIEnv *env, jobject thiz, jint jsessionId)
{
    sp<ISecureProcessorHidl> secureprocessor =
        JMediaSecureProcessor::GetSecureProcessor(env, thiz);
    if (secureprocessor == NULL) {
        return -1;
    }

    ErrorCodeHidl retCode = secureprocessor->deleteSession(jsessionId);
    if (retCode != ErrorCodeHidl::SECURE_PROCESSOR_OK) {
        String8 msg("deleteSession failed");
        if (retCode == ErrorCodeHidl::SECURE_PROCESSOR_FAIL) {
            msg += ": session not found ";
        } else {
            msg.appendFormat(": general failure (%d)", retCode);
        }
        ALOGE("%s", msg.c_str());
        return -1;
    }

    return 0;
}
static const JNINativeMethod gMethods[] = {
    {"native_release", "()V", (void *)mediaSecureProcessor_native_release},
    {"native_init", "()V", (void *)mediaSecureProcessor_native_init},

    {"native_setup", "(Ljava/lang/String;)V",
     (void *)mediaSecureProcessor_native_setup},

    {"createSession", "()I", (void *)mediaSecureProcessor_createSession},

    {"getConfig",
     "(ILcom/qti/media/secureprocessor/MediaSecureProcessorConfig;)Lcom/qti/"
     "media/secureprocessor/MediaSecureProcessorConfig;",
     (void *)mediaSecureProcessor_getConfig},

    {"setConfig",
     "(ILcom/qti/media/secureprocessor/MediaSecureProcessorConfig;)I",
     (void *)mediaSecureProcessor_setConfig},

    {"startSession", "(I)I", (void *)mediaSecureProcessor_startSession},

    {"processImage",
     "(ILandroid/hardware/HardwareBuffer;Lcom/qti/media/secureprocessor/"
     "MediaSecureProcessorConfig;)Lcom/qti/media/secureprocessor/"
     "MediaSecureProcessorConfig;",
     (void *)mediaSecureProcessor_processImage},

    {"stopSession", "(I)I", (void *)mediaSecureProcessor_stopSession},

    {"deleteSession", "(I)I", (void *)mediaSecureProcessor_deleteSession},
};

static const JNINativeMethod cMethods[] = {
    {"createConfigBuffer", "(II)J",
     (void *)mediaSecureProcessorConfig_createConfigBuffer},

    {"addEntry", "(Ljava/lang/String;Ljava/lang/String;)I",
     (void *)mediaSecureProcessorConfig_addEntry__ILjava_lang_String_2},

    {"addEntry", "(Ljava/lang/String;[II)I",
     (void *)mediaSecureProcessorConfig_addEntry__I_3II},

    {"addEntry", "(Ljava/lang/String;[BI)I",
     (void *)mediaSecureProcessorConfig_addEntry__I_3BI},

    {"addEntry", "(Ljava/lang/String;[JI)I",
     (void *)mediaSecureProcessorConfig_addEntry__I_3JI},

    {"getEntry",
     "(Ljava/lang/String;)Lcom/qti/media/secureprocessor/"
     "MediaSecureProcessorConfig$ConfigEntry;",
     (void *)mediaSecureProcessorConfig_getEntry},

    {"insertCustomTags", "(Ljava/util/Map;)I",
     (void *)mediaSecureProcessorConfig_insertCustomTags},

    {"getEntryCount", "()I", (void *)mediaSecureProcessorConfig_getEntryCount},

    {"getEntryByIndex",
     "(I)Lcom/qti/media/secureprocessor/"
     "MediaSecureProcessorConfig$ConfigEntry;",
     (void *)mediaSecureProcessorConfig_getEntryByIndex},

    {"clear", "()I", (void *)mediaSecureProcessorConfig_clear},

    {"isEmpty", "()Z", (void *)mediaSecureProcessorConfig_isEmpty},

    {"checkEntry", "(Ljava/lang/String;)Z",
     (void *)mediaSecureProcessorConfig_checkEntry},

    {"getSize", "()I", (void *)mediaSecureProcessorConfig_getSize},
};

int register_mediaSecureProcessor(JNIEnv *env)
{
    return AndroidRuntime::registerNativeMethods(
        env, "com/qti/media/secureprocessor/MediaSecureProcessor", gMethods,
        NELEM(gMethods));
}
int register_mediaSecureProcessorConfig(JNIEnv *env)
{
    return AndroidRuntime::registerNativeMethods(
        env, "com/qti/media/secureprocessor/MediaSecureProcessorConfig",
        cMethods, NELEM(cMethods));
}

jint JNI_OnLoad(JavaVM *vm, void * /* reserved */)
{
    JNIEnv *env = NULL;
    jint result = -1;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("Failed to GetEnv\n");
        goto exit;
    }

    if (env == NULL) goto exit;

    if (register_mediaSecureProcessor(env) < 0) {
        ALOGE("ERROR: MediaSecureProcessor native registration failed");
        goto exit;
    }

    if (register_mediaSecureProcessorConfig(env) < 0) {
        ALOGE("ERROR: MediaSecureProcessorConfig native registration failed");
        goto exit;
    }

    result = JNI_VERSION_1_4;

exit:
    return result;
}
