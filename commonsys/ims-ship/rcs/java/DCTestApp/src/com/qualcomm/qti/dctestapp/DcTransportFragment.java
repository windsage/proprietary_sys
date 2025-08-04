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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import vendor.qti.imsdatachannel.client.ImsDataChannelStatusCallback;
import vendor.qti.imsdatachannel.client.ImsDataChannelTransport;

public class DcTransportFragment extends DialogFragment implements ImsDataChannelEventListener {

    final String LOG_TAG = "DCTestApp:DcTransportFragment";

    int mSlotId;
    String mIccid;
    Executor mExecutor = new ScheduledThreadPoolExecutor(1);

    private Button createDcConnectionsButton;
    private Button closeDcConnectionsButton;
    private Button sendMessageToAllDCConnections;

    private List<String> pendingAvailableDcConnections = new ArrayList<String>();
    private ArrayAdapter<String> dcConnectionArrayAdapter;
    private Spinner dcConnectionSpinner;
    private Button openDcConnectionButton;

    private List<ImsDataChannelAttributes[]> pendingDcSetupReqs = new ArrayList<ImsDataChannelAttributes[]>();
    private ViewGroup dcSetupRequestContainer;
    private int nextReqNum = 0;

    private List<String> logs = new ArrayList<String>();
    private ViewGroup logContainer;

    private String mDcXml;
    private String mCallId;
    private ImsDataChannelServiceManager mDcManager = null;
    private ImsDataChannelTransport dcTransport = null;
    private Map<String, DcConnectionFragment> dcConnectionFragmentMap = new HashMap<String, DcConnectionFragment>();
    private String selectedDcConnectionId;
    private Set<String> bootstrapDcConnectionIds = new HashSet<String>();
    private DcSetupRequestFragment activeSetupRequestFragment = null;
    private Map<String, Button> reqButtons = new HashMap<String, Button>();
    private Set<String> unAcceptedDcIds = new HashSet<String>();

    private SubscriptionFragment mSubFragment;

    File externalFileDir;
    final String FILE_NAME = "DataChannelAppInfo.xml";
    ExecutorService threadPool = Executors.newFixedThreadPool(4);

    public DcTransportFragment(String callId, int slotId, String iccId, SubscriptionFragment subFrag) {
	mCallId = callId;
        mSlotId = slotId;
        mIccid = iccId;
	mSubFragment = subFrag;
	mDcManager = DcServiceManager.getInstance();
        Log.d(LOG_TAG, " SlotId[" + mSlotId + "], iccid[" + mIccid + "]");
    }

    public int getSlotId() {
        return mSlotId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate()");
        externalFileDir = getActivity().getExternalFilesDir(null);
        mDcXml = readXml();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView() start");
        View rootView = inflater.inflate(R.layout.fragment_transport, container, false);

        createDcConnectionsButton = rootView.findViewById(R.id.button_createDcConnections);
        createDcConnectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    createDcConnectionsFromFile();
                } catch(IOException e) {
                    Log.e(LOG_TAG, "createDcConnectionsFromFile threw IOException!!!");
                } catch(XmlPullParserException e) {
                    Log.e(LOG_TAG, "createDcConnectionsFromFile threw XmlPullParserException, printing stack trace");
                    e.printStackTrace();
                }
            }
        });
        if (dcTransport != null) {
            createDcConnectionsButton.setEnabled(true);
        }

        sendMessageToAllDCConnections = rootView.findViewById(R.id.button_sendMsgToAllDCConnections);

        sendMessageToAllDCConnections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsgToAllConnections();
            }
        });

        dcConnectionArrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item);
        dcConnectionSpinner = rootView.findViewById(R.id.spinner_dcConnections);
        dcConnectionSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
                Log.d(LOG_TAG, "Spinner selected: " + pos);
                selectedDcConnectionId = dcConnectionArrayAdapter.getItem(pos);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d(LOG_TAG, "Spinner nothing selected");
                selectedDcConnectionId = null;
            }
        });
        dcConnectionSpinner.setAdapter(dcConnectionArrayAdapter);
        for (String dcId : pendingAvailableDcConnections) {
            Log.d(LOG_TAG, "adding dcId[" + dcId + "] to dcConnectionArrayAdapter");
            dcConnectionArrayAdapter.add(dcId);
        }

        openDcConnectionButton = rootView.findViewById(R.id.button_openDcConnection);
        openDcConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedDcConnectionId != null) {
                    Log.d(LOG_TAG, "Showing selectedDcConnectionId = " + selectedDcConnectionId);
                    showDcConnection(selectedDcConnectionId);
                }
            }
        });

        closeDcConnectionsButton = rootView.findViewById(R.id.button_closeDcConnections);
        closeDcConnectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeAllDcConnections();
            }
        });

        dcSetupRequestContainer = rootView.findViewById(R.id.linearLayout_dcSetupRequestContainer);
        for (ImsDataChannelAttributes[] attrs : pendingDcSetupReqs) {
            handleNewDcSetupRequest(attrs);
        }
        pendingDcSetupReqs.clear();

        logContainer = rootView.findViewById(R.id.linearLayout_dcStatusMessageLog);
        for (String log : logs) {
            showLog(log);
        }

        return rootView;
    }

    private void sendMsgToAllConnections() {
	    for (DcConnectionFragment f : dcConnectionFragmentMap.values()) {
		    threadPool.submit(() -> {
			    f.openFileLocation();
		    });
	    }
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

    /*-------- Overrides for ImsDataChannelEventListener --------*/

    @Override
    public void onDataChannelAvailable(ImsDataChannelAttributes attr, ImsDataChannelConnection dcConnection) {
        Log.d(LOG_TAG, "onDataChannelAvailable called");
        newLog("New Data Channel available: dcId=" + attr.getDcId());
        handleNewDcConnection(attr, dcConnection);
    }

    @Override
    public void onDataChannelSetupRequest(ImsDataChannelAttributes[] attrs) {
        Log.d(LOG_TAG, "onDataChannelSetupRequest called");
        if (dcSetupRequestContainer != null) {
            handleNewDcSetupRequest(attrs);
        } else {
            pendingDcSetupReqs.add(attrs);
        }
    }

    @Override
    public void onDataChannelCreated(ImsDataChannelAttributes attr, ImsDataChannelConnection dcConnection) {
        Log.d(LOG_TAG, "onDataChannelCreated called");
        newLog("New Data Channel created: dcId=" + attr.getDcId());
        handleNewDcConnection(attr, dcConnection);
    }

    @Override
    public void onDataChannelSetupError(ImsDataChannelAttributes attr, ImsDataChannelErrorCode code) {
        newLog("Data Channel setup error: dcId=" + attr.getDcId() + " code=" + code.getImsDataChannelErrorCode());
    }

    @Override
    public void onDataChannelTransportClosed(ImsReasonCode reasonCode) {
        Log.d(LOG_TAG, "onDataChannelTransportClosed(reasonCode=" + reasonCode.getImsReasonCode() + ")");
        newLog("Data Channel Transport closed reasonCode=" + reasonCode.getImsReasonCode());
        getActivity().runOnUiThread(() -> {
            dcConnectionArrayAdapter.clear();
            closeDcConnectionsButton.setEnabled(false);
        });
        dcConnectionFragmentMap.clear();
        bootstrapDcConnectionIds.clear();
	dcTransport = null;
	mCallId = null;
        mSubFragment.removeTransportForCallId(mCallId);
    }

    @Override
    public void onDataChannelCommandError(ImsDataChannelAttributes attr, ImsDataChannelCommandErrorCode errorCode) {
        newLog("Data Channel command error: dcId=" + attr.getDcId() + " errorCode=" + errorCode.getImsDataChannelCommandErrorCode());
    }

    @Override
    public void onDataChannelSetupError(String dcId, ImsDataChannelErrorCode code) {
        newLog("Data Channel setup error: dcId=" + dcId + " code=" + code.getImsDataChannelErrorCode());
    }

    @Override
    public void onDataChannelCommandError(String dcId, ImsDataChannelCommandErrorCode errorCode) {
        newLog("Data Channel command error: dcId=" + dcId + " errorCode=" + errorCode.getImsDataChannelCommandErrorCode());
    }

    @Override
    public void onDataChannelSetupCancelRequest(String[] dcIdList) {
        Log.d(LOG_TAG, "onDataChannelSetupCancelRequest called for dcIdList length " + dcIdList.length);
        for(int i=0;i<dcIdList.length;i++){
            newLog("Data Channel Setup Cancel error: dcId=" + dcIdList[i]);
        }
        getActivity().runOnUiThread(() -> {
            if (activeSetupRequestFragment != null) {
                activeSetupRequestFragment.onCancelSetupRequest(dcIdList);
            }
        });
    }

    /** Helper methods */

    private String readXml() {
        Log.d(LOG_TAG, "readXml(): Filename: " + FILE_NAME);
        File xmlFile = new File(externalFileDir, FILE_NAME);
        Log.d(LOG_TAG,"readXml(): Reading file : " + xmlFile.getPath());

        StringBuilder text = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(xmlFile));
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line + '\n');
            }
            reader.close();
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error occured while reading text file!!");
        }
        return text.toString();
    }

    private void createDcConnectionsFromFile() throws XmlPullParserException, IOException {
        Log.d(LOG_TAG, "createDcConnectionsFromFile() begin");
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(mDcXml));

        // Read xml ids
        String id;
        List<String> channelIdList = new ArrayList<String>();
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "DataChannelAppInfo");
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG &&
                parser.getName().equals("DataChannel")) {
                if ((id = parser.getAttributeValue(null, "dcId")) != null) {
                    channelIdList.add(id);
                    unAcceptedDcIds.remove(id);
                    Log.d(LOG_TAG, "createDcConnectionsFromFile got id=" + id);
                }
            }
        }
        String[] arr = channelIdList.toArray(new String[channelIdList.size()]);
        Log.d(LOG_TAG, "createDcConnectionsFromFile() array size=" + arr.length);

        if (dcTransport != null) {
            try {
                dcTransport.createDataChannel(arr, mDcXml);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "dcTransport.createDataChannel RemoteException!!!");
            }
        }
    }

    private void closeAllDcConnections() {
        int size = dcConnectionFragmentMap.size();
        Log.d(LOG_TAG, "closeAllDcConnections(): there are " + size);
        closeDcConnectionsButton.post(() -> {
            closeDcConnectionsButton.setEnabled(false);
        });
        if (size == 0) {
            return;
        }
        ImsDataChannelConnection[] dc = new ImsDataChannelConnection[size];
        ImsDataChannelErrorCode code = new ImsDataChannelErrorCode();
        int i = 0;
        for (DcConnectionFragment f : dcConnectionFragmentMap.values()) {
            dc[i] = f.getDcConnection();
            i++;
        }
        if (dcTransport != null) {
            try {
                dcTransport.closeDataChannel(dc, code);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "dcTransport.closeDataChannel RemoteException!!!");
            }
        } else {
            Log.e(LOG_TAG, "closeAllDcConnections() dcTransport is null");
            newLog("Can't close all dc connections since dcTransport is null");
        }
    }

    public void tryCreateDataChannelTransport() {
        if (mCallId != null) {
            if (dcTransport != null && dcTransport.getSlotId() == mSlotId && dcTransport.getCallId() == mCallId) {
                Log.d(LOG_TAG, "tryCreateDataChannelTransport() already created for callId=" + mCallId);
		return;
            }
            try {
                Log.d(LOG_TAG, "tryCreateDataChannelTransport() slotId=" + mSlotId + " callId=" + mCallId);
                dcTransport = mDcManager.createDataChannelTransport(mSlotId, mCallId, this);
                newLog("Data Channel Transport created for callId=\"" + mCallId + "\"");
                if (createDcConnectionsButton != null) {
                    createDcConnectionsButton.post(() -> {
                        createDcConnectionsButton.setEnabled(true);
                    });
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "createDataChannelTransport RemoteException!!!");
            }
        }
    }

    public void handleCallEnded() {
        getActivity().runOnUiThread(() -> {
            createDcConnectionsButton.setEnabled(false);
            dcSetupRequestContainer.removeAllViews();
            if (activeSetupRequestFragment != null) {
                activeSetupRequestFragment.onCallEnded();
            }
        });
        unAcceptedDcIds.clear();
        closeAllDcConnections();
        DcConnectionFragment.resetFiles();
        try {
            mDcManager.closeDataChannelTransport(dcTransport);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "handleCallEnded() mDcManager.closeDataChannelTransport RemoteException!!!");
        }
        dcTransport = null;
        mCallId = null;
	mSubFragment.removeTransportForCallId(mCallId);
    }

    private void handleNewDcConnection(ImsDataChannelAttributes attr, ImsDataChannelConnection dcConnection) {
        if (unAcceptedDcIds.contains(attr.getDcId())) {
            newLog("New datachannel " + attr.getDcId() + " has not been requested. Ignoring.");
            Log.e(LOG_TAG, "handleNewDcConnection() got datachannel that hasn't been requested");
            return;
        }
        DcConnectionFragment dcConnectionFragment = new DcConnectionFragment(attr, dcConnection, dcTransport, externalFileDir);
        String dcConnectionId = mCallId + " : " + attr.getDcId() + " : " + attr.getDataChannelStreamId();
        ImsDataChannelStatusCallback statusListener = new ImsDataChannelStatusCallback() {
            @Override
            public void onClosed(ImsDataChannelErrorCode code) {
                Log.d(LOG_TAG, "onClosed(code=" + code.getImsDataChannelErrorCode() + ") dcId=" + attr.getDcId());
                newLog("DataChannel[" + dcConnectionId + "] closed: code[" + code.getImsDataChannelErrorCode() + "]");
                dcConnectionFragment.onClosed(code);
                dcConnectionFragmentMap.remove(dcConnectionId); // This one is redundant
                getActivity().runOnUiThread(() -> {
                    dcConnectionArrayAdapter.remove(dcConnectionId);
                    Log.d(LOG_TAG, "onClose, removed " + dcConnectionId + " from spinner");
                });
                if (bootstrapDcConnectionIds.contains(dcConnectionId)) {
                    Log.d(LOG_TAG, "Found " + dcConnectionId + " in bootstrapDcConnectionIds[" + bootstrapDcConnectionIds.size() + "]");
                }
                if (bootstrapDcConnectionIds.remove(dcConnectionId) && bootstrapDcConnectionIds.isEmpty()) {
                    Log.d(LOG_TAG, "onClosed(): closed last bootstrap dc");
                    newLog("Last bootstrap datachannel closed");
                    handleCallEnded();
                }
            }

            @Override
            public void onStateChange(ImsDataChannelState dcState) {
                int state = dcState.getState().getDataChannelState();
                Log.d(LOG_TAG, "onStateChange(state=" + state + ") dcId=" + dcState.getDcId());
                String message = "DataChannel state changed: state[" + state + "]";
                newLog("DataChannel state changed: " + state);
                dcConnectionFragment.onStateChange(dcState);
                if (state == DataChannelState.DATA_CHANNEL_CLOSED) {
                    // Remove from map so that closeAllDcConnections doesn't try to close again
                    dcConnectionFragmentMap.remove(dcConnectionId);
                }
            }
        };
        try {
            dcConnection.initialize(mExecutor, statusListener, dcConnectionFragment);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "handleNewDcConnection() dcConnection.initialize RemoteException!!!");
        }

        dcConnectionFragmentMap.put(dcConnectionId, dcConnectionFragment);
        if (dcConnectionArrayAdapter != null) {
            Log.d(LOG_TAG, "handleNewDcConnection() adding [ " + dcConnectionId + " ] to dcConnectionArrayAdapter");
            getActivity().runOnUiThread(() -> {
                dcConnectionArrayAdapter.add(dcConnectionId);
                closeDcConnectionsButton.setEnabled(true);
            });
        } else {
            Log.d(LOG_TAG, "handleNewDcConnection() adding [ " + dcConnectionId + " ] to pendingAvailableDcConnections");
            pendingAvailableDcConnections.add(dcConnectionId);
        }
        if (attr.getDataChannelStreamId() < 1000) {
            bootstrapDcConnectionIds.add(dcConnectionId);
            if (attr.getDataChannelStreamId() == 0) {
                dcConnectionFragment.sendInitialBDCMessage();
                Log.d(LOG_TAG, "handleNewDcConnection() sent initial bootstrap message");
            }
        }

	sendMessageToAllDCConnections.setEnabled(true);
    }

    public void handleNewDcSetupRequest(ImsDataChannelAttributes[] attrs) {
        int reqNum = nextReqNum++;
        newLog("Received Data Channel setup request: " + reqNum);
        Log.i(LOG_TAG,"handleNewDcSetupRequest() reqNum=" + reqNum);
        dcSetupRequestContainer.post(() -> {
            Button button = new Button(getActivity());
            button.setText("Dc Setup Request " + reqNum);
            button.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                                     LayoutParams.WRAP_CONTENT));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDcSetupRequest(attrs, reqNum);
                }
            });
            dcSetupRequestContainer.addView(button);
            reqButtons.put(Integer.toString(reqNum), button);
        });
        for (ImsDataChannelAttributes attr : attrs) {
            unAcceptedDcIds.add(attr.getDcId());
        }
    }

    public void respondToDataChannelSetUpRequest(ImsDataChannelResponse[] r, int reqNum) {
        for (ImsDataChannelResponse resp : r) {
            // This should not be necessary but modem is giving us back data channels that we did not accept
            if (resp.isAcceptStatus()) {
                unAcceptedDcIds.remove(resp.getDcId());
            }
        }
        if (dcTransport != null) {
            try {
                dcTransport.respondToDataChannelSetUpRequest(r, mDcXml);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "respondToDataChannelSetUpRequest RemoteException!!!");
            }
        } else {
            Log.e(LOG_TAG, "respondToDataChannelSetUpRequest() dcTransport is null");
            newLog("Can't respond to data channel setup request since dcTransport is null");
        }
        dcSetupRequestContainer.removeView(reqButtons.remove(Integer.toString(reqNum)));
    }

    private void showDcConnection(String dcConnectionId) {
        FragmentManager fragmentManager = getChildFragmentManager();
        DialogFragment dcConnectionFragment = dcConnectionFragmentMap.get(dcConnectionId);
        if (dcConnectionFragment != null) {
            dcConnectionFragment.show(fragmentManager, dcConnectionId);
        } else {
            newLog("Error: no object found for dcConnectionId = " + dcConnectionId + ". Connection may be stuck in closed state without receiving onClosed()");
        }
    }

    private void showDcSetupRequest(ImsDataChannelAttributes[] attrs, int reqNum) {
        FragmentManager fragmentManager = getChildFragmentManager();
        DcSetupRequestFragment dcSetupRequestFragment = new DcSetupRequestFragment(this, attrs, reqNum);
        dcSetupRequestFragment.show(fragmentManager, "dcSetupReq" + reqNum);
        activeSetupRequestFragment = dcSetupRequestFragment;
        Log.d(LOG_TAG, "showDcSetupRequest reqNum=" + reqNum);
    }

    private ArrayList<ImsDataChannelConnection> findApplicationDataChannel()
    {
        int ADCsize = 0;
        ArrayList<ImsDataChannelConnection> ADCList = new ArrayList<ImsDataChannelConnection>();
        for (DcConnectionFragment f : dcConnectionFragmentMap.values()) {
            ImsDataChannelConnection dc = f.getDcConnection();
            ImsDataChannelAttributes attr = dc.getConnectionAttributes();
            if(attr.getDataChannelStreamId() >= 1000)
            {
                ADCsize++;
                ADCList.add(dc);
                Log.d(LOG_TAG, "findApplicationDataChannel: Found ADC: Updated ADCList size " + ADCsize);
            }
        }
        ADCsize = ADCList.size();
        Log.d(LOG_TAG,"Returned ADCList Size is" + ADCsize);
        return ADCList;
    }

    public void triggerAppCrash()
    {
        //int size = dcConnectionFragmentMap.size();
        ArrayList<ImsDataChannelConnection> ADCList = findApplicationDataChannel();
        int size = ADCList.size();
        Log.d(LOG_TAG, "TriggerAppCrash: close All App DcConnections: there are " + size);
        if (ADCList.size() == 0) {
            Log.d(LOG_TAG, "TriggerAppCrash: There are no ADC to close");
            newLog("Error: TriggerAppCrash: There are no ADC to close");
           // DcConnectionFragment.resetFiles();
           /* getActivity().runOnUiThread(() -> {
              createDcConnectionsButton.setEnabled(false);
              dcSetupRequestContainer.removeAllViews();
              if (activeSetupRequestFragment != null) {
                activeSetupRequestFragment.onCallEnded();
              }
              //dcConnectionArrayAdapter.clear();
              //closeDcConnectionsButton.setEnabled(false);
            });*/
            return;
        }
        ImsDataChannelConnection[] dc = new ImsDataChannelConnection[size];
        ImsDataChannelErrorCode code = new ImsDataChannelErrorCode();
        code.setImsDataChannelErrorCode(ImsDataChannelErrorCode.DCS_DC_ERROR_APP_CRASH);
        Log.d(LOG_TAG, "TriggerAppCrash: Setting ErrorCode as DCS_DC_ERROR_APP_CRASH: " + ImsDataChannelErrorCode.DCS_DC_ERROR_APP_CRASH);
        int i = 0;
        for(ImsDataChannelConnection dataChannel : ADCList) {
            dc[i] = dataChannel;
            String dcConnectionId = mCallId + " : " + dataChannel.getConnectionAttributes().getDcId() +
                                    " : " + dataChannel.getConnectionAttributes().getDataChannelStreamId();
            DcConnectionFragment dcConnectionFragment = dcConnectionFragmentMap.get(dcConnectionId);
            if (dcConnectionFragment != null)
            {
                Log.d(LOG_TAG, "TriggerAppCrash; Calling onClosed with ERROR_APP_CRASH on ADC");
                dcConnectionFragment.onClosed(code);
            }
            dcConnectionFragmentMap.remove(dcConnectionId);
            getActivity().runOnUiThread(() -> {
                dcConnectionArrayAdapter.remove(dcConnectionId);
                Log.d(LOG_TAG, "TriggerAppCrash, removed ADC: " + dcConnectionId + " from spinner");
                newLog("TriggerAppCrash: removed ADC: " + dcConnectionId);
            });
            i++;
        }
        /*for (DcConnectionFragment f : dcConnectionFragmentMap.values()) {
            dc[i] = f.getDcConnection();
            i++;
        }*/
        unAcceptedDcIds.clear();
        if (dcTransport != null) {
            try {
                dcTransport.closeDataChannel(dc, code);

            } catch (RemoteException e) {
                Log.e(LOG_TAG, "dcTransport.closeDataChannel RemoteException!!!");
            }
        } else {
            Log.e(LOG_TAG, "TriggerAppCrash dcTransport is null");
            newLog("TriggerAppCrash: Can't close all dc connections since dcTransport is null");
        }
        /*DcConnectionFragment.resetFiles();
        getActivity().runOnUiThread(() -> {
            createDcConnectionsButton.setEnabled(false);
            dcSetupRequestContainer.removeAllViews();
            if (activeSetupRequestFragment != null) {
                activeSetupRequestFragment.onCallEnded();
            }
            dcConnectionArrayAdapter.clear();
            closeDcConnectionsButton.setEnabled(false);
        });
        dcConnectionFragmentMap.clear();
        bootstrapDcConnectionIds.clear();
        dcTransport = null;
        mCallId = null;*/
    }
}
