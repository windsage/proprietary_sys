/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 *  Copyright (c) 2020-2021, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

#define LOG_TAG "BluetoothMCPService_jni"

#define LOG_NDEBUG 0

#include <base/bind.h>
#include <base/callback.h>
#include <map>
#include <mutex>
#include <shared_mutex>
#include <vector>

#include "android_runtime/AndroidRuntime.h"
#include "base/logging.h"
#include "com_android_bluetooth.h"
#include "hardware/bt_mcp.h"
#include "hardware/bluetooth.h"

using bluetooth::mcp_server::McpServerCallbacks;
using bluetooth::mcp_server::McpServerInterface;
using bluetooth::Uuid;
static McpServerInterface* sMcpServerInterface = nullptr;


namespace android {
static jmethodID method_OnConnectionStateChanged;
static jmethodID method_MediaControlPointChangedRequest;
static jmethodID method_TrackPositionChangedRequest;
static jmethodID method_PlayingOrderChangedRequest;
static jmethodID method_McpServerInitializedCallback;

static std::shared_timed_mutex interface_mutex;

static jobject mCallbacksObj = nullptr;
static std::shared_timed_mutex callbacks_mutex;

class McpServerCallbacksImpl : public McpServerCallbacks {
   public:
  ~McpServerCallbacksImpl() = default;

  void OnConnectionStateChange(int state, const RawAddress& bd_addr) override {
    LOG(INFO) << __func__;
    std::shared_lock<std::shared_timed_mutex> lock(callbacks_mutex);
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid() || mCallbacksObj == nullptr) return;
    ScopedLocalRef<jbyteArray> addr(
        sCallbackEnv.get(), sCallbackEnv->NewByteArray(sizeof(RawAddress)));
    if (!addr.get()) {
      LOG(ERROR) << "Failed to new jbyteArray bd addr for connection state";
      return;
    }
    sCallbackEnv->SetByteArrayRegion(addr.get(), 0, sizeof(RawAddress),
                                     (jbyte*)&bd_addr);
    sCallbackEnv->CallVoidMethod(mCallbacksObj, method_OnConnectionStateChanged,
                                 (jint)state, addr.get());
  }

  void MediaControlPointChangeReq(uint8_t state, const RawAddress& bd_addr) override {
    LOG(INFO) << __func__;

    std::shared_lock<std::shared_timed_mutex> lock(callbacks_mutex);
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid() || mCallbacksObj == nullptr) return;

    ScopedLocalRef<jbyteArray> addr(
        sCallbackEnv.get(), sCallbackEnv->NewByteArray(sizeof(RawAddress)));
    if (!addr.get()) {
      LOG(ERROR) << "Failed to new jbyteArray bd addr for connection state";
      return;
    }

    sCallbackEnv->SetByteArrayRegion(addr.get(), 0, sizeof(RawAddress),
                                     (jbyte*)&bd_addr);
    sCallbackEnv->CallVoidMethod(mCallbacksObj, method_MediaControlPointChangedRequest,
                                 (jint)state, addr.get());
  }

  void TrackPositionChangeReq(int32_t position) override {
    LOG(INFO) << __func__;

    std::shared_lock<std::shared_timed_mutex> lock(callbacks_mutex);
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid() || mCallbacksObj == nullptr) return;

    sCallbackEnv->CallVoidMethod(mCallbacksObj, method_TrackPositionChangedRequest,
                                 (jint)position);
  }

  void PlayingOrderChangeReq(uint32_t order) override {
    LOG(INFO) << __func__;

    std::shared_lock<std::shared_timed_mutex> lock(callbacks_mutex);
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid() || mCallbacksObj == nullptr) return;

    sCallbackEnv->CallVoidMethod(mCallbacksObj, method_PlayingOrderChangedRequest,
                                 (jint)order);
  }

  void McpServerInitializedCallback(uint8_t status) override {
    LOG(INFO) << __func__;

    std::shared_lock<std::shared_timed_mutex> lock(callbacks_mutex);
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid() || mCallbacksObj == nullptr) return;

    sCallbackEnv->CallVoidMethod(mCallbacksObj, method_McpServerInitializedCallback,
                                 (jint)status);
  }
};


static McpServerCallbacksImpl sMcpServerCallbacks;

static void classInitNative(JNIEnv* env, jclass clazz) {
  LOG(INFO) << __func__ << ": class init native";
  method_OnConnectionStateChanged =
      env->GetMethodID(clazz, "OnConnectionStateChanged", "(I[B)V");
  method_MediaControlPointChangedRequest =
      env->GetMethodID(clazz, "MediaControlPointChangedRequest", "(I[B)V");
  method_TrackPositionChangedRequest =
      env->GetMethodID(clazz, "TrackPositionChangedRequest", "(I)V");
  method_PlayingOrderChangedRequest =
      env->GetMethodID(clazz, "PlayingOrderChangedRequest", "(I)V");
  method_McpServerInitializedCallback =
      env->GetMethodID(clazz, "McpServerInitializedCallback", "(I)V");
  LOG(INFO) << __func__ << ": succeeds";
}

Uuid uuid = Uuid::FromString("00001849-0000-1000-8000-00805F9B34FB");

static void initNative(JNIEnv* env, jobject object) {
  std::unique_lock<std::shared_timed_mutex> interface_lock(interface_mutex);
  std::unique_lock<std::shared_timed_mutex> callbacks_lock(callbacks_mutex);

  const bt_interface_t* btInf = getBluetoothInterface();
  if (btInf == nullptr) {
    LOG(ERROR) << "Bluetooth module is not loaded";
    return;
  }

  if (sMcpServerInterface != nullptr) {
    LOG(INFO) << "Cleaning up McpServer Interface before initializing...";
    sMcpServerInterface->Cleanup();
    sMcpServerInterface = nullptr;
  }

  if (mCallbacksObj != nullptr) {
    LOG(INFO) << "Cleaning up McpServer callback object";
    env->DeleteGlobalRef(mCallbacksObj);
    mCallbacksObj = nullptr;
  }

  if ((mCallbacksObj = env->NewGlobalRef(object)) == nullptr) {
    LOG(ERROR) << "Failed to allocate Global Ref for Mcp Controller Callbacks";
    return;
  }
  LOG(INFO) << "mcs callback initialized";
  sMcpServerInterface = (McpServerInterface* )btInf->get_profile_interface(
      BT_PROFILE_MCP_ID);
  if (sMcpServerInterface == nullptr) {
    LOG(ERROR) << "Failed to get Bluetooth Hearing Aid Interface";
    return;
  }

  sMcpServerInterface->Init(&sMcpServerCallbacks, uuid);
}

static void cleanupNative(JNIEnv* env, jobject object) {
  std::unique_lock<std::shared_timed_mutex> interface_lock(interface_mutex);
  std::unique_lock<std::shared_timed_mutex> callbacks_lock(callbacks_mutex);

  const bt_interface_t* btInf = getBluetoothInterface();
  if (btInf == nullptr) {
    LOG(ERROR) << "Bluetooth module is not loaded";
    return;
  }

  if (sMcpServerInterface != nullptr) {
    sMcpServerInterface->Cleanup();
    sMcpServerInterface = nullptr;
  }

  if (mCallbacksObj != nullptr) {
    env->DeleteGlobalRef(mCallbacksObj);
    mCallbacksObj = nullptr;
  }
}

static jboolean mediaControlPointOpcodeSupportedNative(JNIEnv* env, jobject object,
                                           jint feature) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  sMcpServerInterface->MediaControlPointOpcodeSupported(feature);
  return JNI_TRUE;
}

static jboolean mediaControlPointNative(JNIEnv* env, jobject object,
                                           jint value, jint status) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  sMcpServerInterface->MediaControlPoint(value, status);
  return JNI_TRUE;
}

static jboolean mediaStateNative(JNIEnv* env, jobject object,
                                           jint state) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  sMcpServerInterface->MediaState(state);
  return JNI_TRUE;
}

static jboolean mediaPlayerNameNative(JNIEnv* env, jobject object,
                                           jstring playerName) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;


  const char *nativeString = env->GetStringUTFChars(playerName, nullptr);
  if (!nativeString) {
    jniThrowIOException(env, EINVAL);
    return JNI_FALSE;
  }
  sMcpServerInterface->MediaPlayerName((uint8_t*)nativeString);
  env->ReleaseStringUTFChars(playerName, nativeString);
  return JNI_TRUE;
}

static jboolean trackChangedNative(JNIEnv* env, jobject object) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  sMcpServerInterface->TrackChanged();
  return JNI_TRUE;
}

static jboolean trackPositionNative(JNIEnv* env, jobject object,
                                           jint playPosition) {
  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;


  sMcpServerInterface->TrackPosition(playPosition);
  return JNI_TRUE;
}

static jboolean trackDurationNative(JNIEnv* env, jobject object,
                                           jint duration) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  sMcpServerInterface->TrackDuration(duration);

  return JNI_TRUE;
}

static jboolean trackTitleNative(JNIEnv* env, jobject object,
                                           jstring title) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  const char *nativeString = env->GetStringUTFChars(title, nullptr);
  if (!nativeString) {
    jniThrowIOException(env, EINVAL);
    return JNI_FALSE;
  }
  sMcpServerInterface->TrackTitle((uint8_t*)nativeString);
  env->ReleaseStringUTFChars(title, nativeString);
  return JNI_TRUE;
}

static jboolean setActiveDeviceNative(JNIEnv* env, jobject object,
                                         jint profile, jint set_id,
                                         jbyteArray address, jint is_csip) {
  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  jbyte* addr = env->GetByteArrayElements(address, nullptr);
  RawAddress bd_addr = RawAddress::kEmpty;
  if (addr) {
    bd_addr.FromOctets(reinterpret_cast<const uint8_t*>(addr));
  }
  if (bd_addr == RawAddress::kEmpty) {
    LOG(INFO) << __func__ << " active device is null";
  }

  sMcpServerInterface->SetActiveDevice(bd_addr, set_id, profile, is_csip);
  env->ReleaseByteArrayElements(address, addr, 0);
  return JNI_TRUE;
}

static jboolean bondStateChangeNative(JNIEnv* env, jobject object,
                                          jint state, jbyteArray address) {
  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  jbyte* addr = env->GetByteArrayElements(address, nullptr);
  if (!addr) {
    jniThrowIOException(env, EINVAL);
    return JNI_FALSE;
  }
  RawAddress* tmpraw = (RawAddress*)addr;

  sMcpServerInterface->BondStateChange(*tmpraw, state);
  env->ReleaseByteArrayElements(address, addr, 0);
  return JNI_TRUE;
}

static jboolean playingOrderSupportedNative(JNIEnv* env, jobject object,
                                           jint order) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  sMcpServerInterface->PlayingOrderSupported(order);

  return JNI_TRUE;
}

static jboolean playingOrderNative(JNIEnv* env, jobject object,
                                           jint order) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  sMcpServerInterface->PlayingOrder(order);

  return JNI_TRUE;
}

static jboolean contentControlIdNative(JNIEnv* env, jobject object,
                                           jint ccid) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  sMcpServerInterface->ContentControlId(ccid);
  return JNI_TRUE;
}

static jboolean disconnectMcpNative(JNIEnv* env, jobject object,
                                           jbyteArray address) {

  LOG(INFO) << __func__;
  std::shared_lock<std::shared_timed_mutex> lock(interface_mutex);
  if (!sMcpServerInterface) return JNI_FALSE;

  jbyte* addr = env->GetByteArrayElements(address, nullptr);
  if (!addr) {
    jniThrowIOException(env, EINVAL);
    return JNI_FALSE;
  }
  RawAddress* tmpraw = (RawAddress*)addr;

  sMcpServerInterface->DisconnectMcp(*tmpraw);
  env->ReleaseByteArrayElements(address, addr, 0);
  return JNI_TRUE;
}

static JNINativeMethod sMethods[] = {
    {"classInitNative", "()V", (void*)classInitNative},
    {"initNative", "()V", (void*)initNative},
    {"cleanupNative", "()V", (void*)cleanupNative},
    {"mediaStateNative", "(I)Z", (void*)mediaStateNative},
    {"mediaPlayerNameNative", "(Ljava/lang/String;)Z", (void*)mediaPlayerNameNative},
    {"mediaControlPointOpcodeSupportedNative", "(I)Z", (void*)mediaControlPointOpcodeSupportedNative},
    {"mediaControlPointNative", "(II)Z", (void*)mediaControlPointNative},
    {"trackChangedNative", "()Z", (void*)trackChangedNative},
    {"trackTitleNative", "(Ljava/lang/String;)Z", (void*)trackTitleNative},
    {"trackPositionNative", "(I)Z", (void*)trackPositionNative},
    {"trackDurationNative", "(I)Z", (void*)trackDurationNative},
    {"playingOrderSupportedNative", "(I)Z", (void*)playingOrderSupportedNative},
    {"playingOrderNative", "(I)Z", (void*)playingOrderNative},
    {"setActiveDeviceNative", "(II[BI)Z", (void*)setActiveDeviceNative},
    {"contentControlIdNative", "(I)Z", (void*)contentControlIdNative},
    {"disconnectMcpNative", "([B)Z", (void*)disconnectMcpNative},
    {"bondStateChangeNative", "(I[B)Z", (void*)bondStateChangeNative},
};

int register_com_android_bluetooth_mcp(JNIEnv* env) {
  return jniRegisterNativeMethods(
      env, "com/android/bluetooth/mcp/McpNativeInterface",
      sMethods, NELEM(sMethods));
}
}  // namespace android

