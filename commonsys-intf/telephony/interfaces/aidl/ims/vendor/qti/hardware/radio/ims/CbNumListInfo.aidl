/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CbNumInfo;

@VintfStability
parcelable CbNumListInfo {
    /*
     * only voice class i.e 1 is supported
     */
    int serviceClass;
    CbNumInfo[] cbNumInfo;
}