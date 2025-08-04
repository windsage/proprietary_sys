/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.dspasr;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import com.qualcomm.qti.voiceai.dspasr.data.Settings;
import com.qualcomm.qti.voiceai.dspasr.R;
public class LanguageSelectActivity extends AppCompatActivity {
    private final String[] mASRSupportedLanguage = { "EN_US",  "ZH_CN",  "GR_DE",  "ES_US",
      "RS_RU",  "KO_KR",  "FR_FC",  "JA_JP",  "PT_PU",  "TR_TK",  "PO_PL",  "CA_CT",  "DU_DT",
      "AR_AB",  "SW_SD",  "IT_IA",  "IN_ID",  "IN_HI",  "FI_FN",  "VI_VT",  "HE_HB",  "UK_UR",
      "GR_GK",  "MA_ML",  "CZ_CH",  "RO_RM",  "DA_DN",  "HU_HG",  "IN_TM",  "NO_NW",  "TH_TA",
      "IN_UR",  "CR_CT",  "BL_BG",  "LI_LT",  "LA_LT",  "MA_MO",  "IN_ML",  "WL_CY",  "SO_SK",
      "IN_TG",  "PE_PR",  "LA_LV",  "IN_BN",  "SE_SR",  "AZ_AB",  "SL_SV",  "IN_KD",  "ES_ET",
      "MC_MD",  "BR_TN",  "BS_BQ",  "IS_ID",  "AR_AM",  "NP_NA",  "MN_MG",  "BS_BN",  "KZ_KH",
      "AL_AN",  "SW_SH",  "GA_GL",  "IN_MR",  "IN_PB",  "SI_SH",  "KM_KH",  "SH_SO",  "YO_RB",
      "SO_SM",  "AF_AK",  "OC_TA",  "GE_GA",  "BE_LA",  "TA_JI",  "IN_SD",  "IN_GJ",  "AH_MR",
      "YI_DH",  "LA_LO",  "UZ_BK",  "FA_RS",  "HT_CL",  "PK_PT",  "TK_MN",  "NY_SK",  "ML_TS",
      "IN_SK",  "LX_BG",  "MY_MR",  "TB_TA",  "TA_LG",  "ML_GS",  "IN_AS",  "TA_TR",  "HW_WN",
      "LN_GL",  "HA_US",  "BS_KR",  "JV_SE",  "SD_NS"};


    private static final int UPDATE_ASR_CONFIG = 2000;
    private Handler mHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
            SystemBarStyle.dark(getResources().getColor(R.color.background, getTheme())));
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_language_select);
        initView();
        mHandler = ClientApplication.getInstance().getHandler();
    }

    private void initView() {
        ListView languageList = findViewById(R.id.language_list);
        LanguageListAdapter languageAdapter = new LanguageListAdapter(this);
        languageList.setAdapter(languageAdapter);

        languageList.setOnItemClickListener((adapterView, view, position, id) -> {
            Settings.setASRLanguage(LanguageSelectActivity.this,
                    mASRSupportedLanguage[position]);
            updateConfig();
            languageAdapter.notifyDataSetChanged();
        });

        ImageView back = findViewById(R.id.language_toolbar_button);
        back.setOnClickListener(view -> finish());
    }

    private void updateConfig() {
        mHandler.sendEmptyMessage(UPDATE_ASR_CONFIG);
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
