/**
 * Copyright (c) 2014, 2023, 2024 Qualcomm Technologies, Inc. All Rights Reserved.
 * Qualcomm Technologies Proprietary and Confidential.
 */

package com.qti.xdivert;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.telephony.CallForwardingInfo;

import android.telephony.TelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.AttributeSet;
import android.widget.Toast;

import java.util.function.Consumer;

import org.codeaurora.telephony.utils.RegistrantList;

// This class handles the actual processing required for XDivert feature.
// Handles the checkbox display.

public class XDivertCheckBoxPreference extends CheckBoxPreference {
    private static final String LOG_TAG = "XDivertCheckBoxPreference";
    private final boolean DBG = true; //(PhoneGlobals.DBG_LEVEL >= 2);

    int mNumPhones;
    int mAction; // Holds the CFNRc value.i.e.Registration/Disable
    int mReason; // Holds Call Forward reason.i.e.CF_REASON_NOT_REACHABLE
    String[] mLine1Number; // Holds the line numbers for both the slots
    String[] mCFLine1Number;// Holds the CFNRc number for both the slots
    String[] mCFBLine1Number;// Holds the CFB number for both the slots

    // Holds the status of Call Waiting for both the slot
    boolean[] mSlotCallWaiting;

    // Holds the value of XDivert feature
    boolean mXdivertStatus;
    TimeConsumingPreferenceListener mTcpListener;
    private static Context mContext;
    private XDivertUtility mXDivertUtility;
    private SubscriptionManager mSubscriptionManager;
    private Handler mGetoptionCompleteHandler, mSetOptionCompleteHandler,
            mRevertOptionCompleteHandler, mResponseNotReceivedHandler;
    private HandlerExecutor mHandlerExecutor;
    private final Looper mLooper;
    private TelephonyManager mTelephonyManager;
    private HandlerThread mHandlerThread;

    private static final int SLOT1 = 0;
    private static final int SLOT2 = 1;
    private static final int MESSAGE_GET_CFNRC = 2;
    private static final int MESSAGE_GET_CALL_WAITING = 3;
    private static final int MESSAGE_GET_CFB = 4;
    private static final int MESSAGE_SET_CFNRC = 5;
    private static final int MESSAGE_SET_CALL_WAITING = 6;
    private static final int MESSAGE_SET_CFB = 7;
    private static final int REVERT_SET_CFNRC = 8;
    private static final int REVERT_SET_CALL_WAITING = 9;
    private static final int START = 10;
    private static final int STOP = 11;
    private static final int INVALID_SLOT = 12;
    private static final int MESSAGE_NO_RESPONSE = 13;
    private static final int TIME = 20;
    private static final int WAIT_TIMEOUT = 20000;
    static final int RESPONSE_ERROR = 400; //Should be in sync with TimeConsumingPreferenceActivity

    public XDivertCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mHandlerThread = new HandlerThread("XdivertHandlerThread");
        mHandlerThread.start();
        mLooper = mHandlerThread.getLooper();
        mTelephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mGetoptionCompleteHandler = new GetOptionCompleteHandler(mLooper);
        mSetOptionCompleteHandler = new SetOptionCompleteHandler(mLooper);
        mRevertOptionCompleteHandler = new RevertOptionCompleteHandler(mLooper);
        mResponseNotReceivedHandler = new ResponseNotReceivedHandler(mLooper);
        mHandlerExecutor = new HandlerExecutor(mGetoptionCompleteHandler);
    }

    public XDivertCheckBoxPreference(Context context) {
        this(context, null);
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading, String[] line1Number){
        mTcpListener = listener;

        mXDivertUtility = XDivertUtility.getInstance();

        mNumPhones = mTelephonyManager.getActiveModemCount();
        // Store the numbers to shared preference
        for (int i = 0; i < mNumPhones; i++) {
            Log.d(LOG_TAG, "init slot" + i + " = " + line1Number[i]);
            mXDivertUtility.storeNumber(line1Number[i], i);
        }

        processStartDialog(START, true);
        if (!skipReading) {
            mLine1Number = new String[mNumPhones];
            mCFLine1Number = new String[mNumPhones];
            mSlotCallWaiting = new boolean[mNumPhones];
            mCFBLine1Number = new String[mNumPhones];
            for (int i = 0; i < mNumPhones; i++) {
                mLine1Number[i] = line1Number[i];
            }

            //Query for CFNRc for SLOT1.
            mGetoptionCompleteHandler.sendMessage(mGetoptionCompleteHandler
                .obtainMessage(MESSAGE_GET_CFNRC, SLOT1, 0));
        }
    }

    @Override
    protected void onClick() {
        super.onClick();

        processStartDialog(START, false);
        mSlotCallWaiting[SLOT1] = mXdivertStatus;
        mSlotCallWaiting[SLOT2] = mXdivertStatus;

        mReason = CallForwardingInfo.REASON_NOT_REACHABLE;

        //Check if CFNRc(SLOT1) & CW(SLOT1) is already set due to Partial Setting operation.
        //if set,then send the request for SLOT2, else for SLOT1.
        boolean requestForSlot1 = PhoneNumberUtils.compare(mCFLine1Number[SLOT1],
                mLine1Number[SLOT2]);

        if ((requestForSlot1) && (requestForSlot1 == mSlotCallWaiting[SLOT1])
                && !mXdivertStatus) {
            // Due to limitation in lower layers, back-to-back requests to SLOT1 & SLOT2
            // cannot be sent.For ex: CF request on SLOT1 followed by CF request on SLOT2.
            // Miminum of 3000ms delay is needed to send the 2nd request.
            // Hence added postDelayed to CF request for SLOT2.

            // Set CFNRc for SLOT2.
            CallForwardingInfo callForwardingInfo = new CallForwardingInfo(!mXdivertStatus
                    /* enabled */, mReason, mLine1Number[SLOT1], TIME);

            mSetOptionCompleteHandler.sendMessageDelayed(mSetOptionCompleteHandler
                    .obtainMessage(MESSAGE_SET_CFNRC, SLOT2, 0, callForwardingInfo), 1000);
        } else {
            // Due to limitation in lower layers, back-to-back requests to SLOT1 & SLOT2
            // cannot be sent.For ex: CF request on SLOT2 followed by CF request on SLOT1.
            // Miminum of 3000ms delay is needed to send the 2nd request.
            // Hence added postDelayed to CF request for SLOT1.

            //Set CFNRc for SLOT1.
            CallForwardingInfo callForwardingInfo = new CallForwardingInfo(!mXdivertStatus
                    /* enabled*/, mReason, mLine1Number[SLOT2], TIME);

            mSetOptionCompleteHandler.sendMessageDelayed(mSetOptionCompleteHandler
                    .obtainMessage(MESSAGE_SET_CFNRC, SLOT1, 0, callForwardingInfo), 1000);
        }
    }

    private void sendDelayedMessageForNoResponse() {
        mResponseNotReceivedHandler.sendMessageDelayed(mResponseNotReceivedHandler
                .obtainMessage(MESSAGE_NO_RESPONSE), WAIT_TIMEOUT);
    }

    void queryCallWaiting(int slotId) {
        //Get Call Waiting for "slotId" slot
        mGetoptionCompleteHandler.sendMessage(mGetoptionCompleteHandler
                .obtainMessage(MESSAGE_GET_CALL_WAITING, slotId, 0));
    }

    void queryCallForwardingBusy(int slotId) {
        //Get call Forwarding Busy status
        mGetoptionCompleteHandler.sendMessage(mGetoptionCompleteHandler
                .obtainMessage(MESSAGE_GET_CFB, slotId, 0));
    }

    private boolean validateXDivert() {
        // Compares if - SLot1 line number == CFNRc number of Slot2
        // Slot2 line number == CFNRc number of Slot1.
        boolean check1 = PhoneNumberUtils.compare(mCFLine1Number[SLOT1], mLine1Number[SLOT2]);
        boolean check2 = PhoneNumberUtils.compare(mCFLine1Number[SLOT2], mLine1Number[SLOT1]);
        boolean status = false;
        Log.d(LOG_TAG," CFNR SLOT1 = " + check1 + " CFNR SLOT2 = " + check2 +
               " mSlotCallWaiting = " + mSlotCallWaiting[SLOT1] + " mSlotCallWaiting = "
               + mSlotCallWaiting[SLOT2]);
        if ((mCFLine1Number[SLOT1] != null) && (mCFLine1Number[SLOT2] != null)) {
            if ((check1) && (check1 == check2)) {
                if (mSlotCallWaiting[SLOT1] && (mSlotCallWaiting[SLOT1] ==
                        mSlotCallWaiting[SLOT2])) {
                    status = true;
                }
            }
        }
        return status;
    }

    private boolean validateSmartCallForward(int slotId) {
        int nextSlotId = mXDivertUtility.getNextPhoneId(slotId);
        return PhoneNumberUtils.compare(mCFBLine1Number[slotId], mLine1Number[nextSlotId]);
    }

    public void displayAlertMessage(boolean status) {
        int slotStatus[] = {R.string.xdivert_not_active, R.string.xdivert_not_active};
        int resSlotId[] = {R.string.sub_1, R.string.sub_2};
        String dispMsg = this.getContext().getString(R.string.xdivert_status_active);
        if (!status) {
            dispMsg = this.getContext().getString(R.string.xdivert_status_not_active);
        }

        Log.d(LOG_TAG, "displayAlertMessage:  dispMsg = " + dispMsg);
        //check if activity is not in finishing state to diplay alert box
        if (!(((Activity)(this.getContext())).isFinishing())){
           new AlertDialog.Builder(this.getContext())
           .setTitle(R.string.xdivert_status)
           .setMessage(dispMsg)
           .setIcon(android.R.drawable.ic_dialog_alert)
           .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                           Log.d(LOG_TAG, "displayAlertMessage:  onClick");
                         }
                      })
           .show()
           .setOnDismissListener(new DialogInterface.OnDismissListener() {
                      public void onDismiss(DialogInterface dialog) {
                           Log.d(LOG_TAG, "displayAlertMessage:  onDismiss");
                        }
                     });
           }
    }

    private void processStopDialog(final int state, final boolean read) {
        if (mTcpListener != null) {
            Log.d(LOG_TAG,"stop");
            new Handler(Looper.getMainLooper()).post(() -> {
                mTcpListener.onFinished(XDivertCheckBoxPreference.this, read);
            });
        }
    }

    private void processError(int error) {
        if (mTcpListener != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                mTcpListener.onError(XDivertCheckBoxPreference.this, error);
            });
        }
    }

    private void processStartDialog(final int state, final boolean read) {
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                int mode = state;
                if (mode == START) {
                    if (mTcpListener != null) {
                        Log.d(LOG_TAG,"start");
                        mTcpListener.onStarted(XDivertCheckBoxPreference.this, read);
                    }
                }
                Looper.loop();
            }
        }).start();
    }

    private TelephonyManager getTelMgr(int slotId) {
        int subId = mSubscriptionManager.getSubscriptionId(slotId);
        if (mSubscriptionManager.isActiveSubscriptionId(subId)) {
            return mTelephonyManager.createForSubscriptionId(subId);
        }
        Log.w(LOG_TAG, "getTelMgr: no valid subid for slot " + slotId);
        return null;
    }

    class GetOptionCompleteHandler extends Handler {

        GetOptionCompleteHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            sendDelayedMessageForNoResponse();
            switch (msg.what) {
                case MESSAGE_GET_CFNRC:
                    handleGetCFNRC(msg.arg1);
                    break;
                case MESSAGE_GET_CALL_WAITING:
                    handleGetCallWaiting(msg.arg1);
                    break;
                case MESSAGE_GET_CFB:
                    handleGetCFB(msg.arg1);
            }
        }

        class CallForwardInfoCallback implements TelephonyManager.CallForwardingInfoCallback {
            int slotId;

            CallForwardInfoCallback(int slotId) {
                this.slotId = slotId;
            }

            @Override
            public void onCallForwardingInfoAvailable(CallForwardingInfo info) {
                if (info != null) {
                    mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                    if (info.getReason() == CallForwardingInfo.REASON_NOT_REACHABLE) {
                        handleGetCFNRCResponse(info, slotId);
                    } else if (info.getReason() == CallForwardingInfo.REASON_BUSY) {
                        handleGetCFBResponse(info, slotId);
                    } else {
                        processError(RESPONSE_ERROR);
                        processStopDialog(STOP, true);
                    }
                }
            }

            @Override
            public void onError(int error) {
                mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                processError(getErrorCode(error));
                processStopDialog(STOP, true);
            }
        }

        private void handleGetCFNRC(int slotId) {
            TelephonyManager telMgr = getTelMgr(slotId);
            if (telMgr != null) {
                telMgr.getCallForwarding(CallForwardingInfo.REASON_NOT_REACHABLE,
                        mHandlerExecutor, new CallForwardInfoCallback(slotId));
            } else {
                mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                processError(TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
                processStopDialog(STOP, true);
            }
        }

        private void handleGetCallWaiting(int slotId) {
            TelephonyManager telMgr = getTelMgr(slotId);
            if (telMgr != null) {
                telMgr.getCallWaitingStatus(mHandlerExecutor, new Consumer<Integer>() {
                    @Override
                    public void accept(Integer result) {
                        mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                        if (result == TelephonyManager.CALL_WAITING_STATUS_UNKNOWN_ERROR
                                || result == TelephonyManager.CALL_WAITING_STATUS_NOT_SUPPORTED) {
                            processError(RESPONSE_ERROR);
                            processStopDialog(STOP, true);
                        } else {
                            handleGetCallWaitingResponse(slotId, result);
                        }
                    }
                });
            } else {
                mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                processError(TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
                processStopDialog(STOP, true);
            }
        }

        private void handleGetCFB(int slotId) {
            TelephonyManager telMgr = getTelMgr(slotId);
            if (telMgr != null) {
                telMgr.getCallForwarding(CallForwardingInfo.REASON_BUSY, mHandlerExecutor,
                     new CallForwardInfoCallback(slotId));
            } else {
                mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                processError(TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
                processStopDialog(STOP, true);
            }
        }
    }

    class SetOptionCompleteHandler extends Handler {

        SetOptionCompleteHandler (Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            sendDelayedMessageForNoResponse();
            switch (msg.what) {
                case MESSAGE_SET_CFNRC:
                    handleSetCFNRC(msg.arg1, (CallForwardingInfo) msg.obj);
                    break;

                case MESSAGE_SET_CALL_WAITING:
                    handleSetCallWaiting(msg.arg1);
                    break;

                case MESSAGE_SET_CFB:
                    handleSetCFB(msg.arg1, (CallForwardingInfo) msg.obj);
                    break;
            }
        }

        private void handleSetCallWaiting(int slotId) {
            TelephonyManager telMgr = getTelMgr(slotId);

            if (telMgr != null) {
                telMgr.setCallWaitingEnabled(true /*enabled*/, mHandlerExecutor,
                        new Consumer<Integer>() {
                            @Override
                            public void accept(Integer result) {
                                if (result == TelephonyManager.CALL_WAITING_STATUS_ENABLED) {
                                    mResponseNotReceivedHandler.removeMessages(
                                            MESSAGE_NO_RESPONSE);
                                    handleSetCallWaitingResponse(slotId);
                                } else {
                                    handleRevertOperation(slotId, REVERT_SET_CALL_WAITING);
                                }
                            }
                });
            } else {
                mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                processError(TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
                processStopDialog(STOP, false);
            }
        }

        private void handleSetCFB(int slotId, CallForwardingInfo cfi) {
            TelephonyManager telMgr = getTelMgr(slotId);

            if (telMgr != null) {
                Log.d(LOG_TAG, "handleSetCFB slotId:: "+slotId+ "cfi:: " +cfi);
                telMgr.setCallForwarding(cfi, mHandlerExecutor, new Consumer<Integer>() {
                    @Override
                    public void accept(Integer result) {
                    mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                        if (result == TelephonyManager.CallForwardingInfoCallback.
                                RESULT_SUCCESS) {
                            handleSetCFBResponse(slotId);
                        } else {
                            if (mTcpListener != null) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    mTcpListener.onError(XDivertCheckBoxPreference.this,
                                            RESPONSE_ERROR);
                                    mTcpListener.onFinished(XDivertCheckBoxPreference.this, false);
                                });
                            }
                        }
                    }
                });
            }  else {
                mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                processError(TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
                processStopDialog(STOP, false);
            }
        }

        private void handleSetCFNRC(int slotId, CallForwardingInfo cfi) {
            TelephonyManager telMgr = getTelMgr(slotId);

            if (telMgr != null) {
                Log.d(LOG_TAG, "handleSetCFNRC slotId:: "+slotId+ "cfi:: " +cfi);
                telMgr.setCallForwarding(cfi, mHandlerExecutor, new Consumer<Integer>() {
                    @Override
                    public void accept(Integer result) {
                        mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                        if (result == TelephonyManager.CallForwardingInfoCallback.
                                RESULT_SUCCESS) {
                            handleSetCFNRCResponse(slotId);
                        } else {
                            processError(getErrorCode(result));
                            handleRevertOperation(slotId, REVERT_SET_CFNRC);
                        }
                    }
                });
            } else {
                mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                processError(TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
                processStopDialog(STOP, false);
            }
        }
    }

    /*Revert operations would be handled as follows:
    **case 1: CFNRc(SLOT1)->failure
    **        No Revert operation.
    **case 2: CFNRc(SLOT1)->success, CW(SLOT1)->failure
    **        Revert CFNRc(SLOT1).
    **case 3: CFNRc(SLOT1)->success, CW(SLOT1)->success,
    **        CFNRc(SLOT2)->failure
    **        No Revert operation. Display toast msg stating XDivert set only for Slot0.
    **case 4: CFNRc(SLOT1)->success, CW(SLOT1)->success,
    **        CFNRc(SLOT2)->success, CW(SLOT2)->failure
    **        Revert CFNRc(SLOT2) and display toast msg stating XDivert set only for Slot0.
    */
    class RevertOptionCompleteHandler extends Handler {

        RevertOptionCompleteHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            sendDelayedMessageForNoResponse();
            switch (msg.what) {
                case REVERT_SET_CFNRC:
                    handleRevertSetCFNRC(msg.arg1, (CallForwardingInfo) msg.obj);
                    break;
            }
        }
    }

    class ResponseNotReceivedHandler extends Handler {

        ResponseNotReceivedHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_NO_RESPONSE:
                    handleResponseNotReceived();
            }
        }

        private void handleResponseNotReceived() {
            processError(RESPONSE_ERROR);
            processStopDialog(STOP, true);
        }
    }

    private void handleRevertSetCFNRC(int slotId, CallForwardingInfo cfi) {
        if (DBG) Log.d(LOG_TAG, "handleRevertSetCFNRC: done slotId = " + slotId);

        processStopDialog(STOP, false);

        TelephonyManager telMgr = getTelMgr(slotId);
        if (telMgr != null) {
            telMgr.setCallForwarding(cfi, mHandlerExecutor, new Consumer<Integer>() {
                    @Override
                    public void accept(Integer result) {
                        mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
                        if (result != TelephonyManager.CallForwardingInfoCallback.
                                RESULT_SUCCESS) {
                            processError(getErrorCode(result));
                        }
                    }
            });
        } else {
            mResponseNotReceivedHandler.removeMessages(MESSAGE_NO_RESPONSE);
            processError(TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
        }
        Toast toast = Toast.makeText(this.getContext(),
                R.string.xdivert_partial_set, Toast.LENGTH_LONG);
        toast.show();
    }

    private void handleGetCFNRCResponse(CallForwardingInfo cfresponse, int slotId) {
        if (DBG) Log.d(LOG_TAG, "handleGetCFResponse: done slotId = " + slotId);
        if (slotId == SLOT1) {
            if (cfresponse.isEnabled()) {
                mCFLine1Number[SLOT1] = cfresponse.getNumber();
            }

            //Query Call Waiting for SLOT1
            queryCallWaiting(SLOT1);
        } else if (slotId == SLOT2) {
            if (cfresponse.isEnabled()) {
                mCFLine1Number[SLOT2] = cfresponse.getNumber();
            }
            //Query Call Waiting for SLOT2
            queryCallWaiting(SLOT2);
        }
    }

    private void handleGetCFBResponse(CallForwardingInfo cfresponse, int slotId) {
        if (DBG) Log.d(LOG_TAG, "handleGetCFBResponse: done slotId = " + slotId);
        if (cfresponse.isEnabled()) {
            mCFBLine1Number[slotId] = cfresponse.getNumber();
        }

        processStopDialog(STOP, true);

        //Check if CF numbers match the slot's phone numbers and
        //Call Waiting is enabled and
        //Call forwarding Busy is enabled then set the checkbox accordingly.
        new Handler(Looper.getMainLooper()).post(() -> {
            mXdivertStatus = validateXDivert();
            mXdivertStatus &= validateSmartCallForward(slotId);
            displayAlertMessage(mXdivertStatus);
            setChecked(mXdivertStatus);
            mXDivertUtility.onXDivertChanged(mXdivertStatus);
            mXDivertUtility.setXDivertStatus(mXdivertStatus);
        });
    }

    private void handleSetCFBResponse(int slotId) {
        if (DBG) Log.d(LOG_TAG, "handleSetCFBResponse: done on slotId = " + slotId);

        int nextSlotId = mXDivertUtility.getNextPhoneId(slotId);
        mCFBLine1Number[slotId] = mLine1Number[nextSlotId];

        if (mTcpListener != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                mTcpListener.onFinished(XDivertCheckBoxPreference.this, false);
            });

            //Check if CF numbers match the slot's phone numbers and
            //Call Waiting is enabled and
            //Call forwarding Busy is enabled then set the checkbox accordingly.
            new Handler(Looper.getMainLooper()).post(() -> {
                mXdivertStatus = validateXDivert();
                displayAlertMessage(mXdivertStatus);
                setChecked(mXdivertStatus);
                mXDivertUtility.onXDivertChanged(mXdivertStatus);
                mXDivertUtility.setXDivertStatus(mXdivertStatus);
            });
        }
    }

    private void handleSetCFNRCResponse(int slotId) {
        if (DBG) Log.d(LOG_TAG, "handleSetCFResponse: done on Slot = " + slotId);

        if (slotId == SLOT1) {
            mCFLine1Number[slotId] = mLine1Number[SLOT2];
        } else {
            mCFLine1Number[slotId] = mLine1Number[SLOT1];
        }

        //Set Call Waiting for the "slotId" slot
        mSetOptionCompleteHandler.sendMessage(mSetOptionCompleteHandler
                .obtainMessage(MESSAGE_SET_CALL_WAITING, slotId, 0));
    }

    private void handleGetCallWaitingResponse(int slotId, int result) {
        if (DBG) Log.d(LOG_TAG, "handleGetCallWaitingResponse: CW state successfully queried.");

        mSlotCallWaiting[slotId] = (result == TelephonyManager.CALL_WAITING_STATUS_ENABLED);

        if (slotId == SLOT1) {
            Log.d(LOG_TAG,"CW for Slot1 = " + mSlotCallWaiting[SLOT1]);

            // Due to limitation in lower layers, back-to-back requests to SLOT1 & SLOT2
            // cannot be sent.For ex: CF request on SLOT2 followed by CF request on SLOT1.
            // Miminum of 3000ms delay is needed to send the 2nd request.
            // Hence added postDelayed to CF request for SLOT1.
            //Query Call Forward for SLOT2
            mGetoptionCompleteHandler.sendMessageDelayed(mGetoptionCompleteHandler
                    .obtainMessage(MESSAGE_GET_CFNRC, SLOT2, 0), 1000);
        } else if (slotId == SLOT2) {
            Log.d(LOG_TAG,"CW for Slot2 = " + mSlotCallWaiting[SLOT2]);

            //validate smart call forward
            int imsSlotId = getImsSlotId();
            if (imsSlotId != INVALID_SLOT) {
                queryCallForwardingBusy(imsSlotId);
                return;
            }
            processStopDialog(STOP, true);

            //Check if CF numbers match the slot's phone numbers and
            //Call Waiting is enabled, then set the checkbox accordingly.
            new Handler(Looper.getMainLooper()).post(() -> {
                mXdivertStatus = validateXDivert();
                displayAlertMessage(mXdivertStatus);
                setChecked(mXdivertStatus);
                mXDivertUtility.onXDivertChanged(mXdivertStatus);
                mXDivertUtility.setXDivertStatus(mXdivertStatus);
            });
        }
    }

    private int getImsSlotId() {
        if (!(mXDivertUtility.isCarrierOneSupported())) {
            return INVALID_SLOT;
        }

        int imsSlotId = INVALID_SLOT;
        for (int phoneId = 0; phoneId < mNumPhones; phoneId++) {
            if (mXDivertUtility.isValidImsPhoneId(phoneId)) {
                return phoneId;
            }
        }
        return imsSlotId;
    }

    private void handleSetCallWaitingResponse(int slotId) {
        Log.d(LOG_TAG, "handleSetCallWaitingResponse success slotId = " + slotId);

        if (slotId == SLOT1) {
            mSlotCallWaiting[SLOT1] = (!mSlotCallWaiting[SLOT1]);

            //Set Call Forward for SLOT2
            // Due to limitation in lower layers, back-to-back requests to SLOT1 & SLOT2
            // cannot be sent.For ex: CF request on SLOT2 followed by CF request on SLOT1.
            // Miminum of 3000ms delay is needed to send the 2nd request.
            // Hence added postDelayed to CF request for SLOT2.
            CallForwardingInfo callForwardingInfo = new CallForwardingInfo(!mXdivertStatus
                    /* enabled*/, mReason, mLine1Number[SLOT1], TIME);

            mSetOptionCompleteHandler.sendMessageDelayed(mSetOptionCompleteHandler
                    .obtainMessage(MESSAGE_SET_CFNRC, SLOT2, 0, callForwardingInfo), 1000);
        } else if (slotId == SLOT2) {
            mSlotCallWaiting[SLOT2] = !(mSlotCallWaiting[SLOT2]);

            //validate smart call forward
            int imsSlotId = getImsSlotId();
            if (imsSlotId != INVALID_SLOT) {
                int nextSlotId = mXDivertUtility.getNextPhoneId(imsSlotId);
                CallForwardingInfo callForwardingInfo = new CallForwardingInfo(!mXdivertStatus
                        /* enabled*/, CallForwardingInfo.REASON_BUSY, mLine1Number[nextSlotId], 0);
                mSetOptionCompleteHandler.sendMessage(mSetOptionCompleteHandler
                        .obtainMessage(MESSAGE_SET_CFB, imsSlotId, 0, callForwardingInfo));
                return;
            }

            if (mTcpListener != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    mTcpListener.onFinished(XDivertCheckBoxPreference.this, false);
                });

                //After successful operation of setting CFNRc & CW,
                //set the checkbox accordingly.
                new Handler(Looper.getMainLooper()).post(() -> {
                    mXdivertStatus = validateXDivert();
                    displayAlertMessage(mXdivertStatus);
                    setChecked(mXdivertStatus);
                    mXDivertUtility.onXDivertChanged(mXdivertStatus);
                    mXDivertUtility.setXDivertStatus(mXdivertStatus);
                });
            }
        }
    }

    private void handleRevertOperation(int slot, int event) {
        Log.d(LOG_TAG,"handleRevertOperation slot = " + slot + "Event = " + event);
        new Handler(Looper.getMainLooper()).post(() -> {
            setChecked(mXdivertStatus);
        });
        if (slot == SLOT1) {
            switch (event) {
                case REVERT_SET_CFNRC:
                    mCFLine1Number[slot] = null;
                    if (mTcpListener != null) {
                       new Handler(Looper.getMainLooper()).post(() -> {
                            mTcpListener.onFinished(XDivertCheckBoxPreference.this, false);
                        });
                    }
                    break;

                case REVERT_SET_CALL_WAITING:
                    revertCFNRC(SLOT1);
                break;
            }
        } else if (slot == SLOT2) {
            switch (event) {
                case REVERT_SET_CFNRC:
                    mCFLine1Number[slot] = null;
                    if (mTcpListener != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            mTcpListener.onFinished(XDivertCheckBoxPreference.this, false);
                        });
                    }

                    Toast toast = Toast.makeText(this.getContext(),
                            R.string.xdivert_partial_set,
                            Toast.LENGTH_LONG);
                            toast.show();
                break;

                case REVERT_SET_CALL_WAITING:
                    revertCFNRC(SLOT2);
                break;
            }
        }
    }

    private void revertCFNRC(int slotId) {
        Log.d(LOG_TAG,"revertCFNRc slotId = " + slotId);

        int reason = CallForwardingInfo.REASON_NOT_REACHABLE;
        if (slotId == SLOT1) {
            CallForwardingInfo callForwardingInfo = new CallForwardingInfo(mXdivertStatus
                    /* enabled */, reason, mLine1Number[SLOT2], TIME);
            mRevertOptionCompleteHandler.sendMessage(mRevertOptionCompleteHandler
                    .obtainMessage(REVERT_SET_CFNRC, slotId, 0, callForwardingInfo));
        } else if (slotId == SLOT2) {
            CallForwardingInfo callForwardingInfo = new CallForwardingInfo(mXdivertStatus
                    /* enabled */, reason, mLine1Number[SLOT1], TIME);
            mRevertOptionCompleteHandler.sendMessage(mRevertOptionCompleteHandler
                    .obtainMessage(REVERT_SET_CFNRC, slotId, 0, callForwardingInfo));
        }
    }

    private int getErrorCode(int error) {
        int errorCode;
        if (error == TelephonyManager.CallForwardingInfoCallback
            .RESULT_ERROR_FDN_CHECK_FAILURE) {
            errorCode = TimeConsumingPreferenceActivity.FDN_CHECK_FAILURE;
        } else {
            errorCode = TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
        }
        return errorCode;
    }

    void dispose() {
        Log.d(LOG_TAG, "dispose");
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
    }
}
