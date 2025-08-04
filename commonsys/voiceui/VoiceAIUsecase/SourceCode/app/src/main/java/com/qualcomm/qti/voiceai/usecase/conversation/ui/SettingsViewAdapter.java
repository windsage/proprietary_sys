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
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.conversation.data.Settings;
import com.qualcomm.qti.voiceai.usecase.translation.TranslationManager;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorSupportedLanguage;
import com.qualcomm.qti.voiceai.usecase.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;

public class SettingsViewAdapter extends RecyclerView.Adapter<SettingsViewAdapter.ViewHolder> {
    private final static String TAG = SettingsViewAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<String> mTranslatorSupportedLanguage;

    /**
     * Instantiates a new Settings view adapter.
     *
     * @param context the context
     */
    public SettingsViewAdapter(Context context, ArrayList<String> translatorSupportedLanguage) {
        mContext = context;
        mTranslatorSupportedLanguage = translatorSupportedLanguage;
        Log.d(TAG, "SettingsViewAdapter mTranslatorSupportedLanguage =" + mTranslatorSupportedLanguage);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder");
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.translate_language, parent, false);
        final ViewHolder holder = new SettingsViewAdapter.ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder");
        try {
            Log.d(TAG," LANGUAGE="+mTranslatorSupportedLanguage.get(holder.getAdapterPosition())+
                    " holder.getAdapterPosition()="+holder.getAdapterPosition()+"  isChecked= "
                    + holder.mCheckBox.isChecked());
            boolean shouldCheck = false;
            for(String item : Settings.getTranslationLanguages(mContext)){
                if(item.equals(mTranslatorSupportedLanguage.get(holder.getAdapterPosition()))){
                    // transcription and translate language should different
                    if(Utils.transcribeLanguageNames[Settings.getTranscriptionLanguage(mContext)].
                            equals(Utils.translateLanguageNames[holder.getAdapterPosition()])){
                        HashSet<String> hashSet = Settings.getTranslationLanguages(mContext);
                        hashSet.remove(mTranslatorSupportedLanguage.get(holder.getAdapterPosition()));
                        Settings.setTranslationLanguages(mContext, hashSet);
                    }else {
                        if(!holder.mCheckBox.isChecked()) {
                            Log.d(TAG, " setChecked(true)");
                            holder.mCheckBox.setChecked(true);
                        }
                    }
                    shouldCheck = true;
                }
            }
            if(!shouldCheck){
                holder.mCheckBox.setChecked(false);
            }
            holder.mTranscription_translate_language.
                    setText(Utils.translateLanguageNames[holder.getAdapterPosition()]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG," onCheckedChanged Position=" + holder.getAdapterPosition()
                        + " isChecked="+isChecked);
                if(!TranslationManager.isAllModesDownloaded()) {
                    if(isChecked) {
                        Toast.makeText(mContext,"Please check network and wait for models downloaded",
                                Toast.LENGTH_LONG).show();
                        buttonView.setChecked(false);
                        return;
                    }
                }
                HashSet<String> hashSet = Settings.getTranslationLanguages(mContext);
                if(isChecked && !hashSet.isEmpty()) {
                    for(String item : hashSet) {
                        if(item.equals(mTranslatorSupportedLanguage.get(holder.getAdapterPosition()))){
                            return;
                        }
                    }
                }
                if(isChecked) {
                    if((!hashSet.isEmpty()) && hashSet.size() >= 2){
                        Toast.makeText(mContext," Only allow 2 languages for translation",
                                Toast.LENGTH_LONG).show();
                        buttonView.setChecked(false);
                        return;
                    }
                    if(Settings.getTranscriptionLanguage(mContext) == holder.getAdapterPosition()){
                        holder.mCheckBox.setChecked(false);
                        Toast.makeText(mContext," Transcript and translate language can not same",
                                Toast.LENGTH_LONG).show();
                        buttonView.setChecked(false);
                        return;
                    }
                    hashSet.add(mTranslatorSupportedLanguage.get(holder.getAdapterPosition()));
                    Log.d(TAG, "onCheckedChanged isChecked = "+ isChecked +" hashSet="+ hashSet);
                    Settings.setTranslationLanguages(mContext, hashSet);
                }else{
                    if (hashSet.isEmpty()) {
                        return;
                    }
                    hashSet.remove(mTranslatorSupportedLanguage.get(holder.getAdapterPosition()));
                    Log.d(TAG, "onCheckedChanged isChecked = "+ isChecked +" hashSet="+ hashSet);
                    Settings.setTranslationLanguages(mContext, hashSet);
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount()");
        //return mValues.size();
        try {
            return mTranslatorSupportedLanguage.size();
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

        public final CheckBox mCheckBox;
        public final TextView mTranscription_translate_language;


        /**
         * Instantiates a new View holder.
         *
         * @param view the view
         */
        public ViewHolder(View view) {
            super(view);
            Log.d(TAG, "new ViewHolder");
            mView = view;
            mCheckBox = view.findViewById(R.id.language_id);
            mTranscription_translate_language =
                    view.findViewById(R.id.settings_transcription_translate_language);
        }
    }
}