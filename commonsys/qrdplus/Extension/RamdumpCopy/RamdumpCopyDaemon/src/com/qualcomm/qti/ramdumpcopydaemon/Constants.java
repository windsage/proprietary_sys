/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.ramdumpcopydaemon;

public class Constants {
    public static final int ECONNREFUSED = -1;/* Connection refused */
    public static final int EIO = -5;         /* Copy failed */
    public static final int EAGAIN = -11;     /* Dump has been copied */
    public static final int ENOREADY = -21;   /* Not ready */
    public static final int EINVAL = -22;     /* Invalid dump */
    public static final int ENOSPC = -28;     /* No space left on device */

    public static final int STATUS_OK = 1;

    public static final int STATUS_COPYING = 1; /* current file is under copying */
    public static final int STATUS_DONE = 2;    /* current file copy done */

    public static final int FROM_SERVER = 100;
    public static final int FROM_UI = 200;

    public static final int CMD_COPY_TYPE = 0;
    public static final int CMD_VALIDATED = 1;
    public static final int CMD_TOTAL_SIZE = 2;
    public static final int CMD_TOTAL_COUNT = 3;
    public static final int CMD_COPY_UPDATE = 4;
    public static final int CMD_COPY_FINISHED = 5; /* the whole copy process finished */

    public static final int TYPE_COMBINED = 0;
    public static final int TYPE_MULTIPLE = 1;

    public static String errorToString(int request) {
        String errorMsg;
        switch (request) {
            case EAGAIN:
                errorMsg = "Dump has been copied, no need to copy again";
                break;
            case EINVAL:
                errorMsg = "Invalid dump";
                break;
            case ENOSPC:
                errorMsg = "No space left on device";
                break;
            case ENOREADY:
                errorMsg = "Not ready";
                break;
            case ECONNREFUSED:
                errorMsg = "Connection refused";
                break;
            case EIO:
                errorMsg = "Copy failed";
                break;
            default:
                errorMsg = "Unknown error";
                break;
        }
        return errorMsg;
    }
}