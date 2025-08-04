/* ======================================================================
*  Copyright (c) 2021 Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/
package com.qti.location.sdk;

import android.location.Location;

/*
 * <p>Copyright (c) 2021 Qualcomm Technologies, Inc.</p>
 * <p>All Rights Reserved.</p>
 * <p>Confidential and Proprietary - Qualcomm Technologies, Inc</p>
 * <br/>
 * <p><b>IZatAltitudeReceiver</b> interface @version 1.0.0 </p>
 */

/** @addtogroup IZatAltitudeReceiver
@{ */

/** API for
 * injecting accuracy altitude of a location to a Qualcomm Location framework.
 */
public abstract class IZatAltitudeReceiver {
    protected IZatAltitudeReceiverResponseListener mResponseListener;

    /**
     * <p>
     * Constructor - IZatAltitudeReceiver. </p>
     *
     * @param listener Listener to receive altitude Receiver
     *         responses. This parameter cannot be NULL, otherwise
     *         a {@link IZatIllegalArgumentException} is
     *         thrown.
     * @throws IZatIllegalArgumentException The listener parameter is NULL.
     * @return
     * None.
     */
    protected IZatAltitudeReceiver(IZatAltitudeReceiverResponseListener listener) {
        if (null == listener) {
            throw new IZatIllegalArgumentException(
                    "Unable to obtain IZatAltitudeReceiver instance");
        }
        mResponseListener = listener;
    }

    /**
     * Push the accuracy altitude based on latitude/longitude.
     * <p>
     * This pushes the location which includes the accuracy altitude
     * to underlying location service.
     * </p>
     *
     * @param location location returned from Altitude provider.
     *                 Use API setAltitude() and
     *                 setVerticalAccuracyMeters() to set the
     *                 accurate altitude and altitude uncertainty
     *                 when altitude info is valid. Please note that
     *                 both methods need to be caleld and both
     *                 hasAltitude()and hasVerticalAccuracy() need
     *                 to return true to indicate that altitude
     *                 acquisition was successful. All other fields
     *                 in location object should not be altered, in
     *                 particular, the elasedRealTimeStamp in the
     *                 location report should not be changed.
     *
     * @return
     * None.
     */
    public abstract void pushAltitude(Location location);

/** @} */ /* end_addtogroup IZatAltitudeReceiver */

/** @addtogroup IZatAltitudeReceiver
@{ */

    /**
     * Interface class IZatAltitudeReceiverResponseListener.
     *
     * <p>This interface
     * receives altitude lookup request from Qualcomm location framework.</p>
     */
    public interface IZatAltitudeReceiverResponseListener {

        /**
         * Altitude lookup request to an altitude provider.
         * <p>
         * This API is called by the underlying service back
         * to applications when they need accuracy altitude.
         * Applications should implement this interface.</p>
         *
         * @param location original location used to request accuacy altitude.
         * @param isEmergency indicates whether the device is under emergency call.
         *
         * @return
         * None.
         */
        void onAltitudeLookupRequest(Location location, boolean isEmergency);
    }

/** @} */ /* end_addtogroup IZatAltitudeReceiver */
}
