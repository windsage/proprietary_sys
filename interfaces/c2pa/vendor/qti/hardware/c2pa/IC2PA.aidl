/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.c2pa;

import android.hardware.common.Ashmem;
import vendor.qti.hardware.c2pa.C2PADataTypePair;
import vendor.qti.hardware.c2pa.EnrollResponse;
import vendor.qti.hardware.c2pa.SignResponse;
import vendor.qti.hardware.c2pa.ValidateResponse;

@VintfStability
interface IC2PA {
    /**
     *
     * This method is used to enable C2PA services by enrolling this device and acquiring the
     * required certificates.
     *
     * @param in configParams Configuration parameters like apiKey, etc. used for enrollment.
     *
     * @return EnrollResponse Response for signMedia whether enrollment succeeded, failed, etc.
     */
    EnrollResponse enroll(in List<C2PADataTypePair> configParams);

    /**
     * This method is used to sign the input media file, some fixed assertions, custom assertions
     * into C2PA claims and return the signed media in a container format comprising of it all,
     * example JPEG for Camera Snapshots.
     *
     * @param in mediaFile Input media data file, to be signed with C2PA claim
     *
     * @param in configParams Media or other configuration parameters used for signing.
     *
     * @param in assertions Standard or custom assertion values to be added to media.
     *
     * @param out signedMediaFile Output media data signed with C2PA claim data.
     *
     * @return SignResponse Response for signMedia whether signing succeeded, failed, etc.
     */
    SignResponse signMedia(in Ashmem mediaFile, in List<C2PADataTypePair> configParams,
                           in List<C2PADataTypePair> assertions, out Ashmem signedMediaFile);

    /**
     * This method is used to validate the C2PA claims present inside the media file and
     * returns the validation report along with few of the requested assertions.
     *
     * @param in Input media file as input and validate the media.
     *
     * @param in inParams Media or other configuration parameters used for validation.
     *
     * @param out outParams Information including claim report returned if validation succeeds.
     *
     * @return ValidateResponse Response for validateMedia whether media is valid, invalid, etc.
     */
    ValidateResponse validateMedia(in Ashmem mediaFile, in List<C2PADataTypePair> inParams,
                                   out List<C2PADataTypePair> outParams);
}
