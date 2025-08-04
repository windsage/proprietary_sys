/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.am;
import vendor.qti.hardware.radio.am.AudioError;

@VintfStability
interface IQcRilAudioResponse {

    /**
     * Response API that Telephony invokes corresponding to IQcRilAudioRequest API
     * when request to query audio parameters is sent by QcRil.
     *
     * @param params - string value of Audio parameters
     *
     */
    oneway void queryParametersResponse(in int token, in String params);

    /**
     * Response API that Telephony invokes corresponding to IQcRilAudioRequest API
     * when request to set audio parameters is sent by QcRil.
     *
     * @param errorCode - int value indicating the status
     *
     */
     oneway void setParametersResponse(in int token, in AudioError errorCode);
}
