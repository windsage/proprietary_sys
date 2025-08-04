LOCAL_PATH := $(call my-dir)

#=============================================
#  Listen API lib: JACK format
#=============================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    src/com/qualcomm/listen/ListenTypes.java \
    src/com/qualcomm/listen/ListenSoundModel.java

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := Listen
LOCAL_SYSTEM_EXT_MODULE := true

LOCAL_STATIC_JAVA_LIBRARIES := vendor.qti.hardware.ListenSoundModelAidl-V1-java

include $(BUILD_STATIC_JAVA_LIBRARY)

#=============================================
#  Listen API lib: JAR format
#=============================================
include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
    src/com/qualcomm/listen/ListenTypes.java \
    src/com/qualcomm/listen/ListenSoundModel.java

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := ListenJAR
LOCAL_SYSTEM_EXT_MODULE := true

LOCAL_STATIC_JAVA_LIBRARIES := vendor.qti.hardware.ListenSoundModelAidl-V1-java

#replaced ANDROID_COMPILE_WITH_JACK by the local flag to
#only affect this module compilation
LOCAL_JACK_ENABLED := disabled

include $(BUILD_STATIC_JAVA_LIBRARY)

