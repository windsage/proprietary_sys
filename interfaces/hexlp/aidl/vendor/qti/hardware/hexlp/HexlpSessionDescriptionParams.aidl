/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpPortParam;

@VintfStability
parcelable HexlpSessionDescriptionParams {
    HexlpPortParam[] port_params;
}
