/*
 * Copyright (c) 2020 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voice.activation.aidl;

import android.os.Bundle;
import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.activation.aidl.IGenerateSoundModelCallback;
import com.quicinc.voice.activation.aidl.IUtteranceCallback;

/*
 * The enrollment APIs are used during voice enrollment. This allows the users to go through
 * a defined number of steps and enroll their voice on the device.
 */
interface IEnrollmentService {

    /*
     * This is the first step of the training, where we ask to start the user voice enrollment process.
     * Once this method is called, the QVA must set up their environment for enrollment and
     * respond with a success or an error using the provided callback.
     *
     * @param params Other parameters needed for the invocation of this method.
     * @param soundModelFileDescriptor object used to transfer sound model data.
     * @param resultCallback a callback used to indicate if Voice Enrollment started successfully or not
     */
    oneway void startUserVoiceEnrollment(in Bundle params,
        in ParcelFileDescriptor soundModelFileDescriptor, IResultCallback resultCallback);

    /*
     * Method to cancel the voice enrollment process. This method may be called after an
     * enrollment has started, and during, before, or after any numbers of utterance trainings.
     * All utterance trainings in the current enrollment session will be deleted.
     *
     * @param resultCallback callback to be called to indicate if the enrollment cancellation
     *                       was successful or not.
     * @param params Parameters containers. This field exists to avoid AIDL modifications in case
     *               a new parameter needs to be added to this method.
     */
    oneway void cancelUserVoiceEnrollment(IResultCallback resultCallback, in Bundle params);

    /*
     * Method to finish the voice enrollment process. This method will be called after the user went
     * through all utterances and finished the training.
     *
     * @param generateSoundModelCallback used to transfer trained sound model.
     * @param params Parameters containers. This field exists to avoid AIDL modifications in case
     *               a new parameter needs to be added to this method.
     */
    oneway void commitUserVoiceEnrollment(
        IGenerateSoundModelCallback generateSoundModelCallback, in Bundle params);

    /*
     * Method to finish the voice enrollment process. This method will be called after the user went
     * through all utterances and finished the training. As a result, a voice model must be created and
     * loaded into the DSP.
     *
     * @param params Parameters containers. This field exists to avoid AIDL modifications in case
     *               a new parameter needs to be added to this method.
     */
    oneway void finishUserVoiceEnrollment(in Bundle params);

    /*
     * This is the method used to request the Voice APK to collect an utterance for voice enrollment.
     * This method will be called as many times as the number of utterances in provided by getUtterancesInfo().
     *
     * @param params Other parameters needed for the invocation of this method.
     * @param utteranceCallback Object used to provide feedback to the caller about the utterance collection.
     *                           This feedback is used to show UI to the customer during the enrollment.
     */
    oneway void startUtteranceTraining(IUtteranceCallback utteranceCallback, in Bundle params);

    /*
     * If in progress, cancels a single utterance enrollment. This will result in the current
     * utterance being cancelled and user will have to retrain the current utterance. All previous
     * utterances are unaffected.
     *
     * @param resultCallback Object used to provide feedback to the caller about the success or
     *                       failure of the cancellation request
     * @param params Parameters containers. This field exists to avoid AIDL modifications in case
     *               a new parameter needs to be added to this method.
     */
    oneway void cancelUtteranceTraining(IResultCallback resultCallback, in Bundle params);

    /*
     * Method used to retrieve information about the utterances needed for voice enrollment.
     *
     * @param params Other parameters needed for the invocation of this method.
     *
     * @return a list with informations about the utterances that need to be collected from the user,
     *         each utterance information in a Bundle.
     */
    List<Bundle> getUtterancesInfo(in Bundle params);

    /*
     * Method used by the client to transfer clean recordings to this service.
     * Transfer 1 recording per called.
     * Client needs to call this method N(N = clean recordings count) times.
     *
     * @param recording shortArray of per clean recording.
     */
    void transferRecording(in Bundle params);

    /*
     * Method used to remove the unique platform UV data.
     *
     * @param resultCallback  a callback used to indicate if remove platform UV data successfully or not
     */
    oneway void removeGeneralUV(IResultCallback resultCallback);

    /*
     * Whether platform UV enrolled or not.
     *
     * @return True if platform UV enrolled.
     */
    boolean isGeneralUVEnrolled();
}
