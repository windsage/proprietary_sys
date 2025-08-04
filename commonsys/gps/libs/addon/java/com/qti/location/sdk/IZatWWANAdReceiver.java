/* ======================================================================
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/
package com.qti.location.sdk;

/*
 * <p>Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.</p>
 * <p>All Rights Reserved.</p>
 * <p>Confidential and Proprietary - Qualcomm Technologies, Inc</p>
 * <br/>
 * <p><b>IZatWWANAdReceiver</b> interface @version 1.0.0 </p>
 */

/** @addtogroup IZatWWANAdReceiver
@{ */

/** API for
 * injecting WWAN positioning assistance data to a Qualcomm Location framework.
 */
public abstract class IZatWWANAdReceiver {

    public enum IZatWWANADType {
        LOW_ACCURACY,
        HIGH_ACCURACY
    }

    protected IZatWWANAdRequestListener mReqListener;

    /**
     * <p>
     * Constructor - IZatWWANAdReceiver. </p>
     *
     * @param  listener Listener to receive WWAN AD request from
     *         Qualcomm Location framework.
     *         This parameter cannot be NULL, otherwise
     *         a {@link IZatIllegalArgumentException} is
     *         thrown.
     * @throws IZatIllegalArgumentException The listener parameter is NULL.
     * @return
     * None.
     */
    protected IZatWWANAdReceiver(IZatWWANAdRequestListener listener) {
        if (null == listener) {
            throw new IZatIllegalArgumentException(
                    "Unable to obtain IZatWWANAdReceiver instance");
        }
        mReqListener = listener;
    }

    /**
     * Push the WWAN assistance data.
     * <p>
     * This method pushes WWAN AD in response to the WWAN AD request
     * received via onWWANAdRequest.
     * Permission required: com.qualcomm.permission.IZAT
     * </p>
     *
     * @param requestId request ID from underlying WWAN positioning service.
     *        request ID should match previous ID received via onWWANAdRequest.
     * @param status indicates if WWAN AD provider successfully provides data.
     * @param respType WWAN assistance data response type, high accuracy or low accuracy.
     * @param respPayload byte array of response payload. This payload is encoded
     *        and encrypted.
     *
     * @return
     * None.
     */
    public abstract void pushWWANAssistanceData(int requestId, boolean status,
            IZatWWANADType respType, byte[] respPayload);

/** @} */ /* end_addtogroup IZatWWANAdReceiver */

/** @addtogroup IZatWWANAdReceiver
@{ */

    /**
     * Interface class IZatWWANAdRequestListener.
     *
     * <p>This interface
     * receives WWAN assistance data request from Qualcomm location framework.</p>
     */
    public interface IZatWWANAdRequestListener {

        /**
         * WWAN Assistance data request to WWAN assistance data provider.
         * <p>
         * This API is called by the underlying service back
         * to Java service that can provide assistance data.</p>
         *
         * @param requestId request ID from underlying WWAN positioning service.
         * @param reqPayload byte array of request payload. This payload is encoded
         *        and encrypted.
         *
         * @return
         * None.
         */
        void onWWANAdRequest(int requestId, byte[] reqPayload);
    }

/** @} */ /* end_addtogroup IZatWWANAdReceiver */
}
