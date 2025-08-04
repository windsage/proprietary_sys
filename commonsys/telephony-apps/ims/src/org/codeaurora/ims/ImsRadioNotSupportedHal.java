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
 * Default HAL class that is invoked when no IImsRadio HAL is available.
 * Typical use case for this when the target does not support telephony/ril
 */

public class ImsRadioNotSupportedHal implements IImsRadio {

    private void fail() throws RemoteException {
        throw new RemoteException("Radio is not supported");
    }

    // Implementation of IImsRadio java interface where all methods throw an exception
    @Override
    public boolean isFeatureSupported(int feature) {
        return false;
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void addParticipant(int token, String address, int clirMode,
                               CallDetails callDetails) throws RemoteException {
        fail();
    }

    @Override
    public void dial(int token, String address, EmergencyCallInfo eInfo,
                     int clirMode, CallDetails callDetails, boolean isEncrypted,
                     CallComposerInfo ccInfo, RedialInfo redialInfo)
                     throws RemoteException {
        fail();
    }

    @Override
    public void sendUssd(int token, String address) throws RemoteException {
        fail();
    }

    @Override
    public void cancelPendingUssd(int token) throws RemoteException {
        fail();
    }

    @Override
    public void answer(int token, int callType, int ipPresentation, int rttMode)
                       throws RemoteException {
        fail();
    }

    @Override
    public void deflectCall(int token, int index, String number)
                            throws RemoteException {
        fail();
    }

    @Override
    public void sendSms(int token, int messageRef, String format, String smsc,
                        boolean isRetry, byte[] pdu) throws RemoteException {
        fail();
    }

    @Override
    public void acknowledgeSms(int token, int messageRef, int deliverStatus)
                               throws RemoteException {
        fail();
    }

    @Override
    public void acknowledgeSmsReport(int token, int messageRef,
            int statusReportStatus) throws RemoteException {
        fail();
    }

    @Override
    public String getSmsFormat() throws RemoteException{
        return null;
    }

    @Override
    public void sendGeolocationInfo(int token, double lat, double lon,
                                    Address address) throws RemoteException {
        fail();
    }

    @Override
    public void hangup(int token, int connectionId, String userUri, String confUri,
            boolean mpty, int failCause, String errorInfo) throws RemoteException {
        fail();
    }

    @Override
    public void queryServiceStatus(int token) throws RemoteException {
        fail();
    }

    @Override
    public void setServiceStatus(int token, List<CapabilityStatus> capabilityStatusList,
                                 int restrictCause) throws RemoteException {
        fail();
    }

    @Override
    public void getImsRegistrationState(int token) throws RemoteException {
        fail();
    }

    @Override
    public void requestRegistrationChange(int token, int imsRegState)
                                          throws RemoteException {
        fail();
    }

    @Override
    public void modifyCallInitiate(int token, CallModify callModify)
            throws RemoteException {
        fail();
    }

    @Override
    public void cancelModifyCall(int token, int callId) throws RemoteException {
        fail();
    }

    @Override
    public void modifyCallConfirm(int token, CallModify callModify)
            throws RemoteException {
        fail();
    }

    @Override
    public void hold(int token, int callId) throws RemoteException {
        fail();
    }

    @Override
    public void resume(int token, int callId) throws RemoteException {
        fail();
    }

    @Override
    public void conference(int token) throws RemoteException {
        fail();
    }

    @Override
    public void explicitCallTransfer(int token, int srcCallId, int type, String number,
                                     int destCallId) throws RemoteException {
        fail();
    }

    @Override
    public void setConfig(int token, int item, boolean boolValue,
            int intValue, String strValue, int errorCause)
            throws RemoteException, ImsException {
        fail();
    }

    @Override
    public void getConfig(int token, int item, boolean boolValue,
            int intValue, String strValue, int errorCause) throws RemoteException {
        fail();
    }

    @Override
    public void sendDtmf(int token, int callId, char c) throws RemoteException {
        fail();
    }

    @Override
    public void startDtmf(int token, int callId, char c) throws RemoteException {
        fail();
    }

    @Override
    public void stopDtmf(int token, int callId) throws RemoteException {
        fail();
    }

    @Override
    public void setSuppServiceNotification(int token, boolean enable)
            throws RemoteException {
        fail();
    }

    @Override
    public void getClir(int token) throws RemoteException {
        fail();
    }

    @Override
    public void setClir(int token, int clirMode) throws RemoteException {
        fail();
    }

    @Override
    public void getCallWaiting(int token, int serviceClass) throws RemoteException {
        fail();
    }

    @Override
    public void setCallWaiting(int token, boolean enable, int serviceClass)
            throws RemoteException {
        fail();
    }

    @Override
    public void setCallForwardStatus(int token, int startHour, int startMinute,
                                     int endHour, int endMinute, int action,
                                     int cfReason, int serviceClass, String number,
                                     int timeSeconds) throws RemoteException {
        fail();
    }

    @Override
    public void queryCallForwardStatus(int token, int cfReason, int serviceClass,
                                       String number, boolean expectMore) throws RemoteException {
        fail();
    }

    @Override
    public void getClip(int token) throws RemoteException {
        fail();
    }

    @Override
    public void setUiTtyMode(int token, int uiTtyMode) throws RemoteException {
        fail();
    }

    @Override
    public void exitEmergencyCallbackMode(int token) throws RemoteException {
        fail();
    }

    @Override
    public void suppServiceStatus(int token, int operationType, int facility,
                                  String[] inCbNumList, String password,
                                  int serviceClass, boolean expectMore) throws RemoteException {
        fail();
    }

    @Override
    public void getColr(int token) throws RemoteException {
        fail();
    }

    @Override
    public void setColr(int token, int presentationValue) throws RemoteException {
        fail();
    }

    @Override
    public void getRtpStatistics(int token) throws RemoteException {
        fail();
    }

    @Override
    public void getRtpErrorStatistics(int token) throws RemoteException {
        fail();
    }

    @Override
    public void getImsSubConfig(int token) throws RemoteException {
        fail();
    }

    @Override
    public void sendRttMessage(int token, String message) throws RemoteException {
        fail();
    }

    @Override
    public void queryVirtualLineInfo(int token, String msisdn) throws RemoteException {
        fail();
    }

    @Override
    public void registerMultiIdentityLines(int token, Collection
            <MultiIdentityLineInfo> linesInfo) throws RemoteException {
        fail();
    }

    @Override
    public void sendSipDtmf(int token, String requestCode) throws RemoteException {
        fail();
    }

    @Override
    public void setMediaConfiguration(int token, Point screenSize, Point avcSize,
            Point hevcSize) throws RemoteException {
        fail();
    }

    @Override
    public void queryMultiSimVoiceCapability(int token) throws RemoteException {
        fail();
    }

    @Override
    public void exitSmsCallBackMode(int token) throws RemoteException {
        fail();
    }

    @Override
    public void sendVosSupportStatus(int token, boolean isVosSupported) throws RemoteException {
        fail();
    }

    @Override
    public void sendVosActionInfo(int token, VosActionInfo vosActionInfo) throws RemoteException {
        fail();
    }

    @Override
    public void setGlassesFree3dVideoCapability(int token, boolean enable3dVideo)
            throws RemoteException {
        fail();
    }

    @Override
    public void abortConference(int token, int conferenceAbortReason)
            throws RemoteException {
        fail();
    }

    @Override
    public String toAvailability(boolean v) {
        return null;
    }
}
