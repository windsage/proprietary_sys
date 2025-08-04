/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.internal.telephony.shell;

import android.os.ShellCommand;
import android.util.Log;

import java.io.PrintWriter;

public final class QtiShellCommand extends ShellCommand {
    private static final String LOG_TAG = "QtiShellCommand";
    private static final String CMD_MAIN_GET = "get";
    private static final String CMD_MAIN_ENABLE = "enable";
    private static final String CMD_MAIN_DISABLE = "disable";

    private static final String FEATURE_TEMP_DDS = "--temp-dds";
    private static final String FEATURE_SMART_PERM_DDS = "--smart-perm-dds";
    private static final String FEATURE_DUAL_DATA = "--dual-data";

    private static final int RESULT_SUCCESS = 0;
    private static final int RESULT_FAILURE = -1;

    private final ShellHandlerDelegate mDelegate;

    public QtiShellCommand(ShellHandlerDelegate delegate) {
        mDelegate = delegate;
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null || "help".equals(cmd) || "-h".equals(cmd)) {
            return handleDefaultCommands(cmd);
        }
        final PrintWriter perr = getErrPrintWriter();
        switch(cmd) {
            case CMD_MAIN_GET:
                return handleGetAction(getNextArgRequired(), perr);
            case CMD_MAIN_ENABLE:
                return handleEnableAction(getNextArgRequired(), perr);
            case CMD_MAIN_DISABLE:
                return handleDisableAction(getNextArgRequired(), perr);
            default:
                log("Error cmd: " + cmd);
        }
        return handleErrorCmd(perr);
    }

    @Override
    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Extension phone (settings) commands:");
        pw.println("   help or -h to show help text");
        pw.println("   get with parameters below");
        pw.println("       --temp-dds to retrieve the current temp dds setting");
        pw.println("            It refers to data during call rather than telephony auto dds,");
        pw.println("            and also refers to smart temp dds when modem supports it.");
        pw.println("       --smart-perm-dds to retrieve the current smart perm dds setting");
        pw.println("       --dual-data to retrieve the current data++data setting");
        pw.println("   enable with parameters below");
        pw.println("       --temp-dds to enable temp dds");
        pw.println("            It refers to data during call rather than telephony auto dds,");
        pw.println("            and also refers to smart temp dds when modem supports it.");
        pw.println("       --smart-perm-dds to enable smart perm dds");
        pw.println("       --dual-data to enable data++data feature");
        pw.println("   disable with parameters below");
        pw.println("       --temp-dds to disable temp dds");
        pw.println("            It refers to data during call rather than telephony auto dds,");
        pw.println("            and also refers to smart temp dds when modem supports it.");
        pw.println("       --smart-perm-dds to disable smart perm dds");
        pw.println("       --dual-data to disable data++data feature");
    }

    private int handleErrorCmd(PrintWriter perr) {
        perr.println("Invalid Cmd: execute extphone help or -h for details");
        return RESULT_FAILURE;
    }

    private int handleGetAction(String feature, PrintWriter perr) {
        if (feature == null) {
            return handleErrorCmd(perr);
        }
        PrintWriter pw = getOutPrintWriter();
        switch (feature) {
            case FEATURE_TEMP_DDS:
                pw.println(mDelegate.getTempDdsEnableState());
                break;
            case FEATURE_SMART_PERM_DDS:
                pw.println(mDelegate.getSmartPermDdsEnableState());
                break;
            case FEATURE_DUAL_DATA:
                pw.println(mDelegate.getDualDataEnableState());
                break;
            default:
                return handleErrorCmd(perr);
        }
        return RESULT_SUCCESS;
    }

    private int handleEnableAction(String feature, PrintWriter perr) {
        if (feature == null) {
            return handleErrorCmd(perr);
        }
        switch (feature) {
            case FEATURE_TEMP_DDS:
                return handleAction(mDelegate::enableTempDdsSwitch, perr);
            case FEATURE_SMART_PERM_DDS:
                return handleAction(mDelegate::enableSmartPermDdsSwitch, perr);
            case FEATURE_DUAL_DATA:
                return handleAction(mDelegate::enableDualData, perr);
            default:
                log("Error feature: " + feature);
        }
        return handleErrorCmd(perr);
    }

    private int handleDisableAction(String feature, PrintWriter perr) {
        if (feature == null) {
            return handleErrorCmd(perr);
        }
        switch (feature) {
            case FEATURE_TEMP_DDS:
                return handleAction(mDelegate::disableTempDdsSwitch, perr);
            case FEATURE_SMART_PERM_DDS:
                return handleAction(mDelegate::disableSmartPermDdsSwitch, perr);
            case FEATURE_DUAL_DATA:
                return handleAction(mDelegate::disableDualData, perr);
            default:
                log("Error feature: " + feature);
        }
        return handleErrorCmd(perr);
    }

    private int handleAction(ActionInvoker action, PrintWriter perr) {
        if (!action.invoke()) {
            perr.println("Failure");
            return RESULT_FAILURE;
        }
        perr.println("Success");
        return RESULT_SUCCESS;
    }

    private interface ActionInvoker {
        public boolean invoke();
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
