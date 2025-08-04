/**
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd2;

import android.content.Context;
import android.media.AudioManager;
import android.telecom.TelecomManager;
import android.util.Log;

import java.lang.StringBuilder;
import java.util.Arrays;

/**
 * This class handles the AT+CLVL command.
 */
public class AtClvlCmdHandler extends AtCmdBaseHandler implements AtCmdHandler {

    private static final String TAG = "AtClvlCmdHandler";
    private static final int INVALID_VOLUME = -1;

    private Context mContext;
    private final AudioManager mAudioManager;
    private final TelecomManager mTelecomManager;
    private final int mMaxMediaVolume;
    private final int mMaxCallVolume;

    public AtClvlCmdHandler(Context context) throws AtCmdHandlerInstantiationException {
        super(context);
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        mMaxMediaVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mMaxCallVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public String getCommandName() {
        return "+CLVL";
    }

    private String getFormattedVolumeRange() {
        // Range: (1-max)
        return new StringBuilder()
                .append("(1-")
                .append(getMaxVolume())
                .append(")")
                .toString();
    }

    private boolean isInCall() {
        return mTelecomManager.isInCall();
    }

    private int getAudioStream() {
        return isInCall()
                ? AudioManager.STREAM_VOICE_CALL
                : AudioManager.STREAM_MUSIC;
    }

    private int getMaxVolume() {
        return isInCall()
                ? mMaxCallVolume
                : mMaxMediaVolume;
    }

    private int getVolumeFromToken(String input) {
        int volumeFromToken = INVALID_VOLUME;
        try {
            volumeFromToken = Integer.parseInt(input);
        } catch(NumberFormatException ex) {
            Log.e(TAG, "Not an integer, input: " + input + ": " + ex);
        }
        return volumeFromToken;
    }

    @Override
    public AtCmdResponse handleCommand(AtCmd cmd) {
        AtCmdResponse ret = null;
        String tokens[] = cmd.getTokens();
        String resultString = null;
        boolean isAtCmdRespOk = false;
        boolean isSetCmd = false;

        Log.d(TAG, "+CLVL command processing entry"
                + ", opCode: " + cmd.getOpcode()
                + ", tokens: " + (tokens == null ? "Null" : Arrays.toString(tokens)));

        if (mAudioManager == null) {
            return new AtCmdResponse(AtCmdResponse.RESULT_ERROR,
                    cmd.getAtCmdErrStr(AtCmd.AT_ERR_OP_NOT_ALLOW));
        }

        /*
        1. AT+CLVL   -> same as AT+CLVL?
        2. AT+CLVL?  -> reports the current value of level
        3. AT+CLVL=? -> reports level range +CLVL: (0-max)
        4. AT+CLVL=x -> sets level to x
        */

        switch (cmd.getOpcode()) {
            case AtCmd.AT_OPCODE_NA:
                // AT+CLVL
                // Fallthrough. AT+CLVL is the same as AT+CLVL?

            case AtCmd.ATCMD_OPCODE_NA_QU:
                // AT+CLVL?
                int volume = mAudioManager.getStreamVolume(getAudioStream());
                resultString = Integer.toString(volume);
                isAtCmdRespOk = true;
                break;

            case AtCmd.ATCMD_OPCODE_NA_EQ_QU:
                // AT+CLVL=?
                resultString = getFormattedVolumeRange();
                isAtCmdRespOk = true;
                break;

            case AtCmd.ATCMD_OPCODE_NA_EQ_AR:
                // AT+CLVL=<level>
                isSetCmd = true;

                // Must have exactly one token.
                if (tokens == null || tokens.length != 1) {
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_INCORRECT_PARAMS);
                    break;
                }

                int newVolume = getVolumeFromToken(tokens[0]);
                int maxVolume = getMaxVolume();

                // Range check
                if (newVolume < 1 || newVolume > maxVolume) {
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_INCORRECT_PARAMS);
                    break;
                }

                try {
                    int audioStream = getAudioStream();
                    Log.d(TAG, "setStreamVolume, audioStream: " + audioStream
                            + ", volume: " + newVolume);
                    mAudioManager.setStreamVolume(audioStream,
                            newVolume,
                            AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
                } catch (SecurityException e) {
                    // this can happen if this request ends up changing the Do Not Disturb mode
                    resultString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_OP_NOT_ALLOW);
                    break;
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
}
