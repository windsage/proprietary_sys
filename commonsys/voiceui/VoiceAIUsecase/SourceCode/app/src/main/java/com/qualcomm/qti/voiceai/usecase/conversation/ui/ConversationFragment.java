/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.conversation.ui;

import static com.qualcomm.qti.voiceai.usecase.Facade.getConversationManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.qti.voiceai.usecase.Facade;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationContent;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationRecord;
import com.qualcomm.qti.voiceai.usecase.conversation.data.Settings;
import com.qualcomm.qti.voiceai.usecase.translation.TranslateProgressListener;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorSupportedLanguage;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorWrapper;
import com.qualcomm.qti.voiceai.usecase.utils.AppPermissionUtils;
import com.qualcomm.qti.voiceai.usecase.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ConversationFragment extends Fragment {
    private static final String TAG = ConversationFragment.class.getSimpleName();
    private TextView mEditText;
    private View mASRButton;
    private TextView mASRButtonText;
    private RecyclerView mList;
    private TranscriptionViewAdapter mAdapter;
    private final ArrayList<ConversationContent> mConversationContents = new ArrayList<>();
    private TranslatorWrapper[] mTranslatorWrappers;
    private String defaultConversationName;

    private final int MAX_TRANSLATOR_NUM = 2;

    private AlertDialog mDialogTextInput;

    private final static DateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    private boolean isListening = false;
    private boolean isConversationStarted = false;
    private ConversationViewModel mConversationViewModel;
    private Observer<ConversationViewModel.AsrResult> mASRResultObserver;
    private int mConversationId = -1;
    private int mTranslatorSize;

    private final ActivityResultLauncher<String> mRequestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean result) {
                            if(!result) {
                                Toast.makeText(getActivity(), "No RECORD_AUDIO Permission",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                mConversationViewModel.setMicPermissionGranted(true);
                            }
                        }
                    });

    public ConversationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_conversation, container, false);
        mEditText = root.findViewById(R.id.text_start_tip);
        mASRButton = root.findViewById(R.id.push_to_talk);
        mASRButton.setBackgroundResource(R.drawable.asr_active);
        mASRButtonText= root.findViewById(R.id.push_to_talk_text);
        mASRButtonText.setText(R.string.voice_input_start_text);
        mList = root.findViewById(R.id.list);
        mList.setVisibility(View.GONE);
        mTranslatorWrappers = new TranslatorWrapper[MAX_TRANSLATOR_NUM];
        mASRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "mASRButton clicked, isListening=" + isListening);
                if(AppPermissionUtils.requestRuntimePermissions(getActivity()))  {
                    Log.d(TAG, "requestRuntimePermissions" );
                    return;
                }
                if(isMicPermissionGranted()) {
                    Log.d(TAG, "isMicPermissionGranted" );
                    isListening = !isListening;
                    mConversationViewModel.setASRListening(isListening);
                } else {
                    requestMicPermission();
                }
            }
        });
        mList.setLayoutManager(new LinearLayoutManager(getActivity()) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                } catch (Exception e) {
                    Log.e(TAG, "catch exception in RecyclerView");
                }
            }
        });
        mAdapter = new TranscriptionViewAdapter(getActivity(), mConversationContents);
        mList.setAdapter(mAdapter);
        mASRResultObserver = result -> {
            ConversationContent conversationContent = null;
            if(mConversationContents.size() > 0) {
                conversationContent = mConversationContents.get(mConversationContents.size() - 1);
            }
            if(conversationContent == null || conversationContent.isFinal()){
                conversationContent = new ConversationContent(++mConversationId,result.getResult());
                mConversationContents.add(conversationContent);
            }else{
                conversationContent.updateTranscriptionContent(result.getResult());
            }
            if(result.isFinal()){
                conversationContent.setFinal(true);
            }
            int index = mConversationId;
            //todo: need add translation to conversation
            for(int i = 0; i < mTranslatorSize; i++) {
                Facade.getTranslationManager().translate(mTranslatorWrappers[i],new TranslateProgressListener() {
                    @Override
                    public void onDone(String translationId, String result) {
                        Log.d(TAG, "onDone " + "translationId : " + translationId
                            + " result : " + result);
                        mConversationContents.get(mConversationContents.size() - 1).
                                addTranslationContent(Integer.parseInt(translationId), result);
                        mAdapter.notifyItemChanged(index);
                    }

                    @Override
                    public void onError(String translationId) {
                        Log.e(TAG, "onError translationId : " + translationId);
                    }
                },Integer.toString(i), result.getResult());
            }
            mAdapter.notifyItemChanged(mConversationId);
            mList.smoothScrollToPosition(mConversationContents.size() -1);
        };
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mConversationViewModel = new ViewModelProvider(requireActivity()).get(ConversationViewModel.class);
        mConversationViewModel.getASRListening().observe(requireActivity(),
                this::updateASRListeningState);
        mConversationViewModel.getASRResult().observe(requireActivity(), mASRResultObserver);
    }

    private boolean isMicPermissionGranted() {
        return Boolean.TRUE.equals(mConversationViewModel.getIsMicPermissionGranted().getValue());
    }

    private void requestMicPermission() {
        mRequestPermission.launch(Manifest.permission.RECORD_AUDIO);
    }

    private void setASRButtonEnabled(boolean enabled) {
        int backgroundId = enabled ? R.drawable.asr_active : R.drawable.asr_deactive;
        mASRButton.setBackground(getResources().getDrawable(backgroundId, null));
        int textId = enabled ? R.string.voice_input_start_text : R.string.voice_input_stop_text;
        mASRButtonText.setText(textId);
    }

    private void updateASRListeningState(boolean listening) {
        Log.d(TAG,"updateASRListeningState listening="+listening);
        isListening = listening;
        mConversationId = -1;
        defaultConversationName = DATE_FORMAT.format(new Date());
        if(isListening){
            if(Settings.getRealtimeModeEnabled(getContext())) {
                mTranslatorSize = 0;
                int indexLanguage =  Settings.getTranscriptionLanguage(getContext());
                String transcriptLanguage =
                        TranslatorSupportedLanguage.getSupportedLanguages().get(indexLanguage);
                Log.d(TAG, "transcriptLanguage = "+ transcriptLanguage);
                for(String item : Settings.getTranslationLanguages(getContext())) {
                    mTranslatorWrappers[mTranslatorSize] = Facade.getTranslationManager().createTranslator(
                            TranslatorSupportedLanguage.convertLanguage(transcriptLanguage),
                            TranslatorSupportedLanguage.convertLanguage(item));
                    Log.d(TAG," item=" + item);
                    mTranslatorSize++;
                }
                mConversationContents.clear();
                mList.setVisibility(View.VISIBLE);
                mEditText.setVisibility(View.GONE);
                mAdapter.setTranslateLanguages(mTranslatorSize);
            } else {
                mList.setVisibility(View.GONE);
                mEditText.setText(R.string.voice_stop_tip);
                mEditText.setVisibility(View.VISIBLE);
            }
            setASRButtonEnabled(false);
            if(!isConversationStarted) {
                isConversationStarted = true;
            }
        }else {
            if(Settings.getRealtimeModeEnabled(getContext())) {
                mList.setVisibility(View.GONE);
                if (isConversationStarted) {
                    mEditText.setText(R.string.voice_input_clip);
                    if (mConversationContents.size() > 0) {
                        showSaveConversationDialog();
                    } else {
                        for (int i = 0; i < mTranslatorSize; i++) {
                            Facade.getTranslationManager().releaseTranslator(mTranslatorWrappers[i]);
                        }
                    }
                }
                mEditText.setVisibility(View.VISIBLE);
                setASRButtonEnabled(true);
            } else {
                if (isConversationStarted) {
                    mEditText.setText(R.string.voice_input_clip);
                    showSaveConversationDialog();
                }
                mEditText.setVisibility(View.VISIBLE);
                setASRButtonEnabled(true);
            }
        }
    }

    final void saveCurrentConversation(String name){
        Log.d(TAG,"saveCurrentConversation");
        ArrayList<String> arrayList = new ArrayList<>(Settings.getTranslationLanguages(getContext()));
        int transcriptionLanguage = Settings.getTranscriptionLanguage(getContext());
        ConversationRecord conversationRecord = new ConversationRecord(defaultConversationName,
                TranslatorSupportedLanguage.getSupportedLanguages().get(transcriptionLanguage),arrayList);
        if(name != null) {
            conversationRecord.setConversationAlias(name);
        }
        for (int i =0; i< mConversationContents.size(); i++){
            conversationRecord.addConversationContent(mConversationContents.get(i));
        }
        getConversationManager().saveConversationRecord(conversationRecord);
        for(int i = 0; i < mTranslatorSize; i++) {
            Facade.getTranslationManager().releaseTranslator(mTranslatorWrappers[i]);
        }
    }

    private void showSaveConversationDialog() {

        AlertDialog.Builder dialogBuilder =new AlertDialog.Builder(getContext(),
                                                android.R.style.Theme_Material_Light_Dialog_Alert);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);

        dialogBuilder.setView(view)
                .setTitle(R.string.save_conversation_title)
                .setCancelable(false)
                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {

                    EditText editText = mDialogTextInput.findViewById(R.id.et_param);
                    String aliasName = editText.getText().toString();
                    Log.d(TAG,"aliasName = " + aliasName);
                    if(Settings.getRealtimeModeEnabled(getContext())) {
                        new Handler(Looper.getMainLooper()).post(()
                                -> saveCurrentConversation(aliasName));
                    } else {
                        BufferModeHandlerThread.addName(defaultConversationName,aliasName);
                    }
                    mDialogTextInput.dismiss();
                    mDialogTextInput = null;
                });

        mDialogTextInput = dialogBuilder.show();
        EditText editText = mDialogTextInput.findViewById(R.id.et_param);
        editText.setHint(defaultConversationName);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG,"afterTextChanged s = " +s.toString());
                if (Facade.getConversationManager().isConversationRecordAliasExists(s.toString())) {
                    Toast.makeText(getContext(),
                            getString(R.string.dup_alias_name), Toast.LENGTH_LONG).show();
                    mDialogTextInput.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                } else if (s.toString().length() > 20) {
                    Toast.makeText(getContext(),
                            getString(R.string.alias_name_too_long), Toast.LENGTH_LONG).show();
                    mDialogTextInput.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    if(!mDialogTextInput.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled()) {
                        mDialogTextInput.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    }
                }
            }
        });
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

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ");
        mConversationViewModel.setASRListening(false);
    }
}