/*
 * Copyright (c) 2014, 2015, 2019, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteserver;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HwBinder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager;
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

import vendor.qti.hardware.radio.uim_remote_server.V1_0.IUimRemoteServiceServer;
import vendor.qti.hardware.radio.uim_remote_server.V1_0.IUimRemoteServiceServerResponse;
import vendor.qti.hardware.radio.uim_remote_server.V1_0.IUimRemoteServiceServerIndication;
import vendor.qti.hardware.radio.uim_remote_server.V1_0.UimRemoteServiceServerApduType;
import vendor.qti.hardware.radio.uim_remote_server.V1_0.UimRemoteServiceServerResultCode;

public class UimRemoteServerService extends Service {
    private final String LOG_TAG = "UimRemoteServerService";

    public static class UimRemoteError {

        public static final int UIM_REMOTE_SUCCESS = 0;

        public static final int UIM_REMOTE_ERROR = 1;
    }

    private static class Application {
        public String name;
        public String key;
        public boolean parsingFail;
    }

    private Map mUimRemoteServerWhiteList;
    private Context mContext;
    private int simSlots = 0;
    private UimRemoteServerProxy mUimRemoteServerProxy;
    IUimRemoteServerServiceCallback mCb = null;
    private ResponseHandler mResponseHandler;
    protected static final int EVENT_SERVER_CONNECT_RESPONSE = 1;
    protected static final int EVENT_SERVER_DISCONNECT_RESPONSE = 2;
    protected static final int EVENT_SERVER_APDU_RESPONSE = 3;
    protected static final int EVENT_SERVER_TRANSFER_ATR_RESPONSE = 4;
    protected static final int EVENT_SERVER_POWER_RESPONSE = 5;
    protected static final int EVENT_SERVER_RESET_SIM_RESPONSE = 6;
    protected static final int EVENT_SERVER_TRANSFER_CARD_READER_STATUS_RESPONSE = 7;
    protected static final int EVENT_SERVER_ERROR_RESPONSE = 8;
    protected static final int EVENT_SERVER_TRANSFER_PROTOCOL_RESPONSE = 9;
    protected static final int EVENT_SERVER_DISCONNECT_INDICATION = 10;
    protected static final int EVENT_SERVER_STATUS_INDICATION = 11;
    private static final int INVALID_TOKEN = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onCreate()");
        mContext = this;

        TelephonyManager manager =
                (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        simSlots = manager.getPhoneCount();
        mUimRemoteServerProxy = new UimRemoteServerProxy(mContext);
        HandlerThread thread = new HandlerThread(LOG_TAG);
        thread.start();
        mResponseHandler = new ResponseHandler(thread.getLooper());
        for(int i = 0; i < simSlots; i++) {
            mUimRemoteServerProxy.registerResponseHandler(mResponseHandler,i);
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

    private final IUimRemoteServerService.Stub mBinder = new IUimRemoteServerService.Stub() {
        public int registerCallback(IUimRemoteServerServiceCallback cb) {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.i(LOG_TAG, "register callback: Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            UimRemoteServerService.this.mCb = cb;
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int deregisterCallback(IUimRemoteServerServiceCallback cb) {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "deregister Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            UimRemoteServerService.this.mCb = null;
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int uimRemoteServerConnectReq(int slot, int maxMessageSize) {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Connect Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if(slot >= simSlots) {
                Log.e(LOG_TAG, "Sim Slot not supported!" + slot);
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if (mUimRemoteServerProxy == null) {
                Log.e(LOG_TAG, "service is not connected");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            Log.d(LOG_TAG, "uimRemoteServerConnectReq() - maxMessageSize: "
                    + maxMessageSize + "slot: " + slot);
            try {
                int token = mUimRemoteServerProxy.uimRemoteServerConnectReq(
                        slot, maxMessageSize);
                if (token == INVALID_TOKEN) {
                    return UimRemoteError.UIM_REMOTE_ERROR;
                }
            } catch(RemoteException ex) {
                Log.e(LOG_TAG, "uimRemoteEvent: exception" + ex);
            }
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int uimRemoteServerDisconnectReq(int slot) {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Disconnect Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if(slot >= simSlots) {
                Log.e(LOG_TAG, "Sim Slot not supported!" + slot);
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if (mUimRemoteServerProxy == null) {
                Log.e(LOG_TAG, "service is not connected");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            Log.d(LOG_TAG, "uimRemoteServerDisconnectReq() slot: " + slot);
            try {
                int token = mUimRemoteServerProxy.uimRemoteServerDisconnectReq(slot);
                if (token == INVALID_TOKEN) {
                    return UimRemoteError.UIM_REMOTE_ERROR;
                }
            } catch(RemoteException ex) {
                Log.e(LOG_TAG, "uimRemoteEvent: exception" + ex);
            }
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int uimRemoteServerApduReq(int slot, byte[] cmd) {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Apdu Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if(slot >= simSlots) {
                Log.e(LOG_TAG, "Sim Slot not supported!" + slot);
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if (mUimRemoteServerProxy == null) {
                Log.e(LOG_TAG, "service is not connected");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            Log.d(LOG_TAG, "uimRemoteServerApduReq() - cmd length: " + cmd.length +
                    " slot: " + slot);
            try {
                int token = mUimRemoteServerProxy.uimRemoteServerApduReq(slot, cmd);
                if (token == INVALID_TOKEN) {
                    return UimRemoteError.UIM_REMOTE_ERROR;
                }
            } catch(RemoteException ex) {
                Log.e(LOG_TAG, "uimRemoteEvent: exception" + ex);
            }
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int uimRemoteServerTransferAtrReq(int slot) {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "ATR Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if(slot >= simSlots) {
                Log.e(LOG_TAG, "Sim Slot not supported!" + slot);
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if (mUimRemoteServerProxy == null) {
                Log.e(LOG_TAG, "service is not connected");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            Log.d(LOG_TAG, "uimRemoteServerTransferAtrReq() slot: " + slot);
            try {
                int token = mUimRemoteServerProxy.uimRemoteServerTransferAtrReq(slot);
                if (token == INVALID_TOKEN) {
                    return UimRemoteError.UIM_REMOTE_ERROR;
                }
            } catch(RemoteException ex) {
                Log.e(LOG_TAG, "uimRemoteEvent: exception" + ex);
            }
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int uimRemoteServerPowerReq(int slot, boolean state) {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Power Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if(slot >= simSlots) {
                Log.e(LOG_TAG, "Sim Slot not supported!" + slot);
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if (mUimRemoteServerProxy == null) {
                Log.e(LOG_TAG, "service is not connected");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            Log.d(LOG_TAG, "uimRemoteServerPowerReq() - state: " + state + " slot: " + slot);
            try {
                int token = mUimRemoteServerProxy.uimRemoteServerPowerReq(slot, state);
                if (token == INVALID_TOKEN) {
                    return UimRemoteError.UIM_REMOTE_ERROR;
                }
            } catch(RemoteException ex) {
                Log.e(LOG_TAG, "uimRemoteEvent: exception" + ex);
            }
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }

        public int uimRemoteServerResetSimReq(int slot) {
            if(!verifyAuthenticity(mBinder.getCallingUid())) {
                Log.d(LOG_TAG, "Reset Cannot perform! returning failure");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if(slot >= simSlots) {
                Log.e(LOG_TAG, "Sim Slot not supported!" + slot);
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            if (mUimRemoteServerProxy == null) {
                Log.e(LOG_TAG, "service is not connected");
                return UimRemoteError.UIM_REMOTE_ERROR;
            }
            Log.d(LOG_TAG, "uimRemoteServerResetSimReq() slot: " + slot);
            try {
                int token = mUimRemoteServerProxy.uimRemoteServerResetSimReq(slot);
                if (token == INVALID_TOKEN) {
                    return UimRemoteError.UIM_REMOTE_ERROR;
                }
            } catch(RemoteException ex) {
                Log.e(LOG_TAG, "uimRemoteEvent: exception" + ex);
            }
            return UimRemoteError.UIM_REMOTE_SUCCESS;
        }
    };

    private Application readApplication(XmlResourceParser parser) throws XmlPullParserException, IOException {
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
                    if(app.parsingFail)
                    {
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
                 mUimRemoteServerWhiteList = Collections.unmodifiableMap(table);
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

        if(mUimRemoteServerWhiteList == null) {
            Log.e(LOG_TAG, "empty white list");
            return ret;
        }
        String[] packageNames = mContext.getPackageManager().getPackagesForUid(uid);
        for(String packageName : packageNames){
            if(mUimRemoteServerWhiteList.containsKey(packageName)){
                String hash = (String)mUimRemoteServerWhiteList.get(packageName);
                String compareHash = new String();
                try {
                    Signature[] sigs = mContext.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;
                    for(Signature sig: sigs) {

                        //get the raw certificate into input stream
                        final byte[] rawCert = sig.toByteArray();
                        InputStream certStream = new ByteArrayInputStream(rawCert);

                        //Read the X.509 Certificate into certBytes
                        final CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                        final X509Certificate x509Cert = (X509Certificate)certFactory.generateCertificate(certStream);
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
            Log.d(LOG_TAG, "handleMessage what = " + msg.what);
            if (mCb == null) {
                Log.e(LOG_TAG, "No ServerService Callback ...");
                return;
            }
            int slotId = msg.arg1;
            switch (msg.what) {
                case EVENT_SERVER_CONNECT_RESPONSE:
                    try {
                        UimRemoteServerResult result = (UimRemoteServerResult) msg.obj;
                        mCb.uimRemoteServerConnectResp(slotId, result.getSapConnectRsp(),
                                result.getMaxMsgSize());
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "ServerConnectResponse: exception" + ex);
                    }
                    break;
                case EVENT_SERVER_DISCONNECT_RESPONSE:
                    try {
                        mCb.uimRemoteServerDisconnectResp(slotId, 0);  // success
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "DisconnectResp: exception" + ex);
                    }
                    break;
                case EVENT_SERVER_APDU_RESPONSE:
                    try {
                        UimRemoteServerResult result = (UimRemoteServerResult) msg.obj;
                        mCb.uimRemoteServerApduResp(slotId, convertResultCodeToStatus(
                                result.getResultCode(), false), result.getApduRsp());
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "ServerApduResp: exception" + ex);
                    }
                    break;
                case EVENT_SERVER_TRANSFER_ATR_RESPONSE:
                    try {
                        UimRemoteServerResult result = (UimRemoteServerResult) msg.obj;
                        mCb.uimRemoteServerTransferAtrResp(slotId, convertResultCodeToStatus(
                                result.getResultCode(), false), result.getAtr());
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "ServerConnectResponse: exception" + ex);
                    }
                    break;
                case EVENT_SERVER_POWER_RESPONSE:
                    try {
                        UimRemoteServerResult result = (UimRemoteServerResult) msg.obj;
                        mCb.uimRemoteServerPowerResp(slotId, convertResultCodeToStatus(
                                result.getResultCode(), true));
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteServerPowerResp: exception" + ex);
                    }
                    break;
                case EVENT_SERVER_RESET_SIM_RESPONSE:
                    try {
                        UimRemoteServerResult result = (UimRemoteServerResult) msg.obj;
                        mCb.uimRemoteServerResetSimResp(slotId, convertResultCodeToStatus(
                                result.getResultCode(), true));
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteServerResetSimResp: exception" + ex);
                    }
                    break;
                case EVENT_SERVER_DISCONNECT_INDICATION:
                    try {
                        UimRemoteServerResult result = (UimRemoteServerResult) msg.obj;
                        mCb.uimRemoteServerDisconnectInd(slotId, result.getDisconnectType());
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteServerDisconnectInd: exception" + ex);
                    }
                    break;
                case EVENT_SERVER_STATUS_INDICATION:
                    try {
                        UimRemoteServerResult result = (UimRemoteServerResult) msg.obj;
                        mCb.uimRemoteServerStatusInd(slotId, result.getStatus());
                    } catch(RemoteException ex) {
                        Log.e(LOG_TAG, "uimRemoteServerStatusInd: exception" + ex);
                    }
                    break;
            }
        }
    }

    private int convertResultCodeToStatus(int resultCode, boolean response) {
        int respStatus = 0;
        switch(resultCode)
        {
            case UimRemoteServiceServerResultCode.UIM_REMOTE_SERVICE_SERVER_SUCCESS:
                // UIM_REMOTE_SERVER_SUCCESS
                respStatus = 0;
                break;
           case UimRemoteServiceServerResultCode.
                    UIM_REMOTE_SERVICE_SERVER_CARD_NOT_ACCESSSIBLE:
                // UIM_REMOTE_SERVER_SIM_NOT_READY
                respStatus = 2;
                break;
            case UimRemoteServiceServerResultCode.
                    UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_OFF:
                // UIM_REMOTE_SERVER_SIM_ALREADY_POWERED_OFF
                if (response) {
                    respStatus = 17;
                } else {
                    respStatus = 3;
                }
                break;
            case UimRemoteServiceServerResultCode.UIM_REMOTE_SERVICE_SERVER_CARD_REMOVED:
                // UIM_REMOTE_SERVER_SIM_ABSENT
                if (response) {
                    respStatus = 11;
                } else {
                    respStatus = 4;
                }
                break;
            case UimRemoteServiceServerResultCode.
                    UIM_REMOTE_SERVICE_SERVER_DATA_NOT_AVAILABLE:
                // UIM_REMOTE_SERVER_SIM_DATA_NOT_AVAILABLE
                respStatus = 6;
                break;
            case UimRemoteServiceServerResultCode.
                    UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_ON:
                // UIM_REMOTE_SERVER_SIM_ALREADY_POWERED_ON
                respStatus = 18;
                break;
            default:
                if (response) {
                    // UIM_REMOTE_SERVER_GENERIC_FAILURE
                    respStatus = 2;
                } else {
                    // UIM_REMOTE_SERVER_INTERNAL_FAILURE
                    respStatus = 1;
                }
                break;
        }
        return respStatus;
    }
}
