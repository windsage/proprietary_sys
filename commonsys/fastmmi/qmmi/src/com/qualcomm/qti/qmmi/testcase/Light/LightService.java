/*
 * Copyright (c) 2017, 2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.qmmi.testcase.Light;

import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.util.function.Supplier;

import android.provider.Settings;

import android.hardware.light.HwLightState;
import android.hardware.light.ILights;
import android.hardware.light.LightType;
import android.hardware.light.FlashMode;
import android.hardware.light.BrightnessMode;

import com.qualcomm.qti.qmmi.R;
import com.qualcomm.qti.qmmi.bean.TestCase;
import com.qualcomm.qti.qmmi.framework.BaseService;
import com.qualcomm.qti.qmmi.model.HidlManager;
import com.qualcomm.qti.qmmi.model.AidlManager;
import com.qualcomm.qti.qmmi.utils.LogUtils;

public class LightService extends BaseService {
    private AidlManager aidlManager = null;
    private HidlManager hidlManager = null;
    private Supplier<ILights> mLightService = null;
    private ILights mVintfLights = null;
    private int mOriginalScreenBrightness = 0 ;

    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.logi("onStartCommand");

        mLightService =  new VintfHalCache();
        if (mLightService == null) {
            LogUtils.loge( "No Light AIDL Service here");
        } else {
            mVintfLights = mLightService.get();
        }

        aidlManager = AidlManager.getInstance();
        if (aidlManager == null) {
            LogUtils.loge("No aidl manager found");
            hidlManager = HidlManager.getInstance();
            if (hidlManager == null) {
                LogUtils.loge("No hidl manager found");
            }
        }

        mOriginalScreenBrightness = Settings.System.getInt(this.getApplicationContext().getContentResolver(),
            Settings.System.SCREEN_BRIGHTNESS, 255);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void register() { }

    @Override
    public int stop(TestCase testCase) {
        int type = Integer.valueOf(testCase.getParameter().get("type"));
        try {
            setLight(testCase, false);
        } catch (Throwable t) {
            //catch NoClassDefFoundError if AIDL & HIDL not exist
            LogUtils.logi( "NoClassDefFoundError occur!");
        }

        //disable charger before test light "rgb"
        if (type == LightType.NOTIFICATIONS) {
            if (aidlManager != null) {
                aidlManager.chargerEnable(true);
            } else if (hidlManager != null) {
                hidlManager.chargerEnable(true);
            }
        }
        return 0;
    }

    @Override
    public int run(TestCase testCase) {

        int type = Integer.valueOf(testCase.getParameter().get("type"));
        //disable charger before test light "rgb"
        if (type == LightType.NOTIFICATIONS) {
            if (aidlManager != null) {
                aidlManager.chargerEnable(false);
            } else if (hidlManager != null) {
                hidlManager.chargerEnable(false);
            }
        }

        setLight(testCase, true);
        if (mVintfLights != null) {
            updateView(testCase.getName(), this.getResources().getString(R.string.light_on));
        } else {
            updateView(testCase.getName(), this.getResources().getString(R.string.light_service_miss));
        }
        return 0;
    }

    private void setLight(TestCase testCase, boolean on) {

        int type = Integer.valueOf(testCase.getParameter().get("type"));
        String color = testCase.getParameter().get("color");
        try {
            LogUtils.logi("service run for type:" + type + " ; color:" + color + " ; on:" + on);

            if (type == LightType.BACKLIGHT) {
                int brightness = on? 255:mOriginalScreenBrightness;
                    LogUtils.loge("LIGHT: debug1: mOriginalScreenBrightness = " + mOriginalScreenBrightness);
                    Settings.System.putInt(this.getApplicationContext().getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, brightness);
                    LogUtils.loge("LIGHT: debug: 255 ok?");
                    return;
            }

            int colorValue = 0;
            if (type == LightType.BUTTONS) {   //Button light test
                colorValue = on? 0xFF020202:0x00000000;
            } else if (type == LightType.NOTIFICATIONS) {  // LED light test
                if ("red".equalsIgnoreCase(color)){
                    colorValue = on? 0xFF0000:0x00000000;
                } else if ("blue".equalsIgnoreCase(color)) {
                    colorValue = on? 0x0000FF:0x00000000;
                } else if ("green".equalsIgnoreCase(color)) {
                    colorValue = on? 0x00FF00:0x00000000;
                } else {
                    LogUtils.loge("LIGHT: Unknow LED color");
                }
            }

            if (mVintfLights != null) {
                HwLightState lightState = new HwLightState();
                lightState.color = colorValue;
                lightState.flashMode = (byte)FlashMode.NONE;
                lightState.flashOnMs = 0;
                lightState.flashOffMs = 0;
                lightState.brightnessMode = (byte) BrightnessMode.USER;
                mVintfLights.setLightState(type, lightState);
            }
        } catch (Exception e) {
            LogUtils.loge("Exception in light service" + e.toString());
            e.printStackTrace();
        } catch (Throwable t) {
            //catch NoClassDefFoundError if AIDL & HIDL not exist
            LogUtils.loge("NoClassDefFoundError occur!");
        }
    }

    private static class VintfHalCache implements Supplier<ILights>, IBinder.DeathRecipient {
        private ILights mInstance = null;

        @Override
        public synchronized ILights get() {
            if (mInstance == null) {
                IBinder binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService(
                        "android.hardware.light.ILights/default"));
                if (binder != null) {
                    mInstance = ILights.Stub.asInterface(binder);
                    try {
                        binder.linkToDeath(this, 0);
                    } catch (RemoteException e) {
                        LogUtils.loge("Unable to register DeathRecipient for " + mInstance);
                    }
                }
            }
            return mInstance;
        }

        @Override
        public synchronized void binderDied() {
            mInstance = null;
        }
    }
}
