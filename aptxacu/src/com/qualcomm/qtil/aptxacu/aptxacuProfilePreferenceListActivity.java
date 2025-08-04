/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxacu;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class aptxacuProfilePreferenceListActivity extends AppCompatActivity {
  private static final String TAG = "aptxacuProfilePreferenceListActivity";
  private static final boolean DBG = false;

  private List<AppList> installedApps;
  private AppAdapter installedAppAdapter;
  ListView userInstalledApps;

  private static final String userPrefFileName = "user-profile-prefs.json";
  private String mACUUserPrefFilePath;

  private String[] mSpinnerLabels;
  private String[] mSpinnerValues;

  private Map<String, String> mProfilePreferenceListMap;

  private static final int EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST = 1;
  private final aptxacuProfilePreferenceListHandler mHandler =
      new aptxacuProfilePreferenceListHandler(this);

  private Context mContext = null;
  private aptxacuApplication mApp = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate");
    super.onCreate(savedInstanceState);

    // Set the system bar insets to be ignored by the window decor.
    setupSystemBarInsets();

    mContext = getApplicationContext();
    mApp = (aptxacuApplication) mContext;
    if (mApp == null) {
      Log.e(TAG, "unable to get app");
      return;
    }

    if (mHandler == null) {
      Log.e(TAG, "unable to create handler");
      return;
    }

    mProfilePreferenceListMap = new ConcurrentHashMap<String, String>();

    if (GetACUFilePath()) {
      Log.i(TAG, "Profile Preferences User File is: " + mACUUserPrefFilePath);
      // Load user app preference file
      LoadPrefFile(mACUUserPrefFilePath);
    }

    setContentView(R.layout.activity_app_list);

    // Get spinner labels
    mSpinnerLabels = getResources().getStringArray(R.array.spinner_labels);
    final ArrayList<String> spinnerLabels = new ArrayList<String>();
    for (int i = 0; i < mSpinnerLabels.length; ++i) {
      spinnerLabels.add(mSpinnerLabels[i]);
    }

    // Get spinner values
    mSpinnerValues = getResources().getStringArray(R.array.spinner_values);
    final ArrayList<String> spinnerValues = new ArrayList<String>();
    for (int i = 0; i < mSpinnerValues.length; ++i) {
      spinnerValues.add(mSpinnerValues[i]);
    }

    userInstalledApps = (ListView) findViewById(R.id.installed_app_list);
    installedApps = getInstalledApps();
    installedAppAdapter =
        new AppAdapter(
            aptxacuProfilePreferenceListActivity.this, installedApps, spinnerLabels, spinnerValues);
    userInstalledApps.setAdapter(installedAppAdapter);
  }

  public Map<String, String> GetPrefMap() {
    return mProfilePreferenceListMap;
  }

  public class AppAdapter extends BaseAdapter {

    public LayoutInflater layoutInflater;
    public List<AppList> listStorage;
    public ArrayList<String> mSpinnerLabels;
    public ArrayList<String> mSpinnerValues;

    public AppAdapter(
        Context context,
        List<AppList> customizedListView,
        ArrayList<String> spinnerLabels,
        ArrayList<String> spinnerValues) {
      layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      listStorage = customizedListView;
      mSpinnerLabels = spinnerLabels;
      mSpinnerValues = spinnerValues;
    }

    @Override
    public int getCount() {
      return listStorage.size();
    }

    @Override
    public Object getItem(int position) {
      return position;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    class ViewHolder {
      TextView textInListView;
      ImageView imageInListView;
      Spinner spinnerInListView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

      View view = null;
      if (convertView == null) {
        // There is no view at this position, we create a new one
        final ViewHolder listViewHolder = new ViewHolder();
        view = layoutInflater.inflate(R.layout.installed_app_list, parent, false);
        listViewHolder.textInListView = (TextView) view.findViewById(R.id.list_app_name);
        listViewHolder.imageInListView = (ImageView) view.findViewById(R.id.app_icon);
        listViewHolder.spinnerInListView = (Spinner) view.findViewById(R.id.spinner);

        // Attaching data adapter to spinner
        ArrayAdapter<String> adapter =
            new ArrayAdapter<String>(
                aptxacuProfilePreferenceListActivity.this,
                android.R.layout.simple_spinner_item,
                mSpinnerLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listViewHolder.spinnerInListView.setAdapter(adapter);
        listViewHolder.spinnerInListView.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
              @Override
              public void onItemSelected(
                  AdapterView<?> parent, View view, int spnPosition, long id) {
                // Update app profile preference in AppList
                String profilePref = mSpinnerValues.get(spnPosition);
                AppList app = (AppList) listViewHolder.spinnerInListView.getTag();
                app.setProfilePref(profilePref);
                if (DBG)
                  Log.d(
                      TAG,
                      "getView onItemSelected getName(): "
                          + app.getName()
                          + " profilePref: "
                          + profilePref
                          + " spnPosition: "
                          + spnPosition
                          + " position: "
                          + position);
              }

              @Override
              public void onNothingSelected(AdapterView<?> parent) {
                // Auto-generated method stub
                if (DBG) Log.d(TAG, "getView onNothingSelected");
              }
            });

        view.setTag(listViewHolder);
        listViewHolder.spinnerInListView.setTag(listStorage.get(position));
      } else {
        // Recycle a View that already exists
        if (DBG) Log.d(TAG, "getView convertView != null position: " + position);
        view = convertView;
        ((ViewHolder) view.getTag()).spinnerInListView.setTag(listStorage.get(position));
      }

      // Once we have a reference to the View we are returning, we set its values.
      ViewHolder viewHolder = (ViewHolder) view.getTag();
      viewHolder.textInListView.setText(listStorage.get(position).getName());
      viewHolder.imageInListView.setImageDrawable(listStorage.get(position).getIcon());
      viewHolder.spinnerInListView.setSelection(
          mSpinnerValues.indexOf(listStorage.get(position).getProfilePref()));

      if (DBG)
        Log.d(
            TAG,
            "getView position: "
                + position
                + " getName: "
                + listStorage.get(position).getName()
                + " getProfilePref: "
                + listStorage.get(position).getProfilePref()
                + " getProfilePref mSpinnerValues idx: "
                + mSpinnerValues.indexOf(listStorage.get(position).getProfilePref()));

      return view;
    }
  }

  private List<AppList> getInstalledApps() {
    PackageManager pkgMgr = getPackageManager();
    List<AppList> apps = new ArrayList<AppList>();
    List<PackageInfo> installedPackages = pkgMgr.getInstalledPackages(0);

    for (int i = 0; i < installedPackages.size(); i++) {
      PackageInfo pkgInfo = installedPackages.get(i);
      if ((!isSystemPackage(pkgInfo))) {
        String appName = pkgInfo.applicationInfo.loadLabel(pkgMgr).toString();
        Drawable icon = pkgInfo.applicationInfo.loadIcon(pkgMgr);
        String packageName = pkgInfo.applicationInfo.packageName;

        if (mProfilePreferenceListMap.containsKey(packageName)) {
          String profilePref = mProfilePreferenceListMap.get(packageName);
          Log.i(
              TAG,
              "getInstalledApps CheckPrefMap packageName:"
                  + packageName
                  + " profilePref: "
                  + profilePref);
          apps.add(new AppList(appName, icon, packageName, profilePref));
        } else {
          Log.i(
              TAG,
              "getInstalledApps CheckPrefMap no packageName: "
                  + packageName
                  + " profilePref: "
                  + mSpinnerValues[0]);
          apps.add(new AppList(appName, icon, packageName, mSpinnerValues[0]));
        }
      }
    }

    // Sort AppName in ascending order
    Collections.sort(apps, AppList.AppNameComparator);

    return apps;
  }

  private boolean isSystemPackage(PackageInfo pkgInfo) {
    return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }

  private void LoadPrefFile(String prefFilePath) {
    BufferedReader jsonStreamReader = null;
    StringBuilder jsonStrBuilder = null;

    try {
      File jsonInputFile = new File(prefFilePath);
      if (DBG) Log.d(TAG, "LoadPrefFile prefFilePath: " + prefFilePath);
      if (jsonInputFile.exists()) {
        jsonStreamReader =
            new BufferedReader(new InputStreamReader(new FileInputStream(jsonInputFile), "UTF-8"));
        jsonStrBuilder = new StringBuilder();

        String jsonLine;
        while ((jsonLine = jsonStreamReader.readLine()) != null) {
          jsonStrBuilder.append(jsonLine);
        }

        JSONArray jsonData = new JSONArray(jsonStrBuilder.toString());
        long length = jsonData.length();
        for (int i = 0; i < length; ++i) {
          JSONObject jsonOb = jsonData.optJSONObject(i);
          if (jsonOb != null) {
            for (Iterator<String> keys = jsonOb.keys(); keys.hasNext(); ) {
              String key = keys.next();
              String value = jsonOb.getString(key);
              if (DBG) Log.d(TAG, "LoadPrefFile Profile Preference: " + key + "->" + value);
              mProfilePreferenceListMap.put(key, value);
            }
          }
        }
      } else {
        Log.w(TAG, "Profile Preference File " + prefFilePath + " does not exist: skipping");
      }
    } catch (Exception e) {
      Log.e(
          TAG,
          "Exception on loading Profile Preference File JSON data from "
              + prefFilePath
              + ": "
              + e.toString());
    } finally {
      try {
        if (jsonStreamReader != null) {
          jsonStreamReader.close();
        }
      } catch (IOException e) {
        Log.e(TAG, "IOException " + e.toString());
      }
    }
  }

  private void saveAppListToJson(List<AppList> installedApps) {
    JSONArray jsonArray = new JSONArray();
    JSONObject jsonObj = null;

    for (AppList applist : installedApps) {
      jsonObj = new JSONObject();
      try {
        jsonObj.put(applist.packageName, applist.profilePref);
      } catch (JSONException e) {
        Log.e(TAG, "saveAppListToJson error: " + e.toString());
        e.printStackTrace();
      }
      jsonArray.put(jsonObj);
    }

    // Save JSON data to SharedPreference
    SetAppAudioProfilePreferenceList(jsonArray);

    if (GetACUFilePath()) {
      try {
        FileWriter file = new FileWriter(mACUUserPrefFilePath, false);
        file.write(jsonArray.toString());
        file.close();
      } catch (Exception e) {
        Log.e(TAG, "saveAppListToJson error: " + e.toString());
        e.printStackTrace();
      }
    }
  }

  public void SetAppAudioProfilePreferenceList(JSONArray jsonArray) {
    String audioProfilePreferenceList = jsonArray.toString();
    Message msg = mHandler.obtainMessage(EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST);
    msg.obj = audioProfilePreferenceList;
    mHandler.sendMessage(msg);
  }

  private boolean GetACUFilePath() {
    try {
      String ACUFilesDirPath = this.getFilesDir().getAbsolutePath();
      mACUUserPrefFilePath = ACUFilesDirPath + "/" + userPrefFileName;
      if (DBG) Log.d(TAG, "ACU Profile Preferences User File is: " + mACUUserPrefFilePath);
    } catch (Exception e) {
      Log.e(TAG, "Exception on Profile Preference File resource " + e.toString());
      return false;
    }
    return true;
  }

  // AppList model
  public static class AppList {
    private String appName;
    Drawable icon;
    private String packageName;
    private String profilePref;

    public AppList(String appName, Drawable icon, String packageName, String profilePref) {
      if (DBG) Log.d(TAG, "AppList: " + appName);
      this.appName = appName;
      this.icon = icon;
      this.packageName = packageName;
      this.profilePref = profilePref;
    }

    public String getName() {
      return appName;
    }

    public Drawable getIcon() {
      return icon;
    }

    public String getPackages() {
      return packageName;
    }

    public String getProfilePref() {
      if (DBG) Log.d(TAG, "getProfilePref: [" + profilePref + "]");
      return profilePref;
    }

    public void setProfilePref(String profilePref) {
      if (DBG) Log.d(TAG, "setProfilePref: [" + profilePref + "]");
      this.profilePref = profilePref;
    }

    // Comparator to sort AppList by AppName alphabetically
    public static Comparator<AppList> AppNameComparator =
        new Comparator<AppList>() {
          public int compare(AppList a1, AppList a2) {
            String AppName1 = a1.getName().toUpperCase();
            String AppName2 = a2.getName().toUpperCase();

            // Sort in ascending order
            return AppName1.compareTo(AppName2);
          }
        };
  }

  protected void onStart() {
    super.onStart();
  }

  protected void onRestart() {
    super.onRestart();
  }

  protected void onResume() {
    super.onResume();
  }

  @Override
  public void onPause() {
    // Update JSON file
    saveAppListToJson(installedApps);
    super.onPause();
  }

  protected void onStop() {
    super.onStop();
  }

  protected void onDestroy() {
    super.onDestroy();
  }

  /**
   * Setup the system bar insets.
   *
   * <p>This method is called in onCreate() to set the system bar insets to be ignored by the window
   * decor.
   */
  private void setupSystemBarInsets() {
    // Tell the window decor to ignore system bars (e.g. status bar, navigation bar)
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

    // Set a listener to apply window insets to the content view
    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(android.R.id.content),
        (v, windowInsets) -> {
          // Get the system bar insets (e.g. status bar, navigation bar)
          Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
          // Apply the insets as padding to the content view
          v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

          // Consume the insets to prevent them from being applied to other views
          return WindowInsetsCompat.CONSUMED;
        });
  }

  private class aptxacuProfilePreferenceListHandler extends Handler {
    private aptxacuProfilePreferenceListActivity mProfilePreferenceListActivity = null;

    public aptxacuProfilePreferenceListHandler(
        aptxacuProfilePreferenceListActivity profilePreferenceListActivity) {
      mProfilePreferenceListActivity = profilePreferenceListActivity;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST:
          String audioProfilePreferenceList = (String) msg.obj;
          mApp.SetAppAudioProfilePreferenceList(audioProfilePreferenceList);
          break;

        default:
          break;
      }
    }
  }
}
