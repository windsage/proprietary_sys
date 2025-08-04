/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxui;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothLeAudioCodecConfig;
import android.bluetooth.BluetoothLeAudioCodecStatus;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/** LE Audio codec configuration. */
public class aptxuiLeAudioCodecConfig {
  private static final String TAG = "aptxuiLeAudioCodecConfig";
  private static final boolean DBG = false;
  private static final boolean VDBG = false;

  private BluetoothLeAudio mLeAudio = null;
  private BluetoothDevice mLeAudioActiveDevice = null;
  private Context mContext = null;
  private int mCodecType = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_INVALID;
  private BluetoothLeAudioCodecConfig mLeAudioCurrentInputCodecConfig = null;
  private BluetoothLeAudioCodecConfig mLeAudioCurrentOutputCodecConfig = null;
  private String mConnectedDeviceAddress = "none";
  private String mPrevConnectedDeviceAddress = "none";
  private boolean mIsQssSupported = false;
  private boolean mIsGattQssService = false;
  private boolean mIsGattQssSupported = false;
  private boolean mNotifyQssSupport = false;

  private List<leAudioDeviceCapability> mLeAudioDeviceCaps =
      new ArrayList<leAudioDeviceCapability>();
  private aptxuiApplication mApp = null;

  /** Constructor of aptxuiLeAudioCodecConfig. */
  aptxuiLeAudioCodecConfig(Context context) {
    mContext = context;
    mApp = (aptxuiApplication) mContext;
  }

  /** LE Audio device input and output selectable capabilities. */
  private class leAudioDeviceCapability {
    BluetoothDevice mDevice = null;
    List<BluetoothLeAudioCodecConfig> mInputCodecSelectableCapabilities = new ArrayList<>();
    List<BluetoothLeAudioCodecConfig> mOutputCodecSelectableCapabilities = new ArrayList<>();
    boolean mCheckGattQssService;
    boolean mGattQssService;
    boolean mGattQssSupport;

    public leAudioDeviceCapability(
        BluetoothDevice device,
        List<BluetoothLeAudioCodecConfig> inputCodecSelectableCapabilities,
        List<BluetoothLeAudioCodecConfig> outputCodecSelectableCapabilities) {
      if (device == null) {
        throw new RuntimeException("Failed to create leAudioDeviceCapability, device is null");
      }
      mDevice = device;

      if (inputCodecSelectableCapabilities == null) {
        throw new RuntimeException(
            "Failed to create leAudioDeviceCapability, inputCodecSelectableCapabilities is null");
      }
      mInputCodecSelectableCapabilities = inputCodecSelectableCapabilities;

      if (outputCodecSelectableCapabilities == null) {
        throw new RuntimeException(
            "Failed to create leAudioDeviceCapability, outputCodecSelectableCapabilities is null");
      }
      mOutputCodecSelectableCapabilities = outputCodecSelectableCapabilities;
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
      if (mOutputCodecSelectableCapabilities == null) return false;
      for (BluetoothLeAudioCodecConfig codecConfig : mOutputCodecSelectableCapabilities) {
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
      for (BluetoothLeAudioCodecConfig codecConfig : mOutputCodecSelectableCapabilities) {
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
   * Set LE Audio object reference.
   *
   * @param leAudio LE Audio object
   */
  void initLeAudio(BluetoothLeAudio leAudio) {
    Log.i(TAG, "initLeAudio:");
    mLeAudio = leAudio;
  }

  /**
   * Check LE Audio is active device.
   *
   * @return true on success, otherwise false
   */
  public boolean isLeAudioActiveDevice() {
    if (VDBG) Log.d(TAG, "isLeAudioActiveDevice: " + (mLeAudioActiveDevice != null));
    return (mLeAudioActiveDevice != null);
  }

  /**
   * Reset states when disconnected.
   *
   * @param device remote Bluetooth device
   */
  public void disconnect(BluetoothDevice device) {
    Log.i(TAG, "disconnect for " + device);
    if (device != null) {
      for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
        if (leAudioDeviceCap.getAddress().equals(device.getAddress())) {
          // Remove cached capabilities.
          if (DBG) Log.d(TAG, "disconnect remove cached capabilities for " + device);
          mLeAudioDeviceCaps.remove(leAudioDeviceCap);
          break;
        }
      }
    } else {
      if (mLeAudio != null) {
        boolean noConnectedLeAudioDevices = mLeAudio.getConnectedDevices().isEmpty();
        if (noConnectedLeAudioDevices && !mLeAudioDeviceCaps.isEmpty()) {
          if (DBG) Log.d(TAG, "disconnect remove all cached capabilities");
          // Remove all cached capabilities.
          mLeAudioDeviceCaps.clear();
        }
      }

      if (DBG) Log.d(TAG, "disconnect reset states");
      // Reset states when no active device.
      mCodecType = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_INVALID;
      mLeAudioCurrentInputCodecConfig = null;
      mLeAudioCurrentOutputCodecConfig = null;
      mLeAudioActiveDevice = null;
      mConnectedDeviceAddress = "none";
      mPrevConnectedDeviceAddress = "none";
      mIsQssSupported = false;
      mIsGattQssService = false;
      mIsGattQssSupported = false;
      mNotifyQssSupport = false;
    }
  }

  /**
   * Get LE Audio active device.
   *
   * @param device remote Bluetooth device
   */
  public void getLeAudioActiveDevice(BluetoothDevice device) {
    if (device == null) {
      Log.w(TAG, "getLeAudioActiveDevice device null");
      return;
    }

    Log.i(TAG, "getLeAudioActiveDevice for " + device + " " + device.getName());

    if (mLeAudio == null) {
      Log.w(TAG, "getLeAudioActiveDevice mLeAudio null");
      return;
    }

    Boolean isActiveDevice =
        aptxuiBTControl.mAdapter.getActiveDevices(BluetoothProfile.LE_AUDIO).contains(device);

    if (isActiveDevice) {
      Log.i(TAG, "getLeAudioActiveDevice set active device for " + device + " " + device.getName());
      // Set active device.
      mLeAudioActiveDevice = device;
    }
  }

  /**
   * Get LE Audio codec config selectable capabilities.
   *
   * @param device remote Bluetooth device
   */
  public void getLeAudioCodecsSelectableCapabilities(BluetoothDevice device) {
    if (device == null) {
      Log.w(TAG, "getLeAudioCodecsSelectableCapabilities device null");
      return;
    }
    Log.i(TAG, "getLeAudioCodecsSelectableCapabilities for " + device + " " + device.getName());

    if (mLeAudioActiveDevice == null) {
      Log.w(TAG, "getLeAudioCodecsSelectableCapabilities mLeAudioActiveDevice null");
      return;
    }

    // Only get selectable capabilities for active device.
    if (!device.getAddress().equals(mLeAudioActiveDevice.getAddress())) {
      Log.i(
          TAG,
          "getLeAudioCodecsSelectableCapabilities only get capabilities for active device "
              + mLeAudioActiveDevice);
      return;
    }

    if (mLeAudio == null) {
      Log.w(TAG, "getLeAudioCodecsSelectableCapabilities mLeAudio null");
      return;
    }

    int groupId = mLeAudio.getGroupId(device);
    BluetoothLeAudioCodecStatus codecStatus = mLeAudio.getCodecStatus(groupId);
    if (codecStatus == null) {
      Log.w(TAG, "getLeAudioCodecsSelectableCapabilities codecStatus null for " + device);
      return;
    }

    List<BluetoothLeAudioCodecConfig> inputCodecSelectableCapabilities =
        codecStatus.getInputCodecSelectableCapabilities();
    if (inputCodecSelectableCapabilities == null) {
      Log.w(TAG, "getLeAudioCodecsSelectableCapabilities inputCodecSelectableCapabilities null");
      return;
    }

    if (DBG) {
      for (BluetoothLeAudioCodecConfig inputCodecConfig : inputCodecSelectableCapabilities) {
        Log.d(TAG, "getLeAudioCodecsSelectableCapabilities inputCodecConfig " + inputCodecConfig);
      }
    }

    List<BluetoothLeAudioCodecConfig> outputCodecSelectableCapabilities =
        codecStatus.getOutputCodecSelectableCapabilities();
    if (outputCodecSelectableCapabilities == null) {
      Log.w(TAG, "getLeAudioCodecsSelectableCapabilities outputCodecSelectableCapabilities null");
      return;
    }

    if (DBG) {
      for (BluetoothLeAudioCodecConfig outputCodecConfig : outputCodecSelectableCapabilities) {
        Log.d(TAG, "getLeAudioCodecsSelectableCapabilities outputCodecConfig " + outputCodecConfig);
      }
    }

    if (!mLeAudioDeviceCaps.isEmpty()) {
      for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
        if (leAudioDeviceCap.getAddress().equals(device.getAddress())) {
          // Check whether cached selectable capabilities contain same selectable capabilities.
          boolean sameInputCodecSelectableCapabilities =
              inputCodecSelectableCapabilities.containsAll(
                  leAudioDeviceCap.mInputCodecSelectableCapabilities);
          boolean sameOutputCodecSelectableCapabilities =
              outputCodecSelectableCapabilities.containsAll(
                  leAudioDeviceCap.mOutputCodecSelectableCapabilities);

          if (sameInputCodecSelectableCapabilities && sameOutputCodecSelectableCapabilities) {
            Log.i(TAG, "getLeAudioCodecsSelectableCapabilities same selectable capabilities");
            // Same selectable capabilities, return.
            return;
          } else {
            Log.i(
                TAG,
                "getLeAudioCodecsSelectableCapabilities remove cached selectable capabilities");
            // Remove cached selectable capabilities.
            mLeAudioDeviceCaps.remove(leAudioDeviceCap);
            break;
          }
        }
      }
    }

    Log.i(TAG, "getLeAudioCodecsSelectableCapabilities add new selectable capabilities");
    leAudioDeviceCapability newLeAudioDeviceCap =
        new leAudioDeviceCapability(
            device, inputCodecSelectableCapabilities, outputCodecSelectableCapabilities);
    mLeAudioDeviceCaps.add(newLeAudioDeviceCap);
  }

  /**
   * Get LE Audio codec config.
   *
   * @param device remote Bluetooth device
   */
  public void getLeAudioCodecConfig(BluetoothDevice device) {
    if (device == null) {
      Log.w(TAG, "getLeAudioCodecConfig device null");
      return;
    }

    if (mLeAudioActiveDevice == null) {
      Log.w(TAG, "getLeAudioCodecConfig mLeAudioActiveDevice null");
      // Reset states when no active device.
      mLeAudioCurrentInputCodecConfig = null;
      mLeAudioCurrentOutputCodecConfig = null;
      return;
    }

    // Only get codec status for active device.
    if (!device.getAddress().equals(mLeAudioActiveDevice.getAddress())) {
      Log.i(
          TAG,
          "getLeAudioCodecConfig only get codecStatus for active device " + mLeAudioActiveDevice);
      return;
    }

    if (mLeAudio == null) {
      Log.w(TAG, "getLeAudioCodecConfig mLeAudio null");
      return;
    }

    int groupId = mLeAudio.getGroupId(mLeAudioActiveDevice);
    BluetoothLeAudioCodecStatus codecStatus = mLeAudio.getCodecStatus(groupId);
    if (codecStatus == null) {
      Log.w(TAG, "getLeAudioCodecConfig codecStatus null");
      return;
    }

    mLeAudioCurrentInputCodecConfig = codecStatus.getInputCodecConfig();
    mLeAudioCurrentOutputCodecConfig = codecStatus.getOutputCodecConfig();
    if (VDBG) Log.d(TAG, "GetLeAudioCodecConfig: codecStatus: " + codecStatus.toString());
    if (DBG)
      Log.d(
          TAG,
          "getLeAudioCodecConfig mLeAudioCurrentInputCodecConfig: "
              + mLeAudioCurrentInputCodecConfig);
    if (DBG)
      Log.d(
          TAG,
          "getLeAudioCodecConfig mLeAudioCurrentOutputCodecConfig: "
              + mLeAudioCurrentOutputCodecConfig);

    if (mLeAudioCurrentOutputCodecConfig != null) {
      // Get output codec type
      mCodecType = mLeAudioCurrentOutputCodecConfig.getCodecType();
    }

    // Notify LE Audio notification.
    notifyLeAudioNotification(device);
  }

  /**
   * Check if aptX Adaptive available for active device output codec config.
   *
   * @return true on success, otherwise false
   */
  public boolean isAptXAdaptiveAvailable() {
    if (mLeAudioDeviceCaps.isEmpty() || mLeAudioActiveDevice == null) return false;
    for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
      if (leAudioDeviceCap.getAddress().equals(mLeAudioActiveDevice.getAddress())) {
        if (leAudioDeviceCap.isCodecTypeAvailable(
            BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE_LE)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if QSS supported for active device, for output codec config.
   *
   * @return true on success, otherwise false
   */
  public boolean isQssSupported() {
    if (mLeAudioDeviceCaps.isEmpty() || mLeAudioActiveDevice == null) return false;
    for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
      if (leAudioDeviceCap.getAddress().equals(mLeAudioActiveDevice.getAddress())) {
        if (isAptXAdaptiveAvailable()
            && leAudioDeviceCap.isSampleRateAvailable(
                BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE_LE,
                BluetoothLeAudioCodecConfig.SAMPLE_RATE_96000)) {
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
    if (mLeAudioDeviceCaps.isEmpty() || mLeAudioActiveDevice == null) return false;
    for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
      if (leAudioDeviceCap.getAddress().equals(mLeAudioActiveDevice.getAddress())) {
        if (leAudioDeviceCap.mGattQssSupport) {
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
    if (mLeAudioDeviceCaps.isEmpty() || mLeAudioActiveDevice == null) return false;
    for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
      if (leAudioDeviceCap.getAddress().equals(mLeAudioActiveDevice.getAddress())) {
        if (leAudioDeviceCap.mGattQssService) {
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
    if (mLeAudioDeviceCaps.isEmpty() || mLeAudioActiveDevice == null) return false;
    for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
      if (leAudioDeviceCap.getAddress().equals(device.getAddress())) {
        if (leAudioDeviceCap.mCheckGattQssService) {
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
    if (!mLeAudioDeviceCaps.isEmpty()) {
      for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
        if (leAudioDeviceCap.getAddress().equals(device.getAddress())) {
          if (DBG) Log.d(TAG, "setGattQssServiceSupport for " + device);
          leAudioDeviceCap.mCheckGattQssService = true;
          leAudioDeviceCap.mGattQssService = gattQssService;
          leAudioDeviceCap.mGattQssSupport = gattQssServiceChar;
        }
      }
    }

    // Notify LE Audio notification.
    notifyLeAudioNotification(device);
  }

  /**
   * Notify LE Audio notification for output codec config.
   *
   * @param device remote Bluetooth device
   */
  private void notifyLeAudioNotification(BluetoothDevice device) {
    if (mLeAudioActiveDevice == null) return;
    mConnectedDeviceAddress = mLeAudioActiveDevice.getAddress();

    if (!device.getAddress().equals(mConnectedDeviceAddress)) {
      Log.i(TAG, "notifyLeAudioNotification only notify for " + mConnectedDeviceAddress);
      // Only notify LE Audio notification for active device.
      return;
    }

    // Only check GATT QSS if Snapdragon Sound enabled.
    if (mApp.isQssEnabled() && !checkGattQssService(device)) {
      Log.i(TAG, "notifyLeAudioNotification GATT QSS service not queried for " + device);
      // GATT QSS service not queried for active device.
      return;
    }

    if (DBG)
      Log.d(
          TAG,
          "notifyLeAudioNotification mCodecType: "
              + mCodecType
              + " mConnectedDeviceAddress: "
              + mConnectedDeviceAddress
              + " mPrevConnectedDeviceAddress: "
              + mPrevConnectedDeviceAddress);

    Intent intent = new Intent(mContext, aptxuiNotify.class);

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
        Log.i(TAG, "notifyLeAudioNotification ACTION_NOTIFY_QSS_SUPPORT");
      }

      mPrevConnectedDeviceAddress = mConnectedDeviceAddress;
    }
  }

  /** Debugging information. */
  public void dump(PrintWriter pw) {
    pw.println("");
    pw.println("[" + TAG + "]");
    pw.println("LE Audio connected devices (" + mLeAudioDeviceCaps.size() + ")");
    if (!mLeAudioDeviceCaps.isEmpty()) {
      for (leAudioDeviceCapability leAudioDeviceCap : mLeAudioDeviceCaps) {
        pw.println("Device: " + leAudioDeviceCap.getAddress());
        pw.println("checkGattQssService: " + leAudioDeviceCap.mCheckGattQssService);
        pw.println("gattQssService: " + leAudioDeviceCap.mGattQssService);
        pw.println("gattQssServiceChar: " + leAudioDeviceCap.mGattQssSupport);
        pw.println("Input capabilities.");
        if (leAudioDeviceCap.mInputCodecSelectableCapabilities != null) {
          for (BluetoothLeAudioCodecConfig codecConfig :
              leAudioDeviceCap.mInputCodecSelectableCapabilities) {
            if (codecConfig != null) {
              pw.println(" " + codecConfig.toString());
            }
          }
        }
        pw.println("Output capabilities.");
        if (leAudioDeviceCap.mOutputCodecSelectableCapabilities != null) {
          for (BluetoothLeAudioCodecConfig codecConfig :
              leAudioDeviceCap.mOutputCodecSelectableCapabilities) {
            if (codecConfig != null) {
              pw.println(" " + codecConfig.toString());
            }
          }
        }
        pw.println("");
      }
    } else {
      pw.println("LE Audio device not connected");
      return;
    }
    pw.println("");

    if (!isLeAudioActiveDevice()) {
      pw.println("<LE Audio device not active>");
      return;
    }

    pw.println("LE Audio active device: " + mLeAudioActiveDevice);
    pw.println("QSS supported: " + isQssSupported());
    pw.println("Check GATT QSS service: " + checkGattQssService(mLeAudioActiveDevice));
    pw.println("GATT QSS service: " + gattQssService());
    pw.println("GATT QSS supported: " + isGattQssSupported());
    pw.println("Notify QSS support: " + mNotifyQssSupport);

    pw.println("Input codec config:");
    if (mLeAudioCurrentInputCodecConfig != null) {
      pw.println(" " + mLeAudioCurrentInputCodecConfig.toString());
    } else {
      pw.println(" {null}");
    }

    pw.println("Output codec config:");
    if (mLeAudioCurrentOutputCodecConfig != null) {
      pw.println(" " + mLeAudioCurrentOutputCodecConfig.toString());
    } else {
      pw.println(" {null}");
    }
    pw.flush();
  }
}
