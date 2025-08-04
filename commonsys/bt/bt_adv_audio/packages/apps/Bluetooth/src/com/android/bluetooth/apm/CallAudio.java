/*
 * Copyright (c) 2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*****************************************************************************/

package com.android.bluetooth.apm;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static com.android.bluetooth.Utils.enforceBluetoothPrivilegedPermission;

import android.annotation.RequiresPermission;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.hfp.HeadsetA2dpSync;
import com.android.bluetooth.apm.ApmConst;
import com.android.bluetooth.apm.MediaAudio;
import com.android.bluetooth.apm.CallControl;
import android.media.AudioManager;
import com.android.bluetooth.apm.ActiveDeviceManagerService;
import com.android.bluetooth.btservice.ActiveDeviceManager;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.cc.CCService;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import com.android.bluetooth.Utils;
import android.content.Context;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.util.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import android.content.Intent;
import android.os.UserHandle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;

public class CallAudio {

    private static CallAudio mCallAudio;
    private static final String TAG = "APM: CallAudio";
    Map<String, CallDevice> mCallDevicesMap;
    private Context mContext;
    private AudioManager mAudioManager;
    private ActiveDeviceManagerService mActiveDeviceManager;
    private AdapterService mAdapterService;
    public static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;
    public static final String BLUETOOTH_ADMIN_PERM = android.Manifest.permission.BLUETOOTH_ADMIN;
    public static final String BLUETOOTH_PRIVILEGED =
            android.Manifest.permission.BLUETOOTH_PRIVILEGED;
    private static final int MAX_DEVICES = 200;
    public boolean mVirtualCallStarted;
    private CallControl mCallControl = null;
    private boolean mIsVoipLeaWarEnabled = false;
    private CallAudioMessageHandler mHandler;
    // CallAudio handler messages
    public static final int MESSAGE_UPDATE_CONNECTION_STATE = 1;
    public static final int MESSAGE_VOIP_CALL_STARTED = 2;

    private final class CallAudioMessageHandler extends Handler {
        private CallAudioMessageHandler(Looper looper) {
           super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
           BluetoothDevice mAbsActivedevice =
                   mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.CALL_AUDIO);
           switch (msg.what) {
               case MESSAGE_UPDATE_CONNECTION_STATE:
                   Log.d(TAG, "UPDATE_CONNECTION_STATE");
                   if (mAbsActivedevice != null) {
                       CallDevice mCallDevice = mCallDevicesMap.get(mAbsActivedevice.getAddress());
                       if (mCallDevice.deviceConnStatus == BluetoothProfile.STATE_CONNECTED) {
                           broadcastConnStateChange(mAbsActivedevice, BluetoothProfile.STATE_CONNECTED,
                                   BluetoothProfile.STATE_CONNECTED);
                       }
                   }
                   break;
              case MESSAGE_VOIP_CALL_STARTED:
                   Log.d(TAG, "MESSAGE_VOIP_CALL_STARTED");
                   if (mVirtualCallStarted) {
                       if (getConnectionState(mAbsActivedevice) == BluetoothProfile.STATE_CONNECTED) {
                           broadcastAudioState(mAbsActivedevice, BluetoothHeadset.STATE_AUDIO_CONNECTING,
                                   BluetoothHeadset.STATE_AUDIO_CONNECTED);
                       }
                   }
                   break;
              default:
                   break;
           }
        }
    };

    private CallAudio(Context context) {
        Log.d(TAG, "Initialization");
        mContext = context;
        mCallDevicesMap = new ConcurrentHashMap<String, CallDevice>();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mActiveDeviceManager = ActiveDeviceManagerService.get();
        mAdapterService = AdapterService.getAdapterService();
        mCallControl = CallControl.get();

        mIsVoipLeaWarEnabled =
                SystemProperties.getBoolean("persist.enable.bluetooth.voipleawar", false);
        Log.i(TAG, "mIsVoipLeaWarEnabled: " + mIsVoipLeaWarEnabled);
        if (ApmConstIntf.getAospLeaEnabled() && mIsVoipLeaWarEnabled) {
            HandlerThread thread = new HandlerThread("CallAudioHandler");
            thread.start();
            Looper looper = thread.getLooper();
            mHandler = new CallAudioMessageHandler(looper);
        }
    }

    public static CallAudio init(Context context) {
        if(mCallAudio == null) {
            mCallAudio = new CallAudio(context);
            CallAudioIntf.init(mCallAudio);
        }
        return mCallAudio;
    }

    public static CallAudio get() {
        return mCallAudio;
    }

    protected void cleanup() {
        Log.d(TAG, " cleanup ");
        mCallDevicesMap.clear();
    }

    public boolean autoConnect(BluetoothDevice device) {
        Log.e(TAG, "AutoConnect: " + device);
        return connect(device, true, false);
    }

    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    public boolean connect(BluetoothDevice device) {
        return connect(device, false, false);
    }

    private boolean connect(BluetoothDevice device, boolean autoConnect, boolean allProfiles) {
        Log.e(TAG, "connect: " + device + " allProfile: " + allProfiles +
                                          " autoConnect: " + autoConnect);
        if(device == null) {
            return false;
        }

        boolean isAospLeAudioEnabled = ApmConstIntf.getAospLeaEnabled();
        Log.i(TAG, "isAospLeAudioEnabled: " + isAospLeAudioEnabled);

        if (!isAospLeAudioEnabled &&
            getConnectionPolicy(device) == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            Log.e(TAG, "Cannot connect to " + device + " : CONNECTION_POLICY_FORBIDDEN");
            return false;
        }

        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        if (dMap == null)
            return false;

        int profileID =
             dMap.getSupportedProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
        if (profileID == ApmConst.AudioProfiles.NONE) {
            Log.e(TAG, "Can Not connect to " + device +
                               ". Device does not support call service.");
            return false;
        }

        CallDevice mCallDevice = mCallDevicesMap.get(device.getAddress());
        if(mCallDevice == null) {
            if(mCallDevicesMap.size() >= MAX_DEVICES)
                return false;
            mCallDevice = new CallDevice(device, profileID);
            mCallDevicesMap.put(device.getAddress(), mCallDevice);
        }

        if (!isAospLeAudioEnabled &&
            mCallDevice.profileConnStatus[CallDevice.SCO_STREAM] !=
                                        BluetoothProfile.STATE_CONNECTED) {
           if((ApmConst.AudioProfiles.HFP & profileID) ==
                                             ApmConst.AudioProfiles.HFP) {
             HeadsetService service = HeadsetService.getHeadsetService();
             if (service == null) {
                 return false;
             }
             service.connectHfp(device);
          }
        }

        if (mCallDevice.profileConnStatus[CallDevice.LE_STREAM] ==
                                       BluetoothProfile.STATE_CONNECTED) {
           Log.i(TAG, "LE call already connected");
           return false;
        }

        StreamAudioService mStreamService =
                             StreamAudioService.getStreamAudioService();
        if (mStreamService != null &&
            (ApmConst.AudioProfiles.HAP_LE & profileID) ==
                                       ApmConst.AudioProfiles.HAP_LE) {
           Log.i(TAG, "HAP is supported ");
           if(autoConnect) {
                BluetoothDevice groupDevice = mStreamService.getDeviceGroup(device);
                mStreamService.connectLeStream(groupDevice, profileID);
                Log.i(TAG, "Auto Connect Request. Connecting group: " + groupDevice);
            }
           mStreamService.connectLeStream(device, ApmConst.AudioProfiles.BAP_CALL);
        }
        else if ((mStreamService != null) &&
            (((ApmConst.AudioProfiles.BAP_CALL & profileID) ==
                                       ApmConst.AudioProfiles.BAP_CALL) ||
            ((ApmConst.AudioProfiles.TMAP_CALL & profileID) ==
                                       ApmConst.AudioProfiles.TMAP_CALL))){
            if(autoConnect) {
                BluetoothDevice groupDevice = mStreamService.getDeviceGroup(device);
                mStreamService.connectLeStream(groupDevice, profileID);
                Log.i(TAG, "Auto Connect Request. Connecting group: " + groupDevice);
            } else {
                mStreamService.connectLeStream(device, profileID);
            }
        }
        return true;
    }

    public boolean connect(BluetoothDevice device, Boolean allProfiles) {
        if(allProfiles) {
            DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
            if (dMap == null)
                return false;

            int profileID = dMap.getSupportedProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
            if((ApmConst.AudioProfiles.HFP & profileID) == ApmConst.AudioProfiles.HFP) {
                HeadsetService service = HeadsetService.getHeadsetService();
                if (service == null) {
                    return false;
                }
                return service.connectHfp(device);
            } else {
                /*Common connect for LE Media and Call handled from StreamAudioService*/
                return true;
            }
        }

        return connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        Log.i(TAG, " disconnect: " + device);
        CallDevice mCallDevice;

        if(device == null)
            return false;

        mCallDevice = mCallDevicesMap.get(device.getAddress());
        if(mCallDevice == null) {
            Log.e(TAG, "Ignore: Device " + device + " not present in list");
            return false;
        }

        Log.i(TAG, " disconnect: HFP Connectionstatus: " +
                          mCallDevice.profileConnStatus[CallDevice.SCO_STREAM]);

        boolean isAospLeAudioEnabled = ApmConstIntf.getAospLeaEnabled();
        Log.i(TAG, "isAospLeAudioEnabled: " + isAospLeAudioEnabled);
        if (!isAospLeAudioEnabled &&
            mCallDevice.profileConnStatus[CallDevice.SCO_STREAM] !=
                                          BluetoothProfile.STATE_DISCONNECTED) {
            HeadsetService service = HeadsetService.getHeadsetService();
            if(service != null) {
                service.disconnectHfp(device);
            }
        }

        Log.i(TAG, " disconnect: LeCallAudio Connectionstatus: " +
                          mCallDevice.profileConnStatus[CallDevice.LE_STREAM]);
        if (mCallDevice.profileConnStatus[CallDevice.LE_STREAM] !=
                                           BluetoothProfile.STATE_DISCONNECTED) {
            StreamAudioService service = StreamAudioService.getStreamAudioService();
            if(service != null) {
                service.disconnectLeStream(device, true, false);
            }
        }

        return true;
    }

    public boolean disconnect(BluetoothDevice device, Boolean allProfiles) {
        Log.i(TAG, " disconnect: " + device + ", allProfiles: " + allProfiles);
        if(allProfiles) {
            CallDevice mCallDevice = mCallDevicesMap.get(device.getAddress());
            if(mCallDevice == null) {
                Log.e(TAG, "Ignore: Device " + device + " not present in list");
                return false;
            }

            Log.i(TAG, " disconnect: HFP Connectionstatus: " +
                              mCallDevice.profileConnStatus[CallDevice.SCO_STREAM]);
            if(mCallDevice.profileConnStatus[CallDevice.SCO_STREAM] != BluetoothProfile.STATE_DISCONNECTED) {
                return disconnect(device);
            } else {
                /*Common connect for LE Media and Call handled from StreamAudioService*/
                return true;
            }
        }

        return disconnect(device);
    }

    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    public boolean startScoUsingVirtualVoiceCall() {
        Log.d(TAG, "startScoUsingVirtualVoiceCall");
        BluetoothDevice mActivedevice = null;
        int profile;
        mActiveDeviceManager = ActiveDeviceManagerService.get();
        if(mActiveDeviceManager != null) {
            mActivedevice = mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.CALL_AUDIO);
            if(mActivedevice == null) {
                Log.e(TAG, "startScoUsingVirtualVoiceCall failed. Active Device is null");
                return false;
            }
        } else {
            return false;
        }

        mVirtualCallStarted = true;
        checkA2dpState();

        HeadsetService headsetService = HeadsetService.getHeadsetService();
        profile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.CALL_AUDIO);
        switch(profile) {
            case ApmConst.AudioProfiles.HFP:
                if(headsetService != null) {
                    if(headsetService.startScoUsingVirtualVoiceCall()) {
                        return true;
                    }
                }
                break;
            case ApmConst.AudioProfiles.BAP_CALL:
            case ApmConst.AudioProfiles.TMAP_CALL:
                if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled()) {
                    if(headsetService != null && headsetService.isScoOrCallActive()) {
                        Log.i(TAG, "startScoUsingVirtuallCall: telecom call is ongoing, return false directly");
                        break;
                    }
                    if (getConnectionState(mActivedevice) != BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "startScoUsingVirtuallCall, active device not connected, return false directly");
                        break;
                    }
                    mCallControl = CallControl.get();
                    if (mCallControl != null) {
                        mCallControl.setVirtualCallActive(true);
                    }
                    BluetoothDevice mAbsActivedevice =
                            mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.CALL_AUDIO);
                    Log.i(TAG, "startScoUsingVirtuallCall: activeDevice " + mAbsActivedevice
                            + " audioState " + getAudioState(mAbsActivedevice));

                    // fake SCO audio connecting event once receive startScoUsingVirtualCall request
                    Log.i(TAG, "startScoUsingVirtuallCall, broadcast audio connected ");
                    broadcastAudioState(mAbsActivedevice, BluetoothHeadset.STATE_AUDIO_DISCONNECTED,
                            BluetoothHeadset.STATE_AUDIO_CONNECTING);
                    CallDevice mCallDevice = mCallDevicesMap.get(mAbsActivedevice.getAddress());
                    mCallDevice.broadcastScoStatus = BluetoothHeadset.STATE_AUDIO_CONNECTING;

                    // fake SCO audio connected event if ACM voice audio not connected in 1000ms
                    Message msg = mHandler.obtainMessage(MESSAGE_VOIP_CALL_STARTED);
                    mHandler.sendMessageDelayed(msg, 1000);
                    return true;
                } else {
                    StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
                    if(mStreamAudioService != null) {
                        if(mStreamAudioService.startStream(mActivedevice)) {
                            mCallControl = CallControl.get();
                            if (mCallControl != null) {
                                mCallControl.setVirtualCallActive(true);
                            }
                            return true;
                        }
                    }
                }
                break;
            default:
                Log.e(TAG, "Unhandled profile");
                break;
        }

        Log.e(TAG, "startScoUsingVirtualVoiceCall failed. Device: " + mActivedevice);
        mVirtualCallStarted = false;
        if(ApmConst.AudioProfiles.HFP != profile) {
            HeadsetService service = HeadsetService.getHeadsetService();
            if(service != null) {
                service.getHfpA2DPSyncInterface().releaseA2DP(null);
            }
        }
        return false;
    }

    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    public boolean stopScoUsingVirtualVoiceCall() {
        Log.d(TAG, "stopScoUsingVirtualVoiceCall");
        BluetoothDevice mActivedevice = null;
        int profile;
        mActiveDeviceManager = ActiveDeviceManagerService.get();
        if(mActiveDeviceManager != null) {
            mActivedevice = mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.CALL_AUDIO);
            if(mActivedevice == null) {
                if(mVirtualCallStarted) {
                    mVirtualCallStarted = false;
                    mCallControl = CallControl.get();
                    if (mCallControl != null) {
                        mCallControl.setVirtualCallActive(false);
                    }
                    HeadsetService headsetService = HeadsetService.getHeadsetService();
                    if(headsetService != null) {
                        headsetService.stopScoUsingVirtualVoiceCall();
                    }
                }
                Log.e(TAG, "stopScoUsingVirtualVoiceCall failed. Active Device is null");
                return false;
            }
        } else {
            return false;
        }

        profile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.CALL_AUDIO);
        switch(profile) {
            case ApmConst.AudioProfiles.HFP:
                HeadsetService headsetService = HeadsetService.getHeadsetService();
                if(headsetService != null) {
                    mVirtualCallStarted = false;
                    return headsetService.stopScoUsingVirtualVoiceCall();
                }
                break;
            case ApmConst.AudioProfiles.BAP_CALL:
            case ApmConst.AudioProfiles.TMAP_CALL:
                if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled()) {
                    mVirtualCallStarted = false;
                    mCallControl = CallControl.get();
                    if (mCallControl != null) {
                        mCallControl.setVirtualCallActive(false);
                    }
                    BluetoothDevice mAbsActivedevice =
                            mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.CALL_AUDIO);

                    // fake SCO audio disconnected event while stopScoUsingVirtualCall is invoked
                    broadcastAudioState(mAbsActivedevice, BluetoothHeadset.STATE_AUDIO_CONNECTED,
                            BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                    Log.i(TAG, "stopScoUsingVirtuallCall return true directly ");
                    return true;
                } else {
                    StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
                    if(mStreamAudioService != null) {
                        mVirtualCallStarted = false;
                        mCallControl = CallControl.get();
                        if (mCallControl != null) {
                            mCallControl.setVirtualCallActive(false);
                        }
                        return mStreamAudioService.stopStream(mActivedevice);
                    }
                }
                break;
            default:
                Log.e(TAG, "Unhandled profile");
                break;
        }

        Log.e(TAG, "stopScoUsingVirtualVoiceCall failed. Device: " + mActivedevice);
        return false;
    }

    void remoteDisconnectVirtualVoiceCall(BluetoothDevice device) {
        if(device == null)
            return;
        ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
        if(device.equals(mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.CALL_AUDIO)) &&
                        mActiveDeviceManager.isStableState(ApmConst.AudioFeatures.CALL_AUDIO)) {
            stopScoUsingVirtualVoiceCall();
        }
    }

    public boolean isVirtualCallStarted() {
        Log.d(TAG, "isVirtualCallStarted " + mVirtualCallStarted);
        return mVirtualCallStarted;
    }

    public boolean isVoipLeaWarEnabled() {
        Log.d(TAG, "isVoipLeaWarEnabled " + mIsVoipLeaWarEnabled);
        return mIsVoipLeaWarEnabled;
    }

    public int getProfileConnectionState(BluetoothDevice device, int profile) {
        CallDevice mCallDevice = mCallDevicesMap.get(device.getAddress());
        if(mCallDevice == null)
            return BluetoothProfile.STATE_DISCONNECTED;

        return mCallDevice.profileConnStatus[mCallDevice.getProfileIndex(profile)];
    }

    boolean setActiveProfile(BluetoothDevice device, int profile) {
        if (device == null) {
            return false;
        }

        if(getProfileConnectionState(device, profile) == BluetoothProfile.STATE_DISCONNECTED) {
            return false;
        }

        int volProfile = (profile == ApmConst.AudioProfiles.HFP) ?
                        ApmConst.AudioProfiles.HFP : ApmConst.AudioProfiles.VCP;
        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        dMap.setActiveProfile(device, ApmConst.AudioFeatures.CALL_AUDIO, profile);
        dMap.setActiveProfile(device, ApmConst.AudioFeatures.CALL_VOLUME_CONTROL, volProfile);

        StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
        BluetoothDevice groupDevice = streamAudioService.getDeviceGroup(device);
        if(!device.equals(groupDevice)) {
            dMap.setActiveProfile(groupDevice, ApmConst.AudioFeatures.CALL_AUDIO, profile);
            dMap.setActiveProfile(groupDevice, ApmConst.AudioFeatures.CALL_VOLUME_CONTROL, volProfile);
        }
        CCService ccService = CCService.getCCService();
        if (ccService != null) {
            ccService.updateActiveProfile(device, getProfileConnectionState(device, profile));
        }

        mActiveDeviceManager.setActiveDevice(device, ApmConst.AudioFeatures.CALL_AUDIO, false);
        return true;
    }

    int getProfile(BluetoothDevice mDevice) {
        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        int profileID = dMap.getProfile(mDevice, ApmConst.AudioFeatures.CALL_AUDIO);
        Log.d(TAG," getProfile for device " + mDevice + " profileID " + profileID);
        return profileID;
    }

    void checkA2dpState() {
        MediaAudio sMediaAudio = MediaAudio.get();
        BluetoothDevice sMediaActivedevice =
        mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
        //if(sMediaAudio.isA2dpPlaying(sMediaActivedevice)) {
            Log.d(TAG," suspendA2DP isA2dpPlaying true " + " for device " + sMediaActivedevice);
            int profileID = mActiveDeviceManager.getActiveProfile(
                                ApmConst.AudioFeatures.CALL_AUDIO);
            if(ApmConst.AudioProfiles.HFP != profileID) {
                if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled()) {
                    Log.d(TAG, "No need suspend A2DP for Voip WAR in AOSP LE Audio");
                    return;
                }
                HeadsetService service = HeadsetService.getHeadsetService();
                if(service != null) {
                    service.getHfpA2DPSyncInterface().suspendA2DP(
                    HeadsetA2dpSync.A2DP_SUSPENDED_BY_CS_CALL, null);
                }
            }
        //}
    }

    public int connectAudio() {
        BluetoothDevice mActivedevice = null;
        boolean status = false;

        mActiveDeviceManager = ActiveDeviceManagerService.get();
        if(mActiveDeviceManager != null) {
                mActivedevice = mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.CALL_AUDIO);
        }
        if (mActivedevice == null) {
            Log.w(TAG, "connectAudio: no active device");
            return BluetoothStatusCodes.ERROR_NO_ACTIVE_DEVICES;
        }
        Log.i(TAG, "connectAudio: device=" + mActivedevice + ", " + Utils.getUidPidString());

        int profileID = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.CALL_AUDIO);

        if(ApmConst.AudioProfiles.HFP == profileID) {
            HeadsetService service = HeadsetService.getHeadsetService();
            if (service == null) {
                return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
            }
            enforceBluetoothPrivilegedPermission(service);
            return service.connectAudio(mActivedevice);
        } else if ((ApmConst.AudioProfiles.BAP_CALL == profileID) ||
                  (ApmConst.AudioProfiles.TMAP_CALL == profileID)) {
            boolean isCallActive = false;
            CCService ccService = CCService.getCCService();
            if (ccService != null) {
                isCallActive = (ccService.shouldCallAudioBeActive() || mVirtualCallStarted);
            }
            if (isCallActive) {
                StreamAudioService service = StreamAudioService.getStreamAudioService();
                if(service != null) {
                    checkA2dpState();
                    status = service.startStream(mActivedevice);
                }
            } else {
                Log.i(TAG, "ignoring connectAudio for BAP as no active call");
            }
        } else {
            Log.e(TAG, "Unhandled connect audio request for profile: " + profileID);
            status = false;
        }

        if(status == false) {
            Log.e(TAG, "failed connect audio request for device: " + mActivedevice);
            if(ApmConst.AudioProfiles.HFP != profileID) {
                HeadsetService service = HeadsetService.getHeadsetService();
                if(service != null) {
                    service.getHfpA2DPSyncInterface().releaseA2DP(null);
                }
            }
            return BluetoothStatusCodes.ERROR_UNKNOWN;
        }

        return BluetoothStatusCodes.SUCCESS;
    }

    public int disconnectAudio() {
        BluetoothDevice mActivedevice = null;
        boolean mStatus = false;

        mActiveDeviceManager = ActiveDeviceManagerService.get();
        Log.i(TAG, "disconnectAudio: ActiveDeviceManager=" + mActiveDeviceManager);
        if(mActiveDeviceManager != null) {
                mActivedevice = mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.CALL_AUDIO);
        }
        if (mActivedevice == null) {
            Log.w(TAG, "disconnectAudio: no active device");
            return BluetoothStatusCodes.ERROR_NO_ACTIVE_DEVICES;
        }
        Log.i(TAG, "disconnectAudio: device=" + mActivedevice + ", " + Utils.getUidPidString());

        int profileID = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.CALL_AUDIO);

        if(ApmConst.AudioProfiles.HFP == profileID) {
            HeadsetService service = HeadsetService.getHeadsetService();
            if (service == null) {
                return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
            }
            enforceBluetoothPrivilegedPermission(service);
            return service.disconnectAudio();
        } else if(ApmConst.AudioProfiles.BAP_CALL == profileID) {
            StreamAudioService service = StreamAudioService.getStreamAudioService();
            if(service != null) {
                mStatus = service.stopStream(mActivedevice);
            }
        } else {
            Log.e(TAG, "Unhandled disconnectAudio request for profile: " + profileID);
            mStatus = false;
        }

        if(ApmConst.AudioProfiles.HFP != profileID) {
            HeadsetService service = HeadsetService.getHeadsetService();
            if(service != null) {
                service.getHfpA2DPSyncInterface().releaseA2DP(null);
            }
        }
        return mStatus == false ? BluetoothStatusCodes.ERROR_UNKNOWN :
                BluetoothStatusCodes.SUCCESS;
    }

    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    public boolean setConnectionPolicy(BluetoothDevice device, Integer connectionPolicy) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED,
                "Need BLUETOOTH_PRIVILEGED permission");
        boolean mStatus;

        Log.d(TAG, "setConnectionPolicy: device=" + device
            + ", connectionPolicy=" + connectionPolicy + ", " + Utils.getUidPidString());

        mStatus = mAdapterService.getDatabase()
            .setProfileConnectionPolicy(device, BluetoothProfile.HEADSET, connectionPolicy);

        if (mStatus &&
                connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            connect(device);
        } else if (mStatus &&
                connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            disconnect(device);
        }
        return mStatus;
    }

    public int getConnectionPolicy(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED,
                 "Need BLUETOOTH_PRIVILEGED permission");
        if(mAdapterService != null) {
            int connPolicy;
            connPolicy = mAdapterService.getDatabase()
                .getProfileConnectionPolicy(device, BluetoothProfile.HEADSET);
            Log.d(TAG, "getConnectionPolicy: device=" + device
                    + ", connectionPolicy=" + connPolicy);
            return connPolicy;
        } else {
            return BluetoothProfile.CONNECTION_POLICY_UNKNOWN;

        }
    }

    public int getAudioState(BluetoothDevice device) {
        if(device == null)
            return BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
        CallDevice mCallDevice;
        mCallDevice = mCallDevicesMap.get(device.getAddress());
        if (mCallDevice == null) {
            Log.w(TAG, "getAudioState: device " + device + " was never connected/connecting");
            return BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
        }
        Log.i(TAG, "getAudioState: mCallDevice.scoStatus  " +
                mCallDevice.scoStatus);
        return mCallDevice.scoStatus;
    }

    private List<BluetoothDevice> getNonIdleAudioDevices() {
        if(mCallDevicesMap.size() == 0) {
            return new ArrayList<>(0);
        }

        ArrayList<BluetoothDevice> devices = new ArrayList<>();
        for (CallDevice mCallDevice : mCallDevicesMap.values()) {
            if (mCallDevice.scoStatus != BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                devices.add(mCallDevice.mDevice);
            }
        }
        return devices;
    }

    public boolean isAudioOn() {
        int numConnectedAudioDevices = getNonIdleAudioDevices().size();
        Log.d(TAG," isAudioOn: The number of audio connected devices "
                      + numConnectedAudioDevices);
             return numConnectedAudioDevices > 0;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        Log.i(TAG, "getConnectedDevices: ");
        if(mCallDevicesMap.size() == 0) {
            Log.i(TAG, "no device is Connected:");
            return new ArrayList<>(0);
        }

        List<BluetoothDevice> connectedDevices = new ArrayList<>();
        for(CallDevice mCallDevice : mCallDevicesMap.values()) {
            if(mCallDevice.deviceConnStatus == BluetoothProfile.STATE_CONNECTED) {
                connectedDevices.add(mCallDevice.mDevice);
            }
        }
        Log.i(TAG, "ConnectedDevices: = " + connectedDevices.size());
        return connectedDevices;
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");

        Log.i(TAG, "getDevicesMatchingConnectionStates: ");
        List<BluetoothDevice> devices = new ArrayList<>();
        if (states == null) {
            return devices;
        }

        BluetoothDevice [] bondedDevices = null;
        bondedDevices = mAdapterService.getBondedDevices();
        if(bondedDevices == null) {
            return devices;
        }

        for (BluetoothDevice device : bondedDevices) {
            CallDevice mCallDevice;
            int state = BluetoothProfile.STATE_DISCONNECTED;

            DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
            if(dMap == null) {
                return new ArrayList<>(0);
            }
            if(dMap.getProfile(device, ApmConst.AudioFeatures.CALL_AUDIO) == ApmConst.AudioProfiles.NONE) {
                continue;
            }

            mCallDevice = mCallDevicesMap.get(device.getAddress());
            if(mCallDevice != null)
                state = mCallDevice.deviceConnStatus;

            for(int s: states) {
                if(s == state) {
                    devices.add(device);
                    break;
                }
            }
        }
        return devices;
    }

    public int getConnectionState(BluetoothDevice device) {

        if(device == null)
            return BluetoothProfile.STATE_DISCONNECTED;
        CallDevice mCallDevice;
        mCallDevice = mCallDevicesMap.get(device.getAddress());
        if(mCallDevice != null) {
            Log.d(TAG, "getConnectionState: mCallDevice.deviceConnStatus " +
                    mCallDevice.deviceConnStatus);
            return mCallDevice.deviceConnStatus;
        }
        Log.d(TAG, "getConnectionState: return disconnected");
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    public boolean isVoiceOrCallActive() {
        boolean isVoiceActive = isAudioOn();
        if (ApmConst.getQtiLeAudioEnabled() || !isVoipLeaWarEnabled()) {
            isVoiceActive = isVoiceActive || mVirtualCallStarted;
        }
        HeadsetService mHeadsetService = HeadsetService.getHeadsetService();
        if(mHeadsetService != null) {
            isVoiceActive = isVoiceActive || mHeadsetService.isScoOrCallActive();
        }
        Log.d(TAG, "isVoiceOrCallActive: return " + isVoiceActive);
        return isVoiceActive;
    }

    private void broadcastConnStateChange(BluetoothDevice device, int fromState, int toState) {
        Log.d(TAG,"broadcastConnectionState " + device + ": " + fromState + "->" + toState);
        HeadsetService mHeadsetService = HeadsetService.getHeadsetService();
        if(mHeadsetService == null) {
            Log.w(TAG,"broadcastConnectionState: HeadsetService not initialized. Return!");
            return;
        }

        if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled()) {
            CallDevice mCallDevice = mCallDevicesMap.get(device.getAddress());
            if (mCallDevice != null) {
                Log.w(TAG,"broadcastConnectionState: pendingUpdateDiscState is " +
                        mCallDevice.pendingUpdateDiscState);
            }
            if (mCallDevice != null && mCallDevice.pendingUpdateDiscState != -1) {
                Log.w(TAG,"broadcastConnectionState: pending update disc state. Return!");
                return;
            }
        }

        Intent intent = new Intent(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, fromState);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, toState);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        mHeadsetService.sendBroadcastAsUser(intent, UserHandle.ALL,
                BLUETOOTH_CONNECT, Utils.getTempAllowlistBroadcastOptions());
    }

    private void broadcastAudioState(BluetoothDevice device, int fromState, int toState) {
        Log.d(TAG,"broadcastAudioState " + device + ": " + fromState + "->" + toState);

        if (!ApmConst.getQtiLeAudioEnabled() && !isVoipLeaWarEnabled()) {
            Log.i(TAG, "broadcastAudioState(): Qti LE-A not enabled, return");
            return;
        }

        HeadsetService mHeadsetService = HeadsetService.getHeadsetService();
        if(mHeadsetService == null) {
            Log.d(TAG,"broadcastAudioState: HeadsetService not initialized. Return!");
            return;
        }

        if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled()) {
            if (!mVirtualCallStarted && toState == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                Log.i(TAG, "broadcastAudioState(): ignore audio connected if VOIP is not started");
                return;
            }

            if (device == null) {
                Log.d(TAG,"broadcastAudioState: device is null. Return!");
                return;
            }
            BluetoothDevice mAbsActivedevice =
                    mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.CALL_AUDIO);
            if (mAbsActivedevice != null) {
                Log.d(TAG,"broadcastAudioState: absActiveDevice " + mAbsActivedevice);
            }
            if (mAbsActivedevice != null && !device.equals(mAbsActivedevice)) {
                Log.d(TAG,"broadcastAudioState: non-active absolute device. Return!");
                return;
            }
            CallDevice mCallDevice = mCallDevicesMap.get(device.getAddress());
            if (mCallDevice.broadcastScoStatus == toState) {
                Log.d(TAG,"broadcastAudioState: broadcastScoStatus is same. Return!");
                return;
            }
            mCallDevice.broadcastScoStatus = toState;

            Intent intent = new Intent(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
            intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, fromState);
            intent.putExtra(BluetoothProfile.EXTRA_STATE, toState);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            mHeadsetService.sendBroadcastAsUser(intent, UserHandle.ALL,
                    BLUETOOTH_CONNECT, Utils.getTempAllowlistBroadcastOptions());
        } else {
            Intent intent = new Intent(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
            intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, fromState);
            intent.putExtra(BluetoothProfile.EXTRA_STATE, toState);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            mHeadsetService.sendBroadcastAsUser(intent, UserHandle.ALL,
                    BLUETOOTH_CONNECT, Utils.getTempAllowlistBroadcastOptions());
        }
    }

    public void onConnStateChange(BluetoothDevice device, int state, int profile, boolean isFirstMember) {
        Log.w(TAG, "onConnStateChange: state:" + state + " for device " + device + " new group: " + isFirstMember);
        if((state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING)
                        && isFirstMember) {
            StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
            BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);
            if(groupDevice != null) {
                CallDevice mCallDevice = mCallDevicesMap.get(groupDevice.getAddress());
                if(mCallDevice == null) {
                    mCallDevice = new CallDevice(groupDevice, profile, BluetoothProfile.STATE_CONNECTED);
                    mCallDevicesMap.put(groupDevice.getAddress(), mCallDevice);
                } else {
                    int profileIndex = mCallDevice.getProfileIndex(profile);
                    mCallDevice.profileConnStatus[profileIndex] = BluetoothProfile.STATE_CONNECTED;
                    mCallDevice.deviceConnStatus = state;
                }
            }
        } else if(isFirstMember && (state == BluetoothProfile.STATE_DISCONNECTING ||
                        state == BluetoothProfile.STATE_DISCONNECTED)) {
            StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
            BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);
            CallDevice mCallDevice = (groupDevice == null) ?
                                      null : mCallDevicesMap.get(groupDevice.getAddress());
            int prevState = BluetoothProfile.STATE_CONNECTED;
            Log.w(TAG, "onConnStateChange: mCallDevice: " + mCallDevice);
            if(mCallDevice != null) {
                prevState = mCallDevice.deviceConnStatus;
                int profileIndex = mCallDevice.getProfileIndex(profile);
                mCallDevice.profileConnStatus[profileIndex] = state;
                mCallDevice.deviceConnStatus = state;
                Log.w(TAG, "onConnStateChange: device: " + groupDevice + " state = " +  mCallDevice.deviceConnStatus);
            }
            ActiveDeviceManager mDeviceManager = AdapterService.getAdapterService().getActiveDeviceManager();
            mDeviceManager.onDeviceConnStateChange(groupDevice, state, prevState,
                            ApmConst.AudioFeatures.CALL_AUDIO);
            if(!device.equals(groupDevice)) {
                DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
                dMap.profileConnectionUpdate(groupDevice, ApmConst.AudioFeatures.CALL_AUDIO, profile, false);
            }
        }

        if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled() &&
                (state == BluetoothProfile.STATE_DISCONNECTING ||
                state == BluetoothProfile.STATE_DISCONNECTED)) {
            CallDevice mCallDevice = mCallDevicesMap.get(device.getAddress());
            if (isFirstMember) {
                Log.w(TAG, "onConnStateChange: CSIP group disconnecting/disconnected");
                BluetoothDevice mAbsActivedevice =
                        mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.CALL_AUDIO);
                if (device.equals(mAbsActivedevice) && isVirtualCallStarted()) {
                    mCallAudio.stopScoUsingVirtualVoiceCall();
                }

                if (mCallDevice != null && mCallDevice.pendingUpdateDiscState != -1) {
                    Log.w(TAG, "onConnStateChange: update pending disc connection state");
                    int pendingDiscState = mCallDevice.pendingUpdateDiscState;
                    mCallDevice.pendingUpdateDiscState = -1;
                    broadcastConnStateChange(device, BluetoothProfile.STATE_CONNECTED,
                            pendingDiscState);
                }

                StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
                BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);
                List<BluetoothDevice> groupMembers =
                            mStreamAudioService.getGroupMembers(groupDevice);
                if (groupMembers != null) {
                    for (BluetoothDevice memberDevice: groupMembers) {
                        CallDevice mMemberDevice = mCallDevicesMap.get(memberDevice.getAddress());
                        if(mMemberDevice != null && (!mCallDevice.equals(mMemberDevice))
                                && (mMemberDevice.pendingUpdateDiscState != -1)) {
                            Log.w(TAG, "onConnStateChange: update disc connection state for pending non-primary members");
                            int pendingMemberDiscState = mMemberDevice.pendingUpdateDiscState;
                            mMemberDevice.pendingUpdateDiscState = -1;
                            broadcastConnStateChange(memberDevice, BluetoothProfile.STATE_CONNECTED,
                                    pendingMemberDiscState);
                        }
                    }
                }
            } else {
                mCallDevice.pendingUpdateDiscState = state;
                Log.w(TAG, "onConnStateChange: ignore broadcast disc connection state for non-primary member");
            }
        }
        onConnStateChange(device, state, profile);
    }

    public void onConnStateChange(BluetoothDevice device, Integer state, Integer profile) {
        int prevState;
        Log.w(TAG, "onConnStateChange: profile: " + profile + " state: "
                                                  + state + " for device " + device);
        if(device == null)
            return;
        CallDevice mCallDevice = mCallDevicesMap.get(device.getAddress());
        StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
        BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);

        Log.w(TAG, "onConnStateChange: mCallDevice:" + mCallDevice);

        if(mCallDevice == null) {
            if (state == BluetoothProfile.STATE_DISCONNECTED)
                return;
            if (mCallDevicesMap.size() >= MAX_DEVICES) {
                Log.w(TAG, "onConnStateChange: Reached max devices.");
                return;
            }
            mCallDevice = new CallDevice(device, profile, state);
            mCallDevicesMap.put(device.getAddress(), mCallDevice);
            if (!ApmConst.getQtiLeAudioEnabled() && !isVoipLeaWarEnabled()) {
                Log.d(TAG, "onConnStateChange: Don't broadcast here for AOSP LeAudio Call profile");
            } else {
                broadcastConnStateChange(device,
                                         BluetoothProfile.STATE_DISCONNECTED, state);
            }
            return;
        }

        int profileIndex = mCallDevice.getProfileIndex(profile);
        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        prevState = mCallDevice.deviceConnStatus;
        Log.w(TAG, "onConnStateChange: prevState: " + prevState);
        mCallDevice.profileConnStatus[profileIndex] = state;

        if(state == BluetoothProfile.STATE_CONNECTED) {
            dMap.profileConnectionUpdate(device,
                                         ApmConst.AudioFeatures.CALL_AUDIO, profile, true);
            if(!device.equals(groupDevice)) {
                dMap.profileConnectionUpdate(groupDevice, ApmConst.AudioFeatures.CALL_AUDIO, profile, true);
            }
        } else if(state == BluetoothProfile.STATE_DISCONNECTED) {
            dMap.profileConnectionUpdate(device, ApmConst.AudioFeatures.CALL_AUDIO, profile, false);
            if((!device.equals(groupDevice)) && ApmConst.AudioProfiles.HFP == profile) {
                dMap.profileConnectionUpdate(groupDevice, ApmConst.AudioFeatures.CALL_AUDIO, profile, false);
            }
        }

        if (!ApmConst.getQtiLeAudioEnabled() && !isVoipLeaWarEnabled()) {
            Log.w(TAG, "AOSP LE Call profile enabled, return ");
            CCService ccService = CCService.getCCService();
            if (state == BluetoothProfile.STATE_CONNECTED ||
                state == BluetoothProfile.STATE_DISCONNECTED) {
                if (ccService != null) {
                    ccService.updateActiveProfile(device, state);
                }
            }
            return;
        }

        int otherProfileConnectionState = mCallDevice.profileConnStatus[(profileIndex+1)%2];
        Log.w(TAG, " otherProfileConnectionState: " + otherProfileConnectionState);

        if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled()) {
            if(device.equals(groupDevice)) {
                BluetoothDevice mAbsActivedevice =
                    mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.CALL_AUDIO);
                if (device.equals(mAbsActivedevice) && isVirtualCallStarted() &&
                            (state == BluetoothProfile.STATE_DISCONNECTING ||
                            state == BluetoothProfile.STATE_DISCONNECTED)) {
                    Log.w(TAG, " onConnStateChange TWM device disconnecting/disconnected");
                    mCallAudio.stopScoUsingVirtualVoiceCall();
                }
            }
            if (state == BluetoothProfile.STATE_DISCONNECTING &&
                    otherProfileConnectionState == BluetoothProfile.STATE_CONNECTING) {
                int preferredProfile = dMap.getProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
                boolean isPreferredProfile = (preferredProfile == profile);

                Log.w(TAG, "onConnStateChange Profile is disconnecting while otherProfile is connecting ");
                if (isPreferredProfile) {
                    Log.w(TAG, " Disconnecting state isPreferredProfile: " + isPreferredProfile);
                    mCallDevice.profileSwitching = true;
                    broadcastConnStateChange(device, prevState, state);

                    mCallDevice.profileConnStatus[profileIndex] = BluetoothProfile.STATE_DISCONNECTED;
                    mCallDevice.deviceConnStatus = BluetoothProfile.STATE_DISCONNECTED;
                    broadcastConnStateChange(device, state, BluetoothProfile.STATE_DISCONNECTED);
                 }
                 return;
            } else if (state == BluetoothProfile.STATE_CONNECTING &&
                    otherProfileConnectionState == BluetoothProfile.STATE_DISCONNECTING) {
                 Log.w(TAG, "onConnStateChange Profile is connecting while otherProfile is disconnecting ");
                 mCallDevice.profileSwitching = true;
                 mCallDevice.profileConnStatus[(profileIndex+1)%2] = BluetoothProfile.STATE_DISCONNECTED;
                 broadcastConnStateChange(device, prevState, BluetoothProfile.STATE_DISCONNECTED);

                 mCallDevice.profileConnStatus[profileIndex] = state;
                 mCallDevice.deviceConnStatus = state;
                 broadcastConnStateChange(device, BluetoothProfile.STATE_DISCONNECTED, state);
                 return;
            }
        }

        switch(otherProfileConnectionState) {
        /*Send Broadcast based on state of other profile*/
            case BluetoothProfile.STATE_DISCONNECTED:
                broadcastConnStateChange(device, prevState, state);
                mCallDevice.deviceConnStatus = state;
                if(state == BluetoothProfile.STATE_CONNECTED && ApmConst.getQtiLeAudioEnabled()) {
                    int supportedProfiles = dMap.getSupportedProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
                    if(profile == ApmConst.AudioProfiles.HFP &&
                            (supportedProfiles & ApmConst.AudioProfiles.BAP_CALL) == ApmConst.AudioProfiles.BAP_CALL) {
                        Log.w(TAG, "Connect LE Voice after HFP auto connect from remote");
                        StreamAudioService mStreamService = StreamAudioService.getStreamAudioService();
                        if(mStreamService != null) {
                            mStreamService.connectLeStream(groupDevice, ApmConst.AudioProfiles.BAP_CALL);
                        }
                    } else {
                        ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
                        if(mActiveDeviceManager != null) {
                            mActiveDeviceManager.setActiveDevice(device, ApmConst.AudioFeatures.CALL_AUDIO);
                        }
                    }
                }
                break;
            case BluetoothProfile.STATE_CONNECTING:
                int preferredProfile = dMap.getProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
                boolean isPreferredProfile = (preferredProfile == profile);

                Log.w(TAG, " isPreferredProfile: " + isPreferredProfile);
                if(state == BluetoothProfile.STATE_CONNECTED && isPreferredProfile) {
                    broadcastConnStateChange(device, prevState, state);
                    mCallDevice.deviceConnStatus = state;
                }
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                if(state == BluetoothProfile.STATE_CONNECTING ||
                        state == BluetoothProfile.STATE_CONNECTED) {
                    broadcastConnStateChange(device, prevState, state);
                    mCallDevice.deviceConnStatus = state;
                }
                break;
            case BluetoothProfile.STATE_CONNECTED:
                if(state == BluetoothProfile.STATE_CONNECTED) {
                    dMap.profileConnectionUpdate(device, ApmConst.AudioFeatures.CALL_AUDIO, profile, true);
                    if(prevState != state) {
                        broadcastConnStateChange(device, prevState, state);
                        mCallDevice.deviceConnStatus = state;
                    }
                    ActiveDeviceManagerService mActiveDeviceManager =
                                     ActiveDeviceManagerService.get();
                    if(mActiveDeviceManager != null) {
                        mActiveDeviceManager.setActiveDevice(device, ApmConst.AudioFeatures.CALL_AUDIO);
                    }
                } else if(state == BluetoothProfile.STATE_DISCONNECTED) {
                    if(prevState != BluetoothProfile.STATE_CONNECTED) {
                        broadcastConnStateChange(device, prevState, BluetoothProfile.STATE_CONNECTED);
                        mCallDevice.deviceConnStatus = BluetoothProfile.STATE_CONNECTED;
                    } else {
                        ActiveDeviceManagerService mActiveDeviceManager =
                                     ActiveDeviceManagerService.get();
                        if(mActiveDeviceManager != null && device.equals(mActiveDeviceManager.
                                            getActiveDevice(ApmConst.AudioFeatures.CALL_AUDIO))) {
                            mActiveDeviceManager.setActiveDevice(device, ApmConst.AudioFeatures.CALL_AUDIO);
                        }
                    }
                }
                break;
        }

        CCService ccService = CCService.getCCService();
        if (state == BluetoothProfile.STATE_CONNECTED ||
            state == BluetoothProfile.STATE_DISCONNECTED) {
            if (ccService != null) {
                ccService.updateActiveProfile(device, state);
            }
        }

        if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled()) {
            if (state == BluetoothProfile.STATE_CONNECTED && mCallDevice.profileSwitching) {
                Log.w(TAG,"onConnStateChange: send MESSAGE_UPDATE_CONNECTION_STATE for profile switch case");
                mCallDevice.profileSwitching = false;
                Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_CONNECTION_STATE);
                mHandler.sendMessageDelayed(msg, 3000);
            }
        }
    }

    public void onAudioStateChange(BluetoothDevice device, Integer state) {
        int prevStatus;
        MediaAudio sMediaAudio = MediaAudio.get();
        boolean mGroupScoConnected = false;
        if(device == null)
            return;
        CallDevice mCallDevice = mCallDevicesMap.get(device.getAddress());
        if(mCallDevice == null) {
            return;
        }

        Log.d(TAG, "onAudioStateChange: device" + device + " State: " + state);

        if(mCallDevice.scoStatus == state)
            return;

        HeadsetService service = HeadsetService.getHeadsetService();
        CCService ccService = CCService.getCCService();
        int profileID = mActiveDeviceManager.getActiveProfile(
                               ApmConst.AudioFeatures.CALL_AUDIO);
        BluetoothDevice mActivedevice = mActiveDeviceManager.getActiveDevice(
                               ApmConst.AudioFeatures.CALL_AUDIO);
        if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
            if (ccService != null) {
                if(false/*!(ccService.shouldCallAudioBeActive() || mVirtualCallStarted)*/) {
                    if(ApmConst.AudioProfiles.BAP_CALL  == profileID) {
                        StreamAudioService mStreamAudioService =
                                    StreamAudioService.getStreamAudioService();
                        if(mStreamAudioService != null) {
                            Log.w(TAG, "Call not active, disconnect stream");
                            mStreamAudioService.stopStream(mActivedevice);
                        }
                    }
                }
            }
        } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
            StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
            BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);
            if(!device.equals(groupDevice)) {
                Log.d(TAG, "onAudioStateChange: Check status of other group members");
                List<BluetoothDevice> groupMembers =
                            mStreamAudioService.getGroupMembers(groupDevice);
                if (groupMembers != null) {
                  for (BluetoothDevice memberDevice: groupMembers) {
                    CallDevice mMemberDevice = mCallDevicesMap.get(memberDevice.getAddress());
                    if(mMemberDevice != null && (!mCallDevice.equals(mMemberDevice)) &&
                            (mMemberDevice.scoStatus == BluetoothHeadset.STATE_AUDIO_CONNECTED)) {
                        mGroupScoConnected = true;
                     }
                  }

                 if(!mGroupScoConnected) {
                    for (BluetoothDevice memberDevice: groupMembers) {
                        CallDevice mMemberDevice = mCallDevicesMap.get(memberDevice.getAddress());
                        if(mMemberDevice != null && (!mCallDevice.equals(mMemberDevice))) {
                            if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled()) {
                                Log.d(TAG, "onAudioStateChange: ignore audio disconnected for AOSP LeAudio");
                            } else {
                                broadcastAudioState(memberDevice, mCallDevice.scoStatus,
                                        BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                            }
                        }
                    }
                 }
              } else {
                Log.d(TAG, "onAudioStateChange: groupMembers is null");
              }
            }
        }

        prevStatus = mCallDevice.scoStatus;
        mCallDevice.scoStatus = state;
        VolumeManager mVolumeManager = VolumeManager.get();
        mVolumeManager.updateStreamState(device, state, ApmConst.AudioFeatures.CALL_AUDIO);
        if (sMediaAudio != null) {
            //handle cache Gaming off when hang up call
            if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED && !isAudioOn()) {
                sMediaAudio.handleCacheGamingOff(device);
            }
        }
        if((!mGroupScoConnected) || state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
            if (ApmConst.getAospLeaEnabled() && isVoipLeaWarEnabled() &&
                    state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                Log.d(TAG, "onAudioStateChange: ignore audio disconnected for AOSP LeAudio");
            } else {
                broadcastAudioState(device, prevStatus, state);
            }
        }

        if(state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
            if(ApmConst.AudioProfiles.HFP != profileID) {
                if(service != null) {
                    service.getHfpA2DPSyncInterface().releaseA2DP(null);
                }
            }
          //mAudioManager.setBluetoothScoOn(false);
        } /*else {
          mAudioManager.setBluetoothScoOn(true);
        }*/
    }

    public void setAudioParam(String param) {
        mAudioManager.setParameters(param);
    }

    public void setBluetoothScoOn(boolean on) {
        mAudioManager.setBluetoothScoOn(on);
    }

    public AudioManager getAudioManager() {
        return mAudioManager;
    }

    class CallDevice {
        BluetoothDevice mDevice;
        int[] profileConnStatus = new int[2];
        int deviceConnStatus;
        int scoStatus;
        int broadcastScoStatus;
        boolean profileSwitching;
        int pendingUpdateDiscState;

        public static final int SCO_STREAM = 0;
        public static final int LE_STREAM = 1;

        CallDevice(BluetoothDevice device, int profile, int state) {
            mDevice = device;
            if(profile == ApmConst.AudioProfiles.HFP) {
                profileConnStatus[SCO_STREAM] = state;
                profileConnStatus[LE_STREAM] = BluetoothProfile.STATE_DISCONNECTED;
            } else {
                profileConnStatus[LE_STREAM] = state;
                profileConnStatus[SCO_STREAM] = BluetoothProfile.STATE_DISCONNECTED;
            }
            deviceConnStatus = state;
            scoStatus = BluetoothHeadset.STATE_AUDIO_DISCONNECTED;;
            broadcastScoStatus = BluetoothHeadset.STATE_AUDIO_DISCONNECTED;;
            profileSwitching = false;
            pendingUpdateDiscState = -1;
        }

        CallDevice(BluetoothDevice device, int profile) {
            this(device, profile, BluetoothProfile.STATE_DISCONNECTED);
        }

        public int getProfileIndex(int profile) {
            if(profile == ApmConst.AudioProfiles.HFP)
                return SCO_STREAM;
            else
                return LE_STREAM;
        }
    }
}

