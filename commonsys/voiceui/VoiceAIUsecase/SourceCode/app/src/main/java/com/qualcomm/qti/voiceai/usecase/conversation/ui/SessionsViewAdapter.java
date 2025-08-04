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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.qualcomm.qti.voiceai.usecase.Facade;
import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationRecord;

import java.util.ArrayList;

public class SessionsViewAdapter extends RecyclerView.Adapter<SessionsViewAdapter.ViewHolder> {
    private final static String TAG = SessionsViewAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<ConversationRecord> mConversationRecords;
    private ArrayList<ConversationRecord> mDeleteRecords;
    private OnItemClickListener onItemClickListener;
    private boolean isNormalMode;


    /**
     * Instantiates a new Transcription view adapter.
     *
     * @param context the context
     */
    public SessionsViewAdapter(Context context, ArrayList<ConversationRecord> conversationRecords) {
        Log.d(TAG, "test: size ="+conversationRecords.size()+" content:"+conversationRecords);
        mContext = context;;
        mConversationRecords = conversationRecords;
        mDeleteRecords = new ArrayList<>();
        isNormalMode = true;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setConversationRecords(ArrayList<ConversationRecord> conversationRecords) {
        mConversationRecords = conversationRecords;
        Log.d(TAG,"setConversationRecords conversationRecords = "+ conversationRecords);
        notifyDataSetChanged();
    }

    public void setSessionsMode(boolean isNormalMode) {
        this.isNormalMode = isNormalMode;
        notifyDataSetChanged();
    }

    public ArrayList<ConversationRecord> getDeleteList( ) {
        return mDeleteRecords;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder");
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.speech_session, parent, false);

        final ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder");
        try {
            String alisaName = mConversationRecords.get(
                    holder.getAdapterPosition()).getConversationAlias();
            holder.mSessionName.setText(alisaName != null ?
                    alisaName :  mConversationRecords.get(
                            holder.getAdapterPosition()).getConversationName());
            holder.mTranscription_language.setText("Transcription:" +
                    mConversationRecords.get(
                            holder.getAdapterPosition()).getTranscriptionLanguage());
            holder.mTranslate_language.setText("Translation:" +
                    mConversationRecords.get(
                            holder.getAdapterPosition()).getTranslationLanguages().toString());
            if(isNormalMode) {
                holder.mDelete.setVisibility(View.GONE);
                holder.mMore.setVisibility(View.VISIBLE);
            }else {
                holder.mDelete.setVisibility(View.VISIBLE);
                holder.mMore.setVisibility(View.GONE);
            }
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isNormalMode) {
                        onItemClickListener.onItemClick(v, holder.getAdapterPosition(), holder);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onItemClickListener.onItemLongClick(v, holder.getAdapterPosition(), holder);
                setSessionsMode(false);
                return true;
            }
        });
        holder.mDelete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "onCheckedChanged isChecked="+isChecked);
                if(isChecked){
                    mDeleteRecords.add(mConversationRecords.get(holder.getAdapterPosition()));
                }else{
                    mDeleteRecords.remove(mConversationRecords.get(holder.getAdapterPosition()));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount()");
        //return mValues.size();
        try {
            return Facade.getConversationManager().getAllConversationRecords().size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "returning 0");

        return 0;
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
        public final TextView mSessionName;
        public final TextView mTranscription_language;
        public final TextView mTranslate_language;
        public final ImageView mMore;
        public final CheckBox mDelete;

        public ViewHolder(View view) {
            super(view);
            Log.d(TAG, "new ViewHolder");
            mView = view;
            mSessionName = view.findViewById(R.id.session_name);
            mTranscription_language = view.findViewById(R.id.transcription_language);
            mTranslate_language = view.findViewById(R.id.transcription_translate_language);
            mMore = view.findViewById(R.id.segment_arrow);
            mDelete = view.findViewById(R.id.segment_delete);

        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position, ViewHolder viewHolder);
        void onItemLongClick(View view, int position, ViewHolder viewHolder);
    }

}