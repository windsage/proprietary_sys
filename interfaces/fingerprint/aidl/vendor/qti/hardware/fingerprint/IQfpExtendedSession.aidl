/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.fingerprint;

import vendor.qti.hardware.fingerprint.AuthToken;
import vendor.qti.hardware.fingerprint.EnrollRecord;
import vendor.qti.hardware.fingerprint.IQfpExtendedSessionCallback;
import vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal;
import vendor.qti.hardware.fingerprint.Status;

@VintfStability
interface IQfpExtendedSession {

    /**
     * Get configuration parameters - wake up display behavior and
     * duplicate enrollment template accept/reject
     *
     * @param  name   name of configuration parameter to set.
     * @return value of configuration parameter to set.
     */
    String getConfigValue(in String name);

    /**
     * Set configuration parameters - wake up display behavior and
     * duplicate enrollment template accept/reject
     *
     * @param  name   name of configuration parameter to set.
     * @param  value  value of configuration parameter to set.
     * @return SUCCESS on success or FAILURE on failure.
     */
    Status setConfigValue(in String name, in String value);

    /**
     * Get complete enrollment record from enrollee id.
     *
     * Each client is expected to call init interface and acquire a context
     * handle before exercising any other interface.
     *
     * @param  enrolleeId enrollee id.
     * @return     enrollment record.
     */
    EnrollRecord getEnrollRecord(in String enrolleeId);

    /**
     * Generic interface to process request.
     *
     * @param  request  request as byte array.
     * @return response as byte array.
     */
    byte[] processRequest(in byte[] request);

    /**
     * Generic interface to process asynchronous request.
     *
     * @param  request      request as byte array.
     * @return valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal asyncProcessRequest(in byte[] request);

    /**
     * Get the Debug Data for captured image.
     *
     * @param  cb     asynchronous callback to fire.
     * @return SUCCESS on success or FAILURE on failure.
     */
    Status registerDebugDataCallback(in IQfpExtendedSessionCallback cb);

    /**
     * Start capture of live data for streaming.
     *
     * @param   mode        capture mode.
     * @param   forceLUT    LUT to be used for image capture.
     * @return valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal captureImage(in int mode, in int forceLUT);

    /**
     * Open connection with Fingerprint TA.
     *
     * @return  response as byte array.
     */
    byte[] openFramework();

    /**
     * Close connection with Fingerprint TA.
     *
     * @return SUCCESS on success or FAILURE on failure.
     */
    Status closeFramework();

    /**
     * Handle Vendor command.
     *
     * @param   request     request as byte array.
     * @return  response as byte array.
     */
    byte[] handlePVCmd(in byte[] request);

    /**
     * Toggle the state of IRQ.
     *
     * @param   irq         IRQ to be toggled.
     * @param   enable      Enable/Disable the IRQ.
     * @return SUCCESS on success or FAILURE on failure.
     */
    Status toggleIRQ(in int irq, in int enable);

    /**
     * Handle Calibration test request synchronously.
     *
     * @param   request     request as byte array.
     * @return  response as byte array.
     */
    byte[] calibTest(in byte[] request);

    /**
     * Capture air image or idle sensor data.
     *
     * @param   version     version of the AIC request.
     * @param   flags       flags to determine AIC behavior.
     * @return  valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal captureAirImage(in int version, in int flags);

    /**
     * Capture HRM information.
     *
     * @param   version     version of the HRM Capture request.
     * @return  valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal captureHRM(in int version);

    /**
     * Tie/Untie the fingers of a user.
     *
     * @param   version     version of the Tie/Untie request.
     * @param   enrolleeId  user/enrollee Id.
     * @param   token       authentication token.
     * @param   type        determines tie or untie type.
     * @param   tieId       Id of the tied fingers.
     * @param   numFingers  number of fingers.
     * @param   fingerIds   finger Ids of the fingers to tie/untie.
     * @return  valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal tieUntie(in int version, in String enrolleeId,
                                            in AuthToken token, in int type, in int tieId,
                                            in int numFingers, in int[] fingerIds);

    /**
     * Get the Tied fingers list of a user.
     *
     * @param   version     version of the Tie/Untie request.
     * @param   enrolleeId  user/enrollee Id.
     * @return  valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal retrieveTiedFingerList(in int version, in String enrolleeId);

    /**
     * Updates QFS Setting. Also handles pre and post QFS setting update.
     *
     * @param   version     version of the Updates QFS Setting request.
     * @param   token       authentication token.
     * @param   settingId   Id of the QFS setting.
     * @param   valueLen    setting length.
     * @param   value       setting value.
     * @return  valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal updateQfsSetting(in int version, in AuthToken token,
                                                    in int settingId, in int valueLen,
                                                    in byte[] value);

    /**
     * Retrieve a QFS Setting.
     *
     * @param   version     version of the QFS setting request.
     * @param   settingId   Id of the QFS setting.
     * @param   challengeId challenge Id authentication.
     * @return  valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal retrieveQfsSetting(in int version, in int settingId,
                                                      in long challengeId);

    /**
     * Force Sense start request.
     * @param   version     version of the forcesense request.
     * @param   mode        supports following commands:
     *                      START_FORCESENSE
     *                      START_FORCESENSE_CAL
     * @return  valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal startForceSense(in int version, in int mode);

    /**
     * Force Sense request.
     * @param   version     version of the forcesense request.
     * @param   cmd         forcesense cmd Id.
     * @param   subcmd      forcesense stage/sub-command Id.
     * @param   data        forcesense data input for each cmd/subcmd.
     * @return SUCCESS on success or FAILURE on failure.
     */
    Status notifyForceSense(in int version, in int cmd, in int subcmd, in byte[] data);

    /**
     * Set Touch events info.
     * @param   version     version of the touch event request.
     * @param   numEvents   number of touch events.
     * @param   eventLength size of each touch event.
     * @param   events      touch events info.
     * @return SUCCESS on success or FAILURE on failure.
     */
    Status setTouchInfo(in int version, in int numEvents, in int eventLength, in byte[] events);

    /**
     * Stylus start request.
     * @param   version     version of the Stylus request.
     * @return  valid IQfpExtendedCancellationSignal on SUCCESS, null on failure.
     */
    IQfpExtendedCancellationSignal startStylus(in int version);

    /**
     * Stylus request.
     * @param   version     version of the Stylus request.
     * @param   cmd         supports following commands:
     *                      REQ_STYLUS_INIT
     *                      REQ_STYLUS_DRAW
     *                      REQ_STYLUS_SBGE
     *                      REQ_STYLUS_DEINIT
     * @return SUCCESS on success or FAILURE on failure.
     */
    Status notifyStylus(in int version, in int cmd, in byte[] data);

}
