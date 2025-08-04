Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. 
All rights reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.


Version : 1.0.0
Date : 9 May 2024

Whats New:

API Management toolchain support:
    We have observed in past there have been several false iic violations reported by QIIFA tool when the toolchain/compiler is changed.
    In such scenarios the fix needs to be patched to libabigail source code which is a opensource tool and can take upto 2-3 weeks to merge the fix.
    In such cases the component owners can create a new GRD based on the new toolchain and report the actual issue to qiifa_squad.


New Toolchain Workflow
    If a iic incompatibilty issue is observed on a component due to toolchain difference, then  IIC needs to be tracked using a different set of GRD created specific for the toolchain
    1.To add a new GRD for a toolchain we need to add --toolchain which takes in a unique value for the e.g. <toolchain_name><toolchain_version>
    2.IIC check for a command needs two additional arguments
        -- toolchain same as the one used while generating GRD in step1
        -- toolchain_config: absolute path to the toolchain config file toolchain_config.json which specifies the fallback toolchain version for indiviual components
            for e.g while iic invocation command if we provided gc11 as toolchain arg and the component is not configured to used gcc11 then we can specify in the toolchain_config.json against which toolchain version we want to check the IIC , below is the content of sample toolchain_config.json

            {
                "component_name" : "gcc7"
            }
