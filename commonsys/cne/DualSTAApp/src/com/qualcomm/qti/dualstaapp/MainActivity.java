/*
 * Copyright (c) 2020,2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dualstaapp;

import vendor.qti.data.factoryservice.Result;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.os.IBinder;
import android.os.ServiceManager;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET ;
import static android.net.NetworkCapabilities.NET_CAPABILITY_OEM_PAID;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

class Token extends vendor.qti.data.mwqemaidlservice.IMwqemToken.Stub{
    @Override
    public String getInterfaceHash() {
        return vendor.qti.data.mwqemaidlservice.IMwqemToken.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return vendor.qti.data.mwqemaidlservice.IMwqemToken.VERSION;
    }
}

public class MainActivity extends Activity {

    private final String TAG = "DualSTA";
    private String appName = "DualSTA";

    final Context context = this;

    Button requestNetworkButton, initializeMwqemButton, enableMwqemButton, disableMwqemButton, disableMwqemforAllUidsButton;

    private NetworkRequest mOemWifiRequest;
    private ConnectivityManager cm;

    private vendor.qti.data.factory.V2_2.IFactory mFactoryHidl = null;
    private vendor.qti.data.factoryservice.IFactory mFactoryAidl = null;
    private Token mToken = null;
    private vendor.qti.data.mwqem.V1_0.IMwqemService mMwqemServiceHidl;
    private vendor.qti.data.mwqemaidlservice.IMwqemService mMwqemServiceAidl;
    private IBinder mBinder;
    private static long testNetworkHandle;
    private boolean isAidl = false;
    private boolean isHidl = false;
    public Network getNetwork() {
        return Network.fromNetworkHandle(testNetworkHandle);
    }

    private NetworkCallback mWifiOemNetworkCallback = new NetworkCallback() {

        @Override
        public void onAvailable(Network network) {
            testNetworkHandle = network.getNetworkHandle();
            Log.e(TAG, "mWifiOemNetworkCallback network available: cellNetId " + testNetworkHandle);
            Toast.makeText(getApplicationContext(),"mWifiOemNetworkCallback network available: cellNetId "
                    + testNetworkHandle, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            if (network.equals(getNetwork()) && networkCapabilities.hasCapability(NET_CAPABILITY_VALIDATED)) {
                Log.e(TAG, "mWifiOemNetworkCallback network validated: cellNetId " + testNetworkHandle);
                Toast.makeText(getApplicationContext(),"mWifiOemNetworkCallback network validated: cellNetId "
                    + testNetworkHandle, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.MainActivity);

        requestNetworkButton = (Button) findViewById(R.id.requestNetwork);
        initializeMwqemButton = (Button) findViewById(R.id.initializeWmqem);
        enableMwqemButton = (Button) findViewById(R.id.enableMwqem);
        disableMwqemButton = (Button) findViewById(R.id.disableMwqem);
        disableMwqemforAllUidsButton = (Button) findViewById(R.id.disableMwqemforAllUids);

        mMwqemServiceHidl = null;
        mMwqemServiceAidl = null;

        mOemWifiRequest = new NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_OEM_PAID)
                .addTransportType(TRANSPORT_WIFI)
                .build();

        cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        requestNetworkButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.e(TAG, "requestNetwork called");
                cm.requestNetwork(mOemWifiRequest , mWifiOemNetworkCallback);
            }
        });

        initializeMwqemButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.e(TAG, "initializeMwqemButton was clicked");
                final CbResults results = new CbResults();
                try {
                    Log.e(TAG, "Call Ifactory getService");
                    String ISERVICE_INTERFACE = vendor.qti.data.factoryservice.IFactory.DESCRIPTOR + "/default";
                    if (ServiceManager.isDeclared(ISERVICE_INTERFACE)){
                        mBinder = ServiceManager.waitForService(ISERVICE_INTERFACE);
                        mFactoryAidl = vendor.qti.data.factoryservice.IFactory.Stub.asInterface(mBinder);
                        isAidl=true;
                    } else {
                        mFactoryHidl = vendor.qti.data.factory.V2_2.IFactory.getService();
                        isHidl=true;
                    }
                } catch (RemoteException | NoSuchElementException e) {
                    Log.e(TAG, "cne factory not supported: " + e);
                }

                if (mFactoryHidl == null && mFactoryAidl == null) {
                    Log.e(TAG, "cne IFactory returned null");
                    return;
                }
                try {
                    if (isAidl == false){
                    Log.e(TAG, "Call createIMwqemService");
                    mFactoryHidl.createIMwqemService(
                            new vendor.qti.data.mwqem.V1_0.IMwqemToken.Stub() {},
                            (int status, vendor.qti.data.mwqem.V1_0.IMwqemService service) -> {
                                results.status = status;
                                results.service = service;
                    });
                    Log.e(TAG, "HIDL Instance is created");
                    mMwqemServiceHidl = results.service;
                    } else {
                        mToken = new Token();
                        Result result = new Result();
                        mMwqemServiceAidl = mFactoryAidl.createIMwqemService(mToken,result);
                        Log.e(TAG, "AIDL Instance is created");
                    }
                    Log.e(TAG, "createIMwqemService success");
                } catch (RemoteException e) {
                    Log.e(TAG, "Call createIMwqemService but in catch");
                }
            }
        });

        enableMwqemButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.e(TAG, "EnableMwqemButton is Clicked");
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                View enableMwqemPromptView = layoutInflater.inflate(R.layout.EnableMwqemPrompt, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                alertDialogBuilder.setView(enableMwqemPromptView);

                final EditText pref1UidsEditText = (EditText) enableMwqemPromptView.findViewById(R.id.pref1Uids);
                final EditText pref2UidsEditText = (EditText) enableMwqemPromptView.findViewById(R.id.pref2Uids);

                alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ArrayList<Integer> pref1_uids, pref2_uids;
                            pref1_uids = new ArrayList<Integer> ();
                            pref2_uids = new ArrayList<Integer> ();
                            Integer pref1 = new Integer(1);
                            Integer pref2 = new Integer(2);
                            int size1=0, size2=0;
                            int pref1UidsAidl[];
                            int pref2UidsAidl[];
                            Scanner scPref1 = new Scanner(pref1UidsEditText.getText().toString());
                            while (scPref1.hasNextInt()) {
                                pref1_uids.add(scPref1.nextInt());
                                size1+=1;
                            }
                            Scanner scPref2 = new Scanner(pref2UidsEditText.getText().toString());
                            while (scPref2.hasNextInt()) {
                                pref2_uids.add(scPref2.nextInt());
                                size2+=1;
                            }
                            pref1UidsAidl=new int[size1];
                            pref2UidsAidl=new int[size2];
                            for (int i = 0; i < size1; i++)
                            {
                                pref1UidsAidl[i] = pref1_uids.get(i);
                            }
                            for (int i = 0; i < size2; i++)
                            {
                                pref2UidsAidl[i] = pref2_uids.get(i);
                            }
                            for( int i = 0 ; i < size1; i++ ){
                                Log.e("UID of enableMwqem given for Latency Specific is : %s",Integer.toString(pref1_uids.get(i)));
                            }
                            for( int i = 0 ; i < size2; i++ ){
                                Log.e("UID of enableMwqem given for Optimize throughput is : %s",Integer.toString(pref2_uids.get(i)));
                            }
                            try {
                                if (isHidl) {
                                    if (mMwqemServiceHidl != null){
                                        Log.e(TAG, "Calling Enable Mwqem for HIDL for Latency Specific UID's");
                                        mMwqemServiceHidl.enableMwqem(
                                        pref1_uids, pref1.byteValue());
                                        Log.e(TAG, "Calling Enable Mwqem for HIDL for Optimizime Throughput UID's");
                                        mMwqemServiceHidl.enableMwqem(
                                        pref2_uids, pref2.byteValue());
                                    }
                                } else if (isAidl) {
                                      if (mMwqemServiceAidl != null) {
                                            Log.e(TAG, "Calling Enable Mwqem for AIDL for Latency Specific UID's");
                                            mMwqemServiceAidl.enableMwqem(
                                            pref1UidsAidl, pref1.byteValue());
                                            Log.e(TAG, "Calling Enable Mwqem for AIDL for Optimizime Throughput UID's");
                                            mMwqemServiceAidl.enableMwqem(
                                            pref2UidsAidl, pref2.byteValue());
                                      }
                                } else {
                                    Log.e(TAG, "Please register Wifi OEM network first");
                                }
                            } catch (RemoteException e) {
                                Log.e(TAG, "enableMwqem in catch");
                                Log.e(TAG, "enableMwqem calling failed");
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

                    AlertDialog alertEnableMwqem = alertDialogBuilder.create();
                    alertEnableMwqem.show();
            }
        });

        disableMwqemButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                LayoutInflater layoutInflater = LayoutInflater.from(context);
                View disableMwqemPromptView = layoutInflater.inflate(R.layout.DisableMwqemPrompt, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                alertDialogBuilder.setView(disableMwqemPromptView);

                final EditText uidsEditText = (EditText) disableMwqemPromptView.findViewById(R.id.uids);

                alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.e(TAG, "Disable Mwqem Button is clicked");
                            ArrayList<Integer> uids;
                            uids = new ArrayList<Integer> ();
                            int size=0;
                            Scanner sc = new Scanner(uidsEditText.getText().toString());
                            while (sc.hasNextInt()) {
                                uids.add(sc.nextInt());
                                size+=1;
                            }
                            int uids_aidl[];
                            uids_aidl=new int[size];
                            for (int i = 0;i < size;i++){
                                uids_aidl[i] = uids.get(i);
                            }
                            for( int i = 0 ; i < size; i++ ){
                                Log.e("UID of disable Mwqem is : %s",Integer.toString(uids.get(i)));
                            }
                            try {
                                if (isHidl) {

                                    if(mMwqemServiceHidl != null) {
                                        Log.e(TAG, "disableMwqem for HIDL is Called");
                                        mMwqemServiceHidl.disableMwqem(uids);
                                    }
                                } else if (isAidl) {
                                    if(mMwqemServiceAidl != null) {
                                        Log.e(TAG, "disableMwqem for AIDL is Called");
                                        mMwqemServiceAidl.disableMwqem(uids_aidl);
                                    }
                                } else {
                                    Log.e(TAG, "Please register Wifi OEM network first");
                                }
                            } catch (RemoteException e) {
                                Log.e(TAG, "disableMwqem in catch");
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

                    AlertDialog alertDisableMwqem = alertDialogBuilder.create();
                    alertDisableMwqem.show();
            }
        });

        disableMwqemforAllUidsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    if (isHidl) {
                        if (mMwqemServiceHidl != null){
                            Log.e(TAG, "disable Mwqem for All for HIDL is Called");
                            mMwqemServiceHidl.disableMwqemforAllUids();
                        }
                    } else if (isAidl) {
                          if (mMwqemServiceAidl != null){
                              Log.e(TAG, "disable Mwqem for All for AIDL is Called");
                              mMwqemServiceAidl.disableMwqemforAllUids();
                          }
                    }
                    else {
                        Log.e(TAG, "Please register Wifi OEM network first");
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "disableMwqemforAllUids in catch");
                }
            }
        });

    }

    private static class CbResults {
        int status;
        vendor.qti.data.mwqem.V1_0.IMwqemService service;
    }
}
