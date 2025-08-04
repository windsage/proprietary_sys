/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.conversation.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qualcomm.qti.voiceai.usecase.Facade;
import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationContent;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationRecord;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorSupportedLanguage;

import java.util.ArrayList;

public class SessionsFragment extends Fragment {
    private static final String TAG = SessionsFragment.class.getSimpleName();
    private ConstraintLayout mConstraintLayoutSummary;
    private ConstraintLayout mConstraintLayoutDetails;
    private RecyclerView mSessionsList;
    private RecyclerView mSessionsDetailList;
    private SessionsViewAdapter mSessionsViewAdapter;
    private TranscriptionViewAdapter mTranscriptionViewAdapter;
    private View mSessionsDelete;
    private TextView mTVConversationName;
    private ImageView mConversationMore;
    private ImageView mConversationBack;
    private android.app.AlertDialog mDialogTextInput;
    private android.app.AlertDialog mDialogDelete;
    private ConversationRecord mCurrent;

    public SessionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_sessions, container, false);
        mConstraintLayoutSummary = root.findViewById(R.id.conversation_fragment_layout_summary);
        mConstraintLayoutDetails = root.findViewById(R.id.conversation_fragment_layout_details);
        mSessionsDelete = root.findViewById(R.id.sessions_delete);
        mTVConversationName =  root.findViewById(R.id.text_conversation_name);
        mConversationBack = root.findViewById(R.id.conversation_back);
        mConversationMore = root.findViewById(R.id.conversation_more);
        mSessionsList = root.findViewById(R.id.sessions_list);
        mSessionsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSessionsDetailList = root.findViewById(R.id.sessions_detail_list);
        mSessionsDetailList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSessionsViewAdapter = new SessionsViewAdapter(Facade.getApplicationContext(),
                Facade.getConversationManager().getAllConversationRecords());

        mTranscriptionViewAdapter = new TranscriptionViewAdapter(Facade.getApplicationContext(),null);
        mSessionsDetailList.setAdapter(mTranscriptionViewAdapter);
        mSessionsViewAdapter.setOnItemClickListener(new SessionsViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position,
                                    SessionsViewAdapter.ViewHolder viewHolder) {
                Log.d(TAG,"onItemClick position=" + position 
                      + " getAdapterPosition="+viewHolder.getAdapterPosition());
                ConversationRecord conversationRecord = Facade.getConversationManager()
                        .getAllConversationRecords().get(viewHolder.getAdapterPosition());
                Facade.getConversationManager().readConversationContentFromLocal(conversationRecord);
                mCurrent = conversationRecord;
                Log.d(TAG," conversationRecord="+conversationRecord);
                Log.d(TAG," conversationRecord.getConversationContents().size()="+conversationRecord.getConversationContents().size());
                for(ConversationContent item : conversationRecord.getConversationContents()){
                    Log.d(TAG, " item ="+item.getTranscriptionContent());

                }
                mTranscriptionViewAdapter.setConversationContents(conversationRecord.getConversationContents());
                ArrayList<String> arrayList = conversationRecord.getTranslationLanguages();
                int[] translateLanguages =  new int[arrayList.size()];
                for(int i =0; i < arrayList.size(); i++) {
                    translateLanguages[i] = TranslatorSupportedLanguage.convert(arrayList.get(i)).ordinal();
                }
                mTranscriptionViewAdapter.setTranslateLanguages(arrayList.size());
                mTranscriptionViewAdapter.notifyDataSetChanged();
                mTVConversationName.setText(TextUtils.isEmpty(
                        conversationRecord.getConversationAlias()) ?
                        conversationRecord.getConversationName() :
                        conversationRecord.getConversationAlias());
                mConstraintLayoutSummary.setVisibility(View.GONE);
                mSessionsList.setVisibility(View.GONE);
                mConstraintLayoutDetails.setVisibility(View.VISIBLE);
                mSessionsDetailList.setVisibility(View.VISIBLE);
            }

            @Override
            public void onItemLongClick(View view, int position,
                                        SessionsViewAdapter.ViewHolder viewHolder) {
                Log.d(TAG,"onItemLongClick position=" + position 
                      + " getAdapterPosition="+viewHolder.getAdapterPosition());
                mSessionsDelete.setVisibility(View.VISIBLE);
                mSessionsDelete.postInvalidate();
            }
        });
        mSessionsList.setAdapter(mSessionsViewAdapter);
        mSessionsDelete.setOnClickListener(v -> {
            ArrayList<ConversationRecord> mDeleteList = mSessionsViewAdapter.getDeleteList();
            for (ConversationRecord conversationRecord : mDeleteList) {
                Facade.getConversationManager().deleteConversationRecord(conversationRecord);
            }
            mSessionsViewAdapter.setSessionsMode(true);
            mSessionsDelete.setVisibility(View.GONE);
            mSessionsViewAdapter.setConversationRecords(
                    Facade.getConversationManager().getAllConversationRecords());
        });

        mConversationMore.setOnClickListener(this::showList);
        mConversationBack.setOnClickListener(v -> {
            mConstraintLayoutDetails.setVisibility(View.GONE);
            mConstraintLayoutSummary.setVisibility(View.VISIBLE);
            mSessionsList.setVisibility(View.VISIBLE);
            mSessionsViewAdapter.setConversationRecords(
                    Facade.getConversationManager().getAllConversationRecords());
            mSessionsViewAdapter.setSessionsMode(true);
        });

        return root;
    }

    public void showList(View view) {
        final String[] items = {"Rename", "Delete"};
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        alertBuilder.setItems(items, (dialogInterface, i) -> {
            if(i == 0) {
                showRenameDialog();
            } else {
                showDeleteDialog();
            }
        });
        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    private void showRenameDialog() {

        android.app.AlertDialog.Builder dialogBuilder =new android.app.AlertDialog.Builder(getContext(),
                android.R.style.Theme_Material_Light_Dialog_Alert);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);

        dialogBuilder.setView(view)
                .setTitle("Rename")
                .setCancelable(true)
                .setNegativeButton("Cancel", (dialog, which) -> mDialogTextInput.dismiss())
                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {

                    EditText editText = mDialogTextInput.findViewById(R.id.et_param);
                    String aliasName = editText.getText().toString();
                    Log.d(TAG,"aliasName = " + aliasName);
                    mCurrent.setConversationAlias(aliasName);
                    Facade.getConversationManager().updateConversationRecordAliasName(mCurrent);
                    mTVConversationName.setText(aliasName);
                    mDialogTextInput.dismiss();
                    mDialogTextInput = null;
                });

        mDialogTextInput = dialogBuilder.show();
        EditText editText = mDialogTextInput.findViewById(R.id.et_param);
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

    private void showDeleteDialog() {

        android.app.AlertDialog.Builder dialogBuilder =new android.app.AlertDialog.Builder(getContext(),
                android.R.style.Theme_Material_Light_Dialog_Alert);
        dialogBuilder.setTitle("Delete Conversation")
                .setCancelable(true)
                .setNegativeButton("Cancel", (dialog, which) -> mDialogDelete.dismiss())
                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                    Facade.getConversationManager().deleteConversationRecord(mCurrent);
                    mDialogDelete.dismiss();
                    mConstraintLayoutDetails.setVisibility(View.GONE);
                    mConstraintLayoutSummary.setVisibility(View.VISIBLE);
                    mSessionsList.setVisibility(View.VISIBLE);
                    mSessionsDetailList.setVisibility(View.GONE);
                    mSessionsViewAdapter.setConversationRecords(
                            Facade.getConversationManager().getAllConversationRecords());

                    mDialogDelete = null;
                    //todo:enter normal mode
                });
        mDialogDelete = dialogBuilder.show();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        mSessionsViewAdapter.setConversationRecords(
                Facade.getConversationManager().getAllConversationRecords());
        Log.d(TAG, "onResume ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ");
    }

}