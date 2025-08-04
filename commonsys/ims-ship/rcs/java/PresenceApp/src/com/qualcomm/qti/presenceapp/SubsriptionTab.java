/**
 * Copyright (c)2020-2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.PresenceApp;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.qualcomm.qti.PresenceApp.service.TaskListener;
import com.qualcomm.qti.PresenceApp.tasks.PublishTask;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import com.android.ims.ImsManager;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class SubsriptionTab extends Fragment {
    int mSub;
    String mIccid;
    int mTabInstance;
    String mNumber = "";
    ImsManager imsMgr;
    private long fragmentID;
    private SubsStatusAdapter mSubscriptionStatusAdapter;
    private ContactArrayAdapter<Contact> mAdapter = null;
    final String LOG_TAG = MainActivity.LOG_TAG + ":SubsriptionTab";
    final static String IMS_CALL_COMPOSER = "qti.settings.call_composer";
    static final int MOBILE_DATA =1;
    static final int VOLTE_VT = 2;
    static final int CALL_COMPOSER = 3;
    private boolean mVoLTEenabled = true;
    private boolean mVTenabled = true;
    private boolean mCallComposerEnabled = true;

    private static final int CAPABILITY_TYPE_NONE = 0;
    private static final int CAPABILITY_TYPE_VOICE = 1 << 0;
    private static final int CAPABILITY_TYPE_VIDEO = 1 << 1;
    private static final int CAPABILITY_TYPE_UT = 1 << 2;
    private static final int CAPABILITY_TYPE_SMS = 1 << 3;
    private static final int CAPABILITY_TYPE_CALL_COMPOSER = 1 << 4;

    private class ImsCapabilityStatusObserver extends IImsCapabilityCallback.Stub {
        @Override
        public void onQueryCapabilityConfiguration(int capability, int radioTech, boolean enabled) {
            Log.e(LOG_TAG, "onQueryCapabilityConfiguration received");
        }

        @Override
        public void onChangeCapabilityConfigurationError(int capability, int radioTech, int reason) {
            Log.e(LOG_TAG, "onChangeCapabilityConfigurationError");
        }

        @Override
        public void onCapabilitiesStatusChanged(int config) {
            Log.e(LOG_TAG, "onCapabilitiesStatusChanged for tabInstance: "+ mTabInstance);
            boolean isVolteEnabled = ((config & CAPABILITY_TYPE_VOICE) == CAPABILITY_TYPE_VOICE);
            boolean isVideoEnabled = ((config & CAPABILITY_TYPE_VIDEO) == CAPABILITY_TYPE_VIDEO);
            boolean isCallComposerEnabled = ((config & CAPABILITY_TYPE_CALL_COMPOSER) == CAPABILITY_TYPE_CALL_COMPOSER);

            Log.i(LOG_TAG, "volteenabled : "+isVolteEnabled+" , video enabled: "+isVideoEnabled+" , callcomposer enabled: "+isCallComposerEnabled);
            if(MainActivity.mCapabilities.get(mTabInstance) == null)
                return;

            MainActivity.mCapabilities.get(mTabInstance).setVolteStatus(isVolteEnabled);
            MainActivity.mCapabilities.get(mTabInstance).setVtStatus(isVideoEnabled);
            MainActivity.mCapabilities.get(mTabInstance).setCallComposerStatus(isCallComposerEnabled);

            boolean isCapChanged = (mVoLTEenabled != isVolteEnabled) ||
                                   (mVTenabled != isVideoEnabled) ||
                                   (mCallComposerEnabled != isCallComposerEnabled);

            if(isCapChanged) {
                Log.e(LOG_TAG," onCapabilities changed");
                PublishTask task = new PublishTask(MainActivity.mCtx, mTabInstance);
                task.execute();
            } else {
                Log.e(LOG_TAG, " onCapabilities not changed");
            }

            mVoLTEenabled = isVolteEnabled;
            mVTenabled = isVideoEnabled;
            mCallComposerEnabled = isCallComposerEnabled;

        }

    }

    ImsCapabilityStatusObserver imsCapStatus = new ImsCapabilityStatusObserver();

    public ContactArrayAdapter<Contact> getAdapter() {
        return mAdapter;
    }

    private void getSelfContact() {
        Context context = getContext();
        SubscriptionInfo subInfo = null;

        if(context == null) return;

        SubscriptionManager subMgr =  SubscriptionManager.from(context);
        if(subMgr != null) {
            subInfo = subMgr.getActiveSubscriptionInfoForSimSlotIndex(mTabInstance);
        }

        if(subInfo != null) {
            mNumber = subInfo.getNumber();
        }
    }

    private class PublishListener extends TaskListener {
        @Override
        public void onCommandStatus(String Status, int Id) {
            Log.d(LOG_TAG, "PublishTaskListener: onCommandStatus : " + Status + " for tabInstance : " + mTabInstance);
            SubsStatus subStatus = mSubscriptionStatusAdapter.getSubsStatus();
            subStatus.setData(SubsStatus.publishCmdStatus, Status);
            mSubscriptionStatusAdapter.updateSubsData(subStatus);
        }

        @Override
        public void onSipResponse(int Reasoncode, String reason) {
            Log.d(LOG_TAG, "PublishTaskListener: onSipResponse : " + Reasoncode);
            SubsStatus subStatus = mSubscriptionStatusAdapter.getSubsStatus();
            subStatus.setData(SubsStatus.publishResponse, (Integer.toString(Reasoncode) + " " + reason));
            mSubscriptionStatusAdapter.updateSubsData(subStatus);
        }

        @Override
        public void onPublishTrigger(String type) {
            Log.d(LOG_TAG, "PublishTaskListener: onPublishTrigger : " + type);
            SubsStatus subStatus = mSubscriptionStatusAdapter.getSubsStatus();
            subStatus.setData(SubsStatus.publishTrigger, type);
            mSubscriptionStatusAdapter.updateSubsData(subStatus);
        }

        @Override
        public void onRegistrationChange(String x) {
            Log.d(LOG_TAG, "PublishTaskListener: Registration Status: " + x);
            SubsStatus subStatus = mSubscriptionStatusAdapter.getSubsStatus();
            subStatus.setData(SubsStatus.imsRegistration, x);
            mSubscriptionStatusAdapter.updateSubsData(subStatus);
        }
    };
    private PublishListener mTaskListener = new PublishListener();
    public SubsriptionTab(int index, int Sub, String iccid) {
        // Required empty public constructor
        Log.d(LOG_TAG, "Sub[" + mSub+"], iccid[" + iccid +"], tabInstance[" + index +"]" );
        mSub = Sub;
        mIccid = iccid;
        mTabInstance = index;
        Thread t = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    if(imsMgr == null) {
                        imsMgr = ImsManager.getInstance(MainActivity.mCtx, mTabInstance);
                        imsMgr.addCapabilitiesCallbackForSubscription(imsCapStatus, mSub);
                    }
                } catch (Exception e) {
                    Log.d(LOG_TAG, "Exception caught: ");
                    e.printStackTrace();
                }
                if(mSubscriptionStatusAdapter == null) {
                  getSelfContact();
                  mSubscriptionStatusAdapter = new SubsStatusAdapter(mTabInstance);
                  mSubscriptionStatusAdapter.setContactUri(mNumber);
                }
            }
        };
        t.start();
    }

    private void getVtVoLTEStatus() {
        ImsMmTelManager mmTelManager = ImsMmTelManager.createForSubscriptionId(mSub);
        mVoLTEenabled = mmTelManager.isAdvancedCallingSettingEnabled();
        Log.e(LOG_TAG, "getVtVoLTEStatus:VoLTE enabled:"+ mVoLTEenabled);

        if(MainActivity.mCapabilities.get(mTabInstance) == null)
            return;

        MainActivity.mCapabilities.get(mTabInstance).enableDisableFeatures(mVoLTEenabled);
        // Now check for VT
        mVTenabled = mmTelManager.isVtSettingEnabled();
        Log.e(LOG_TAG, "getVtVoLTEStatus: VT enabled:"+ mVTenabled);
        MainActivity.mCapabilities.get(mTabInstance).setVtStatus(mVTenabled);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View vw = (View) inflater.inflate(R.layout.fragment_subsription_tab, container, false);
        getSelfContact();
        if(mSubscriptionStatusAdapter == null)
           mSubscriptionStatusAdapter = new SubsStatusAdapter(mTabInstance);
        mSubscriptionStatusAdapter.setContext(getContext());
        mSubscriptionStatusAdapter.setContactUri(mNumber);
        RecyclerView recyclerView = vw.findViewById(R.id.recycleView);
        recyclerView.setAdapter(mSubscriptionStatusAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ArrayList<Contact> contactsList = new ArrayList<Contact>(
                                        MainActivity.phoneBookContacts.values());

        if(mNumber != null && !(mNumber.equals("")) ) {
            Contact selfContact =  new Contact("Self", mNumber, 0,
                                                    "<Not Subscribed>","");
            contactsList.add(0,selfContact);

            if(!(MainActivity.contacts.containsKey(mNumber)))
                MainActivity.contacts.put(mNumber, selfContact);
        }

        if(mAdapter == null) {
            mAdapter = new ContactArrayAdapter(vw.getContext(),
                    R.layout.fragment_contact_list, R.id.ContactList,
                    contactsList);
        }
        ListView lv = (ListView)vw.findViewById(R.id.ContactList);
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
              @Override
              public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                  Log.d(LOG_TAG, "instance[" + mTabInstance+"]" );
                  Intent i = new Intent();
                  i.setClassName("com.qualcomm.qti.PresenceApp",
                          "com.qualcomm.qti.PresenceApp.ContactInfo");
                  TextView name = view.findViewById(R.id.name);
                  i.putExtra(ContactInfo.CONTACT_NAME, name.getText());
                  TextView phone = view.findViewById(R.id.phone);
                  i.putExtra(ContactInfo.CONTACT_PHONE, phone.getText());
                  //i.putExtra("Contact", MainActivity.contacts.get(position));
                  i.putExtra(ContactInfo.CONTACT_INDEX, position);
                  i.putExtra(ContactInfo.CONTACT_TAB_INSTANCE, mTabInstance);
                  startActivity(i);
              }
          });
        return vw;
    }
    public void bindToAidlService() {
        //Called from MainActivity
        getVtVoLTEStatus();
        MainActivity.mServiceWrapper.initialize(MainActivity.mCtx,  mTabInstance, mIccid,mTaskListener);
    }
    public void releaseAidlService() {
        //Called from MainActivity
        MainActivity.mServiceWrapper.release(mTabInstance);
    }
    public int getTabInstance() {
        return mTabInstance;
    }
    public void setId(long id) {fragmentID = id;}
    public long getFragmentId() {return fragmentID;}
}
