LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE        := VtsHalDynamicDDSAidl
LOCAL_MODULE_TAGS   := optional
LOCAL_CFLAGS        := -O0 -g

LOCAL_SRC_FILES     += VtsAidlDynamicDdsV1_0_targetTest.cpp

LOCAL_SHARED_LIBRARIES := liblog \
                          libbinder_ndk \
                          libutils \
                          vendor.qti.data.factoryservice-V1-ndk \
                          vendor.qti.hardware.data.dynamicddsaidlservice-V1-ndk \

LOCAL_STATIC_LIBRARIES := VtsHalHidlTargetTestBase

LOCAL_MODULE_OWNER := qti

include $(BUILD_NATIVE_TEST)
