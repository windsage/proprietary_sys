/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#include <android/binder_auto_utils.h>
#include <android/binder_ibinder_jni.h>
#include <android/binder_manager.h>
#include <jni.h>
#include <log/log.h>

extern "C" JNIEXPORT jobject JNICALL
  Java_com_qualcomm_qti_servicelib_ServiceLib_nativeWaitForService__Ljava_lang_String_2(JNIEnv* env, jclass /**/, jstring str) {
    ALOGI("%s", __func__);

    const char* name = env->GetStringUTFChars(str, nullptr);

    ALOGI("nativeWaitForService name: %s", name);

    jobject jbinder = nullptr;

    if (AServiceManager_isDeclared(name)) {
        ndk::SpAIBinder binder = ndk::SpAIBinder(AServiceManager_waitForService(name));
        jbinder = AIBinder_toJavaBinder(env, binder.get());
    } else {
        ALOGI("Service not declared");
    }

    env->ReleaseStringUTFChars(str, name);

    return jbinder;
}

extern "C" JNIEXPORT jboolean JNICALL
  Java_com_qualcomm_qti_servicelib_ServiceLib_nativeIsDeclared__Ljava_lang_String_2(JNIEnv* env, jclass /**/, jstring str) {
    ALOGI("%s", __func__);

    const char* name = env->GetStringUTFChars(str, nullptr);

    ALOGI("nativeIsDeclared name: %s", name);

    jobject jbinder = nullptr;
    bool val = AServiceManager_isDeclared(name);
    ALOGI("nativeIsDeclared service status: %d", val);

    env->ReleaseStringUTFChars(str, name);

    return val;
}
