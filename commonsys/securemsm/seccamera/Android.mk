ifneq ($(TARGET_BOARD_AUTO),true)
include $(call all-subdir-makefiles)
endif
