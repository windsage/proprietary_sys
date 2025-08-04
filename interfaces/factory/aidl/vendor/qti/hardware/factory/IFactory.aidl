/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

// FIXME: license file, or use the -l option to generate the files with the header.

package vendor.qti.hardware.factory;

import vendor.qti.hardware.factory.FactoryResult;
import vendor.qti.hardware.factory.IResultType;
import vendor.qti.hardware.factory.ReadFileReq;
import vendor.qti.hardware.factory.ReadFileResult;
import vendor.qti.hardware.factory.WriteFileReq;
import vendor.qti.hardware.factory.FactoryResult;

// Interface inherits from vendor.qti.hardware.factory@1.0::IFactory but AIDL does not support interface inheritance.
@VintfStability
interface IFactory {
    // Adding return type to method instead of out param boolean success since there is only one return value.
    /**
     * Enable / Disable Charger
     *
     * @param enable
     * @param out result
     */
    boolean chargerEnable(in boolean enable);

    // Adding return type to method instead of out param FactoryResult result since there is only one return value.
    /**
     * Execute command with value.
     *
     * @param cmd:
     * @param value:
     * @return
     *
     * Support commands and value:
     * 1. Set property : cmd = "setprop" value = "aaa.bbb.ccc=xxxx"
     * 2. Get property : cmd = "getprop" value = "aaa.bbb.ccc"
     *
     */
    FactoryResult delegate(in String cmd, in String value);

    // Adding return type to method instead of out param IResultType result since there is only one return value.
    /**
     * dir List FileName To File
     *
     * @param filePath:/mnt/vendor/persist/FTM_AP/
     * @param fileName: directory.txt
     * @return
     */
    IResultType dirListFileNameToFile(in String path, in String name);

    // Adding return type to method instead of out param boolean success since there is only one return value.
    /**
     * Enter ship mode
     *
     * @param out result 0 on success, -1 on failure
     */
    boolean enterShipMode();

    // Adding return type to method instead of out param IResultType result since there is only one return value.
    /**
     * Erase All Files
     *
     * @param fileName
     * @return
     */
    IResultType eraseAllFiles(in String path);

    // Adding return type to method instead of out param FactoryResult result since there is only one return value.
    /**
     * Get smb status(seconds)
     *
     * @return the smb status string
     */
    FactoryResult getSmbStatus();

    // Adding return type to method instead of out param ReadFileResult result since there is only one return value.
    /**
     * Read File
     *
     * @param filePath:/mnt/vendor/persist/FTM_AP/
     * @param ReadFileReq
     * @param out ReadFileResult
     */
    ReadFileResult readFile(in String path, in ReadFileReq req);

    // Adding return type to method instead of out param FactoryResult result since there is only one return value.
    /**
     * Start binary with specified param
     *
     * @param input binary name
     * @param input param
     * @return
     */
    FactoryResult runApp(in String name, in String params, in boolean isStart);

    // Adding return type to method instead of out param boolean success since there is only one return value.
    /**
     * Enable / Disable Wifi
     *
     * @param enable
     * @param out result
     */
    boolean wifiEnable(in boolean enable);

    // Adding return type to method instead of out param IResultType result since there is only one return value.
    /**
     * Write File
     *
     * @param filePath:/mnt/vendor/persist/FTM_AP/
     * @param WriteFileReq
     * @return
     */
    IResultType writeFile(in String path, in WriteFileReq req);
}
