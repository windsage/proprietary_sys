/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallFailCauseResponse;

/**
 * HangupRequestInfo is used to end the call.
 * Lower layers will process HangupRequestInfo only if connIndex is > 0.
 */
@VintfStability
parcelable HangupRequestInfo {
    int connIndex;
    boolean multiParty;
    String connUri;
    int conf_id;
    CallFailCauseResponse failCauseResponse;
}

