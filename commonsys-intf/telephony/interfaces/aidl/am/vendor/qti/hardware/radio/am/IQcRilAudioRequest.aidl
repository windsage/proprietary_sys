/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.am;

@VintfStability
interface IQcRilAudioRequest {

    /**
     * QcRil invokes this request to get Audio parameters from Telephony.
     *
     * @param params to query Audio parameters
     *
     */
    oneway void queryParameters(in int token, in String params);

    /**
     * QcRil invokes this request to set Audio parameters in upper layers via Telephony.
     *
     * @param params to set as Audio parameters
     *
     */
    oneway void setParameters(in int token, in String params);
}
