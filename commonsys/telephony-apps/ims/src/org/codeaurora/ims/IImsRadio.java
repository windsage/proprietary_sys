/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.graphics.Point;
import android.location.Address;
import android.os.RemoteException;
import android.telephony.ims.ImsException;
import java.util.Collection;
import java.util.List;

/*
 * Internal interface that forwards requests to the hardware abstraction layer
 * (HAL).
 */

public interface IImsRadio {

    /**
     * Check if a particular feature is supported or not.
     * @param feature - feature ID
     * return true if support else false.
     */
    boolean isFeatureSupported(int feature);

    boolean isAlive();

    void dispose();

    void addParticipant(int token, String address, int clirMode,
                        CallDetails callDetails) throws RemoteException;

    void dial(int token, String address, EmergencyCallInfo eInfo, int clirMode,
              CallDetails callDetails, boolean isEncrypted, CallComposerInfo ccInfo,
              RedialInfo redialInfo) throws RemoteException;
    /**
     * Sends USSD request to RIL via the IImsRadio interface.
     * @param address for USSD request
     */
    void sendUssd(int token, String address) throws RemoteException;

    /**
     * Sends cancel USSD request to RIL via the IImsRadio interface.
     */
    void cancelPendingUssd(int token) throws RemoteException;

    void answer(int token, int callType, int ipPresentation, int rttMode) throws RemoteException;

    /**
     * Deflect call to number.
     * @param number - phone number.
     */
    void deflectCall(int token, int index, String number) throws RemoteException;

    /**
     * This method will be triggered by the platform when the user attempts to send an SMS. This
     * method should be implemented by the IMS providers to provide implementation of sending an SMS
     * over IMS.
     *
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param format the format of the message.
     * @param smsc the Short Message Service Center address.
     * @param isRetry whether it is a retry of an already attempted message or not.
     * @param pdu PDU representing the contents of the message.
     */
    void sendSms(int token, int messageRef, String format, String smsc,
                 boolean isRetry, byte[] pdu) throws RemoteException;
    /**
     * This method will be triggered by the platform after
     * {@link #onSmsReceived(int, String, byte[])} has been called to deliver the result to the IMS
     * provider.
     *
     * @param token token provided in {@link #onSmsReceived(int, String, byte[])}
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param deliverStatus result of delivering the message.
     */
    void acknowledgeSms(int token, int messageRef, int deliverStatus) throws RemoteException;

    /**
     * This method will be triggered by the platform after
     * {@link #onSmsStatusReportReceived(int, int, String, byte[])} or
     * {@link #onSmsStatusReportReceived(int, String, byte[])} has been called to provide the
     * result to the IMS provider.
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param statusReportStatus result of delivering the message.
     */
    void acknowledgeSmsReport(int token, int messageRef,
                              int statusReportStatus) throws RemoteException;

    /**
     * Get SMS format.
     * @return the format of the message. Valid values are {SmsMessage#FORMAT_3GPP} and
     * {SmsMessage#FORMAT_3GPP2}.
     */
    String getSmsFormat() throws RemoteException;

    /**
     * Send geolocation information.
     * @param lat - Latitude location coordinate
     * @param lon - Longitude location coordinate
     * @param address - Address information
     */
    void sendGeolocationInfo(int token, double lat, double lon, Address address)
            throws RemoteException;

    void hangup(int token, int connectionId, String userUri, String confUri,
                boolean mpty, int failCause, String errorInfo) throws RemoteException;

    /**
     * Query IMS service status.
     */
    void queryServiceStatus(int token) throws RemoteException;

    /**
     * Set IMS service status.
     * @param capabilityStatusList - capability status list.
     * @param restrictCause - call restrict cause.
     */
    void setServiceStatus(int token, List<CapabilityStatus> capabilityStatusList,
                          int restrictCause) throws RemoteException;

    /**
     * Get IMS registration state.
     */
    void getImsRegistrationState(int token) throws RemoteException;

    /**
     * Send IMS registration state.
     * @param imsRegState - RegState.
     */
    void requestRegistrationChange(int token, int imsRegState) throws RemoteException;

    /**
     * Modify call initiate.
     * @param callModify - call modify request.
     */
    void modifyCallInitiate(int token, CallModify callModify) throws RemoteException ;

    /**
     * Send the cancel modify call request to lower layers
     * @param callId - call ID.
     */
    void cancelModifyCall(int token, int callId) throws RemoteException;

    /**
     * Confirming call type change request
     * @param callModify - call modify request.
     */
    void modifyCallConfirm(int token, CallModify callModify) throws RemoteException;

    void hold(int token, int callId) throws RemoteException;

    void resume(int token, int callId) throws RemoteException;

    void conference(int token) throws RemoteException;

     /**
      * Connects the two calls and disconnects the subscriber from both calls
      * Explicit Call Transfer occurs asynchronously
      * and may fail. Final notification occurs via
      * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
      * java.lang.Object) registerForPreciseCallStateChanged()}.
      */
    void explicitCallTransfer(int token, int srcCallId, int type, String number,
                              int destCallId) throws RemoteException ;

    /**
     * Set IMS config
     * @param item - ConfigItem
     * @param boolValue - bool value, false/true.
     * @param intValue - int value.
     * @param strValue - sting value.
     * @param errorCause - ConfigFailureCause.
     */
    void setConfig(int token, int item, boolean boolValue, int intValue, String strValue,
                   int errorCause) throws RemoteException, ImsException;

    /**
     * Get IMS config
     * @param item - ConfigItem
     * @param boolValue - bool value, false/true.
     * @param intValue - int value.
     * @param strValue - sting value.
     * @param errorCause - ConfigFailureCause.
     */
    void getConfig(int token, int item, boolean boolValue, int intValue, String strValue,
                   int errorCause) throws RemoteException;

    /**
     * Sends a DTMF code. According to <a href="http://tools.ietf.org/html/rfc2833">RFC 2833</a>,
     * event 0 ~ 9 maps to decimal value 0 ~ 9, '*' to 10, '#' to 11, event 'A' ~ 'D' to 12 ~ 15,
     * and event flash to 16. Currently, event flash is not supported.
     *
     * @param callId call Id
     * @param c the DTMF to send. '0' ~ '9', 'A' ~ 'D', '*', '#' are valid inputs.
     */
    void sendDtmf(int token, int callId, char c) throws RemoteException;

    /**
     * Start a DTMF code. According to <a href="http://tools.ietf.org/html/rfc2833">RFC 2833</a>,
     * event 0 ~ 9 maps to decimal value 0 ~ 9, '*' to 10, '#' to 11, event 'A' ~ 'D' to 12 ~ 15,
     * and event flash to 16. Currently, event flash is not supported.
     *
     * @param callId call Id
     * @param c the DTMF to send. '0' ~ '9', 'A' ~ 'D', '*', '#' are valid inputs.
     */
    void startDtmf(int token, int callId, char c) throws RemoteException;

    /**
     * Stop a DTMF code.
     */
    void stopDtmf(int token, int callId) throws RemoteException;

    /**
     * Enables/disbables supplementary service related notifications from
     * the network.
     *
     * @param enable true to enable notifications, false to disable.
     */
    void setSuppServiceNotification(int token, boolean enable) throws RemoteException;

    /**
     * Get the configuration of the CLIR supplementary service
     */
    void getClir(int token) throws RemoteException;

    /**
     * Set the configuration of the CLIR supplementary service
     * @param clirMode - clir mode to be set.
     */
    void setClir(int token, int clirMode) throws RemoteException;

    /**
     * Get call waiting.
     * @param serviceClass is a sum of SERVICE_CLASS_*
     */
    void getCallWaiting(int token, int serviceClass) throws RemoteException;

    /**
     * Set call waiting.
     * @param enable is true to enable, false to disable
     * @param serviceClass is a sum of SERVICE_CLASS_*
     */
    void setCallWaiting(int token, boolean enable, int serviceClass) throws RemoteException;

    /**
     * Set the configuration of the call forward for specified service class.
     * @param startHour - start hour.
     * @param startMinute - start minute.
     * @param endHour - end hour.
     * @param endMinute - end minute.
     * @param action -  one of CF_ACTION_*
     * @param cfReason - one of CF_REASON_*
     * @param serviceClass - serviceClass is a sum of SERVICE_CLASSS_*
     * @param number - phone number.
     * @param timeSeconds - seconds for no reply only.
     */
    void setCallForwardStatus(int token, int startHour, int startMinute,
                              int endHour, int endMinute, int action,
                              int cfReason, int serviceClass, String number,
                              int timeSeconds) throws RemoteException;

    /**
     * Retrieves the configuration of the call forward for specified service class.
     * @param cfReason - one of CF_REASON_*.
     * @param serviceClass - serviceClass is a sum of SERVICE_CLASSS_*.
     * @param number - phone number.
     */
    void queryCallForwardStatus(int token, int cfReason, int serviceClass,
                                String number, boolean expectMore) throws RemoteException;

    /**
     * Get the configuration of the CLIP supplementary service
     */
    void getClip(int token) throws RemoteException;

    /**
     * Used to set current TTY Mode.
     * @param uiTtyMode - tty mode.
     */
    void setUiTtyMode(int token, int uiTtyMode) throws RemoteException;

    /**
     * Requests Modem to come out of ECBM mode.
     */
    void exitEmergencyCallbackMode(int token) throws RemoteException;

    /**
     * Set call barring supplementary service.
     * @param operationType - Call barring operation [@link SuppSvcResponse
     * .ACTIVATE/DEACTIVATE].
     * @param facilityType - Call barring operation [@Link SuppSvcResponse.FACILITY_*]
     * @param inCbNumList - This will have ICB Number list.
     * @param  password - Password to activate/deactivate the call barring.
     * @param serviceClass - Service class information.
     */
    void suppServiceStatus(int token, int operationType, int facility,
                           String[] inCbNumList, String password, int serviceClass,
                           boolean expectMore) throws RemoteException;

    /**
     * Get the configuration of the COLR supplementary service
     */
    void getColr(int token) throws RemoteException;

    /**
     * Set the configuration of the COLR supplementary service
     * @param presentationValue - IP presentation to be set
     */
    void setColr(int token, int presentationValue) throws RemoteException;

    /**
     * Get RTP Statistics.
     */
    void getRtpStatistics(int token) throws RemoteException;

    /**
     * Get RTP error statistics.
     */
    void getRtpErrorStatistics(int token) throws RemoteException;

    /**
     * Get IMS config.
     */
    void getImsSubConfig(int token) throws RemoteException;

    /**
     * Send RTT Message.
     * @param message - RTT message to be sent.
     */
    void sendRttMessage(int token, String message) throws RemoteException;

    /**
     * Used by client for retrieving p-associated URIs of a line.
     * @param msisdn - msisdn of the line.
     */
    void queryVirtualLineInfo(int token, String msisdn) throws RemoteException;

    /**
     * Send registration request for all the lines.
     * @param linesInfo - List of MultiIdentityLineInfo.
     */
    void registerMultiIdentityLines(int token,
            Collection<MultiIdentityLineInfo> linesInfo) throws RemoteException;

    /**
     * Sends SIP DTMF string to RIL.
     * @param requestCode - SIP DTMF request.
     */
    void sendSipDtmf(int token, String requestCode) throws RemoteException;

    /**
     * Set media configuration to modem.
     * @param screenSize, screen size.
     * @param avcSize, avc codec size.
     * @param hevcSize, hevc code size.
     */
    void setMediaConfiguration(int token, Point screenSize, Point avcSize, Point hevcSize)
        throws RemoteException;

    /**
     * Query multi sim voice capability.
     */
    void queryMultiSimVoiceCapability(int token) throws RemoteException;

    /**
     * Exit SCBM mode.
     */
    void exitSmsCallBackMode(int token) throws RemoteException;

    /**
     * Send VOS SUPPORT Status to RIL.
     * @param isVosSupported - true if device supports video online service, false if unsupported.
     */
    void sendVosSupportStatus(int token, boolean isVosSupported) throws RemoteException;

    /**
     * Send VOS ACTION INFO to RIL.
     * @param vosActionInfo - VOS action info like move/touch info.
     */
    void sendVosActionInfo(int token, VosActionInfo vosActionInfo) throws RemoteException;

    /**
     * Send enable glasses free 3d video capability request to RIL.
     * @param enable3dVideo - informs device supports glasses free 3d video,
     * modem will use this flag to negotiate with network during MO call setup.
     */
    void setGlassesFree3dVideoCapability(int token, boolean enable3dVideo) throws RemoteException;

    /**
     * Send abort conference request to RIL.
     * @param abortConferenceReason - informs the reason for aborting conference.
     */
    void abortConference(int token, int conferenceAbortReason) throws RemoteException;

    default String toAvailability(boolean v) {
        return v ? "available" : "unavailable";
    }
}
