LOCAL_PATH            := $(call my-dir)

COMMON_INCLUDES     := $(TOP)/frameworks/native/include/gui\
                       $(TOP)/frameworks/native/include/ui \
                       $(TOP)/frameworks/native/include/binder \
                       $(TOP)/frameworks/native/libs/nativewindow/include \
                       $(TOP)/frameworks/native/libs/arect/include \
                       $(TOP)/frameworks/native/libs/nativebase/include \
                       $(TOP)/frameworks/av/media/libstagefright/bqhelper/include \
                       $(TOP)/hardware/interfaces/common/support/include

MY_CFLAGS  := -g -fdiagnostics-show-option -Wno-format -Wno-missing-braces \
              -Wno-missing-field-initializers -Wno-unused-parameter

LOCAL_SANITIZE := cfi integer_overflow

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS      := optional
LOCAL_MODULE           := libsystemhelper_jni
LOCAL_SYSTEM_EXT_MODULE := true
LOCAL_PRELINK_MODULE    := false
LOCAL_CPP_EXTENSION     := .cpp

# includes
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include \
                    $(COMMON_INCLUDES)

LOCAL_VINTF_FRAGMENTS := vendor.qti.hardware.systemhelperaidl.xml

### Add all source file names to be included in lib separated by a whitespace
LOCAL_SRC_FILES         := svc.cpp \
                           SystemEvent.cpp \
                           SystemEventAIDL.cpp \
                           SystemResource.cpp \
                           SystemResourceAIDL.cpp

LOCAL_SHARED_LIBRARIES := liblog \
                          libbinder_ndk \
                          libcutils \
                          libbinder \
                          libutils \
                          libhidlbase \
                          libgui \
                          libui \
                          vendor.qti.hardware.systemhelper@1.0 \
                          android.hardware.graphics.bufferqueue@1.0 \
                          libstagefright_bufferqueue_helper \
                          vendor.qti.hardware.trustedui@1.0 \
                          vendor.qti.hardware.systemhelperaidl-V1-ndk \
                          vendor.qti.hardware.trustedui-V1-ndk

LOCAL_HEADER_LIBRARIES := vendor_common_inc

LOCAL_CFLAGS += $(MY_CFLAGS)
LOCAL_MODULE_OWNER := qti

include $(BUILD_SHARED_LIBRARY)

