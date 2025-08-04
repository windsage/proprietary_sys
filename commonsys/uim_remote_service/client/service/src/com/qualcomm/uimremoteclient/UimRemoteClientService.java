/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteclient;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.HwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyCallback;
import android.content.pm.Signature;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.content.res.XmlResourceParser;

import java.io.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.MessageDigest;

import vendor.qti.hardware.radio.uim_remote_client.V1_0.IUimRemoteServiceClient;
import vendor.qti.hardware.radio.uim_remote_client.V1_0.IUimRemoteServiceClientResponse;
import vendor.qti.hardware.radio.uim_remote_client.V1_0.IUimRemoteServiceClientIndication;
import vendor.qti.hardware.radio.uim_remote_client.V1_0.UimRemoteEventReqType;

public class UimRemoteClientService extends Service {
    private final String LOG_TAG = "UimRemoteClientService";

    public static class UimRemoteError {

        public static final int UIM_REMOTE_SUCCESS = 0;

        public static final int UIM_REMOTE_ERROR = 1;
    }

    private Context mContext;
    private final AtomicInteger mToken = new AtomicInteger(0);
    private int simSlots = 0;
    private UimRemoteClientProxy mUimRemoteClientProxy;
    private TelephonyManager[] mTelephonyManager = null;
    private SubscriptionManager mSubscriptionManager;
    private RadioStateListener[] mRadioStateListener = null;

    IUimRemoteClientServiceCallback mCb = null;
    private ResponseHandler mResponseHandler;
    protected static final int EVENT_REMOTE_EVENT_RESPONSE = 1;
    protected static final int EVENT_REMOTE_APDU_RESPONSE = 2;
    protected static final int EVENT_REMOTE_APDU_INDICATION = 3;
    protected static final int EVENT_REMOTE_CONNECT_INDICATION = 4;
    protected static final int EVENT_REMOTE_DISCONNECT_INDICATION = 5;
    protected static final int EVENT_REMOTE_POWERUP_INDICATION = 6;
    protected static final int EVENT_REMOTE_POWERDOWN_INDICATION = 7;
    protected static final int EVENT_REMOTE_RESET_INDICATION = 8;
    protected static final int EVENT_REMOTE_RADIO_STATE_INDICATION = 9;
    private static final int INVALID_TOKEN = -1;

    private static class Application {
        public String name;
        public String key;
        public boolean parsingFail;
    }

    private Map mUimRemoteClientWhiteList;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onCreate()");
        mContext = this;

        mSubscriptionManager = (SubscriptionManager) mContext
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        TelephonyManager manager =
                (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            simSlots = manager.getPhoneCount();
        }
        mUimRemoteClientProxy = new UimRemoteClientProxy(mContext);
        HandlerThread thread = new HandlerThread(LOG_TAG);
        thread.start();
        mResponseHandler = new ResponseHandler(thread.getLooper());
        mTelephonyManager = new TelephonyManager[simSlots];
        mRadioStateListener = new RadioStateListener[simSlots];
        for(int i = 0; i < simSlots; i++) {
            mUimRemoteClientProxy.registerResponseHandler(mResponseHandler, i);
            mRadioStateListener[i] = new RadioStateListener(i);
            int subId = SubscriptionManager.getSubscriptionId(i);
            if (mSubscriptionManager.isActiveSubscriptionId(subId) && manager != null) {
                Log.i(LOG_TAG, "slotIndex = " + i + ", subId = " + subId);
                mTelephonyManager[i] = manager.createForSubscriptionId(subId);
                mTelephonyManager[i].registerTelephonyCallback(
                    mContext.getMainExecutor(), mRadioStateListener[i]);
            }
        }
        //initing whitelist
        getWhiteList();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "onBind()");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy()");
        stopSelf();
        super.onDestroy();
    }

    private class RadioStateListener extends TelephonyCallback
                     implements TelephonyCallback.RadioPowerStateListener{

        int slotIndex;
        public RadioStateListener(int slotIndex) {
             Log.i(LOG_TAG, "slotIndex : " + slotIndex);
             this.slotIndex = slotIndex;
        }
        @Override
        public void onRadioPowerStateChanged(int state) {
            Log.i(LOG_TAG, "state : " + state + ", slot : " + slotIndex);
            Message msg = mRadioStateHdlr.obtainMessage(state, slotIndex);
            msg.sendToTarget();
        }
    }

    private Handler mRadioStateHdlr = new Handler() {
        public void handleMessage (Message msg) {
            try {
                int slotId = (int) msg.obj;
                Log.i(LOG_TAG, "handleMessage : slotIndex = " + slotId + ", state = " + msg.what);
                if (slotId >= simSlots || mTelephonyManager == null) {
                    return;
                }
                switch(msg.what) {
                    case TelephonyManager.RADIO_POWER_ON:
                        sendUimRemoteRadioStateIndication(slotId, 2);
                        break;
                    case TelephonyManager.RADIO_POWER_OFF:
                        sendUimRemoteRadioStateIndication(slotId, 1);
                        break;
                    case TelephonyManager.RADIO_POWER_UNAVAILABLE:
                        sendUimRemoteRadioStateIndication(slotId, 0);
                        break;
                    default:
                        break;
                }
            }
            catch (Exception e) {
                Log.e(LOG_TAG, "error occured when parsing the resp/ind");
            }
        }
    };

    private void sendUimRemoteRadioStateIndication(int slotId, int radioState) {
        if (mResponseHandler != null) {
            mResponseHandler.sendMessage(mResponseHandler.obtainMessage
                    (EVENT_REMOTE_RADIO_STATE_INDICATION, slotId, -1, radioState));
        }
    }

    private final IUimRemoteClientService.Stub mBinder = new IUimRemoteClientService.Stub() {
        public int registerCallback(IUimRemoteClientServiceCallback cb) throws RemoteException {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            UimRemoteClientService.this.mCb = cb;
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int deregisterCallback(IUimRemoteClientServiceCallback cb) throws RemoteException {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            UimRemoteClientService.this.mCb = null;
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int uimRemoteEvent(int slot, int event, byte[] atr, int errCode,
                    boolean has_transport, int transport, boolean has_usage, int usage,
                    boolean has_apdu_timeout, int apdu_timeout, boolean has_disable_all_polling,
                    int disable_all_polling, boolean has_poll_timer, int poll_timer)
                    throws RemoteException {
            UimRemoteEventReqType mEventType = new UimRemoteEventReqType();
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if(slot >= simSlots) {
                Log.e(LOG_TAG, "Sim Slot not supported!" + slot);
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if (mUimRemoteClientProxy == null) {
                Log.e(LOG_TAG, "service is not connected");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            int token = mUimRemoteClientProxy.uimRemoteEvent(slot, event, atr, errCode,
                    has_transport, transport, has_usage, usage, has_apdu_timeout,
                    apdu_timeout, has_disable_all_polling, disable_all_polling,
                    has_poll_timer, poll_timer);
            if (token == INVALID_TOKEN) {
                return UimRemoteError.UIM_REMOTE_ERROR;
            } else {
                return UimRemoteError.UIM_REMOTE_SUCCESS;
            }
        }

        public int uimRemoteApdu(int slot, int apduStatus, byte[] apduResp)
                throws RemoteException {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if(slot >= simSlots) {
                Log.e(LOG_TAG, "Sim Slot not supported!" + slot);
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if (mUimRemoteClientProxy == null) {
                Log.e(LOG_TAG, "service is not connected");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            int token = mUimRemoteClientProxy.uimRemoteApdu(slot, apduStatus, apduResp);
            if (token == INVALID_TOKEN) {
                return UimRemoteError.UIM_REMOTE_ERROR;
            } else {
                return UimRemoteError.UIM_REMOTE_SUCCESS;
            }
        }
    };

    private Application readApplication(XmlResourceParser parser)
                    throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "Application");
        Application app = new Application();
        int eventType = parser.next();
        while(eventType != XmlPullParser.END_TAG) {
            if(eventType != XmlPullParser.START_TAG) {
                app.parsingFail = true;
                Log.e(LOG_TAG, "parse fail");
                break;
            }
            String tagName = parser.getName();
            if(tagName.equals("PackageName")){
                eventType = parser.next();
                if(eventType == XmlPullParser.TEXT){
                    app.name = parser.getText();
                    eventType = parser.next();
                }
                if((eventType != XmlPullParser.END_TAG) || !(parser.getName().equals("PackageName"))){
                     //Invalid tag or invalid xml format
                    app.parsingFail = true;
                    Log.e(LOG_TAG, "parse fail");
                    break;
                }
            }
            else if(tagName.equals("SignatureHash")){
                eventType = parser.next();
                if(eventType == XmlPullParser.TEXT){
                    app.key = parser.getText();
                    eventType = parser.next();
                }
                if((eventType != XmlPullParser.END_TAG) || !(parser.getName().equals("SignatureHash"))){
                     //Invalid tag or invalid xml format
                    app.parsingFail = true;
                    Log.e(LOG_TAG, "parse fail");
                    break;
                }
            }
            else{
                 app.parsingFail = true;
                 Log.e(LOG_TAG, "parse fail" + tagName);
                 break;
            }
            eventType = parser.next();
        }
        if((eventType != XmlPullParser.END_TAG) || !(parser.getName().equals("Application"))){
            //End Tag that ended the loop is not Application
            app.parsingFail = true;
        }
        return app;
    }

    private void getWhiteList(){
        try {
            String name = null;
            Application app = null;
            boolean fail = false;
            int eventType;
            HashMap<String, String> table = new HashMap<String, String>();

            XmlResourceParser parser = this.getResources().getXml(R.xml.applist);
            parser.next();

            //Get the parser to point to Entries Tag.
            if(parser.getEventType() == XmlPullParser.START_DOCUMENT){
                name = parser.getName();
                while(name == null ||
                       (name != null && !name.equals("Entries"))){
                    parser.next();
                    name = parser.getName();
                }
            }

            parser.require(XmlPullParser.START_TAG, null, "Entries");
            eventType = parser.next();

            //Loop until END_TAG is encountered
            while(eventType != XmlPullParser.END_TAG) {

                //If the TAG is not a START_TAG, break the loop
                //with Failure.
                if(eventType != XmlPullParser.START_TAG) {
                    fail = true;
                    Log.e(LOG_TAG, "parse fail");
                    break;
                }

                name = parser.getName();
                if(name.equals("Application")) {
                    app = readApplication(parser);
                    if(app.parsingFail){
                        fail = true;
                        Log.e(LOG_TAG, "parse fail");
                        break;
                    }
                    else if(app.name != null || app.key != null){
                        table.put(app.name, app.key);
                    }
                }
                else {
                    fail = true;
                    Log.e(LOG_TAG, "parse fail" + name);
                    break;
                }
                eventType = parser.next();
            }
            if(fail || eventType != XmlPullParser.END_TAG ||
                         !(parser.getName().equals("Entries"))){
                //parsing failure
                Log.e(LOG_TAG, "FAIL");
            }
            else if(!table.isEmpty()) {
                 mUimRemoteClientWhiteList = Collections.unmodifiableMap(table);
            }
        }
        catch(Exception e) {
            Log.e(LOG_TAG, "Exception: "+ e);
        }
    }

    private static String bytesToHex(byte[] inputBytes) {
        final StringBuilder sb = new StringBuilder();
        for(byte b : inputBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    private boolean verifyAuthenticity(int uid){
        boolean ret = false;

        if(mUimRemoteClientWhiteList == null) {
            return ret;
        }
        String[] packageNames = mContext.getPackageManager().getPackagesForUid(uid);
        for(String packageName : packageNames){
            if(mUimRemoteClientWhiteList.containsKey(packageName)){
                String hash = (String)mUimRemoteClientWhiteList.get(packageName);
                String compareHash = new String();
                try {
                    Signature[] sigs = mContext.getPackageManager().getPackageInfo(
                            packageName, PackageManager.GET_SIGNATURES).signatures;
                    for(Signature sig: sigs) {

                        //get the raw certificate into input stream
                        final byte[] rawCert = sig.toByteArray();
                        InputStream certStream = new ByteArrayInputStream(rawCert);

                        //Read the X.509 Certificate into certBytes
                        final CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                        final X509Certificate x509Cert = (X509Certificate)certFactory.
                                generateCertificate(certStream);
                        byte[] certBytes = x509Cert.getEncoded();

                        //get the fixed SHA-1 cert
                        MessageDigest md = MessageDigest.getInstance("SHA-1");
	                    md.update(certBytes);
	                    byte[] certThumbprint = md.digest();

                        //cert in hex format
                        compareHash = bytesToHex(certThumbprint);

                        if(hash.equals(compareHash)) {
                            ret = true;
                            break;
                        }
                    }
                }
                catch(Exception e) {
                    Log.e(LOG_TAG, "Exception reading client data!" + e);
                }
            }
        }
        return ret;
    }

    public class ResponseHandler extends Handler {

        ResponseHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "handleMessage what = " + msg.what + ", slotId = " + msg.arg1);
            if (mCb == null) {
                Log.e(LOG_TAG, "No ClientService Callback...");
                return;
            }
            int slotId = msg.arg1;
            switch (msg.what) {
                case EVENT_REMOTE_EVENT_RESPONSE:
                    try {
                        int eventResp = (int) msg.obj;
                        mCb.uimRemoteEventResponse(slotId, eventResp);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteEventResponse: exception" + ex);
                    }
                    break;
                case EVENT_REMOTE_APDU_RESPONSE:
                    try {
                        int apduResp = (int) msg.obj;
                        mCb.uimRemoteApduResponse(slotId, apduResp);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteApduResponse: exception" + ex);
                    }
                    break;
                case EVENT_REMOTE_APDU_INDICATION:
                    try {
                        byte[] apduInd = (byte[]) msg.obj;
                        mCb.uimRemoteApduIndication(slotId, apduInd);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteApduIndication: exception" + ex);
                    }
                    break;
                case EVENT_REMOTE_CONNECT_INDICATION:
                    try {
                        mCb.uimRemoteConnectIndication(slotId);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteConnectIndication: exception" + ex);
                    }
                    break;
                case EVENT_REMOTE_DISCONNECT_INDICATION:
                    try {
                        mCb.uimRemoteDisconnectIndication(slotId);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteDisconnectIndication: exception" + ex);
                    }
                    break;
                case EVENT_REMOTE_POWERUP_INDICATION:
                    try {
                        mCb.uimRemotePowerUpIndication(slotId);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemotePowerUpIndication: exception" + ex);
                    }
                    break;
                case EVENT_REMOTE_POWERDOWN_INDICATION:
                    try {
                        mCb.uimRemotePowerDownIndication(slotId);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemotePowerDownIndication: exception" + ex);
                    }
                    break;
                case EVENT_REMOTE_RESET_INDICATION:
                    try {
                        mCb.uimRemoteResetIndication(slotId);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteResetIndication: exception" + ex);
                    }
                    break;
                case EVENT_REMOTE_RADIO_STATE_INDICATION:
                    try {
                        int radioState = (int) msg.obj;
                        mCb.uimRemoteRadioStateIndication(slotId, radioState);
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteRadioStateIndication: exception" + ex);
                    }
                    break;
            }
        }
    }
}
