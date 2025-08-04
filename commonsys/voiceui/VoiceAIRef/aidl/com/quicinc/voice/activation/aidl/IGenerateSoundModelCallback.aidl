/*
 * Copyright (c) 2020 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voice.activation.aidl;

import android.os.Bundle;

/*
 * Used to send sound model data to IGenerateSoundModelService consumers.
 */
interface IGenerateSoundModelCallback {

    /*
     * Method used to send sound model data to IGenerateSoundModelService consumers.
     *
     * @param soundModelFileDescriptor The FileDescriptor that the API consumer must
     *                                 read to retrieve the sound model data.
     * @param params Other parameters containers.
     */
    oneway void onTrainSoundModelSuccess(
        in ParcelFileDescriptor soundModelFileDescriptor, in Bundle params);

    /*
     * Method to be used to notify consumers that have registered a IGenerateSoundModelCallback
     * that the sound model data will not be sent, since generate sound model has not succeeded.
     *
     * @param params Parameter container.
     */
    oneway void onTrainSoundModelFailure(in Bundle params);
}
