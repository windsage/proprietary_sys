LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)

LOCAL_MULTILIB := both
LOCAL_MODULE    := libwfdnative
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := src/WFDNative.cpp

# Additional libraries, maybe more than actually needed
LOCAL_SHARED_LIBRARIES := libandroid_runtime
LOCAL_SHARED_LIBRARIES += libui
LOCAL_SHARED_LIBRARIES += libinput
LOCAL_SHARED_LIBRARIES += libnativehelper
LOCAL_SHARED_LIBRARIES += libutils liblog
LOCAL_SHARED_LIBRARIES += libwfdclient libhidlbase libhidlmemory libbinder libgui
LOCAL_SHARED_LIBRARIES += android.hidl.base@1.0 android.hidl.token@1.0-utils
LOCAL_SHARED_LIBRARIES += android.hardware.graphics.common@1.0 android.hardware.graphics.bufferqueue@1.0
LOCAL_SHARED_LIBRARIES += android.hardware.graphics.bufferqueue@2.0

LOCAL_C_INCLUDES := $(LOCAL_PATH)/inc

LOCAL_HEADER_LIBRARIES := libmmosal_headers libwfd_headers
LOCAL_HEADER_LIBRARIES += vendor_common_inc

# JNI headers
LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

LOCAL_EXPORT_SHARED_LIBRARY_HEADERS := android.hidl.token@1.0-utils android.hidl.memory@1.0

LOCAL_LDLIBS += -llog

LOCAL_STATIC_LIBRARIES += libgui_aidl_static

LOCAL_SANITIZE := integer_overflow
LOCAL_SYSTEM_EXT_MODULE := true
include $(BUILD_SHARED_LIBRARY)
