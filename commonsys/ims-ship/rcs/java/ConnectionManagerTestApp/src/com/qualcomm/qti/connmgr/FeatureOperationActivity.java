/**
 * Copyright (c)2020-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.connmgr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.qualcomm.qti.connmgr.R;
import com.qualcomm.qti.imscmservice.V2_0.outgoingMessage;

import java.util.ArrayList;
import java.util.List;

public class FeatureOperationActivity extends AppCompatActivity {

    final String LOG_TAG = "CMTestApp:FeatureOperationActivity";
    public final static String FEATURE_TAG_NAME = "name";
    public final static String TAB_INSTANCE = "instance";

    int mTabInstance;
    String featureTagName;
    String itemSelected;
    AdapterView connectionAdapterView;
    Spinner ftSpinner;
    static CMConnection connectionObj;
    static CMAidlConnection connAidlObj;
    private static ListView connectionStatusListView;
    static ArrayAdapter<String> ftAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");
        Intent i = this.getIntent();
        featureTagName = i.getStringExtra(FEATURE_TAG_NAME);
        mTabInstance = i.getIntExtra(TAB_INSTANCE, -1);
        setContentView(R.layout.activity_feature_operation);
        connectionObj = MainActivity.globaldata[mTabInstance].
          connectionMap.get(featureTagName);
        connAidlObj = MainActivity.globaldata[mTabInstance].aidlConnectionMap.get(featureTagName);
        TextView ftHeader = findViewById(R.id.ftPageTextView);
        ftHeader.setText(featureTagName);
        Button done_btn = findViewById(R.id.doneButton);
        done_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        connectionStatusListView = findViewById(R.id.ftStatusListView);
        if(connAidlObj != null){
        ftAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                connAidlObj.getConnectionStatusList());
        connectionStatusListView.setAdapter(ftAdapter);
        }
        else{
        ftAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                connectionObj.getConnectionStatusList());
        connectionStatusListView.setAdapter(ftAdapter);
        }
        ftSpinner = findViewById(R.id.ftOperationSpinner);
        ftSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                AdapterView<?> adapterView,
                View view, int i, long l) {
                connectionAdapterView = adapterView;
                itemSelected = adapterView.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        Button ftConnectionButton = findViewById(R.id.ftOperationButton);
        ftConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(itemSelected) {
                    case "Send Message":
                        handleSendMessageSelection(
                            connectionAdapterView.getContext());
                        break;
                    case "Close All Transactions":
                        handleCloseAllTransactionsSelection(
                            connectionAdapterView.getContext());
                        break;
                    case "Set Service Status":
                        handleSetServiceStatusSelection(
                            connectionAdapterView.getContext());
                        break;
                    case "Close Connection":
                        handleCloseConnectionSelection(
                            connectionAdapterView.getContext());
                        break;
                    default:
                        Toast.makeText(
                            connectionAdapterView.getContext(),
                            "Unsupported operation",
                            Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    public void handleSendMessageSelection(Context ctx) {
        ConnectionManagerParser cmParser =
          ConnectionManagerParser.getInstance(ctx);
        Log.d(LOG_TAG,featureTagName+"Send Message");
        cmParser.startParseSipMessage(mTabInstance);
        vendor.qti.ims.imscmaidlservice.OutgoingMessage aidlOutMessage = cmParser.stableAidlMsg;
        if( MainActivity.globaldata[mTabInstance].connAidlMgr != null){
            if(aidlOutMessage!=null){
                if(connAidlObj!=null){
                  if(connAidlObj.isFtRegistered()){
                    boolean status = connAidlObj.sendMessage(
                    aidlOutMessage,
                    MainActivity.globaldata[mTabInstance].userDataArray[mTabInstance]);
                    Log.d(LOG_TAG, "aidl sendMessage :status: " + status);
                  }else{
                    Log.d(LOG_TAG,"ERROR :sendMessage on deRegistered FT");
                    Toast.makeText(connectionAdapterView.getContext(),
                         "FT not registered to send message."+
                         "Trigger Registration and try again",
                         Toast.LENGTH_SHORT).show();
                    String statusText ="ERROR :sendMessage on"+
                       " DeRegistered FT " +
                       "Trigger Registration and retry";
                       connAidlObj.setConnectionStatusList(statusText);
                    ftAdapter.notifyDataSetChanged();
                   }
               }else{
                 Log.d(LOG_TAG, "conn Obj is null");
           }
         }
         else{
                Log.d(LOG_TAG,"sendMessage : parsed aidl outgoingMsg is null");
            }
        }
        else if (MainActivity.globaldata[mTabInstance].connMgr!= null)
        {
            outgoingMessage outMessage = cmParser.msg;
            if (MainActivity.globaldata[mTabInstance].
                connMgrInitialised == 1) {
                //Parse from xml and populate the msg with config params
                if(outMessage!=null){
                    if(connectionObj!=null){
                      if(connectionObj.isFtRegistered()){
                        boolean status = connectionObj.sendMessage(
                        cmParser.msg,
                        MainActivity.globaldata[mTabInstance].
                          userDataArray[mTabInstance]);
                        Log.d(LOG_TAG, "sendMessage :status: " + status);
                      }else{
                        Log.d(LOG_TAG,"ERROR :sendMessage on deRegistered FT");
                        Toast.makeText(connectionAdapterView.getContext(),
                             "FT not registered to send message."+
                             "Trigger Registration and try again",
                             Toast.LENGTH_SHORT).show();
                        String statusText ="ERROR :sendMessage on"+
                           " DeRegistered FT " +
                           "Trigger Registration and retry";
                        connectionObj.setConnectionStatusList(statusText);
                        ftAdapter.notifyDataSetChanged();
                       }
                   }else{
                     Log.d(LOG_TAG, "conn Obj is null");
               }
             }
             else{
                    Log.d(LOG_TAG,"sendMessage : parsed outgoingMsg is null");
                }
            }
        }
    }

    public void handleCloseAllTransactionsSelection(Context ctx) {
        Log.d(LOG_TAG,"CloseAllTransactions on:" + featureTagName);
        if(MainActivity.globaldata[mTabInstance].connAidlMgr != null){
            if(connAidlObj.isFtRegistered()) {
                boolean status = connAidlObj.closeAllTransactions(
                    MainActivity.globaldata[mTabInstance].
                      userDataArray[mTabInstance]);
                Log.d(LOG_TAG, "CloseAllTransactions :status: " + status);
                    String statusText = "CloseAllTransactions :status: "+
                      status;
                      connAidlObj.setConnectionStatusList(statusText);
                    ftAdapter.notifyDataSetChanged();
                }else{
                    Log.d(LOG_TAG,"ERROR :CloseAllTransactions "+
                    "on deRegistered FT");
                    Toast.makeText(connectionAdapterView.getContext(),
                        "FT not registered. Trigger Registration and"+
                        "try again with active transaction",
                        Toast.LENGTH_SHORT).show();
                    String statusText = "ERROR :CloseAllTransactions"+
                         " on DeRegistered FT " +
                         "Trigger Registration and retry "+
                         "with active transaction";
                         connAidlObj.setConnectionStatusList(statusText);
                    ftAdapter.notifyDataSetChanged();
                }
        }
        else if (MainActivity.globaldata[mTabInstance].connMgr!= null) {
            if (MainActivity.globaldata[mTabInstance].
                connMgrInitialised == 1) {
                if(connectionObj.isFtRegistered()) {
                boolean status = connectionObj.closeAllTransactions(
                    MainActivity.globaldata[mTabInstance].
                      userDataArray[mTabInstance]);
                Log.d(LOG_TAG, "CloseAllTransactions :status: " + status);
                    String statusText = "CloseAllTransactions :status: "+
                      status;
                    connectionObj.setConnectionStatusList(statusText);
                    ftAdapter.notifyDataSetChanged();
                }else{
                    Log.d(LOG_TAG,"ERROR :CloseAllTransactions "+
                    "on deRegistered FT");
                    Toast.makeText(connectionAdapterView.getContext(),
                        "FT not registered. Trigger Registration and"+
                        "try again with active transaction",
                        Toast.LENGTH_SHORT).show();
                    String statusText = "ERROR :CloseAllTransactions"+
                         " on DeRegistered FT " +
                         "Trigger Registration and retry "+
                         "with active transaction";
                    connectionObj.setConnectionStatusList(statusText);
                    ftAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    public void handleSetServiceStatusSelection(Context ctx) {
        int statusVal = 1; // 1- Active, 2 - hold, 3-inactive, 0 -none
        Log.d(LOG_TAG,"SetServiceStatus on:" + featureTagName);
        if(MainActivity.globaldata[mTabInstance].connAidlMgr != null){
            if(connAidlObj.isFtRegistered()) {
                boolean status = connAidlObj.setStatus(statusVal);
                Log.d(LOG_TAG, "SetServiceStatus :status: " + status);
                String statusText = "SetServiceStatus :status: " + status;
                connAidlObj.setConnectionStatusList(statusText);
                ftAdapter.notifyDataSetChanged();
            }else{
                Log.d(LOG_TAG,"ERROR :sendMessage on deRegistered FT");
                Toast.makeText(connectionAdapterView.getContext(),
                    "FT not registered to send message."+
                    "Trigger Registration and try again",
                    Toast.LENGTH_SHORT).show();
                String statusText = "ERROR :sendMessage on DeRegistered FT " +
                                    "Trigger Registration and retry";
                connAidlObj.setConnectionStatusList(statusText);
                ftAdapter.notifyDataSetChanged();
            }
        }
        else if (MainActivity.globaldata[mTabInstance].connMgr!= null &&
            MainActivity.globaldata[mTabInstance].connMgrInitialised == 1) {
            if(connectionObj.isFtRegistered()) {
                boolean status = connectionObj.setStatus(statusVal);
                Log.d(LOG_TAG, "SetServiceStatus :status: " + status);
                String statusText = "SetServiceStatus :status: " + status;
                connectionObj.setConnectionStatusList(statusText);
                ftAdapter.notifyDataSetChanged();
            }else{
                Log.d(LOG_TAG,"ERROR :sendMessage on deRegistered FT");
                Toast.makeText(connectionAdapterView.getContext(),
                    "FT not registered to send message."+
                    "Trigger Registration and try again",
                    Toast.LENGTH_SHORT).show();
                String statusText = "ERROR :sendMessage on DeRegistered FT " +
                                    "Trigger Registration and retry";
                connectionObj.setConnectionStatusList(statusText);
                ftAdapter.notifyDataSetChanged();
            }
        }
    }

    public void handleCloseConnectionSelection(Context ctx) {
        boolean status = false;
        Log.d(LOG_TAG,"CloseConnection on:" + featureTagName);
        if(MainActivity.globaldata[mTabInstance].connAidlMgr != null){
            handleCloseAllTransactionsSelection(ctx);
            status = connAidlObj.closeConnection(
                MainActivity.globaldata[mTabInstance].serviceHandle);
            Log.d(LOG_TAG, "CloseConnection :status: " + status);
            if(status == true){
              Button bckButton = findViewById(R.id.doneButton);
              bckButton.setVisibility(View.VISIBLE);

              Log.d(LOG_TAG, "CloseConnection connectionMapSize :"+
                  MainActivity.globaldata[mTabInstance].
                  connectionMap.size());
              updateStatusOnCloseAidlConnSuccess(mTabInstance,
              connAidlObj, featureTagName);

              Toast.makeText(connectionAdapterView.getContext(),
                "CloseConnection Status: " + status +
                "\n Please Click Back button to return to main screen",
                Toast.LENGTH_SHORT).show();
            }
        }
        else if (MainActivity.globaldata[mTabInstance].connMgr!= null) {
            if (MainActivity.globaldata[mTabInstance].
                connMgrInitialised == 1) {
                //closeAllTransactions
                handleCloseAllTransactionsSelection(ctx);
                status = connectionObj.closeConnection(
                    MainActivity.globaldata[mTabInstance].serviceHandle);
                Log.d(LOG_TAG, "CloseConnection :status: " + status);
                if(status == true){
                  Button bckButton = findViewById(R.id.doneButton);
                  bckButton.setVisibility(View.VISIBLE);

                  Log.d(LOG_TAG, "CloseConnection connectionMapSize :"+
                      MainActivity.globaldata[mTabInstance].
                      connectionMap.size());
                  updateStatusOnCloseConnSuccess(mTabInstance,
                                       connectionObj, featureTagName);

                  Toast.makeText(connectionAdapterView.getContext(),
                    "CloseConnection Status: " + status +
                    "\n Please Click Back button to return to main screen",
                    Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static void updateStatusOnCloseConnSuccess(int tabInstance,
                                         CMConnection connObj, String ftName) {
                  MainActivity.globaldata[tabInstance].connectionMap.
                      remove(ftName, connObj);
                  MainActivity.globaldata[tabInstance].connectionSpinnerList.
                      add(ftName);
                  MainActivity.globaldata[tabInstance].
                      registeredConnectionSpinnerList.
                      remove(ftName);
                  MainActivity.mSectionsPagerAdapter.mSubsTabs[tabInstance].
                      updateRegisteredFtSpinnerElement(
                        MainActivity.globaldata[tabInstance].
                          primaryScreenView);
                  MainActivity.mSectionsPagerAdapter.mSubsTabs[tabInstance].
                      updateFTSpinnerElement(
                        MainActivity.globaldata[tabInstance].
                          primaryScreenView);

                  String statusText = "CloseConnection :status: true";
                  connObj.setConnectionStatusList(statusText);
                  ftAdapter.notifyDataSetChanged();
                  String cmStatus ="Close Connection FT[ "+ftName +"]";
                  MainActivity.globaldata[tabInstance].connMgrStatusList.
                      add(cmStatus);
                  int spinnerListSize = MainActivity.globaldata[tabInstance].
                    registeredConnectionSpinnerList.size();
                  String regFT ="Registered FT: ";
                  for(int i=0;i<spinnerListSize;i++){
                    regFT = regFT.concat(
                        MainActivity.globaldata[tabInstance].
                        registeredConnectionSpinnerList.get(i));
                  }
                  MainActivity.globaldata[tabInstance].connMgrStatusList.
                      add(regFT);
                  MainActivity.mSectionsPagerAdapter.mSubsTabs[tabInstance].
                      statusAdapter.notifyDataSetChanged();

    }

    public static void updateStatusOnCloseAidlConnSuccess(int tabInstance,
                                         CMAidlConnection connObj, String ftName) {
                  MainActivity.globaldata[tabInstance].aidlConnectionMap.
                      remove(ftName, connObj);
                  MainActivity.globaldata[tabInstance].connectionSpinnerList.
                      add(ftName);
                  MainActivity.globaldata[tabInstance].
                      registeredConnectionSpinnerList.
                      remove(ftName);
                  MainActivity.mSectionsPagerAdapter.mSubsTabs[tabInstance].
                      updateRegisteredFtSpinnerElement(
                        MainActivity.globaldata[tabInstance].
                          primaryScreenView);
                  MainActivity.mSectionsPagerAdapter.mSubsTabs[tabInstance].
                      updateFTSpinnerElement(
                        MainActivity.globaldata[tabInstance].
                          primaryScreenView);

                  String statusText = "CloseConnection :status: true";
                  connObj.setConnectionStatusList(statusText);
                  ftAdapter.notifyDataSetChanged();
                  String cmStatus ="Close Connection FT[ "+ftName +"]";
                  MainActivity.globaldata[tabInstance].connMgrStatusList.
                      add(cmStatus);
                  int spinnerListSize = MainActivity.globaldata[tabInstance].
                    registeredConnectionSpinnerList.size();
                  String regFT ="Registered FT: ";
                  for(int i=0;i<spinnerListSize;i++){
                    regFT = regFT.concat(
                        MainActivity.globaldata[tabInstance].
                        registeredConnectionSpinnerList.get(i));
                  }
                  MainActivity.globaldata[tabInstance].connMgrStatusList.
                      add(regFT);
                  MainActivity.mSectionsPagerAdapter.mSubsTabs[tabInstance].
                      statusAdapter.notifyDataSetChanged();

    }
}
