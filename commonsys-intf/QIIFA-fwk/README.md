Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
All rights reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.


QIIFA API MANAGEMENT TOOL

Please refer below document that guides in new component onboarding.

https://confluence.qualcomm.com/confluence/download/attachments/478119960/Versioning%20and%20Compatibility%20LA%2C%20LE.docx?api=v2

 --------------------------------------------------------------------------------------------------------------------------------------------------------
INVOCATION

    Mandatory Arguments
    --platform: Supported Values: [LA,LE]
    --wroot: Absolute path to the workspace root directory
    --qiifa_tools: Absolute Path to QIIFA tools directory

    --golden_ref: Generates Golden Reference : Excpeted Values : [all,component_name]

    --iic: Runs Interface Integrity check : Excpeted Values : [enforce,warning,component_name]
    --cr: Out Path to  Compatibility Report

    --symbol: Mandatory on LA, path to symbols dirctory(contains shared object files .so)
    --rootfs: Mandatory on LE, path to QIIFA rootfs directory(contains shared object files .so)

    --skip_cd_check: This converts errors of missing cd.xml file to warnings

    --toolchain: Specify toolchain or compiler name followed by version e.g. gcc11, used for adding Toolchain based GRD generation and IIC check
    --toolchain_config: Absolute path to the file named toolchain_config.json which specifies the fallback toolchain Version for indiviual components

   --help : prints above info

 --------------------------------------------------------------------------------------------------------------------------------------------------------

USAGE EXAMPLES

=========================================== LA Platform =================================================

    GRD Generation

        python <QIIFA-fwk dir>/plugins/qiifa_api_management/qiifa_api_management.py
        --qiifa_tools <abs path to QIIFA tools>
        --wroot  <absolute-workspace-root-path>
        --symbol <absolute-symbols-folder-path> e.g. $OUT/symbols
        --platform LA
        --golden_ref [all,component_name]
        --toolchain <toolchain_name_version> (Requierd only for Toolchain based GRD)

    IIC check

        python <QIIFA-fwk dir>/plugins/qiifa_api_management/qiifa_api_management.py
        --qiifa_tools <abs path to QIIFA tools>
        --wroot  <absolute-workspace-root-path>
        --symbol <absolute-symbols-folder-path> e.g. $OUT/symbols
        --platform LA
        --toolchain <toolchain_name_version>
        --toolchain_config <toolchain_config.json filepath>
        --iic [enforce,warning,component_name] (Requierd only for Toolchain based GRD)
        --cr <out path to compatiblity report> (Requierd only for Toolchain based GRD)

=========================================== LE Platform =================================================

    GRD Generation

        python <QIIFA-fwk dir>/plugins/qiifa_api_management/qiifa_api_management.py
        --qiifa_tools <abs path to QIIFA tools>
        --wroot  <absolute-workspace-root-path>
        --rootfs <absolute path to QIIFA-rootfs directory> e.g. build-*-*/QIIFA-rootfs
        --platform LA
        --golden_ref [all,component_name]
        --toolchain <toolchain_name_version> (Requierd only for Toolchain based GRD)

    IIC check

        python <QIIFA-fwk dir>/plugins/qiifa_api_management/qiifa_api_management.py
        --qiifa_tools <abs path to QIIFA tools>
        --wroot  <absolute-workspace-root-path>
        --rootfs <absolute path to QIIFA-rootfs directory> e.g. build-*-*/QIIFA-rootfs
        --platform LA
        --toolchain <toolchain_name_version>
        --toolchain_config <toolchain_config.json filepath>
        --iic [enforce,warning,component_name] (Requierd only for Toolchain based GRD)
        --cr <out path to compatiblity report> (Requierd only for Toolchain based GRD)

 --------------------------------------------------------------------------------------------------------------------------------------------------------
