/*===========================================================================
 * Copyright(c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Proprietary and Confidential.
==========================================================================*/

RideModeAudio
==============================================

This application have below limitations:
1. User need to enter this app to click disable_ridemode radio button if this app becomes
   a background application and user want to disable ride mode feature.
2. The pre-selected recording will be played if user have not disabled ridemode feature,
   user need to disable ride mode feature to restore normal calls.
   a. User explicitly (using InCallUi widgets) accept the incoming call to talk to
      the caller.
   b. User make an outgoing call which it get accepted by remote user.

Proposed implementation for sending back local customized audio:
This feature need to call Media APIs of android framework, the in-call music playback feature will handle this feature in low layer.
 • Requires signature/privileged permission android.permission.MODIFY_PHONE_STATE
 • App selects device AudioDeviceInfo.TYPE_TELEPHONY explicitly with API setPreferredDevice()

Media APIs call flow:
    AudioManager.requestAudioFocus(null,AudioManager.STREAM_MUSIC,
             AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    MediaPlayer.setDataSource(this, contentUri);
    MediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    AudioDeviceInfo telephonyDevice = getTelephonyDevice();
    MediaPlayer.setPreferredDevice(telephonyDevice);
    MediaPlayer.prepare();
    MediaPlayer.start();
