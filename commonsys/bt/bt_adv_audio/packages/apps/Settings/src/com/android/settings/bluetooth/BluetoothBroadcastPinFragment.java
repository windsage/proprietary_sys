/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.android.settings.bluetooth;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.DialogFragment;

import android.app.settings.SettingsEnums;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.bluetooth.BluetoothBroadcastEnableController;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.BroadcastProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog fragment for renaming a Bluetooth device.
 */
public class BluetoothBroadcastPinFragment extends InstrumentedDialogFragment
        implements RadioGroup.OnCheckedChangeListener {

    public static BluetoothBroadcastPinFragment newInstance() {
        Log.d(TAG, "newInstance");
        BluetoothBroadcastPinFragment frag = new BluetoothBroadcastPinFragment();
        return frag;
    }

    public static final String TAG = "BluetoothBroadcastPinFragment";

    private Context mContext;
    @VisibleForTesting
    AlertDialog mAlertDialog = null;
    private Dialog mDialog = null;
    private Button mOkButton = null;
    private TextView mCurrentPinView;

    private String mCurrentPin = "4308";
    private int mUserSelectedPinConfiguration = -1;
    private int mUserSelectedPinConfigurationIndex = -1;
    private int mCurrentPinConfiguration = -1;

    private List<Integer> mRadioButtonIds = new ArrayList<>();
    private List<String> mRadioButtonStrings = new ArrayList<>();

    private LocalBluetoothManager mManager;
    private LocalBluetoothProfileManager mProfileManager;
    private BroadcastProfile mBapProfile;

    private int getDialogTitle() {
       return R.string.bluetooth_broadcast_pin_configure_dialog;
    }

    private void updatePinConfiguration() {
        Log.d(TAG, "updatePinConfiguration with " + Integer.toString(mUserSelectedPinConfiguration));
        if (mUserSelectedPinConfiguration == -1 || mUserSelectedPinConfigurationIndex ==
                mCurrentPinConfiguration) {
          Log.e(TAG, "mUserSelectedPinConfigurationIndex: " + mUserSelectedPinConfigurationIndex +
                    "mCurrentPinConfiguration: " + mCurrentPinConfiguration);
          return;
        }
        // Call lower layer to generate new pin
        if (mUserSelectedPinConfiguration != 0)
           mBapProfile.setEncryption(true, mUserSelectedPinConfiguration, false);
        else
           mBapProfile.setEncryption(false, mUserSelectedPinConfiguration, false);
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");
        super.onAttach(context);
        mContext = context;
        mManager = Utils.getLocalBtManager(mContext);
        mProfileManager = mManager.getProfileManager();
        mBapProfile = (BroadcastProfile) mProfileManager.getBroadcastProfile();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        //Dialog mDialog = onCreateDialog(new Bundle());
        //this.show(this.getActivity().getSupportFragmentManager(), "PinFragment");
    }

    /*
    public void show() {
        Log.e(TAG, "show");
        this.show(this.getActivity().getSupportFragmentManager(), "PinFragment");
    }
    */

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //String deviceName = getDeviceName();
        Log.d(TAG, "onCreateDialog - enter");
        if (savedInstanceState != null) {
            Log.e(TAG, "savedInstanceState != null");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(getDialogTitle())
                .setView(createDialogView())
                .setPositiveButton(R.string.okay, (dialog, which) -> {
                    //setDeviceName(mDeviceNameView.getText().toString().trim());
                    updatePinConfiguration();
                })
                .setNegativeButton(android.R.string.cancel, null);
        mAlertDialog = builder.create();
        Log.d(TAG, "onCreateDialog - exit");
        return mAlertDialog;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_FRAGMENT;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
    }

    private int getRadioButtonGroupId() {
        return R.id.bluetooth_broadcast_pin_config_radio_group;
    }

    private void setCurrentPin(String pin) {
        mCurrentPin = pin;
    }

    private String getCurrentPin() {
        return mCurrentPin;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Log.d(TAG, "Index changed to " + checkedId);
        // radioButton = (RadioButton) view.findViewById(checkedId);
        int index = mRadioButtonIds.indexOf(checkedId);
        Log.d(TAG, "index: " + index);
        String[] stringArrayValues = getContext().getResources().getStringArray(
                R.array.bluetooth_broadcast_pin_config_values);
        mUserSelectedPinConfiguration = Integer.parseInt(stringArrayValues[index]);
        mUserSelectedPinConfigurationIndex = index;
        Log.d(TAG, "Selected Pin Configuration " + Integer.toString(mUserSelectedPinConfiguration));
    }

    private int getLength(byte[] bytes) {
        if(bytes == null || bytes.length != 16) {
            return 0;
        }
        int i;
        for (i = 0; i < 16; i++) {
            if (bytes[15 - i] == 0) {
                break;
            }
        }
        Log.d(TAG, "getLength: " + i);
        return i;
    }

    private int toIndex(int len) {
        String[] stringArrayValues = getContext().getResources().getStringArray(
                R.array.bluetooth_broadcast_pin_config_values);
        for (int index = stringArrayValues.length - 1; index >= 0; index--) {
            if (len == Integer.parseInt(stringArrayValues[index])) {
                Log.d(TAG, "toIndex: " + index);
                return index;
            }
        }
        return 0;
    }

    private View createDialogView() {
        Log.d(TAG, "onCreateDialogView - enter");
        final LayoutInflater layoutInflater = (LayoutInflater)getActivity()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.xml.bluetooth_broadcast_pin_config, null);

        final RadioGroup radioGroup = (RadioGroup) view.findViewById(getRadioButtonGroupId());
        if (radioGroup == null) {
            Log.e (TAG, "Not able to find RadioGroup");
            return null;
        }
        radioGroup.clearCheck();
        radioGroup.setOnCheckedChangeListener(this);

        // Fill up the Radio Group
        mRadioButtonIds.add(R.id.bluetooth_broadcast_pin_unencrypted);
        mRadioButtonIds.add(R.id.bluetooth_broadcast_pin_4);
        mRadioButtonIds.add(R.id.bluetooth_broadcast_pin_16);
        String[] stringArray = getContext().getResources().getStringArray(
                R.array.bluetooth_broadcast_pin_config_titles);
        for (int i = 0; i < stringArray.length; i++) {
            mRadioButtonStrings.add(stringArray[i]);
        }
        if (mBapProfile != null) {
            byte[] currentKey = mBapProfile.getEncryptionKey();
            mCurrentPinConfiguration = toIndex(getLength(currentKey));
            Log.d(TAG, "Current pin config: " + mCurrentPinConfiguration);
        }
        RadioButton radioButton;
        for (int i = 0; i < mRadioButtonStrings.size(); i++) {
            radioButton = (RadioButton) view.findViewById(mRadioButtonIds.get(i));
            if (radioButton == null) {
                Log.e(TAG, "Unable to show dialog by no radio button:" + mRadioButtonIds.get(i));
                return null;
            }
            radioButton.setText(mRadioButtonStrings.get(i));
            radioButton.setEnabled(true);
            if (i == mCurrentPinConfiguration) {
                radioButton.setChecked(true);
            }
        }

        mCurrentPinView = (TextView) view.findViewById(R.id.bluetooth_broadcast_current_pin);
        //mCurrentPinView.setText("Current Pin is " + getCurrentPin());
        Log.d(TAG, "onCreateDialogView - exit");
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mAlertDialog = null;
        mOkButton = null;
        mCurrentPinView = null;
        mRadioButtonIds = new ArrayList<>();
        mRadioButtonStrings = new ArrayList<>();
        mUserSelectedPinConfiguration = -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (mOkButton == null) {
            if (mAlertDialog != null) {
                mOkButton = mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                mOkButton.setEnabled(true);
            } else {
                Log.d(TAG, "onResume: mAlertDialog is null");
            }
        }
    }
}
