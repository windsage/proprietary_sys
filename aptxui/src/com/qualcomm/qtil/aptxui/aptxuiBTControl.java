/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxui;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothLeAudioCodecStatus;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.Executor;

public class aptxuiBTControl {
  private static final String TAG = "aptxuiBTControl";
  private static final boolean DBG = false;
  private static final boolean VDBG = false;

  private aptxuiApplication mApp = null;
  private Context mContext = null;
  private Executor mExecutor;

  public static BluetoothAdapter mAdapter;
  private static BluetoothA2dp mA2dp = null;
  private static BluetoothLeAudio mLeAudio = null;
  private static BluetoothGatt mBluetoothGatt = null;

  private boolean mLeAudioCallbackRegistered = false;
  private boolean mPreferredAudioProfilesChangedCallbackRegistered = false;
  private int mAudioModeOutputOnly = 0;
  private int mAudioModeDuplex = 0;

  private static aptxuiA2dpCodecConfig mA2dpCodecConfig = null;
  private static aptxuiLeAudioCodecConfig mLeAudioCodecConfig = null;
  private static aptxuiBTControlHandler mHandler = null;

  /* Message types for the handler */
  private static final int MSG_ADAPTER_ACTION_STATE_CHANGED = 1;
  private static final int MSG_A2DP_ACTION_CONNECTION_STATE_CHANGED = 2;
  private static final int MSG_A2DP_ACTION_ACTIVE_DEVICE_CHANGED = 3;
  private static final int MSG_A2DP_ACTION_CODEC_CONFIG_CHANGED = 4;
  private static final int MSG_ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED = 5;
  private static final int MSG_ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED = 6;
  private static final int MSG_ACTION_LE_AUDIO_CODEC_CONFIG_CHANGED = 7;
  private static final int MSG_CONNECT_TO_A2DP_PROFILE = 8;
  private static final int MSG_CONNECT_TO_LE_AUDIO_PROFILE = 9;

  /* QSS UUID */
  private static final UUID QSS_SERVICE_UUID =
      UUID.fromString("af377309-c034-4bf0-817a-d9c3af214a8f");
  private static final UUID QSS_CHARACTERISTIC_UUID =
      UUID.fromString("66ee0663-8495-4c60-a81a-b587c2518d54");

  /** Constructor of aptxuiBTControl. */
  aptxuiBTControl(Context context) {
    mContext = context;
    mApp = (aptxuiApplication) mContext;
    mExecutor = context.getMainExecutor();

    final BluetoothManager bluetoothManager =
        (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

    if (bluetoothManager == null) {
      throw new RuntimeException("Failed to create bluetoothManager object");
    }

    mAdapter = bluetoothManager.getAdapter();
    if (mAdapter == null) {
      throw new RuntimeException("Failed to create adapter object");
    }

    mA2dpCodecConfig = new aptxuiA2dpCodecConfig(context);
    if (mA2dpCodecConfig == null) {
      throw new RuntimeException("Failed to A2DP codec config object");
    }

    mLeAudioCodecConfig = new aptxuiLeAudioCodecConfig(context);
    if (mLeAudioCodecConfig == null) {
      throw new RuntimeException("Failed to LE Audio codec config object");
    }

    mHandler = new aptxuiBTControlHandler();
    if (mHandler == null) {
      throw new RuntimeException("Failed to event handler object");
    }

    int a2dpState = mAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
    if (DBG) Log.d(TAG, "a2dpState: " + BluetoothProfile.getConnectionStateName(a2dpState));

    if (a2dpState == BluetoothProfile.STATE_CONNECTED) {
      Log.i(TAG, "A2DP already connected");
      mHandler.obtainMessage(MSG_CONNECT_TO_A2DP_PROFILE).sendToTarget();
    }

    int leAudioSupported = mAdapter.isLeAudioSupported();
    if (leAudioSupported == BluetoothStatusCodes.FEATURE_SUPPORTED) {
      Log.i(TAG, "LE Audio supported: " + leAudioSupported);
    } else if (leAudioSupported == BluetoothStatusCodes.FEATURE_NOT_SUPPORTED) {
      Log.i(TAG, "LE Audio not supported: " + leAudioSupported);
    }

    int leAudioState = mAdapter.getProfileConnectionState(BluetoothProfile.LE_AUDIO);
    if (DBG) Log.d(TAG, "leAudioState: " + BluetoothProfile.getConnectionStateName(leAudioState));

    if (leAudioState == BluetoothProfile.STATE_CONNECTED) {
      Log.i(TAG, "LE Audio already connected");
      mHandler.obtainMessage(MSG_CONNECT_TO_LE_AUDIO_PROFILE).sendToTarget();
    }
  }

  /** Start and initialize. */
  public void start() {
    Log.i(TAG, "start");

    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    intentFilter.addAction(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED);
    intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
    intentFilter.addAction(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED);
    intentFilter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED);
    intentFilter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
    mContext.registerReceiver(mReceiver, intentFilter);
  }

  /** Close and deinitialize. */
  public void cleanup() {
    Log.i(TAG, "cleanup");
    mContext.unregisterReceiver(mReceiver);
  }

  /**
   * Check A2DP profile available.
   *
   * @return true on success, otherwise false
   */
  public boolean isA2dpProfileAvailable() {
    if (VDBG) Log.d(TAG, "isA2dpProfileAvailable: " + (mA2dp != null));
    return (mA2dp != null);
  }

  /**
   * Check if A2DP profile preferred.
   *
   * @return true on success, otherwise false
   */
  public boolean isA2dpProfilePreferred() {
    if (VDBG)
      Log.d(TAG, "isA2dpProfilePreferred: " + (mAudioModeOutputOnly == BluetoothProfile.A2DP));
    return (mAudioModeOutputOnly == BluetoothProfile.A2DP);
  }

  /**
   * Check LE Audio profile available.
   *
   * @return true on success, otherwise false
   */
  public boolean isLeAudioProfileAvailable() {
    if (VDBG) Log.d(TAG, "isLeAudioProfileAvailable: " + (mLeAudio != null));
    return (mLeAudio != null);
  }

  /**
   * Check if LE Audio profile preferred.
   *
   * @return true on success, otherwise false
   */
  public boolean isLeAudioProfilePreferred() {
    if (VDBG)
      Log.d(
          TAG, "isLeAudioProfilePreferred: " + (mAudioModeOutputOnly == BluetoothProfile.LE_AUDIO));
    return (mAudioModeOutputOnly == BluetoothProfile.LE_AUDIO);
  }

  /**
   * Check if dual mode audio is enabled.
   *
   * @return true on success, otherwise false
   */
  public boolean isDualModeAudioEnabled() {
    if (VDBG)
      Log.d(
          TAG,
          "isDualModeAudioEnabled: " + (!(mAudioModeOutputOnly == 0 && mAudioModeDuplex == 0)));
    return (!(mAudioModeOutputOnly == 0 && mAudioModeDuplex == 0));
  }

  /**
   * Gets the preferred profile for each audio mode for system routed audio.
   *
   * @param device Remote Bluetooth Device
   */
  public void getPreferredAudioProfiles(BluetoothDevice device) {
    Bundle mPreferredAudioProfiles = mAdapter.getPreferredAudioProfiles(device);
    mAudioModeOutputOnly = mPreferredAudioProfiles.getInt(BluetoothAdapter.AUDIO_MODE_OUTPUT_ONLY);
    mAudioModeDuplex = mPreferredAudioProfiles.getInt(BluetoothAdapter.AUDIO_MODE_DUPLEX);
    if (DBG)
      Log.d(
          TAG,
          "getPreferredAudioProfiles: device: "
              + device
              + " mAudioModeOutputOnly: "
              + mAudioModeOutputOnly
              + " mAudioModeDuplex: "
              + mAudioModeDuplex);
  }

  /** Get the profile proxy object associated with the A2DP profile. */
  public void initA2dpProfileProxy() {
    Log.i(TAG, "initA2dpProfileProxy");
    if (isA2dpProfileAvailable()) return;
    mAdapter.getProfileProxy(mContext, new aptxuiBTControlListener(), BluetoothProfile.A2DP);
  }

  /** Get the profile proxy object associated with the LE Audio profile. */
  public void initLeAudioProfileProxy() {
    Log.i(TAG, "initLeAudioProfileProxy");
    if (isLeAudioProfileAvailable()) return;
    mAdapter.getProfileProxy(mContext, new aptxuiBTControlListener(), BluetoothProfile.LE_AUDIO);
  }

  /**
   * Connects to the GATT server of the device.
   *
   * @param device remote Bluetooth device
   */
  public void connectGatt(BluetoothDevice device) {
    if (device != null) {
      Log.i(TAG, "connectGatt for " + device + " " + device.getName());

      mBluetoothGatt =
          device.connectGatt(
              mContext, false, new aptxuiGattCallback(), BluetoothDevice.TRANSPORT_LE);

      if (mBluetoothGatt != null) {
        if (DBG) Log.i(TAG, "connectGatt connected for " + mBluetoothGatt.getDevice());
      }
    } else {
      Log.i(TAG, "connectGatt no active device");
    }
  }

  /**
   * Set GATT QSS support for device.
   *
   * @param device remote Bluetooth device
   * @param gattQssService true or false
   * @param gattQssServiceChar true or false
   */
  public void setGattQssServiceSupport(
      BluetoothDevice device, boolean gattQssService, boolean gattQssServiceChar) {
    Log.i(
        TAG,
        "setGattQssServiceSupport for "
            + device
            + " gattQssService: "
            + gattQssService
            + " gattQssServiceChar: "
            + gattQssServiceChar);

    if (mA2dpCodecConfig != null
        && isA2dpProfileAvailable()
        && mA2dpCodecConfig.isA2dpActiveDevice()
        && (isA2dpProfilePreferred() || !isDualModeAudioEnabled())) {
      boolean containsA2dpDevice = mA2dp.getConnectedDevices().contains(device);
      if (containsA2dpDevice) {
        mA2dpCodecConfig.setGattQssServiceSupport(device, gattQssService, gattQssServiceChar);
      }
    }

    if (mLeAudioCodecConfig != null
        && isLeAudioProfileAvailable()
        && mLeAudioCodecConfig.isLeAudioActiveDevice()
        && (isLeAudioProfilePreferred() || !isDualModeAudioEnabled())) {
      boolean containsLeAudioDevice = mLeAudio.getConnectedDevices().contains(device);
      if (containsLeAudioDevice) {
        mLeAudioCodecConfig.setGattQssServiceSupport(device, gattQssService, gattQssServiceChar);
      }
    }
  }

  /** Handler to process broadcast receiver intents. */
  class aptxuiBTControlHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ADAPTER_ACTION_STATE_CHANGED:
          {
            Intent intent = (Intent) msg.obj;
            int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            int previousState =
                intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);

            Log.i(
                TAG,
                "MSG_ADAPTER_ACTION_STATE_CHANGED newState: "
                    + BluetoothAdapter.nameForState(newState)
                    + " previousState: "
                    + BluetoothAdapter.nameForState(previousState));

            switch (newState) {
              case BluetoothAdapter.STATE_TURNING_OFF:
                if (isA2dpProfileAvailable()) {
                  mA2dpCodecConfig.disconnect(null);
                }

                if (isLeAudioProfileAvailable()) {
                  mLeAudioCodecConfig.disconnect(null);
                }
                break;
              default:
                break;
            }
            break;
          }

        case MSG_A2DP_ACTION_CONNECTION_STATE_CHANGED:
          {
            Intent intent = (Intent) msg.obj;
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
            int previousState =
                intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);

            if (device != null)
              Log.i(
                  TAG,
                  "MSG_A2DP_ACTION_CONNECTION_STATE_CHANGED for "
                      + device
                      + " newState: "
                      + BluetoothProfile.getConnectionStateName(newState)
                      + " previousState: "
                      + BluetoothProfile.getConnectionStateName(previousState));

            if (previousState == newState) {
              // Nothing has changed
              break;
            }

            switch (newState) {
              case BluetoothA2dp.STATE_CONNECTING:
              case BluetoothA2dp.STATE_CONNECTED:
                if (!isA2dpProfileAvailable()) {
                  Log.i(TAG, "MSG_A2DP_ACTION_CONNECTION_STATE_CHANGED call initA2dpProfileProxy");
                  initA2dpProfileProxy();
                }
                break;
              case BluetoothA2dp.STATE_DISCONNECTED:
                mA2dpCodecConfig.disconnect(device);
                break;
              default:
                break;
            }
            break;
          }

        case MSG_A2DP_ACTION_ACTIVE_DEVICE_CHANGED:
          {
            Intent intent = (Intent) msg.obj;
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (device != null) {
              Log.i(TAG, "MSG_A2DP_ACTION_ACTIVE_DEVICE_CHANGED for " + device);

              getPreferredAudioProfiles(device);
              mA2dpCodecConfig.getA2dpActiveDevice(device);
              mA2dpCodecConfig.getA2dpCodecsSelectableCapabilities(device);
              mA2dpCodecConfig.getA2dpCodecConfig(device);
              // Only check GATT QSS if Snapdragon Sound enabled and if aptX Adaptive available.
              if (mApp.isQssEnabled()
                  && (mA2dpCodecConfig.isAptXAdaptiveAvailable()
                      && !mA2dpCodecConfig.checkGattQssService(device))) {
                if (DBG) Log.d(TAG, "Connect to the GATT server for " + device);
                connectGatt(device);
              }
            } else {
              Log.i(TAG, "MSG_A2DP_ACTION_ACTIVE_DEVICE_CHANGED no active device");
              mA2dpCodecConfig.disconnect(device);
            }
            break;
          }

        case MSG_A2DP_ACTION_CODEC_CONFIG_CHANGED:
          {
            Intent intent = (Intent) msg.obj;
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothCodecStatus codecStatus =
                intent.getParcelableExtra(BluetoothCodecStatus.EXTRA_CODEC_STATUS);

            if (device != null && codecStatus != null) {
              Log.i(
                  TAG,
                  "MSG_A2DP_ACTION_CODEC_CONFIG_CHANGED for "
                      + device
                      + " codecConfig: "
                      + codecStatus.getCodecConfig());
            } else {
              Log.i(TAG, "MSG_A2DP_ACTION_CODEC_CONFIG_CHANGED codecStatus null for " + device);
            }

            mA2dpCodecConfig.getA2dpCodecsSelectableCapabilities(device);
            mA2dpCodecConfig.getA2dpCodecConfig(device);
            break;
          }

        case MSG_ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED:
          {
            Intent intent = (Intent) msg.obj;
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
            int previousState =
                intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);

            if (device != null)
              Log.i(
                  TAG,
                  "MSG_ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED for "
                      + device
                      + " newState: "
                      + BluetoothProfile.getConnectionStateName(newState)
                      + " previousState: "
                      + BluetoothProfile.getConnectionStateName(previousState));

            if (previousState == newState) {
              // Nothing has changed
              break;
            }

            switch (newState) {
              case BluetoothLeAudio.STATE_CONNECTING:
              case BluetoothLeAudio.STATE_CONNECTED:
                if (!isLeAudioProfileAvailable()) {
                  Log.i(
                      TAG,
                      "MSG_ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED call initLeAudioProfileProxy");
                  initLeAudioProfileProxy();
                }
                break;
              case BluetoothLeAudio.STATE_DISCONNECTED:
                mLeAudioCodecConfig.disconnect(device);
                break;
              default:
                break;
            }
            break;
          }

        case MSG_ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED:
          {
            Intent intent = (Intent) msg.obj;
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (device != null) {
              Log.i(TAG, "MSG_ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED for " + device);

              getPreferredAudioProfiles(device);
              mLeAudioCodecConfig.getLeAudioActiveDevice(device);
              mLeAudioCodecConfig.getLeAudioCodecsSelectableCapabilities(device);
              mLeAudioCodecConfig.getLeAudioCodecConfig(device);
              // Only check GATT QSS if Snapdragon Sound enabled.
              if (mApp.isQssEnabled() && !mLeAudioCodecConfig.checkGattQssService(device)) {
                if (DBG) Log.d(TAG, "Connect to the GATT server for " + device);
                connectGatt(device);
              }
            } else {
              Log.i(TAG, "MSG_ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED no active device");
              mLeAudioCodecConfig.disconnect(device);
            }
            break;
          }

        case MSG_ACTION_LE_AUDIO_CODEC_CONFIG_CHANGED:
          {
            int groupId = msg.arg1;
            BluetoothLeAudioCodecStatus status = (BluetoothLeAudioCodecStatus) msg.obj;
            if (DBG)
              Log.d(
                  TAG,
                  "MSG_ACTION_LE_AUDIO_CODEC_CONFIG_CHANGED for "
                      + groupId
                      + " status:"
                      + status.toString());

            if (mLeAudio == null) {
              Log.w(TAG, "MSG_ACTION_LE_AUDIO_CODEC_CONFIG_CHANGED mLeAudio is null");
              return;
            }

            // Get lead device for the group.
            BluetoothDevice connectedGroupLeadDevice =
                mLeAudio.getConnectedGroupLeadDevice(groupId);
            Log.i(TAG, "MSG_ACTION_LE_AUDIO_CODEC_CONFIG_CHANGED for " + connectedGroupLeadDevice);

            mLeAudioCodecConfig.getLeAudioCodecsSelectableCapabilities(connectedGroupLeadDevice);
            mLeAudioCodecConfig.getLeAudioCodecConfig(connectedGroupLeadDevice);
            break;
          }

        case MSG_CONNECT_TO_A2DP_PROFILE:
          {
            if (!isA2dpProfileAvailable()) {
              Log.i(TAG, "MSG_CONNECT_TO_A2DP_PROFILE call initA2dpProfileProxy");
              initA2dpProfileProxy();
            }
            break;
          }

        case MSG_CONNECT_TO_LE_AUDIO_PROFILE:
          {
            if (!isLeAudioProfileAvailable()) {
              Log.i(TAG, "MSG_CONNECT_TO_LE_AUDIO_PROFILE call initLeAudioProfileProxy");
              initLeAudioProfileProxy();
            }
            break;
          }

        default:
          Log.w(TAG, "Received unexpected msg:" + msg.what);
          break;
      }
    }
  }

  /** Broadcast receiver for all changes to states of various profiles. */
  private final BroadcastReceiver mReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();

          switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
              mHandler.obtainMessage(MSG_ADAPTER_ACTION_STATE_CHANGED, intent).sendToTarget();
              break;

            case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
              mHandler
                  .obtainMessage(MSG_A2DP_ACTION_CONNECTION_STATE_CHANGED, intent)
                  .sendToTarget();
              break;

            case BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED:
              mHandler.obtainMessage(MSG_A2DP_ACTION_ACTIVE_DEVICE_CHANGED, intent).sendToTarget();
              break;

            case BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED:
              mHandler.obtainMessage(MSG_A2DP_ACTION_CODEC_CONFIG_CHANGED, intent).sendToTarget();
              break;

            case BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED:
              mHandler
                  .obtainMessage(MSG_ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED, intent)
                  .sendToTarget();
              break;

            case BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED:
              mHandler
                  .obtainMessage(MSG_ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED, intent)
                  .sendToTarget();
              break;

            default:
              Log.w(TAG, "Received unexpected action:" + action);
              break;
          }
        }
      };

  /** Implements ServiceListener for connected or disconnected events. */
  private final class aptxuiBTControlListener implements BluetoothProfile.ServiceListener {
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      Log.i(TAG, "onServiceConnected: profile:" + BluetoothProfile.getProfileName(profile));

      // Setup Bluetooth profile proxies.
      switch (profile) {
        case BluetoothProfile.A2DP:
          mA2dp = (BluetoothA2dp) proxy;
          mA2dpCodecConfig.initA2dp(mA2dp);
          registerPreferredAudioProfilesChangedCallback();
          break;

        case BluetoothProfile.LE_AUDIO:
          mLeAudio = (BluetoothLeAudio) proxy;
          mLeAudioCodecConfig.initLeAudio(mLeAudio);

          if (!mLeAudioCallbackRegistered) {
            try {
              Log.i(TAG, "registerCallback BluetoothLeAudio.Callback");
              mLeAudio.registerCallback(mExecutor, mLeAudioCallbacks);
              mLeAudioCallbackRegistered = true;
            } catch (Exception e) {
              Log.e(TAG, "Exception on register callback: " + e.toString());
            }
          }
          registerPreferredAudioProfilesChangedCallback();
          break;

        default:
          break;
      }
    }

    @Override
    public void onServiceDisconnected(int profile) {
      Log.i(TAG, "onServiceDisconnected: profile:" + BluetoothProfile.getProfileName(profile));

      // Clear Bluetooth profile proxies.
      switch (profile) {
        case BluetoothProfile.A2DP:
          mA2dp = null;
          mA2dpCodecConfig.initA2dp(mA2dp);
          unregisterPreferredAudioProfilesChangedCallback();
          break;

        case BluetoothProfile.LE_AUDIO:
          if (mLeAudioCallbackRegistered) {
            try {
              Log.i(TAG, "unregisterCallback BluetoothLeAudio.Callback");
              mLeAudio.unregisterCallback(mLeAudioCallbacks);
              mLeAudioCallbackRegistered = false;
            } catch (Exception e) {
              Log.e(TAG, "Exception on unregister callback: " + e.toString());
            }
          }
          mLeAudio = null;
          mLeAudioCodecConfig.initLeAudio(mLeAudio);
          unregisterPreferredAudioProfilesChangedCallback();
          break;

        default:
          break;
      }
    }
  }

  /** Register callback to be notified when the preferred audio profile changes. */
  public void registerPreferredAudioProfilesChangedCallback() {
    if (!mPreferredAudioProfilesChangedCallbackRegistered) {
      try {
        Log.i(TAG, "registerCallback PreferredAudioProfilesChangedCallback");
        mAdapter.registerPreferredAudioProfilesChangedCallback(
            mExecutor, mPreferredAudioProfilesChangedCallback);
        mPreferredAudioProfilesChangedCallbackRegistered = true;
      } catch (Exception e) {
        Log.e(TAG, "Exception on registering callback: " + e.toString());
      }
    }
  }

  /** Unregister callback to be notified when the preferred audio profile changes. */
  public void unregisterPreferredAudioProfilesChangedCallback() {
    if (mPreferredAudioProfilesChangedCallbackRegistered
        && !isA2dpProfileAvailable()
        && !isLeAudioProfileAvailable()) {
      try {
        mPreferredAudioProfilesChangedCallbackRegistered = false;
        mAudioModeOutputOnly = 0;
        mAudioModeDuplex = 0;
        Log.i(TAG, "unregisterCallback PreferredAudioProfilesChangedCallback");
        mAdapter.unregisterPreferredAudioProfilesChangedCallback(
            mPreferredAudioProfilesChangedCallback);
      } catch (Exception e) {
        Log.e(TAG, "Exception on unregistering callback: " + e.toString());
      }
    }
  }

  /** Implements callback methods for preferred audio profile changes. */
  private BluetoothAdapter.PreferredAudioProfilesChangedCallback
      mPreferredAudioProfilesChangedCallback =
          new BluetoothAdapter.PreferredAudioProfilesChangedCallback() {
            @Override
            public void onPreferredAudioProfilesChanged(
                BluetoothDevice device, Bundle preferredAudioProfiles, int status) {
              mAudioModeOutputOnly =
                  preferredAudioProfiles.getInt(BluetoothAdapter.AUDIO_MODE_OUTPUT_ONLY);
              mAudioModeDuplex = preferredAudioProfiles.getInt(BluetoothAdapter.AUDIO_MODE_DUPLEX);

              Log.i(
                  TAG,
                  "onPreferredAudioProfilesChanged: device: "
                      + device
                      + " mAudioModeOutputOnly: "
                      + mAudioModeOutputOnly
                      + " mAudioModeDuplex: "
                      + mAudioModeDuplex
                      + " status: "
                      + status);
            }
          };

  /** Implements callback methods for audio codec config changes. */
  private BluetoothLeAudio.Callback mLeAudioCallbacks =
      new BluetoothLeAudio.Callback() {
        @Override
        public void onCodecConfigChanged(int groupId, BluetoothLeAudioCodecStatus status) {
          if (VDBG) Log.d(TAG, "onCodecConfigChanged for " + groupId + " status " + status);

          Message msg = mHandler.obtainMessage(MSG_ACTION_LE_AUDIO_CODEC_CONFIG_CHANGED);
          msg.arg1 = groupId;
          msg.obj = status;
          mHandler.sendMessage(msg);
        }

        @Override
        public void onGroupStatusChanged(int groupId, int groupStatus) {
          if (VDBG)
            Log.d(TAG, "onGroupStatusChanged for " + groupId + " groupStatus: " + groupStatus);
        }

        @Override
        public void onGroupNodeAdded(BluetoothDevice device, int groupId) {
          if (VDBG) Log.d(TAG, device.getAddress() + " group added: " + groupId);
        }

        @Override
        public void onGroupNodeRemoved(BluetoothDevice device, int groupId) {
          if (VDBG) Log.d(TAG, device.getAddress() + " group removed: " + groupId);
        }
      };

  /** Implements callback methods for GATT events. */
  private class aptxuiGattCallback extends BluetoothGattCallback {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      BluetoothDevice device = gatt.getDevice();
      Log.i(
          TAG,
          "onConnectionStateChange for "
              + device
              + " status: "
              + status
              + " newState: "
              + BluetoothProfile.getConnectionStateName(newState));

      if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
        // Successfully connected to the GATT Server.
        Log.i(TAG, "onConnectionStateChange successfully connected to the GATT Server");
        // Attempt to discover services.
        gatt.discoverServices();
      } else if (status == BluetoothGatt.GATT_SUCCESS
          && newState == BluetoothProfile.STATE_DISCONNECTED) {
        // Successfully disconnected from the GATT Server.
        Log.i(TAG, "onConnectionStateChange successfully disconnected from the GATT Server");
        gatt.close();
        mBluetoothGatt = null;
      } else {
        Log.w(TAG, "onConnectionStateChange failed to setup gatt");
        setGattQssServiceSupport(device, false, false);
        gatt.close();
        mBluetoothGatt = null;
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      BluetoothDevice device = gatt.getDevice();
      if (DBG) Log.d(TAG, "onServicesDiscovered for " + device + " status:" + status);
      if (status != BluetoothGatt.GATT_SUCCESS) {
        Log.w(TAG, "onServicesDiscovered no gatt service for " + device);
        setGattQssServiceSupport(device, false, false);
        gatt.disconnect();
        return;
      }

      if (DBG) {
        for (BluetoothGattService service : gatt.getServices()) {
          Log.i(TAG, "onServicesDiscovered uuid: " + service.getUuid());
        }
      }

      final BluetoothGattService qssService = gatt.getService(QSS_SERVICE_UUID);
      if (qssService == null) {
        Log.w(TAG, "onServicesDiscovered no QSS service for " + device);
        setGattQssServiceSupport(device, false, false);
        gatt.disconnect();
        return;
      }

      final BluetoothGattCharacteristic qssCharacteristic =
          qssService.getCharacteristic(QSS_CHARACTERISTIC_UUID);
      if (qssCharacteristic == null) {
        Log.w(TAG, "onServicesDiscovered no QSS characteristic for " + device);
        setGattQssServiceSupport(device, false, false);
        gatt.disconnect();
        return;
      }

      Log.i(TAG, "onServicesDiscovered read qssCharacteristic for " + device);
      // Read the characteristic from the associated remote device.
      gatt.readCharacteristic(qssCharacteristic);
    }

    @Override
    public void onCharacteristicRead(
        BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
      BluetoothDevice device = gatt.getDevice();
      if (status != BluetoothGatt.GATT_SUCCESS) {
        Log.e(TAG, "onCharacteristicRead failure on " + device + " " + characteristic);
        return;
      }

      if (DBG)
        Log.d(
            TAG,
            "onCharacteristicRead for "
                + device
                + " uuid:"
                + characteristic.getUuid()
                + " status:"
                + status);

      if (QSS_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
        Log.i(TAG, "onCharacteristicRead QSS value: " + (value[0] & 0xFF));

        boolean gattQssService = true;
        boolean gattQssServiceChar = false;
        if ((value[0] & 0xFF) > 0) {
          gattQssServiceChar = true;
        }
        setGattQssServiceSupport(device, gattQssService, gattQssServiceChar);
      }
      gatt.disconnect();
    }
  }

  /** Debugging information. */
  public void dump(PrintWriter pw) {
    pw.println("");
    pw.println("[" + TAG + "]");
    pw.println("Dual Mode Audio Enabled: " + isDualModeAudioEnabled());
    pw.println("A2DP profile available: " + isA2dpProfileAvailable());
    pw.println("A2DP preferred profile: " + isA2dpProfilePreferred());
    pw.println("LE Audio profile available: " + isLeAudioProfileAvailable());
    pw.println("LE Audio preferred profile: " + isLeAudioProfilePreferred());
    pw.println("");

    if (mA2dp == null) {
      pw.println("A2DP profile not available");
    } else {
      mA2dpCodecConfig.dump(pw);
    }

    if (mLeAudio == null) {
      pw.println("LE Audio profile not available");
    } else {
      mLeAudioCodecConfig.dump(pw);
    }
  }
}
