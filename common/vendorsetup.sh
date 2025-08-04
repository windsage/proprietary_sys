# *********************************************************************
# Copyright (c) 2013 Qualcomm Technologies, Inc.  All Rights Reserved.
# Qualcomm Technologies Proprietary and Confidential.
# *********************************************************************

# QC specific definitions & variables
export QCPATH="vendor/qcom/proprietary"
export QCPATH_COMMONSYS="${QCPATH}/commonsys"
export SDCLANG_AE_CONFIG="${QCPATH}/common-noship/etc/sdclang.json"
export SDCLANG_CONFIG="$(pwd)/${QCPATH}/common/config/sdclang.json"
export SDCLANG_CONFIG_AOSP="${QCPATH}/common/config/sdclang-pureAOSP.json"
export QIIFA_BUILD_CONFIG="$(pwd)/out/QIIFA_BUILD_CONFIG/build_config.xml"
export TARGET_USES_QMAA=$(grep 'TARGET_USES_QMAA :=' $(pwd)/device/qcom/${TARGET_BOARD_PLATFORM}/${TARGET_BOARD_PLATFORM}.mk | cut -d '=' -f2 | tr -d ' ' | head -n 1)
