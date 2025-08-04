/**
 * Copyright (c)2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

 package com.qualcomm.qti.connmgr;

 import android.os.Handler;
 import android.os.HandlerThread;
 import android.os.Looper;
 import android.os.RemoteException;
 import android.view.View;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import java.net.InetAddress;
 import java.net.Socket;
 import java.util.HashMap;
 
 import javax.net.SocketFactory;
 
 import android.util.Log;
 import android.net.ConnectivityManager;
 import android.content.Context;
 import android.net.NetworkInfo;
 
 import com.qualcomm.qti.connmgr.R;
 import vendor.qti.ims.imscmaidlservice.AutoConfig;
 import vendor.qti.ims.imscmaidlservice.IImsCmServiceListener;
 import vendor.qti.ims.imscmaidlservice.ConfigData;
 import vendor.qti.ims.imscmaidlservice.KeyValuePairStringType;
 import vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType;
 import vendor.qti.ims.imscmaidlservice.UserConfigKeys;
 import vendor.qti.ims.imscmaidlservice.DeviceConfigKeys;
 import vendor.qti.ims.imscmaidlservice.AutoConfigResponse;
 import vendor.qti.ims.imscmaidlservice.ServiceStatus;
 
 public class ConnectionManagerAidlListenerImpl extends IImsCmServiceListener.Stub {
     final static String TAG = "CMTestApp:ConnectionManagerAidlListenerImpl";
     private long listenerId = 0;
     private final View mStatusView;
     int mTabInstance = -1;
     Handler mHandler = null;
 
     public ConnectionManagerAidlListenerImpl(int instance, View statusView) {
       mTabInstance = instance;
       mStatusView = statusView;
       // UI Thread
       mHandler = new Handler(Looper.getMainLooper());
                 }
 
     public void setListenerId(long listenerId){
       this.listenerId = listenerId;
     }
     public long getListenerId() {
       return this.listenerId;
     }
     /*onStatusChange---- This callback is invoked when there
       is a change in the status of the IMS Connection Manager.
     */
     public void onStatusChange(int status)  {
       Log.d(TAG, "onStatusChange status is "+status);
       switch(status) {
         case ServiceStatus.STATUS_SUCCESS:
         {
           Runnable msg = new Runnable() {
             @Override
             public void run() {
               String status = "Create Connection Manager Status: SUCCESS";
               MainActivity.globaldata[mTabInstance].
               connMgrStatusList.add(status);
               MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                         statusAdapter.notifyDataSetChanged();
               MainActivity.globaldata[mTabInstance].connMgrInitialised = 1;
               Log.d(TAG, "onStatusChange ConnectionManager is initialised");
             }
           };
           mHandler.post(msg);
         }
         break;
         case ServiceStatus.STATUS_FAILURE:
         {
           Runnable msg = new Runnable() {
             @Override
             public void run() {
               String status = "Create Connection Manager Status: FAILURE";
               MainActivity.globaldata[mTabInstance].
               connMgrStatusList.add(status);
              /* MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                             updateCMStatusViewElement(mStatusView);*/
               MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                             statusAdapter.notifyDataSetChanged();
             }
           };
           mHandler.post(msg);
         }
         break;
         case ServiceStatus.STATUS_SERVICE_CLOSED:
         {
           Runnable msg = new Runnable() {
             @Override
             public void run() {
               String status = "Connection Manager Status: SERVICE_CLOSED";
               MainActivity.globaldata[mTabInstance].connMgrStatusList.
                 add(status);
               MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                          statusAdapter.notifyDataSetChanged();
               handleServiceClosedStatusChange();
             }
           };
           mHandler.post(msg);
         }
         break;
         default:
         Log.d(TAG, "onStatusChange unhandled case");
         break;
       }
     }
 
     private void handleServiceClosedStatusChange() {
       Log.d(TAG, "handleStatusChangeServiceClosed: "+
         "ConnectionManager service closed");
       MainActivity.globaldata[mTabInstance].connMgrInitialised = 0;
       MainActivity.globaldata[mTabInstance].aidlConnectionMap.clear();
     }
 
     /*onConfigurationChange---- Callback invoked
     when the configurations are updated*/
     public void onConfigurationChange(ConfigData config, int userdata) {
       if(config != null) {
         if(MainActivity.globaldata[mTabInstance].userData == userdata) {
             Log.d(TAG, "onConfigurationChange: on Sub :"+ mTabInstance);
             Log.d(TAG, "onConfigurationChange: userData :"+ userdata);
           }
 
         MainActivity.globaldata[mTabInstance].aidlConfigdata = config;
         if (config.userConfigData != null) {
           if(config.userConfigData.data.length != 0)  {
             MainActivity.globaldata[mTabInstance].userConfigData.clear();
             for(KeyValuePairStringType var : config.userConfigData.data) {
               Log.d(TAG, "userConfig: Key[" + 
                 var.key +
                 "] Value[" + var.value + "]");
               MainActivity.globaldata[mTabInstance].userConfigData.
                 put(var.key, var.value);
             }
           }
           else {
             Log.d(TAG, "userConfig: no Data received");
           }
         }
         if (config.deviceConfigData != null) {
           if(config.deviceConfigData.data.length != 0)  {
             MainActivity.globaldata[mTabInstance].deviceConfigData.clear();
             for(KeyValuePairStringType var : config.deviceConfigData.data) {
               Log.d(TAG, "deviceConfigData: Key[" +
               var.key +
                 "] Value[" + var.value + "]");
               MainActivity.globaldata[mTabInstance].deviceConfigData.
                 put(var.key, var.value);
             }
           }
           else {
             Log.d(TAG, "deviceConfigData: no Data received");
           }
         }
         if (config.autoConfigData != null) {
           MainActivity.globaldata[mTabInstance].aidlAutoConfigData =
             config.autoConfigData;
           Log.d(TAG, "autoConfig" +
                   MainActivity.globaldata[mTabInstance].aidlAutoConfigData.
                     autoConfigRequestType + "\n" +
                   MainActivity.globaldata[mTabInstance].aidlAutoConfigData.
                     autoConfigXml);
           Log.d(TAG, "autoConfig via toString()" +
                   MainActivity.globaldata[mTabInstance].aidlAutoConfigData.
                     toString());
         }
       }
       else {
         Log.d(TAG, "ConfigData is null");
       }
       Runnable msg = new Runnable() {
         @Override
         public void run() {
           Log.d(TAG, "onConfigurationChange config data via toString()" +
                 MainActivity.globaldata[mTabInstance].aidlConfigdata.toString());
           Toast.makeText(MainActivity.mCtx,
                         "onConfigurationChange called",
                         Toast.LENGTH_SHORT).show();
     }
       };
       mHandler.post(msg);
     }
 
     /*onConfigurationChange_2_1---- Callback invoked
     when the configurations are updated
     public void onConfigurationChange_2_1(ConfigData config, int userdata) {
       if(config != null) {
         if(MainActivity.globaldata[mTabInstance].userData == userdata) {
           Log.d(TAG, "onConfigurationChange_2_1: on Sub :"+ mTabInstance);
             Log.d(TAG, "onConfigurationChange_2_1: userData :"+ userdata);
           }
 
         MainActivity.globaldata[mTabInstance].configdata = config;
         if (config.userConfigData != null) {
           if(!config.userConfigData.data.isEmpty())  {
             MainActivity.globaldata[mTabInstance].userConfigData.clear();
             for(KeyValuePairStringType var : config.userConfigData.data) {
               Log.d(TAG, "userConfig: Key[" + UserConfigKeys.
                 toString(var.key) +
                 "] Value[" + var.value + "]");
               MainActivity.globaldata[mTabInstance].userConfigData.
                 put(var.key, var.value);
             }
           }
           else {
             Log.d(TAG, "userConfig: no Data received");
           }
         }
         if (config.deviceConfigData != null) {
           if(!config.deviceConfigData.data.isEmpty())  {
             MainActivity.globaldata[mTabInstance].deviceConfigData.clear();
             for(KeyValuePairStringType var : config.deviceConfigData.data) {
               Log.d(TAG, "deviceConfigData: Key[" + deviceConfigKeys.
                 toString(var.key) +
                 "] Value[" + var.value + "]");
               MainActivity.globaldata[mTabInstance].deviceConfigData.
                 put(var.key, var.value);
             }
           }
           else {
             Log.d(TAG, "deviceConfigData: no Data received");
           }
         }
         if (config.autoConfigData != null) {
           MainActivity.globaldata[mTabInstance].autoConfigData =
             config.autoConfigData;
           Log.d(TAG, "autoConfig" +
                   MainActivity.globaldata[mTabInstance].autoConfigData.
                     autoConfigRequestType + "\n" +
                   MainActivity.globaldata[mTabInstance].autoConfigData.
                     autoConfigXml);
           Log.d(TAG, "autoConfig via toString()" +
                   MainActivity.globaldata[mTabInstance].autoConfigData.
                     toString());
         }
       }
       else {
         Log.d(TAG, "ConfigData is null");
       }
 
       Runnable msg = new Runnable() {
         @Override
         public void run() {
           Log.d(TAG, "onConfigurationChange2_1 config data via toString()" +
                 MainActivity.globaldata[mTabInstance].configdata.toString());
           Toast.makeText(MainActivity.mCtx,
                         "onConfigurationChange2_1 called",
                         Toast.LENGTH_SHORT).show();
     }
       };
       mHandler.post(msg);
       }
 
     /*onConfigurationChange_2_2---- Callback
     invoked when the configurations are updated
     public void onConfigurationChange_2_2(ConfigData config, int userdata) {
       if(config != null) {
         if(MainActivity.globaldata[mTabInstance].userData == userdata) {
           Log.d(TAG, "onConfigurationChange_2_2: on Sub :"+ mTabInstance);
             Log.d(TAG, "onConfigurationChange_2_2: userData :"+ userdata);
           }
 
         MainActivity.globaldata[mTabInstance].configdata = config;
         if (config.userConfigData != null) {
           if(!config.userConfigData.data.isEmpty())  {
             MainActivity.globaldata[mTabInstance].userConfigData.clear();
             for(KeyValuePairStringType var : config.userConfigData.data) {
               Log.d(TAG, "userConfig: Key[" + UserConfigKeys.
                 toString(var.key) +
                 "] Value[" + var.value + "]");
               MainActivity.globaldata[mTabInstance].userConfigData.
                 put(var.key, var.value);
             }
           }
           else {
             Log.d(TAG, "userConfig: no Data received");
           }
         }
         if (config.deviceConfigData != null) {
           if(!config.deviceConfigData.data.isEmpty())  {
             MainActivity.globaldata[mTabInstance].deviceConfigData.clear();
             for(KeyValuePairStringType var : config.deviceConfigData.data) {
               Log.d(TAG, "deviceConfigData: Key[" + deviceConfigKeys.
                 toString(var.key) +
                 "] Value[" + var.value + "]");
               MainActivity.globaldata[mTabInstance].deviceConfigData.
                 put(var.key, var.value);
             }
           }
           else {
             Log.d(TAG, "deviceConfigData: no Data received");
           }
         }
 
         if (config.autoConfigData != null) {
           MainActivity.globaldata[mTabInstance].autoConfigData =
             config.autoConfigData;
           Log.d(TAG, "autoConfig" +
                   MainActivity.globaldata[mTabInstance].autoConfigData.
                     autoConfigRequestType + "\n" +
                   MainActivity.globaldata[mTabInstance].autoConfigData.
                     autoConfigXml);
           Log.d(TAG, "autoConfig via toString()" +
                   MainActivity.globaldata[mTabInstance].autoConfigData.
                     toString());
         }
       }
       else {
         Log.d(TAG, "ConfigData is null");
       }
 
       Runnable msg = new Runnable() {
         @Override
         public void run() {
           Log.d(TAG, "onConfigurationChange2_2 config data via toString()" +
                 MainActivity.globaldata[mTabInstance].configdata.toString());
           Toast.makeText(MainActivity.mCtx,
                   "onConfigurationChange2_2 called",
                   Toast.LENGTH_SHORT).show();
         }
       };
       mHandler.post(msg);
     }
 */
     /*onCommandStatus---- Callback invoked to notify
     the clients the status of request placed.*/

     @Override
      public final String getInterfaceHash() { 
        return IImsCmServiceListener.HASH; 
      }

      @Override
      public final int getInterfaceVersion() { 
        return IImsCmServiceListener.VERSION; 
      }

     public void onCommandStatus(int userData, int status) {
       if (MainActivity.globaldata[mTabInstance].userData == userData) {
           Log.d(TAG, "onCommandStatus: on Sub :" + mTabInstance);
           Log.d(TAG, "onCommandStatus: userData :" + userData);
           Log.d(TAG, "onCommandStatus: status :" + status);
         }
 
       Runnable msg = new Runnable() {
         @Override
         public void run() {
           Toast.makeText(MainActivity.mCtx,
                         "onCommandStatus : userData[" +
                         userData + "] Status:[" + status + "]",
                         Toast.LENGTH_SHORT).show();
           String statusText = "onCommandStatus: userData[" +
                           userData + "] Status:[" + status + "]";
           MainActivity.globaldata[mTabInstance].
             connMgrStatusList.add(statusText);
           MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
           statusAdapter.notifyDataSetChanged();
       }
       };
       mHandler.post(msg);
     }
     /*onServiceReady---- Callback invoked upon successful service creation.*/
     public void onServiceReady(long serviceHandle, int userData, int status) {
       if(MainActivity.globaldata[mTabInstance].userData == userData) {
           MainActivity.globaldata[mTabInstance].connMgrInitialised = 1 ;
           MainActivity.globaldata[mTabInstance].serviceHandle = serviceHandle;
           Log.d(TAG, "onServiceReady: serviceHandle :"+ serviceHandle);
           Log.d(TAG, "onServiceReady: userData :"+ userData);
           Log.d(TAG, "onServiceReady: status :"+ status);
       }
       Runnable msg = new Runnable() {
         @Override
         public void run() {
           String status = "Connection Manager: onServiceReady";
           MainActivity.globaldata[mTabInstance].connMgrStatusList.add(status);
           MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                   statusAdapter.notifyDataSetChanged();
     }
       };
       mHandler.post(msg);
     }
 
     public void onAcsConnectionStatusChange(int userData, int status) {
       if(MainActivity.globaldata[mTabInstance].userData == userData) {
           Log.d(TAG, "onAcsConnectionStatusChange: on Sub :"+ mTabInstance);
             Log.d(TAG, "onAcsConnectionStatusChange: userData :"+ userData);
             Log.d(TAG, "onAcsConnectionStatusChange: status :"+ status);
           }
 
       Runnable msg = new Runnable() {
         @Override
         public void run() {
           String statusText = "onAcsConnectionStatusChange Status:[" +
                               status + "]";
           MainActivity.globaldata[mTabInstance].connMgrStatusList.
                   add(statusText);
           MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                   statusAdapter.notifyDataSetChanged();
         }
       };
       mHandler.post(msg);
     }
 
     public void onAutoConfigResponse(AutoConfigResponse acsResponse) {
       MainActivity.globaldata[mTabInstance].aidlAcsResponse = acsResponse;
       Log.d(TAG, "onAutoConfigResponse: statusCode:"+
             acsResponse.statusCode);
       Log.d(TAG, "onAutoConfigResponse: reasonPhrase :"+
             acsResponse.reasonPhrase);
       Runnable msg = new Runnable() {
         @Override
         public void run() {
           String status = "onAutoConfigResponse : statusCode[" +
                           acsResponse.statusCode +
                           "] reasonPhrase:[" +
                           acsResponse.reasonPhrase + "]";
           MainActivity.globaldata[mTabInstance].connMgrStatusList.add(status);
           MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
               statusAdapter.notifyDataSetChanged();
         }
       };
       mHandler.post(msg);
     }
 }
 