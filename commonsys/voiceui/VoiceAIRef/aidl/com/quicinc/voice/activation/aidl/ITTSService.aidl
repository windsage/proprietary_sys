/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.activation.aidl;

import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.activation.aidl.ISpeakProgressCallback;

/*
 * A service that provides Text-to-speech ability
 *
 */
interface ITTSService {

    /*
     * initialize TTS engine with specifid language,
     *
     * @param language set language for TTS, "English" by default
     * @param callback callback of TTS Engine service initialization result
     *
     */
    oneway void initTTSEngine(String language, IResultCallback callback);

    /*
     * Speaks the text using the specified queuing strategy and speech parameters.
     *
     * @param params Contains parameters like text to be spoken, queue mode, pitch, speech rate...
     * @param callback Optional, Client receives callback of TTS speaking progress
     *
     */
    oneway void startSpeak(in Bundle params, ISpeakProgressCallback callback);

    /*
     * Interrupts the current utterance and discards other utterances in the queue
     *
     */
    oneway void stopSpeak();

    /*
     * Deinitialization of TTS engine service
     *
     */
    oneway void deinitTTSEngine();
}