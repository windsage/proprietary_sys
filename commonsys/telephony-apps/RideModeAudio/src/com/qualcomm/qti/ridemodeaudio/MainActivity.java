/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.ridemodeaudio;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.PreciseCallState;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.qualcomm.qti.ridemodeaudio.R;

public class MainActivity extends AppCompatActivity {

    private static final boolean DEFAULT_RADIO_BUTTON_ENABLED = false;
    private static final String LOG_TAG = "RideModeAudio";
    private static final String SHARED_PREFERENCE_NAME ="radio_button_enabled";
    private static final String KEY_RADIO_BUTTON_ENABLED = "key_radio_button_enabled";

    private static final Uri FILE_BASE_URI = Uri.parse("content://media/external/audio/media");
    private static final String[] RUNTIME_PERMISSIONS = {Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_MEDIA_AUDIO};

    private static final int ANTITHEFT_VERIFIER_PERMISSION_REQUEST = 99;

    private AudioManager mAudioManager;
    private ContentResolver mContentResolver;
    private List<Audio> mAudioList;
    private MediaPlayer mMediaPlayer;
    private PreciseCallStateListener mPreciseCallStateListener;
    private RadioGroup mRadioGroup;
    private RadioButton mEnableRadio, mDisableRadio;
    private RecyclerAdapter mRecyclerAdapter;
    private RecyclerView mRecyclerView;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mBaseTelephonyManager;

    Map<Integer, PreciseCallStateListener> mTelephonyCallbacks;
    SharedPreferences mSharedPref;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
        int[] grantResults) {

        boolean granted = false;

        if (requestCode == ANTITHEFT_VERIFIER_PERMISSION_REQUEST) {
            for (String str : permissions) {
                Log.d(LOG_TAG, "request Permission :." + str);
            }

            /** Check all permission granted */
            if (grantResults.length > 0) {
                granted = true;

                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        granted = false;
                        break;
                    }
                }
            }

            if (granted) {
                continueCreate();
                return;
            } else {
                Log.d(LOG_TAG, "Permission is not granted.");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBaseTelephonyManager = getSystemService(TelephonyManager.class);
        mSubscriptionManager = getSystemService(SubscriptionManager.class);
        mContentResolver = getContentResolver();
        mAudioManager = getSystemService(AudioManager.class);
        mTelephonyCallbacks = new TreeMap<>();

        for (String runtimePermission : RUNTIME_PERMISSIONS) {
            Log.d(LOG_TAG, "Checking permissions for: " + runtimePermission);
            if (checkSelfPermission(runtimePermission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(RUNTIME_PERMISSIONS, ANTITHEFT_VERIFIER_PERMISSION_REQUEST);
                return;
            }
        }
        continueCreate();
    }

    private void continueCreate() {
        Log.d(LOG_TAG, "continueCreated.");
        setupEdgeToEdge(this);
        setContentView(R.layout.activity_main);

        mRadioGroup = (RadioGroup)findViewById(R.id.radio_group);
        mEnableRadio = (RadioButton)findViewById(R.id.enable_ridemode);
        mDisableRadio = (RadioButton)findViewById(R.id.disable_ridemode);
        mSharedPref = this.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        mRadioGroup.setOnCheckedChangeListener(new RideModeRadioButtonListener());

        retrieveAvailableAudio();
        initView();
    }

    private void saveRadioButtonCheckedState (boolean isChecked) {
        //record radio button checked state to SP
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(KEY_RADIO_BUTTON_ENABLED, isChecked);
        editor.apply();
    }

    class RideModeRadioButtonListener implements OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == R.id.enable_ridemode) {
                updatePhoneListeners(true);
                saveRadioButtonCheckedState(true);
            } else if (checkedId == R.id.disable_ridemode) {
                updatePhoneListeners(false);
                saveRadioButtonCheckedState(false);
            }
        }
    };

    /**
     * retrieve available audio name and uris from Media database
     * and set to audiolist
     */
    private void retrieveAvailableAudio() {
        Cursor cursor = getFolderCursor(mContentResolver);
        if (cursor == null) {
            return;
        }

        mAudioList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                String displayName = cursor.getString(cursor.getColumnIndex(
                        MediaStore.Audio.Media.DISPLAY_NAME));
                String uriString = FILE_BASE_URI + "/" + id;
                Log.d(LOG_TAG, "audio name is: " + displayName +
                        " uriString is: " + uriString);
                mAudioList.add(new Audio(displayName, uriString));
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Initialize the UI with list of available audio resources on the device.
     */
    private void initView() {
        mRecyclerView = findViewById(R.id.audio_item);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        if (!mAudioList.isEmpty())  {
            mRecyclerAdapter = new RecyclerAdapter(mAudioList);
            mRecyclerView.setAdapter(mRecyclerAdapter);
        }

        //get enabled radio button state from SP
        boolean enabled = mSharedPref.getBoolean(KEY_RADIO_BUTTON_ENABLED,
                DEFAULT_RADIO_BUTTON_ENABLED);
        if (enabled) {
            mEnableRadio.setChecked(true);
        } else {
            mDisableRadio.setChecked(true);
        }
    }

    private Cursor getFolderCursor(ContentResolver resolver) {
        String[] projection = {MediaStore.Files.FileColumns._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_MODIFIED};
        String selection = MediaStore.Audio.Media.ALBUM + " = 'Music'";
        return query(resolver, FILE_BASE_URI, projection, selection.toString(),null, null);
    }

    private Cursor query(ContentResolver resolver, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        try {
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private AudioDeviceInfo getTelephonyDevice() {
        AudioDeviceInfo[] deviceList = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device: deviceList) {
            if (device.getType() == AudioDeviceInfo.TYPE_TELEPHONY) {
                return device;
            }
        }
        return null;
    }

    private void playRecording() {
        mMediaPlayer = new MediaPlayer();
        try {
            mAudioManager.requestAudioFocus(null,AudioManager.STREAM_MUSIC,
                  AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            Uri contentUri = Uri.parse(mRecyclerAdapter.mUriString);
            Log.d(LOG_TAG, "playRecording uriString is: " + contentUri);

            mMediaPlayer.setDataSource(this, contentUri);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        AudioDeviceInfo telephonyDevice = getTelephonyDevice();
            mMediaPlayer.setPreferredDevice(telephonyDevice);
            mMediaPlayer.prepare();
            mMediaPlayer.start();

        } catch (IOException e) {
            Log.w(LOG_TAG, "Unable to play track");
        } catch (NullPointerException e) {
            Log.w(LOG_TAG, "The mSelectedUri is invalid");
        }
    }

    private class PreciseCallStateListener extends TelephonyCallback
            implements TelephonyCallback.PreciseCallStateListener {

        @Override
        public void onPreciseCallStateChanged(PreciseCallState preciseCallState) {
            Log.d(LOG_TAG, "call state is:" + preciseCallState.getForegroundCallState());
            if (preciseCallState.getForegroundCallState() ==
                    preciseCallState.PRECISE_CALL_STATE_ACTIVE) {
                playRecording();
            }
        }
    }

    /**
     * Enable/Disable ride mode feature.
     */
    private void updatePhoneListeners(boolean enabled) {
        final int numPhones = mBaseTelephonyManager.getActiveModemCount();
        for (int i = 0; i < numPhones; i++) {
            final SubscriptionInfo subInfo =
            mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i);
            if (subInfo != null) {
                final int subId = subInfo.getSubscriptionId();
                TelephonyManager mgr = mBaseTelephonyManager.createForSubscriptionId(subId);
                if (mgr != null )  {
                    if (enabled) {
                        //enable ride mode
                        PreciseCallStateListener telephonyCallback = new PreciseCallStateListener();
                        mTelephonyCallbacks.put(Integer.valueOf(subId), telephonyCallback);
                        mgr.registerTelephonyCallback(this.getMainExecutor(),
                        mTelephonyCallbacks.get(subId));
                    } else {
                        //disable ride mode
                        if (mTelephonyCallbacks != null && mTelephonyCallbacks.size() > 0){
                            mgr.unregisterTelephonyCallback(mTelephonyCallbacks.get(subId));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        super.onDestroy();
        updatePhoneListeners(false);
    }

    private void setupEdgeToEdge(@NonNull Activity activity) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                                    | WindowInsetsCompat.Type.displayCutout());
                    // Apply the insets paddings to the view.
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    // Return CONSUMED if you don't want the window insets to keep being
                    // passed down to descendant views.
                    return WindowInsetsCompat.CONSUMED;
                });
    }
}
