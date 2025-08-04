/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.IHexlpCallbacks;

@VintfStability
parcelable HexlpCreateSessionParams {
    IHexlpCallbacks cb;
    boolean secure;
    String[] serv_ext_libs;
}
