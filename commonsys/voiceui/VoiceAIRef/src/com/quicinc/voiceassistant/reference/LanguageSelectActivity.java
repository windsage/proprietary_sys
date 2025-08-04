/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.quicinc.voiceassistant.reference.data.Settings;
import com.quicinc.voiceassistant.reference.R;
public class LanguageSelectActivity extends AppCompatActivity {
    private final String[] mASRSupportedLanguage = {"English", "Chinese"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_language_select);
        initView();
    }

    private void initView() {
        ListView languageList = findViewById(R.id.language_list);
        LanguageListAdapter languageAdapter = new LanguageListAdapter(this);
        languageList.setAdapter(languageAdapter);

        languageList.setOnItemClickListener((adapterView, view, position, id) -> {
            Settings.setASRLanguage(LanguageSelectActivity.this,
                    mASRSupportedLanguage[position]);
            languageAdapter.notifyDataSetChanged();
        });

        ImageView back = findViewById(R.id.language_toolbar_button);
        back.setOnClickListener(view -> finish());
    }

    private class LanguageListAdapter extends BaseAdapter {
        private final Context mContext;
        LanguageListAdapter(Context context) {
            mContext = context;
        }
        @Override
        public int getCount() {
            return mASRSupportedLanguage.length;
        }

        @Override
        public String getItem(int position) {
            return mASRSupportedLanguage[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            LanguageItem holder;
            if (convertView != null) {
                view = convertView;
                holder = (LanguageItem) view.getTag();
            } else {
                view = View.inflate(mContext, R.layout.list_item_language, null);
                holder = new LanguageItem();
                holder.mRadioButton = view.findViewById(R.id.language_item_radio);
                holder.mLanguage = view.findViewById(R.id.language_item_name);
                view.setTag(holder);
            }
            String language = mASRSupportedLanguage[position];
            holder.mLanguage.setText(language);
            String asrLanguage = Settings.getASRLanguage(mContext);
            if (TextUtils.isEmpty(asrLanguage)) {
                holder.mRadioButton.setChecked(position == 0);
            } else {
                holder.mRadioButton.setChecked(asrLanguage.equals(language));
            }
            return view;
        }
    }

    private static class LanguageItem {
        RadioButton mRadioButton;
        TextView mLanguage;
    }
}
