/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.conversation.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.conversation.data.Settings;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorSupportedLanguage;

import java.util.ArrayList;

public class SettingsFragment extends Fragment {
    private static final String TAG = SettingsFragment.class.getSimpleName();
    private RadioGroup mRadioGroup;

    private Spinner mSpinner;
    private RecyclerView mSettingsList;
    private SettingsViewAdapter mSettingsViewAdapter;


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        mRadioGroup = root.findViewById(R.id.radio_transcription_mode);
        mSpinner = root.findViewById(R.id.transcription_language_spinner);
        mSettingsList = root.findViewById(R.id.settings_list);
        mSettingsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSpinner.setSelection(Settings.getTranscriptionLanguage(getContext()));
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Settings.setRealtimeModeEnabled(getContext(), checkedId == R.id.radio_realtime_mode);
            }
        });
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Settings.setTranscriptionLanguage(getContext(), position);
                Log.d(TAG, "onItemSelected  position="+position);
                mSettingsViewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayList<String> arrayList = TranslatorSupportedLanguage.getSupportedLanguages();
        mSettingsViewAdapter = new SettingsViewAdapter(getContext(), arrayList);
        mSettingsList.setAdapter(mSettingsViewAdapter);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRadioGroup.check(Settings.getRealtimeModeEnabled(getContext()) ?
                R.id.radio_realtime_mode : R.id.radio_output_mode);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ");
    }

}