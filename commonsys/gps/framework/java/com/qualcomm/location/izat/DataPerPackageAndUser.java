/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2017, 2020 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package com.qualcomm.location.izat;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IInterface;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.utils.IZatServiceContext;
/*
 Purpose: Assist with multi-user scenarios, storing data & callbacks (CallbackData) classified
 per different client package and users.
 Notifies when active user is switched.
 */
public class DataPerPackageAndUser<GenericData extends CallbackData>
        implements IzatService.ISystemEventListener {
    private static final String TAG = "DataPerPackageAndUser";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private final Context mContext;
    private Map<Integer, String> mPackagenamePerPid;
    // Keys are packageName from getPackageName()
    private Map<String, GenericData> mCallbackDataPerPackageCurrentUser;
    private String mCurrentUser;
    // Keys are userName from getUserName()
    private Map<String, Map<String, GenericData>> mCallbackDataPerPackageAllUsers;
    private static final Object sDataLock = new Object();
    private UserChangeListener mUserChangeListener;
    private boolean mUseCommonPackage = false;
    private IZatServiceContext mIZatServiceCtx;

    public interface UserChangeListener<GenericData extends CallbackData> {
        void onUserChange(Map<String , GenericData> prevUserData,
                          Map<String , GenericData> currentUserData);
    }

    public DataPerPackageAndUser(Context ctx, UserChangeListener userChangeListener) {
        mContext = ctx;
        mUserChangeListener = userChangeListener;

        mPackagenamePerPid = new HashMap<Integer, String>();
        mCallbackDataPerPackageCurrentUser = new HashMap<String , GenericData>();
        mCallbackDataPerPackageAllUsers = new HashMap<String , Map<String, GenericData>>();
        mCurrentUser = getUserName(null);
        mCallbackDataPerPackageAllUsers.put(mCurrentUser, mCallbackDataPerPackageCurrentUser);
        mIZatServiceCtx = IZatServiceContext.getInstance(mContext);

        if (UserManager.supportsMultipleUsers()) {
            mIZatServiceCtx.registerSystemEventListener(MSG_USER_SWITCH_ACTION_UPDATE, this);
        }
    }

    @Override
    public void notify(int msgId, Object... args) {
        if (msgId == MSG_USER_SWITCH_ACTION_UPDATE) {
            Intent intent = (Intent)args[0];
            int userId = intent.getExtras().getInt( Intent.EXTRA_USER_HANDLE );
            String newUser = new UserHandle(userId).toString();

            if (VERBOSE) {
                Log.d(TAG, "User switched to " + newUser + " from " + mCurrentUser);
            }

            synchronized (sDataLock) {
                Map<String , GenericData> newUserData =
                        mCallbackDataPerPackageAllUsers.get(newUser);

                if (null == newUserData) {
                    newUserData = new HashMap<String , GenericData>();
                    mCallbackDataPerPackageAllUsers.put(newUser, newUserData);
                }

                mUserChangeListener.onUserChange(mCallbackDataPerPackageCurrentUser, newUserData);

                mCurrentUser = newUser;
                mCallbackDataPerPackageCurrentUser = newUserData;
            }
        }
    }

    public void useCommonPackage() {
        mUseCommonPackage = true;
    }

    public void setData(GenericData callbackData) {
        setData(callbackData, null);
    }

    public void setData(GenericData callbackData, final PendingIntent notifyIntent) {
        synchronized (sDataLock) {
            callbackData.mPackageName = getPackageName(notifyIntent);
            mCallbackDataPerPackageCurrentUser.put(callbackData.mPackageName, callbackData);
        }
    }

    public GenericData getData() {
        return getCallbackDataPerPackageCurrentUser().get(getPackageName(null));
    }

    public GenericData getDataForUid(int uid) {
        Map<String , GenericData> dataMap = getCallbackDataPerPackageForUser(uid);
        if (dataMap == null) {
            Log.e(TAG, "Failed to fetch user data map.");
            return null;
        }
        return dataMap.get(getPackageName(null));
    }

    public GenericData getDataByPkgName(String packageName) {
        return getCallbackDataPerPackageCurrentUser().get(packageName);
    }

    public GenericData getDataByCallback(IInterface callback) {
        GenericData clData = null;

        for (Map.Entry<String, GenericData> entry :
                mCallbackDataPerPackageCurrentUser.entrySet()) {
            if (entry.getValue().mCallback == callback) {
                clData = entry.getValue();
                break;
            }
        }

        return clData;
    }

    public void removeData(GenericData callbackData) {
        synchronized (sDataLock) {
            mCallbackDataPerPackageCurrentUser.remove(callbackData.mPackageName);
        }
    }

    public List<GenericData>  getAllData() {
        return new ArrayList<GenericData>(mCallbackDataPerPackageCurrentUser.values());
    }

    public List<GenericData> getAllDataPerPackageName(String packageName) {
        List<GenericData> callbackDataList = new ArrayList<GenericData>();

        for (Map<String, GenericData> callbackDataPerPackage:
                mCallbackDataPerPackageAllUsers.values()) {
            GenericData clData = callbackDataPerPackage.get(packageName);
            if (null != clData) {
                callbackDataList.add(clData);
            }
        }

        return callbackDataList;
    }

    public String getPackageName(final PendingIntent notifyIntent) {
        String callingPackage = "COMMON";

        if (!mUseCommonPackage) {
            if (null != notifyIntent) {
                callingPackage = notifyIntent.getCreatorPackage();
            } else {
                callingPackage = getPackageNameFromUid(Binder.getCallingUid());
            }
        }
        if (VERBOSE) {
            Log.d(TAG, "callingPackage:" + callingPackage + " user: " + getUserName(notifyIntent));
        }

        return callingPackage;
    }

    private String getPackageNameFromUid(int uid) {
        return mContext.getPackageManager().getNameForUid(uid);
    }

    private String getPackageNameFromPid(int pid) {
        String packageName = mPackagenamePerPid.get(pid);
        if (null == packageName) {
            ActivityManager am = (ActivityManager) mContext.getSystemService(
                    Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> infos = am.getRunningAppProcesses();
            if (infos != null && infos.size() > 0) {
                for(ActivityManager.RunningAppProcessInfo info : infos) {
                    if(info.pid == pid) {
                        // NOTE: Use the first package name found in that process.
                        mPackagenamePerPid.put(pid, info.pkgList[0]);
                        return info.pkgList[0];
                    }
                }
            }
        }
        return packageName;
    }

    private String getUserName(final PendingIntent notifyIntent) {
        UserHandle userHandle = null;
        if (null != notifyIntent) {
            userHandle = notifyIntent.getCreatorUserHandle();
        } else {
            userHandle = Binder.getCallingUserHandle();
        }

        return userHandle.toString();
    }

    // Returns the data for the current user for cases when UserSwitchReceiver has not been
    // triggered yet by the system.
    private Map<String , GenericData> getCallbackDataPerPackageCurrentUser() {
        String userName = getUserName(null);
        if (VERBOSE) {
            Log.d(TAG, "Getting data for user: " + userName);
        }

        return mCallbackDataPerPackageAllUsers.get(userName);
    }

    private Map<String , GenericData> getCallbackDataPerPackageForUser(int uid) {
        UserHandle userHandle = UserHandle.getUserHandleForUid(uid);
        if (userHandle == null) {
            Log.e(TAG, "Failed to get user handle for uid " + uid);
            return null;
        }

        String userName = userHandle.toString();
        if (VERBOSE) {
            Log.d(TAG, "Getting data for user: " + userName);
        }

        return mCallbackDataPerPackageAllUsers.get(userName);
    }
}
