/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.powersavemode;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Insets;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;

import com.qualcomm.qti.powersavemode.modes.PowerSaveMode;
import com.qualcomm.qti.powersavemode.modes.PowerSaveModeManager;

import java.util.ArrayList;
import java.util.List;


public class PowerSaveModeActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final String TAG = PowerSaveModeActivity.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private ModeListAdapter mModeListAdapter;
    private Context mAppContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_save_mode);

        getWindow().getDecorView().setOnApplyWindowInsetsListener((v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, 0);
            return windowInsets;
        });

        mAppContext = getApplicationContext();
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        initializeViews();
    }

    @Override
    public boolean onNavigateUp() {
        if (!super.onNavigateUp()) {
            finish();
        }
        return true;
    }

    private void initializeViews() {
        ListView powerSaveModeList = findViewById(R.id.power_saving_modes);
        powerSaveModeList.setOnItemClickListener(this);
        mModeListAdapter = new ModeListAdapter(this);
        mModeListAdapter.setData(getPowerSaveModes());
        powerSaveModeList.setAdapter(mModeListAdapter);

        mModeListAdapter.setSelectedMode(SharedPreferenceUtils.
                getPowerSaveModeHintType(mAppContext));
        setUserSelectedMode(mModeListAdapter.getSelectPosition());
    }

    private List<PowerSaveMode> getPowerSaveModes() {
        List<PowerSaveMode> powerSaveModes = new ArrayList<>();
        final String[] powerSaveModeOptionsStrings = getResources().getStringArray(
                R.array.power_save_mode_options_strings);
        final int[] powerSaveModeOptionsValues = getResources().getIntArray(
                R.array.power_save_mode_options_values);

        for (int i = 0; i < powerSaveModeOptionsStrings.length; i++) {
            powerSaveModes.add(new PowerSaveMode(powerSaveModeOptionsValues[i],
                    powerSaveModeOptionsStrings[i]));
        }

        return powerSaveModes;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setUserSelectedMode(position);
    }

    private void setUserSelectedMode(int position) {
        PowerSaveMode powerSaveMode = mModeListAdapter.getItem(position);

        int hintType = powerSaveMode.getHintType();
        int savedHintType = SharedPreferenceUtils.getPowerSaveModeHintType(mAppContext);
        int handle = SharedPreferenceUtils.getSessionHandle(mAppContext);
        if (DEBUG) {
            Log.d(TAG, "onItemClick hintType : " + hintType + "   savedHintType = "
                    + savedHintType + " handle = " + handle);
        }

        if (hintType != savedHintType || handle == -1) {
            updateAndSaveUserOption(position, hintType);
            if (PowerSaveModeManager.getInstance().isPowerSaveModeSupport()) {
                tryTurnOnPowerSaveMode(hintType);
            }
        }
    }

    private void updateAndSaveUserOption(int position, int hintType) {
        mModeListAdapter.setSelectedPosition(position);
        SharedPreferenceUtils.savePowerSaveModeHintType(mAppContext, hintType);
    }

    private void tryTurnOnPowerSaveMode(int hint) {
        PowerSaveModeManager modeManager = PowerSaveModeManager.getInstance();

        int savedHandle = SharedPreferenceUtils.getSessionHandle(mAppContext);
        if (savedHandle != -1) {
            modeManager.turnOffPowerSaveMode(savedHandle);
        }

        int handle = modeManager.turnOnPowerSaveMode(getPackageName(), hint);
        if (handle > 0) {
            SharedPreferenceUtils.saveSessionHandle(mAppContext, handle);
        } else {
            SharedPreferenceUtils.saveSessionHandle(mAppContext, -1);
        }
    }

    private static class ModeListAdapter extends BaseAdapter {

        private final List<PowerSaveMode> mPowerSaveModes = new ArrayList<>();
        private final LayoutInflater mInflater;
        private int mSelectedPosition;

        ModeListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        void setData(List<PowerSaveMode> ltmModes) {
            mPowerSaveModes.clear();
            mPowerSaveModes.addAll(ltmModes);
            notifyDataSetChanged();
        }

        int getSelectPosition() {
            return mSelectedPosition;
        }

        void setSelectedPosition(int position) {
            mSelectedPosition = position;
            notifyDataSetChanged();
        }

        void setSelectedMode(int hintType) {
            for (int i = 0; i < mPowerSaveModes.size(); i++) {
                if (hintType == mPowerSaveModes.get(i).getHintType()) {
                    setSelectedPosition(i);
                    break;
                }
            }
        }

        @Override
        public int getCount() {
            return mPowerSaveModes.size();
        }

        @Override
        public PowerSaveMode getItem(int position) {
            return mPowerSaveModes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.power_save_mode_item, parent, false);
            }
            RadioButton modeItemName = convertView.findViewById(R.id.mode_name);
            modeItemName.setText(mPowerSaveModes.get(position).getName());

            modeItemName.setChecked(position == mSelectedPosition);
            return convertView;
        }
    }
}
