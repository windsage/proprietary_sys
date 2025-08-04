/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.listen.ListenTypes;
import com.qualcomm.qti.sva.controller.DetectionEventContainer;
import com.qualcomm.qti.sva.controller.ExtendedSmMgr;
import com.qualcomm.qti.sva.controller.Global;
import com.qualcomm.qti.sva.controller.Msg;
import com.qualcomm.qti.sva.controller.MsgResponse;
import com.qualcomm.qti.sva.controller.SMLParametersManager;
import com.qualcomm.qti.sva.data.IExtendedSmModel;
import com.qualcomm.qti.sva.data.ISettingsModel;
import com.qualcomm.qti.sva.data.ISmModel;
import com.qualcomm.qti.sva.data.SettingsModel;
import com.qualcomm.qti.sva.service.WakeupService;
import com.qualcomm.qti.sva.utils.AppPermissionUtils;
import com.qualcomm.qti.sva.utils.FileUtils;
import com.qualcomm.qti.sva.utils.LogUtils;
import com.qualcomm.qti.sva.utils.Utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ServiceActivity {
    private final static int MSG_DETECTED_RESULT_TIMER_TIMEOUT = 1; // internal msg id < 0x1000
    private final static int SHOW_DETECTED_RESULT_LENGTH = 3000;


    private final String TAG = MainActivity.class.getSimpleName();

    private LinearLayout mLayoutDetectedPrompt;
    private TextView mTvDetectedKeyphrase;
    private TextView mTvAction;
    private TextView mTvTopTitle;
    private int mSelectedIndex = -1;
    private ArrayList<IExtendedSmModel> mTrainableSmList;
    private Messenger mClientMessenger;

    // view item
    private ListView mListView;
    private KeyphraseListAdapter mAdapter;
    private ArrayList<IExtendedSmModel> mListData = new ArrayList<>();
    private AlertDialog mDialogSelectKeyphrase;
    private AlertDialog mDialogNext;
    private AlertDialog mDialogTextInput;
    private AlertDialog mDialogAddUser;
    private Timer mDetectedInformationTimer;

    private Handler mHandler = new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(Message msg) {
            LogUtils.d(TAG, "handleMessage: msg.what = " + msg.what);
            switch (msg.what) {
                case Msg.MSG_REGISTER_CLIENT_RSP:
                    break;
                case Msg.MSG_ESTABLISH_SESSION_RSP:
                    onEstablishSessionRsp((MsgResponse) msg.obj);
                    break;
                case Msg.MSG_TERMINATE_SESSION_RSP:
                    onTerminateSessionRsp((MsgResponse) msg.obj);
                    break;
                case Msg.MSG_RELEASE_ALL_SESSIONS_RSP:
                    onReleaseAllSessionsRsp();
                    break;
                case Msg.MSG_DETECTION_IND:
                    onReceivedDetectionInd((DetectionEventContainer) msg.obj);
                    break;
                case Msg.MSG_AUDIO_SERVER_DIED_IND:
                    onReceivedAudioServerDiedInd();
                    break;
                case MSG_DETECTED_RESULT_TIMER_TIMEOUT:
                    onDetectedResultTimeOut();
                    break;
                case Msg.MSG_ENABLED_ON_OFF:
                    IExtendedSmModel smModel = (IExtendedSmModel) msg.obj;
                    smModel.setLABRecordingStatus(false);
                    break;
                case Msg.MSG_QUERY_SOUND_MODEL_DONE:
                    onQuerySoundModelDone();
                    break;
                default:
                    break;
            }

            return true;
        }
    });

    private KeyphraseListAdapter.OnListenerCallback mListItemCallback
            = new KeyphraseListAdapter.OnListenerCallback() {
        @Override
        public void onLeftClicked(KeyphraseListAdapter.RowViewHolder rowViewHolder) {
            LogUtils.d(TAG, "onLeftClicked: clicked smName = "
                    + rowViewHolder.mItemData.getSoundModelFullFileName());
            final String smFullName = rowViewHolder.mItemData.getSoundModelFullFileName();

            if (rowViewHolder.mSwitch.isChecked()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.friendly_tips)
                        .setMessage(R.string.close_session_first)
                        .setCancelable(false)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        sendRequest(Msg.MSG_TERMINATE_SESSION_REQ, smFullName);
                                        startKeyphraseSettingsActivity(smFullName);
                                    }
                                });

                if (!((MainActivity.this).isFinishing())) {
                    builder.show();
                }
            } else {
                startKeyphraseSettingsActivity(smFullName);
            }
        }

        @Override
        public void onRightClicked(KeyphraseListAdapter.RowViewHolder rowViewHolder) {
            LogUtils.d(TAG, "onRightClicked: clicked smName = "
                    + rowViewHolder.mItemData.getSoundModelFullFileName()
                    + ",  checked status = " + rowViewHolder.mSwitch.isChecked()
                    + ", LABRecording status = " + rowViewHolder.mItemData.getLABRecordingStatus());
            boolean switchStatus = rowViewHolder.mSwitch.isChecked();
            boolean sessionStatus = (rowViewHolder.mItemData.getSessionStatus()
                    == IExtendedSmModel.SessionStatus.STARTED);
            boolean isLABRecording = rowViewHolder.mItemData.getLABRecordingStatus();
            if (switchStatus && !sessionStatus) {
                sendRequest(Msg.MSG_ESTABLISH_SESSION_REQ,
                        rowViewHolder.mItemData.getSoundModelFullFileName());
            } else if (!switchStatus && sessionStatus && isLABRecording) {
                Toast.makeText(getApplicationContext(),
                        "LAB Recording, please disable later", Toast.LENGTH_LONG).show();
                updateUI();
            }else if (!switchStatus && sessionStatus) {
                sendRequest(Msg.MSG_TERMINATE_SESSION_REQ,
                        rowViewHolder.mItemData.getSoundModelFullFileName());
            }
        }
    };

    private void onReleaseAllSessionsRsp() {
        updateUI();
    }

    private void onReceivedAudioServerDiedInd() {
        updateUI();
    }

    private void onDetectedResultTimeOut() {
        stopDetectedInformationTimer();
        mTvTopTitle.setVisibility(View.VISIBLE);
        mLayoutDetectedPrompt.setVisibility(View.GONE);
    }

    private void onReceivedDetectionInd(DetectionEventContainer eventContainer) {
        // send intent when action is non NONE
        String smName = eventContainer.getSmFullName();
        IExtendedSmModel extendedSmModel
                = Global.getInstance().getExtendedSmMgr().getSoundModel(smName);
        ISettingsModel mSettingModel = new SettingsModel(getApplicationContext(), smName);
        String actionName = extendedSmModel.getSoundModelActionName();
        boolean isVoiceRequestEnabled = mSettingModel.getVoiceRequestEnabled();
        int voiceRequestLengt = mSettingModel.getVoiceRequestLength();
        if (getString(R.string.none).equals(actionName)) {
            String keyphraseName = extendedSmModel.getSoundModelPrettyKeyphrase();
            mTvAction.setText(actionName);
            if(mSettingModel.getMultiKWThresholdEnabled()){
                mTvDetectedKeyphrase.setText(keyphraseName + "  KW_ID: " +
                        Integer.toString(eventContainer.getConfLevel().getFKeywordID()));
            }else{
                mTvDetectedKeyphrase.setText(keyphraseName);
            }
            mLayoutDetectedPrompt.setVisibility(View.VISIBLE);
            mTvTopTitle.setVisibility(View.GONE);
            startDetectedInformationTimer();
        }
        if(isVoiceRequestEnabled){
            extendedSmModel.setLABRecordingStatus(true);
            Message ms = Message.obtain();
            ms.what = Msg.MSG_ENABLED_ON_OFF;
            ms.obj = extendedSmModel;
            mHandler.sendMessageDelayed(ms, voiceRequestLengt);
        }
    }

    private void onTerminateSessionRsp(MsgResponse response) {
        LogUtils.d(TAG, "onTerminateSessionRsp: response = " + response);
        updateUI();

        if (MsgResponse.Result.FAILURE == response.getResult()) {
            StringBuilder stringBuilder = new StringBuilder();
            String keyphrase = Global.getInstance().getExtendedSmMgr().getSoundModel(
                    response.getSmFullName()).getSoundModelPrettyKeyphrase();
            stringBuilder.append(keyphrase);
            stringBuilder.append(" ");
            stringBuilder.append(getString(R.string.establish_session_fail_code));
            stringBuilder.append(" ");
            stringBuilder.append(response.getErrorCode());
            Toast.makeText(getApplicationContext(),
                    stringBuilder, Toast.LENGTH_LONG).show();
        }
    }

    private void onEstablishSessionRsp(MsgResponse response) {
        LogUtils.d(TAG, "onEstablishSessionRsp: response = " + response);
        updateUI();
        if (MsgResponse.Result.FAILURE == response.getResult()) {
            StringBuilder stringBuilder = new StringBuilder();
            String keyphrase = Global.getInstance().getExtendedSmMgr().getSoundModel(
                    response.getSmFullName()).getSoundModelPrettyKeyphrase();
            stringBuilder.append(keyphrase);
            stringBuilder.append(" ");
            stringBuilder.append(getString(R.string.establish_session_fail_code));
            stringBuilder.append(" ");
            stringBuilder.append(response.getErrorCode());
            Toast.makeText(getApplicationContext(),
                    stringBuilder, Toast.LENGTH_LONG).show();
        }
    }

    private void onQuerySoundModelDone() {
        LogUtils.d(TAG, "onQuerySoundModelDone");
        updateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppPermissionUtils.isAllPermissionsGranted(this,
                AppPermissionUtils.APP_REQUIRED_PERMISSIONS)) {
            return;
        }

        mClientMessenger = new Messenger(mHandler);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, WakeupService.class);
        startForegroundService(intent);
        bindService(intent, getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        updateUI();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (isServiceConnected()) {
            deRegisterClient(Msg.ClientID.CLIENT_ID_MAIN_ACTIVITY);
            unbindService(getServiceConnection());
        }

        if (null != mDialogSelectKeyphrase) {
            mDialogSelectKeyphrase.dismiss();
            mDialogSelectKeyphrase = null;
        }

        if (null != mDialogNext) {
            mDialogNext.dismiss();
            mDialogNext = null;
        }

        if (null != mDialogAddUser) {
            mDialogAddUser.dismiss();
            mDialogAddUser = null;
        }

        if (null != mHandler) {
            mHandler.removeCallbacksAndMessages(null);
        }

        stopDetectedInformationTimer();

        super.onDestroy();
    }

    @Override
    protected void onServiceConnectedCallback() {
        registerClient(Msg.ClientID.CLIENT_ID_MAIN_ACTIVITY);
        initializeUI();
    }

    @Override
    protected void onServiceDisconnectedCallback() {
        deRegisterClient(Msg.ClientID.CLIENT_ID_MAIN_ACTIVITY);
    }

    @Override
    protected Messenger getClientMessenger() {
        return mClientMessenger;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem debugModeItem = menu.findItem(R.id.menu_item_debug_mode);
        debugModeItem.setVisible(Global.isSvaDebugModeEnabled(MainActivity.this));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        LogUtils.d(TAG, "onOptionsItemSelected: selected item " + item.getTitle());
        switch (id) {
            case R.id.menu_item_training:
                handleTrainingMenuItemSelected();
                break;
            case R.id.menu_item_tutorial:
                Intent tutorialIntent = new Intent(MainActivity.this,
                        TutorialOneActivity.class);
                startActivity(tutorialIntent);
                break;
            case R.id.menu_item_version:
                try {
                    String versionNumber = getString(R.string.version_number)
                            + " " + getPackageManager().getPackageInfo(
                            getPackageName(), 0).versionName;
                    Utils.openAlertDialog(MainActivity.this, null, versionNumber);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.menu_item_debug_mode:
                if (Global.isSvaDebugModeEnabled(MainActivity.this)) {
                    Intent intent = new Intent(this,
                            DebugMainActivity.class);
                    startActivity(intent);
                    finish();
                }
                break;
        }
        return true;
    }

    private void handleTrainingMenuItemSelected() {
        LogUtils.d(TAG, "handleTrainingMenuItemSelected: enter");
        ExtendedSmMgr smMgr = Global.getInstance().getExtendedSmMgr();
        if (smMgr.hasActiveSessions()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.friendly_tips)
                    .setMessage(R.string.close_all_sessions_first)
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sendRequest(Msg.MSG_RELEASE_ALL_SESSIONS_REQ, null);
                                    showSelectKeyphraseDialog();
                                }
                            });

            if (!((MainActivity.this).isFinishing())) {
                builder.show();
            }
        } else {
            showSelectKeyphraseDialog();
        }
    }

    private void initializeUI() {
        mTvTopTitle = findViewById(R.id.tv_top_title);
        mLayoutDetectedPrompt = findViewById(R.id.layout_detected_prompt);
        mTvDetectedKeyphrase = findViewById(R.id.tv_detected_keyphrase);
        mTvAction = findViewById(R.id.tv_action);
        mListView = findViewById(R.id.list_keyphrase);
        mListData.clear();
        mListData = Global.getInstance().getExtendedSmMgr().getSingleKeyphraseSoundModelList();
        mAdapter = new KeyphraseListAdapter(this, mListData, mListItemCallback);
        mListView.setAdapter(mAdapter);
    }

    private void updateUI() {
        LogUtils.d(TAG, "updateUI: update list view data");
        mListData.clear();
        mListData = Global.getInstance().getExtendedSmMgr().getSingleKeyphraseSoundModelList();
        if (null != mAdapter) {
            mAdapter.updateData(mListData);
        }
    }

    private void startKeyphraseSettingsActivity(String smFullName) {
        Intent intent = new Intent(getApplicationContext(),
                KeyphraseSettingsActivity.class);
        intent.putExtra(KeyphraseSettingsActivity.KEY_EXTRA_PARAM_SM_NAME, smFullName);
        startActivity(intent);
    }

    // Resets keyword and user detection bars after SHOW_DETECTION_RESULT_LENGTH milliseconds
    private void startDetectedInformationTimer() {
        LogUtils.d(TAG, "startDetectedInformationTimer: enter");
        stopDetectedInformationTimer();
        mDetectedInformationTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(MSG_DETECTED_RESULT_TIMER_TIMEOUT);
            }
        };
        mDetectedInformationTimer.schedule(timerTask, SHOW_DETECTED_RESULT_LENGTH);
    }

    private void stopDetectedInformationTimer() {
        LogUtils.d(TAG, "stopDetectedInformationTimer: enter");
        if (null != mDetectedInformationTimer) {
            mDetectedInformationTimer.cancel();
            mDetectedInformationTimer = null;
        }
    }

    private void showSelectKeyphraseDialog() {
        LogUtils.d(TAG, "showSelectKeyphraseDialog: enter");
        ExtendedSmMgr smMgr = Global.getInstance().getExtendedSmMgr();
        ArrayList<String> keyphraseList = new ArrayList<>();
        mTrainableSmList = smMgr.getTrainableSoundModelList();
        for (IExtendedSmModel item : mTrainableSmList) {
            ISmModel.ModelVersion smVersion = item.getSoundModelVersion();
            String versionString = "(" + smVersion.getLable() + ")";
            keyphraseList.add(item.getSoundModelPrettyKeyphrase() + versionString);
        }

        if (keyphraseList.isEmpty()) {
            LogUtils.d(TAG, "showSelectKeyphraseDialog: no available sound models");
            return;
        }

        keyphraseList.add(getString(R.string.create_your_own_4_0));
        if(Utils.isSupportUDK7()){
            keyphraseList.add(getString(R.string.create_your_own_7_0));
        }
        final CharSequence[] items = keyphraseList.toArray(
                new CharSequence[keyphraseList.size()]);
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getString(R.string.select_one_keyphrase))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        mDialogSelectKeyphrase = null;
                    }
                })
                .setSingleChoiceItems(items, mSelectedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        mSelectedIndex = which;
                        LogUtils.d(TAG, "showSelectKeyphraseDialog:onClick: single choice "
                                + "mSelectedIndex = " + mSelectedIndex);
                    }
                })
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        // get sound model name or keyphrase
                        if (-1 == mSelectedIndex || mSelectedIndex > (mTrainableSmList.size() + 1)) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.select_one_item_first),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        String smName;
                        boolean isUdk;
                        if (mSelectedIndex < mTrainableSmList.size()) {
                            smName = mTrainableSmList.get(mSelectedIndex).getSoundModelFullFileName();
                            isUdk = false;
                        } else {
                            smName = items[mSelectedIndex].toString();
                            isUdk = true;
                        }

                        // dismiss this dialog
                        dialogInterface.dismiss();
                        mDialogSelectKeyphrase = null;
                        mSelectedIndex = -1;

                        LogUtils.d(TAG, "showSelectKeyphraseDialog:onClick: next button "
                                + "selected smName = " + smName);

                        // show name sound model or create keyphrase dialog
                        showNextDialog(smName, isUdk);

                    }
                });

        mDialogSelectKeyphrase = dialogBuilder.show();
    }

    private void showNextDialog(final String smName, final boolean isUdk) {
        LogUtils.d(TAG, "showNextDialog: smName = " + smName);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        String message;
        String title;
        if (isUdk) {
            title = getString(R.string.new_keyphrase_dialog_title);
            message = getString(R.string.new_keyphrase_dialog_message);
        } else {
            title = getString(R.string.new_sm_dialog_title);
            message = getString(R.string.new_sm_dialog_message);
        }

        dialogBuilder.setView(view)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText editText = mDialogNext.findViewById(R.id.et_param);
                        String inputText = editText.getText().toString();
                        String title = getString(R.string.friendly_tips);
                        String message;

                        // check whether is empty or not
                        if ("".equals(inputText)) {
                            LogUtils.d(TAG, "showNextDialog: invalid input value");
                            if (isUdk) {
                                message = getString(R.string.keyphrase_empty);
                            } else {
                                message = getString(R.string.sm_name_empty);
                            }
                            Utils.openAlertDialog(MainActivity.this, title, message);
                            return;
                        }
                        if (isUdk) {
                            ISmModel.ModelVersion version =
                                    Utils.getUdkVersion(MainActivity.this, smName);
                            if (version == ISmModel.ModelVersion.VERSION_7_0) {
                                ByteBuffer langModel = Global.getInstance().getExtendedSmMgr()
                                        .getLanguageModel(getApplicationContext(), version);
                                int result = SMLParametersManager.getInstance()
                                        .setSMLParamsForTextInput(inputText, langModel);
                                if (result != ListenTypes.UdkTextInputQualityPass) {
                                    switch (result) {
                                        case ListenTypes.STATUS_ETOO_SMALL_SYLLABLES:
                                            message = getString(
                                                    R.string.text_input_error_small_syllables);
                                            break;
                                        case ListenTypes.STATUS_ETOO_MANY_SYLLABLES:
                                            message = getString(
                                                    R.string.text_input_error_many_syllables);
                                            break;
                                        case ListenTypes.STATUS_ELOCALE_MISMATCH:
                                            message = getString(
                                                    R.string.text_input_error_locale_mismatch);
                                            break;
                                        default:
                                            message = getString(R.string.sm_error_failed);
                                            break;
                                    }
                                    Utils.openAlertDialog(MainActivity.this, title, message);
                                    return;
                                }
                            }
                        }
                        String keyphraseOrNewSmName = inputText;

                        // check if conflict or not
                        if (!isUdk) {
                            keyphraseOrNewSmName = inputText + ISmModel.SUFFIX_TRAINED_SOUND_MODEL;
                            String newSmPath = Global.PATH_ROOT + "/" + keyphraseOrNewSmName;
                            if (FileUtils.isExist(newSmPath)) {
                                message = getString(R.string.name_conflict_message);
                                Utils.openAlertDialog(MainActivity.this, title, message);
                                return;
                            }
                        } else {
                            // for UDK, do not allowed input the UIM keyphrase
                            ArrayList<String> keyphraseList = Global.getInstance()
                                    .getExtendedSmMgr().getUIMKeyphraseList();
                            for (String item : keyphraseList) {
                                if (keyphraseOrNewSmName.equalsIgnoreCase(item)) {
                                    message = getString(R.string.keyphrase_conflict_message);
                                    Utils.openAlertDialog(MainActivity.this, title, message);
                                    return;
                                }
                            }
                        }

                        if (!isUdk) {
                            LogUtils.d(TAG, "showNextDialog: input new sm name = "
                                    + keyphraseOrNewSmName);
                        } else {
                            LogUtils.d(TAG, "showNextDialog: input keyphrase = "
                                + keyphraseOrNewSmName);
                            if(smName.equalsIgnoreCase(getString(R.string.create_your_own_7_0))){
                                showTextInputDialog(smName, keyphraseOrNewSmName);
                                // dismiss this dialog
                                mDialogNext.dismiss();
                                mDialogNext = null;
                                return;
                            }
                        }

                        // show add new user dialog
                        showAddUserDialog(smName, keyphraseOrNewSmName, null);

                        // dismiss this dialog
                        mDialogNext.dismiss();
                        mDialogNext = null;
                    }
                });

        mDialogNext = dialogBuilder.show();
        EditText editText = mDialogNext.findViewById(R.id.et_param);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Window window = mDialogNext.getWindow();
                    if (null != window) {
                        window.setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            }
        });
        editText.requestFocus();
    }

    private void showTextInputDialog(final String oldSmName, final String keyphraseOrNewSmName) {
        LogUtils.d(TAG, "showTextInputDialog: oldSmName = " + oldSmName
                + " keyphraseOrNewSmName = " + keyphraseOrNewSmName);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);

        dialogBuilder.setView(view)
                .setTitle(R.string.new_textinput_dialog_title)
                .setMessage(R.string.new_textinput_dialog_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText editText = mDialogTextInput.findViewById(R.id.et_param);
                        String inputText = editText.getText().toString();
                        LogUtils.d(TAG, "showTextInputDialog: inputText = "
                                + inputText);
                        if (TextUtils.isEmpty(inputText)) {
                            inputText = null;
                        }

                        // show add new user dialog
                        showAddUserDialog(oldSmName, keyphraseOrNewSmName, inputText);

                        // dismiss this dialog
                        mDialogTextInput.dismiss();
                        mDialogTextInput = null;
                    }
                });

        mDialogTextInput = dialogBuilder.show();
        EditText editText = mDialogTextInput.findViewById(R.id.et_param);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Window window = mDialogTextInput.getWindow();
                    if (null != window) {
                        window.setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            }
        });
        editText.requestFocus();
    }



    private void showAddUserDialog(final String oldSmName, final String keyphraseOrNewSmName,final String textinput) {
        LogUtils.d(TAG, "showAddUserDialog: oldSmName = " + oldSmName
                + " keyphraseOrNewSmName = " + keyphraseOrNewSmName + " textinput = "  + textinput);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        builder.setView(view)
                .setTitle(R.string.add_user_dialog_title)
                .setMessage(R.string.add_user_dialog_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.train, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText editText = mDialogAddUser.findViewById(R.id.et_param);
                        String userName = editText.getText().toString();
                        LogUtils.d(TAG, "showAddUserDialog: userName = " + userName);
                        String title = getString(R.string.friendly_tips);
                        String message;

                        // check whether is empty or not
                        if ("".equals(userName)) {
                            LogUtils.d(TAG, "showAddUserDialog: invalid user name");
                            message = getString(R.string.user_name_empty);
                            Utils.openAlertDialog(MainActivity.this, title, message);
                            return;
                        }

                        // start training activity
                        startTrainingActivity(oldSmName, keyphraseOrNewSmName, textinput, userName);

                        // dismiss the dialog
                        mDialogAddUser.dismiss();
                        mDialogAddUser = null;
                    }
                });

        mDialogAddUser = builder.show();
        EditText editText = mDialogAddUser.findViewById(R.id.et_param);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Window window = mDialogAddUser.getWindow();
                    if (null != window) {
                        window.setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            }
        });
        editText.requestFocus();
    }

    private void startTrainingActivity(String oldSmName, String keyphraseOrNewSmName,
                                       String textinput, String userName) {
        LogUtils.d(TAG, "startTrainingActivity: oldSmName = " + oldSmName
                + " keyphraseOrNewSmName = " + keyphraseOrNewSmName + " textinput = " + textinput
                + " userName = " + userName);

        Intent intent = new Intent(this, TrainingActivity.class);
        intent.putExtra(TrainingActivity.KEY_EXTRA_PREVIOUS_SM_NAME, oldSmName);
        intent.putExtra(TrainingActivity.KEY_EXTRA_KEYPHRASE_OR_NEW_SM_NAME, keyphraseOrNewSmName);
        intent.putExtra(TrainingActivity.KEY_EXTRA_USER_NAME, userName);
        if(textinput != null){
            intent.putExtra(TrainingActivity.KEY_TEXT_INPUT, textinput);
        }
        intent.putExtra(TrainingActivity.KEY_EXTRA_IS_ADD_USER_TO_PREVIOUS_MODEL, false);
        startActivity(intent);
    }
}
