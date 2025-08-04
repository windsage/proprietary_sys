/*
* Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
* All rights reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/
package com.qualcomm.qti.dctestapp;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.telephony.SubscriptionInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vendor.qti.imsdatachannel.aidl.DataChannelState;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelAttributes;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelCommandErrorCode;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelErrorCode;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelResponse;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelState;
import vendor.qti.imsdatachannel.aidl.ImsReasonCode;
import vendor.qti.imsdatachannel.client.ImsDataChannelConnection;
import vendor.qti.imsdatachannel.client.ImsDataChannelEventListener;
import vendor.qti.imsdatachannel.client.ImsDataChannelServiceAvailabilityCallback;
import vendor.qti.imsdatachannel.client.ImsDataChannelServiceManager;

public class SubscriptionFragment extends Fragment implements ImsDataChannelServiceAvailabilityCallback {

    final String LOG_TAG = "DCTestApp:SubscriptionFragment";

    int mSub;
    int mSlotId;
    String mIccid;
    Executor mExecutor = new ScheduledThreadPoolExecutor(1);

    private ArrayAdapter<String> dcTransportArrayAdapter;
    private Spinner dcTransportSpinner;
    private Button openDcTransportButton;
    private String selectedDcTransportId;
    private Button appCrashButton;
    private List<String> logs = new ArrayList<String>();
    private ViewGroup logContainer;

    private boolean mDataChannelServiceAvailable = false;
    private ImsDataChannelServiceManager mDcManager = null;
    private Map<String, DcTransportFragment> dcTransportFragmentMap = new HashMap<String, DcTransportFragment>();

    public SubscriptionFragment(SubscriptionInfo subInfo) {
        mSub = subInfo.getSubscriptionId();
        mSlotId = subInfo.getSimSlotIndex();
        mIccid = subInfo.getIccId();
        mDcManager = DcServiceManager.getInstance();
        Log.d(LOG_TAG, "SubId[" + mSub + "], SlotId[" + mSlotId + "], iccid[" + mIccid + "]");
    }

    public SubscriptionFragment(int sub, int slotId, String iccid) {
        mSub = sub;
        mSlotId = slotId;
        mIccid = iccid;
        mDcManager = DcServiceManager.getInstance();
        Log.d(LOG_TAG, "SubId[" + mSub + "], SlotId[" + mSlotId + "], iccid[" + mIccid + "]");
    }

    public int getSub() {
        return mSub;
    }

    public int getSlotId() {
        return mSlotId;
    }

    public void onReceivedCallId(String callId) {
        Log.d(LOG_TAG,"onReceivedCallId(callId=\"" + callId + "\")");
        newLog("Call Started with callId=\"" + callId + "\"");
	if(mDataChannelServiceAvailable) {
	if(dcTransportFragmentMap.containsKey(callId)) {
		Log.d(LOG_TAG,"onReceivedCallId transport already present");
	} else {
		Log.d(LOG_TAG,"onReceivedCallId before creating transport fragment");
		dcTransportFragmentMap.put(callId, new DcTransportFragment(callId, mSlotId, mIccid, this));
		Log.d(LOG_TAG,"onReceivedCallId now add");
		getActivity().runOnUiThread(() -> {
		    dcTransportArrayAdapter.add(callId);
		});
		Log.d(LOG_TAG,"onReceivedCallId after add");
		dcTransportFragmentMap.get(callId).tryCreateDataChannelTransport();
		Log.d(LOG_TAG,"onReceivedCallId after trycreatedatachanneltransport");
	}
	} else {
		Log.d(LOG_TAG,"onReceivedCallId mDataChannelServiceAvailable unavailable");
		newLog("mDataChannelServiceAvailable is unavailable");
	}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate()");
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView() start");
        View rootView = inflater.inflate(R.layout.fragment_subscription, container, false);


        dcTransportArrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item);
        dcTransportSpinner = rootView.findViewById(R.id.spinner_dcTransports);
        dcTransportSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
                    Log.d(LOG_TAG, "Spinner selected: " + pos);
                    selectedDcTransportId = dcTransportArrayAdapter.getItem(pos);
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    Log.d(LOG_TAG, "Spinner nothing selected");
                    selectedDcTransportId = null;
                }
            });
        dcTransportSpinner.setAdapter(dcTransportArrayAdapter);
	openDcTransportButton = rootView.findViewById(R.id.button_openDcTransport);
	openDcTransportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedDcTransportId != null) {
                    Log.d(LOG_TAG, "Showing selectedDcTransportId = " + selectedDcTransportId);
                    showDcTransport(selectedDcTransportId);
                }
            }
        });

        appCrashButton = rootView.findViewById(R.id.button_triggerAppCrash);
        appCrashButton.setOnClickListener(new View.OnClickListener() {
                           @Override
	                   public void onClick(View view) {
		                triggerAppCrash();
	                   }
	});

        logContainer = rootView.findViewById(R.id.linearLayout_dcStatusMessageLog);
        for (String log : logs) {
            showLog(log);
        }

        return rootView;
    }



    private void showLog(String log) {
        if (logContainer == null) {
            Log.d(LOG_TAG, "showLog logContainer is null. Not showing log now");
            return;
        }
        logContainer.post(() -> {
            TextView logView = new TextView(getActivity());
            logView.setText(log);
            logView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                                     LayoutParams.WRAP_CONTENT));
            logView.setTextSize(14);
            logView.setPadding(8, 8, 8, 8);
            logContainer.addView(logView);
        });
    }

    private void newLog(String log) {
        logs.add(log);
        showLog(log);
    }

    /*-------- Callbacks from connectImsDataChannelService --------*/

    public void onServiceConnected() {
        Log.d(LOG_TAG, "got OnServiceConnected(), calling getAvailability");
        newLog("Data Channel Service connected");
        try {
            mDcManager.getAvailability(mSlotId, this);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "getAvailability RemoteException!!!");
        }
    }

    public void onServiceDisonnected() {
        newLog("Data Channel Service disconnected");
        // #TODO cleanup everything that still needs to be cleaned up
    }

    /*-------- Overrides for ImsDataChannelServiceAvailabilityCallback --------*/

    @Override
    public void onAvailable() {
        Log.d(LOG_TAG, "got onAvailable(), setting mDataChannelServiceAvailable = true");
        newLog("Data Channel Service available");
        mDataChannelServiceAvailable = true;
	//onReceivedCallId("1");
	//onReceivedCallId("2");
    }

    @Override
    public void onUnAvailable() {
        Log.d(LOG_TAG, "got onUnAvailable(), setting mDataChannelServiceAvailable = false");
        newLog("Data Channel Service unavailable");
        mDataChannelServiceAvailable = false;
	for(DcTransportFragment transport: dcTransportFragmentMap.values()) {
            transport.handleCallEnded();
	}
    }

    public void removeTransportForCallId(String callId) {
     /*	    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
	    Fragment fragmentToRemove = dcTransportFragmentMap.get(callId);
	    dcTransportFragmentMap.remove(callId);
	    if (fragmentToRemove != null) {
		        FragmentTransaction transaction = fragmentManager.beginTransaction();
			    transaction.remove(fragmentToRemove);
			        transaction.commit();
	    }
	    */
	    getActivity().runOnUiThread(() -> {
	        dcTransportArrayAdapter.remove(callId);
	    });
    }


    private void showDcTransport(String callId) {
        FragmentManager fragmentManager = getChildFragmentManager();
        DialogFragment dcTransportFragment = dcTransportFragmentMap.get(callId);
        if (dcTransportFragment != null) {
            dcTransportFragment.show(fragmentManager, callId);
        } else {
            newLog("Error: no object found for callId = " + callId + ". Transport may be stuck in closed state without receiving onClosed()");
        }
    }

    private void triggerAppCrash() {
	    Log.d(LOG_TAG, "triggerAppCrash");
	    for(DcTransportFragment transport: dcTransportFragmentMap.values()) {
		    transport.triggerAppCrash();
	    }
	    //TODO: check if transportfragmentMap should be cleared here
    }
}
