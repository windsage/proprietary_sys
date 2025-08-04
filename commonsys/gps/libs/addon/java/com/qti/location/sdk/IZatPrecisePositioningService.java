/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package com.qti.location.sdk;

import android.location.Location;
import java.util.Map;
import java.util.HashMap;
import android.util.Log;

/** @addtogroup IZatPrecisePositioningService
    @{ */

/**
 * The IZatPrecisePositioningService provides the interface for interacting with the Qualcomm Location precise positioning service.
 *
 * <pre>
 * <code>
 *
 *  // Sample Code
 *
 *  import android.app.Activity;
 *  import android.os.Bundle;
 *  import android.view.View;
 *  import android.widget.Button;
 *  import com.qti.location.sdk.IZatManager;
 *  import com.qti.location.sdk.IZatPrecisePositioningService;
 *
 *  public class FullscreenActivity extends Activity {
 *
 *     private IZatManager mIzatMgr = null;
 *     private IZatPrecisePositioningService mPpSevice = null;
 *
 *     @Override
 *     protected void onCreate(Bundle savedInstanceState) {
 *
 *          ...
 *          // get the instance of IZatManager
 *          mIzatMgr = IZatManager.getInstance(getApplicationContext());
 *
 *          ...
 *          // create a callback object used to receive the location objects.
 *          final PrecisePositionCallback preciseCb = new PrecisePositionCallback();
 *
 *          ...
 *          final Button connectButton = (Button)findViewById(R.id.connectButton);
 *          connectButton.setOnClickListener(new View.OnClickListener() {
 *              @Override
 *              public void onClick(View v) {
 *
 *                  // connect to IZatPrecisePositioningService through IZatManager
 *                  if (mIzatMgr != null) {
 *                      mPpSevice = mIzatMgr.connectToPrecisePositioningService();
 *                  }
 *              }
 *          });
 *
 *          ...
 *          final Button startButton = (Button)findViewById(R.id.startButton);
 *          startButton.setOnClickListener(new View.OnClickListener() {
 *              @Override
 *              public void onClick(View v) {
 *
 *                  // start an precise positioning session with time interval and
 *                  // precise type and correction type
 *                  if (mPpSevice != null) {
 *                      // create a precise positioning request object
 *                      long timeInterval = 1000;
 *                      int preciseType = 2;
 *                      int correctionType = 1;
 *                      IZatPrecisePositioningService.IZatPrecisePositioningRequest request =
 *                          new IZatPrecisePositioningRequest(timeInterval,
 *                          IZatPreciseType.fromInt(preciseType),
 *                          IZatCorrectionType.fromInt(correctionType));
 *
 *                      // start the session.
 *                      mPpService.startPrecisePositioningSession(preciseCb, request);
 *                  }
 *              }
 *          });
 *      }
 *  }
 *
 *  public class PrecisePositionCallback implements
 *          IZatPrecisePositioningService.IZatPrecisePositioningCallback  {
 *      public void onLocationAvailable(Location[] locations) {
 *          ...
 *      }
 *      public void onResponseCallback(IZatLocationResponse response) {
 *          ...
 *      }
 *  }
 *
 * </code>
 * </pre>
 */

public interface IZatPrecisePositioningService {

    public final int TIME_INTERVAL_DEFAULT_MS = 1000;

    /**
     * Starts a precise positioning session.
     *
     * This function enables applications to start a precise positioning session,
     *
     * Permission required: android.permission.ACCESS_FINE_LOCATION </p>
     * @param callback Callback to receive location and response.
     *         This parameter cannot be NULL, otherwise a
     *         {@link IZatIllegalArgumentException} is thrown.
     * @param pPRequest precise location request for the updates.
     *         This parameter cannot be NULL, otherwise a
     *         {@link IZatIllegalArgumentException} is thrown.
     * @throws IZatIllegalArgumentException One or more parameters are NULL or invalid.
     * @return
     * None.
     */
    void startPrecisePositioningSession(IZatPrecisePositioningCallback callback,
                                         IZatPrecisePositioningRequest pPRequest)
                                         throws IZatIllegalArgumentException;
    /**
     * Stops a precise positioning session.
     *
     * This function enables applications to stop a running precise positioning
     * session.
     *
     * @return
     * None.
     */
    void stopPrecisePositioningSession();

/** @} */  /* end_addtogroup IZatPrecisePositioningService */

/** @addtogroup IZatPrecisePositioningService
    @{ */

    /**
     * Interface class IZatPrecisePositioningCallback.
     *
     * IZatPrecisePositioningCallback is the interface for receiving location and
     * response.
     *
     * <pre>
     * <code>
     *
     *  // Sample Code
     *
     *  import android.app.Activity;
     *  import android.os.Bundle;
     *  import android.view.View;
     *  import android.widget.Button;
     *  import android.util.Log;
     *  import com.qti.location.sdk.IZatManager;
     *  import com.qti.location.sdk.IZatPrecisePositioningService;
     *
     *  public class FullscreenActivity extends Activity {
     *
     *      ...
     *      // create a callback object used to receive the location and response.
     *      final PrecisePositionCallback locationCb = new PrecisePositionCallback();
     *
     *      ...
     *      final Button startButton = (Button)findViewById(R.id.startButton);
     *      startButton.setOnClickListener(new View.OnClickListener() {
     *          @Override
     *          public void onClick(View v) {
     *             ...
     *             // start the session.
     *             mPpService.startPrecisePositioningSession(locationCb, request);
     *          }
     *      });
     *  }
     *
     *  public class PrecisePositionCallback implements
     *          IZatPrecisePositioningService.IZatPrecisePositioningCallback {
     *
     *      public void onLocationAvailable(Location location) {
     *          Log.d("IzatSDK", "latitude:" + location.getLatitude() + "longitude:" +
     *                  location.getLongitude());
     *      }
     *      public void onResponseCallback(IZatLocationResponse response) {
     *          Log.d("IzatSDK", "response:" + response);
     *      }
     *  }
     *
     * </code>
     * </pre>
     */
    interface IZatPrecisePositioningCallback {
        /**
         * Location callback.
         *
         * This API is called by the underlying service back
         * to applications when location are available.
         * Applications should implement this interface.
         *
         * @param location available location.
         * @return
         * None.
         */
        void onLocationAvailable(Location locations);
        /**
         * Location callback.
         *
         * This API is called by the underlying service back
         * to applications when service report session response.
         * Applications should implement this interface.
         *
         * @param response session response.
         * @return
         * None.
         */
        void onResponseCallback(IZatLocationResponse response);
    }

/** @} */  /* end_addtogroup IZatPrecisePositioningService */

/** @addtogroup IZatPrecisePositioningService
    @{ */

    /**
     * Class IZatPrecisePositioningRequest.
     */
    public class IZatPrecisePositioningRequest {
        long mMinTimeInterval = TIME_INTERVAL_DEFAULT_MS;
        IZatPreciseType mPreciseType;
        IZatCorrectionType mCorrectionType;
        /**
         * Creates an precise positioning request.
         *
         * This function constructs an precise positioning request.
         *
         * @param minTimeInterval interval time between fix
         * @param preciseType precise positioning type including meter level positioning(MLP),
         *        deciemeter level positioning(DLP), and meter level positioning without correction
         *        service(MLP_WOCS).
         * @param correctionType correction data type including default, NTRIP and 3GPP.
         *
         * @return
         * None.
         */
        public IZatPrecisePositioningRequest(long minTimeInterval, IZatPreciseType preciseType,
                                             IZatCorrectionType correctionType) {
            setTimeInterval(minTimeInterval);
            setPreciseType(preciseType);
            setCorrectionType(correctionType);
        }
        /**
         * Sets the time interval, in milliseconds.
         *
         * This function enables applications to set the time interval in the precise positioning
         * request. The time interval is 1000 milliseconds default. precise positioning session
         * will only support 1Hz position report no matter what value time interval is set.
         *
         * @param minTimeInterval Time interval in milliseconds,
         *         which must be greater than zero, otherwise a
         *         {@link IZatIllegalArgumentException} is thrown.
         * @throws IZatIllegalArgumentException Time interval is less than or equal to zero.
         * @return
         * None.
         */
        public void setTimeInterval(long minTimeInterval)
                throws IZatIllegalArgumentException {
            if (minTimeInterval <= 0)
                throw new IZatIllegalArgumentException("invalid time interval");
            mMinTimeInterval = TIME_INTERVAL_DEFAULT_MS;
        }
        /**
         * Gets the time interval that has been set.
         *
         * This function enables applications to get the time interval that has
         * been set in the precise positioning request. It returns 1000 if the time interval
         * is not set.
         *
         * @return The time interval in milliseconds.
         */
        public long getTimeInterval() {
            return mMinTimeInterval;
        }
        /**
         * Sets the precise positioning type.
         *
         * This function enables applications to set the precise type in the precise positioning
         * request.
         *
         * @param preciseType Precise positioning type
         *
         * @return
         * None.
         */
        public void setPreciseType(IZatPreciseType preciseType) {
            mPreciseType = preciseType;
        }
        /**
         * Gets the precise type that has been set.
         *
         * This function enables applications to get the precise type that has
         * been set in the precise positioning request.
         *
         * @return The precise type.
         */
        public IZatPreciseType getPreciseType() {
            return mPreciseType;
        }
        /**
         * Sets the correction data type.
         *
         * This function enables applications to set the correction data type
         * in the precise positioning request.
         *
         * @param correctionType Correction data type
         *
         * @return
         * None.
         */
        public void setCorrectionType(IZatCorrectionType correctionType) {
            mCorrectionType = correctionType;
        }
        /**
         * Gets the correction type that has been set.
         *
         * This function enables applications to get the correction type that has
         * been set in the precise positioning request.
         *
         * @return The correction type.
         */
        public IZatCorrectionType getCorrectionType() {
            return mCorrectionType;
        }
    }
    /**
     * Enum IZatPreciseType, which defines the precise positioning type.
     *
     */
    public enum IZatPreciseType {
        /**
         * meter level positioning.
         */
        PRECISE_TYPE_MLP(1),
        /**
         * decimeter level positioning.
         */
        PRECISE_TYPE_DLP(2),
        /**
         * meter level positioning without correction service.
         */
        PRECISE_TYPE_MLP_WOCS(3);

        private final int value;
        private IZatPreciseType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        private static final Map<Integer, IZatPreciseType> valueMap =
                new HashMap<Integer, IZatPreciseType>();

        static {
            for (IZatPreciseType type : IZatPreciseType.values()) {
                    valueMap.put(type.value, type);
            }
        }

        public static IZatPreciseType fromInt(int value) {
                return valueMap.get(value);
        }
    }
    /**
     * Enum IZatCorrectionType, which defines the correction data type.
     *
     */

    public enum IZatCorrectionType {
        /**
         * correction data type is default,
         * the precise positioning service will select the appropriate data type.
         */
        CORRECTION_TYPE_DEFAULT(1),
        /**
         * correction data type is Radio Technical Commission for Maritime Services.
         */
        CORRECTION_TYPE_RTCM(2),
        /**
         * correction data type is The 3rd Generation Partnership Project.
         */
        CORRECTION_TYPE_3GPP(3);

        private final int value;
        private IZatCorrectionType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        private static final Map<Integer, IZatCorrectionType> valueMap =
                new HashMap<Integer, IZatCorrectionType>();

        static {
            for (IZatCorrectionType type : IZatCorrectionType.values()) {
                    valueMap.put(type.value, type);
            }
        }

        public static IZatCorrectionType fromInt(int value) {
                return valueMap.get(value);
        }
    }
    /**
     * Enum IZatLocationResponse, which defines the location response.
     *
     */
    public enum IZatLocationResponse {
        /**
         * Session is success.
         */
        LOCATION_RESPONSE_SUCCESS(0),
        /**
         * Session is failed for unknown reason.
         */
        LOCATION_RESPONSE_UNKNOWN_FAILURE(1),
        /**
         * Feature is not supported.
         */
        LOCATION_RESPONSE_NOT_SUPPORTED(2),
        /**
         * No valid license.
         */
        LOCATION_RESPONSE_NO_VALID_LICENSE(3),
        /**
         * There is no correction data.
         */
        LOCATION_RESPONSE_NO_CORRECTION(4),
        /**
         * Another precise session is already running.
         */
        LOCATION_RESPONSE_ANOTHER_PRECISE_SESSION_RUNNING(5);

        private final int value;
        private IZatLocationResponse(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        private static final Map<Integer, IZatLocationResponse> valueMap =
                new HashMap<Integer, IZatLocationResponse>();

        static {
            for (IZatLocationResponse response : IZatLocationResponse.values()) {
                    valueMap.put(response.value, response);
            }
        }

        protected static IZatLocationResponse fromInt(int value) {
                return valueMap.get(value);
        }
    }
    /**
     * Enum IZatLocationQuality, which defines the location quality.
     * location quality is stored in location extra Bundle report from location callback
     * {@link com.qti.location.sdk.IZatPrecisePositioningService.IZatPrecisePositioningCallback}.
     *
     * <pre>
     * <code>
     *
     *  // Sample Code
     * import android.location.Location;
     * import com.qti.location.sdk.IZatPrecisePositioningService;
     *
     *  public class PrecisePositionCallback implements
     *          IZatPrecisePositioningService.IZatPrecisePositioningCallback {
     *
     *      public void onLocationAvailable(Location location) {
     *          IZatLocationQuality qualityType =
     *                  IZatLocationQuality.fromInt(location.getExtras().getInt("Quality_type"));
     *          Log.d("IzatSDK", "quality_type:" + qualityType);
     *      }
     *  }
     *
     * </code>
     * </pre>
     *
     */
    public enum IZatLocationQuality {
        /**
         * location quality is standard GNSS.
         */
        LOCATION_QUALITY_STANDALONE(0),
        /**
         * location quality is Differential GNSS.
         */
        LOCATION_QUALITY_DGNSS(1),
        /**
         * location quality is RTK float.
         */
        LOCATION_QUALITY_FLOAT(2),
        /**
         * location quality is RTK fixed.
         */
        LOCATION_QUALITY_FIXED(3);

        private final int value;
        private IZatLocationQuality(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        private static final Map<Integer, IZatLocationQuality> valueMap =
                new HashMap<Integer, IZatLocationQuality>();

        static {
            for (IZatLocationQuality response : IZatLocationQuality.values()) {
                    valueMap.put(response.value, response);
            }
        }

        public static IZatLocationQuality fromInt(int value) {
                return valueMap.get(value);
        }
    }

}
