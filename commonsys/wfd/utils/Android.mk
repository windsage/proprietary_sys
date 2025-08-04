LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# ------------------------------------------------------------------------------
#            Common definitons
# ------------------------------------------------------------------------------

LOCAL_CFLAGS := -D_ANDROID_ \
                -Werror=sizeof-pointer-memaccess \
                -Werror=unused-but-set-variable \
                -Werror=missing-field-initializers \
                -Werror=missing-braces \
                -Werror=parentheses

ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS),true)
# ------------------------------------------------------------------------------
#            WFD COMMON UTILS SRC
# ------------------------------------------------------------------------------

LOCAL_SRC_FILES := \
    src/wfd_cfg_parser.cpp \
    src/wfd_netutils.cpp \
    src/wfd_cfg_utils.cpp

# ------------------------------------------------------------------------------
#             WFD COMMON UTILS INC
# ------------------------------------------------------------------------------

LOCAL_C_INCLUDES := $(LOCAL_PATH)/./inc

LOCAL_HEADER_LIBRARIES := libmmosal_headers libwfd_headers media_plugin_headers

# ------------------------------------------------------------------------------
#            WFD COMMON UTILS SHARED LIB
# ------------------------------------------------------------------------------

LOCAL_SHARED_LIBRARIES := libmmosal
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libcutils
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += libnl
LOCAL_SHARED_LIBRARIES += libwfdmminterface
LOCAL_LDLIBS += -llog

LOCAL_MULTILIB := both
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libwfdconfigutils

LOCAL_SANITIZE := integer_overflow
LOCAL_SYSTEM_EXT_MODULE := true
include $(BUILD_SHARED_LIBRARY)
endif

# ------------------------------------------------------------------------------

include $(call all-makefiles-under,$(LOCAL_PATH))

# ------------------------------------------------------------------------------
#            END
# ------------------------------------------------------------------------------
