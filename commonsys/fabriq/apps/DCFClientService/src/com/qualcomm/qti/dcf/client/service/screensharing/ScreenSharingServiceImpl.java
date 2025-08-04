/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client.service.screensharing;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.InputEvent;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.qualcomm.qti.dcf.client.service.R;
import com.qualcomm.qti.dcf.client.service.screensharing.p2p.WifiP2pManagerHelper;
import com.qualcomm.wfd.WfdDevice;
import com.qualcomm.wfd.WfdEnums;
import com.qualcomm.wfd.WfdStatus;
import com.qualcomm.wfd.service.ISessionManagerService;
import com.qualcomm.wfd.service.IWfdActionListener;
import com.qualcomm.wfd.service.IWfdSession;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScreenSharingServiceImpl extends IScreenSharingService.Stub
        implements WfdSurfaceView.Callback, WfdSurfaceView.EventListener {

    private static final String TAG = "ScreenSharingService";
    private static final String QESDK_DCF_CLIENT_PACKAGE_NAME = "com.qualcomm.qti.dcf.qesdkclient";
    private static final String SERVICE_PACKAGE = "com.qualcomm.wfd.service";
    private static final String SERVICE_ACTION = "com.qualcomm.wfd.service.WfdService";
    private static final String LOCAL_ADDRESS_PERMISSION =
            android.Manifest.permission.LOCAL_MAC_ADDRESS;
    private static final String CONFIGURE_WIFI_DISPLAY_PERMISSION =
            android.Manifest.permission.CONFIGURE_WIFI_DISPLAY;
    private static final String QC_WFD_PERMISSION = "com.qualcomm.permission.wfd.QC_WFD";
    private static final int UIBC_ENABLED_EVENT_VALUE = 2;
    private static final int UIBC_DISABLED_EVENT_VALUE = 3;
    private final Context mContext;
    private int mWfdState = ScreenSharingService.WfdState.UNKNOWN;
    private int mWfdType = ScreenSharingService.WfdType.SOURCE;
    private String mConnectedDeviceAddress;

    private final WifiP2pManagerHelper mWifiP2pManagerHelper;
    private final Handler mHandler = new Handler();
    private Handler mWfdHandler;
    private HandlerThread mWfdThread;
    private IActionListener mActionListener;
    private Bundle mSurfacePro;
    private volatile boolean mIsSessionCanceled;

    private final List<IWfdStatusListener> mWfdStatusListeners = new ArrayList<>();
    private ISessionManagerService mSessionManagerService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: Wfd service");
            mSessionManagerService = ISessionManagerService.Stub.asInterface(service);
            registerWfdActionLister();
            updateLocalWfdState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected: Wfd service");
            mSessionManagerService = null;
            resetConnectedDeviceAddress();
            updateLocalWfdState(ScreenSharingService.WfdState.UNKNOWN);
        }
    };

    private final IWfdActionListener mWfdActionListener = new IWfdActionListener.Stub() {
        @Override
        public void onStateUpdate(int newState, int sessionId) {
            Log.d(TAG, "onStateUpdate newState = " +newState + " sessionId = "+sessionId);
            if ((newState == WfdEnums.SessionState.INITIALIZED.ordinal() &&  sessionId > 0)
                    || newState == WfdEnums.SessionState.TEARDOWN.ordinal()
                    || newState == WfdEnums.SessionState.INVALID.ordinal()) {
                mHandler.post(() -> {
                    deinitSession();
                    teardownP2pConnection();
                    dismissWfdSurfaceIfNeeded();
                });
            }
            if (newState == WfdEnums.SessionState.PLAY.ordinal()) {
                // To make sure surface must be updated on sink in below case.
                // 1.When first start WFDSession, SurfaceView finish creation faster than
                // WFDSession state is PLAY.
                // 2.There is no screen lock when turn on screen.
                if (mWfdSurfaceView != null) {
                    updateSurfaceProEx();
                }
                mHandler.post(() -> {
                    notifyClientSuccess();
                });
            }
            mHandler.post(() -> updateLocalWfdState());
        }

        @Override
        public void notifyEvent(int event, int sessionId) {
            Log.d(TAG, "notifyEvent event = " + event);
            mHandler.post(() -> {
                if (event == UIBC_ENABLED_EVENT_VALUE) {
                    if (mWfdSurfaceView != null) {
                        mWfdSurfaceView.startUIBCEventCapture();
                        updateSurfaceProEx();
                    }
                } else if (event == UIBC_DISABLED_EVENT_VALUE) {
                    if (mWfdSurfaceView != null) mWfdSurfaceView.stopUIBCEventCapture();
                }
            });
        }

        @Override
        public void notify(Bundle b, int sessionId) {

        }
    };

    private final WifiP2pManagerHelper.P2pStatusListener mP2pStatusListener
            = new WifiP2pManagerHelper.P2pStatusListener() {
        @Override
        public void onConnected(WifiP2pGroup wifiP2pGroup) {
            Log.d(TAG, "p2p onConnected");
            if (wifiP2pGroup != null && mWfdType == ScreenSharingService.WfdType.SINK) {
                Collection<WifiP2pDevice> wifiP2pDevices = wifiP2pGroup.getClientList();

                if (wifiP2pDevices.isEmpty()) {
                    Log.d(TAG, "p2p group created but no device connected");
                    teardownSession();
                } else {
                    for (WifiP2pDevice wifiP2pDevice : wifiP2pDevices) {
                        // GroupOwner will receive two broadcasts when only one client is connected,
                        // IP address of Client device in the first broadcast is empty because it
                        // still not be assigned, and when IP address be assigned it will receive
                        // the second broadcast. So start session only when IP address is assigned.
                        if (wifiP2pDevice.getIpAddress() != null) {
                            WifiP2pWfdInfo wfdInfo = wifiP2pDevice.getWfdInfo();
                            if (wfdInfo != null && wfdInfo.isSessionAvailable()
                                    && wfdInfo.getDeviceType() == WifiP2pWfdInfo.DEVICE_TYPE_WFD_SOURCE) {
                                Log.d(TAG, "start session as sink");
                                startWfdSession(mWifiP2pManagerHelper.getLocalWifiP2pDevice(),
                                        wifiP2pDevice);
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "p2p onDisconnected mWfdState = " + mWfdState);
            if (mWfdState == ScreenSharingService.WfdState.BUSY) {
                notifyClientSuccess();
                handleStopScreenSharing(null);
            }
        }

        @Override
        public void onP2pStateChanged() {
            if (mSessionManagerService != null) {
                updateLocalWfdState();
            }
        }
    };
    private FrameLayout mSinkUIContainer;
    private WfdSurfaceView mWfdSurfaceView;
    private IWfdSession mWfdSession;

    public ScreenSharingServiceImpl(Context context) {
        mContext = context;
        mWifiP2pManagerHelper = new WifiP2pManagerHelper(context);
        mWifiP2pManagerHelper.registerListener(mP2pStatusListener);
        mWifiP2pManagerHelper.initialize();
        bindWfdService();
    }

    public void destroyService() {
        unbindWfdService();
        mWifiP2pManagerHelper.unregisterListener(mP2pStatusListener);
        mWifiP2pManagerHelper.deinit();
    }

    @Override
    public void requestDeviceInfo(IDeviceInfoListener listener) {
        checkPermission(LOCAL_ADDRESS_PERMISSION);
        mHandler.post(() -> {
            mWifiP2pManagerHelper.requestLocalDeviceInfo(address -> {
                try {
                    listener.onDeviceAddressAvailable(address);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public void setWfdDeviceType(int deviceType) {
        checkPermission(CONFIGURE_WIFI_DISPLAY_PERMISSION);
        mHandler.post(() -> {
            mWfdType = deviceType;
            mWifiP2pManagerHelper.setWfdDeviceType(deviceType);
        });
    }

    @Override
    public int getWfdDeviceType() {
        return mWfdType;
    }

    @Override
    public int getWfdState() {
        return mWfdState;
    }

    @Override
    public String getConnectedDeviceAddress() {
        return mConnectedDeviceAddress;
    }

    @Override
    public void startScreenSharing(String peerAddress, IActionListener listener) {
        checkPermission(QC_WFD_PERMISSION);
        mHandler.post(() -> handleStartScreenSharing(peerAddress, listener));
    }

    @Override
    public void stopScreenSharing(IActionListener listener) {
        checkPermission(QC_WFD_PERMISSION);
        mHandler.post(() -> handleStopScreenSharing(listener));
    }

    @Override
    public void registerWfdStatusListener(IWfdStatusListener listener) {
        checkPermission(QC_WFD_PERMISSION);
        mHandler.post(() -> {
            if (!mWfdStatusListeners.contains(listener)) {
                mWfdStatusListeners.add(listener);
                try {
                    listener.onWfdStateChanged(mWfdState);
                    listener.asBinder().linkToDeath(new DeathRecipient() {
                        @Override
                        public void binderDied() {
                            mWfdStatusListeners.remove(listener);
                        }
                    }, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void unregisterWfdStatusListener(IWfdStatusListener listener) {
        checkPermission(QC_WFD_PERMISSION);
        mHandler.post(() -> mWfdStatusListeners.remove(listener));
    }

    private void checkPermission(String permission) {
        String callingPackage = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (QESDK_DCF_CLIENT_PACKAGE_NAME.equals(callingPackage)) return;

        // Otherwise,explicitly check for caller permission ...
        if (mContext.checkCallingOrSelfPermission(permission) != PERMISSION_GRANTED) {
            Log.w(TAG, "Caller needs permission '" + permission + "' " + callingPackage);
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid()
                    + ", must have permission " + permission);
        }
    }

    private void handleStopScreenSharing(IActionListener listener) {
        mActionListener = listener;
        if (mWfdState == ScreenSharingService.WfdState.BUSY) {
            teardownSession();
        } else {
            teardownP2pConnection();
        }
    }

    private void teardownSession() {
        mWfdHandler.post(()-> {
            if (mWfdSession != null) {
                try {
                    Log.w(TAG, "teardown session");
                    mWfdSession.teardown();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void deinitSession() {
        mIsSessionCanceled = true;
        mWfdHandler.post(() -> {
            if (mWfdSession != null) {
                try {
                    Log.w(TAG, "deinit session");
                    mWfdSession.deinit();
                    mWfdSession = null;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleStartScreenSharing(String peerAddress, IActionListener listener) {
        mActionListener = listener;
        if (mSessionManagerService == null) {
            notifyClientFailure(ScreenSharingService.Error.NO_WFD_SERVICE);
        } else {
            establishP2pConnection(peerAddress);
        }
    }

    private void establishP2pConnection(String peerAddress) {
        mWifiP2pManagerHelper.establishP2pConnection(peerAddress,
                new WifiP2pManagerHelper.P2pActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "establishP2pConnection onSuccess");
                startWfdSession(mWifiP2pManagerHelper.getLocalWifiP2pDevice(),
                        mWifiP2pManagerHelper.getPeerWifiP2pDevice());
            }

            @Override
            public void onFailure(int reasonCode) {
                notifyClientFailure(reasonCode);
                Log.d(TAG, "establishP2pConnection onFailure reasonCode = " + reasonCode);
            }
        });
    }

    private void teardownP2pConnection() {
        mWifiP2pManagerHelper.setMiracastMode(WifiP2pManager.MIRACAST_DISABLED);
        mWifiP2pManagerHelper.teardownP2pConnection(
                new WifiP2pManagerHelper.P2pActionListener() {
            @Override
            public void onSuccess() {
                notifyClientSuccess();
                Log.d(TAG, "teardownP2pConnection onSuccess");
            }

            @Override
            public void onFailure(int reasonCode) {
                notifyClientFailure(reasonCode);
                Log.d(TAG, "teardownP2pConnection onFailure");
            }
        });
    }

    private void notifyClientSuccess() {
        if (mActionListener != null) {
            try {
                mActionListener.onSuccess();
                mActionListener = null;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyClientFailure(int reasonCode) {
        if (mActionListener != null) {
            try {
                mActionListener.onFailure(reasonCode);
                mActionListener = null;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void bindWfdService() {
        Intent wfdServiceIntent = new Intent(SERVICE_ACTION);
        wfdServiceIntent.setPackage(SERVICE_PACKAGE);
        if (!mContext.bindService(wfdServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "bind wfd Service: bind fail!!!");
        }
        mWfdThread = new HandlerThread("wfd");
        mWfdThread.start();
        mWfdHandler = new Handler(mWfdThread.getLooper());
    }

    private void unbindWfdService() {
        deinitSession();
        mContext.unbindService(mServiceConnection);
        mSessionManagerService = null;
        mWfdThread.quit();
    }

    private void registerWfdActionLister() {
        try {
            IBinder binder =  mSessionManagerService.getManagedSession();
            if (binder instanceof IWfdSession) {
                mWfdSession = (IWfdSession) binder;
                mWfdSession.registerListener(mWfdActionListener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private WfdStatus getWfdStatus() {
        if (mWfdSession != null) {
            try {
                return mWfdSession.getStatus();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private int convertToLocalWfdState(int state) {
        if (state == WfdEnums.SessionState.INITIALIZED.ordinal()){
            return ScreenSharingService.WfdState.IDLE;
        } else if (state == WfdEnums.SessionState.ESTABLISHED.ordinal()
                || state == WfdEnums.SessionState.PLAY.ordinal()
                || state == WfdEnums.SessionState.PAUSE.ordinal()
                || state == WfdEnums.SessionState.STANDBY.ordinal()) {
            return ScreenSharingService.WfdState.BUSY;
        } else if (state == WfdEnums.SessionState.INVALID.ordinal()){
            return ScreenSharingService.WfdState.UNKNOWN;
        } else {
            Log.d(TAG, "do nothing since intermediate state");
            return mWfdState;
        }
    }

    private void updateLocalWfdState() {
        mWfdHandler.post(() -> {
            if (!mWifiP2pManagerHelper.isP2pStateEnabled()) {
                updateLocalWfdState(ScreenSharingService.WfdState.UNKNOWN);
                return;
            }

            WfdStatus wfdStatus = getWfdStatus();
            if (wfdStatus != null) {
                if (wfdStatus.connectedDevice != null) {
                    mConnectedDeviceAddress = wfdStatus.connectedDevice.macAddress;
                } else {
                    resetConnectedDeviceAddress();
                }
                updateLocalWfdState(convertToLocalWfdState(wfdStatus.state));
            } else {
                resetConnectedDeviceAddress();
                updateLocalWfdState(ScreenSharingService.WfdState.IDLE);
            }
        });
    }

    private void updateLocalWfdState(int wfdState) {
        if (mWfdState != wfdState) {
            mWfdState = wfdState;
            notifyWfdStateChanged();
        }
    }

    private void resetConnectedDeviceAddress() {
        mConnectedDeviceAddress = "";
    }

    private void notifyWfdStateChanged() {
        for (IWfdStatusListener listener : mWfdStatusListeners) {
            try {
                listener.onWfdStateChanged(mWfdState);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void startWfdSession(WifiP2pDevice localDevice, WifiP2pDevice peerDevice) {
        mWfdSession = createWfdSession();
        if (mWfdSession == null) {
            notifyClientFailure(ScreenSharingService.Error.CREATE_SESSION_FAILED);
        } else {
            setMiracastModeIfNeeded();
            setAvPlaybackMode();
            showWfdSurfaceIfNeeded();
            startWfdSessionAsync(localDevice, peerDevice);
        }
    }

    private void setMiracastModeIfNeeded() {
        if (mWfdType == ScreenSharingService.WfdType.SOURCE) {
            mWifiP2pManagerHelper.setMiracastMode(WifiP2pManager.MIRACAST_SOURCE);
        }
    }

    private void showWfdSurfaceIfNeeded() {
        if (mWfdType == ScreenSharingService.WfdType.SINK) {
            showWfdSurface();
        }
    }

    private void setAvPlaybackMode() {
        mWfdHandler.post(() -> {
            if (mWfdSession != null) {
                try {
                    mWfdSession.setAvPlaybackMode(WfdEnums.AVPlaybackMode.AUDIO_VIDEO.ordinal());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private IWfdSession createWfdSession() {
        try {
            IWfdSession wfdSession = IWfdSession.Stub.asInterface(
                    mSessionManagerService.getWiFiDisplaySession());
            if (mWfdType == ScreenSharingService.WfdType.SOURCE) {
                wfdSession.setDeviceType(WfdEnums.WFDDeviceType.SOURCE.getCode());
            } else {
                wfdSession.setDeviceType(WfdEnums.WFDDeviceType.PRIMARY_SINK.getCode());
            }
            return wfdSession;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void startWfdSessionAsync(WifiP2pDevice localDevice, WifiP2pDevice peerDevice) {
        mIsSessionCanceled = false;
        mWfdHandler.post(() -> {
            int ret = -1;
            if (mWfdSession != null) {
                try {
                    WfdDevice localWfdDevice = null;
                    if (!mIsSessionCanceled) {
                        localWfdDevice = convertToWfdDevice(localDevice, false);
                    }

                    WfdDevice peerWfdDevice = null;
                    if (!mIsSessionCanceled) {
                        peerWfdDevice = convertToWfdDevice(peerDevice, true);
                    }

                    if (!mIsSessionCanceled) {
                        int initRet = mWfdSession.init(mWfdActionListener, localWfdDevice);
                        Log.d(TAG, "startWfdSessionAsync initRet = " +initRet);
                    }

                    if (!mIsSessionCanceled) {
                        ret = mWfdSession.startWfdSession(peerWfdDevice);
                        Log.d(TAG, "startWfdSessionAsync ret = " +ret);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            if (ret < 0) {
                notifyFailResultOfStartSession(ret);
            }
        });
    }

    private WfdDevice convertToWfdDevice(WifiP2pDevice p2pDevice, boolean needIp) {
        WfdDevice wfdDevice = new WfdDevice();
        WifiP2pWfdInfo wfdInfo = p2pDevice.getWfdInfo();

        wfdDevice.macAddress = p2pDevice.deviceAddress;
        wfdDevice.deviceName = p2pDevice.deviceName;
        wfdDevice.deviceType = wfdInfo.getDeviceType();
        wfdDevice.netType = WfdEnums.NetType.WIFI_P2P.ordinal();
        wfdDevice.rtspPort = wfdInfo.getControlPort();
        wfdDevice.isAvailableForSession = wfdInfo.isSessionAvailable();
        if (wfdInfo.isR2Supported()) {
            wfdDevice.extSupport = wfdInfo.getR2DeviceInfo();
        }

        if (needIp) {
            WifiP2pInfo wifiP2pInfo = mWifiP2pManagerHelper.getConnectionInfo();
            if (wifiP2pInfo != null && !wifiP2pInfo.isGroupOwner) {
                wfdDevice.ipAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
            } else {
                wfdDevice.ipAddress = p2pDevice.getIpAddress().getHostAddress();
            }

            Log.d(TAG, "ipAddress = " + wfdDevice.ipAddress);
        }

        return wfdDevice;
    }

    private void notifyFailResultOfStartSession(int sessionId) {
        mHandler.post(() -> {
            deinitSession();
            dismissWfdSurfaceIfNeeded();
            teardownP2pConnection();
            notifyClientFailure(sessionId == WfdEnums.ErrorType.OPERATION_TIMED_OUT.getCode() ?
                    ScreenSharingService.Error.START_SESSION_TIMEOUT :
                    ScreenSharingService.Error.START_SESSION_FAILED);
        });
    }


    private void showWfdSurface() {
        WindowManager windowManager = (WindowManager) mContext.
                getSystemService(Context.WINDOW_SERVICE);
        mSinkUIContainer = new FrameLayout(mContext);
        mWfdSurfaceView = new WfdSurfaceView(mContext, this, this);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        ImageButton tearDownBtn = new ImageButton(mContext);
        tearDownBtn.setOnClickListener(v -> {
            teardownSession();
            dismissWfdSurfaceIfNeeded();
        });
        tearDownBtn.setImageResource(R.drawable.ic_baseline_clear_24);
        tearDownBtn.setBackgroundResource(0);
        FrameLayout.LayoutParams btnLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLayoutParams.leftMargin = 100;
        btnLayoutParams.topMargin = 100;
        mSinkUIContainer.addView(mWfdSurfaceView);
        mSinkUIContainer.addView(tearDownBtn, btnLayoutParams);
        windowManager.addView(mSinkUIContainer, layoutParams);
    }

    private void dismissWfdSurfaceIfNeeded() {
        WindowManager windowManager = (WindowManager) mContext.
                getSystemService(Context.WINDOW_SERVICE);
        if (mSinkUIContainer != null) {
            windowManager.removeView(mSinkUIContainer);
            mWfdSurfaceView = null;
            mSinkUIContainer = null;
        }
    }

    @Override
    public void surfaceCreated(Surface surface) {
        mWifiP2pManagerHelper.setMiracastMode(WifiP2pManager.MIRACAST_SINK);
        mWfdHandler.post(() -> {
            if (mWfdSession != null) {
                try {
                    int setRet = mWfdSession.setSurface(surface);
                    int playRet = mWfdSession.play();
                    Log.d(TAG, "setRet = " + setRet + " playRet = " + playRet);
                    if (mWfdSession.getUIBCStatus()) {
                        if (mWfdSurfaceView != null) mWfdSurfaceView.startUIBCEventCapture();
                    } else {
                        if (mWfdSurfaceView != null) mWfdSurfaceView.stopUIBCEventCapture();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void surfaceChanged(Bundle surfProp) {
        mSurfacePro = surfProp;
        updateSurfaceProEx();
    }

    @Override
    public void surfaceDestroyed() {
        mSurfacePro = null;
        mWifiP2pManagerHelper.setMiracastMode(WifiP2pManager.MIRACAST_DISABLED);
        mWfdHandler.post(() -> {
            if (mWfdSession != null) {
                try {
                    mWfdSession.setSurface(null);
                    mWfdSession.pause();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean sendEvent(InputEvent event) {
        if (mWfdSession != null) {
            try {
                int ret = mWfdSession.sendEvent(event);
                if (ret != 0) {
                    Log.e(TAG, "Error: " + WfdEnums.ErrorType.getValue(ret));
                    return false;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    private void updateSurfaceProEx() {
        mWfdHandler.post(() -> {
            if (mWfdSession != null && mSurfacePro != null) {
                try {
                    WfdStatus status = mWfdSession.getStatus();
                    // When PAUSE, there is neither decoding nor rendering anything.
                    // When PLAYING or PAUSING,  the lock is held as this may take slight time
                    // to complete. So, it is unsafe to update surface during this time.
                    // Thus, only allow when WFDSession state is IDLE and PLAY.
                    if (status.state != WfdEnums.SessionState.IDLE.ordinal() &&
                            status.state != WfdEnums.SessionState.PLAY.ordinal()) {
                        Log.w(TAG, "updateSurfaceProEx: skip update surface for WfdSession is " +
                                "not ready, state=" + status.state);
                        return;
                    }
                    int ret = mWfdSession.setSurfacePropEx(mSurfacePro);
                    if (ret != 0) {
                        Log.e(TAG, "setSurfacePropEx Error: " + WfdEnums.ErrorType.getValue(ret));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
