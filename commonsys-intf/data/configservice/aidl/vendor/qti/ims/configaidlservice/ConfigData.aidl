/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/**
 * The RCS configuration data should be an xml file
 * taken from the <RCSConfig> section of the device/carrier config xml.
 * It may be passed as a uint8_t buffer.
 * isCompressed Flag is required to be True if data is compressed.
 * Only gzip compression is supported.
 */
@VintfStability
parcelable ConfigData {
    boolean isCompressed;
    byte[] config;
}
