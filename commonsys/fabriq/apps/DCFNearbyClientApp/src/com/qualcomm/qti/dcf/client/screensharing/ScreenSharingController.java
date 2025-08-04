/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client.screensharing;

import static com.qualcomm.qti.dcf.client.DCFDataElementKt.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.qualcomm.qti.dcf.client.DCFAdvertiseManager;
import com.qualcomm.qti.dcf.client.DCFDataElement;
import com.qualcomm.qti.dcf.client.DCFDevice;
import com.qualcomm.qti.dcf.client.DCFDevicesListener;
import com.qualcomm.qti.dcf.client.DCFScanManager;
import com.qualcomm.qti.dcf.client.UserSettings;
import com.qualcomm.qti.dcf.client.service.screensharing.IActionListener;
import com.qualcomm.qti.dcf.client.service.screensharing.IDeviceInfoListener;
import com.qualcomm.qti.dcf.client.service.screensharing.IScreenSharingService;
import com.qualcomm.qti.dcf.client.service.screensharing.IWfdStatusListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ScreenSharingController implements UserSettings.OnSettingsChangedListener,
        DCFDevicesListener, UserSettings.OnEnvironmentReadyListener {

    public @interface WfdState {
        int UNKNOWN = 0;
        int IDLE = 1;
        int BUSY = 2;
    }

    public @interface WfdType {
        int SOURCE = 0;
        int SINK = 1;
    }

    //WIFI P2P errors are 0 or positive values, to diff, we define our errors as negative values
    public @interface Error {
        int NO_SCREEN_SHARING_SERVICE = -1;
        int BINDER_CALL_FAILED = -2;
        int P2P_DISCOVERY_TIMEOUT = -3;
        int P2P_CONNECTING_TIMEOUT = -4;
        int NO_WFD_SERVICE = -5;
        int CREATE_SESSION_FAILED = -6;
        int START_SESSION_FAILED = -7;
        int START_SESSION_TIMEOUT = -8;
    }

    private static final String TAG = "ScreenSharingController";
    private static final String SERVICE_PACKAGE = "com.qualcomm.qti.dcf.client.service";
    private static final String SERVICE_ACTION =
            "com.qualcomm.qti.dcf.action.SCREEN_SHARING_SERVICE";
    private static final String ANONYMIZED_DEVICE_ADDRESS = "02:00:00:00:00:00";
    private static final int WIFI_ADDR_TYPE = 0x03;
    private static final int WIFI_DISPLAY_DE_SIZE = 9;
    private static final int MAC_ADDRESS_BYTES_SIZE = 6;
    private static final int WFD_STATE_INDEX = 0;
    private static final int WFD_TYPE_INDEX = 1;
    private static final int DEVICE_ADDRESS_START_INDEX = 3;

    private static ScreenSharingController sController = new ScreenSharingController();

    private Context mContext;
    private int mWfdType = WfdType.SOURCE;
    // TODO: obtain and listen the real wfd state from screen sharing service
    private int mWfdState = WfdState.UNKNOWN;
    // TODO: obtain the real p2p address from screen sharing service
    private String mP2pAddress = ANONYMIZED_DEVICE_ADDRESS;
    private boolean mScreenSharingEnabled = false;
    private List<DCFDataElement> mDataElementList = new ArrayList<>();
    private List<ScreenSharingDevicesListener> mScreenSharingDevicesListeners = new ArrayList<>();
    private List<WfdStatusListener> mWfdStatusListeners = new ArrayList<>();

    private IScreenSharingService mScreenSharingService;
    private final Handler mHandler = new Handler();

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: screen sharing service");
            mScreenSharingService = IScreenSharingService.Stub.asInterface(service);
            syncWfdTypeToService();
            registerWfdStatusListener();
            requestOwnDeviceAddress();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: screen sharing service");
            mScreenSharingService = null;
        }
    };

    private IWfdStatusListener mWfdStatusListener = new IWfdStatusListener.Stub() {
        @Override
        public void onWfdStateChanged(int state) {
            Log.d(TAG, "onWfdStateChanged state = " + state);
            handleNotifyStateChanged(state);
        }

        @Override
        public void onWfdDeviceTypeChanged(int wfdDeviceType) {
            handleNotifyDeviceTypeChanged(wfdDeviceType);
        }
    };

    @Override
    public void onEnvironmentReady() {
        enableScreenSharingCapability(UserSettings.INSTANCE.getWfdEnable());
        listenUserSettingsChanged();
    }

    public static ScreenSharingController getInstance() {
        return sController;
    }

    private ScreenSharingController() {}

    public void init(Context appContext) {
        mContext = appContext;
        mWfdType = UserSettings.INSTANCE.getWfdType();
        mDataElementList.add(createWifiDisplayDataElement());
        UserSettings.INSTANCE.addOnEnvironmentReadyListener(this);
    }

    public boolean isScreenSharingEnabled() {
        return mScreenSharingEnabled;
    }

    public int getWfdType() {
        return mWfdType;
    }

    public int getWfdState() {
        return mWfdState;
    }

    public String getConnectedDeviceAddress() {
        if (mScreenSharingService != null) {
            try {
                return mScreenSharingService.getConnectedDeviceAddress();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public void startScreenSharing(String peerAddress, ScreenSharingActionListener listener) {
        if (mScreenSharingService != null) {
            try {
                mScreenSharingService.startScreenSharing(peerAddress,
                        new IActionListener.Stub() {
                            @Override
                            public void onSuccess() {
                                handleNotifyClientOperationSuccess(listener);
                            }

                            @Override
                            public void onFailure(int reasonCode) {
                                handleNotifyClientOperationFailed(listener, reasonCode);
                            }
                        });
            } catch (RemoteException e) {
                e.printStackTrace();
                handleNotifyClientOperationFailed(listener, Error.BINDER_CALL_FAILED);
            }
        } else {
            handleNotifyClientOperationFailed(listener, Error.NO_SCREEN_SHARING_SERVICE);
        }
    }

    public void stopScreenSharing(ScreenSharingActionListener listener) {
        if (mScreenSharingService != null) {
            try {
                mScreenSharingService.stopScreenSharing(new IActionListener.Stub() {
                    @Override
                    public void onSuccess() {
                        handleNotifyClientOperationSuccess(listener);
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        handleNotifyClientOperationFailed(listener, reasonCode);
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
                handleNotifyClientOperationFailed(listener, Error.BINDER_CALL_FAILED);
            }
        } else {
            handleNotifyClientOperationFailed(listener, Error.NO_SCREEN_SHARING_SERVICE);
        }
    }

    @Override
    public void onSettingsChanged(SharedPreferences sharedPreferences, String key) {
        if (UserSettings.KEY_WFD_ENABLE.equals(key)) {
            enableScreenSharingCapability(UserSettings.INSTANCE.getWfdEnable());
        } else if (UserSettings.KEY_WFD_TYPE.equals(key)) {
            onWfdTypeChanged(UserSettings.INSTANCE.getWfdType());
        }
    }

    public void registerScreenSharingDevicesListener(ScreenSharingDevicesListener listener) {
        boolean isNeedListenDevicesChange = mScreenSharingDevicesListeners.isEmpty();
        mScreenSharingDevicesListeners.add(listener);
        if (isNeedListenDevicesChange) {
            DCFScanManager.getInstance().registerDCFDevicesListener(this);
        }
    }

    public void unregisterScreenSharingDevicesListener(ScreenSharingDevicesListener listener) {
        mScreenSharingDevicesListeners.remove(listener);
        if (mScreenSharingDevicesListeners.isEmpty()) {
            DCFScanManager.getInstance().unregisterDCFDevicesListener(this);
        }
    }

    public void registerWfdStatusListener(WfdStatusListener listener) {
        if (!mWfdStatusListeners.contains(listener)) {
            mWfdStatusListeners.add(listener);
            listener.onWfdStateChanged(mWfdState);
        }
    }

    public void unregisterWfdStatusListener(WfdStatusListener listener) {
        mWfdStatusListeners.remove(listener);
    }

    @Override
    public void onDevicesChanged(List<DCFDevice> devices) {
        Map<String, ScreenSharingProperty> devicesProperty = new HashMap<>();
        for (DCFDevice device : devices) {
            String deviceId = parseDeviceId(device);
            ScreenSharingProperty property = parseScreenSharingProperty(device);
            if (!TextUtils.isEmpty(deviceId)) {
                StringBuilder msg = new StringBuilder()
                        .append("onDevicesChanged: deviceId=")
                        .append(deviceId);
                if (property == null) {
                    msg.append(" property is null");
                } else {
                    msg.append(" type=")
                            .append(property.getWfdType())
                            .append(" state=")
                            .append(property.getWfdState());
                }
                Log.i(TAG, msg.toString());
            }
            if (!TextUtils.isEmpty(deviceId) && property != null) {
                devicesProperty.put(deviceId, property);
            }
        }
        for (ScreenSharingDevicesListener listener : mScreenSharingDevicesListeners) {
            listener.onDevicesPropertyChanged(devicesProperty);
        }
    }

    private void onWfdTypeChanged(int wfdType) {
        mWfdType = wfdType;
        updateWfdTypeInDataElement();
        syncWfdTypeToService();
    }

    private void updateWfdTypeInDataElement() {
        Log.d(TAG, "updateWfdTypeInDataElement mWfdType = " + mWfdType);
        DCFDataElement wifiDisplay = mDataElementList.get(0);
        wifiDisplay.getData()[WFD_TYPE_INDEX] = (byte) (mWfdType & 0xFF);
        if (mScreenSharingEnabled) {
            advertiseScreenSharingCapability();
        }
    }

    private void syncWfdTypeToService() {
        if (mScreenSharingService != null) {
            try {
                if (mWfdState == WfdState.BUSY) {
                    mScreenSharingService.stopScreenSharing(new IActionListener.Stub() {
                        @Override
                        public void onSuccess() throws RemoteException {
                            Log.i(TAG, "onSuccess: stopScreenSharing");
                            mScreenSharingService.setWfdDeviceType(mWfdType);
                        }

                        @Override
                        public void onFailure(int reasonCode) throws RemoteException {
                            Log.i(TAG, "onFailure: stopScreenSharing");
                            mScreenSharingService.setWfdDeviceType(mWfdType);
                        }
                    });
                } else {
                    mScreenSharingService.setWfdDeviceType(mWfdType);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void enableScreenSharingCapability(boolean enable) {
        //screen sharing capability no changed, return directly.
        if (mScreenSharingEnabled == enable) return;

        mScreenSharingEnabled = enable;
        Log.i(TAG, "enableScreenSharingCapability: mScreenSharingEnabled=" + mScreenSharingEnabled);
        if (mScreenSharingEnabled) {
            advertiseScreenSharingCapability();
            bindScreenSharingService();
        } else {
            stopAdvertiseScreenSharingCapability();
            unbindScreenSharingService();
            handleNotifyStateChanged(WfdState.UNKNOWN);
        }
    }

    private void bindScreenSharingService() {
        Intent screenSharingServiceIntent = new Intent(SERVICE_ACTION);
        screenSharingServiceIntent.setPackage(SERVICE_PACKAGE);
        if (!mContext.bindService(screenSharingServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "bindScreenSharingService: bind fail!!!");
        }
    }

    private void unbindScreenSharingService() {
        if (mScreenSharingService != null) {
            try {
                if (mWfdState == WfdState.BUSY) {
                    mScreenSharingService.stopScreenSharing(null);
                }
                mScreenSharingService.unregisterWfdStatusListener(mWfdStatusListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mContext.unbindService(mServiceConnection);
            mScreenSharingService = null;
        }
    }

    private void registerWfdStatusListener() {
        try {
            mScreenSharingService.registerWfdStatusListener(mWfdStatusListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateWfdStateInDataElement() {
        DCFDataElement wifiDisplay = mDataElementList.get(0);
        wifiDisplay.getData()[WFD_STATE_INDEX] = (byte) (mWfdState & 0xFF);
        if (mScreenSharingEnabled) {
            advertiseScreenSharingCapability();
        }
    }

    private void updateDeviceAddressInDataElement() {
        DCFDataElement wifiDisplay = mDataElementList.get(0);
        String[] p2pAddressArray = mP2pAddress.split(":");
        for (int i = 0; i < p2pAddressArray.length; i++) {
            wifiDisplay.getData()[i + DEVICE_ADDRESS_START_INDEX] =
                    (byte)(Integer.parseInt(p2pAddressArray[i], 16) & 0xFF);
        }
        if (mScreenSharingEnabled) {
            advertiseScreenSharingCapability();
        }
    }

    private void requestOwnDeviceAddress() {
        try {
            mScreenSharingService.requestDeviceInfo(new IDeviceInfoListener.Stub() {
                @Override
                public void onDeviceAddressAvailable(String ownDeviceAddress) {
                    handleNotifyDeviceAddressChanged(ownDeviceAddress);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void handleNotifyDeviceAddressChanged(String ownDeviceAddress) {
        mHandler.post(() -> {
            if (!TextUtils.isEmpty(ownDeviceAddress) && !ownDeviceAddress.equals(mP2pAddress)) {
                mP2pAddress = ownDeviceAddress;
                updateDeviceAddressInDataElement();
            }
        });
    }

    private void handleNotifyStateChanged(int wfdState) {
        mHandler.post(() -> {
            if (mWfdState == wfdState) {
                return;
            }
            mWfdState = wfdState;
            updateWfdStateInDataElement();
            for (WfdStatusListener listener : mWfdStatusListeners) {
                listener.onWfdStateChanged(mWfdState);
            }
        });
    }

    private void handleNotifyDeviceTypeChanged(int wfdDeviceType) {
        mHandler.post(() -> {
            if (mWfdType != wfdDeviceType) {
                Log.w(TAG, "the service device type is different from user selected, " +
                        "please check whether another apps are using");
            }
        });
    }

    private void advertiseScreenSharingCapability() {
        DCFAdvertiseManager.getInstance().addDataElements(mDataElementList);
    }

    private void stopAdvertiseScreenSharingCapability() {
        DCFAdvertiseManager.getInstance().removeDataElements(mDataElementList);
    }

    private void listenUserSettingsChanged() {
        UserSettings.INSTANCE.addOnSettingsChangedListener(this);
    }

    private DCFDataElement createWifiDisplayDataElement() {
        Log.d(TAG, "create WifiDisplay DataElement wfdState = " + mWfdState
                + " wfdType = " + mWfdType + " p2pAddress = " + mP2pAddress);
        byte wfdStateByte = (byte) (mWfdState & 0xFF);
        byte wfdTypeByte = (byte) (mWfdType & 0xFF);
        byte addressTypeByte = (byte) (WIFI_ADDR_TYPE & 0xFF);

        byte[] addressBytes = new byte[MAC_ADDRESS_BYTES_SIZE];
        if (!TextUtils.isEmpty(mP2pAddress)) {
            String[] p2pAddressArray = mP2pAddress.split(":");
            for (int i = 0; i < p2pAddressArray.length; i++) {
                addressBytes[i] = (byte)(Integer.parseInt(p2pAddressArray[i], 16) & 0xFF);
            }
        }

        ByteBuffer wifiDisplay = ByteBuffer.allocate(WIFI_DISPLAY_DE_SIZE);
        wifiDisplay.put(wfdStateByte);
        wifiDisplay.put(wfdTypeByte);
        wifiDisplay.put(addressTypeByte);
        wifiDisplay.put(addressBytes);
        return new DCFDataElement(DE_TAG_WIFI_DISPLAY, wifiDisplay.array());
    }

    private String parseDeviceId(DCFDevice device) {
        for (DCFDataElement de : device.getDataElements()) {
            if (de.getTag() == DE_TAG_DEVICE_ID) {
                byte[] data = de.getData();
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < MAC_ADDRESS_BYTES_SIZE; i++) {
                    builder.append(String.format("%02x", data[i]));
                }
                return builder.toString();
            }
        }
        return null;
    }

    private ScreenSharingProperty parseScreenSharingProperty(DCFDevice device) {
        for (DCFDataElement de : device.getDataElements()) {
            if (de.getTag() == DE_TAG_WIFI_DISPLAY) {
                byte[] data = de.getData();
                int wfdState = data[WFD_STATE_INDEX] & 0xFF;
                int wfdType= data[WFD_TYPE_INDEX] & 0xFF;
                StringBuilder builder = new StringBuilder();
                for (int i = DEVICE_ADDRESS_START_INDEX; i < WIFI_DISPLAY_DE_SIZE; i++) {
                    builder.append(String.format("%02x", data[i]));
                    if (i < WIFI_DISPLAY_DE_SIZE - 1) {
                        builder.append(":");
                    }
                }
                return new ScreenSharingProperty(wfdState, wfdType, builder.toString());
            }
        }
        return null;
    }

    private void handleNotifyClientOperationSuccess(ScreenSharingActionListener listener) {
        mHandler.post(() -> {
            listener.onSuccess();
        });
    }

    private void handleNotifyClientOperationFailed(ScreenSharingActionListener listener,
                                                   int reasonCode) {
        mHandler.post(() -> {
            listener.onFailure(reasonCode);
        });
    }

    public interface ScreenSharingDevicesListener {
        void onDevicesPropertyChanged(Map<String, ScreenSharingProperty> devicesProperty);
    }

    public interface WfdStatusListener {
        void onWfdStateChanged(int state);
    }

    public interface ScreenSharingActionListener {
        void onSuccess();
        void onFailure(int reasonCode);
    }
}
