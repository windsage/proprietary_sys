/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CbNumInfo;
import vendor.qti.hardware.radio.ims.CfData;
import vendor.qti.hardware.radio.ims.SsInfoData;
import vendor.qti.hardware.radio.ims.SsRequestType;
import vendor.qti.hardware.radio.ims.SsServiceType;
import vendor.qti.hardware.radio.ims.SsTeleserviceType;

/**
 * Data structure containing details of SS request and response information.
 */
@VintfStability
parcelable StkCcUnsolSsResult {
    SsServiceType serviceType = SsServiceType.INVALID;
    SsRequestType requestType = SsRequestType.INVALID;
    SsTeleserviceType teleserviceType = SsTeleserviceType.INVALID;
    int serviceClass;
    int result;
    SsInfoData[] ssInfoData;
    CfData[] cfData;
    CbNumInfo[] cbNumInfo;
}

