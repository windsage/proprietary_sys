/**
 * Copyright (c) 2022-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.content.ClipData;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Arrays;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;

import vendor.qti.imsdatachannel.aidl.DataChannelState;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelAttributes;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelCommandErrorCode;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelErrorCode;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelMessage;
import vendor.qti.imsdatachannel.aidl.ImsDataChannelState;
import vendor.qti.imsdatachannel.aidl.ImsMessageStatus;
import vendor.qti.imsdatachannel.aidl.ImsMessageStatusInfo;
import vendor.qti.imsdatachannel.client.ImsDataChannelConnection;
import vendor.qti.imsdatachannel.client.ImsDataChannelMessageCallback;
import vendor.qti.imsdatachannel.client.ImsDataChannelTransport;

public class DcConnectionFragment extends DialogFragment implements ImsDataChannelMessageCallback {

    private String LOG_TAG = "DCTestApp:DcConnectionFragment";

    ImsDataChannelAttributes mAttr;
    private ImsDataChannelConnection mDcConnection = null;
    private ImsDataChannelTransport mDcTransport = null;

    Executor mExecutor = new ScheduledThreadPoolExecutor(1);

    private List<String> messages = new ArrayList<String>();
    private List<String> pendingFilesToSend = new ArrayList<String>();
    private HashMap<String, String> ExtToMimeMap = new HashMap<String,String>();
    private ViewGroup msgContainer;
    private Button sendMsgButton;
    private Button closeDcConnectionButton;
    private boolean mDataChannelConnected = true;
    private int nextMsgNum = 1;
    private String dcConnectionId;
    private int maxFragmentLength = 64000;
    private boolean waitingForCommandStatus = false;
    private Queue<byte[]> pendingDCMessages = new LinkedList<byte[]>();
    private int nextSegmentNum = 0;
    private int nextFileOffset = 0;
    private static final int PICKFILE_RESULT_CODE = 1;

    private File mExternalFileDir;
    final String BOOTSTRAP_MSG_FILE_NAME = "DataChannelMsg.txt";
    final String APPLICATION_MSG_FILE_NAME = "AppDataChannelMsg.txt";
    final String BOOTSTRAP_RCVD_MSG_FILE_NAME = "ReceivedBootstrapMsg.txt";
    final String APPLICATION_RCVD_MSG_FILE_NAME = "ReceivedAppMsg.txt";
    private static int bootstrapBytesWritten = 0;
    private static int appBytesWritten = 0;
    final int FILE_SIZE_LIMIT = 52428800; // 50MiB
    final int MAX_CACHED_FRAGMENT_NUM = 20;
    final int MIN_CACHED_FRAGMENT_NUM = 5;
    private String ReceivedFileName = "";
    private int receivedFileSize = 0;

    public DcConnectionFragment(ImsDataChannelAttributes attr, ImsDataChannelConnection dcConnection, ImsDataChannelTransport dcTransport, File externalFileDir) {
        Log.d(LOG_TAG,
              "attr[dcId[" + attr.getDcId() +
              "attr[dcHandle[" + attr.getDcHandle() +
              "], streamId[" + attr.getDataChannelStreamId() +
              "], label[" + attr.getDataChannelLabel() + "]]");
        mAttr = attr;
        mDcConnection = dcConnection;
        mDcTransport = dcTransport;
        mExternalFileDir = externalFileDir;
        dcConnectionId = attr.getDcId() + " : " + attr.getDataChannelStreamId();
        if(attr.getMaxMessageSize() == 0)
        {
            maxFragmentLength = 64000;
        } else if(attr.getMaxMessageSize() > 1000000)
        {
            Log.d(LOG_TAG, " Received remoteMaxSize larger than 1MB; limiting to 1MB size");
            maxFragmentLength = 1000000;
        } else
        {
            maxFragmentLength = attr.getMaxMessageSize();
        }
        Log.d(LOG_TAG, " MaxMessageSize is set to: " + maxFragmentLength);
        LOG_TAG = LOG_TAG + "[" + dcConnectionId + "]";
        fillExtToMimeHashMap();
    }

    public ImsDataChannelConnection getDcConnection() {
        return mDcConnection;
    }

    public static void resetFiles() {
        Log.d("DCTestApp:DcConnectionFragment", "Done writing to files: bootstrapBytesWritten=" + bootstrapBytesWritten + " appBytesWritten=" + appBytesWritten);
        bootstrapBytesWritten = 0;
        appBytesWritten = 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");
        View root = inflater.inflate(R.layout.fragment_dc_connection, container, false);

        TextView dataChannelLabel = root.findViewById(R.id.textView_dataChannelLabel);
        dataChannelLabel.setText(mAttr.getDataChannelLabel());

        // Present messages sent and received
        msgContainer = root.findViewById(R.id.linearLayout_dcMessageLog);
        for (String message : messages) {
            showMessage(message);
        }

        sendMsgButton = root.findViewById(R.id.button_sendMessage);
        sendMsgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "sendMessage button clicked");
                openFileLocation();
            }
        });
        sendMsgButton.setEnabled(mDataChannelConnected);

        closeDcConnectionButton = root.findViewById(R.id.button_closeDcConnection);
        closeDcConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleCloseDcConnection();
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }

    /*-------- Callbacks from ImsDataChannelStatusCallback --------*/

    public void onClosed(ImsDataChannelErrorCode code) {
        newMessage("DataChannel closed: code[" + code.getImsDataChannelErrorCode() + "]");
        mDataChannelConnected = false;
        if (sendMsgButton != null) {
            sendMsgButton.post(() -> {
                sendMsgButton.setEnabled(mDataChannelConnected);
            });
        }
    }

    public void onStateChange(ImsDataChannelState dcState) {
        int state = dcState.getState().getDataChannelState();
        newMessage("DataChannel state changed: " + state);
        mDataChannelConnected = state == DataChannelState.DATA_CHANNEL_CONNECTED;
        if (sendMsgButton != null) {
            sendMsgButton.post(() -> {
                sendMsgButton.setEnabled(mDataChannelConnected);
            });
        }
    }

    private void showMessage(String message) {
        if (msgContainer == null) {
            return;
        }
        msgContainer.post(() -> {
            TextView msgView = new TextView(getActivity());
            msgView.setText(message);
            msgView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                                     LayoutParams.WRAP_CONTENT));
            msgView.setTextSize(14);
            msgView.setPadding(8, 8, 8, 8);
            msgContainer.addView(msgView);
            // Log.d(LOG_TAG, "showMessage(): added message: " + message);
        });
    }

    private void newMessage(String message) {
        messages.add(message);
        showMessage(message);
    }

    private void sendDataChannelMessage(byte[] bytes) {
        if (waitingForCommandStatus) {
            Log.d(LOG_TAG, "sendDataChannelMessage() queueing message for later");
            pendingDCMessages.add(bytes);
        } else {
            waitingForCommandStatus = true;
            Log.d(LOG_TAG, "sendDataChannelMessage() :"+bytes.length);
            ImsDataChannelMessage msg = new ImsDataChannelMessage();
            msg.setDcId(mAttr.getDcId());
            msg.setDcHandle(mAttr.getDcHandle());
            String protocolId = "51"; // TODO make configurable
            Log.d(LOG_TAG, "sendDataChannelMessage() protocolId = " + protocolId);
            msg.setProtocolId(protocolId);
            msg.setMessageId(Integer.toString(nextMsgNum));
            nextMsgNum++;
            msg.setMessage(bytes);
            Log.d(LOG_TAG, "sendDataChannelMessage() msgId=" + msg.getMessageId() + " length=" + msg.getMessage().length + " bytes");
            newMessage("Sending: message length " + msg.getMessage().length + " bytes");
            try {
                mDcConnection.sendMessage(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "sendDataChannelMessage RemoteException!!!");
            }
        }
    }

    private void fillExtToMimeHashMap() {
        ExtToMimeMap.put("css","text/css");
        ExtToMimeMap.put("mpga","audio/mpeg");
        ExtToMimeMap.put("csv","text/csv");
        ExtToMimeMap.put("weba","audio/webm");
        ExtToMimeMap.put("txt","text/plain");
        ExtToMimeMap.put("vtt","text/vtt");
        ExtToMimeMap.put("otf","font/otf");
        ExtToMimeMap.put("html","text/html");
        ExtToMimeMap.put("htm","text/html");
        ExtToMimeMap.put("ttf","font/ttf");
        ExtToMimeMap.put("apng","image/apng");
        ExtToMimeMap.put("woff","font/woff");
        ExtToMimeMap.put("woff2","font/woff2");
        ExtToMimeMap.put("avif","image/avif");
        ExtToMimeMap.put("bmp","image/bmp");
        ExtToMimeMap.put("7z","application/x-7z-compressed");
        ExtToMimeMap.put("gif","image/gif");
        ExtToMimeMap.put("atom","application/atom+xml");
        ExtToMimeMap.put("png","image/png");
        ExtToMimeMap.put("pdf","application/pdf");
        ExtToMimeMap.put("svg","image/svg+xml");
        ExtToMimeMap.put("mjs","application/javascript");
        ExtToMimeMap.put("js","application/javascript");
        ExtToMimeMap.put("webp","image/webp");
        ExtToMimeMap.put("json","application/json");
        ExtToMimeMap.put("ico","image/x-icon");
        ExtToMimeMap.put("rss","application/rss+xml");
        ExtToMimeMap.put("tif","image/tiff");
        ExtToMimeMap.put("tar","application/x-tar");
        ExtToMimeMap.put("tiff","image/tiff");
        ExtToMimeMap.put("xhtml","application/xhtml+xml");
        ExtToMimeMap.put("xht","application/xhtml+xml");
        ExtToMimeMap.put("jpeg","image/jpeg");
        ExtToMimeMap.put("jpg","image/jpeg");
        ExtToMimeMap.put("xslt","application/xslt+xml");
        ExtToMimeMap.put("mp4","video/mp4");
        ExtToMimeMap.put("xml","application/xml");
        ExtToMimeMap.put("mpeg","video/mpeg");
        ExtToMimeMap.put("gz","application/gzip");
        ExtToMimeMap.put("webm","video/webm");
        ExtToMimeMap.put("zip","application/zip");
        ExtToMimeMap.put("mp3","audio/mp3");
        ExtToMimeMap.put("wasm","application/wasm");
    }

    private String getMimeType(String extension) {
        return ExtToMimeMap.get(extension);
    }

    private void generateHeader(File inputStream) {
        String startLine = new String();
        String filename = inputStream.getName();
        String mimeType = getMimeType((filename.substring(filename.lastIndexOf(".") + 1)).toLowerCase());

	if(mimeType == null) {
             mimeType = "";
        }

	startLine+=("POST /post HTTP/1.1");
        startLine+=("\r\n");
        startLine+=("Accept: */*");
        startLine+=("\r\n");
        startLine+=("Content-Length: "+inputStream.length());
        startLine+=("\r\n");
        startLine+=("Content-Type: "+mimeType);
        startLine+=("\r\n");
        startLine+=("Content-Disposition: attachment;filename=\""+filename+"\"");
        startLine+=("\r\n");
        Log.d(LOG_TAG, " mime type: "+getMimeType((filename.substring(filename.lastIndexOf(".") + 1)).toLowerCase()));
        Log.d(LOG_TAG, "filename: \""+filename+"\"");
        sendDataChannelMessage(startLine.getBytes());
    }

    private RandomAccessFile getInputStream() throws IOException {
        File msgFile;
        RandomAccessFile inputStream = null;
        if(!pendingFilesToSend.isEmpty()) {
            msgFile = new File(mExternalFileDir,pendingFilesToSend.get(0));
        } else {
            Log.e(LOG_TAG, " no files to send");
            return null;
        }
        Path msgPath = Paths.get(msgFile.getPath());

        inputStream = new RandomAccessFile(msgFile, "r");
         Log.e(LOG_TAG,"getInputStream() Reading file : " + msgFile.getPath()+
                         " of total length: " + inputStream.length() + " bytes");
        return inputStream;
    }

    private void readFile(RandomAccessFile inputStream) throws IOException {
         byte[] tempFragmentMsg = new byte[maxFragmentLength];

         if(inputStream == null) {
             Log.i(LOG_TAG,"ReadFile input stream is null");
             return;
         }

         if(pendingDCMessages.size() > MAX_CACHED_FRAGMENT_NUM) {
             Log.i(LOG_TAG,"ReadFile() Msg pending message list has 20 messages already");
             return;
         }

         if(nextFileOffset == 0) {
             generateHeader(new File(mExternalFileDir,pendingFilesToSend.get(0)));
         }
         inputStream.seek(nextFileOffset);
         int bytesRead = inputStream.read(tempFragmentMsg);

         while(bytesRead != -1){
             nextFileOffset = nextFileOffset + bytesRead;
             if(bytesRead == maxFragmentLength) {
                 sendDataChannelMessage(tempFragmentMsg);
             } else {
                 byte[] sendData = Arrays.copyOfRange(tempFragmentMsg, 0, bytesRead);
                 sendDataChannelMessage(sendData);
             }
             nextSegmentNum++;
             Log.i(LOG_TAG,"ReadFile() Msg Sent of Length " + bytesRead + " Fragment Number " + nextSegmentNum);
             if(pendingDCMessages.size() > MAX_CACHED_FRAGMENT_NUM) {
               Log.i(LOG_TAG,"ReadFile() Msg pending message list has 20 messages already");
               return;
             }
             bytesRead = inputStream.read(tempFragmentMsg);
         }
         Log.i(LOG_TAG,"ReadFile() Total Number of Fragmented Msg Sent for one file " + nextSegmentNum);

         nextFileOffset = 0;
         nextSegmentNum = 0;
         pendingFilesToSend.remove(0);
    }

    synchronized public void handleSendDcMessage() {
        Log.d(LOG_TAG, "handleSendDcMessage()");
        RandomAccessFile inputStream = null;
        Log.e(LOG_TAG,"handleSendDcMessage() Max allowed fragment length: " + maxFragmentLength);
        try {
            inputStream = getInputStream();
            //this will read the 64k bytes
            readFile(inputStream);
            if(nextFileOffset == 0 &&(!pendingFilesToSend.isEmpty()) && pendingDCMessages.size() < 2) {
                generateHeader(new File(mExternalFileDir,pendingFilesToSend.get(0)));
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "handleSendDcMessage IOException!!! Using dummy data");
            e.printStackTrace();
            newMessage("Couldn't read message file. Sending dummy data.");
            sendDummyData();

        } finally{
            //inputstream close can also result in IO exception, handling it using try catch
            try{
                if(inputStream != null){
                    inputStream.close();
                }
            } catch (IOException e){
                Log.e(LOG_TAG, "handleSendDcMessage IOException!!! inputstream close " + e.toString());
            }
        }
    }

    private void sendDummyData(){
        byte[] bytes;
        String s = "";
            for (int i = 0; i < 35; i++) {
                s += "abc1234567890";
            }
            bytes = s.getBytes();
            //return bytes;
        Log.e(LOG_TAG,"handleSendDcMessage() Total message length " + bytes.length + " bytes");
        int numOfSegments = 0;
        for (int i = 0; i < bytes.length; i += maxFragmentLength) {
            int j = Math.min(bytes.length, i + maxFragmentLength);
            byte[] tempFragmentMsg = Arrays.copyOfRange(bytes, i, j);
            sendDataChannelMessage(tempFragmentMsg);
            numOfSegments++;
            Log.e(LOG_TAG,"handleSendDcMessage() Msg Sent of Length " + tempFragmentMsg.length + " Fragment Number " + numOfSegments);
        }
    }
    private void handleCloseDcConnection() {
        Log.d(LOG_TAG, "handleCloseDcConnection()");
        ImsDataChannelConnection[] dc = new ImsDataChannelConnection[]{mDcConnection};
        Log.e(LOG_TAG,"handleCloseDcConnection() dcHandle is " + mDcConnection.getConnectionAttributes().getDcHandle());
        Log.e(LOG_TAG,"handleCloseDcConnection() dcId is " + mDcConnection.getConnectionAttributes().getDcId());
        ImsDataChannelErrorCode code = new ImsDataChannelErrorCode();
        code.setImsDataChannelErrorCode(ImsDataChannelErrorCode.DCS_DC_ERROR_NONE);
        try {
            mDcTransport.closeDataChannel(dc, code);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "dcTransport.closeDataChannel RemoteException!!!");
        }
        dismiss();
    }

    public void sendInitialBDCMessage() {
        pendingFilesToSend.add(BOOTSTRAP_MSG_FILE_NAME);
         handleSendDcMessage();
    }

    public void openFileLocation() {
        Log.e(LOG_TAG, " openFileLocation");
        final File folder = new File(mExternalFileDir.getAbsolutePath()+"/toSend");
        Log.d(LOG_TAG, "folder being read "+folder.getPath());
        Log.d(LOG_TAG, "absolute path: "+mExternalFileDir.getAbsolutePath());

        for (final File fileEntry : folder.listFiles()) {
            Log.d(LOG_TAG,"file selected: "+fileEntry.getName());
            pendingFilesToSend.add("/toSend/"+fileEntry.getName());
        }

        handleSendDcMessage();
    }
              //
    /*-------- Overrides for ImsDataChannelMessageCallback --------*/

    @Override
    public void onMessageReceived(ImsDataChannelMessage msg) {
        byte[] bytes = msg.getMessage();
        String s = new String(bytes, StandardCharsets.UTF_8);
        Log.d(LOG_TAG, "onMessagedReceived() length: " + bytes.length + " bytes");
        newMessage("Received: message length " + bytes.length + " bytes");

       if(s.contains("POST /post HTTP/1.1")) {
           Log.d(LOG_TAG, "onMessageReceived this is header");
           String[] splitArr = s.split("\r\n");
           for(String compStr : splitArr) {
               if(compStr.contains("Content-Length:")) {
                   receivedFileSize = Integer.parseInt((compStr.split(": "))[1]);
                   Log.d(LOG_TAG, "received file size should be "+receivedFileSize);
               }
               else if(compStr.contains("Content-Disposition: attachment;filename=")) {
                   ReceivedFileName = "Received_";
                   ReceivedFileName += (compStr.split("attachment;filename="))[1];
                   ReceivedFileName = ReceivedFileName.replace("\"", "");
                   Log.d(LOG_TAG, "received file anme is: "+ReceivedFileName);
               }
           }
           resetFiles();
           return;
       }
       // Write message to file up to
       File msgFile;
       int bytesWritten;
       if (mAttr.getDataChannelStreamId() < 1000) {
           if(ReceivedFileName == "") {
               msgFile = new File(mExternalFileDir, BOOTSTRAP_RCVD_MSG_FILE_NAME);
           }
           bytesWritten = bootstrapBytesWritten;
       } else {
           //msgFile = new File(mExternalFileDir, APPLICATION_RCVD_MSG_FILE_NAME);
           bytesWritten = appBytesWritten;
       }

        msgFile = new File(mExternalFileDir, ReceivedFileName);
        if (bytesWritten + bytes.length > FILE_SIZE_LIMIT) {
            Log.e(LOG_TAG, "onMessageReceived(): file size limit reached. Not writing anymore bytes: " + msgFile.getPath());
            return;
        }
        Path msgPath = Paths.get(msgFile.getPath());
        Log.d(LOG_TAG,"onMessageReceived() Writing file : " + msgFile.getPath());
        try {
            if (bytesWritten == 0) {
                // Default options truncate file to 0
                Log.d(LOG_TAG, "onMessageReceived(): File truncated before writing");
                Files.write(msgPath, bytes);
            } else {
                Files.write(msgPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "onMessageReceived() could not write to file" + e.toString());
        }
        if (mAttr.getDataChannelStreamId() < 1000) {
            bootstrapBytesWritten += bytes.length;
        } else {
            appBytesWritten += bytes.length;
        }
    }

    @Override
    public void onMessageSendStatus(ImsMessageStatusInfo msgStatusInfo) {
        Log.d(LOG_TAG, "onMessageSendStatus()");
        int status = msgStatusInfo.getMsgStatus().getImsMessageStatus();
        String message = "Send status: " + status;
        Log.d(LOG_TAG, "onMessageSendStatus() status: " + status);
        newMessage(message);
    }

    @Override
    public void onMessageSendCommandError(ImsDataChannelCommandErrorCode errorcode) {
        Log.d(LOG_TAG, "onMessageSendCommandError()");
        int errCode = errorcode.getImsDataChannelCommandErrorCode();
        waitingForCommandStatus = false;
        //SUCCESS
        if(errCode == 0)
        {
            Log.d(LOG_TAG, "onMessageSendCommandError() Received errCode:SUCCESS");
            if(!pendingDCMessages.isEmpty())
            {
                Log.d(LOG_TAG, "onMessageSendCommandError(SUCCESS) sending one pending message fragment");
                sendDataChannelMessage(pendingDCMessages.remove());
            }

           if((pendingDCMessages.size() < MIN_CACHED_FRAGMENT_NUM) && (!pendingFilesToSend.isEmpty())) {
              Log.d(LOG_TAG, "onMessageSendCommandError() backup pending messages cache");
        handleSendDcMessage();
           }

        }
        //All other failure cases; pending msgs are cleared
        else
        {
            Log.d(LOG_TAG, "onMessageSendCommandError() Received errCode: " + errCode);
            Log.d(LOG_TAG, "onMessageSendCommandError() clearing pending message fragments");
            pendingDCMessages.clear();
            nextFileOffset = 0;
            nextSegmentNum = 0;
            pendingFilesToSend.clear();
        }
        newMessage("Command Error: " + errCode);
    }
}
