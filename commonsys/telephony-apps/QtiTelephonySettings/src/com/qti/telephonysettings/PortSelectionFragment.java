/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccSlotInfo;
import android.telephony.UiccSlotMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PortSelectionFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "PortSelectionFragment";

    private Context mContext;
    private View mView;
    private String[] mSimType;
    private Button updateButton;
    private Spinner[] mDropdown;
    private int mActiveModemCount;
    private int mPosition;
    private TelephonyManager mTelMgr;
    private Collection<UiccSlotMapping> mSimSlotMappings;
    private Collection<UiccSlotMapping> mUserSelectedMappings;

    private static final String PHYSICAL_SIM = "PSIM";
    private static final String ESIM = "ESIM";
    private static final String MEP_ESIM = "MEP SIM";
    private static final String MEP = "MEP";
    private static final String PORT = "Port";
    private static final int DEFAULT_PORT_INDEX = 0;

    private static final int EVENT_SIM_SLOT_STATUS_CHANGED = 1;

    private final int[] DROPDOWN_IDs = {R.id.dropdown_slot1, R.id.dropdown_slot2};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mContext = getContext();
        String title = mContext.getResources().getString(R.string.port_selection_title);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);

        mTelMgr = mContext.getSystemService(TelephonyManager.class);

        if (mTelMgr != null) {
            mActiveModemCount = mTelMgr.getActiveModemCount();
            mDropdown = new Spinner[mActiveModemCount];
            mSimType = new String[mActiveModemCount];
        }

        mUserSelectedMappings = new ArrayList<>();
        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        mView = inflater.inflate(R.layout.port_selection_fragment, container, false);
        updateButton = mView.findViewById(R.id.update_button);

        updateSimSlotMappings();
        for (int slotId = 0; slotId < mActiveModemCount; slotId++) {
            mDropdown[slotId] = (Spinner) mView.findViewById(DROPDOWN_IDs[slotId]);
            updateMenu(slotId);
            mDropdown[slotId].setOnItemSelectedListener(new SimPortMenuSelectedListener(slotId));
        }

        updateButton.setOnClickListener(this);
        return mView;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() != R.id.update_button) return;
        Exception exception = null;

        try {
            if (mUserSelectedMappings.equals(mSimSlotMappings)) {
                Toast.makeText(mContext, mContext.getResources().
                        getString(R.string.no_change), Toast.LENGTH_SHORT).show();
                return;
            }
            mTelMgr.setSimSlotMapping(mUserSelectedMappings);
        } catch (IllegalArgumentException e) {
            exception = e;
            Log.e(TAG, "Illegal mapping requested");
            showToast(false, mContext.getResources().getString(R.string.invalid_combination));
        } catch (IllegalStateException e) {
            exception = e;
            Log.e(TAG, "Operation failed");
            showToast(false, mContext.getResources().getString(R.string.operation_failed));
        }

        if (exception == null) showToast(true, "");
        updateSimSlotMappings();
        updateMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        updateSimSlotMappings();
        updateMenu();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED.equals(intent.getAction())) {
                Log.d(TAG, "ACTION_SIM_SLOT_STATUS_CHANGED");
                updateSimSlotMappings();
                updateMenu();
            }
        }
    };

    // Extract the relevant Physical slot Index and Port Index from the item selected.
    // For ex: PSIM1 item will translate to Physical slot Index 0 and Port Index 0.
    // and MEP SIM2 Port2 will translate to Physical slot Index 1 and Port Index 1.
    private int[] convItemToMappingParams(String item) {
        String[] splitItem = item.split("\\s+");
        int size = splitItem.length;
        int[] params = {-1, -1};

        if (splitItem == null || size == 0) return params;

        if (splitItem[0].startsWith(PHYSICAL_SIM) || splitItem[0].startsWith(ESIM)) {
            int index = Integer.parseInt(splitItem[0].replaceAll("[^0-9]", ""));
            params[0] = (index - 1); // Physical Slot Index
            params[1] = DEFAULT_PORT_INDEX;
            return params;
        } else if (splitItem[0].startsWith(MEP)) {
            for (int i = 0, j = i + 1 ; j < size; i++) {
                int index = Integer.parseInt(splitItem[j++].replaceAll("[^0-9]", ""));
                params[i] = (index - 1); // Physical Slot Index and Port Index
            }
            return params;
        }
        return params;
    }

    private class SimPortMenuSelectedListener implements AdapterView.OnItemSelectedListener {
        private int slotId;
        private int selectedPosition = mPosition;

        public SimPortMenuSelectedListener(int slotId) {
            this.slotId = slotId;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view,
                int position, long id) {
            if (selectedPosition == position) return;
            selectedPosition = position;
            Object item = adapterView.getItemAtPosition(position);

            if (item != null) {
                int[] slotMappingParams = convItemToMappingParams(item.toString());

                UiccSlotMapping selectedSlotMapping =
                        new UiccSlotMapping(slotMappingParams[1] /* portIndex */,
                                slotMappingParams[0] /* physical slot index */, slotId);
                Collection<UiccSlotMapping> tempMappings =
                        mUserSelectedMappings.stream().collect(Collectors.toList());

                // Traverse and replace the mapping for the user selected logical slotId.
                for (UiccSlotMapping uiccSlotMapping : tempMappings) {
                    if (uiccSlotMapping.getLogicalSlotIndex() == slotId) {
                        mUserSelectedMappings.remove(uiccSlotMapping);
                        mUserSelectedMappings.add(selectedSlotMapping);
                    }
                }

                mUserSelectedMappings = mUserSelectedMappings.stream()
                        .sorted(Comparator.comparingInt(UiccSlotMapping::getPhysicalSlotIndex)
                                .thenComparingInt(UiccSlotMapping::getPortIndex))
                        .collect(Collectors.toList());
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            // TODO Auto-generated method stub
        }
    }

    private void showToast(boolean success, String failureString) {
        if (success) {
            Toast.makeText(mContext, mContext.getResources().getString(
                    R.string.switch_sim_port_pass), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, failureString, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMenu(int slotId) {
        List<String> supportedSimTypes = new ArrayList<String>();

        supportedSimTypes = getSupportedSimTypeString();
        Log.d(TAG, "supportedSimTypes: " + supportedSimTypes);

        mPosition = getPosition(mSimSlotMappings, supportedSimTypes, slotId);
        Log.d(TAG, "mPosition: " + mPosition);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_item, supportedSimTypes);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mDropdown[slotId].setAdapter(dataAdapter);
        mDropdown[slotId].setSelection(mPosition);
    }

    private void updateSimSlotMappings() {
        if (mTelMgr == null) return;
        mSimSlotMappings = mTelMgr.getSimSlotMapping();
        Log.d(TAG, "Current SimSlotMapping: " + mSimSlotMappings);

        if (mUserSelectedMappings != null && mUserSelectedMappings.equals(mSimSlotMappings)) return;

        mUserSelectedMappings.clear();
        for (UiccSlotMapping uiccSlotMapping : mSimSlotMappings) {
            mUserSelectedMappings.add(new UiccSlotMapping(uiccSlotMapping.getPortIndex(),
                    uiccSlotMapping.getPhysicalSlotIndex(),
                    uiccSlotMapping. getLogicalSlotIndex()));
        }
        Log.d(TAG, "UserSelectedMappings: " + mUserSelectedMappings);
    }

    private int getPosition(Collection<UiccSlotMapping> simSlotMappings,
            List<String> supportedSimType, int slotId) {
        for (UiccSlotMapping slotMapping : simSlotMappings) {
            if (slotMapping.getLogicalSlotIndex() == slotId) {
                int physicalSlotId = slotMapping.getPhysicalSlotIndex();
                int portIndex = slotMapping.getPortIndex();
                String simTypeString = "";

                if (mSimType == null || mSimType[physicalSlotId] == null) return 0;

                if (mSimType[physicalSlotId] == PHYSICAL_SIM || mSimType[physicalSlotId] == ESIM) {
                    simTypeString = mSimType[physicalSlotId] + (physicalSlotId + 1);
                } else if (mSimType[physicalSlotId] == MEP_ESIM) {
                    simTypeString =
                            mSimType[physicalSlotId] + (physicalSlotId + 1) + " " + PORT +
                                    (portIndex + 1);
                }

                if (supportedSimType != null && supportedSimType.contains(simTypeString)) {
                    return supportedSimType.indexOf(simTypeString);
                }
            }
        }
        return 0;
    }

    private void updateMenu() {
        for (int slotId = 0; slotId < mActiveModemCount; slotId++) {
            updateMenu(slotId);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "OnDestroy");
        mContext.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private UiccSlotInfo[] getUiccSlotsInfo() {
        UiccSlotInfo[] slotsInfo = null;
        if (mTelMgr != null) {
            slotsInfo = mTelMgr.getUiccSlotsInfo();
        }
        return slotsInfo;
    }

    private List<UiccCardInfo> getUiccCardsInfo() {
        List<UiccCardInfo> cardInfos = null;
        if (mTelMgr != null) {
            cardInfos = mTelMgr.getUiccCardsInfo();
        }
        return cardInfos;
    }

    private String[] getSupportedSimTypes(UiccSlotInfo[] slotsInfo,
            List<UiccCardInfo> uiccCardsInfo) {
        if (slotsInfo == null || uiccCardsInfo == null) {
            Log.d(TAG, "SlotInfo or CardInfo is null");
            return mSimType;
        }

        for (int index = 0; index < mActiveModemCount; index++) {
            UiccSlotInfo slotInfo = slotsInfo[index];
            UiccCardInfo cardInfo = uiccCardsInfo.get(index);

            if (slotInfo == null || cardInfo == null) {
                Log.d(TAG, "SlotInfo or CardInfo is null for index: " + index);
                continue;
            }

            if (slotInfo.getCardStateInfo() == UiccSlotInfo.CARD_STATE_INFO_PRESENT) {
                if (cardInfo.isEuicc()) {
                    mSimType[index] =
                            cardInfo.isMultipleEnabledProfilesSupported() ?  MEP_ESIM : ESIM;
                } else {
                    mSimType[index] = PHYSICAL_SIM;
                }
            }
        }
        return mSimType;
    }

    private List<String> getSupportedSimTypeString() {
        List<String> all_slots = new ArrayList<String>();
        String[] simType = getSupportedSimTypes(getUiccSlotsInfo(), getUiccCardsInfo());

        if (simType == null || simType.length < mActiveModemCount) {
            Log.e(TAG, "Invalid supported sim types");
            return all_slots;
        }

        for (int slotId = 0; slotId < mActiveModemCount; slotId++) {
            if (simType[slotId] == PHYSICAL_SIM) {
                all_slots.add("PSIM" + (slotId + 1));
            } else if (simType[slotId] == ESIM) {
                all_slots.add("ESIM" + (slotId + 1));
            } else if (simType[slotId] == MEP_ESIM) {
                all_slots.add("MEP SIM" + (slotId + 1) + " Port1");
                all_slots.add("MEP SIM" + (slotId + 1) + " Port2");
            }
        }
        return all_slots;
    }
}
