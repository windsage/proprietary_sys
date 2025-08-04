/**
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd2;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.media.AudioManager;
import android.media.IAudioService;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import java.util.Arrays;

/**
 * This class handles the AT+CMUT command.
 */
public class AtCmutCmdHandler extends AtCmdBaseHandler implements AtCmdHandler {

    private static final String TAG = "AtCmutCmdHandler";
    private Context mContext;
    private final AudioManager mAudioManager;
    private final TelecomManager mTelecomManager;
    private static final int INVALID_MUTE_STATUS = -1;
    private static final int MUTE_OFF = 0;
    private static final int MUTE_ON  = 1;

    public AtCmutCmdHandler(Context context) throws AtCmdHandlerInstantiationException {
        super(context);
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }

    @Override
    public String getCommandName() {
        return "+CMUT";
    }

    private int getMuteStatusFromToken(String input) {
        int muteStatus = INVALID_MUTE_STATUS;
        try {
            muteStatus = Integer.parseInt(input);
        } catch(NumberFormatException ex) {
            Log.e(TAG, "Not an integer, input: " + input + ": " + ex);
        }
        return muteStatus;
    }

    @Override
    public AtCmdResponse handleCommand(AtCmd cmd) {
        AtCmdResponse ret = null;
        String tokens[] = cmd.getTokens();
        String resultString = null;
        boolean isAtCmdRespOk = false;
        boolean isSetCmd = false;

        Log.d(TAG, "+CMUT command processing entry"
                + ", opCode: " + cmd.getOpcode()
                + ", tokens: " + (tokens == null ? "Null" : Arrays.toString(tokens)));

        if (mAudioManager == null || mTelecomManager == null) {
            return new AtCmdResponse(AtCmdResponse.RESULT_ERROR,
                    cmd.getAtCmdErrStr(AtCmd.AT_ERR_OP_NOT_ALLOW));
        }

        /*
        1. AT+CMUT   -> same as AT+CMUT?
        2. AT+CMUT?  -> returns whether mic is muted or not
        3. AT+CMUT=  -> same as AT+CMUT=0
        4. AT+CMUT=0 -> mute off (mic is active)
        5. AT+CMUT=1 -> mute on (mis is muted)
        6. AT+CMUT=? -> reports the supported values for the command
        */

        switch (cmd.getOpcode()) {
            case AtCmd.AT_OPCODE_NA:
                // AT+CMUT
                // Fallthrough. AT+CMUT is the same as AT+CMUT?

            case AtCmd.ATCMD_OPCODE_NA_QU:
                // AT+CMUT?
                boolean isMicMuted = mAudioManager.isMicrophoneMute();
                resultString = isMicMuted ? "1" : "0";
                isAtCmdRespOk = true;
                break;

            case AtCmd.ATCMD_OPCODE_NA_EQ_QU:
                // AT+CMUT=?
                resultString = "(0,1)";
                isAtCmdRespOk = true;
                break;

            case AtCmd.ATCMD_OPCODE_NA_EQ:
                // AT+CMUT=
                // Fallthrough. AT+CMUT= is the same as AT+CMUT=0

            case AtCmd.ATCMD_OPCODE_NA_EQ_AR:
                // AT+CMUT=<level>
                isSetCmd = true;

                // Must have either zero or one token.
                if (tokens == null || tokens.length > 1) {
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_INCORRECT_PARAMS);
                    break;
                }

                // If there is no ongoing call, the mute operation should not be allowed since
                // the AT+CMUT command is specifically used to mute the mic during a call and also
                // mute operation should not be allowed when device is in Emergency call since
                // mute option is not available in Emergency callUI.
                if (!mTelecomManager.isInCall() || mTelecomManager.isInEmergencyCall()) {
                    Log.d(TAG, "Set micmute operation not allowed during Emergency call" +
                            " or when no active call.");
                    return new AtCmdResponse(AtCmdResponse.RESULT_ERROR,
                            cmd.getAtCmdErrStr(AtCmd.AT_ERR_OP_NOT_ALLOW));
                }

                int newMuteStatus = INVALID_MUTE_STATUS;

                if (tokens.length == 0) {
                    // this is AT+CMUT= command, which is the same as AT+CMUT=0
                    newMuteStatus = MUTE_OFF;
                } else {
                    // this is AT+CMUT=<n> command
                    newMuteStatus = getMuteStatusFromToken(tokens[0]);
                }

                // Range check
                if (newMuteStatus < 0 || newMuteStatus > 1) {
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_INCORRECT_PARAMS);
                    break;
                }

                boolean shouldMute = (newMuteStatus == MUTE_ON);
                Log.d(TAG, "+CMUT shouldMute: " + shouldMute);

                IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
                IAudioService service = IAudioService.Stub.asInterface(b);

                try {
                    service.setMicrophoneMute(shouldMute, mContext.getOpPackageName(),
                            getCurrentUserId(), mContext.getAttributionTag());
                } catch(RemoteException ex) {
                    Log.e(TAG, "Remote exception while setting mute. " + ex);
                }
                isAtCmdRespOk = true;
                break;
        }

        if (!isSetCmd && isAtCmdRespOk) {
            resultString = getCommandName() + ": " + resultString;
        }

        return isAtCmdRespOk
                ? new AtCmdResponse(AtCmdResponse.RESULT_OK, resultString)
                : new AtCmdResponse(AtCmdResponse.RESULT_ERROR, resultString);
    }

    private int getCurrentUserId() {
        try {
            UserInfo currentUser = ActivityManager.getService().getCurrentUser();
            return currentUser.id;
        } catch (RemoteException e) {
            // Activity manager not running, nothing we can do assume user 0.
            return UserHandle.USER_OWNER;
        }
    }
}
