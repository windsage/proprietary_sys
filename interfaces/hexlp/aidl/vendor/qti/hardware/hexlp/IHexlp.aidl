/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpError;
import vendor.qti.hardware.hexlp.HexlpSessionResponse;
import vendor.qti.hardware.hexlp.HexlpCreateSessionParams;
import vendor.qti.hardware.hexlp.HexlpSessionDescriptionParams;
import vendor.qti.hardware.hexlp.HexlpSPPBuffer;
import vendor.qti.hardware.hexlp.HexlpCustomElements;
import vendor.qti.hardware.hexlp.HexlpMemBuffer;

@VintfStability
interface IHexlp {
    /**
     * !
     * @brief       Synchronous. Preload service or provider extensions ahead of time.
     *
     * @description Load time-consuming extensions in order to reduce start latency in runtime.
     *
     * @param in    Extensible array of extension lib names.
     *
     * @param out   Extensible array of failure indexes which are NOT loaded successfully.
     *
     * @param return       HEXLP_OK          Load all extensions successfully.
     *                     HEXLP_ERR         Load none or part of extensions NOT successfully.
     */
    HexlpError LoadExtensions(in String[] preload_extensions, out int[] failure_indexes);

    /**
     * !
     * @brief       Synchronous. Unload service or provider extensions that are unnecessarily persistent.
     *
     * @description Opposite operations of LoadExtensions.
     *
     * @param in    Extensible array of extension lib names.
     *
     * @param out   Extensible array of failure indexes which are NOT unloaded successfully.
     *
     * @param return       HEXLP_OK          Unoad all extensions successfully.
     *                     HEXLP_ERR         Unload none or part of extensions NOT successfully.
     */
    HexlpError UnloadExtensions(in String[] unload_extensions, out int[] failure_indexes);

    /**
     * !
     * @brief       Synchronous. Create a new session.
     *
     * @description Hexlp will create a Framework Session object on the Service Framework and load the Service Extension
     *              after calling this API.
     *
     * @param in            Configuration parameters, including IHexlpCallbacks instance, secure/non-secure flag, extensible
     *                      array of service extension lib names.
     *
     * @param in(optional)  Reserved for any other possible static configs.
     *
     * @param return        HEXLP_OK          Create session successfully.
     *                      HEXLP_ERR         Create session NOT successfully.
     */
    HexlpError CreateSession(in HexlpCreateSessionParams create_session_params, in @nullable HexlpCustomElements custom_ctrls);

    /**
     * !
     * @brief       Synchronous. Destroy the already created session.
     *
     * @description Destroy the already created Framework Session object and unload the Service Extension associated
     *              with the Session (if unused by other Sessions).
     *
     * @param return       HEXLP_OK          Destroy session successfully.
     *                     HEXLP_ERR         Destroy session NOT successfully.
     */
    HexlpError DestroySession();

    /**
     * !
     * @brief       Synchronous. Open the session with valid configurations.
     *
     * @description Open the Session with port configurations and valid use-case parameters.
     *
     * @param in    HexlpSessionDescriptionParams including port configuration parameters(i.e. resolution, format etc).
     *              HexlpCustomElements contains valid use-case parameters and session properties(optional).
     *
     * @param out   Predefined/ port information.
     *              Buffer requirements.
     *              Optional custom elements.
     *
     * @param return   Handling status of this call:
     *                     HEXLP_OK                      Open session successfully.
     *                     HEXLP_ERR                     Open session failed due to unknown error.
     *                     HEXLP_ERR_INVALID_CFG         Open session failed due to invalid use-case parameters.
     *                     HEXLP_ERR_HW                  Open session failed due to HW errors.
     *                     HEXLP_ERR_RESOURCES           Open session failed due to NOT enough resources.
     */
    HexlpError OpenSession(in HexlpSessionDescriptionParams open_session_params, in HexlpCustomElements custom_ctrls, out HexlpSessionResponse open_session_rsps);

    /**
     * !
     * @brief       Synchronous. Close the already opened Session.
     *
     * @description Opposite operations of OpenSession.
     *
     * @param return       HEXLP_OK          Close session successfully.
     *                     HEXLP_ERR         Close session NOT successfully.
     */
    HexlpError CloseSession();

    /**
     * !
     * @brief       Asynchronous. Provide Client resources and optional custom controls for processing.
     *
     * @description This should only be called after OpenSession successfully.
     *
     * @param in              One SPP buffer.
     *
     * @param in(optional)    Dynamic controls (i.e. algo parameter tunings, session properties etc).
     *
     * @param return       HEXLP_OK          Send processing parameters successfully.
     *                     HEXLP_ERR         Send processing parameters NOT successfully.
     */
    HexlpError SendProcessingParameters(in HexlpSPPBuffer spp_buf, in @nullable HexlpCustomElements custom_ctrls);

    /**
     * !
     * @brief       Asynchronous. Provide multiple resources and optional custom controls for processing.
     *
     * @description This should only be called after OpenSession successfully.
     *
     * @param in              Array of SPP buffers.
     *
     * @param in(optional)    Dynamic controls (i.e. algo parameter tunings, session properties etc).
     *
     * @param return       HEXLP_OK          Send multiple processing parameters successfully.
     *                     HEXLP_ERR         Send multiple processing parameters NOT successfully.
     */
    HexlpError SendMultipleProcessingParameters(in HexlpSPPBuffer[] spp_bufs, in @nullable HexlpCustomElements custom_ctrls);

    /**
     * !
     * @brief       Asynchronous. Reconfigure the session with new port configurations or new use-cases.
     *
     * @description ReconfigureSession call supports dynamic port changing or use-case switching.
     *
     * @param in    New session port configuration parameters and possible new use-case parameters.
     *
     * @param return       HEXLP_OK          Reconfigure session successfully.
     *                     HEXLP_ERR         Reconfigure session unsuccessfully.
     */
    HexlpError ReconfigureSession(in HexlpSessionDescriptionParams reconfig_session_params, in @nullable HexlpCustomElements custom_ctrls);

    /**
     * !
     * @brief       Asynchronous. Flush the session.
     *
     * @description When flush is called, all SPPs are expected to return back.
     *
     * @param return       HEXLP_OK          Flush session successfully.
     *                     HEXLP_ERR         Flush session unsuccessfully.
     */
    HexlpError FlushSession();

    /**
     * !
     * @brief       Asynchronous. Drain the session.
     *
     * @description When drain is called, all in-flight SPPs are expected to be processed.
     *
     * @param return       HEXLP_OK          Drain session successfully.
     *                     HEXLP_ERR         Drain session unsuccessfully.
     */
    HexlpError DrainSession();

    /**
     * !
     * @brief       Synchronous. Premap buffers ahead of time.
     *
     * @description Premap buffers ahead of time to reduce runtime mapping overhead.
     *              No need to attach Fence information with this call.
     *
     * @param in    Extensible Extensible array of buffers need to be pre-mapped.
     *
     * @param out   Extensible Extensible array of cookies which are NOT pre-mapped successfully.
     *
     * @param return       HEXLP_OK          Successfully handled by Hexlp.
     *                     HEXLP_ERR         Unsuccessfully handled by Hexlp.
     */
    HexlpError PreMapBuffers(in HexlpMemBuffer[] pre_mapped_bufs, out int[] failure_cookies);

    /**
     * !
     * @brief       Synchronous. Unmap buffers which are pre-mapped.
     *
     * @description Unmap buffers which are pre-mapped previously. Only cookies need to be sent
     *              during this time.
     *
     * @param in    Extensible Extensible array of buffer cookies need to be unmapped.
     *
     * @param out   Extensible Extensible array of cookies which are NOT unmapped successfully.
     *
     * @param return       HEXLP_OK          Successfully handled by Hexlp.
     *                     HEXLP_ERR         Unsuccessfully handled by Hexlp.
     */
    HexlpError UnMapBuffers(in int[] unmapped_buf_cookies, out int[] failure_cookies);
}
