/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.HashMap;

import com.qualcomm.atfwd.AtCmdHandler;
import com.qualcomm.atfwd.AtCmdHandler.AtCmdHandlerInstantiationException;

public class AtCmdFwdAidl  extends vendor.qti.hardware.radio.atcmdfwd.IAtCmdFwd.Stub {
    private static final String LOG_TAG = "AtCmdFwdAidl";
    private Context mContext;
    private HashMap<String, AtCmdHandler> mCmdHandlers;

    private void addHandler(AtCmdHandler cmd) {
        mCmdHandlers.put(cmd.getCommandName().toUpperCase(), cmd);
    }

    private void initAtCmdHandlers(Context c) {
        AtCmdHandler cmd;

        mCmdHandlers = new HashMap<String, AtCmdHandler>();
        try {
            addHandler(new AtCkpdCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(LOG_TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCtsaCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(LOG_TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCfunCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(LOG_TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCrslCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(LOG_TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtCssCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(LOG_TAG, "Unable to instantiate command", e);
        }

        try {
            addHandler(new AtQcpwrdnCmdHandler(c));
        } catch (AtCmdHandlerInstantiationException e) {
            Log.e(LOG_TAG, "Unable to instantiate command", e);
        }
    }

    public AtCmdFwdAidl(Context c) {
        Log.i(LOG_TAG, "AtCmdFwdAidl");
        mContext = c;
        initAtCmdHandlers(mContext);
    }

    @Override
    public final vendor.qti.hardware.radio.atcmdfwd.AtCmdResponse processAtCmd(
            vendor.qti.hardware.radio.atcmdfwd.AtCmd cmd) throws RemoteException {
        vendor.qti.hardware.radio.atcmdfwd.AtCmdResponse ret = new
                vendor.qti.hardware.radio.atcmdfwd.AtCmdResponse();
        AtCmdHandler h = mCmdHandlers.get(cmd.name.toUpperCase());

        Log.i(LOG_TAG, "Command name " + cmd.name);

        if (h == null) {
            Log.e(LOG_TAG,"Unhandled command " + cmd);
            ret.result = vendor.qti.hardware.radio.atcmdfwd.AtCmdResult.ATCMD_RESULT_ERROR;
            ret.response = "+CME ERROR: 4";
            return ret;
        }

        try {
            com.qualcomm.atfwd.AtCmd atcmd;

            Log.d(LOG_TAG, "num of tokens " + cmd.token.num);
            if (cmd.token.num > 0) {
                String[] tokenItems = new String[cmd.token.num];
                for (int i = 0; i < cmd.token.num; i++) {
                    tokenItems[i] = cmd.token.items[i];
                }
                atcmd = new com.qualcomm.atfwd.AtCmd(cmd.opCode,
                    cmd.name, tokenItems);

            } else {
                atcmd = new com.qualcomm.atfwd.AtCmd(cmd.opCode,
                    cmd.name, null);
            }

            com.qualcomm.atfwd.AtCmdResponse resp = h.handleCommand(atcmd);
            ret.result = resp.getResult();
            ret.response = resp.getResponse();
            Log.i(LOG_TAG, "aidl result=" + ret.result);
            Log.i(LOG_TAG, "aidl response=" + ret.response);

            // aidl interface expect a non-null string, however, atfwd
            // handler may have null reponse string return. Use an empty
            // string for this case.
            if (ret.response == null) {
                ret.response = "";
            }

            return ret;
        } catch(Throwable e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            Log.e(LOG_TAG, "AtCmd Handler Exception!");
            ret.result = vendor.qti.hardware.radio.atcmdfwd.AtCmdResult.ATCMD_RESULT_ERROR;
            ret.response = "+CME ERROR: 2";
            return ret;
        }
    }

    @Override
    public final int getInterfaceVersion() {
        return vendor.qti.hardware.radio.atcmdfwd.IAtCmdFwd.VERSION;
    }

    @Override
    public String getInterfaceHash() {
        return vendor.qti.hardware.radio.atcmdfwd.IAtCmdFwd.HASH;
    }
}

