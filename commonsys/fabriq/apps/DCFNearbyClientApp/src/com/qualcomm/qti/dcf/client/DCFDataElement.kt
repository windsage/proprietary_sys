/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

// Google provide 128 DE Tags for test use-cases and defined the first DE as TEST_DE_BEGIN.
// The range is [Integer.MAX_VALUE - 127(2147483520), Integer.MAX_VALUE(2147483647)].
const val TEST_DE_BEGIN = Integer.MAX_VALUE - 127

const val DE_TAG_DEVICE_ID = TEST_DE_BEGIN
const val DE_TAG_DEVICE_INFO = TEST_DE_BEGIN + 1
const val DE_TAG_DEVICE_ADDRESS = TEST_DE_BEGIN + 2
const val DE_TAG_BATTERY_STATUS = TEST_DE_BEGIN + 3
const val DE_TAG_WIFI_DISPLAY = TEST_DE_BEGIN + 4
const val DE_TAG_USER_ACTIVITY = TEST_DE_BEGIN + 5

data class DCFDataElement(
    val tag: Int,
    val data: ByteArray,
)
