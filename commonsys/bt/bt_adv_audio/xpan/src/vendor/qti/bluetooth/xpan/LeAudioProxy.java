/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothLeAudioCodecConfig;
import android.bluetooth.BluetoothLeAudioCodecStatus;

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import android.util.Log;

public class LeAudioProxy {

    private static final String TAG = "XpanLeAudio";
    private static boolean VDBG = XpanUtils.VDBG;
    private static boolean DBG = XpanUtils.DBG;

    private static BluetoothLeAudio mLeAudio = null;
    private Context mCtx;
    private BluetoothAdapter mBtAdapter;
    static final int CODEC_TYPE_APTX_ADAPTIVE_R4 = 2;
    static final int CODEC_TYPE_DEFAULT = 3;
    private LeProxyListener mProxyListener;
    private BluetoothDevice mActiveDevice = null;
    private final int LE_AUDIO_GROUP_ID_INVALID = -1;
    private Handler mHandler;
    private HashMap<Integer, BluetoothDevice> mGroupMap = new HashMap<Integer, BluetoothDevice>();
    private boolean mCbRegistered = false;

    private final class LeServiceListener implements BluetoothProfile.ServiceListener {

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (VDBG) {
                Log.v(TAG, "onServiceConnected");
            }
            mLeAudio = (BluetoothLeAudio) proxy;
            if (XpanProfileService.getService() == null) {
                closeProfileProxy();
                unregisterCallback();
            } else if (mLeServiceListener != null) {
                registerCallback();
                mProxyListener.onServiceStateUpdated(XpanConstants.TRUE);
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (VDBG)
                Log.v(TAG, "onServiceDisconnected");
            mProxyListener.onServiceStateUpdated(XpanConstants.FALSE);
            if (mCbRegistered) {
                unregisterCallback();
            }
            mLeAudio = null;
        }
    }

    private final class LeAudioCallBack implements BluetoothLeAudio.Callback {

        @Override
        public void onCodecConfigChanged(int groupId, BluetoothLeAudioCodecStatus status) {
            if (DBG)
                Log.d(TAG, "onCodecConfigChanged " + groupId + " status " + status);
        }

        @Override
        public void onGroupStatusChanged(int groupId, int groupStatus) {
            if (VDBG)
                Log.d(TAG, "onGroupStatusChanged groupId " + groupId + " groupStatus "
                        + groupStatus);
        }

        @Override
        public void onGroupNodeAdded(BluetoothDevice device, int groupId) {
            if (VDBG)
                Log.d(TAG, "onGroupNodeAdded "+ device + "groupId " + groupId);
            mGroupMap.put(groupId, device);
        }

        @Override
        public void onGroupNodeRemoved(BluetoothDevice device, int groupId) {
            if (VDBG)
                Log.d(TAG, "onGroupNodeRemoved "+ device + "groupId: " + groupId);
            mGroupMap.remove(groupId,device);
        }

        public void onGroupStreamStatusChanged(int groupId, int status) {
            BluetoothDevice device = mGroupMap.get(groupId);
            if(device == null) {
                Log.w(TAG, "onGroupStreamStatusChanged " + groupId + " not found");
                return;
            }
            if (VDBG)
                Log.v(TAG, "onGroupStreamStatusChanged " + device + " " + status + " " + groupId);
            Message msg =
                    mHandler.obtainMessage(XpanProfileService.MSG_STREAM_STATE_CHANGED);
            msg.arg1 = status;
            msg.obj = device;
            mHandler.sendMessage(msg);
        }
    }

    private LeAudioCallBack mLeAudioCallBack = new LeAudioCallBack();
    private LeServiceListener mLeServiceListener = new LeServiceListener();

    LeAudioProxy(Context ctx, LeProxyListener listener, Handler handler) {
        if (VDBG)
            Log.v(TAG, TAG);
        mCtx = ctx;
        mBtAdapter = XpanUtils.getInstance(mCtx).getBluetoothAdapter();
        mProxyListener = listener;
        mHandler = handler;
        getProfileProxy();
    }

    void close() {
        if (DBG)
            Log.d(TAG, "close");
        closeProfileProxy();
        unregisterCallback();
        mLeAudio = null;
    }

    /* Creates new Input and output codec config updates type (Default/R4) only */
    boolean setCodecConfigPreference(BluetoothDevice device, int codecType) {
        boolean isSet = false;
        if (DBG)
            Log.d(TAG, "setCodecConfigPreference " + device + " codecType " + codecType);
        if (mLeAudio == null || device == null) {
            Log.w(TAG, "setCodecConfigPreference mLeAudio is null");
            return isSet;
        }
        int groupId = mLeAudio.getGroupId(device);
        if (groupId == LE_AUDIO_GROUP_ID_INVALID) {
            Log.w(TAG, "setCodecConfigPreference invalid groupid");
            return isSet;
        }
        BluetoothLeAudioCodecConfig updatedCodecConfig = new BluetoothLeAudioCodecConfig.Builder()
                .setCodecType(codecType).build();
        mLeAudio.setCodecConfigPreference(groupId, updatedCodecConfig, updatedCodecConfig);
        isSet = true;
        return isSet;
    }

    /**
     * set LE Audio active device.
     *
     * @param device Remote Bluetooth Device
     */
    void setActiveDevice(BluetoothDevice device) {
        mActiveDevice = device;
    }

    /**
     * Get LE Audio active device.
     *
     * @param device Remote Bluetooth Device
     * @return Current Active Bluetooth device
     */
    BluetoothDevice getActiveDevice() {
        return mActiveDevice;
    }

    BluetoothDevice getConnectedDevices() {
        if (mLeAudio == null) {
            Log.w(TAG, "getConnectedDevices " + ((mLeAudio == null) ? "mLeAudion null "
                    : "getConnectedDevices device size 0"));
            return null;
        }
        List<BluetoothDevice> deviceList = mLeAudio.getConnectedDevices();
        if (DBG)
            Log.d(TAG, "getConnectedDevices " + deviceList);
        BluetoothDevice device = null;
        if (deviceList != null && deviceList.size() > 0) {
            device = deviceList.get(0);
        }
        if (DBG)
            Log.d(TAG, "getConnectedDevices " + device);
        return device;
    }

    boolean isConnected(BluetoothDevice device) {
        if (mLeAudio == null) {
            Log.w(TAG, "isLeAudioConnected mLeAudio null");
            return false;
        }
        return mLeAudio.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED;
    }

    boolean isActiveDevice(BluetoothDevice device) {
        boolean active = false;
        if (mActiveDevice != null && mActiveDevice.equals(device)) {
            active = true;
        }
        return active;
    }

    /*
     * Returns true, if dut supports APTX ADAPTIVE R4 Codec
     */
    boolean isR4CodecSupport(BluetoothDevice device) {
        boolean codecSupport = false;
        if (mLeAudio == null || device == null) {
            Log.w(TAG, "isR4CodecSupport " + mLeAudio + " device " + device);
            return codecSupport;
        }
        int groupId = mLeAudio.getGroupId(device);
        if (groupId == LE_AUDIO_GROUP_ID_INVALID) {
            Log.w(TAG, "isR4CodecSupport LE_AUDIO_GROUP_ID_INVALID");
            return codecSupport;
        }
        BluetoothLeAudioCodecStatus codecStatus = mLeAudio.getCodecStatus(groupId);
        if (codecStatus == null) {
            Log.w(TAG, "isR4CodecSupport getCodecStatus null");
            return codecSupport;
        }
        List<BluetoothLeAudioCodecConfig> listopCapas = codecStatus
                .getOutputCodecSelectableCapabilities();

        for (BluetoothLeAudioCodecConfig config : listopCapas) {
            if (config.getCodecType() == CODEC_TYPE_APTX_ADAPTIVE_R4) {
                codecSupport = true;
                break;
            }
        }
        if (!codecSupport && DBG) {
            for (BluetoothLeAudioCodecConfig config : listopCapas) {
                Log.w(TAG, "isR4CodecSupport " + config);
            }
        }
        return codecSupport;
    }

    interface LeProxyListener {
        void onServiceStateUpdated(int state);
    }

    void setGroupId(BluetoothDevice device) {
        if (mLeAudio != null && device != null) {
            int groupId = mLeAudio.getGroupId(device);
            if (groupId == -1) {
                Log.w(TAG, "setGroupId groupid not found " + device);
            }
            mGroupMap.put(groupId, device);
            if (DBG)
                Log.d(TAG, "setGroupId " + mGroupMap);
        } else {
            Log.w(TAG, "setGroupId " + mLeAudio + " " + device + " " + mGroupMap);
        }
    }

    private void getProfileProxy() {
        if (mLeAudio != null) {
            Log.w(TAG, "getProfileProxy ignore ");
            return;
        }
        if (VDBG) {
            Log.v(TAG, "getProfileProxy");
        }
        boolean isSuccess = mBtAdapter.getProfileProxy(mCtx, mLeServiceListener,
                BluetoothProfile.LE_AUDIO);
        if (!isSuccess) {
            Log.w(TAG, "getProfileProxy ignored");
        }

    }

    private void closeProfileProxy() {
        if (mLeAudio == null) {
            Log.w(TAG, "closeProfileProxy ignore");
            return;
        }
        if (VDBG) {
            Log.v(TAG, "closeProfileProxy");
        }
        mBtAdapter.closeProfileProxy(BluetoothProfile.LE_AUDIO, mLeAudio);
    }

    private void registerCallback() {
        if (mLeAudio == null) {
            Log.w(TAG, "registerCallback ignore mLeAudio null");
            return;
        }
        try {
            if (VDBG) {
                Log.v(TAG, "registerCallback");
            }
            mLeAudio.registerCallback(Executors.newSingleThreadExecutor(), mLeAudioCallBack);
        } catch (NullPointerException | IllegalArgumentException e) {
            if (DBG) {
                Log.d(TAG, "registerCallback " + e.toString());
            }
        }
        mCbRegistered = true;
    }

    private void unregisterCallback() {
        if (mLeAudio == null) {
            Log.w(TAG, "unregisterCallback ignore");
            return;
        }
        try {
            if (VDBG) {
                Log.v(TAG, "unregisterCallback");
            }
            mLeAudio.unregisterCallback(mLeAudioCallBack);
        } catch (NullPointerException | IllegalArgumentException e) {
            if (DBG) {
                Log.d(TAG, "unregisterCallback " + e.toString());
            }
        }
        mCbRegistered = false;
    }
}
