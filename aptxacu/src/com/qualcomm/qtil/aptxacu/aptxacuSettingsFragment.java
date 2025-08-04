/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxacu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class aptxacuSettingsFragment extends PreferenceFragmentCompat
    implements OnSharedPreferenceChangeListener, aptxacuApplication.OnStateChangedListener {
  private static final String TAG = "aptxacuSettingsFragment";
  private static final boolean DBG = false;

  private aptxacuApplication mApp = null;

  private Preference mAudioProfilePreferenceList;
  private ListPreference mAptxAndAptxHdPriority;
  private ListPreference mAptxAdaptive96KHzSampleRate;
  private ListPreference mAudioProfileOverride;

  private Context mContext = null;
  private boolean mResumed = false;

  private static final String PREF_CAT_APTX_AND_APTX_HD = "pref_cat_aptx_and_aptx_hd";
  private static final String PREF_CAT_APTX_ADAPTIVE = "pref_cat_aptx_adaptive";
  private static final String PREF_CAT_CODEC_AUDIO_PROFILE = "pref_cat_codec_audio_profile";

  private static final int EVENT_APP_STATE_CHANGED = 1;
  private static final int EVENT_ACU_APTX_AND_APTX_HD_PRIORITY = 2;
  private static final int EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE = 3;
  private static final int EVENT_ACU_AUDIO_PROFILE_OVERRIDE = 4;

  private final aptxacuSettingsHandler mHandler = new aptxacuSettingsHandler(this);

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences);
    if (DBG) Log.d(TAG, "onCreatePreferences");

    mContext = getActivity().getApplicationContext();
    mApp = (aptxacuApplication) mContext;
    if (mApp == null) {
      Log.e(TAG, "unable to get app");
      return;
    }

    if (mHandler == null) {
      Log.e(TAG, "unable to create handler");
      return;
    }

    // PreferenceCategory aptX and aptX HD
    PreferenceCategory pref_cat_aptx_and_aptx_hd =
        (PreferenceCategory) findPreference(PREF_CAT_APTX_AND_APTX_HD);
    String pref_cat_aptx_and_aptx_hd_str = mContext.getString(R.string.pref_cat_aptx_and_aptx_hd);
    pref_cat_aptx_and_aptx_hd.setTitle(pref_cat_aptx_and_aptx_hd_str);
    pref_cat_aptx_and_aptx_hd.setIconSpaceReserved(false);

    // Set aptX and aptX HD priority preference
    mAptxAndAptxHdPriority = findPreference(aptxacuALSDefs.APTX_AND_APTX_HD_PRIORITY);
    String aptx_and_aptx_hd_priority_title_str =
        mContext.getString(R.string.aptx_and_aptx_hd_priority_title);
    mAptxAndAptxHdPriority.setTitle(aptx_and_aptx_hd_priority_title_str);
    mAptxAndAptxHdPriority.setIconSpaceReserved(false);
    mAptxAndAptxHdPriority.setSingleLineTitle(false);
    mAptxAndAptxHdPriority.setPersistent(true);

    // PreferenceCategory aptX Adaptive
    PreferenceCategory pref_cat_aptx_adaptive =
        (PreferenceCategory) findPreference(PREF_CAT_APTX_ADAPTIVE);
    String pref_cat_aptx_adaptive_str = mContext.getString(R.string.pref_cat_aptx_adaptive);
    pref_cat_aptx_adaptive.setTitle(pref_cat_aptx_adaptive_str);
    pref_cat_aptx_adaptive.setIconSpaceReserved(false);

    // Set aptX Adaptive 96KHz sample rate preference
    mAptxAdaptive96KHzSampleRate = findPreference(aptxacuALSDefs.APTX_ADAPTIVE_96KHZ_SAMPLE_RATE);
    String aptx_adaptive_96khz_sample_rate_title_str =
        mContext.getString(R.string.aptx_adaptive_96khz_sample_rate_title);
    mAptxAdaptive96KHzSampleRate.setTitle(aptx_adaptive_96khz_sample_rate_title_str);
    mAptxAdaptive96KHzSampleRate.setIconSpaceReserved(false);
    mAptxAdaptive96KHzSampleRate.setSingleLineTitle(false);
    mAptxAdaptive96KHzSampleRate.setPersistent(true);

    // Codec audio profile
    PreferenceCategory pref_cat_codec_audio_profile =
        (PreferenceCategory) findPreference(PREF_CAT_CODEC_AUDIO_PROFILE);
    String pref_cat_codec_audio_profile_str =
        mContext.getString(R.string.pref_cat_codec_audio_profile);
    pref_cat_codec_audio_profile.setTitle(pref_cat_codec_audio_profile_str);
    pref_cat_codec_audio_profile.setIconSpaceReserved(false);

    // Set audio profile override preference
    mAudioProfileOverride = findPreference(aptxacuALSDefs.AUDIO_PROFILE_OVERRIDE);
    String audio_profile_override_title_str =
        mContext.getString(R.string.audio_profile_override_title);
    mAudioProfileOverride.setTitle(audio_profile_override_title_str);
    mAudioProfileOverride.setIconSpaceReserved(false);
    mAudioProfileOverride.setSingleLineTitle(false);
    mAudioProfileOverride.setPersistent(true);

    // App audio profile preference list
    PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("pref_screen");
    mAudioProfilePreferenceList = findPreference("pref_audio_profile_preference");
    mAudioProfilePreferenceList.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            Intent intent = new Intent(mContext, aptxacuProfilePreferenceListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
          }
        });

    getPreferenceManager().setDefaultValues(mContext, R.xml.preferences, false);
    update();
  }

  @Override
  public void onResume() {
    if (DBG) Log.d(TAG, "onResume");
    mApp.registerOnStateChangedListener(this);
    super.onResume();
    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    mResumed = true;
    update();
  }

  @Override
  public void onPause() {
    if (DBG) Log.d(TAG, "onPause");
    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    mApp.unregisterOnStateChangedListener(this);
    mResumed = false;
    super.onPause();
  }

  @Override
  public void onStart() {
    super.onStart();
    if (DBG) Log.d(TAG, "onStart");
  }

  @Override
  public void onStop() {
    super.onStop();
    if (DBG) Log.d(TAG, "onStop");
  }

  @Override
  public void onStateChanged(aptxacuApplication app) {
    if (DBG) Log.d(TAG, "onStateChanged");
    Message msg = mHandler.obtainMessage(EVENT_APP_STATE_CHANGED);
    mHandler.sendMessage(msg);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (DBG) Log.d(TAG, "onSharedPreferenceChanged");
    Message msg = null;
    if (key.equalsIgnoreCase(aptxacuALSDefs.APTX_AND_APTX_HD_PRIORITY)) {
      msg = mHandler.obtainMessage(EVENT_ACU_APTX_AND_APTX_HD_PRIORITY);
    } else if (key.equalsIgnoreCase(aptxacuALSDefs.APTX_ADAPTIVE_96KHZ_SAMPLE_RATE)) {
      msg = mHandler.obtainMessage(EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE);
    } else if (key.equalsIgnoreCase(aptxacuALSDefs.AUDIO_PROFILE_OVERRIDE)) {
      msg = mHandler.obtainMessage(EVENT_ACU_AUDIO_PROFILE_OVERRIDE);
      update();
    }
    msg.obj = key;
    mHandler.sendMessage(msg);
  }

  // Update preference values
  private void update() {
    if (mResumed == false) return;

    String AptxAndAptxHdPriorityVal = mApp.GetAptxAndAptxHdPriority();
    if (mAptxAndAptxHdPriority.getValue() == null) {
      mAptxAndAptxHdPriority.setValueIndex(0);
    }

    if (AptxAndAptxHdPriorityVal.equalsIgnoreCase("DEFAULT")) {
      mAptxAndAptxHdPriority.setValueIndex(0);
    } else if (AptxAndAptxHdPriorityVal.equalsIgnoreCase("APTX")) {
      mAptxAndAptxHdPriority.setValueIndex(1);
    } else if (AptxAndAptxHdPriorityVal.equalsIgnoreCase("APTX_HD")) {
      mAptxAndAptxHdPriority.setValueIndex(2);
    }

    String AptxAdaptive96KHzSampleRateVal = mApp.GetAptxAdaptive96KHzSampleRate();
    if (mAptxAdaptive96KHzSampleRate.getValue() == null) {
      mAptxAdaptive96KHzSampleRate.setValueIndex(1);
    }

    if (AptxAdaptive96KHzSampleRateVal.equalsIgnoreCase("ON")) {
      mAptxAdaptive96KHzSampleRate.setValueIndex(0);
    } else if (AptxAdaptive96KHzSampleRateVal.equalsIgnoreCase("OFF")) {
      mAptxAdaptive96KHzSampleRate.setValueIndex(1);
    }

    String AudioProfileOverrideVal = mApp.GetAudioProfileOverride();
    if (mAudioProfileOverride.getValue() == null) {
      mAudioProfileOverride.setValueIndex(0);
    }

    if (AudioProfileOverrideVal.equalsIgnoreCase("AUTO_ADJUST")) {
      mAudioProfileOverride.setValueIndex(0);
      // Enable app audio profile preference list button
      mAudioProfilePreferenceList.setEnabled(true);
    } else if (AudioProfileOverrideVal.equalsIgnoreCase("HIGH_QUALITY")) {
      mAudioProfileOverride.setValueIndex(1);
      // Disable app audio profile preference list button
      mAudioProfilePreferenceList.setEnabled(false);
    } else if (AudioProfileOverrideVal.equalsIgnoreCase("GAMING_MODE")) {
      mAudioProfileOverride.setValueIndex(2);
      // Disable app audio profile preference list button
      mAudioProfilePreferenceList.setEnabled(false);
    }
  }

  private class aptxacuSettingsHandler extends Handler {
    private aptxacuSettingsFragment mSettingsFragment = null;

    public aptxacuSettingsHandler(aptxacuSettingsFragment settingsFragment) {
      mSettingsFragment = settingsFragment;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case EVENT_APP_STATE_CHANGED:
          mSettingsFragment.update();
          break;

        case EVENT_ACU_APTX_AND_APTX_HD_PRIORITY:
          mApp.acuAptxAndAptxHdPriority();
          break;

        case EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE:
          mApp.acuAptxAdaptive96KHzSampleRate();
          break;

        case EVENT_ACU_AUDIO_PROFILE_OVERRIDE:
          mApp.acuAudioProfileOverride();
          break;

        default:
          break;
      }
    }
  }
}
