LOCAL_PATH := $(call my-dir)
PREBUILT_DIR_PATH := $(LOCAL_PATH)

ifeq ($(TARGET_BOARD_PLATFORM),qssi)
  ifeq ($(strip $(TARGET_BOARD_SUFFIX)),)
    -include $(PREBUILT_DIR_PATH)/target/product/qssi/Android.mk
  endif
endif

ifeq ($(TARGET_BOARD_PLATFORM),qssi)
  ifeq ($(strip $(TARGET_BOARD_SUFFIX)),_64)
    -include $(PREBUILT_DIR_PATH)/target/product/qssi_64/Android.mk
  endif
endif

-include $(sort $(wildcard $(PREBUILT_DIR_PATH)/*/*/Android.mk))
