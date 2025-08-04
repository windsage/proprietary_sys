/*
 * Copyright (c) 2017, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.qmmi.model;

import android.os.RemoteException;
import android.os.ServiceManager;

import com.qualcomm.qti.qmmi.utils.LogUtils;

import vendor.qti.hardware.factory.FactoryResult;
import vendor.qti.hardware.factory.IFactory;

public final class AidlManager {

    private volatile static AidlManager mAidlManager = null;
    public static final String MM_AUDIO_FTM_BIN = "mm-audio-ftm";
    public static final String FTM_DAEMON_BIN = "ftmdaemon";
    private IFactory iFactory = null;

    private AidlManager() {
        LogUtils.logi("aidl mgr init");
        final String aidlServiceName = IFactory.class.getCanonicalName() + "/default";
        try {
            if (ServiceManager.isDeclared(aidlServiceName)) {
                LogUtils.logi("IFactory AIDL service was declared");

                iFactory = IFactory.Stub.asInterface(ServiceManager.waitForService("vendor.qti.hardware.factory.IFactory/default"));
                if (iFactory == null) {
                    throw new IllegalStateException("IFactory AIDL service was declared but NOT found");
                } else {
                    LogUtils.logi("IFactory AIDL service was declared and found");
                }
            } else {
                iFactory = null;
                throw new IllegalStateException("IFactory AIDL service was NOT declared");
            }
        } catch (Exception e) {
            iFactory = null;
            throw new IllegalStateException("IFactory AIDL service declared or waitForService exception : " + e.toString());
        }
    }

    public static AidlManager getInstance() {
        if (mAidlManager == null) {
            synchronized (AidlManager.class) {
                if (mAidlManager == null) {
                    try {
                        mAidlManager = new AidlManager();
                    } catch (IllegalStateException e) {
                        LogUtils.loge("IFactory AIDL  getInstance exception : " + e.toString());
                        return null;
                    }
                }
            }
        }
        return mAidlManager;
    }

    public FactoryResult runApp(String name, String params, boolean isStart) {
        LogUtils.logi("start run: " + name + ", params: " + params + ",istart:" + isStart);
        if (iFactory != null) {
            FactoryResult result = null;
            try {
                result = iFactory.runApp(name, params, isStart);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        } else {
            LogUtils.loge("the factory aidl not exist");
            return null;
        }
    }

    public FactoryResult runAudioApp(String params) {
        FactoryResult r = runApp(MM_AUDIO_FTM_BIN, params, true);

        //stop after finished
        //runApp(MM_AUDIO_FTM_BIN, "", false);
        return r;
    }

    public FactoryResult stopAudioApp() {
        FactoryResult r = runApp(MM_AUDIO_FTM_BIN, "", false);
        return r;
    }

    public FactoryResult runFtmDaemonApp(String params) {
        FactoryResult r = runApp(FTM_DAEMON_BIN, params, true);
        return r;
    }

    public FactoryResult stopFtmDaemonApp() {
        FactoryResult r = runApp(FTM_DAEMON_BIN, "", false);
        return r;
    }

    public boolean chargerEnable(boolean enable) {
        if (iFactory != null) {
            boolean result = false;
            try {
                result = iFactory.chargerEnable(enable);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        } else {
            LogUtils.loge("the factory aidl not exist");
            return false;
        }
    }

    public boolean wifiEnable(boolean enable) {
        if (iFactory != null) {
            boolean result = false;
            try {
                result = iFactory.wifiEnable(enable);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        } else {
            LogUtils.loge("the factory aidl not exist");
            return false;
        }
    }

    public boolean enterShipMode() {
        if (iFactory != null) {
            boolean result = false;
            try {
                result = iFactory.enterShipMode();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        } else {
            LogUtils.loge("the factory aidl not exist");
            return false;
        }
    }

    public FactoryResult getSmbStatus() {
        if (iFactory != null) {
            FactoryResult result = null;
            try {
                result = iFactory.getSmbStatus();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        } else {
            LogUtils.loge("the factory aidl not exist");
            return null;
        }
    }

    public FactoryResult delegate(String cmd, String value) {
        LogUtils.logi("delegate: cmd: " + cmd + ", value: " + value);
        if (iFactory != null) {
            FactoryResult result = null;
            try {
                result = iFactory.delegate(cmd, value);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        } else {
            LogUtils.loge("the factory aidl not exist");
            return null;
        }
    }

}
