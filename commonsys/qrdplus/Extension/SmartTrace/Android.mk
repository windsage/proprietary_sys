ifneq (,$(filter userdebug eng, $(TARGET_BUILD_VARIANT)))
  LOCAL_PATH:= $(call my-dir)
  include $(CLEAR_VARS)
  LOCAL_MODULE       := binder_trace_dump.sh
  LOCAL_MODULE_TAGS  := optional
  LOCAL_MODULE_CLASS := EXECUTABLES
  LOCAL_SRC_FILES    := shell/binder_trace_dump.sh
  LOCAL_MODULE_PATH  := $(TARGET_OUT_SYSTEM_EXT_EXECUTABLES)
  LOCAL_SYSTEM_EXT_MODULE := true
  include $(BUILD_PREBUILT)

  include $(CLEAR_VARS)
  LOCAL_MODULE       := perfetto_dump.sh
  LOCAL_MODULE_TAGS  := optional
  LOCAL_MODULE_CLASS := EXECUTABLES
  LOCAL_SRC_FILES    := shell/perfetto_dump.sh
  LOCAL_MODULE_PATH  := $(TARGET_OUT_SYSTEM_EXT_EXECUTABLES)
  LOCAL_SYSTEM_EXT_MODULE := true
  include $(BUILD_PREBUILT)

  include $(CLEAR_VARS)
  LOCAL_MODULE       := perfetto.cfg
  LOCAL_MODULE_TAGS  := optional
  LOCAL_MODULE_CLASS := ETC
  LOCAL_SRC_FILES    := perfetto.cfg
  LOCAL_MODULE_PATH  := $(TARGET_OUT_SYSTEM_EXT_ETC)
  LOCAL_SYSTEM_EXT_MODULE := true
  include $(BUILD_PREBUILT)
endif
