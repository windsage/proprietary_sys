ifeq ($(call is-board-platform-in-list, qssi),true)
include $(call all-subdir-makefiles)
endif