/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.trustedui;

/**
 * TUIResponse code
 */
@VintfStability
@Backing(type="int")
enum TUIResponse {
    TUI_SUCCESS = 0,
    TUI_FAILURE = -1,
    TUI_LISTENER_ERROR = -2,
    TUI_ALREADY_RUNNING = -3,
    TUI_IS_NOT_RUNNING = -4,
}
