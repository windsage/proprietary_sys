/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;

import com.qualcomm.qti.sva.controller.Global;
import com.qualcomm.qti.sva.data.ISettingsModel;
import com.qualcomm.qti.sva.data.ISmModel;
import com.qualcomm.qti.sva.data.SettingsModel;
import com.qualcomm.qti.sva.utils.LogUtils;
import com.qualcomm.qti.sva.utils.Utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

public class GeneralSettingsActivity extends Activity {
    private final String TAG = GeneralSettingsActivity.class.getSimpleName();

    // view variable define
    private EditText mSm3Et1stKeyphrase;
    private EditText mSm3Et1stUser;
    private EditText mSm3Et2ndUser;
    private EditText mSm3Et2ndKeyphrase;
    private EditText mSm2Et1stKeyphrase;
    private EditText mSm2Et1stUser;
    private EditText mEtGMMTraining;
    private Spinner mSpinnerTrainingPath;
    private Spinner mSpinnerUDK7BaseModelPath;
    private Spinner mSpinnerUDK4BaseModelPath;
    private Spinner mSpinnerPDKEnrollmentRecordingTimes;
    private RelativeLayout mLayoutDetectionTone;
    private Switch mSwitchDetectionTone;
    private RelativeLayout mLayoutAdvancedDetails;
    private Switch mSwitchAdvancedDetails;
    private ISettingsModel mSettingModel;
    private static final int MAX_TRAINING_TIMES = 5;
    private static final int MIN_TRAINING_TIMES = 3;

    private static final int MAX_GMM_TRAINNING_LEVEL = 100;
    private static final int MIN_GMM_TRAINNING_LEVEL = 0;
    private static final int[] maps = {
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.UNPROCESSED};
    private static String[] TRAINING_TIMES_OPTIONS = {
            Integer.toString(MIN_TRAINING_TIMES),
            Integer.toString(MAX_TRAINING_TIMES)
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingModel = new SettingsModel(getApplicationContext(), null);
        setContentView(R.layout.activity_general_settings);
        Utils.setUpEdgeToEdge(this);
        initializeUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveTrainingPath();
        saveUDKBaseModel();
        saveRecordingTimes();
    }
    private void saveTrainingPath() {
        TrainingPathSpinnerItem item = (TrainingPathSpinnerItem)
                (mSpinnerTrainingPath.getSelectedItem());
        mSettingModel.setGlobalTrainingPath(item.getValue());
    }
    private void saveUDKBaseModel() {
        String udk4_name = (String)
                (mSpinnerUDK4BaseModelPath.getSelectedItem());
        mSettingModel.setUDK4BaseSoundModel(udk4_name);
        String udk7_name = (String)
                (mSpinnerUDK7BaseModelPath.getSelectedItem());
        mSettingModel.setUDK7BaseSoundModel(udk7_name);
    }

    private void saveRecordingTimes() {
        String recordingTimes = (String) mSpinnerPDKEnrollmentRecordingTimes.getSelectedItem();
        mSettingModel.setPDKEnrollmentRecordingTimes(Integer.parseInt(recordingTimes));
    }
    private void initializeUI() {
        //sound model 3.0 global settings
        mSm3Et1stKeyphrase = findViewById(R.id.sm3_edit_first_stage_keyphrase);
        mSm3Et1stKeyphrase.setText(String.valueOf(
                mSettingModel.getGlobal1stKeyphraseConfidenceLevel(ISmModel.ModelVersion.VERSION_3_0)));
        mSm3Et1stKeyphrase.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mSettingModel.setGlobal1stKeyphraseConfidenceLevel(
                            ISmModel.ModelVersion.VERSION_3_0,
                            Integer.valueOf(s.toString()));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        mSm3Et2ndKeyphrase = findViewById(R.id.sm3_edit_second_stage_keyphrase);
        mSm3Et2ndKeyphrase.setText(String.valueOf(
                mSettingModel.getGlobal2ndKeyphraseConfidenceLevel(ISmModel.ModelVersion.VERSION_3_0)));
        mSm3Et2ndKeyphrase.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mSettingModel.setGlobal2ndKeyphraseConfidenceLevel(
                            ISmModel.ModelVersion.VERSION_3_0,
                            Integer.valueOf(s.toString()));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        mSm3Et1stUser = findViewById(R.id.sm3_edit_first_stage_user);
        mSm3Et1stUser.setText(String.valueOf(
                mSettingModel.getGlobal1stUserConfidenceLevel(ISmModel.ModelVersion.VERSION_3_0)));
        mSm3Et1stUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mSettingModel.setGlobal1stUserConfidenceLevel(
                            ISmModel.ModelVersion.VERSION_3_0,
                            Integer.valueOf(s.toString()));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        mSm3Et2ndUser = findViewById(R.id.sm3_edit_second_stage_user);
        mSm3Et2ndUser.setText(String.valueOf(
                mSettingModel.getGlobal2ndUserConfidenceLevel(ISmModel.ModelVersion.VERSION_3_0)));
        mSm3Et2ndUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mSettingModel.setGlobal2ndUserConfidenceLevel(
                            ISmModel.ModelVersion.VERSION_3_0,
                            Integer.valueOf(s.toString()));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        //sound model 2.0 global settings
        mSm2Et1stKeyphrase = findViewById(R.id.sm2_edit_first_stage_keyphrase);
        mSm2Et1stKeyphrase.setText(String.valueOf(
                mSettingModel.getGlobal1stKeyphraseConfidenceLevel(ISmModel.ModelVersion.VERSION_2_0)));
        mSm2Et1stKeyphrase.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mSettingModel.setGlobal1stKeyphraseConfidenceLevel(
                            ISmModel.ModelVersion.VERSION_2_0,
                            Integer.valueOf(s.toString()));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        mSm2Et1stUser = findViewById(R.id.sm2_edit_first_stage_user);
        mSm2Et1stUser.setText(String.valueOf(
                mSettingModel.getGlobal1stUserConfidenceLevel(ISmModel.ModelVersion.VERSION_2_0)));
        mSm2Et1stUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mSettingModel.setGlobal1stUserConfidenceLevel(
                            ISmModel.ModelVersion.VERSION_2_0,
                            Integer.valueOf(s.toString()));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        //global gmm training settings.
        mEtGMMTraining = findViewById(R.id.edit_gmm_training);
        mEtGMMTraining.setText(String.valueOf(mSettingModel.getGlobalGMMTrainingConfidenceLevel()));
        mEtGMMTraining.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String editString = s.toString();
                    int level = Integer.valueOf(editString);
                    if (level > MAX_GMM_TRAINNING_LEVEL || level < MIN_GMM_TRAINNING_LEVEL
                            || !String.valueOf(level).equals(editString)) {
                        if (level > MAX_GMM_TRAINNING_LEVEL) {
                            level = MAX_GMM_TRAINNING_LEVEL;
                        } else if (level < MIN_GMM_TRAINNING_LEVEL){
                            level = MIN_GMM_TRAINNING_LEVEL;
                        }
                        mEtGMMTraining.setText(String.valueOf(level));
                    }
                    mSettingModel.setGlobalGMMTrainingConfidenceLevel(level);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        // training audio path
        mSpinnerTrainingPath = findViewById(R.id.spinner_training_path);
        String[] optionItems = getResources().getStringArray(
                R.array.training_path_entry_value);
        ArrayList<TrainingPathSpinnerItem> trainingPathOptions = new ArrayList<>();
        for (int i = 0; i < optionItems.length; i++) {
            trainingPathOptions.add(new TrainingPathSpinnerItem(optionItems[i]));
        }
        ArrayAdapter<TrainingPathSpinnerItem> voiceRequestArrayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                trainingPathOptions);
        voiceRequestArrayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mSpinnerTrainingPath.setAdapter(voiceRequestArrayAdapter);
        mSpinnerTrainingPath.setSelection(getItemPosition(optionItems,
                mSettingModel.getGlobalTrainingPath()));

        // UDK4.O base model path
        String[] udkBaseModelItems = filterUDKBaseModel();
        ArrayList<String> udkBaseModelOptions = new ArrayList<>(Arrays.asList(udkBaseModelItems));

        mSpinnerUDK4BaseModelPath = findViewById(R.id.spinner_udk4_base_model_path);
        ArrayAdapter<String> udk4BaseModelArrayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                udkBaseModelOptions);
        udk4BaseModelArrayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mSpinnerUDK4BaseModelPath.setAdapter(udk4BaseModelArrayAdapter);
        int udk4_index;
        for(udk4_index = 0; udk4_index < udkBaseModelItems.length; udk4_index++){
            if(udkBaseModelItems[udk4_index].equalsIgnoreCase(
                            mSettingModel.getUDK4BaseSoundModel())){
                break;
            }
        }
        mSpinnerUDK4BaseModelPath.setSelection(udk4_index);

        // UDK7.O base model path
        mSpinnerUDK7BaseModelPath = findViewById(R.id.spinner_udk7_base_model_path);
        ArrayAdapter<String> udk7baseModelArrayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                udkBaseModelOptions);
        udk7baseModelArrayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mSpinnerUDK7BaseModelPath.setAdapter(udk7baseModelArrayAdapter);
        int udk7_index;
        for(udk7_index = 0; udk7_index < udkBaseModelItems.length; udk7_index++){
            if(udkBaseModelItems[udk7_index].equalsIgnoreCase(
                            mSettingModel.getUDK7BaseSoundModel())){
                break;
            }
        }
        mSpinnerUDK7BaseModelPath.setSelection(udk7_index);

        // PDK enrollment recording times
        ArrayList<String> enrollmentOptions = new ArrayList<>(Arrays.asList(TRAINING_TIMES_OPTIONS));
        mSpinnerPDKEnrollmentRecordingTimes = findViewById(R.id.spinner_pdk_recording_times);
        ArrayAdapter<String> trainingTimesArrayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                enrollmentOptions);
        trainingTimesArrayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mSpinnerPDKEnrollmentRecordingTimes.setAdapter(trainingTimesArrayAdapter);
        LogUtils.d(TAG, "setSelection = " + getPDKEnrollmentTimesItemPosition(
                mSettingModel.getPDKEnrollmentRecordingTimes()));
        mSpinnerPDKEnrollmentRecordingTimes.setSelection(getPDKEnrollmentTimesItemPosition(
                mSettingModel.getPDKEnrollmentRecordingTimes()));

        mSwitchDetectionTone = findViewById(R.id.switch_detection_tone);
        mSwitchDetectionTone.setChecked(mSettingModel.getGlobalDetectionToneEnabled());
        mLayoutDetectionTone = findViewById(R.id.layout_detection_tone);
        mLayoutDetectionTone.setTag(mSwitchDetectionTone);
        mLayoutDetectionTone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Switch switchDetectionTone = (Switch) view.getTag();
                boolean bChecked = !switchDetectionTone.isChecked();
                switchDetectionTone.setChecked(bChecked);
                mSettingModel.setGlobalDetectionToneEnabled(bChecked);
                LogUtils.d(TAG, "onClick: detection tone bChecked = " + bChecked);
            }
        });

        mSwitchAdvancedDetails = findViewById(R.id.switch_advanced_details);
        mSwitchAdvancedDetails.setChecked(mSettingModel.getGlobalIsDisplayAdvancedDetails());
        mLayoutAdvancedDetails = findViewById(R.id.layout_advanced_details);
        mLayoutAdvancedDetails.setTag(mSwitchAdvancedDetails);
        mLayoutAdvancedDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Switch switchAdvancedDetails = (Switch) view.getTag();
                boolean bChecked = !switchAdvancedDetails.isChecked();
                switchAdvancedDetails.setChecked(bChecked);
                mSettingModel.setGlobalIsDisplayAdvancedDetails(bChecked);
                LogUtils.d(TAG, "onClick: advanced details bChecked = " + bChecked);
            }
        });
    }

    private class TrainingPathSpinnerItem {
        private int mValue;
        private String mPath;

        TrainingPathSpinnerItem(String path) {
            mPath = path;
            if (mPath.equalsIgnoreCase("MIC")){
                mValue = MediaRecorder.AudioSource.MIC;
            }else if (mPath.equalsIgnoreCase("UNPROCESSED")){
                mValue = MediaRecorder.AudioSource.UNPROCESSED;
            }else{
                mValue = MediaRecorder.AudioSource.VOICE_RECOGNITION;
            }
        }

        @Override
        public String toString() {
            return "" + mPath;
        }

        public int getValue() {
            return mValue;
        }
    }
    private int getItemPosition(String[] options, int selected) {
        int i;
        for(i = 0; i < maps.length; i++){
            if(maps[i] == selected) break;
        }
        LogUtils.d(TAG, "getItemPosition: selected = " + selected + " item = " + i);
        if(i >= maps.length){
            return 1;//VOICE_RECOGNITION
        }else{
            return i;
        }
    }

    private String[] filterUDKBaseModel() {

        File bsmDir = new File(Global.PATH_ROOT);
        if (null != bsmDir && bsmDir.exists()) {
            File[] files = bsmDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String fileName) {
                    if (fileName.endsWith(ISmModel.SUFFIX_MERGED_LANGUAGE_MODEL)) {
                        return true;
                    }
                    return false;
                }
            });

            if (null == files || 0 == files.length) {
                LogUtils.e(TAG, "getTrainingBaseModel: no base sound model file found");
                return null;
            }

            String[] names = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                names[i] = files[i].getName();
            }
            return names;
        }
        LogUtils.e(TAG, "getTrainingBaseModel: base sound model folder not found");
        return null;
    }

    private int getPDKEnrollmentTimesItemPosition(int selected) {
        String times = Integer.toString(selected);
        for (int i = 0; i < TRAINING_TIMES_OPTIONS.length; i++) {
            if (TRAINING_TIMES_OPTIONS[i].equals(times)) {
                return i;
            }
        }
        LogUtils.e(TAG, "getPDKEnrollmentTimesItemPosition error: selected item not found");
        return 0;
    }
}
