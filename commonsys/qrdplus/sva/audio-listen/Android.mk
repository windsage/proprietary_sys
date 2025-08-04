#
# Copyright (c) 2017 Qualcomm Technologies, Inc.
# All Rights Reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.
#
ifeq ($(TARGET_BOARD_PLATFORM), $(filter $(TARGET_BOARD_PLATFORM),qssi sdm660 msm8937 msm8953))
ifneq ($(TARGET_BOARD_SUFFIX), _32go)
ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS), true)
    include $(call all-subdir-makefiles)
endif
endif
endif #QSSI check

