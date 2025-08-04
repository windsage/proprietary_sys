/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/**
 * Supported Token types
 */
@VintfStability
@Backing(type="int")
enum TokenType {
    /**
     * APP token
     */
    IMS_APP_TOKEN = 0,
    /**
     * Auth token
     */
    IMS_AUTH_TOKEN = 1,
    /**
     * Client token
     */
    IMS_CLIENT_TOKEN = 2,
}
