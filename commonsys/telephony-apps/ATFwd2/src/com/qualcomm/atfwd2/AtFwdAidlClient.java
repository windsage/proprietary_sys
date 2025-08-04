/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd2;

import com.qualcomm.atfwd2.AtCmdHandler.AtCmdHandlerInstantiationException;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

 /**
  * Class used to send and receive requests/responses/indications to/from IAtFwd AIDL service.
  */
public class AtFwdAidlClient {
    private static final String TAG = "AtFwdAidlClient";
    private static final boolean DBG = true;
    private static final int EVENT_DO_ATFWD_CLIENT_INIT = 1;
    private static final int INIT_RETRY_INTERVAL = 10000;  // 10 seconds
    private static final int MAX_INIT_RETRY_ATTEMPTS = 5;

    /** Name of the IAtFwd service */
    static final String ATFWD_SERVICE_NAME =
        "vendor.qti.hardware.radio.atfwd.IAtFwd/AtFwdAidl";
    private vendor.qti.hardware.radio.atfwd.IAtFwd mAtFwdAidl;
    private vendor.qti.hardware.radio.atfwd.IAtFwdIndication mAtFwdIndicationAidl;

    private IBinder mBinder;
    private Context mContext;
    private Handler mHandler;
    private Object mAtFwdLock = new Object();

    /** Map of a command to its handler */
    private Map<String, AtCmdHandler> mCmdHandlers;

    /** The death recepient object that gets notified when IAtFwd service dies. */
    AtFwdDeathRecipient mDeathRecipient;

    /** Number of retry attempts to fetch the IAtFwd HAL service instance */
    private int mInitRetryCount = 0;

    public void cleanUp() {
        Log.d(TAG,"cleanUp");
        synchronized (mAtFwdLock) {
            if (mAtFwdAidl != null) {
                try {
                    mBinder.unlinkToDeath(mDeathRecipient, 0 /* Not used */);
                } catch (Exception ex) {
                    Log.e(TAG, "Unable to unlink death recipient", ex);
                }
                mAtFwdAidl = null;
                mAtFwdIndicationAidl = null;
            }
        }
        mInitRetryCount = 0;
    }

    public AtFwdAidlClient(Context context) {
        mContext = context;
        HandlerThread headlerThread = new HandlerThread(TAG);
        headlerThread.start();
        mHandler = new Handler(headlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case EVENT_DO_ATFWD_CLIENT_INIT:
                        Log.d(TAG, "EVENT_DO_ATFWD_CLIENT_INIT"
                                + ", retry count: " + (++mInitRetryCount));
                        if (mInitRetryCount <= MAX_INIT_RETRY_ATTEMPTS) {
                            initAtFwdAidlClient();
                        } else {
                            Log.d(TAG, "Exceeded max retry attempts");
                        }
                        break;
                }
            }
        };
        mDeathRecipient = new AtFwdDeathRecipient();
        initAtCmdHandlers(mContext);
        initAtFwdAidlClient();
    }

    private void initAtCmdHandlers(Context c) {
        mCmdHandlers = new HashMap<String, AtCmdHandler>();

        try {
            addHandler(new AtCkpdCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCtsaCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCfunCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCrslCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCssCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtQcpwrdnCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtClvlCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCmutCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtQcddsCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(TAG, "Unable to instantiate command", e);
        }
    }

    private void addHandler(AtCmdHandler cmdHandler) {
        mCmdHandlers.put(cmdHandler.getCommandName().toUpperCase(), cmdHandler);
    }

    /**
     * Initialize the instance of IAtFwd. Get the service and register the callback object
     * to be called for the unsolicited indications.
     */
    private void initAtFwdAidlClient() {
        // With the current architecture, ServiceManager.waitForDeclaredService() will wait for a
        // second before attempting to start the HAL service of its own in case its not already
        // running.
        // If the underlying modem does not have support for AT commands, then the HAL service will
        // not be started by the ATFWD-daemon, and the daemon itself will exit. In this case,
        // ServiceManager.waitForDeclaredService() will indefinitely try to bring up the HAL
        // service, in vain.
        // To prevent this, explicitly check if the HAL service has been brought up, and only then
        // attempt to fetch the service instance.

        mBinder = ServiceManager.checkService(ATFWD_SERVICE_NAME);

        if (mBinder == null) {
            // The HAL service is not up, recheck after 10 seconds, for at most 5 times
            Log.d(TAG, "HAL service is not up. Retry after 10 seconds");
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(EVENT_DO_ATFWD_CLIENT_INIT),
                    INIT_RETRY_INTERVAL);
            return;
        }

        mAtFwdAidl = vendor.qti.hardware.radio.atfwd.IAtFwd.Stub.asInterface(mBinder);
        if(mAtFwdAidl == null) {
            Log.e(TAG, "Get binder for AtFwd StableAIDL failed");
            return;
        }
        Log.d(TAG, "Get binder for AtFwd StableAIDL is successful");

        try {
            mBinder.linkToDeath(mDeathRecipient, 0 /* Not Used */);
        } catch (android.os.RemoteException ex) {
            Log.e(TAG, "linkToDeath failed", ex);
        }

        synchronized (mAtFwdLock) {
            mAtFwdIndicationAidl = new AtFwdIndicationAidl();
            try {
                Log.d(TAG, "Registering callback");
                mAtFwdAidl.setIndicationCallback(mAtFwdIndicationAidl);
            } catch (android.os.RemoteException ex) {
                Log.e(TAG, "Failed to call setCallbacks stable AIDL API", ex);
            }
        }
    }

    /**
     * Class that implements the binder death recipient to be notified when
     * IAtFwd service dies.
     */
    final class AtFwdDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service has died
         */
        @Override
        public void binderDied() {
            Log.e(TAG, "IAtFwd died");
            cleanUp();
            initAtFwdAidlClient();
        }
    }

    /**
     * Class that implements the IatFwdIndication interface, used to receive indications from
     */
    class AtFwdIndicationAidl extends
        vendor.qti.hardware.radio.atfwd.IAtFwdIndication.Stub {

        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.atfwd.IAtFwdIndication.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.atfwd.IAtFwdIndication.HASH;
        }

        @Override
        public void onAtCommandForwarded(
                int serial, vendor.qti.hardware.radio.atfwd.AtCmd command) {

            Log.i(TAG, "onAtCommandForwarded - serial: " + serial);
            processAtCommand(serial, command);
        }
    }

    private void processAtCommand(
            int serial, vendor.qti.hardware.radio.atfwd.AtCmd aidlAtCommand) {
        try {
            AtCmd atCommand = convertAidlAtCmd(aidlAtCommand);
            Log.d(TAG, "processAtCommand - serial: " + serial + ", atCommand: " + atCommand);

            // Fetch the class that will handle this AT command
            AtCmdHandler commandHandler = mCmdHandlers.get(atCommand.getName().toUpperCase());
            AtCmdResponse response;

            // Process the command
            if (commandHandler == null) {
                Log.e(TAG,"Handler not found for " + atCommand.getName().toUpperCase());
                response = new AtCmdResponse(AtCmdResponse.RESULT_ERROR, "+CME ERROR: 4");
            } else {
                Log.d(TAG,"Found handler for " + atCommand.getName().toUpperCase());
                response = commandHandler.handleCommand(atCommand);
            }

            // Send the result back to the native side
            sendAtCommandProcessedState(serial, response);
        } catch (Exception ex) {
            Log.e(TAG, "Exception processing AT command with serial: " + serial, ex);
            sendAtCommandProcessedState(
                    serial,
                    new AtCmdResponse(AtCmdResponse.RESULT_ERROR, "+CME ERROR: 2"));
        }
    }


    /**
     * Sends AT command processed state to native side
     *
     * @param serial - serial number of AT command as received in the indication
     * @param response - response containing the result identifier and the response string
     */
    private void sendAtCommandProcessedState(int serial, AtCmdResponse response) {
        try {
            Log.i(TAG, "sendAtCommandProcessedState "
                    + "- serial: " + serial
                    + ", response: {" + response.getResult()
                    + ", " + response.getResponse()
                    + "} ");
            if (mAtFwdAidl != null) {
                // Convert response instance to that of AIDL class
                vendor.qti.hardware.radio.atfwd.AtCmdResponse aidlResponse =
                        convertToAidlResponse(response);

                // Send the response to the native side
                mAtFwdAidl.sendAtCommandProcessedState(serial, aidlResponse);
            } else {
                Log.e(TAG, "sendAtCommandProcessedState: service is not connected");
            }
        } catch (Exception ex) {
            Log.e(TAG, "sendAtCommandProcessedState - exception", ex);
        }
    }

    /**
     * Convert AtCmdResponse instance to AIDL type
     */
    private vendor.qti.hardware.radio.atfwd.AtCmdResponse convertToAidlResponse(
            AtCmdResponse response) {
        vendor.qti.hardware.radio.atfwd.AtCmdResponse aidlResponse =
                new vendor.qti.hardware.radio.atfwd.AtCmdResponse();

        aidlResponse.result = response.getResult();
        aidlResponse.response = response.getResponse();

        // AIDL interface expects a non-null string. Change null to an empty string.
        if (aidlResponse.response == null) {
            aidlResponse.response = "";
        }
        return aidlResponse;
    }

    /**
     * Convert AIDL AtCmd to local type
     */
    private AtCmd convertAidlAtCmd(
            vendor.qti.hardware.radio.atfwd.AtCmd aidlAtCommand) throws NullPointerException {
        int numberOfTokenItems = aidlAtCommand.token.numberOfItems;

        if (numberOfTokenItems > 0) {
            String[] tokenItems = new String[numberOfTokenItems];
            for (int i = 0; i < numberOfTokenItems; i++) {
                tokenItems[i] = aidlAtCommand.token.items[i];
            }
            return new AtCmd(aidlAtCommand.opCode, aidlAtCommand.name, tokenItems);
        } else {
            return new AtCmd(aidlAtCommand.opCode, aidlAtCommand.name, null);
        }
    }
}
