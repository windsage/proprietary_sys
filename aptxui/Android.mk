ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS), true)
ifneq ($(TARGET_HAS_LOW_RAM),true)
LOCAL_PATH := $(call my-dir)

#=============================================
#  aptxui APK
#=============================================
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_DEX_PREOPT := false
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_CERTIFICATE := platform
LOCAL_PROTOC_OPTIMIZE_TYPE := micro
LOCAL_PACKAGE_NAME := aptxui
LOCAL_USE_AAPT2 := true
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_PRIVILEGED_MODULE := true
LOCAL_SYSTEM_EXT_MODULE := true

LOCAL_STATIC_ANDROID_LIBRARIES := \
     androidx.preference_preference \

include $(BUILD_PACKAGE)

#==================================================
# Install priv-app permisison file
#==================================================
include $(CLEAR_VARS)

LOCAL_MODULE := privapp-permissions-aptxui.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_TAGS := optional
LOCAL_SYSTEM_EXT_MODULE := true
LOCAL_MODULE_PATH := $(TARGET_OUT_SYSTEM_EXT_ETC)/permissions

include $(BUILD_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
endif #TARGET_HAS_LOW_RAM
endif #TARGET_FWK_SUPPORTS_FULL_VALUEADDS
