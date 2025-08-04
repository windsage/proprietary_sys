/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package my.tests.snapdragonsdktest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.graphics.Color;
import android.location.Location;

import com.qti.location.sdk.IZatManager;
import com.qti.location.sdk.IZatPrecisePositioningService;
import com.qti.location.sdk.IZatPrecisePositioningService.IZatPrecisePositioningRequest;
import com.qti.location.sdk.IZatPrecisePositioningService.IZatPreciseType;
import com.qti.location.sdk.IZatPrecisePositioningService.IZatCorrectionType;
import com.qti.location.sdk.IZatIllegalArgumentException;
import com.qti.location.sdk.IZatFeatureNotSupportedException;
import com.qti.location.sdk.IZatGnssConfigServices;
import com.qti.location.sdk.IZatGnssConfigServices.IZatPreciseLocationConfigService;
import com.qti.location.sdk.IZatGnssConfigServices.IZatPreciseLocationConfigService.*;

public class PrecisePositionActivity extends Activity {
    private static String TAG = "PrecisePositionTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private IZatManager mIzatMgr;
    private IZatPrecisePositioningService mPpService;
    private IZatGnssConfigServices mConfigService = null;
    private IZatPreciseLocationConfigService mPreciseConfig = null;
    private ListView mListView;
    ArrayAdapter<String> mAdapter;
    ArrayList<String> mResult = new ArrayList<String>();
    String mUserName;
    String mHostName;
    String mPassword;
    String mMountPoint;
    int mPort;
    int mNmeaInterval;
    // one instance variables
    ArrayAdapter<String> mPpSessionListadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate, this " + this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pp);
        doInit();
    }

    @Override
    protected void onDestroy() {
        if (VERBOSE) {
            Log.v(TAG, "onDestroy session");
        }
        mPpService.stopPrecisePositioningSession();
        super.onDestroy();
    }

    private void doInit() {
        mListView = findViewById(R.id.log);
        mAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, mResult);
        mListView.setAdapter(mAdapter);
        mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        try {
            mIzatMgr = IZatManager.getInstance(getApplicationContext());
            mPpService = mIzatMgr.connectToPrecisePositioningService();
            mConfigService = mIzatMgr.connectGnssConfigServices();
            if (null != mConfigService) {
                mPreciseConfig = mConfigService.getPreciseLocationConfigService();
            }
        } catch (SecurityException e) {
            return;
        } catch (IZatFeatureNotSupportedException e) {
            Log.d(TAG, "precise position is not supported!");
        }
        if (mPpService == null) {
            if (VERBOSE) {
                Log.v(TAG, "Failed to get Precise Position Service");
            }
        }

        if (VERBOSE) {
            Log.v(TAG, "SDK and Service Version:" + mIzatMgr.getVersion());
        }

        final Button startPpSession = (Button) findViewById(R.id.startBtn);
        final Button stopPpSession = (Button) findViewById(R.id.stopBtn);
        final Button ntripConfig = (Button) findViewById(R.id.ntripBtn);

        startPpSession.setOnClickListener(v -> {
                showStartDialog();
        });

        stopPpSession.setOnClickListener(v -> {
            Log.v(TAG, "click on stop session");
            try {
                mPpService.stopPrecisePositioningSession();

            } catch (IZatIllegalArgumentException e) {
                Log.e(TAG, e.getMessage());
            } catch (IZatFeatureNotSupportedException e) {
                Log.e(TAG, e.getMessage());
            }
        });

        ntripConfig.setOnClickListener(v -> {
                showNtripDialog();
        });
    } // doInit

    private void showStartDialog() {
        Log.v(TAG, "showStartDialog entry");
        LayoutInflater inflater;
        View promptsView;

        // get the prompts view
        inflater = LayoutInflater.from(this);
        promptsView = inflater.inflate(R.layout.ppprompts, null);

        final EditText editTimeInterval =
            (EditText)(promptsView.findViewById(R.id.editTextTimeInterval));
        final EditText editPreciseType =
            (EditText)(promptsView.findViewById(R.id.editTextPreciseType));
        final EditText editCorrectionType =
            (EditText)(promptsView.findViewById(R.id.editTextCorrectionType));

        // Default values to speed-up quick tests
        editTimeInterval.setText("1000");
        editPreciseType.setText("2");
        editCorrectionType.setText("1");

        AlertDialog.Builder alertDialogBuilder =
            new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        // set dialog message
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Start",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                long timeInterval = Long.parseLong(editTimeInterval.getText().toString());
                int preciseType =
                    Integer.parseInt(editPreciseType.getText().toString());
                int correctionType = Integer.parseInt(editCorrectionType.getText().toString());
                Log.d(TAG, "timeInterval=" + timeInterval + ", preciseType=" + preciseType +
                        ", correctionType=" + correctionType);

                IZatPrecisePositioningService.IZatPrecisePositioningRequest pPRequest =
                        new IZatPrecisePositioningRequest(timeInterval,
                        IZatPreciseType.fromInt(preciseType),
                        IZatCorrectionType.fromInt(correctionType));

                PrecisePositionCallback locationCb =
                        new PrecisePositionCallback(PrecisePositionActivity.this);
                try {
                    mPpService.startPrecisePositioningSession(locationCb, pPRequest);
                } catch (IZatFeatureNotSupportedException e) {
                    Log.e(TAG, e.getMessage());
                } catch(RuntimeException e) {
                    Log.e(TAG, "Failed to start the session !!");
                }
            }
        });

        alertDialogBuilder.setNegativeButton("Cancel",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                dialog.cancel();
            }
        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    } // showAddDialog


    private void showNtripDialog() {
        Log.v(TAG, "showNtripDialog entry");
        LayoutInflater inflater;
        View promptsView;

        // get the prompts view
        inflater = LayoutInflater.from(this);
        promptsView = inflater.inflate(R.layout.ntripprompts, null);

        final EditText editHostName  =
            (EditText)(promptsView.findViewById(R.id.editTextHostName));
        final EditText editPort =
            (EditText)(promptsView.findViewById(R.id.editTextPort));
        final EditText editUserName =
            (EditText)(promptsView.findViewById(R.id.editTextUserName));
        final EditText editPassword =
            (EditText)(promptsView.findViewById(R.id.editTextPassword));
        final EditText editPoint =
            (EditText)(promptsView.findViewById(R.id.editTextPoint));
        final EditText editNmeaInterval =
            (EditText)(promptsView.findViewById(R.id.editTextNmeaInterval));
        final CheckBox enableOPTIN = (CheckBox) (promptsView.findViewById(R.id.checkboxOPTIN));
        final CheckBox enableNmea = (CheckBox) (promptsView.findViewById(R.id.checkboxnmea));
        final CheckBox enableSSL = (CheckBox) (promptsView.findViewById(R.id.checkboxSSL));

        // last values to speed-up quick tests
        editHostName.setText(mHostName);
        editPort.setText(String.valueOf(mPort));
        editUserName.setText(mUserName);
        editPassword.setText(mPassword);
        editPoint.setText(mMountPoint);
        editNmeaInterval.setText(String.valueOf(mNmeaInterval));

        AlertDialog.Builder alertDialogBuilder =
            new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        // set dialog message
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Enable",
            new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "onClick Enable");
                mNmeaInterval = Integer.parseInt(editNmeaInterval.getText().toString());
                mPort = Integer.parseInt(editPort.getText().toString());
                mUserName = editUserName.getText().toString();
                mHostName = editHostName.getText().toString();
                mMountPoint = editPoint.getText().toString();
                mPassword = editPassword.getText().toString();
                boolean isOPTIN = enableOPTIN.isChecked();
                boolean isNmea = enableNmea.isChecked();
                boolean isSSL = enableSSL.isChecked();
                Log.d(TAG, "hostName=" + mHostName + ", port=" + mPort + ", userName=" + mUserName +
                        ", password=" + mPassword + ", mountPoint=" + mMountPoint +", nmeaInterval="
                        + mNmeaInterval + ", isNmea=" + isNmea + ", isOPTIN=" + isOPTIN + ", isSSL="
                        + isSSL);

                try {
                    if (null != mPreciseConfig) {
                        if (isOPTIN == true) {
                            mPreciseConfig.setPreciseLocationOptIn(
                                    IZatPreciseLocationOptIn.OPTED_IN_FOR_LOCATION_REPORT);
                        } else {
                            mPreciseConfig.setPreciseLocationOptIn(
                                    IZatPreciseLocationOptIn.NOT_OPTED_IN_FOR_LOCATION_REPORT);
                        }
                        IZatPreciseLocationNTRIPSettings settings =
                                new IZatPreciseLocationNTRIPSettings(mHostName,
                                        mMountPoint,
                                        mPort,
                                        mUserName,
                                        mPassword,
                                        isSSL,
                                        mNmeaInterval);
                        mPreciseConfig.enablePreciseLocation(settings, isNmea);
                    }

                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to enable Ntrip configuration!");
                }
            }
        });

        alertDialogBuilder.setNegativeButton("Disable",
            new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(TAG, "onClick Disable");
                if (null != mPreciseConfig) {
                    mPreciseConfig.disablePreciseLocation();
                }
                dialog.cancel();
            }
        });

        alertDialogBuilder.setNeutralButton("Cancel",
            new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(TAG, "onClick Cancel");
                dialog.cancel();
            }
        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    } // showAddDialog

    public void showResultsOnUI(String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mResult.size() >= 15) {
                    mResult.remove(0);
                }
                mResult.add(result);

                if (null != mListView) {
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }
  }
