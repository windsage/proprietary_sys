LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#-----------------------------------------------------------------
# Define
#-----------------------------------------------------------------
LOCAL_CFLAGS := -D_ANDROID_

#----------------------------------------------------------------
# SRC CODE
#----------------------------------------------------------------
LOCAL_SRC_FILES := src/WFDSurfaceMediaSource.cpp

#----------------------------------------------------------------
# INCLUDE PATH
#----------------------------------------------------------------
LOCAL_C_INCLUDES := $(LOCAL_PATH)/inc
ifeq ($(TARGET_USES_QCOM_DISPLAY_BSP), true)
    LOCAL_HEADER_LIBRARIES := display_intf_headers
    LOCAL_CFLAGS += -DQTI_BSP
endif
LOCAL_SHARED_LIBRARIES := liblog
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libgui
LOCAL_SHARED_LIBRARIES += libui
LOCAL_SHARED_LIBRARIES += libstagefright_foundation

LOCAL_MULTILIB := both
LOCAL_MODULE:= libwfdavenhancements

LOCAL_MODULE_TAGS := optional

LOCAL_SANITIZE := integer_overflow
LOCAL_SYSTEM_EXT_MODULE := true
include $(BUILD_SHARED_LIBRARY)

#-----------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MULTILIB := both
LOCAL_MODULE := libwfdavenhancements_headers
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)/inc
LOCAL_SYSTEM_EXT_MODULE := true
include $(BUILD_HEADER_LIBRARY)
