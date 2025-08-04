/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd2;

import android.content.Context;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Arrays;

/**
 * This class handles the $QCDDS command.
 */
public class AtQcddsCmdHandler extends AtCmdBaseHandler implements AtCmdHandler {

    private static final String TAG = "AtQcddsCmdHandler";
    private Context mContext;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;
    private final TelecomManager mTelecomManager;

    public AtQcddsCmdHandler(Context context) throws AtCmdHandlerInstantiationException {
        super(context);
        mContext = context;
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mTelecomManager = context.getSystemService(TelecomManager.class);
    }

    @Override
    public String getCommandName() {
        return "$QCDDS";
    }

    @Override
    public AtCmdResponse handleCommand(AtCmd cmd) {
        AtCmdResponse ret = null;
        String tokens[] = cmd.getTokens();
        String resultString = null;
        boolean isAtCmdRespOk = false;
        boolean isSetCmd = false;

        Log.d(TAG, "$QCDDS command processing entry, opCode: " + cmd.getOpcode()
                + ", tokens: " + (tokens == null ? "Null" : Arrays.toString(tokens)));

        /*
        1. $QCDDS?  -> returns the slot id for current DDS
        2. $QCDDS=x -> sets DDS to slot x
        3. $QCDDS=? -> return range of slot ids
        */

        switch (cmd.getOpcode()) {
            case AtCmd.ATCMD_OPCODE_NA_QU:
                // $QCDDS?
                int slotIndex = SubscriptionManager.getSlotIndex(
                        SubscriptionManager.getDefaultDataSubscriptionId());
                if (slotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_NOT_FOUND);
                    break;
                }
                resultString = String.valueOf(slotIndex);
                isAtCmdRespOk = true;
                break;

            case AtCmd.ATCMD_OPCODE_NA_EQ_QU:
                // $QCDDS=?
                resultString = "(0,1)";
                isAtCmdRespOk = true;
                break;

            case AtCmd.ATCMD_OPCODE_NA_EQ_AR:
                // $QCDDS=<SLOT_ID>
                Log.d(TAG, "Handle $QCDDS=<SLOT_ID> tokens[0]=" + tokens[0]);
                isSetCmd = true;

                // Must have one token.
                if (tokens == null || tokens.length != 1 || !isTokenValid(tokens[0])) {
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_INCORRECT_PARAMS);
                    break;
                }

                int subId = getSubIdFromToken(tokens[0]);

                if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    Log.d(TAG, "Invalid sub id: " + subId);
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_INCORRECT_PARAMS);
                    break;
                }

                // DDS switch operation is not allowed in below scenarios
                // 1. During emergency call or active call.
                // 2. Smart dds feature is enabled
                // 3. Phone is in ECBM or SCBM mode.
                boolean isEcbmEnabled = mTelephonyManager.getEmergencyCallbackMode();
                boolean isScbmEnabled = TelephonyProperties.in_scbm().orElse(false);
                boolean isSmartDdsEnabled = isSmartDdsEnabled();
                boolean isInCall = mTelecomManager.isInCall();
                boolean isInEmergencyCall = mTelecomManager.isInEmergencyCall();
                boolean isAirplaneModeOn = isAirplaneModeOn();

                if (isInCall || isInEmergencyCall || isSmartDdsEnabled || isEcbmEnabled
                        || isScbmEnabled || isAirplaneModeOn) {
                    Log.d(TAG, "DDS switch operation is not allowed. isInCall: " + isInCall
                            + " isInEmergencyCall: " + isInEmergencyCall + " isSmartDdsEnabled: "
                            + isSmartDdsEnabled + " isEcbmEnabled: " + isEcbmEnabled
                            + " isScbmEnabled: " + isScbmEnabled + " isAirplaneModeOn: "
                            + isAirplaneModeOn);
                    return new AtCmdResponse(AtCmdResponse.RESULT_ERROR,
                            cmd.getAtCmdErrStr(AtCmd.AT_ERR_OP_NOT_ALLOW));
                }

                try {
                    Log.d(TAG, "Set DDS to subId: " + subId);
                    mSubscriptionManager.setDefaultDataSubId(subId);
                } catch (Exception ex) {
                    Log.e(TAG, "Remote exception:" + ex);
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_UNKNOWN);
                    break;
                }
                isAtCmdRespOk = true;
                break;
        }

        if (!isSetCmd && isAtCmdRespOk) {
            resultString = getCommandName() + ": " + resultString;
        }

        Log.d(TAG, "resultString = " + resultString);

        return isAtCmdRespOk
                ? new AtCmdResponse(AtCmdResponse.RESULT_OK, resultString)
                : new AtCmdResponse(AtCmdResponse.RESULT_ERROR, resultString);
    }

    private int getSubIdFromToken(String input) {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        try {
            subId = mSubscriptionManager.getSubscriptionId(Integer.parseInt(input));
        } catch(NumberFormatException ex) {
            Log.e(TAG, "Not an integer, input: " + input + ": " + ex);
        }
        return subId;
    }

    private boolean isSmartDdsEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SMART_DDS_SWITCH, 0) == 1;
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private boolean isTokenValid(String token) {
        int slotIndex = -1;
        try {
            slotIndex = Integer.parseInt(token);
        } catch(NumberFormatException ex) {
            Log.e(TAG, "Not an integer, token: " + token + ": " + ex);
        }
      return (slotIndex >= 0 && slotIndex < mTelephonyManager.getActiveModemCount());
    }
}
