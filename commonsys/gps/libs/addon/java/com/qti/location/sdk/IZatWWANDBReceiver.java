/* ======================================================================
*  Copyright (c) 2017-2023 Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/

package com.qti.location.sdk;

import java.util.List;
import com.qti.location.sdk.IZatDBCommon.IZatCellInfo;
import com.qti.location.sdk.IZatStaleDataException;
import com.qti.location.sdk.IZatDBCommon.IZatAPBSSpecialInfoTypes;
import com.qti.location.sdk.IZatDBCommon.IZatApBsListStatus;

import android.location.Location;

/*
 * <p>Copyright (c) 2017 Qualcomm Technologies, Inc.</p>
 * <p>All Rights Reserved.</p>
 * <p>Confidential and Proprietary - Qualcomm Technologies, Inc</p>
 * <br/>
 * <p><b>IZatWWANDBReceiver</b> interface @version 1.0.0 <p>
 */

/** @addtogroup IZatWWANDBReceiver
@{ */

/** API for
 * injecting a WWAN Cell Location database to a Qualcomm Location framework.
 */
@Deprecated
public abstract class IZatWWANDBReceiver {

    /**
     * <p>
     * Constructor - IZatWWANDBReceiver. </p>
     *
     * @param listener Listener to receive WWAN DB Receiver
     *         responses. This parameter cannot be NULL, otherwise
     *         a {@link IZatIllegalArgumentException} is
     *         thrown.
     * @throws IZatIllegalArgumentException The listener parameter is NULL.
     * @return
     * None.
     */
    protected IZatWWANDBReceiver(Object listener)
                                throws IZatIllegalArgumentException {
    }

    /**
     * Requests list of base stations.
     * <p>
     * This enables WWAN database providers to request a list of BSs
     * that require location information.
     * </p>
     *
     * @param expire_in_days Number of days until
     *                        the associated location of a BS (if
     *                        available) will expire.
     *                        Optional parameter.
     *                        If 0 is provided, only BSs that have an
     *                        already expired location or no
     *                        location associated with it will be
     *                        fetched.
     * @return
     * None.
     */
    public abstract void requestBSList(int expire_in_days);

    /**
     * Requests a WWAN DB update.
     * <p>
     * This enables a WWAN database provider to insert a list of BSs
     * with location information.
     * </p>
     *
     * @param location_data Location information of base stations.
     *                          If not available, a NULL/empty list can be
     *                          provided. The maximum number of elements is allowed,
     *                          defined by {@link MAXIMUM_INJECTION_ELEMENTS}.
     * @param special_info Special information on a base station. If
     *                         not available, a NULL/empty list can be
     *                         provided. The maximum number of elements is allowed,
     *                         defined by {@link MAXIMUM_INJECTION_ELEMENTS}.
     * @param days_valid  Number of days for which location_data and
     *                   special_info will be valid. Optional
     *                   parameter. Defaults to 15 days if 0 is
     *                   provided.
     * @return
     * None.
     */
    public abstract void pushWWANDB(List<IZatBSLocationData> location_data,
                                    List<IZatBSSpecialInfo> special_info,
                                    int days_valid);

/** @} */ /* end_addtogroup IZatWWANDBReceiver */

/** @addtogroup IZatWWANDBReceiver
@{ */


    // For backwards compatibility only
    // Same structure as new common IZatCellInfo

    @Deprecated
    public static class IZatBSInfo {
        public IZatBSInfo(IZatCellInfo cellInfo) {
        }

    }

     /**
     * Interface class IZatWWANDBReceiverResponseListener.
     *
     * <p>This interface
     * receives responses from a WWAN database receiver in a Qualcomm
     * Location framework.</p>
     */
     @Deprecated
    public interface IZatWWANDBReceiverResponseListener {

        /**
         * Response to a BS list request.
         * <p>
         * This API is called by the underlying service back
         * to applications when a list of BSs is available.
         * Applications should implement this interface.</p>
         *
         * @param bs_list   List of BSs.
         *
         * @return
         * None.
         */
        void onBSListAvailable(List<IZatBSInfo> bs_list);


        /**
         * Response for a BS location injection request.
         * <p>
         * This API is called by the underlying service back
         * to applications when a BS location injection completes.
         * Applications should implement this interface.</p>
         *
         * @param is_success Injection of BS locations success or
         *                        failure.
         * @param error Error details if the BS location injection
         *                   failed.
         *
         * @return
         * None.
         */
        void onStatusUpdate(boolean is_success, String error);

        /**
         * Service request to a WWAN DB provider.
         * <p>
         * This API is called by the underlying service back
         * to applications when they need service. Applications should
         * implement this interface.</p>
         *
         * @return
         * None.
         */
        void onServiceRequest();
    }

    /**
     * Interface class IZatWWANDBReceiverResponseListener.
     *
     * <p>This interface
     * receives responses from a WWAN database receiver in a Qualcomm
     * Location framework.</p>
     */
    public interface IZatWWANDBReceiverResponseListenerExt{

        /**
         * Response to a BS list request.
         * <p>
         * This API is called by the underlying service back
         * to applications when a list of BSs is available.
         * Applications should implement this interface.</p>
         *
         * @param bs_list   List of BSs.
         * @param bs_status Status of the BS list.
         * @param location  Location of area of interest.
         *
         * @return
         * None.
         */
        void onBSListAvailable(List<IZatCellInfo> bs_list,
                               IZatApBsListStatus bs_status,
                               Location location);

        /**
         * Response for a BS location injection request.
         * <p>
         * This API is called by the underlying service back
         * to applications when a BS location injection completes.
         * Applications should implement this interface.</p>
         *
         * @param is_success Injection of BS locations success or
         *                        failure.
         * @param error Error details if the BS location injection
         *                   failed.
         *
         * @return
         * None.
         */
        void onStatusUpdate(boolean is_success, String error);

        /**
         * Service request to a WWAN DB provider.
         * <p>
         * This API is called by the underlying service back
         * to applications when they need service. Applications should
         * implement this interface.</p>
         *
         * @return
         * None.
         */
        void onServiceRequest();
    }

/** @} */ /* end_addtogroup IZatWWANDBReceiver */


/** @addtogroup IZatWWANDBReceiver
@{ */

    /**
     * Class IZatBSLocationData.
     */
    public abstract static class IZatBSLocationDataBase {
        /**
         * Enumeration of reliablity types.
         */
        public enum IZatReliablityTypes {
            VERY_LOW,    /**< The probability of a position outlier is 1
             1 in one hundred or even more likely. */
            LOW,         /**< The probability of a position outlier is about
             1 in one thousand. */
            MEDIUM,      /**< The probability of a position outlier is about
             1 in 100 thousand. */
            HIGH,        /**< The  probability of a position outlier is about
             1 in 10 million. */
            VERY_HIGH    /**< The probability of a position outlier is about 1 in
             1 in a thousand million. \n
             Until sufficient experience is obtained, the reliability
             input value should remain unset or set to LOW. */
        }
        /**
         *
         * @param iZatCellInfo
         * @param location
         * @param horizontalReliability
         * @param altitudeReliability
         */
        public IZatBSLocationDataBase(IZatCellInfo iZatCellInfo, Location location,
                                      IZatReliablityTypes horizontalReliability,
                                      IZatBSLocationData.IZatReliablityTypes altitudeReliability) {
        }

        /**
         *
         * @param iZatCellInfo
         * @param location
         */
        public IZatBSLocationDataBase(IZatCellInfo iZatCellInfo, Location location) {
        }

        public IZatBSLocationDataBase(IZatBSLocationDataBase bsLocationDataBase) {
        }
    }

    public static class IZatBSLocationData extends IZatBSLocationDataBase {

        /**
         * @param iZatCellInfo
         * @param location
         */
        public IZatBSLocationData(IZatCellInfo iZatCellInfo, Location location) {
            super(iZatCellInfo, location);
        }

        /**
         * @param iZatCellInfo
         * @param location
         * @param horizontalReliability
         * @param altitudeReliability
         */
        public IZatBSLocationData(IZatCellInfo iZatCellInfo, Location location,
                                  IZatReliablityTypes horizontalReliability,
                                  IZatReliablityTypes altitudeReliability) {
            super(iZatCellInfo, location, horizontalReliability, altitudeReliability);
        }

        /**
         *
         * @param iZatCellInfo
         * @param location
         * @param horizontalReliability
         * @param altitudeReliability
         * @param horizontalCoverageRadius
         */
        public IZatBSLocationData(IZatCellInfo iZatCellInfo, Location location,
                                  IZatReliablityTypes horizontalReliability,
                                  IZatReliablityTypes altitudeReliability,
                                  float horizontalCoverageRadius) {
            super(iZatCellInfo, location, horizontalReliability, altitudeReliability);
        }

        public IZatBSLocationData(IZatBSLocationData bsLocationData) {
            super(bsLocationData);
        }

    }
/** @} */ /* end_addtogroup IZatWWANDBReceiver */

/** @addtogroup IZatWWANDBReceiver
@{ */

    /**
     * Class IZatBSSpecialInfo.
     */
    public static class IZatBSSpecialInfo {

        /**
         * Constructor - IZatBSSpecialInfo.
         *
         * @param cellInfo
         * @param info
         */
        public IZatBSSpecialInfo(IZatCellInfo cellInfo,
                                 IZatAPBSSpecialInfoTypes info) {
        }

    }
}
/** @} */ /* end_addtogroup IZatWWANDBReceiver */
