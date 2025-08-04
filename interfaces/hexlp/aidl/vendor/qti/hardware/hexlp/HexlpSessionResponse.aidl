/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpError;
import vendor.qti.hardware.hexlp.HexlpPredefinedPortResponse;
import vendor.qti.hardware.hexlp.HexlpCustomPortResponse;
import vendor.qti.hardware.hexlp.HexlpBufferRequirementResponse;
import vendor.qti.hardware.hexlp.HexlpCustomElements;

@VintfStability
parcelable HexlpSessionResponse {
    HexlpPredefinedPortResponse[] predefined_port_rsps;
    HexlpCustomPortResponse[] custom_port_rsps;
    HexlpBufferRequirementResponse[] buf_req_rsps;
    HexlpCustomElements custom_ctrls_rsps;
}
