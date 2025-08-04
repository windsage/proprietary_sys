/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.HexlpError;
import vendor.qti.hardware.hexlp.HexlpSessionResponse;
import vendor.qti.hardware.hexlp.HexlpOBDInfo;
import vendor.qti.hardware.hexlp.HexlpCustomElements;
import vendor.qti.hardware.hexlp.HexlpReturnedBufferInfo;

@VintfStability
oneway interface IHexlpCallbacks {
    /**
    * !
    * @brief       Asynchronous. Notify Client that DrainSession call is completed.
    *
    * @description This occurs after Client calls DrainSession.
    *
    * @param in          HEXLP_OK          Drain session successfully.
    *                    HEXLP_ERR         Drain session failed.
    */
    void OnDrainDone(HexlpError drain_rsp);

    /**
    * !
    * @brief       Asynchronous. Notify Client that hexlp has entered an error state.
    *
    * @description Should be called whenever hexlp encounters any unrecovered error, so that Client can handle
    *              accordingly (i.e. stop sending any command to Hexlp, bypass buffers, destroy sessions etc).
    *
    * @param in    HEXLP_ERR_FATAL    hexlp runs into FATAL error state, afterwards Client should not send
    *                                 any other calls down anymore.
    */
    void OnError(in HexlpError reported_err);

    /**
    * !
    * @brief       Asynchronous. Notify Client that FlushSession call is completed.
    *
    * @description This occurs after Client calls FlushSession.
    *
    * @param in          HEXLP_OK          Flush session successfully.
    *                    HEXLP_ERR         Flush session failed.
    */
    void OnFlushDone(HexlpError flush_rsp);

    /**
    * !
    * @brief       Asynchronous. Notify Client that ReconfigureSession call is completed.
    *
    * @description This occurs after Client calls ReconfigureSession.
    *              Reconfigure status as well as necessary parameters needed to be extracted are returned.
    *
    * @param in    Status of reconfigure handling in Hexlp.
    *
    * @param in    Reconfigure response is filled with necessary information, including:
    *              predefined/custom port information, buffer requirements and perhaps other custom elements.
    */
    void OnReconfigDone(in HexlpError reconfig_err, in HexlpSessionResponse reconfig_rsp);

    /**
    * !
    * @brief       Asynchronous. Notify Client the information of one input buffer.
    *
    * @description Notify Client the necessary information of one input buffer, which is sent to Hexlp through
    *              SPP, so that Client can handle (i.e. release) it accordingly.
    *
    * @param in    Necessary information extracted from previous SPP buffer.
    */
    void OnInputBufferDone(in HexlpReturnedBufferInfo[] hexlp_ibd_info);

    /**
    * !
    * @brief       Asynchronous. Notify Client the information of multiple input buffers.
    *
    * @description Notify Client the necessary information of multiple input buffers, which are sent to Hexlp
    *              through SPP, so that Client can handle (i.e. release) them accordingly.
    *
    * @param in    Necessary information extracted from previous SPP buffers.
    */
    void OnInputBuffersDone(in HexlpReturnedBufferInfo[] hexlp_ibds_info);

    /**
    * !
    * @brief       Asynchronous. Notify Client the information of one output buffer.
    *
    * @description Notify Client the necessary information of one output buffer, it could already
    *              be filled by Hexlp or remain empty (i.e flush), so that Client can send it
    *              out for rendering or drop it accordingly.
    *
    * @param in    Necessary information attched from Hexlp.
    */
    void OnOutputBufferDone(in HexlpOBDInfo hexlp_obd_info);

    /**
    * !
    * @brief       Asynchronous. Notify Client the information of multiple output buffers.
    *
    * @description Notify Client the necessary information of multiple output buffers, they could already
    *              be filled by Hexlp or remain empty (i.e. flush), so that Client can send them
    *              out for rendering or drop them accordingly.
    *
    * @param in    Necessary information attched from Hexlp.
    */
    void OnOutputBuffersDone(in HexlpOBDInfo[] hexlp_obds_info);

    /**
    * !
    * @brief       Asynchronous. Notify Client for custom information.
    *
    * @description Send custom elements in case hexlp wants to return any custom information after
    *              receiving SPP call with custom controls.
    *
    * @param in    Customer elements coming from hexlp.
    */
    void OnSPPCustomResponse(in HexlpCustomElements custom_rsps);
}
