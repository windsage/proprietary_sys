/* ======================================================================
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/

package com.qti.location.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** @addtogroup IZatGnssConfigServices
    @{ */
public interface IZatGnssConfigServices {

    /**
     * Get IZatSvConfigService instance.
     * <p>
     * This function get the instance of IZatSvConfigService</p>
     *
     * @param
     * None
     *
     * @return
     * The instance of IZatSvConfigService.
     * @throws IZatFeatureNotSupportedException The feature is not supported in this device.
     */
    @Deprecated
    public IZatSvConfigService getSvConfigService() throws IZatFeatureNotSupportedException;

    /**
     * Get IZatRobustLocationConfigService instance.
     * <p>
     * This function get the instance of
     * IZatRobustLocationConfigService</p>
     *
     * @param
     * None
     *
     * @return
     * The instance of IZatRobustLocationConfigService.
     * @throws IZatServiceUnavailableException The feature is not available in this device.
     */
    public IZatRobustLocationConfigService getRobustLocationConfigService()
            throws IZatServiceUnavailableException;


    /**
     * Get IZatPreciseLocationConfigService instance.
     * <p>
     * This function get the instance of
     * IZatPreciseLocationConfigService</p>
     *
     * @param
     * None
     *
     * @return
     * The instance of IZatPreciseLocationConfigService.
     * @throws IZatServiceUnavailableException The feature is not available in this device.
     */
    public IZatPreciseLocationConfigService getPreciseLocationConfigService()
            throws IZatServiceUnavailableException;

    /**
     * Get IZatNtnConfigService instance.
     * <p>
     * This function get the instance of
     * IZatNtnConfigService</p>
     *
     * @param
     * None
     *
     * @return
     * The instance of IZatNtnConfigService.
     */
    public IZatNtnConfigService getNtnConfigService();

    /**
     * Permission required: com.qualcomm.permission.ACCESS_USER_CONSENT_API </p>
     *
     * This field indicates whether Application Developer has obtained End User Opt-In to enable
     * this feature (must be set to true to enable positioning functionality).
     * IZatGnssConfigServices will keep the current user consent status persistent across device
     * boot cycles, and continue to use it until the application is uninstalled. Please note that
     * it is an application developer responsibility to comply with all relevant privacy and
     * regulatory requirements. Use of the API is subject to, and only authorized for parties who
     * have executed, a license for this feature.
     */
    public void setNetworkLocationUserConsent(boolean hasUserConsent);
    /**
     * Inject SUPL root certificate into the modem
     * <p>
     * This function specifies the SUPL certificate to use in AGNSS sessions. </p>
     *
     * Permission required: com.qualcomm.permission.IZAT </p>
     * @param suplCertId SUPL Certificate ID, Range of [0,9].
     * @param suplCertData content of SUPL certificate file (*.der) whose length could be
     *                     2000 bytes at most.
     * @throws IZatIllegalArgumentException The suplCertData exceeds 2000 bytes or suplCertId is
     *                                      out of range [0,9].
     *         SecurityException if APP doesn't have IZAT permission.
     * @return
     * None.
     */
    public void injectSuplCert(int suplCertId, byte[] suplCertData);

    /** @addtogroup IZatSvConfigService
        @{ */
    @Deprecated
    public interface IZatSvConfigService {

        /**
         * Enum IzatGnssSvType.
         *
         * IzatGnssSvType specified constellations which can be enabled or
         * disabled via:
         * {@link com.qti.location.sdk.IZatGnssConfigService.setGnssSvTypeConfig()}
         */
        @Deprecated
        public enum IzatGnssSvType {

            UNKNOWN(0),
            GPS(1),
            SBAS(2),
            GLONASS(3),
            QZSS(4),
            BEIDOU(5),
            GALILEO(6),
            NAVIC(7);

            private final int value;
            private IzatGnssSvType(int value) {
                this.value = value;
            }
            public int getValue() {
                return value;
            }

            private static final Map<Integer, IzatGnssSvType> valueMap =
                    new HashMap<Integer, IzatGnssSvType>();

            static {
                for (IzatGnssSvType type : IzatGnssSvType.values()) {
                    valueMap.put(type.value, type);
                }
            }

            protected static IzatGnssSvType fromInt(int value) {
                return valueMap.get(value);
            }
        }

        /**
         * Requests the current GNSS SV Type Configuration.
         * <p>
         * This function sends a request to GNSS engine to
         * fetch the current GNSS SV Type Configuration </p>
         *
         * Permission required: com.qualcomm.permission.ACCESS_SV_CONFIG_API </p>
         * @param gnssConfigCb Callback to receive the SV Type configuration.
         *         This parameter cannot be NULL, otherwise a
         *         {@link IZatIllegalArgumentException} is thrown.
         * @throws IZatIllegalArgumentException The gnssConfigCb parameter is NULL.
         *
         * @return
         * None.
         */
        @Deprecated
        void getGnssSvTypeConfig(IZatSvConfigCallback gnssConfigCb)
                throws IZatIllegalArgumentException;

        /**
         * Sets the GNSS SV Type configuration.
         * <p>
         * This function specifies the GNSS SV Types (constellations) to be
         * enabled and disabled.</p>
         *
         * Permission required: com.qualcomm.permission.ACCESS_SV_CONFIG_API </p>
         * @param enabledSvTypeSet Set of IzatGnssSvType to be enabled.
         *         Pass null or an empty set if no enablement required.
         * @param disabledSvTypeSet Set of IzatGnssSvType to be disabled.
         *         Pass null or an empty set if no disablement required.
         * @throws IZatIllegalArgumentException The enabledSvTypeSet and
         *          disabledSvTypeSet parameters both are NULL.
         *          Both enabledSvTypeSet and disabledSvTypeSet contain
         *          the same element.
         *
         * @return
         * None.
         */
        @Deprecated
        void setGnssSvTypeConfig(Set<IzatGnssSvType> enabledSvTypeSet,
                                 Set<IzatGnssSvType> disabledSvTypeSet)
                throws IZatIllegalArgumentException;

        /**
         * Resets the GNSS SV Type configuration.
         * <p>
         * This function resets the GNSS SV Type configuration to
         * default values in the underlying service.</p>
         *
         * Permission required: com.qualcomm.permission.ACCESS_SV_CONFIG_API </p>
         * @param
         * None
         *
         * @return
         * None.
         */
        @Deprecated
        void resetGnssSvTypeConfig();

        /**
         * Class IZatGnssConfigCallback.
         *
         * IZatGnssConfigCallback provides the GNSS Config fetched
         * from the underlying service.
         */
        @Deprecated
        interface IZatSvConfigCallback {

            /**
             * GNSS SV Type Config Callback.
             * <p>
             * This API is called by the underlying service back
             * to applications in response to the getGnssSvTypeConfig
             * request.</p>
             *
             * @param enabledSvTypeSet Set of IzatGnssSvType.
             * @param disabledSvTypeSet Set of IzatGnssSvType.
             */

            void getGnssSvTypeConfigCb(Set<IzatGnssSvType> enabledSvTypeSet,
                                       Set<IzatGnssSvType> disabledSvTypeSet);
        }
    }
    /** @} */ /* end_addtogroup IZatSvConfigService */

    /** @addtogroup IZatRobustLocationConfigService
        @{ */
    public interface IZatRobustLocationConfigService {
        /**
         * Class IzatRLConfigInfo.
         */

        public class IzatRLConfigInfo {
            public static final int ENABLE_STATUS_VALID = 1;
            public static final int ENABLE_FORE911_STATUS_VALID = 2;
            public static final int VERSION_INFO_VALID = 4;

            public int mValidMask;
            public boolean mEnableStatus;
            public boolean mEnableForE911Status;
            public int major;
            public int minor;
        }

        /**
         * Requests the robust location Configuration.
         * <p>
         * This function sends a request to GNSS engine to
         * fetch the current robust location Configuration </p>
         *
         * Permission required: android.permission.ACCESS_ROBUST_LOCATION </p>
         * @param IZatRLConfigCallback Callback to receive the robust location configuration.
         *         This parameter cannot be NULL, otherwise a
         *         {@link IZatIllegalArgumentException} is thrown.
         * @throws IZatIllegalArgumentException The gnssConfigCb parameter is NULL.
         *         SecurityException if the device bootloader is unlocked.
         * @return
         * None.
         */

        void getRobustLocationConfig(IZatRLConfigCallback callback);

        /**
         * Sets the robust location configuration.
         * <p>
         * This function enable/disable the robust location feature</p>
         *
         * Permission required: android.permission.ACCESS_ROBUST_LOCATION </p>
         * @param enable set true/false to enable/disable the robust location feature.
         * @param enableForE911 set true/false to enable/disable the robust location
         *        feature for E911 emergency call.
         * @throws SecurityException if the device bootloader is unlocked.
         *
         * @return
         * None.
         */

        void setRobustLocationConfig(boolean enable, boolean enableForE911);

        /**
         * Inject Merkle tree configure buffer which reads from a .xml configure file.
         * <p>
         * Configure file contains Merkle Root, Merkle Nodes and information for
         * up to 2 public keys. This merkle tree is used by the standard position
         * engine (SPE).
         * Client should wait for the command to finish.
         * Behavior is not defined if client issues a second
         * request of configMerkleTree() without waiting for the
         * previous configMerkleTree() to finish.
         * Please note that caller should free the merkleTreeXml. </p>
         *
         * Permission required: android.permission.ACCESS_ROBUST_LOCATION </p>
         * @param merkleTreeXml: merkle tree char buffer
         * @param xmlSize: the length of char buffer
         * @throws SecurityException if Robust Location is not supported.
         * @return true, if the API request has been accepted;
         *         false, if the API request has not been accepted for further processing.
         */
        boolean configMerkleTree(String merkleTreeXml, int xmlSize);

        /**
         * API to Enable/Disable OSNMA (Open Source Navigation Message Authentication)
         * operation in standard position engine (SPE).
         * <p>
         * Client should wait for the command to finish.
         * Behavior is not defined if client issues a second
         * request of configOsnmaEnablement() without waiting for the
         * previous configOsnmaEnablement() to finish. </p>
         *
         * Permission required: android.permission.ACCESS_ROBUST_LOCATION </p>
         * @param isEnabled: The flag to indicate enable or disable OSNMA
         * @throws SecurityException if Robust Location is not supported.
         * @return true, if the API request has been accepted;
         *         false, if the API request has not been accepted for further processing.
         */
        boolean configOsnmaEnablement(boolean isEnabled);

        /**
         * Class IZatRLConfigCallback.
         *
         * IZatGnssRLCallback provides the robust location Config fetched
         * from the underlying service.
         */

        interface IZatRLConfigCallback {

            /**
             * Robust location Config Callback.
             * <p>
             * This API is called by the underlying service back
             * to applications in response to the getRobustLocationConfig
             * request.</p>
             *
             * @param info robust location config info.
             */

            void getRobustLocationConfigCb(IzatRLConfigInfo info);
        }
    }
    /** @} */ /* end_addtogroup IZatRobustLocationConfigService */

    /** @addtogroup IZatPreciseLocationConfigService
        @{ */
    public interface IZatPreciseLocationConfigService {

        /** Some NTRIP mount points requires NMEA GGA.
         *  Due to privacy considerations, this setting indicates
         *  that end user agreed to send location to NTRIP server.
         */
        enum IZatPreciseLocationOptIn {
            OPTED_IN_FOR_LOCATION_REPORT,
            NOT_OPTED_IN_FOR_LOCATION_REPORT
        }

        /**
         * Class IZatPreciseLocationNTRIPSettings
         */
        public class IZatPreciseLocationNTRIPSettings {
            protected String mHostNameOrIP;
            protected String mMountPointName;
            protected int mPort;
            protected String mUserName;
            protected String mPassword;
            protected boolean mUseSSL;
            protected int mNmeaUpdateInterval;

            /**
             * Configuration settings for NTRIP caster connection.
             * <p>
             * All the parameters are mandatory </p>
             *
             * @param hostNameOrIP Caster host name or IP address.
             * @param mountPointName Name of the caster mount point to connect to.
             * @param port Caster port to connect to.
             * @param userName Caster mount point credentials user name.
             * @param password Caster mount point credentials password.
             * @param nmeaUpdateInterval The interval NMEA sentence sent to caster in seconds.
             * @throws IZatIllegalArgumentException Any of the parameters is NULL.
             *
             * @return
             * IZatPreciseLocationNTRIPSettings.
             */
            public IZatPreciseLocationNTRIPSettings(
                    String hostNameOrIP,
                    String mountPointName,
                    int port,
                    String userName,
                    String password,
                    boolean useSSL,
                    int nmeaUpdateInterval)
                    throws IZatIllegalArgumentException {

                mHostNameOrIP = validateString(hostNameOrIP);
                mMountPointName = validateString(mountPointName);
                mUserName = validateString(userName);
                mPassword = validateString(password);
                mPort = port;
                mUseSSL = useSSL;
                mPort = port;
                mNmeaUpdateInterval = nmeaUpdateInterval;
            }

            private String validateString(String param) {
                if (null == param || param.isEmpty()) {
                    throw new IZatIllegalArgumentException();
                }

                return param;
            }
        }

        /**
         * Set the user consent for sharing location to NTRIP mount point.
         * Shall be called before enablePreciseLocation.
         * @param consent Consent is granted or not.
         *
         * It is the responsibility of the OEM to comply with all applicable privacy and data
         * protection laws, rules, and regulation (for example, obtaining consumer opt-in consent).
         * To enable Qualcomm SW device to report its location to the NTRIP mount point, OEM App
         * shall explicitly configure Qualcomm SW.
         */
        void setPreciseLocationOptIn(IZatPreciseLocationOptIn optin);

        /**
         * Enables the Precise Location data stream using the desired NTRIP caster settings.
         * <p>
         * This is system-global and will affect any ongoing or future GNSS tracking session. </p>
         *
         * It is the responsibility of the OEM to comply with all applicable privacy and data
         * protection laws, rules, and regulation (for example, obtaining consumer opt-in consent).
         * To enable Qualcomm SW device to report its location to the NTRIP mount point, OEM App
         * shall explicitly configure Qualcomm SW.
         *
         * Permission required: android.permission.ACCESS_PRECISE_LOCATION_API </p>
         * @param ntripSettings Settings for the NTRIP connection as defined in
         *                      IZatPreciseLocationNTRIPSettings.
         * @param requiresInitialNMEA Boolean indicating if the correction data server requires
         *                      device location in the form of GGA NMEA string.
         * @throws IZatIllegalArgumentException The settings are not properly initialized.
         *
         * @return
         * None.
         */
        void enablePreciseLocation(IZatPreciseLocationNTRIPSettings ntripSettings,
                boolean requiresInitialNMEA)
                throws IZatIllegalArgumentException;

        /**
         * Disables the Precise Location data stream.
         * <p>
         * This is system-global and will affect any ongoing or future GNSS tracking session. </p>
         *
         * Permission required: android.permission.ACCESS_PRECISE_LOCATION_API </p>
         * @return
         * None.
         */
        void disablePreciseLocation();
    }
    /** @} */ /* end_addtogroup IZatPreciseLocationConfigService */

    /** @addtogroup IZatNtnConfigService
        @{ */
    public interface IZatNtnConfigService {

        public static final int GPS_L1_ENABLE_BIT = 0x1;
        public static final int GPS_L5_ENABLE_BIT = 0x2;

        /**
         * Register NTN status callback.
         * <p>
         * This function registers ntnStatusCallback function
         * to receive NTN status from underlying service. </p>
         *
         * @param IZatNtnStatusCallback to receive the changes of enabled signal types.
         *         This parameter cannot be NULL, otherwise a
         *         {@link IZatIllegalArgumentException} is thrown.
         * @throws IZatIllegalArgumentException The callback parameter is NULL.
         * @return
         * None.
         */

        void registerNtnStatusCallback(IZatNtnStatusCallback callback);

        /**
         * Set 3rd party NTN capability.
         * <p>
         * This function tells GNSS modules if device has 3rd party NTN capability.
         * This info is useful for GNSS modules such as optimize xtra file download
         * internal. No response for this function.
         * Permission required: com.qualcomm.qti.permission.ACCESS_SV_CONFIG_API </p>
         *
         * @param boolean isCapable.
         * @throws SecurityException if APP doesn't have ACCESS_SV_CONFIG_API permission.
         * @return
         * None.
         */

        void set3rdPartyNtnCapability(boolean isCapable);

        /**
         * Requests the NTN status.
         * <p>
         * This function send a request to get current NTN status from underlying service.
         * NTN Status will be reported asynchronously via ntnConfigSignalMaskResponse.
         * Permission required: com.qualcomm.qti.permission.ACCESS_SV_CONFIG_API </p>
         *
         * @param none.
         * @throws IZatIllegalArgumentException if IZatNtnStatusCallback
         *         is not registered.
         *         RuntimeException if previous get or set API call haven't get response.
         *         SecurityException if APP doesn't have ACCESS_SV_CONFIG_API permission.
         * @return
         * None.
         */

        void getNtnConfigSignalMask();

        /**
         * Sets the NTN status.
         * <p>
         * This function set the NTN status. NTN status will be reported asynchronously
         * via ntnConfigSignalMaskResponse after this function is called.
         * SDK client shall wait for ntnConfigSignalMaskResponse and then do further operation.
         * Permission required: com.qualcomm.qti.permission.ACCESS_SV_CONFIG_API </p>
         *
         * @param gpsSignalTypeConfigMask,
         *        Bit 1: set to 1 enable GPS L1, set to 0 to disable GPS L1.
         *        Bit 2: set to 1 enable GPS L5, set to 0 to disable GPS L5.
         * @throws IZatIllegalArgumentException if IZatNtnStatusCallback
         *         is not registered.
         *         RuntimeException if previous get or set API call haven't get response.
         *         SecurityException if APP doesn't have ACCESS_SV_CONFIG_API permission.
         * @return
         * None.
         */

        void setNtnConfigSignalMask(int gpsSignalTypeConfigMask);


        /**
         * Class IZatNtnStatusCallback.
         *
         * IZatNtnStatusCallback provides the NTN status information
         * from the underlying service.
         * ntnConfigSignalMaskResponse will be called when:
         *    1, getNTNStatus is called;
         *    2, setNTNStatus is called;
         * ntnConfigSignalMaskChanged will be called when
         *    1, GPS L1 or L5 status changes by modem itself.
         *
         */

        interface IZatNtnStatusCallback {

            /**
             * 3rd party NTN status Callback to response getNtnConfigSignalMask
             * and setNtnConfigSignalMask.
             * <p>
             * This API is called by the underlying service back
             * to applications to provide current NTN status in
             * response to setNtnConfigSignalMask and
             * getNtnConfigSignalMask.
             * </p>
             *
             * @param isSuccess, indicates if getNtnConfigSignalMask or
             *        setNtnConfigSignalMask is successfully executed.
             * @param gpsSignalTypeConfigMask,
             *        Bit 1: 0 - GPS L1 is disabled, 1 - GPS L1 is enabled;
             *        Bit 2: 0 - GPS L5 is disabled, 1 - GPS L5 is enabled.
             *        gpsSignalTypeConfigMask = -1 means invalid.
             */

            void ntnConfigSignalMaskResponse(boolean isSuccess, int gpsSignalTypeConfigMask);

            /**
             * 3rd party NTN status Callback to indicate NTN status change.
             * <p>
             * This API is called by the underlying service back
             * to applications to indicate there is NTN status change.
             * This API will be called when NTN status change happened
             * in Modem side, e.g. dialing E911.
             *
             * It is required that Location SDK client shall exit 3rd party NTN mode
             * by calling setNtnConfigSignalMask API with exactly
             * same gpsSignalTypeConfigMask immediately after receiving
             * ntnConfigSignalMaskChanged.
             * </p>
             *
             * @param gpsSignalTypeConfigMask,
             *        Bit 1: 0 - GPS L1 is disabled, 1 - GPS L1 is enabled;
             *        Bit 2: 0 - GPS L5 is disabled, 1 - GPS L5 is enabled.
             */

            void ntnConfigSignalMaskChanged(int gpsSignalTypeConfigMask);
        }
    }
    /** @} */ /* end_addtogroup IZatNtnConfigService */
}
/** @} */ /* end_addtogroup IZatGnssConfigServices */
