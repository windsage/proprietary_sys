/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpByteValCustomElement;
import vendor.qti.hardware.hexlp.HexlpCharValCustomElement;
import vendor.qti.hardware.hexlp.HexlpBoolValCustomElement;
import vendor.qti.hardware.hexlp.HexlpIntValCustomElement;
import vendor.qti.hardware.hexlp.HexlpFloatValCustomElement;
import vendor.qti.hardware.hexlp.HexlpDoubleValCustomElement;
import vendor.qti.hardware.hexlp.HexlpLongValCustomElement;
import vendor.qti.hardware.hexlp.HexlpStringValCustomElement;

@VintfStability
parcelable HexlpCustomElements {
    HexlpByteValCustomElement[]    byte_custom_element;
    HexlpCharValCustomElement[]    char_custom_element;
    HexlpBoolValCustomElement[]    bool_custom_element;
    HexlpIntValCustomElement[]     int_custom_element;
    HexlpFloatValCustomElement[]   float_custom_element;
    HexlpDoubleValCustomElement[]  double_custom_element;
    HexlpLongValCustomElement[]    long_custom_element;
    HexlpStringValCustomElement[]  string_custom_element;
}
