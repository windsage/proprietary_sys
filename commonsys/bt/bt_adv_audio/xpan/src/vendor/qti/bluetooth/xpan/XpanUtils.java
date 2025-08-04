/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.annotation.RequiresPermission;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.annotation.RequiresPermission;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
//import android.bluetooth.BluetoothXpan;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.MacAddress;
import android.os.Binder;
import android.os.Build;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

public class XpanUtils {
    public static final String LOG_TAG = "XpanUtil";
    private static final String LOG_TAG_SM = "XPANSM";
    public static final String TAG = "XpanUtils";
    public static final boolean DBG = true; // Log.isLoggable(LOG_TAG, Log.DEBUG);
    public static final boolean VDBG = true; // Log.isLoggable(LOG_TAG, Log.VERBOSE);
    static boolean DBGSM = false;
    private int mLPTimeout = 10;
    private ComponentName mServiceComponent = null;
    /*
     * set below to avoid country code related issue during locally
     *  HS creation cmd wifi force-country-code enabled US
     */

    private BluetoothAdapter mBtAdapter;
    private BluetoothManager btManager;

    /*  TWT Configuration */

    /**
     * Use below command to pull and push twt config file
     *
     * adb pull /data/data/vendor.qti.bluetooth.xpan/shared_prefs/config_twt.xml
     * adb push config_twt.xml data/data/vendor.qti.bluetooth.xpan/shared_prefs/
     */

    private List<TwtWakeParams> mListTwt = new ArrayList<TwtWakeParams>();

    private static final String KEY_GAMING_SI = "gaming_si"; // service or wake interval
    private static final String KEY_GAMING_SP = "gaming_sp"; // service period or wake duration
    private static final String KEY_GAMING_RIGHT_OFFSET = "gaming_right_offset";
    private static final String KEY_LOSSLESS_SI = "lossless_si";
    private static final String KEY_LOSSLESS_SP = "lossless_sp";
    private static final String KEY_LOSSLESS_RIGHT_OFFSET = "lossless_right_offset";

    private static final int GAMING_SI = 4000; // 4 micro seconds
    private static final int GAMING_SP = 2000; // 2 micro seconds
    private static final int GAMING_RIGHT_OFFSET = 0;
    private static final int LOSSLESS_SI = 131072; //131.072  ms
    private static final int LOSSLESS_SP = 32500; //32.5 ms
    private static final int LOSSLESS_RIGHT_OFFSET =29696; //29.696 ms

    private boolean mIsDebug = false;

    private static final String XPAN_TWT_FILE = "config_twt";
    private static final String NAME_UUID = "uuid";

    private static final String XPAN_DEVICES_FILE = "xpan_devices";
    private static final String PREF_CODEC = "codec";

    private Context mCtx;
    private static XpanUtils sInstance;
    private static final Object INSTANCE_LOCK = new Object();

    private ArrayList<BluetoothDevice> mListDevice = new ArrayList<BluetoothDevice>();

    private XpanUtils(Context ctx) {
        mCtx = ctx;
        if (btManager == null) {
            btManager = mCtx.getSystemService(BluetoothManager.class);
        }
        if (btManager != null && mBtAdapter == null) {
            mBtAdapter = btManager.getAdapter();
        }
        if (btManager == null || mBtAdapter == null) {
            Log.w(TAG, "mBtManager " + btManager + ", mBtAdapter " + mBtAdapter);
        }
        DBGSM = Log.isLoggable(LOG_TAG_SM, Log.VERBOSE);
        mIsDebug = Build.isDebuggable();
        mServiceComponent = new ComponentName(mCtx.getApplicationContext().getPackageName(),
                XpanProfileService.class.getCanonicalName());
        if (VDBG)
            Log.v(TAG, TAG);
        if (mIsDebug) {
            SharedPreferences pref = getSharedPreferences(TEST_CONFIG);
            if (pref != null) {
                Map<String, ?> keys = pref.getAll();
                if (keys.isEmpty()) {
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean(KEY_FIXED_FREQ, false);
                    editor.putInt(KEY_FIXED_CHANNEL, 0);
                    editor.putBoolean(KEY_LP_ENABLE, XpanConstants.DEFAULT_LP_ENABLE);
                    editor.putInt(KEY_LOWPOWER_INTERVAL, XpanConstants.DEFAULT_LP_INTERVAL);
                    editor.putBoolean(KEY_LOHS_OPEN, false);
                    editor.putString(KEY_BAND, WifiUtils.BAND_5GHZ);
                    editor.apply();
                }
                mFixedFreq = pref.getBoolean(KEY_FIXED_FREQ, false);
                mFixedChannel = pref.getInt(KEY_FIXED_CHANNEL, 0);
                mLpEnable = pref.getBoolean(KEY_LP_ENABLE, XpanConstants.DEFAULT_LP_ENABLE);
                mLPTimeout = pref.getInt(KEY_LOWPOWER_INTERVAL, XpanConstants.DEFAULT_LP_INTERVAL);
                mLohsOpen = pref.getBoolean(KEY_LOHS_OPEN, false);
                mBand = pref.getString(KEY_BAND, WifiUtils.BAND_5GHZ);
                if (mLPTimeout > 6000 || mLPTimeout < 10) {
                    mLPTimeout = 600;
                }
                if (DBG)
                    Log.d(TAG,
                            " mLPTimeout " + mLPTimeout + " mFixedFreq " + mFixedFreq
                                    + " mFixedChannel " + mFixedChannel + " mLpEnable "
                                    + mLpEnable + " DBGSM " + DBGSM);
            }
        }
    }

    void close() {
        if (DBG)
            Log.d(TAG, "close");
        sInstance = null;
        btManager = null;
        mBtAdapter = null;
        mListDevice.clear();
    }

   /**
     * Get singleton instance.
     */
    public static XpanUtils getInstance(Context ctx) {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new XpanUtils(ctx);
            }
            return sInstance;
        }
    }

    BluetoothAdapter getBluetoothAdapter() {
        if (mBtAdapter == null) {
            if (DBG)
                Log.w(TAG, "getBluetoothAdapter null");
        }
        return mBtAdapter;
    }

    int getLowPowerInterval() {
        return mLPTimeout;
    }

    BluetoothDevice getIdentityAddress(BluetoothDevice device) {
        BluetoothDevice identityDevice;
        String addr = device.getAddress();
        String identityAddr = device.getIdentityAddress();
        if (TextUtils.isEmpty(identityAddr)) {
            Log.w(TAG, "getIdentityAddress not valid " + identityAddr);
            identityDevice = device;
        } else {
            identityDevice = mBtAdapter.getRemoteDevice(identityAddr);
            if (!addr.equalsIgnoreCase(identityAddr)) {
                // Do we need to update this address to XM ?
            }
        }
        if (DBG)
            Log.d(TAG, "getIdentityAddress " + device + "  " + identityDevice);
        return identityDevice;
    }

    /**
     * Returns the anonymized hardware address. The first three octets will be
     * suppressed for anonymization.
     * <p>
     * For example, "XX:XX:XX:AA:BB:CC".
     *
     * @return Anonymized bluetooth hardware address as string
     */
    String getAddress(BluetoothDevice device) {
        if (Build.isDebuggable()) {
            return device.getAddress();
        } else {
            return "XX:XX:XX" + device.getAddress().substring(8);
        }
    }

    List<BluetoothDevice> loadXpanDevices() {
        List<BluetoothDevice> xpanDevices = new ArrayList<BluetoothDevice>();
        List<BluetoothDevice> previouseXpanDevices = getXpanDevices();
        if (previouseXpanDevices.isEmpty()) {
            return xpanDevices;
        }
        List<BluetoothDevice> bondedDevices = mBtAdapter
                .getMostRecentlyConnectedDevices();
        if (DBG)
            Log.d(TAG, "loadXpanDevices bondedDevices " + bondedDevices);
        if (bondedDevices == null || bondedDevices.isEmpty()) {
            return xpanDevices;
        }
        for (BluetoothDevice device : previouseXpanDevices) {
            if (bondedDevices.contains(device)) {
                xpanDevices.add(device);
            }
        }
        if (DBG)
            Log.d(TAG, "loadXpanDevices " + xpanDevices);
        return xpanDevices;
    }

    void cacheDevice(BluetoothDevice device, int state) {
        SharedPreferences pref = getSharedPreferences(XPAN_DEVICES_FILE);
        if (pref == null) {
            return;
        }
        String addr = device.getAddress();
        SharedPreferences.Editor editor = pref.edit();
        switch (state) {
        case XpanConstants.QLL_SUPPORT:
            editor.putInt(addr, state);
            break;
        case XpanConstants.QLL_NOT_SUPPORT:
            if (pref.contains(addr)) {
                editor.remove(addr);
            }
            break;
        }

        editor.apply();
        if (DBG)
            Log.d(TAG, "cacheDevice " + addr + " " + state);
    }

    private List<BluetoothDevice> getXpanDevices() {
        SharedPreferences pref = getSharedPreferences(XPAN_DEVICES_FILE);
        List<BluetoothDevice> devicesList = new ArrayList<BluetoothDevice>();
        if (pref == null) {
            return devicesList;
        }
        Map<String, ?> keys = pref.getAll();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            String address = entry.getKey();
            if (VDBG) Log.v(TAG, entry.getKey() + ": " + entry.getValue().toString());
            BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
            devicesList.add(device);
        }
        if (DBG)
            Log.d(TAG, "getXpanDevices " + devicesList);
        return devicesList;
    }

    void cacheDeviceCodec(BluetoothDevice device, int type) {
        SharedPreferences pref = getSharedPreferences(PREF_CODEC);
        if (pref == null) {
            return;
        }
        String addr = device.getAddress();
        SharedPreferences.Editor editor = pref.edit();
        if (type == -1) {
            editor.remove(addr);
        } else {
            editor.putInt(addr, type);
        }
        editor.apply();
        if (VDBG)
            Log.v(TAG, "cacheDeviceCodec " + addr + " " + type);
    }

    boolean isCodecSetPending(BluetoothDevice device) {
        SharedPreferences pref = getSharedPreferences(PREF_CODEC);
        if (pref == null) {
            return false;
        }

        boolean isCodecPending = pref.contains(device.getAddress());
        if (VDBG)
            Log.v(TAG, "isCodecSetPending " + isCodecPending);
        return isCodecPending;
    }

    private void setTwt() {
        SharedPreferences pref = getSharedPreferences(XPAN_TWT_FILE);
        Map<String, ?> keys = pref.getAll();

        if (DBG)
            Log.d(TAG, "setTwt  keys " + keys);

       if (!keys.isEmpty() || !mIsDebug) {
           if (VDBG)
               Log.d(TAG, "pref is not empty or not debug build ");
           return;
        }
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(KEY_GAMING_SI, GAMING_SI);
        editor.putInt(KEY_GAMING_SP, GAMING_SP);
        editor.putInt(KEY_GAMING_RIGHT_OFFSET, GAMING_RIGHT_OFFSET);
        editor.putInt(KEY_LOSSLESS_SI, LOSSLESS_SI);
        editor.putInt(KEY_LOSSLESS_SP, LOSSLESS_SP);
        editor.putInt(KEY_LOSSLESS_RIGHT_OFFSET, LOSSLESS_RIGHT_OFFSET);
        editor.apply();
    }

    private void getTwt() {
        if (mListTwt.size() != 0)
            return;
        int gamingsi, gamingsp, gamigoffset, losslesssi, losslesssp, losslessoffset;
        if (mIsDebug) {
            SharedPreferences pref = getSharedPreferences(XPAN_TWT_FILE);
            gamingsi = pref.getInt(KEY_GAMING_SI, GAMING_SI);
            gamingsp = pref.getInt(KEY_GAMING_SP, GAMING_SP);
            gamigoffset = pref.getInt(KEY_GAMING_RIGHT_OFFSET, GAMING_RIGHT_OFFSET);
            losslesssi = pref.getInt(KEY_LOSSLESS_SI, LOSSLESS_SI);
            losslesssp = pref.getInt(KEY_LOSSLESS_SP, LOSSLESS_SP);
            losslessoffset = pref.getInt(KEY_LOSSLESS_RIGHT_OFFSET, LOSSLESS_RIGHT_OFFSET);
        } else {
            gamingsi = GAMING_SI;
            gamingsp = GAMING_SP;
            gamigoffset = GAMING_RIGHT_OFFSET;
            losslesssi = LOSSLESS_SI;
            losslesssp = LOSSLESS_SP;
            losslessoffset = LOSSLESS_RIGHT_OFFSET;
        }

        TwtWakeParams twtparamsGaming = new TwtWakeParams();
        twtparamsGaming.setUsecase(XpanConstants.USECASE_GAMING);
        twtparamsGaming.setSi(gamingsi);
        twtparamsGaming.setSp(gamingsp);
        twtparamsGaming.setRightoffset(gamigoffset);

        TwtWakeParams twtparamslossless = new TwtWakeParams();
        twtparamslossless.setUsecase(XpanConstants.USECASE_LOSSLESS_MUSIC);
        twtparamslossless.setSi(losslesssi);
        twtparamslossless.setSp(losslesssp);
        twtparamslossless.setRightoffset(losslessoffset);
        mListTwt.add(twtparamslossless);
        mListTwt.add(twtparamsGaming);
    }

    TwtWakeParams getWakeDurations(int usecase) {
        if (mListTwt.size() == 0) {
            setTwt();
            getTwt();
        }
        TwtWakeParams params = null;
        if (usecase != XpanConstants.USECASE_LOSSLESS_MUSIC
                && usecase != XpanConstants.USECASE_GAMING) {
            Log.w(TAG, "getWakeDurations " + usecase + " to  "
                    + XpanConstants.USECASE_LOSSLESS_MUSIC);
            usecase = XpanConstants.USECASE_LOSSLESS_MUSIC;
        }
        for (TwtWakeParams twtWakeParams : mListTwt) {
            if (twtWakeParams.getUsecase() == usecase) {
                params = twtWakeParams;
                break;
            }
        }
        return params;
    }

    TwtWakeParams getTwtParam(int usecase) {
        int typeFlags = XpanConstants.TWT_NEGOTIATION_TYPE_INDIVIDUAL
                | XpanConstants.TWT_FLOW_TYPE_UNANNOUNCED
                | XpanConstants.TWT_TRIGGER_TYPE_NONTRIGGERED;
        TwtWakeParams params = getWakeDurations(usecase);
        if (VDBG)
            Log.v(TAG,    "getTwtParam typeFlags " + typeFlags
                    + "sp " + params.getSp() + " si " + params.getSi() + " tsfPrimary 0 "
                    + " tsfSecondaryOffset " + params.getRightoffset() + " usecase " + usecase);
        return params;
    }

    int getRightOffSet(int usecase) {
        TwtWakeParams twtWakeParams = getWakeDurations(usecase);
        int rightOffset = 0;
        if (twtWakeParams == null) {
            Log.w(TAG, "getRightOffSet invalid");
            return rightOffset;
        }
        rightOffset = twtWakeParams.getRightoffset();
        if (VDBG)
            Log.v(TAG, "getRightOffSet " + rightOffset + " usecase " + usecase);
        return rightOffset;
    }

    void startService( Context ctx, Intent intent) {
        if (DBG)
            Log.v(TAG, "startService");
        setComponentAvailable(ctx, true);
        ctx.startService(intent);
    }

    void stopService(Context ctx) {
        if (DBG)
            Log.v(TAG, "stopService");
        setComponentAvailable(ctx, false);
        close();
    }

    private void setComponentAvailable(Context ctx, boolean enable) {
        if (DBG) {
            Log.d(TAG, "setComponentAvailable " + enable);
        }
        if (mServiceComponent == null) {
            Log.d(TAG ,"mServiceComponent null");
            return;
        }
        ctx.getPackageManager().setComponentEnabledSetting(mServiceComponent,
                enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                       : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP | PackageManager.SYNCHRONOUS);
    }


    int secToMillieSec(int sec) {
        return sec * 1000;
    }

    boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mCtx.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /************* Todo : Remove after test *******/

    private static boolean mFixedFreq = false;
    private static boolean mLpEnable = true;
    private static boolean mLohsOpen = false;
    private static String mBand = "";
    private static int mFixedChannel = 0;

    private final String TEST_CONFIG = "test_config";
    private final String KEY_LOWPOWER_INTERVAL = "lowpower_interval";
    private final String KEY_FIXED_FREQ = "fixed_freq";
    private final String KEY_FIXED_CHANNEL = "fixed_channel";
    private final String KEY_LP_ENABLE = "low_power_enable";
    private final String KEY_LOHS_OPEN = "lohs_open";
    private final String KEY_BAND = "band";

    boolean isFixedFreq() {
        return mFixedFreq;
    }

    boolean isLowpowerEnable() {
        return mLpEnable;
    }

    SharedPreferences getSharedPreferences(String name) {
        SharedPreferences pref = null;
        try {
            pref = mCtx.createDeviceProtectedStorageContext().getSharedPreferences(name,
                    Context.MODE_PRIVATE);
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }
        return pref;
    }

    boolean isLohsOpen() {
        return mLohsOpen;
    }

    String getBand() {
        return mBand;
    }

    int getFixedChannel() {
        return mFixedChannel;
    }

    void cacheMdnsUuid(BluetoothDevice device, String uuid) {
        SharedPreferences pref = getSharedPreferences(NAME_UUID);
        String address = device.getAddress();
        if (DBG)
            Log.d(TAG, "cacheMdnsUuid " + address + "   " + uuid);
        if (pref == null) {
            return;
        }
        Editor edit = pref.edit();
        if(pref.contains(address)) {
            String oldUuid = pref.getString(address,"");
            if (!oldUuid.equalsIgnoreCase(uuid)) {
                edit.remove(address);
                if (DBG)
                    Log.d(TAG, "cacheMdnsUuid prev " + oldUuid);
            }
        }
        edit.putString(address, uuid);
        edit.apply();
    }

    String getMdnsUuidAddress(UUID uuid) {
        if (uuid == null) {
            Log.w(TAG, "getMdnsUuidAddress uuid null");
            return "";
        }
        SharedPreferences pref = getSharedPreferences(NAME_UUID);
        if (pref == null) {
            return "";
        }
        String uuidString = uuid.toString();
        Map<String, ?> entries = pref.getAll();
        if (VDBG)
            Log.v(TAG, "getMdnsUuidAddress " + uuid + " Entries " + entries);
        String address = "";
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            String val = entry.getValue().toString();
            if (val.equalsIgnoreCase(uuidString)) {
                address = entry.getKey();
                break;
            }
        }
        if (DBG) {
            Log.d(TAG, "getMdnsUuidAddress " + uuid + " " + address);
        }
        return address;
    }

    String getMdnsUuid(BluetoothDevice device) {
        SharedPreferences pref = getSharedPreferences(NAME_UUID);
        if (pref == null) {
            return "";
        }
        String address = device.getAddress();
        String uuid =  pref.getString(address, "");
        if (VDBG)
            Log.v(TAG, "getMdnsUuid " + address + " uuid " + uuid);
        return uuid;
    }

    UUID getUuidLocal() {
        SharedPreferences pref = getSharedPreferences(NAME_UUID);
        if (pref!=null && !pref.contains(XpanConstants.KEY_UUID_LOCAL)) {
            Editor editor = pref.edit();
            editor.putString(XpanConstants.KEY_UUID_LOCAL, UUID.randomUUID().toString());
            editor.commit();
        }
        UUID uuidLocal = null;
        if (pref != null) {
            uuidLocal = UUID.fromString(pref.getString(XpanConstants.KEY_UUID_LOCAL, ""));
        } else {
            uuidLocal = UUID.fromString(UUID.randomUUID().toString());
        }
        if (VDBG)
            Log.v(TAG, "getLocalUuid " + uuidLocal);
        return uuidLocal;
    }

    int getPeriodicity(int usecase) {
        return (usecase == XpanConstants.USECASE_AUDIO_STREAMING
            || usecase == XpanConstants.USECASE_LOSSLESS_MUSIC)
                ? XpanConstants.PERIODICITY_LOSSLESS
                : XpanConstants.PERIODICITY_CALL;
    }

    int getSetId(BluetoothDevice device) {
        if (!mListDevice.contains(device)) {
            mListDevice.add(device);
        }
        return mListDevice.indexOf(device);
    }

    Message getMessage(int arg1) {
        Message msg = Message.obtain();
        msg.arg1 = arg1;
        return msg;
    }

    Message getMessage(int arg1, int arg2) {
        Message msg = Message.obtain();
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        return msg;
    }

    Message getMessageWithWhat(int what, int arg1) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = arg1;
        return msg;
    }
}
