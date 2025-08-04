/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxui;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/** A2DP codec configuration. */
public class aptxuiA2dpCodecConfig {
  private static final String TAG = "aptxuiA2dpCodecConfig";
  private static final boolean DBG = false;
  private static final boolean VDBG = false;

  private BluetoothA2dp mA2dp = null;
  private BluetoothDevice mA2dpActiveDevice = null;
  private Context mContext = null;
  private int mCodecType = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
  private int mPrevCodec = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
  private BluetoothCodecConfig mCurrentCodecConfig = null;
  private String mConnectedDeviceAddress = "none";
  private String mPrevConnectedDeviceAddress = "none";
  private boolean mIsQssSupported = false;
  private boolean mIsGattQssService = false;
  private boolean mIsGattQssSupported = false;
  private boolean mNotifyQssSupport = false;

  // QHS support.
  private final long QHS_SUPPORT_MASK = 0x00000C00;
  private final long QHS_SUPPORT_NOT_AVAILABLE = 0x00000400;
  private final long QHS_SUPPORT_AVAILABLE = 0x00000800;

  private List<a2dpDeviceCapability> mA2dpDeviceCaps = new ArrayList<a2dpDeviceCapability>();
  private aptxuiApplication mApp = null;

  /** Constructor of aptxuiA2dpCodecConfig. */
  aptxuiA2dpCodecConfig(Context context) {
    mContext = context;
    mApp = (aptxuiApplication) mContext;
  }

  /** A2DP device selectable capabilities. */
  private class a2dpDeviceCapability {
    BluetoothDevice mDevice = null;
    List<BluetoothCodecConfig> mCodecSelectableCapabilities = new ArrayList<>();
    boolean mCheckGattQssService;
    boolean mGattQssService;
    boolean mGattQssSupport;

    public a2dpDeviceCapability(
        BluetoothDevice device, List<BluetoothCodecConfig> codecSelectableCapabilities) {
      if (device == null) {
        throw new RuntimeException("Failed to create a2dpDeviceCapability, device is null");
      }
      mDevice = device;

      if (codecSelectableCapabilities == null) {
        throw new RuntimeException(
            "Failed to create a2dpDeviceCapability, codecSelectableCapabilities is null");
      }
      mCodecSelectableCapabilities = codecSelectableCapabilities;
      mCheckGattQssService = false;
      mGattQssService = false;
      mGattQssSupport = false;
    }

    /**
     * Check if sample rate available for output codec config.
     *
     * @param codecType the codec type
     * @param sampleRate the codec sample rate
     * @return true on success, otherwise false
     */
    public boolean isSampleRateAvailable(int codecType, int sampleRate) {
      if (mCodecSelectableCapabilities == null) return false;
      for (BluetoothCodecConfig codecConfig : mCodecSelectableCapabilities) {
        if (codecConfig.getCodecType() == codecType
            && ((codecConfig.getSampleRate() & sampleRate) > 0)) {
          return true;
        }
      }
      return false;
    }

    /**
     * Check if codec type available for output codec config.
     *
     * @param codecType the codec type
     * @return true on success, otherwise false
     */
    public boolean isCodecTypeAvailable(int codecType) {
      for (BluetoothCodecConfig codecConfig : mCodecSelectableCapabilities) {
        if (codecConfig.getCodecType() == codecType) {
          return true;
        }
      }
      return false;
    }

    /**
     * Returns the hardware address of this BluetoothDevice.
     *
     * @return Bluetooth hardware address as string
     */
    public String getAddress() {
      if (mDevice == null) return "no available";
      return mDevice.getAddress();
    }

    /** Returns the BluetoothDevice. */
    public BluetoothDevice getDevice() {
      return mDevice;
    }
  }

  /**
   * Set A2DP object reference.
   *
   * @param a2dp A2DP object
   */
  void initA2dp(BluetoothA2dp a2dp) {
    Log.i(TAG, "initA2dp:");
    mA2dp = a2dp;
  }

  /**
   * Check A2DP is active device.
   *
   * @return true on success, otherwise false
   */
  public boolean isA2dpActiveDevice() {
    if (VDBG) Log.d(TAG, "isA2dpActiveDevice: " + (mA2dpActiveDevice != null));
    return (mA2dpActiveDevice != null);
  }

  /**
   * Reset states when disconnected.
   *
   * @param device remote Bluetooth device
   */
  public void disconnect(BluetoothDevice device) {
    Log.i(TAG, "disconnect for " + device);
    if (device != null) {
      for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
        if (a2dpDeviceCap.getAddress().equals(device.getAddress())) {
          // Remove cached capabilities.
          if (DBG) Log.d(TAG, "disconnect remove cached capabilities for " + device);
          mA2dpDeviceCaps.remove(a2dpDeviceCap);
          break;
        }
      }
    } else {
      if (mA2dp != null) {
        boolean noConnectedA2dpDevices = mA2dp.getConnectedDevices().isEmpty();
        if (noConnectedA2dpDevices && !mA2dpDeviceCaps.isEmpty()) {
          if (DBG) Log.d(TAG, "disconnect remove all cached capabilities");
          // Remove all cached capabilities.
          mA2dpDeviceCaps.clear();
        }
      }

      if (DBG) Log.d(TAG, "disconnect reset states");
      // Reset states when no active device.
      mCodecType = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
      mCurrentCodecConfig = null;
      mA2dpActiveDevice = null;
      mPrevCodec = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
      mConnectedDeviceAddress = "none";
      mPrevConnectedDeviceAddress = "none";
      mIsQssSupported = false;
      mIsGattQssService = false;
      mIsGattQssSupported = false;
      mNotifyQssSupport = false;
    }
  }

  /**
   * Get A2DP active device.
   *
   * @param device remote Bluetooth device
   */
  public void getA2dpActiveDevice(BluetoothDevice device) {
    if (device == null) {
      Log.w(TAG, "getA2dpActiveDevice device null");
      return;
    }

    Log.i(TAG, "getA2dpActiveDevice for " + device + " " + device.getName());

    if (mA2dp == null) {
      Log.w(TAG, "getA2dpActiveDevice mA2dp null");
      return;
    }

    List<BluetoothDevice> activeDevices =
        aptxuiBTControl.mAdapter.getActiveDevices(BluetoothProfile.A2DP);

    Boolean isActiveDevice = activeDevices.contains(device);

    if (isActiveDevice) {
      Log.i(TAG, "getA2dpActiveDevice set active device for " + device + " " + device.getName());
      // Set active device
      mA2dpActiveDevice = device;
    }

    if (activeDevices == null) {
      // Reset states when no active device.
      mPrevCodec = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
      mPrevConnectedDeviceAddress = "none";
    }
  }

  /**
   * Get A2DP codec config selectable capabilities.
   *
   * @param device remote Bluetooth device
   */
  public void getA2dpCodecsSelectableCapabilities(BluetoothDevice device) {
    if (device == null) {
      Log.w(TAG, "getA2dpCodecsSelectableCapabilities device null");
      return;
    }
    Log.i(TAG, "getA2dpCodecsSelectableCapabilities for " + device + " " + device.getName());

    if (mA2dpActiveDevice == null) {
      Log.w(TAG, "getA2dpCodecsSelectableCapabilities mA2dpActiveDevice null");
      return;
    }

    // Only get selectable capabilities for active device.
    if (!device.getAddress().equals(mA2dpActiveDevice.getAddress())) {
      Log.i(
          TAG,
          "getA2dpCodecsSelectableCapabilities only get selectable capabilities for active device: "
              + mA2dpActiveDevice);
      return;
    }

    if (mA2dp == null) {
      Log.w(TAG, "getA2dpCodecsSelectableCapabilities mA2dp null");
      return;
    }

    BluetoothCodecStatus codecStatus = mA2dp.getCodecStatus(device);
    if (codecStatus == null) {
      Log.w(TAG, "getA2dpCodecsSelectableCapabilities codecStatus null for " + device);
      return;
    }

    List<BluetoothCodecConfig> selectableCapabilities =
        codecStatus.getCodecsSelectableCapabilities();
    if (selectableCapabilities == null) {
      Log.w(TAG, "getA2dpCodecsSelectableCapabilities selectable capabilities null");
      return;
    }

    if (DBG) {
      for (BluetoothCodecConfig codecConfig : selectableCapabilities) {
        Log.d(TAG, "getA2dpCodecsSelectableCapabilities " + codecConfig);
      }
    }

    if (!mA2dpDeviceCaps.isEmpty()) {
      for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
        if (a2dpDeviceCap.getAddress().equals(device.getAddress())) {
          // Check whether cached selectable capabilities contain same selectable capabilities.
          boolean sameSelectableCapabilities =
              selectableCapabilities.containsAll(a2dpDeviceCap.mCodecSelectableCapabilities);

          if (sameSelectableCapabilities) {
            Log.i(TAG, "getA2dpCodecsSelectableCapabilities same selectable capabilities");
            // Same selectable capabilities, return.
            return;
          } else {
            Log.i(TAG, "getA2dpCodecsSelectableCapabilities remove cached selectable capabilities");
            // Remove cached selectable capabilities.
            mA2dpDeviceCaps.remove(a2dpDeviceCap);
            break;
          }
        }
      }
    }

    Log.i(TAG, "getA2dpCodecsSelectableCapabilities add new selectable capabilities");
    a2dpDeviceCapability newA2dpDeviceCap =
        new a2dpDeviceCapability(device, selectableCapabilities);
    mA2dpDeviceCaps.add(newA2dpDeviceCap);
  }

  /**
   * Get A2DP codec config.
   *
   * @param device remote Bluetooth device.
   */
  public void getA2dpCodecConfig(BluetoothDevice device) {
    if (device == null) {
      Log.w(TAG, "getA2dpCodecConfig device null");
      return;
    }

    if (mA2dpActiveDevice == null) {
      Log.w(TAG, "getA2dpCodecConfig mA2dpActiveDevice null");
      // Reset states when no active device.
      mCurrentCodecConfig = null;
      return;
    }

    // Only get codec status for active device.
    if (!device.getAddress().equals(mA2dpActiveDevice.getAddress())) {
      Log.i(TAG, "getA2dpCodecConfig only get codecStatus for active device " + mA2dpActiveDevice);
      return;
    }

    if (mA2dp == null) {
      Log.w(TAG, "getA2dpCodecConfig mA2dp null");
      return;
    }

    BluetoothCodecStatus codecStatus = mA2dp.getCodecStatus(mA2dpActiveDevice);
    if (codecStatus == null) {
      Log.w(TAG, "getA2dpCodecConfig codecStatus null");
      return;
    }

    mCurrentCodecConfig = codecStatus.getCodecConfig();
    if (DBG) Log.d(TAG, "getA2dpCodecConfig mCurrentCodecConfig: " + mCurrentCodecConfig);

    if (mCurrentCodecConfig != null) {
      // Get output codec type
      mCodecType = mCurrentCodecConfig.getCodecType();
    }

    // Notify A2DP notification.
    notifyA2dpNotification(device);
  }

  /**
   * Check if aptX Adaptive codec selected for codec config.
   *
   * @return true on success, otherwise false
   */
  public boolean isAptXAdaptiveSelected() {
    return (mCodecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE);
  }

  /**
   * Check if aptX Adaptive available for active device codec config.
   *
   * @return true on success, otherwise false
   */
  public boolean isAptXAdaptiveAvailable() {
    if (mA2dpDeviceCaps.size() == 0 || mA2dpActiveDevice == null) return false;
    for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
      if (a2dpDeviceCap.getAddress().equals(mA2dpActiveDevice.getAddress())) {
        if (a2dpDeviceCap.isCodecTypeAvailable(
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if QHS supported for codec config.
   *
   * @return true on success, otherwise false
   */
  public boolean isQhsSupported() {
    return ((mCurrentCodecConfig.getCodecSpecific3() & QHS_SUPPORT_MASK) == QHS_SUPPORT_AVAILABLE);
  }

  /**
   * Check if QSS supported for active device, for output codec config.
   *
   * @return true on success, otherwise false
   */
  public boolean isQssSupported() {
    if (mA2dpDeviceCaps.isEmpty() || mA2dpActiveDevice == null) return false;
    for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
      if (a2dpDeviceCap.getAddress().equals(mA2dpActiveDevice.getAddress())) {
        if (isAptXAdaptiveAvailable()
            && a2dpDeviceCap.isSampleRateAvailable(
                BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE,
                BluetoothCodecConfig.SAMPLE_RATE_96000)
            && isQhsSupported()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if GATT QSS supported for active device.
   *
   * @return true on success, otherwise false
   */
  public boolean isGattQssSupported() {
    if (mA2dpDeviceCaps.isEmpty() || mA2dpActiveDevice == null) return false;
    for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
      if (a2dpDeviceCap.getAddress().equals(mA2dpActiveDevice.getAddress())) {
        if (a2dpDeviceCap.mGattQssSupport) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * GATT QSS service for active device.
   *
   * @return true on success, otherwise false
   */
  public boolean gattQssService() {
    if (mA2dpDeviceCaps.isEmpty() || mA2dpActiveDevice == null) return false;
    for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
      if (a2dpDeviceCap.getAddress().equals(mA2dpActiveDevice.getAddress())) {
        if (a2dpDeviceCap.mGattQssService) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if GATT QSS service queried for device.
   *
   * @param device remote Bluetooth device
   * @return true on success, otherwise false
   */
  public boolean checkGattQssService(BluetoothDevice device) {
    if (mA2dpDeviceCaps.isEmpty() || mA2dpActiveDevice == null) return false;
    for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
      if (a2dpDeviceCap.getAddress().equals(device.getAddress())) {
        if (a2dpDeviceCap.mCheckGattQssService) {
          return true;
        }
      }
    }
    return false;
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
    if (!mA2dpDeviceCaps.isEmpty()) {
      for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
        if (a2dpDeviceCap.getAddress().equals(device.getAddress())) {
          if (DBG) Log.d(TAG, "setGattQssServiceSupport for " + device);
          a2dpDeviceCap.mCheckGattQssService = true;
          a2dpDeviceCap.mGattQssService = gattQssService;
          a2dpDeviceCap.mGattQssSupport = gattQssServiceChar;
        }
      }
    }

    // Notify A2DP notification.
    notifyA2dpNotification(device);
  }

  /**
   * Notify A2DP notification for device.
   *
   * @param device remote Bluetooth device
   */
  private void notifyA2dpNotification(BluetoothDevice device) {
    if (mA2dpActiveDevice == null) return;
    mConnectedDeviceAddress = mA2dpActiveDevice.getAddress();

    if (!device.getAddress().equals(mConnectedDeviceAddress)) {
      Log.i(TAG, "notifyA2dpNotification only notify for " + mConnectedDeviceAddress);
      // Only notify A2DP notification for active device.
      return;
    }

    // Only check GATT QSS if Snapdragon Sound enabled and if aptX Adaptive available.
    if (mApp.isQssEnabled() && (isAptXAdaptiveAvailable() && !checkGattQssService(device))) {
      Log.i(TAG, "notifyA2dpNotification GATT QSS service not queried for " + device);
      // GATT QSS service not queried for active device.
      return;
    }

    if (DBG)
      Log.d(
          TAG,
          "notifyA2dpNotification mCodecType: "
              + mCodecType
              + " mConnectedDeviceAddress: "
              + mConnectedDeviceAddress
              + " mPrevConnectedDeviceAddress: "
              + mPrevConnectedDeviceAddress);

    Intent intent = new Intent(mContext, aptxuiNotify.class);
    intent.putExtra("codec", mCodecType);

    // Check if Snapdragon Sound is enabled.
    if (mApp.isQssEnabled()) {
      mIsQssSupported = isQssSupported();
      mIsGattQssService = gattQssService();
      mIsGattQssSupported = isGattQssSupported();
      // If connected device address changes, set mNotifyQssSupport as false.
      if (!mConnectedDeviceAddress.equals(mPrevConnectedDeviceAddress)) {
        mNotifyQssSupport = false;
      }

      // Only send intent if Snapdragon Sound supported and mNotifyQssSupport is false.
      if (((mIsGattQssService && mIsGattQssSupported && isAptXAdaptiveAvailable())
              || (!mIsGattQssService && mIsQssSupported))
          && !mNotifyQssSupport) {
        intent.setAction(aptxuiNotify.ACTION_NOTIFY_QSS_SUPPORT);
        aptxuiNotify.enqueueWork(mContext, intent);
        mNotifyQssSupport = true;
        Log.i(TAG, "notifyA2dpNotification ACTION_NOTIFY_QSS_SUPPORT");
      }
    }

    // Only send intent if codec or connected device address changes.
    if (mCodecType != mPrevCodec
        || (!mConnectedDeviceAddress.equals(mPrevConnectedDeviceAddress))) {

      if (mApp.isQssEnabled()
          && ((mIsGattQssService && mIsGattQssSupported || (!mIsGattQssService && mIsQssSupported))
              && (mCodecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE))) {
        // Do not show codec notification if QSS supported and aptX Adaptive selected.
      } else {
        intent.setAction(aptxuiNotify.ACTION_NOTIFY_A2DP_CODEC);
        aptxuiNotify.enqueueWork(mContext, intent);
        Log.i(TAG, "notifyA2dpNotification ACTION_NOTIFY_A2DP_CODEC");
      }

      mPrevCodec = mCodecType;
      mPrevConnectedDeviceAddress = mConnectedDeviceAddress;
    }
  }

  /** Debugging information. */
  public void dump(PrintWriter pw) {
    pw.println("");
    pw.println("[" + TAG + "]");
    pw.println("A2DP connected devices (" + mA2dpDeviceCaps.size() + ")");
    if (!mA2dpDeviceCaps.isEmpty()) {
      for (a2dpDeviceCapability a2dpDeviceCap : mA2dpDeviceCaps) {
        pw.println("Device: " + a2dpDeviceCap.getAddress());
        pw.println("checkGattQssService: " + a2dpDeviceCap.mCheckGattQssService);
        pw.println("gattQssService: " + a2dpDeviceCap.mGattQssService);
        pw.println("gattQssServiceChar: " + a2dpDeviceCap.mGattQssSupport);
        pw.println("Capabilities: ");
        if (a2dpDeviceCap.mCodecSelectableCapabilities != null) {
          for (BluetoothCodecConfig codecConfig : a2dpDeviceCap.mCodecSelectableCapabilities) {
            if (codecConfig != null) {
              pw.println(" " + codecConfig.toString());
            }
          }
        }
        pw.println("");
      }
    } else {
      pw.println("A2DP device not connected");
      return;
    }
    pw.println("");

    if (!isA2dpActiveDevice()) {
      pw.println("<A2DP device not active>");
      return;
    }

    pw.println("A2DP active device: " + mA2dpActiveDevice);
    pw.println("QSS supported: " + isQssSupported());
    pw.println("Check GATT QSS service: " + checkGattQssService(mA2dpActiveDevice));
    pw.println("GATT QSS service: " + gattQssService());
    pw.println("GATT QSS supported: " + isGattQssSupported());
    pw.println("Notify QSS support: " + mNotifyQssSupport);

    pw.println("Codec config:");
    if (mCurrentCodecConfig != null) {
      pw.println(" " + mCurrentCodecConfig.toString());
    } else {
      pw.println(" {null}");
    }
    pw.flush();
  }
}
