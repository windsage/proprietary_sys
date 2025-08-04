ifneq ($(TARGET_NO_TELEPHONY), true)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

BUILD_TOP := ../../../../../../
ifeq ($(TARGET_SUPPORTS_WEAR_OS), true)
    ifneq ($(QC_PROP_ROOT), vendor/qcom/proprietary)
        BUILD_TOP := ../../../../../../../
    endif
endif
phone_common_dir := $(BUILD_TOP)/packages/apps/PhoneCommon
chips_dir := $(BUILD_TOP)/frameworks/opt/chips
$(warning $(phone_common_dir))

src_dirs := src  $(phone_common_dir)/src
res_dirs := res $(phone_common_dir)/res $(chips_dir)/res

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.android.contacts.common \
    --extra-packages com.android.ex.chips \
    --extra-packages com.android.phone.common

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.android.vcard \
    android-common \
    guava \
    libchips \
    libphonenumber \

LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx.core_core \
    android-support-v4 \

LOCAL_PACKAGE_NAME := ConferenceDialer
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := false
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PRODUCT_MODULE := true
LOCAL_SDK_VERSION := system_current

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

endif # TARGET_NO_TELEPHONY
