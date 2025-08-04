/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class XpanReceiver extends BroadcastReceiver {

    private static final String TAG = "XpanReceiver";
    private static final boolean DEBUG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;
    private static XpanUtils mUtils = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean xpanSupport = context.getResources().getBoolean(R.bool.profile_supported_xpan);
        if (!xpanSupport) {
            if (VDBG)
                Log.v(TAG, TAG);
            return;
        }
        String action = intent.getAction();
        if (VDBG && !BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            Log.v(TAG, action);
        }

        if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            return;
        }
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR);
        if (DEBUG)
            Log.d(TAG, "state " + state);
        if (state != BluetoothAdapter.STATE_TURNING_ON) {
            return;
        }
        mUtils = XpanUtils.getInstance(context);
        String address = mUtils.getBluetoothAdapter().getAddress();
        if (TextUtils.isEmpty(address)) {
            Log.w(TAG, "Ignore");
            return;
        }
        mUtils.startService(context, new Intent(context, XpanProfileService.class));
    }
}