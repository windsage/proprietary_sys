/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package my.tests.snapdragonsdktest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import com.qti.location.sdk.IZatGnssConfigServices;
import com.qti.location.sdk.IZatGnssConfigServices.IZatRobustLocationConfigService;
import com.qti.location.sdk.IZatGnssConfigServices.IZatNtnConfigService;
import com.qti.location.sdk.IZatGnssConfigServices.IZatNtnConfigService.IZatNtnStatusCallback;
import com.qti.location.sdk.IZatManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;

public class GnssConfigActivity extends Activity {

    private static final String TAG = "RLTest";

    IZatGnssConfigServices mGnssService;
    IZatRobustLocationConfigService mRLService;
    IZatNtnConfigService mNtnService;

    private ListView mListView;
    ArrayList<String> mList = new ArrayList<>();
    ArrayAdapter<String> mResultsAdapter;
    private Handler mHandler;
    private static final int MSG_UPDATE_UI = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gnssconfig);


        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_UI:
                    {
                        Log.d(TAG, "MSG_UPDATE_UI");
                        if (msg.obj != null) {
                            mResultsAdapter.insert((String)msg.obj, 0);
                        }
                        mResultsAdapter.notifyDataSetChanged();
                        mListView.postInvalidate();
                        break;
                    }
                    default: break;
                }
            }
        };

        try {
            IZatManager mIzatMgr = IZatManager.getInstance(getApplicationContext());
            if (null == mIzatMgr) {
                Log.e(TAG, "Failed to get IZat Manager");
                return;
            }
            mGnssService = mIzatMgr.connectGnssConfigServices();
            if (null == mGnssService) {
                Log.e(TAG, "Failed to get Gnss Config Service");
                return;
            }
            mRLService = mGnssService.getRobustLocationConfigService();
            mNtnService = mGnssService.getNtnConfigService();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        if (null == mRLService) {
            Log.e(TAG, "Failed to get Robust Location Service");
            return;
        }

        mResultsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mList);
        final Button configMerkleTreeButton = findViewById(R.id.btnConfigMerkleTree);
        final Button configOsnmaEnablementButton = findViewById(R.id.btnConfigOsnmaEnablement);
        final Button configOsnmaDisablementButton = findViewById(R.id.btnConfigOsnmaDisablement);
        final Button registerNtnButton = findViewById(R.id.registerNtnStatusCallback);
        final Button setNtnCapTrueButton = findViewById(R.id.setNtnCapTrue);
        final Button setNtnCapFalseButton = findViewById(R.id.setNtnCapFalse);
        final Button setNtnButton = findViewById(R.id.setNtn);
        final Button getNtnButton = findViewById(R.id.getNtn);
        final EditText ntnMaskText = findViewById(R.id.ntnMask);
        final Button injectSuplCertButton = findViewById(R.id.btnInjectSuplCert);
        mListView = findViewById(R.id.gnssConfigLog);
        mListView.setAdapter(mResultsAdapter);


        configMerkleTreeButton.setOnClickListener(v -> {
            String path=getExternalFilesDir(null).getAbsolutePath() + "/OSNMA_MerkleTree.xml";
            Log.i(TAG, "path : " + path);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];

            try {
                File merkleTree = new File(path);
                long length = merkleTree.length();
                BufferedReader br = new BufferedReader(new FileReader(merkleTree));

                int len = -1;
                while ((len = br.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, len));
                }

                String content = sb.toString();
                Log.i(TAG, "length : " + length + ", content: " + content);
                mRLService.configMerkleTree(content, (int)length);
                br.close();
            } catch (Exception e) {
                    e.printStackTrace();
            }
        });

        configOsnmaEnablementButton.setOnClickListener(v -> {
            Log.d(TAG, "Enable OSNMA");
            mRLService.configOsnmaEnablement(true);
        });

        configOsnmaDisablementButton.setOnClickListener(v -> {
            Log.d(TAG, "Disable OSNMA");
            mRLService.configOsnmaEnablement(false);
        });

        registerNtnButton.setOnClickListener(v -> {
            Log.d(TAG, "RegisterntnStatusCallback");
            mHandler.obtainMessage(MSG_UPDATE_UI, "RegisterntnStatusCallback").sendToTarget();
            mNtnService.registerNtnStatusCallback(new IZatNtnStatusCallback() {
                public void ntnConfigSignalMaskResponse(boolean isSuccess,
                        int gpsSignalTypeConfigMask) {
                    Log.d(TAG, "ntnConfigSignalMaskResponse: " + isSuccess + ", mask:" +
                            gpsSignalTypeConfigMask);
                    String msg = "ntnConfigSignalMaskResponse: " + isSuccess
                        + ", mask: " + gpsSignalTypeConfigMask;
                    mHandler.obtainMessage(MSG_UPDATE_UI, msg).sendToTarget();
                }

                public void ntnConfigSignalMaskChanged(int gpsSignalTypeConfigMask) {
                    Log.d(TAG, "ntnConfigSignalMaskChanged: mask:" + gpsSignalTypeConfigMask);
                    String msg = "ntnConfigSignalMaskChanged: " +
                        " mask: " + gpsSignalTypeConfigMask;
                    mHandler.obtainMessage(MSG_UPDATE_UI, msg).sendToTarget();
                }
            });
        });

        setNtnButton.setOnClickListener(v -> {
            Log.d(TAG, "SetNTNConfigSignalMask");
            if (!ntnMaskText.getText().toString().equals("")) {
                int t = Integer.parseInt(ntnMaskText.getText().toString());
                mNtnService.setNtnConfigSignalMask(t);
                String msg = "SetNTNConfigSignalMask: " + t;
                mHandler.obtainMessage(MSG_UPDATE_UI, msg).sendToTarget();
            } else {
                Log.e(TAG, "empty paramter in ntnMaskText!");
            }
        });

        getNtnButton.setOnClickListener(v -> {
            Log.d(TAG, "GetNTNConfigSignalMask");
            mNtnService.getNtnConfigSignalMask();
            String msg = "GetNTNConfigSignalMask";
            mHandler.obtainMessage(MSG_UPDATE_UI, msg).sendToTarget();
        });

        setNtnCapTrueButton.setOnClickListener(v -> {
            Log.d(TAG, "setNtnCapability to true");
            mNtnService.set3rdPartyNtnCapability(true);
            String msg = "setNtnCapability to true";
            mHandler.obtainMessage(MSG_UPDATE_UI, msg).sendToTarget();
        });

        setNtnCapFalseButton.setOnClickListener(v -> {
            Log.d(TAG, "setNtnCapability to false");
            mNtnService.set3rdPartyNtnCapability(false);
            String msg = "setNtnCapability to false";
            mHandler.obtainMessage(MSG_UPDATE_UI, msg).sendToTarget();
        });
        injectSuplCertButton.setOnClickListener(v -> {
            //inject supl cert file: sdcard/Android/data/my.tests.snapdragonsdktest/files/supl.der
            String path=getExternalFilesDir(null).getAbsolutePath() + "/supl.der";
            Log.i(TAG, "path : " + path);
            try {
                File suplCert = new File(path);
                FileInputStream fis = new FileInputStream(suplCert);
                byte[] bufferBytes = new byte[(int)suplCert.length()];
                fis.read(bufferBytes);
                Log.i(TAG, "length: " + suplCert.length() + ", bufferBytes :" +
                        Arrays.toString(bufferBytes));
                mGnssService.injectSuplCert(1, bufferBytes);
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
