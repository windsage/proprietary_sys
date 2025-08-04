/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony;

import android.annotation.NonNull;
import android.content.Context;
import android.net.NetworkAgentConfig;
import android.net.NetworkProvider;
import android.net.NetworkScore;
import android.os.Looper;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.AccessNetworkConstants.TransportType;
import android.util.Log;
import android.util.SparseArray;

import com.android.ims.ImsManager;
import com.android.internal.telephony.CarrierInfoManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.WspTypeDecoder;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.cdma.EriManager;
import com.android.internal.telephony.data.DataConfigManager;
import com.android.internal.telephony.data.DataNetwork;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.DataProfileManager;
import com.android.internal.telephony.data.DataProfileManager.DataProfileManagerCallback;
import com.android.internal.telephony.data.DataRetryManager;
import com.android.internal.telephony.data.DataRetryManager.DataRetryManagerCallback;
import com.android.internal.telephony.data.DataServiceManager;
import com.android.internal.telephony.data.DataSettingsManager;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.data.TelephonyNetworkAgent;
import com.android.internal.telephony.data.TelephonyNetworkAgent.TelephonyNetworkAgentCallback;
import com.android.internal.telephony.data.TelephonyNetworkProvider;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.qualcomm.qti.internal.nrNetworkService.MainServiceImpl;
import com.qualcomm.qti.internal.telephony.data.QtiDataConfigManager;
import com.qualcomm.qti.internal.telephony.data.QtiDataNetworkController;
import com.qualcomm.qti.internal.telephony.data.QtiDataProfileManager;
import com.qualcomm.qti.internal.telephony.data.QtiDataRetryManager;
import com.qualcomm.qti.internal.telephony.data.QtiDataServiceManager;
import com.qualcomm.qti.internal.telephony.data.QtiDataSettingsManager;
import com.qualcomm.qti.internal.telephony.data.QtiPhoneSwitcher;
import com.qualcomm.qti.internal.telephony.data.QtiTelephonyNetworkAgent;
import com.qualcomm.qti.internal.telephony.data.QtiTelephonyNetworkProvider;
import com.qualcomm.qti.internal.telephony.QtiCarrierInfoManager;

import java.lang.RuntimeException;

public class QtiTelephonyComponentFactory extends TelephonyComponentFactory {
    private static String LOG_TAG = "QtiTelephonyComponentFactory";

    private static QtiTelephonyComponentFactory sInstance;
    private QtiRIL mRil[] = new QtiRIL[]{null, null};

    public QtiTelephonyComponentFactory() {
        sInstance = this;
    }

    public static QtiTelephonyComponentFactory getInstance() {
        return sInstance;
    }

    @Override
    public GsmCdmaCallTracker makeGsmCdmaCallTracker(GsmCdmaPhone phone,
            @NonNull FeatureFlags featureFlags) {
        Rlog.d(LOG_TAG, "makeGsmCdmaCallTracker");
        return super.makeGsmCdmaCallTracker(phone, featureFlags);
    }

    @Override
    public SmsStorageMonitor makeSmsStorageMonitor(Phone phone,
            @NonNull FeatureFlags featureFlags) {
        Rlog.d(LOG_TAG, "makeSmsStorageMonitor");
        return super.makeSmsStorageMonitor(phone, featureFlags);
    }

    @Override
    public SmsUsageMonitor makeSmsUsageMonitor(Context context, FeatureFlags flags) {
        Rlog.d(LOG_TAG, "makeSmsUsageMonitor");
        return super.makeSmsUsageMonitor(context, flags);
    }

    @Override
    public ServiceStateTracker makeServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci,
            FeatureFlags featureFlags) {
        Rlog.d(LOG_TAG, "makeQtiServiceStateTracker");
        return new QtiServiceStateTracker(phone, ci, featureFlags);
    }

    @Override
    public IccPhoneBookInterfaceManager makeIccPhoneBookInterfaceManager(Phone phone) {
        if (phone != null && phone.getHalVersion().greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            Rlog.d(LOG_TAG, "makeIccPhoneBookInterfaceManager");
            return super.makeIccPhoneBookInterfaceManager(phone);
        } else {
            Rlog.d(LOG_TAG, "makeQtiIccPhoneBookInterfaceManager");
            return new QtiIccPhoneBookInterfaceManager(phone);
        }
    }

    @Override
    public SmsDispatchersController makeSmsDispatchersController(Phone phone,
            FeatureFlags featureFlags) {
        Rlog.d(LOG_TAG, "makeQtiSmsDispatchersController");
        return new QtiSmsDispatchersController(phone, phone.mSmsStorageMonitor,
                phone.mSmsUsageMonitor, featureFlags);
    }

    @Override
    public EriManager makeEriManager(Phone phone, int eriFileSource) {
        Rlog.d(LOG_TAG, "makeEriManager");
        return super.makeEriManager(phone, eriFileSource);
    }

    @Override
    public WspTypeDecoder makeWspTypeDecoder(byte[] pdu) {
        Rlog.d(LOG_TAG, "makeWspTypeDecoder");
        return super.makeWspTypeDecoder(pdu);
    }

    @Override
    public InboundSmsTracker makeInboundSmsTracker(Context context, byte[] pdu, long timestamp,
            int destPort, boolean is3gpp2, boolean is3gpp2WapPdu, String address,
            String displayAddr, String msgBody, boolean isClass0, int subId,
            @InboundSmsHandler.SmsSource int smsSource) {
        Rlog.d(LOG_TAG, "makeInboundSmsTracker");
        return super.makeInboundSmsTracker(context, pdu, timestamp, destPort, is3gpp2,
                is3gpp2WapPdu, address, displayAddr, msgBody, isClass0, subId, smsSource);
    }

    @Override
    public InboundSmsTracker makeInboundSmsTracker(Context context, byte[] pdu, long timestamp,
            int destPort, boolean is3gpp2, String address, String displayAddr, int referenceNumber,
            int sequenceNumber, int messageCount, boolean is3gpp2WapPdu, String msgBody,
            boolean isClass0, int subId, @InboundSmsHandler.SmsSource int smsSource) {
        Rlog.d(LOG_TAG, "makeInboundSmsTracker");
        return super.makeInboundSmsTracker(context, pdu, timestamp, destPort, is3gpp2,
                address, displayAddr, referenceNumber, sequenceNumber, messageCount,
                is3gpp2WapPdu, msgBody, isClass0, subId, smsSource);
    }

    @Override
    public ImsPhoneCallTracker makeImsPhoneCallTracker(ImsPhone imsPhone) {
        Rlog.d(LOG_TAG, "makeImsPhoneCallTracker");
        return super.makeImsPhoneCallTracker(imsPhone);
    }

    @Override
    public CdmaSubscriptionSourceManager
    getCdmaSubscriptionSourceManagerInstance(Context context, CommandsInterface ci, Handler h,
                                             int what, Object obj) {
        Rlog.d(LOG_TAG, "getCdmaSubscriptionSourceManagerInstance");
        return super.getCdmaSubscriptionSourceManagerInstance(context, ci, h, what, obj);
    }

    @Override
    public Phone makePhone(Context context, CommandsInterface ci, PhoneNotifier notifier,
            int phoneId, int precisePhoneType,
            TelephonyComponentFactory telephonyComponentFactory,
            @NonNull FeatureFlags featureFlags) {
        Rlog.d(LOG_TAG, "makePhone");
        return new QtiGsmCdmaPhone(context, ci, notifier, phoneId, precisePhoneType,
                telephonyComponentFactory, featureFlags);
    }

    @Override
    public SubscriptionManagerService makeSubscriptionManagerService(Context c, Looper looper,
            @NonNull FeatureFlags featureFlags) {
        Rlog.d(LOG_TAG, "initSubscriptionManagerService");
        return QtiSubscriptionManagerService.init(c, looper, featureFlags);
    }

    @Override
    public void makeExtTelephonyClasses(Context context,
            Phone[] phones, CommandsInterface[] commandsInterfaces) {
        Rlog.d(LOG_TAG, " makeExtTelephonyClasses ");

        // Settings application uses the "settings_network_and_internet_v2" config value
        // to control the "SIM Cards" menu and "Networks & Internel" application menu.

        // Currently this "settings_network_and_internet_v2" config value is set to TRUE @
        // FeatureFlagUtils due to that "SIM Cards" menu not displayed, modifying the config
        // value below to false to display the "SIM Cards" menu in settings app.
        String value = Settings.Global.getString(context.getContentResolver(),
                "settings_network_and_internet_v2");
        if ((value == null) || !value.equals("false")) {
            Settings.Global.putString(context.getContentResolver(),
                    "settings_network_and_internet_v2", "false");
        }

        QtiPhoneUtils.init(context);
        if (phones[0].getHalVersion().less(RIL.RADIO_HAL_VERSION_1_5)) {
            QtiUiccCardProvisioner.make(context, commandsInterfaces);
            QtiUiccCardProvisionerHelper.init(context);
        }
        ScbmHandler.init(context);
        try {
            /* Init 5G network service */
            MainServiceImpl.init(context);
            ExtTelephonyServiceImpl.init(context);
        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
            Rlog.e(LOG_TAG, "Error creating ExtTelephonyServiceImpl");
        }

        QtiCommandLineService.advertise(context);
    }

    @Override
    public PhoneSwitcher makePhoneSwitcher(int maxDataAttachModemCount, Context context,
            Looper looper, FeatureFlags featureFlags) {
        Rlog.d(LOG_TAG, "makeQtiPhoneSwitcher");
        return QtiPhoneSwitcher.make(maxDataAttachModemCount, context, looper, featureFlags);
    }

    @Override
    public RIL makeRIL(Context context, int preferredNetworkType,
            int cdmaSubscription, Integer instanceId, @NonNull FeatureFlags featureFlags) {
        Rlog.d(LOG_TAG, "makeQtiRIL");
        if (instanceId < mRil.length) {
            mRil[instanceId] = new QtiRIL(context, preferredNetworkType, cdmaSubscription,
                    instanceId, featureFlags);
        } else {
            throw new RuntimeException("RilInstance = " + instanceId + " not allowed!");
        }

        return mRil[instanceId];
    }

    @Override
    public CarrierInfoManager makeCarrierInfoManager(Phone phone) {
        Rlog.d(LOG_TAG, "makeCarrierInfoManager");
        return new QtiCarrierInfoManager(phone);
    }

    @Override
    public DataNetworkController makeDataNetworkController(Phone phone, Looper looper,
            @NonNull FeatureFlags featureFlags) {
        return new QtiDataNetworkController(phone, looper, featureFlags);
    }

    public DataServiceManager makeDataServiceManager(Phone phone, Looper looper,
            @TransportType int transportType) {
        Rlog.d(LOG_TAG, "makeDataServiceManager");
        return new QtiDataServiceManager(phone, looper, transportType);
    }

    @Override
    public DataConfigManager makeDataConfigManager(Phone phone, Looper looper,
            FeatureFlags featureFlags) {
        return new QtiDataConfigManager(phone, looper, featureFlags);
    }

    @Override
    public DataProfileManager makeDataProfileManager(Phone phone,
            DataNetworkController dataNetworkController,
            DataServiceManager dataServiceManager, Looper looper,
            @NonNull FeatureFlags featureFlags,
            DataProfileManagerCallback callback) {
        return new QtiDataProfileManager(phone, dataNetworkController, dataServiceManager,
                looper, featureFlags, callback);
    }

    @Override
    public DataRetryManager makeDataRetryManager(Phone phone,
            DataNetworkController dataNetworkController,
            SparseArray<DataServiceManager> dataServiceManagers,
            Looper looper, FeatureFlags featureFlags,
            DataRetryManagerCallback dataRetryManagerCallback) {
        return new QtiDataRetryManager(phone, dataNetworkController, dataServiceManagers,
                looper, featureFlags, dataRetryManagerCallback);
    }

    @Override
    public TelephonyNetworkAgent makeTelephonyNetworkAgent(Phone phone, Looper looper,
            DataNetwork dataNetwork, NetworkScore score, NetworkAgentConfig config,
            NetworkProvider provider, TelephonyNetworkAgentCallback callback) {
        return new QtiTelephonyNetworkAgent(phone, looper, dataNetwork, score, config,
                provider, callback);
    }

    @Override
    public @NonNull DataSettingsManager makeDataSettingsManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull FeatureFlags featureFlags, @NonNull Looper looper,
            @NonNull DataSettingsManager.DataSettingsManagerCallback callback) {
        return new QtiDataSettingsManager(phone, dataNetworkController, featureFlags, looper,
                callback);
    }

    @Override
    public TelephonyNetworkProvider makeTelephonyNetworkProvider(@NonNull Looper looper,
            @NonNull Context context, @NonNull FeatureFlags flags) {
        Rlog.d(LOG_TAG, "make QtiTelephonyNetworkProvider");
        return new QtiTelephonyNetworkProvider(looper, context, flags);
    }

    public QtiRIL getRil(int slotId) {
        if (slotId < mRil.length) {
            return mRil[slotId];
        } else {
            return null;
        }
    }
}
