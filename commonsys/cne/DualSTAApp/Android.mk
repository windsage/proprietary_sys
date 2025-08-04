ifneq ($(TARGET_BOARD_AUTO),true)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := DualSTAApp
LOCAL_STATIC_JAVA_LIBRARIES := vendor.qti.data.mwqem-V1.0-java \
                               vendor.qti.data.factory-V2.1-java \
                               vendor.qti.data.factory-V2.2-java \
                               vendor.qti.data.factoryservice-V1-java \
                               vendor.qti.data.mwqemaidlservice-V1-java \

LOCAL_CERTIFICATE := platform
LOCAL_MODULE_OWNER := qti
LOCAL_SYSTEM_EXT_MODULE := true
LOCAL_PRIVATE_PLATFORM_APIS := true

include $(BUILD_PACKAGE)

endif
