/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.configuration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.quicinc.voice.activation.aidl.IWakewordSettingsService;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.utility.Constants;

public class ConfigurationConnector extends AbstractConnector {

    public final static String WAKEWORD_SETTINGS_SERVICE_NAME =
            "com.quicinc.voice.activation.engineservice.WakewordSettingsService";
    private final static Intent WAKEWORD_SETTINGS_SERVICE_INTENT = new Intent()
            .setClassName(Constants.QVA_PACKAGE_NAME, WAKEWORD_SETTINGS_SERVICE_NAME);

    public ConfigurationConnector(Context context) {
        super(context, WAKEWORD_SETTINGS_SERVICE_INTENT);
    }

    IWakewordSettingsService getSettingsService() {
        return IWakewordSettingsService.Stub.asInterface(getBinder());
    }

    public boolean setParams( Bundle bundle) {
        return WakewordSettingsManager.setParams(this, bundle);
    }

    public Bundle getParams(Bundle bundle) {
        return WakewordSettingsManager.getParams(this, bundle);
    }
}
