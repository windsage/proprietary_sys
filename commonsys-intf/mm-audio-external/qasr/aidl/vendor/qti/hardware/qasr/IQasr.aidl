/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.qasr;

import vendor.qti.hardware.qasr.IQasrGlobalCallback;
import vendor.qti.hardware.qasr.IQasrCallback;
import vendor.qti.hardware.qasr.QasrModel;
import vendor.qti.hardware.qasr.QasrConfig;
import vendor.qti.hardware.qasr.QasrParameter;

@VintfStability
interface IQasr {
    /**
     * Events not tied to a specific session are received through this callback.
     *
     * @param callback An interface to receive global event callbacks.
     */
    void registerGlobalCallback(in IQasrGlobalCallback callback);

    /**
     * Creates a session with given sound model and returns a session handle.
     *
     * @param model ASR model.
     * @param callback An interface to receive global event callbacks.
     * @return If successful, a unique handle with positive value is returned
     *      for use by the client controlling this session. Else, a service
     *      specific error {@link QasrStatus} is returned.
     *
     */
    int initSession(in QasrModel model, in IQasrCallback callback);

    /**
     * Start ASR to listen to the MIC and receive the text transcription.
     *
     * If Partial transcription is not enabled, the service sends the final
     * transcription and stops processing. Call startListening() to start
     * receiving new transcription.
     *
     * If Partial transcription is enabled, the service continously sends the
     * events with partial transcription until the final transcription is sent.
     * Once the final transcription is sent, call startListening() to start
     * receiving new transcription.
     *
     * If {@link QasrConfig.outputBufferMode} is enabled, the service keeps
     * sending the events each when the buffer is full. Client needs to call
     * stopListening() to stop receiving transcription.
     *
     * @param handle Session handle
     * @param configData ASR configuration parameters
     * @return If failed, Service Specific error {@link QasrStatus} is returned.
     */
    void startListening(in int handle, in QasrConfig configData);

    /**
     * Set parameters with applicable payload.
     *
     * @param handle Session handle
     * @param parameters parameter to set {@link QasrParameter}
     * @return If failed, Service Specific error {@link QasrStatus} is returned.
     */
    void setParameter(in int handle, in QasrParameter parameters);

    /**
     * Stop ASR Listening to MIC.
     *
     * @param handle Session handle
     * @return Service Specific status {@link QasrStatus} is returned.
     */
    void stopListening(in int handle);

    /**
     * Release ASR session.
     *
     * @param handle Session handle
     * @return If failed, Service Specific error {@link QasrStatus} is returned.
     */
    void releaseSession(in int handle);

    /**
     * Get supported input and output languages
     *
     * @param handle Session handle
     * @param out param, to return list of supported languages
     */
    String[] getSupportedLanguages(in int handle);
}
