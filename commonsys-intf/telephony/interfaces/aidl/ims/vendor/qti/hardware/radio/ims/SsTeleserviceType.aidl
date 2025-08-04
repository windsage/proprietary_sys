/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum SsTeleserviceType {
    INVALID,
    ALL_TELE_AND_BEARER_SERVICES,
    ALL_TELESEVICES,
    TELEPHONY,
    ALL_DATA_TELESERVICES,
    SMS_SERVICES,
    ALL_TELESERVICES_EXCEPT_SMS,
}
