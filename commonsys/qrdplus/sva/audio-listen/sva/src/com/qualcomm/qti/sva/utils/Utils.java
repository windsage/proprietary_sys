/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.qualcomm.listen.ListenSoundModel;
import com.qualcomm.listen.ListenTypes;

import com.qualcomm.qti.sva.controller.SMLParametersManager;
import com.qualcomm.qti.sva.R;
import com.qualcomm.qti.sva.data.ISmModel;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static boolean isAtLeastN() {
        return Build.VERSION.SDK_INT >= 24;
    }

    public static void openAlertDialog(Context context, String title, String message) {
        if (null == context || null == message) {
            LogUtils.e(TAG, "openAlertDialog: invalid input params");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (null == title) {
            title = context.getResources().getString(R.string.app_name);
        }
        LogUtils.d(TAG, "openAlertDialog: title= " + title);
        LogUtils.d(TAG, "openAlertDialog: message= " + message);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, null);

        if (!((Activity) context).isFinishing()) {
            builder.show();
        }
    }

    public static String getListenErrorMsg(Context context, int error) {
        if (context != null) {
            switch (error) {
                case ListenTypes.STATUS_EBAD_PARAM:
                    return context.getString(R.string.sm_error_bad_param);
                case ListenTypes.STATUS_EKEYWORD_NOT_IN_SOUNDMODEL:
                    return context.getString(R.string.sm_error_keyword_not_found);
                case ListenTypes.STATUS_EUSER_NOT_IN_SOUNDMODEL:
                    return context.getString(R.string.sm_error_user_not_found);
                case ListenTypes.STATUS_EKEYWORD_USER_PAIR_NOT_IN_SOUNDMODEL:
                    return context.getString(R.string.sm_error_user_kw_not_active);
                case ListenTypes.STATUS_ENOT_SUPPORTED_FOR_SOUNDMODEL_VERSION:
                    return context.getString(R.string.sm_error_version_unsupported);
                case ListenTypes.STATUS_EUSER_KEYWORD_PAIRING_ALREADY_PRESENT:
                    return context.getString(R.string.sm_error_user_data_present);
                case ListenTypes.STATUS_ESOUNDMODELS_WITH_SAME_KEYWORD_CANNOT_BE_MERGED:
                    return context.getString(R.string.sm_error_duplicate_keyword);
                case ListenTypes.STATUS_ESOUNDMODELS_WITH_SAME_USER_KEYWORD_PAIR_CANNOT_BE_MERGED:
                    return context.getString(R.string.sm_error_duplicate_user_kw_air);
                case ListenTypes.STATUS_EMAX_KEYWORDS_EXCEEDED:
                    return context.getString(R.string.sm_error_max_keywords_exceeded);
                case ListenTypes.STATUS_EMAX_USERS_EXCEEDED:
                    return context.getString(R.string.sm_error_max_users_exceeded);
                case ListenTypes.STATUS_ECANNOT_DELETE_LAST_KEYWORD:
                    return context.getString(R.string.sm_error_last_keyword);
                case ListenTypes.STATUS_ENO_SPEACH_IN_RECORDING:
                    return context.getString(R.string.sm_error_no_signal);
                case ListenTypes.STATUS_ETOO_MUCH_NOISE_IN_RECORDING:
                    return context.getString(R.string.sm_error_low_snr);
                case ListenTypes.STATUS_ERECORDING_TOO_SHORT:
                    return context.getString(R.string.sm_error_recording_too_short);
                case ListenTypes.STATUS_ERECORDING_TOO_LONG:
                    return context.getString(R.string.sm_error_recording_too_long);
                case ListenTypes.STATUS_ECHOPPED_SAMPLE:
                    return context.getString(R.string.sm_error_chopped_sample);
                case ListenTypes.STATUS_ECLIPPED_SAMPLE:
                    return context.getString(R.string.sm_error_clipped_sample);
                case ListenTypes.STATUS_BAD_UDK_RECORDING_QUALITY:
                    return context.getString(R.string.sm_error_bad_quality);
            }
        } else {
            LogUtils.e(TAG, "getListenErrorMsg: invalid input params");
        }
        return context.getString(R.string.sm_error_failed);
    }

    public static boolean isSupportImprovedTraining() {
        int result = ListenTypes.STATUS_EFUNCTION_NOT_IMPLEMENTED;
        boolean isAvalibleVersion = false;
        ListenTypes.SMLVersion smlVersion = new ListenTypes.SMLVersion();
        try {
            result = ListenSoundModel.getSMLVersion(smlVersion);
            isAvalibleVersion =
                    Long.toHexString(smlVersion.version).compareToIgnoreCase(
                            "601010000000000") >= 0;
            LogUtils.d(TAG, "isSupportImprovedTraining: SML version: ret "
                        + result + ",version 0x" + Long.toHexString(smlVersion.version));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result == ListenTypes.STATUS_SUCCESS && isAvalibleVersion;
    }

    public static boolean isSupportUDK7() {
        int result = ListenTypes.STATUS_EFUNCTION_NOT_IMPLEMENTED;
        boolean isAvalibleVersion = false;
        ListenTypes.SMLVersion smlVersion = new ListenTypes.SMLVersion();
        try {
            result = ListenSoundModel.getSMLVersion(smlVersion);
            isAvalibleVersion =
                    Long.toHexString(smlVersion.version).compareToIgnoreCase(
                            "700040101000000") >= 0;
            LogUtils.d(TAG, "isSupportUDK7: SML version: ret "
                    + result + ",version 0x" + Long.toHexString(smlVersion.version));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result == ListenTypes.STATUS_SUCCESS && isAvalibleVersion;
    }

    public static boolean isSupportUV72() {
        int result = ListenTypes.STATUS_EFUNCTION_NOT_IMPLEMENTED;
        boolean isAvalibleVersion = false;
        ListenTypes.SMLVersion smlVersion = new ListenTypes.SMLVersion();
        try {
            result = ListenSoundModel.getSMLVersion(smlVersion);
            isAvalibleVersion =
                    Long.toHexString(smlVersion.version).compareToIgnoreCase(
                            "800010102000000") >= 0;
            LogUtils.d(TAG, "isSupportUV72: SML version: ret "
                    + result + ",version 0x" + Long.toHexString(smlVersion.version));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result == ListenTypes.STATUS_SUCCESS && isAvalibleVersion;
    }

    public static boolean isSMLVersionMoreThan9() {
        int result = ListenTypes.STATUS_EFUNCTION_NOT_IMPLEMENTED;
        boolean isAvalibleVersion = false;
        ListenTypes.SMLVersion smlVersion = new ListenTypes.SMLVersion();
        try {
            result = ListenSoundModel.getSMLVersion(smlVersion);
            isAvalibleVersion =
                    Long.toHexString(smlVersion.version).compareToIgnoreCase(
                            "900010007000000") >= 0;
            LogUtils.d(TAG, "isSMLVersionMoreThan9: SML version: ret "
                    + result + ",version 0x" + Long.toHexString(smlVersion.version));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result == ListenTypes.STATUS_SUCCESS && isAvalibleVersion;
    }

    public static ISmModel.ModelVersion getUdkVersion(Context context, String smName) {
        if (context.getString(R.string.create_your_own_3_0).equals(smName)) {
            return ISmModel.ModelVersion.VERSION_3_0;
        } else if(context.getString(R.string.create_your_own_4_0).equals(smName)){
            return ISmModel.ModelVersion.VERSION_4_0;
        } else if(context.getString(R.string.create_your_own_5_0).equals(smName)){
            return ISmModel.ModelVersion.VERSION_5_0;
        } else if(context.getString(R.string.create_your_own_6_0).equals(smName)){
            return ISmModel.ModelVersion.VERSION_6_0;
        } else if(context.getString(R.string.create_your_own_7_0).equals(smName)){
            return ISmModel.ModelVersion.VERSION_7_0;
        } else {
            return ISmModel.ModelVersion.VERSION_UNKNOWN;
        }
    }

    public static void setUpEdgeToEdge(@NonNull Activity activity) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                    int statusBarHeight = activity.getWindow().getDecorView().getRootWindowInsets()
                            .getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    int actionBarHeight = 0;
                    TypedValue tv = new TypedValue();
                    if (activity.getTheme().resolveAttribute(
                            android.R.attr.actionBarSize, tv, true))
                    {
                        actionBarHeight = TypedValue.complexToDimensionPixelSize(
                                tv.data,activity.getResources().getDisplayMetrics());
                    }
                    v.setPadding(insets.left, statusBarHeight + actionBarHeight,
                            insets.right, insets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
    }
}
