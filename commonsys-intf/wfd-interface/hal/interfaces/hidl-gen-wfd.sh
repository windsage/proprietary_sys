/*
  * Copyright (c) 2020 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
  */

# cd $ANDROID_BUILD_TOP

echo -e "\nQCPATH is set to '$QCPATH'"

WFD_INTERFACE_ROOT_DIR="$QCPATH/commonsys-intf/wfd-interface/hal"

WFD_HIDL_ROOT_DIR="$QCPATH/wfd-framework/hal"
WFD_HIDL_ROOT_DIR_SIGMA="$QCPATH/commonsys/wfd-framework/hal"

HIDL_INTERFACES_DIR="$WFD_INTERFACE_ROOT_DIR/interfaces"

HIDL_ANDROIDBP_DIR="$WFD_INTERFACE_ROOT_DIR/interfaces/wifidisplaysession"
HIDL_ANDROIDBP_DIR_SIGMA="$WFD_INTERFACE_ROOT_DIR/interfaces/sigma_miracast"

LIBHIDL_COMPONENT='-r android.hidl:system/libhidl/transport'

echo -e "\n\nGenerate JAVA/C++ files from *.hal files..."
DIR="$WFD_HIDL_ROOT_DIR/hidl/wifidisplaysession/1.0"
DIR_SIGMA="$WFD_HIDL_ROOT_DIR_SIGMA/hidl/sigma_miracast/1.0"

hidl-gen -Landroidbp -r vendor.qti.hardware.wifidisplaysession:$HIDL_ANDROIDBP_DIR $LIBHIDL_COMPONENT vendor.qti.hardware.wifidisplaysession@1.0
hidl-gen -Landroidbp -r vendor.qti.hardware.sigma_miracast:$HIDL_ANDROIDBP_DIR_SIGMA $LIBHIDL_COMPONENT vendor.qti.hardware.sigma_miracast@1.0

echo -e "\n\nGenerate default implementation and make files from *.hal files..."
DIR="$WFD_HIDL_ROOT_DIR/hidl/wifidisplaysession/1.0/default"
DIR_SIGMA="$WFD_HIDL_ROOT_DIR_SIGMA/hidl/sigma_miracast/1.0/default"

hidl-gen -o $DIR -Lc++-impl -r vendor.qti.hardware:$HIDL_INTERFACES_DIR $LIBHIDL_COMPONENT vendor.qti.hardware.wifidisplaysession@1.0
hidl-gen -o $DIR -Landroidbp-impl -r vendor.qti.hardware:$HIDL_INTERFACES_DIR $LIBHIDL_COMPONENT vendor.qti.hardware.wifidisplaysession@1.0

hidl-gen -o $DIR_SIGMA -Lc++-impl -r vendor.qti.hardware:$HIDL_INTERFACES_DIR $LIBHIDL_COMPONENT vendor.qti.hardware.sigma_miracast@1.0
hidl-gen -o $DIR_SIGMA -Landroidbp-impl -r vendor.qti.hardware:$HIDL_INTERFACES_DIR $LIBHIDL_COMPONENT vendor.qti.hardware.sigma_miracast@1.0

hidl-gen -o $DIR -L hash -r vendor.qti.hardware:$HIDL_INTERFACES_DIR $LIBHIDL_COMPONENT vendor.qti.hardware.wifidisplaysession@1.0
hidl-gen -o $DIR_SIGMA -L hash -r vendor.qti.hardware:$HIDL_INTERFACES_DIR $LIBHIDL_COMPONENT vendor.qti.hardware.sigma_miracast@1.0
