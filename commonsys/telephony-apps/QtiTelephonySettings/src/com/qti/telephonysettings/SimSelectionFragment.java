/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.telephony.TelephonyManager;
import android.telephony.UiccSlotMapping;

import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.QtiSimType;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class SimSelectionFragment extends Fragment {

    private static final String TAG = "SimSelectionFragment";
    private View mView;
    private ExtTelephonyManager mExtTelephonyManager;
    private Client mClient;
    private String mPackageName;
    private QtiSimType[] mSupportedSimTypes;
    private QtiSimType[] mUserSelectedSimType;
    private QtiSimType[] mCurrentSimType;
    private Button updateButton;
    private Spinner[] mDropdown;
    private int mActiveModemCount;
    private int mPosition;
    private boolean mServiceConnected = false;
    private Dialog mProgressDialog;

    private static final int TIMEOUT = 90000;
    private static final int DISMISS_PROGRESSBAR_TIMEOUT = 0;
    private static final int EVENT_SWITCH_SIMTYPE_DONE = 1;
    private static final int EVENT_SET_SIM_TYPE_RESPONSE = 2;
    private static final int SINGLE_SIM = 1;

    private final int[] DROPDOWN_IDs = {
            R.id.dropdown_slot1,
            R.id.dropdown_slot2
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        String title = getContext().getResources().getString(R.string.sim_selection_title);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(title);

        TelephonyManager telManager = getContext().getSystemService(TelephonyManager.class);
        mPackageName = getContext().getPackageName();
        mExtTelephonyManager = ExtTelephonyManager.getInstance(getContext());
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);

        if (telManager != null) {
            mActiveModemCount = telManager.getActiveModemCount();
            mSupportedSimTypes = new QtiSimType[mActiveModemCount];
            mUserSelectedSimType = new QtiSimType[mActiveModemCount];
            mCurrentSimType = new QtiSimType[mActiveModemCount];
            mDropdown = new Spinner[mActiveModemCount];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        if (mActiveModemCount == SINGLE_SIM) {
            mView = inflater.inflate(R.layout.sim_selection_fragment_ssim, container, false);
        } else {
            mView = inflater.inflate(R.layout.sim_selection_fragment, container, false);
        }
        updateButton = mView.findViewById(R.id.update_button);

        for (int slotId = 0; slotId < mActiveModemCount; slotId++) {
            mDropdown[slotId] = (Spinner) mView.findViewById(DROPDOWN_IDs[slotId]);

            updateMenu(slotId);

            mDropdown[slotId].setOnItemSelectedListener(new SimTypeMenuSelectedListener(slotId));
        }

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mServiceConnected && mClient != null) {
                    try {
                        if (isSimTypeSame(mUserSelectedSimType, mCurrentSimType)) {
                            Toast.makeText(getContext(),getContext().getResources().
                                    getString(R.string.no_change), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Token token = mExtTelephonyManager.setSimType(mClient,
                                mUserSelectedSimType);
                        if (token == null) {
                            Log.e(TAG, "Invalid token");
                            return;
                        }
                        showProgressBar(true);
                        mHandler.sendMessageDelayed(mHandler.
                                obtainMessage(DISMISS_PROGRESSBAR_TIMEOUT), TIMEOUT);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception : " + e);
                    }
                }
            }
        });
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMenu();
    }

    private boolean isSimTypeSame(QtiSimType[] userSelectedSimType, QtiSimType[] currentSimType) {
        if (userSelectedSimType.length != currentSimType.length) return false;

        for (int slotId = 0; slotId < userSelectedSimType.length; slotId++) {
            if (userSelectedSimType[slotId] == null && currentSimType[slotId] == null) {
                continue;
            } else {
                if (userSelectedSimType[slotId] != null && currentSimType[slotId] != null) {
                    if (userSelectedSimType[slotId].get() == currentSimType[slotId].get()) {
                        continue;
                    }
                }
                Log.d(TAG, "Sim Types are not Same");
                return false;
            }
        }
        Log.d(TAG, "Sim Types are Same");
        return true;
    }

    private class SimTypeMenuSelectedListener implements AdapterView.OnItemSelectedListener {
        private int slotId;
        private int selectedPosition = mPosition;

        public SimTypeMenuSelectedListener(int slotId) {
            this.slotId = slotId;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view,
                int position, long id) {
            if (selectedPosition == position) return;
            selectedPosition = position;
            Object item = adapterView.getItemAtPosition(position);

            if (item != null) {
                Log.d(TAG, "sim type selected : " + item.toString() + " slotId: " +slotId);
                int simType = convertToSimTypeInt(item.toString());
                mUserSelectedSimType[slotId] = new QtiSimType(simType);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            // TODO Auto-generated method stub
        }
    }

    private void showProgressBar(boolean visible) {
        Log.d(TAG, "showProgressBar: " + visible);

        if (visible) {
            CharSequence dialogTitle =
                    getContext().getResources().getString(R.string.progress_dialog_title);
            int padding = 20;

            final View dialogTitleView = getLayoutInflater().inflate(R.layout.progress_dialog,
                    null);
            final TextView titleText = dialogTitleView.findViewById(R.id.summary);
            titleText.setText(dialogTitle);

            final ProgressBar progressBar = new ProgressBar(getContext());
            progressBar.setIndeterminate(true);
            progressBar.setPadding(0, padding / 2, 0, padding);

            mProgressDialog = new AlertDialog.Builder(getContext())
                    .setCustomTitle(dialogTitleView)
                    .setView(progressBar)
                    .setCancelable(false)
                    .create();

            mProgressDialog.show();
        } else {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what: " + msg.what);
            switch (msg.what) {
                case EVENT_SET_SIM_TYPE_RESPONSE:
                    Status status = (Status) msg.obj;
                    showProgressBar(false);
                    if (status.get() == Status.SUCCESS) {
                        showToast(true);
                    } else {
                        showToast(false);
                        updateMenu();
                    }
                    mHandler.removeMessages(DISMISS_PROGRESSBAR_TIMEOUT);
                    break;

                case EVENT_SWITCH_SIMTYPE_DONE:
                    updateMenu();
                    break;

                case DISMISS_PROGRESSBAR_TIMEOUT:
                    showProgressBar(false);
                    Toast.makeText(getContext(), getContext().getResources().getString(
                            R.string.timed_out), Toast.LENGTH_SHORT).show();
                    updateMenu();
                    break;
                default:
                    break;
            }
        }
    };

    private void showToast(boolean success) {
        if (success) {
            Toast.makeText(getContext(), getContext().getResources().getString(
                    R.string.switch_simtype_pass), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), getContext().getResources().getString(
                    R.string.switch_simtype_fail), Toast.LENGTH_SHORT).show();
        }
    }

    private int convertToSimTypeInt(String simtype) {
        if (simtype.startsWith("PSIM")) {
            return QtiSimType.SIM_TYPE_PHYSICAL;
        } else if (simtype.startsWith("iUICC")) {
            return QtiSimType.SIM_TYPE_IUICC;
        } else if (simtype.startsWith("eUICC")) {
            return QtiSimType.SIM_TYPE_ESIM;
        }
        return QtiSimType.SIM_TYPE_INVALID;
    }

    private void updateMenu(int slotId) {
        Log.d(TAG, "updateMenu");
        List<String> supportedSimTypes = new ArrayList<String>();

        supportedSimTypes = getSupportedSimTypeString(slotId);

        mPosition = getPosition(supportedSimTypes, getCurrentSimType(slotId));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, supportedSimTypes);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mDropdown[slotId].setAdapter(dataAdapter);
        mDropdown[slotId].setSelection(mPosition);

        if (!isSimTypeSame(mUserSelectedSimType, mCurrentSimType)) {
            mUserSelectedSimType = Arrays.copyOf(mCurrentSimType, mActiveModemCount);
            Log.d(TAG, "Update UserSelectedSimType with CurrentSimType : "
                    + Arrays.toString(mCurrentSimType));
        }
    }

    private int getPosition(List<String> supportedSimSlots, int simType) {
        String simTypeasString = convertToString(simType);
        if (supportedSimSlots != null && supportedSimSlots.contains(simTypeasString)) {
            return supportedSimSlots.indexOf(simTypeasString);
        }
        return 0;
    }

    private int getCurrentSimType(int slotId) {
        if (mServiceConnected) {
             if (mCurrentSimType[slotId] == null) {
                Log.d(TAG, "Calling getCurrentSimType");
                mCurrentSimType = mExtTelephonyManager.getCurrentSimType();
            }
        } else {
            Log.e(TAG, "ExtTelephony Service not connected!");
        }
        return (mCurrentSimType[slotId] != null) ? mCurrentSimType[slotId].get() :
                QtiSimType.SIM_TYPE_INVALID;
    }

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void onSimTypeChanged(QtiSimType[] simtype) throws RemoteException {
            mCurrentSimType = Arrays.copyOf(simtype, mActiveModemCount);
            Log.d(TAG, "onSimTypeChanged :  " + Arrays.toString(mCurrentSimType));
            mHandler.sendEmptyMessage(SimSelectionFragment.EVENT_SWITCH_SIMTYPE_DONE);
        }

        @Override
        public void setSimTypeResponse(Token token, Status status) throws RemoteException {
            Log.d(TAG, "setSimTypeResponse: token = " + token + " Status = " + status);
            mHandler.sendMessage(mHandler.obtainMessage(
                    SimSelectionFragment.EVENT_SET_SIM_TYPE_RESPONSE, status));
        }
    };

    private void updateMenu() {
        for (int slotId = 0; slotId < mActiveModemCount; slotId++) {
            updateMenu(slotId);
        }
    }

    private String convertToString(int simType) {
        switch (simType) {
            case QtiSimType.SIM_TYPE_PHYSICAL:
                return "PSIM";
            case QtiSimType.SIM_TYPE_ESIM:
                return "eUICC";
            case QtiSimType.SIM_TYPE_IUICC:
                return "iUICC";
            default:
                return "";
        }
    }

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "mExtTelManagerServiceCallback: service connected");
            int[] events = new int[] {
                    ExtPhoneCallbackListener.EVENT_ON_SIM_TYPE_CHANGED};
            mServiceConnected = true;
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mPackageName, mExtPhoneCallbackListener, events);
            updateMenu();
            Log.d(TAG, "Client = " + mClient);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "mExtTelManagerServiceCallback: service disconnected");
            if (mServiceConnected) {
                mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
                mServiceConnected = false;
                mClient = null;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    private QtiSimType[] getSupportedSimTypes() {
        if (mServiceConnected) {
            mSupportedSimTypes = mExtTelephonyManager.getSupportedSimTypes();
        } else {
            Log.e(TAG, "ExtTelephony Service not connected!");
        }
        return mSupportedSimTypes;
    }

    private List<String> getSupportedSimTypeString(int slotId) {
        List<String> all_slots = new ArrayList<String>();
        QtiSimType[] simType = getSupportedSimTypes();

        if (simType == null || simType.length < mActiveModemCount) {
            Log.e(TAG, "Invalid supported sim types");
            return all_slots;
        }

        int simTypeValue = (simType[slotId] != null) ? simType[slotId].get() :
                QtiSimType.SIM_TYPE_INVALID;

        Log.i(TAG, "simTypevalue for slotId " + slotId + " is " + simTypeValue);

        switch (simTypeValue) {
            case QtiSimType.SIM_TYPE_PHYSICAL:
                all_slots.add("PSIM");
                break;
            case QtiSimType.SIM_TYPE_ESIM:
                all_slots.add("eUICC");
                break;
            case QtiSimType.SIM_TYPE_IUICC:
                all_slots.add("iUICC");
                break;
            case QtiSimType.SIM_TYPE_PHYSICAL_ESIM:
                all_slots.add("PSIM");
                all_slots.add("eUICC");
                break;
            case QtiSimType.SIM_TYPE_PHYSICAL_IUICC:
                all_slots.add("PSIM");
                all_slots.add("iUICC");
                break;
            case QtiSimType.SIM_TYPE_ESIM_IUICC:
                all_slots.add("eUICC");
                all_slots.add("iUICC");
                break;
            case QtiSimType.SIM_TYPE_PHYSICAL_ESIM_IUICC:
                all_slots.add("PSIM");
                all_slots.add("eUICC");
                all_slots.add("iUICC");
                break;
            default:
                break;
        }
        return all_slots;
    }
}
