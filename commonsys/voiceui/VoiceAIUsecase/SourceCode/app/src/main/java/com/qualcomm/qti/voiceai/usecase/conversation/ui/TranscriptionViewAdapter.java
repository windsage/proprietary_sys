/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.conversation.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationContent;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationRecord;

import java.util.ArrayList;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ConversationRecord}.
 */
public class TranscriptionViewAdapter extends RecyclerView.Adapter<TranscriptionViewAdapter.ViewHolder> {
    private final static String TAG = TranscriptionViewAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<ConversationContent> mConversationContents;
    private int mTranslateLanguageSize;

    /**
     * Instantiates a new Transcription view adapter.
     *
     * @param context the context
     */
    public TranscriptionViewAdapter(Context context, ArrayList<ConversationContent> conversationContents) {
        //Log.d(TAG, items.toString());
        mContext = context;
        mConversationContents = conversationContents;
    }

    public void setTranslateLanguages(int translateLanguageSize) {
        mTranslateLanguageSize = translateLanguageSize;
    }

    public void setConversationContents(ArrayList<ConversationContent> conversationContents) {
        mConversationContents = conversationContents;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder");
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.speech_segment, parent, false);

        final ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder : " + position);
        try {
            holder.mTranscription.setText(
                    mConversationContents.get(position).getTranscriptionContent());
            if(mTranslateLanguageSize == 2) {
                holder.mTranscription_translate_1.setText(
                        mConversationContents.get(position)
                                .getTranslationContents().get(0));
                holder.mTranscription_translate_1.setVisibility(View.VISIBLE);
                holder.mTranscription_translate_2.setText(
                        mConversationContents.get(position)
                                .getTranslationContents().get(1));
                holder.mTranscription_translate_2.setVisibility(View.VISIBLE);
            } else if(mTranslateLanguageSize == 1 ) {
                holder.mTranscription_translate_1.setText(
                        mConversationContents.get(position)
                                .getTranslationContents().get(0));
                holder.mTranscription_translate_1.setVisibility(View.VISIBLE);
                holder.mTranscription_translate_2.setVisibility(View.GONE);
            } else {
                holder.mTranscription_translate_1.setVisibility(View.GONE);
                holder.mTranscription_translate_2.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        int result = (mConversationContents == null) ? 0 : mConversationContents.size();
        Log.d(TAG, "getItemCount() : " + result);
        return result;
    }


    /**
     * The type View holder.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        /**
         * The M view.
         */
        public final View mView;

        /**
         * The M transcription.
         */
        public final TextView mTranscription;

        /**
         * The M id.
         */
        public final TextView mTranscription_translate_1;
        public final TextView mTranscription_translate_2;


        /**
         * Instantiates a new View holder.
         *
         * @param view the view
         */
        public ViewHolder(View view) {
            super(view);
            Log.d(TAG, "new ViewHolder");
            mView = view;
            mTranscription = view.findViewById(R.id.transcription);
            mTranscription_translate_1 = view.findViewById(R.id.transcription_translate_1);
            mTranscription_translate_2 = view.findViewById(R.id.transcription_translate_2);
        }
    }
}