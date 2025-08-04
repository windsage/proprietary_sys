/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.internal.telephony.shell;

public interface ShellHandlerDelegate {

    default boolean enableDualData() { return false; }
    default boolean disableDualData() { return false; }
    default String getDualDataEnableState() { return String.valueOf(0); }

    default boolean enableTempDdsSwitch() { return false; }
    default boolean disableTempDdsSwitch() { return false; }
    default String getTempDdsEnableState() { return String.valueOf(0); }

    default boolean enableSmartPermDdsSwitch() { return false; }
    default boolean disableSmartPermDdsSwitch() { return false; }
    default String getSmartPermDdsEnableState() { return String.valueOf(0); }
}
