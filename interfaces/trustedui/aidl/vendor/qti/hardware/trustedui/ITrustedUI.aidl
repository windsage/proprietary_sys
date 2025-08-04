/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.trustedui;

import vendor.qti.hardware.trustedui.ITrustedInput;
import vendor.qti.hardware.trustedui.ITrustedUICallback;
import vendor.qti.hardware.trustedui.TUIResponse;
import vendor.qti.hardware.trustedui.TUIConfig;
import vendor.qti.hardware.trustedui.ITrustedInput;
import vendor.qti.hardware.trustedui.TUICreateParams;
import vendor.qti.hardware.trustedui.TUIOutputID;

/**
 * Trusted UI HAL
 *
 * ITrustedUI provides services for handling secure user interactions for usecases such as
 * -# secure payment
 * -# secure confirmation
 *
 * ITrustedUI includes methods to create and start a secure display session and attach a secure way
 * of delivering user input to the session. The interface provides means to configure a layout of
 * the client's choice to be rendered on the screen, as well as a configurable way of accepting user
 * input (touch, keyboard etc).
 */
@VintfStability
interface ITrustedUI {

    /**
     * This method initializes a new session and sets up the trusted entity on the trusted Execute
     * Engine(trustedEE) for secure execution.
     * Client must implement ITrustedInput interface to open/close/read secure user-events and
     * ITrustedUICallback to receive async notifications from the HAL.
     *
     * @param inParams Session creation parameters. Refer TUICreateParams.aidl for the description
     *                 of the parameters.
     *
     * @param input ITrustedInput interface (to open/close/read secure user-events)
     *
     * @param cb Client Callback to report the session result on input handling complete or
     *           on error.
     *
     * @param out TUIOutputID Session identifier. Valid in case of success and should be
     *                        ignored in case of an error.
     *
     * @return TUIResponse - TUI_SUCCESS on success
     *                     - TUI_FAILURE General Failure
     */
    TUIResponse createSession(in TUICreateParams inParams, in ITrustedInput input,
        in ITrustedUICallback cb, out TUIOutputID outParam);

    /**
     * This method shuts down the session and unloads the associated trusted handler.
     *
     * @param sessionId Id of the session to be closed.
     *
     * @return TUIResponse - TUI_SUCCESS on success
     *                     - TUI_FAILURE General Failure
     */
    TUIResponse deleteSession(in int sessionId);

    /**
     * This method allows the client to send custom commands to the trustedEE. This call will
     * synchronously deliver the command in commandData to the trustedEE and return the
     * response. To successfully use this method, the caller must ensure to indicate the maximum
     * size of commandData and response in the minSharedMemSize (Refer to TUICreateParams.aidl).
     *
     * @param sessionId Session identifier returned by a successful call to createSession(..)
     *
     * @param commandId A unique identifier for the command.
     *
     * @param commandData Command payload to be sent to the trustedEE
     *
     * @return TUIResponse - TUI_SUCCESS on success
     *                     - TUI_FAILURE General Failure
     */
    TUIResponse sendCommand(in int sessionId, in char commandId, in byte[] commandData,
        out byte[] response);

    /**
     * This method starts a secure UI session by switching display, input device to secure mode and
     * starts handling user inputs asynchronously. For session to start successfully, secure input
     * device initialization should be successful when requested by HAL. When this call returns,
     * the intended screen layout (user dialog as set in TUIConfig) is displayed on the screen and
     * secure input events can be delivered to HAL with appropriate payload, which are then passed
     * to the trustedEE.
     * Based on the trustedEE's response, the registered callback (set during the 'createSession')
     * notifies the response/error to the client.
     *
     * The Trusted session completes on successful user interaction completion.
     *
     * In case the startSession returns error, the client should not expect a callback from the HAL.
     *
     * @param sessionId Session identifier returned by a successful call to createSession(..)
     *
     * @param cfg List of config parameters required to set up the session. Refer TUIConfig.aidl
     *            for the description of the parameters.
     *
     * @return TUIResponse - TUI_SUCCESS on success
     *                     - TUI_ALREADY_RUNNING if a session is already active with the given cfg
     *                     - TUI_FAILURE on error, or any other error.
     *
     */
    TUIResponse startSession(in int sessionId, in TUIConfig cfg);

    /**
     * Stops the active Trusted UI session by switching the display and trusted input device back
     * to non-secure mode.
     * In case the session was actively handling input at the time of this call(abort scenario),
     * session will be stopped and the callback will report error to the client.
     *
     * @param sessionId Session identifier returned by a successful call to createSession(..)
     *
     * @return TUIResponse - TUI_SUCCESS on success
     *                     - TUI_FAILURE General Failure
     */
    TUIResponse stopSession(in int sessionId);
}
