/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpPortDirection;
import vendor.qti.hardware.hexlp.HexlpPredefinedPortType;

@VintfStability
parcelable HexlpPredefinedPortResponse {
    int port_id;
    HexlpPortDirection direction;
    HexlpPredefinedPortType port_type;
}
