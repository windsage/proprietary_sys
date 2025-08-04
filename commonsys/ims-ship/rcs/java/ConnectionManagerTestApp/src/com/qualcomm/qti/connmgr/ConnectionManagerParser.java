/**
 * Copyright (c)2020-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.connmgr;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.qualcomm.qti.imscmservice.V2_0.keyValuePairBufferType;
import com.qualcomm.qti.imscmservice.V2_0.keyValuePairStringType;
import com.qualcomm.qti.imscmservice.V2_0.outgoingMessage;
import com.qualcomm.qti.imscmservice.V2_0.outgoingMessageKeys;
import com.qualcomm.qti.imscmservice.V2_0.userConfigKeys;
import com.qualcomm.qti.imscmservice.V2_2.deviceConfigKeys;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class ConnectionManagerParser {
    final static String TAG = "CMTestApp:ConnectionManagerParser";
    final String FILE_NAME = "ConnectionManagerConfig.xml";
    private static String XmlFileData = "";
    public static outgoingMessage msg;
    public static vendor.qti.ims.imscmaidlservice.OutgoingMessage stableAidlMsg;
    public static ArrayList<String> ftList = new ArrayList<String>();
    public static HashMap<String, String> parsedFeatureTagMap =
      new HashMap<String, String>();
    static ConnectionManagerParser singleTon = null;

    public static ConnectionManagerParser getInstance(Context ctx) {
        if(singleTon == null) {
            singleTon = new ConnectionManagerParser();
            singleTon.init(ctx);
        }
        return singleTon;
    }


    public ConnectionManagerParser() {
        Log.i(TAG, "ctor of ConnectionManagerParser");
    }

    public void init(Context ctx) {
        File path = ctx.getExternalFilesDir(null);
        File txtFile = new File(path, FILE_NAME);
        Log.e(TAG,"File Path : "+ path.getPath());
        Log.e(TAG,"File Absolute Path : "+ path.getAbsolutePath());
        try{
        Log.e(TAG,"File Cannonical Path : "+ path.getCanonicalPath());
        }
        catch (IOException e) {
            Log.e(TAG, "getCanonicalPath Exception occurred");
        }
        // Read the file Contents in a StringBuilder Object
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(
                new FileReader(txtFile));
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line + '\n');
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Error occured while reading text file!!");
        }

        XmlFileData = text.toString();
    }

    private XmlPullParser getParser() {
        XmlPullParser parser = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
            parser.setInput(new StringReader(XmlFileData));
            //startParse();
        } catch (XmlPullParserException e) {
            //
            }
        return parser;
    }

    public void startParseFeatureTag(int tabInstance)  {
        //reset ftList before parsing
        ftList.clear();
        XmlPullParser xpp = getParser();
        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String TagName = xpp.getName();
                    if(TagName.equals("Tag")) {
                        Log.d(TAG, "before calling parseFeatureTag");
                        String ft = parseFeatureTags(xpp, "Tag",tabInstance);
                        if(!ft.isEmpty())
                        {
                            ftList.add(ft);
                        }
                    }
                    }
                eventType = xpp.nextToken();
                }
        }catch (XmlPullParserException| IOException e) {
            //@TODO: error reading the parser (make a pop-up)
        }
    }

    public void startParseSipMessage(int tabInstance)  {
        XmlPullParser xpp = getParser();
        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String TagName = xpp.getName();
                    if(TagName.equals("sip_message")) {
                        Log.d(TAG, "before calling parseSipMessage");
                        if(ConnectionManagerService.isCMAidlRegistered){
                          Log.d(TAG,"AIDL parse sip message");
                          stableAidlMsg = parseSipMessageForAidl(xpp,"sip_message", tabInstance);
                        }
                        else{
                          msg = parseSipMessage(xpp,"sip_message", tabInstance);
                        }
                    }
                }
                eventType = xpp.nextToken();
            }
        }catch (XmlPullParserException| IOException e) {
            //@TODO: error reading the parser (make a pop-up)
        }
    }

    private outgoingMessage parseSipMessage(
      XmlPullParser parser,
      String endTag,
      int tabInstance) throws XmlPullParserException, IOException {
        outgoingMessage msg = new outgoingMessage();
        int eventType = parser.getEventType();
        String TagName ="";
        String Data = "";
        String message_body_cdata = "";
        String protocol = "";
        while (eventType != XmlPullParser.END_DOCUMENT) {
          switch (eventType) {
            case XmlPullParser.START_TAG:
            {
              TagName = parser.getName();
              if(TagName.equals("message_body")) {
                int token = parser.nextToken();
                while(token != XmlPullParser.CDSECT){
                  token = parser.nextToken();
                }
                message_body_cdata = parser.getText();
              }
            }
            break;
            case XmlPullParser.TEXT:
              Data = parser.getText();
            break;
            case XmlPullParser.END_TAG:
            {
              String engTagName = parser.getName();
               if(engTagName.equals(endTag)) {
                Log.d(TAG,"Return from here");
                return msg;
               }
               else if(TagName.equals("outbound_proxy")) {
                 keyValuePairStringType outboundProxyVal =
                   new keyValuePairStringType();
                 outboundProxyVal.key = outgoingMessageKeys.OutboundProxy;
                 //outboundProxyVal.value = Data;
                 outboundProxyVal.value = MainActivity.globaldata[tabInstance].
                   deviceConfigData.get(deviceConfigKeys.
                     StrSipOutBoundProxyName);
                 msg.data.add(outboundProxyVal);
               } else if(TagName.equals("remoteport")) {
                 keyValuePairStringType remotePortVal = new
                   keyValuePairStringType();
                 remotePortVal.key = outgoingMessageKeys.RemotePort;
                 remotePortVal.value = Data;
                 msg.data.add(remotePortVal);
               }else if(TagName.equals("protocol")) {
                 keyValuePairStringType protocolTypeVal = new
                   keyValuePairStringType();
                 if(Data.equals(0)){
                    protocol = "UDP";
                 }else if(Data.equals(1)){
                    protocol = "TCP";
                 }
                 protocolTypeVal.key = outgoingMessageKeys.Protocol;
                 Log.d(TAG,"protocol: "+Data);
                 protocolTypeVal.value = Data;
                 msg.data.add(protocolTypeVal);
               }else if(TagName.equals("message_type")) {
                 keyValuePairStringType messageTypeVal = new
                   keyValuePairStringType();
                 messageTypeVal.key = outgoingMessageKeys.MessageType;
                 messageTypeVal.value = Data;
                 Log.d(TAG,"message_type: "+Data);
                 msg.data.add(messageTypeVal);
               }else if(TagName.equals("callid")) {
                 keyValuePairStringType callIdTypeVal = new
                   keyValuePairStringType();
                 callIdTypeVal.key = outgoingMessageKeys.CallId;
                 Data = Data.replaceAll("%OutboundProxy%",
                    MainActivity.globaldata[tabInstance].deviceConfigData.
                    get(deviceConfigKeys.StrSipOutBoundProxyName));
                 callIdTypeVal.value = Data;
                 Log.d(TAG,"callid: "+Data);
                 msg.data.add(callIdTypeVal);
               }else if(TagName.equals("message_body")) {
                 keyValuePairBufferType messageData = new
                   keyValuePairBufferType();
                 messageData.key = outgoingMessageKeys.Message;

                 Log.d(TAG,"Before replacing delimitors :[" +
                    message_body_cdata+"]"+
                    "size: "+message_body_cdata.length());
                    Data = message_body_cdata.replaceAll("\\r|\\n|\\t", "");

                 Log.d(TAG,"Domain Name:["+
                    MainActivity.globaldata[tabInstance].
                    userConfigData.get(userConfigKeys.SipHomeDomain)+"]");
                 Data = Data.replaceAll("%DomainName%",
                    MainActivity.globaldata[tabInstance].userConfigData.
                    get(userConfigKeys.SipHomeDomain));
                 Data = Data.replaceAll("%PublicUserId%",
                    MainActivity.globaldata[tabInstance].userConfigData.
                    get(userConfigKeys.SipPublicUserId));
                 Data = Data.replaceAll("%OutboundProxy%",
                    MainActivity.globaldata[tabInstance].deviceConfigData.
                    get(deviceConfigKeys.StrSipOutBoundProxyName));
                 Data = Data.replaceAll("%LocalIpAddress%",
                    MainActivity.globaldata[tabInstance].userConfigData.
                    get(userConfigKeys.LocalHostIPAddress));
                 Data = Data.replaceAll("%OutboundProxyPort%",
                    MainActivity.globaldata[tabInstance].deviceConfigData.
                    get(deviceConfigKeys.SipOutBoundProxyPort));
                 Data = Data.replaceAll("%Security-Verify%",
                    MainActivity.globaldata[tabInstance].deviceConfigData.
                    get(deviceConfigKeys.SecurityVerify));
                 Data = Data.replaceAll("%Protocol%",
                    (protocol.equals("0")? "UDP" : "TCP"));
                 Data = Data.replaceAll("%CRLF%", "\r\n");
                 Log.d(TAG,"After replacing delimitors :[" + Data+"]"+
                    "size: "+Data.length());
                 byte[] messageByteArray = Data.getBytes();
                 for(int i = 0; i < messageByteArray.length; i++) {
                    messageData.value.add(messageByteArray[i]);
                 }
                 msg.bufferData.add(messageData);
                }
            }
            break;
          }
          eventType = parser.next();
        }
        return  msg;
    }

    private vendor.qti.ims.imscmaidlservice.OutgoingMessage parseSipMessageForAidl(
      XmlPullParser parser,
      String endTag,
      int tabInstance) throws XmlPullParserException, IOException {
        vendor.qti.ims.imscmaidlservice.OutgoingMessage msg = new vendor.qti.ims.imscmaidlservice.OutgoingMessage();
        List<vendor.qti.ims.imscmaidlservice.KeyValuePairStringType> tempList = new ArrayList<>();
        List<vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType> tempBufferList = new ArrayList<>();
        int eventType = parser.getEventType();
        String TagName ="";
        String Data = "";
        String message_body_cdata = "";
        String protocol = "";
        while (eventType != XmlPullParser.END_DOCUMENT) {
          switch (eventType) {
            case XmlPullParser.START_TAG:
            {
              TagName = parser.getName();
              if(TagName.equals("message_body")) {
                int token = parser.nextToken();
                while(token != XmlPullParser.CDSECT){
                  token = parser.nextToken();
                }
                message_body_cdata = parser.getText();
              }
            }
            break;
            case XmlPullParser.TEXT:
              Data = parser.getText();
            break;
            case XmlPullParser.END_TAG:
            {
              String engTagName = parser.getName();
               if(engTagName.equals(endTag)) {
                Log.d(TAG,"end returning from here");
                msg.data = tempList.toArray(new vendor.qti.ims.imscmaidlservice.KeyValuePairStringType[0]);
              msg.bufferData = tempBufferList.toArray(new vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType[0]);
            Log.d(TAG, "templist length " + tempList.size());
            Log.d(TAG, "tempbufferlist length " + tempBufferList.size());
            Log.d(TAG,"msg.data " + msg.data.length);
            Log.d(TAG, "msg.bufferData " + msg.bufferData.length);
                 return msg;
               }
               else if(TagName.equals("outbound_proxy")) {
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType outboundProxyVal =
                   new vendor.qti.ims.imscmaidlservice.KeyValuePairStringType();
                 outboundProxyVal.key =  vendor.qti.ims.imscmaidlservice.OutgoingMessageKeys.OutboundProxy;
                 //outboundProxyVal.value = Data;
                 outboundProxyVal.value = MainActivity.globaldata[tabInstance].
                   deviceConfigData.get(vendor.qti.ims.imscmaidlservice.DeviceConfigKeys.
                     StrSipOutBoundProxyName);
                 tempList.add(outboundProxyVal);
                 //msg.data.add(outboundProxyVal);
               } else if(TagName.equals("remoteport")) {
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType remotePortVal = new
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType();
                 remotePortVal.key =  vendor.qti.ims.imscmaidlservice.OutgoingMessageKeys.RemotePort;
                 remotePortVal.value = Data;
                 tempList.add(remotePortVal);
                 //msg.data.add(remotePortVal);
               }else if(TagName.equals("protocol")) {
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType protocolTypeVal = new
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType();
                 if(Data.equals(0)){
                    protocol = "UDP";
                 }else if(Data.equals(1)){
                    protocol = "TCP";
                 }
                 protocolTypeVal.key =  vendor.qti.ims.imscmaidlservice.OutgoingMessageKeys.Protocol;
                 Log.d(TAG,"protocol: "+Data);
                 protocolTypeVal.value = Data;
                 tempList.add(protocolTypeVal);
                 //msg.data.add(protocolTypeVal);
               }else if(TagName.equals("message_type")) {
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType messageTypeVal = new
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType();
                 messageTypeVal.key =  vendor.qti.ims.imscmaidlservice.OutgoingMessageKeys.MessageType;
                 messageTypeVal.value = Data;
                 Log.d(TAG,"message_type: "+Data);
                 tempList.add(messageTypeVal);
                 //msg.data.add(messageTypeVal);
               }else if(TagName.equals("callid")) {
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType callIdTypeVal = new
                vendor.qti.ims.imscmaidlservice.KeyValuePairStringType();
                 callIdTypeVal.key = vendor.qti.ims.imscmaidlservice.OutgoingMessageKeys.CallId;
                 Data = Data.replaceAll("%OutboundProxy%",
                    MainActivity.globaldata[tabInstance].deviceConfigData.
                    get(vendor.qti.ims.imscmaidlservice.DeviceConfigKeys.StrSipOutBoundProxyName));
                 callIdTypeVal.value = Data;
                 Log.d(TAG,"callid: "+Data);
                 tempList.add(callIdTypeVal);
                 //msg.data.add(callIdTypeVal);
               }else if(TagName.equals("message_body")) {
                vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType messageData = new
                vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType();
                //vendor.qti.ims.imscmaidlservice.
                 messageData.key = vendor.qti.ims.imscmaidlservice.OutgoingMessageKeys.Message;

                 Log.d(TAG,"Before replacing delimitors :[" +
                    message_body_cdata+"]"+
                    "size: "+message_body_cdata.length());
                    Data = message_body_cdata.replaceAll("\\r|\\n|\\t", "");

                 Log.d(TAG,"Domain Name:["+
                    MainActivity.globaldata[tabInstance].
                    userConfigData.get(vendor.qti.ims.imscmaidlservice.UserConfigKeys.SipHomeDomain)+"]");
                 Data = Data.replaceAll("%DomainName%",
                    MainActivity.globaldata[tabInstance].userConfigData.
                    get(vendor.qti.ims.imscmaidlservice.UserConfigKeys.SipHomeDomain));
                 Data = Data.replaceAll("%PublicUserId%",
                    MainActivity.globaldata[tabInstance].userConfigData.
                    get(vendor.qti.ims.imscmaidlservice.UserConfigKeys.SipPublicUserId));
                 Data = Data.replaceAll("%OutboundProxy%",
                    MainActivity.globaldata[tabInstance].deviceConfigData.
                    get(vendor.qti.ims.imscmaidlservice.DeviceConfigKeys.StrSipOutBoundProxyName));
                 Data = Data.replaceAll("%LocalIpAddress%",
                    MainActivity.globaldata[tabInstance].userConfigData.
                    get(vendor.qti.ims.imscmaidlservice.UserConfigKeys.LocalHostIPAddress));
                 Data = Data.replaceAll("%OutboundProxyPort%",
                    MainActivity.globaldata[tabInstance].deviceConfigData.
                    get(vendor.qti.ims.imscmaidlservice.DeviceConfigKeys.SipOutBoundProxyPort));
                 Data = Data.replaceAll("%Security-Verify%",
                    MainActivity.globaldata[tabInstance].deviceConfigData.
                    get(vendor.qti.ims.imscmaidlservice.DeviceConfigKeys.SecurityVerify));
                 Data = Data.replaceAll("%Protocol%",
                    (protocol.equals("0")? "UDP" : "TCP"));
                 Data = Data.replaceAll("%CRLF%", "\r\n");
                 Log.d(TAG,"After replacing delimitors :[" + Data+"]"+
                    "size: "+Data.length());
                 byte[] messageByteArray = Data.getBytes();
                 messageData.value = new byte[messageByteArray.length];
                 for(int i = 0; i < messageByteArray.length; i++) {
                    messageData.value[i] = messageByteArray[i];
                 }
                 tempBufferList.add(messageData);
                 //msg.bufferData = new vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType[1];
                 //msg.bufferData[0] = messageData;
                }
            }
            break;
          }
          eventType = parser.next();
          Log.d(TAG,"while next");
        }
        Log.d(TAG,"while next outside");
        msg.data = tempList.toArray(new vendor.qti.ims.imscmaidlservice.KeyValuePairStringType[0]);
        msg.bufferData = tempBufferList.toArray(new vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType[0]);
        Log.d(TAG, "templist length " + tempList.size());
        Log.d(TAG, "tempbufferlist length " + tempBufferList.size());
        Log.d(TAG,"msg.data " + msg.data.length);
        Log.d(TAG, "msg.bufferData " + msg.bufferData.length);
        return msg;
    }

    private String parseFeatureTags(
        XmlPullParser parser,
        String endTag,
        int tabInstance) throws XmlPullParserException,
        IOException {
        Log.d(TAG, "inside parseFeatureTag");
        String parsedFT = "";
        int eventType = parser.getEventType();
        String TagName ="";
        String Data = "";
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    TagName = parser.getName();
                    break;
                case XmlPullParser.TEXT:
                    Data = parser.getText();
                    break;
                case XmlPullParser.END_TAG:
                    String endTagName = parser.getName();
                    if(endTagName.equals(endTag)) {
                        return parsedFT;
                    }
                    else if (endTagName.equals("id")) {
                        Log.d(TAG, "parseFeatureTag: id:" + Data);
                    }
                    else if (endTagName.equalsIgnoreCase("tag_name")) {
                        Log.d(TAG, "parseFeatureTag: tag_name:" + Data);
                        parsedFT = Data;
                    }
                    else if (endTagName.equalsIgnoreCase("tag_string")) {
                        Data = Data.replaceAll("\\r|\\n|\\t", "");
                        MainActivity.globaldata[tabInstance].
                        parsedFeatureTagMap.put(parsedFT, Data);
                    }
                    break;
            }
            eventType = parser.next();
        }
        Log.d(TAG, "parseFeatureTag: parsedFT:" + parsedFT);
        return  parsedFT;
    }
}
