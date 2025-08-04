/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.am;

import vendor.qti.hardware.radio.am.IQcRilAudioRequest;
import vendor.qti.hardware.radio.am.AudioError;
import vendor.qti.hardware.radio.am.IQcRilAudioResponse;

@VintfStability
interface IQcRilAudio {

    /**
     * Set callback for QcRilAudio requests, and return response interface
     * for Telephony to send back respective reponses.
     *
     * @param IQcRilAudioRequest callback Object contains QcRil Audio Request functions.
     *
     * @return IQcRilAudioResponse callback Object contains QcRil Audio Response functions.
     *
     */
    IQcRilAudioResponse setRequestInterface(in IQcRilAudioRequest callback);

    /**
     * Set Error for Audio
     *
     * @param AudioError Enum contains values representing Audio error codes
     */
    oneway void setError(in AudioError errorCode);
}
