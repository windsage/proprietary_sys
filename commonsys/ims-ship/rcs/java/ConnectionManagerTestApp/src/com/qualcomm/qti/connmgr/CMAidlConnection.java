/**
 * Copyright (c)2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.connmgr;

import android.os.RemoteException;
import android.util.Log;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import vendor.qti.ims.imscmaidlservice.IImsCMConnectionListener;
import vendor.qti.ims.imscmaidlservice.IImsCMConnection;
import vendor.qti.ims.imscmaidlservice.ConnectionEvent;
import vendor.qti.ims.imscmaidlservice.ConnectionEventData;
import vendor.qti.ims.imscmaidlservice.MessageType;
import vendor.qti.ims.imscmaidlservice.IncomingMessage;
import vendor.qti.ims.imscmaidlservice.IncomingMessageKeys;
import vendor.qti.ims.imscmaidlservice.KeyValuePairStringType;
import vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType;
import vendor.qti.ims.imscmaidlservice.OutgoingMessage;
import vendor.qti.ims.imscmaidlservice.OutgoingMessageKeys;
import vendor.qti.ims.imscmaidlservice.UserConfigKeys;
import vendor.qti.ims.imscmaidlservice.DeviceConfigKeys;

public class CMAidlConnection {
    final static String LOG_TAG = "CMTestApp:Connection";
    public static final int VERSION_2_0 = 0;
    public static final int VERSION_2_1 = 1;
    public static final int VERSION_2_2 = 2;

    vendor.qti.ims.imscmaidlservice.IImsCMConnection connAidlObj = null;
    long connectionHandle = 0;
    int mTabInstance = -1;
    boolean isFtRegistered = false;
    ConnectionAidlListenerImpl mAidlConnListener = new ConnectionAidlListenerImpl();
    String mTagName = "";
    String mUriStr = "";
    List<String> mConnectionStatusList = new ArrayList<String>();
    View mCmServiceView;

    private class ConnectionAidlListenerImpl extends
           IImsCMConnectionListener.Stub {
        final static String TAG = "CMTestApp:ConnectionAidlListenerImpl";

        private long listenerToken = 0;
        Handler mHandler = null;

        public void setListenerToken(long listenerToken) {
          this.listenerToken = listenerToken;
        }
        public long getListenerToken() {
          return this.listenerToken;
        }
        public ConnectionAidlListenerImpl() {
          mHandler = new Handler(Looper.getMainLooper());
        }
        /*handleEventReceived: Callback function to inform clients about a
          registration status change, changes in service allowed by policy
          manager because of a RAT change,and any forceful terminations
          of the connection object by the QTI framework because of PDP
          status changes.
        */
        @Override
        public void onEventReceived(ConnectionEventData event) {
          Log.d(TAG, "onEventReceived event is "+event.toString());
          CMAidlConnection connObj = CMAidlConnection.this;
          if(event.eEvent == ConnectionEvent.SERVICE_REGISTERED) {
            Log.d(TAG, "onEventReceived Connection for FT: " +
                       mTagName +
                       "\t is Registered: on Sub" +
                       mTabInstance);

            isFtRegistered = true;

            if(!MainActivity.globaldata[mTabInstance].
            registeredConnectionSpinnerList.contains(mTagName)){
              MainActivity.globaldata[mTabInstance].
              registeredConnectionSpinnerList.add(mTagName);
            }
          }
          else {
            isFtRegistered = false;
            if(event.eEvent == ConnectionEvent.SERVICE_CREATED) {
              if(MainActivity.globaldata[mTabInstance].
              connectionSpinnerList.contains(mTagName)){
              MainActivity.globaldata[mTabInstance].
              connectionSpinnerList.remove(mTagName);
              }
            }else if(event.eEvent == ConnectionEvent.SERVICE_NOTREGISTERED) {
              //TO CHECK
              //MainActivity.globaldata[mTabInstance].
              //registeredConnectionSpinnerList.remove(mTagName);
              Log.d(TAG,"ConnectionEvent.SERVICE_NOT_REGISTERED");
            }
          }
          Runnable msg = new Runnable() {
            @Override
            public void run() {
              if(event.eEvent == ConnectionEvent.SERVICE_REGISTERED) {
                Toast.makeText(MainActivity.mCtx,
                        "onEventReceived: SERVICE_REGISTERED For FT[" +
                        mTagName + "]",
                        Toast.LENGTH_SHORT).show();

                String status = "Registered FT: ["+
                                mTagName +
                                "]";
                MainActivity.globaldata[mTabInstance].
                connMgrStatusList.add(status);
                MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                statusAdapter.notifyDataSetChanged();
                MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                    updateRegisteredFtSpinnerElement(mCmServiceView);
              }else if(event.eEvent == ConnectionEvent.SERVICE_CREATED) {
                Toast.makeText(MainActivity.mCtx,
                        "onEventReceived: SERVICE_CREATED For FT[" +
                        mTagName + "]",
                        Toast.LENGTH_SHORT).show();
                MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                                 updateFTSpinnerElement(mCmServiceView);
              }else if(event.eEvent == ConnectionEvent.SERVICE_NOTREGISTERED) {
                Toast.makeText(MainActivity.mCtx,
                        "onEventReceived: SERVICE_NOTREGISTERED For FT["+
                        mTagName + "]",
                        Toast.LENGTH_SHORT).show();
                String status = "Deregistered FT: ["+
                                 mTagName +
                                "]";
                MainActivity.globaldata[mTabInstance].
                  connMgrStatusList.add(status);
                MainActivity.mSectionsPagerAdapter.mSubsTabs[mTabInstance].
                statusAdapter.notifyDataSetChanged();
              }else if(event.eEvent == ConnectionEvent.SERVICE_FORCEFUL_CLOSE ||
                       event.eEvent == ConnectionEvent.SERVICE_TERMINATE_CONNECTION){
                //All other events:
                //SERVICE_FORCEFUL_CLOSE,
                //SERVICE_TERMINATE_CONNECTION
                connObj.closeConnection(
                MainActivity.globaldata[mTabInstance].serviceHandle);

                FeatureOperationActivity.
                updateStatusOnCloseAidlConnSuccess(mTabInstance, connObj, mTagName);
              } else {
                // Do nothing for SERVICE_NOTALLOWED. explicitly call
                // close connection from UI
              }
            }
          };
          mHandler.post(msg);
        }

        /*handleIncomingMessage: This
        callback indicates the incoming message to the client.*/
        @Override
        public void handleIncomingMessage(IncomingMessage data) {
          String sipMessage = "";
          Log.d(TAG, "handleIncomingMessage: on Sub" + mTabInstance);
          if(data.data.length > 0) {
            for(KeyValuePairStringType var : data.data) {
              Log.d(TAG, "handleIncomingMessage: key[" +
               var.key +
                          "] Value[" + var.value +"]");
            }
          }
          else {
            Log.d(TAG, "handleIncomingMessage: no String Type data received");
          }
          //TextViewobj.setText(ImcomignMsg);

          if(data.bufferData.length > 0) {
            for(KeyValuePairBufferType var : data.bufferData) {
                char[] byteData = new char[var.value.length];
                for(int i=0; i< var.value.length; i++)
                {
                    byteData[i] = (char)var.value[i];
                }
                sipMessage = new String(byteData);
                Log.d(TAG, "handleIncomingMessage: key[" +
                 var.key +
                "] Value[" + sipMessage +"]");
            }
          }
          else {
            Log.d(TAG, "handleIncomingMessage: no Buffer Type data received");
          }

          final String sipPacket = sipMessage;
          Runnable msg = new Runnable() {
            @Override
            public void run() {
              Toast.makeText(MainActivity.mCtx,
                      "handleIncomingMessage: For instance[" +
                      mTabInstance + "]",
                      Toast.LENGTH_SHORT).show();
              OutgoingMessage msgResponse = parseIncomingMessage(sipPacket);
              if(msgResponse != null){
                  sendMessage(
                    msgResponse,
                    MainActivity.globaldata[mTabInstance].
                     userDataArray[mTabInstance]);
              }
            }
          };
          mHandler.post(msg);

        }

        /*onCommandStatus: Status of the sendMessage
          (whether or not the message was transmitted to network)
          is returned asynchronously via
          the onCommandStatus callback with messageID as a parameter.
        */
        @Override
        public void onCommandStatus(int status, int userdata) {
          for(int i=0; i<=1; i++) {
            if(MainActivity.globaldata[mTabInstance].userDataArray[i] == userdata) {
              Log.d(TAG, "onCommandStatus: on Sub" + i);
              Log.d(TAG, "onCommandStatus status is ["+
                status + "] userdata " + userdata);
            }
          }
          Runnable msg = new Runnable() {
            @Override
            public void run() {
              Toast.makeText(MainActivity.mCtx,
                  "onCommandStatus: status " + status +
                  "For instance[" + mTabInstance +
                  "] userdata: " + userdata,
                  Toast.LENGTH_SHORT).show();
            }
          };
          mHandler.post(msg);
        }

        @Override
        public final String getInterfaceHash() { return IImsCMConnectionListener.HASH; }

        @Override
        public final int getInterfaceVersion() { return IImsCMConnectionListener.VERSION; }

        private OutgoingMessage parseIncomingMessage(String msgString) {
          OutgoingMessage msg = new OutgoingMessage();
          int msgId=0;
          List<KeyValuePairStringType> msgList = new ArrayList<>();
          String response = "";
          String[] headers = msgString.split("\r\n");
          String fromHeader ="";
          String cseqHeader ="";  
          String callidHeader ="";
          String senderBranch ="";
          String protocol ="";
          String contactHeader ="";
          String portHeader ="";
          String status = "Incoming Msg["+ headers[0] + "]";
          mConnectionStatusList.add(status);
          FeatureOperationActivity.ftAdapter.notifyDataSetChanged();
          Log.d(LOG_TAG,"parseIncomingMessage: Num of headers is ["+
                headers.length+"]");

          if(headers[0].contains("MESSAGE ")){
            for(int i=0; i<headers.length; i++){
              if(headers[i].contains("From: ")) {
                int left = 6;
                fromHeader = headers[i].substring(left, headers[i].length());
                Log.d(LOG_TAG,"From: "+fromHeader);
              } else if(headers[i].contains("CSeq: ")) {
                int left = 6;
                cseqHeader = headers[i].substring(left, headers[i].length());
                Log.d(LOG_TAG,"cseq: "+cseqHeader);
              } else if(headers[i].contains("Call-ID: ")) {
                int left = 9;
                callidHeader = headers[i].substring(left, headers[i].length());
                Log.d(LOG_TAG,"callid: "+callidHeader);
                int leftIndex = headers[i].indexOf("@");
                leftIndex +=1;
                contactHeader = headers[i].substring(
                   leftIndex, headers[i].length());
                Log.d(LOG_TAG,"contactHeader: "+contactHeader);
              } else if(headers[i].contains("Via: ")) {
                int left = headers[i].indexOf("branch=");
                left += 7;
                senderBranch = headers[i].substring(left, headers[i].length());
                Log.d(LOG_TAG,"branch: "+senderBranch);
                int leftindex = 13;
                int right = headers[i].indexOf("[");
                protocol = headers[i].substring(leftindex, right);
                Log.d(LOG_TAG,"protocol: "+protocol);
                int portleftIndex = headers[i].indexOf("]:");
                portleftIndex +=2;
                int portrightIndex = headers[i].indexOf(";branch");
                portHeader = headers[i].substring(
                  portleftIndex, portrightIndex);
                Log.d(LOG_TAG,"portHeader: "+portHeader);
              }
            }
            String lcprotocol = protocol.toLowerCase();
            lcprotocol = lcprotocol.replaceAll("\\s", "");
            String localIpAddress = MainActivity.globaldata[mTabInstance].
              userConfigData.get(UserConfigKeys.LocalHostIPAddress);
            String outboundProxy = MainActivity.globaldata[mTabInstance].
              deviceConfigData.get(DeviceConfigKeys.StrSipOutBoundProxyName);
            String outboundProxyPort = MainActivity.globaldata[mTabInstance].
              deviceConfigData.get(DeviceConfigKeys.SipOutBoundProxyPort);
            String publicUserId = MainActivity.globaldata[mTabInstance].
              userConfigData.get(UserConfigKeys.SipPublicUserId);
            response = "SIP/2.0 200 OK\r\n" +
              "Via: SIP/2.0/"+ protocol + "[" + localIpAddress + "]:" +
              outboundProxyPort + ";branch=" + senderBranch + "\r\n" +
              "Contact: <sip:[" + contactHeader + "]:" + portHeader +
              ";transport=" +lcprotocol+">\r\n" +
              "To: " + fromHeader + "\r\n"+
              "From: <" + publicUserId + ">;tag=3476455352\r\n" +
              "Call-ID: " + callidHeader +"\r\n"+
              "CSeq: " + cseqHeader + "\r\n" +
              "Content-Length: 0\r\n\r\n";

            KeyValuePairStringType callIdType = new KeyValuePairStringType();
            callIdType.key = OutgoingMessageKeys.CallId;
            callIdType.value = callidHeader;
            msgList.add(callIdType);
            //msg.data[msgId++] = callIdType;

            KeyValuePairStringType messagetypeVal = new
              KeyValuePairStringType();
            messagetypeVal.key = OutgoingMessageKeys.MessageType;
            messagetypeVal.value = Integer.toString(MessageType.TYPE_RESPONSE);
            msgList.add(messagetypeVal);
            //msg.data[msgId++] = messagetypeVal;

            KeyValuePairStringType protocolType = new
              KeyValuePairStringType();
            protocolType.key = OutgoingMessageKeys.Protocol;
            protocolType.value = protocol.equals("UDP")? "0" : "1";
            msgList.add(protocolType);
            //msg.data[msgId++] = protocolType;

            KeyValuePairStringType outboundProxyVal = new
              KeyValuePairStringType();
            outboundProxyVal.key = OutgoingMessageKeys.OutboundProxy;
            outboundProxyVal.value = "["+ outboundProxy+ "]";
            msgList.add(outboundProxyVal);
           // msg.data[msgId++] = outboundProxyVal;

            KeyValuePairStringType remotePortVal = new
              KeyValuePairStringType();
            remotePortVal.key = OutgoingMessageKeys.RemotePort;
            remotePortVal.value = Integer.toString(5060);
            //remotePortVal.value = portHeader;
            msgList.add(remotePortVal);
            //msg.data[msgId++] = remotePortVal;

            msg.data = msgList.toArray(new KeyValuePairStringType[0]);
            

            int j = 0;
            List<KeyValuePairBufferType> tempBufferList = new ArrayList<>();
            KeyValuePairBufferType messageData = new
              KeyValuePairBufferType();
            messageData.key = OutgoingMessageKeys.Message;

            Log.d(TAG,"parseIncomingMessage:response" +
                  "Message string:["+ response +"]");
            byte[] messageByteArray = response.getBytes();
            messageData.value = new byte[messageByteArray.length];
            for(int i = 0; i < messageByteArray.length; i++) {
              messageData.value[i] = messageByteArray[i];
            }
            //msg.bufferData[j++] = messageData;
            msg.bufferData = new KeyValuePairBufferType[1];
            msg.bufferData[0] = messageData;
            //tempBufferList.add(messageData);
          }
          else{
            Log.d(LOG_TAG,"parseIncomingMessage: non-incoming msg");
            return null;
          }
          return msg;
        }
    };

    CMAidlConnection(int instance,
                 String TagName,
                 String uriStr,
                 View cmServiceView) {
        mTabInstance = instance;
        mTagName = TagName;
        mUriStr = uriStr;
        mCmServiceView = cmServiceView;
        Log.d(LOG_TAG,"CMAidlConnection ctor FT: "+mTagName);
    }

    public ConnectionAidlListenerImpl getConnListnerImpl() {
        return mAidlConnListener;
    }
    public boolean isFtRegistered() {
        return isFtRegistered;
    }
    public String getTagName() {
        return mTagName;
    }

    public void setIConnectionData(
       vendor.qti.ims.imscmaidlservice.IImsCMConnection connection,
       long connectionHandle,
       long listenerToken) {
        connAidlObj = connection;
        this.connectionHandle = connectionHandle;
        mAidlConnListener.setListenerToken(listenerToken);
    }

    public void setConnectionStatusList(String statusText){
      mConnectionStatusList.add(statusText);
    }

    public List<String> getConnectionStatusList() {
      return mConnectionStatusList;
    }

    public boolean closeConnection(long connectionManager) {
      boolean status = false;
      boolean removeListenerstatus = removeListener(
             mAidlConnListener.getListenerToken());
       if(removeListenerstatus){
        status = MainActivity.globaldata[mTabInstance].connAidlMgr.closeConnection(
            connectionManager,
          connectionHandle);
       }
       return status;
    }

    public boolean removeListener(long listenerToken) {
      try{
        connAidlObj.removeListener(listenerToken);
      }catch(RemoteException e) {
        return false;
      }
      return true;
    }

    public boolean sendMessage(OutgoingMessage data, int userdata) {
       try{
          connAidlObj.sendMessage(
                        data,
                        userdata);
        }catch(RemoteException e) {
          return false;
        }
        return true;
    }

        public boolean closeTransaction(String callID, int userdata) {
            try{
          connAidlObj.closeTransaction(
                        callID,
                        userdata);
        }catch(RemoteException e) {
                return false;
            }
            return true;
        }

        public boolean closeAllTransactions(int userdata) {
            try{
        connAidlObj.closeAllTransactions(
                  userdata);
      }catch(RemoteException e) {
                return false;
            }
            return true;
        }

    public boolean setStatus(int status) {
            try{
        connAidlObj.setStatus(
          mUriStr,
                        status);
            }
            catch(RemoteException e) {
                return false;
      }
            return true;
    }
}
