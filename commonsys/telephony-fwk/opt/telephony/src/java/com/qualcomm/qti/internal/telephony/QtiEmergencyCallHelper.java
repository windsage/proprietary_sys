/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.internal.telephony;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.EcbmHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.satellite.SatelliteController;
import static com.android.internal.telephony.flags.Flags.carrierEnabledSatelliteFlag;

import com.qti.extphone.ExtTelephonyManager;
import static com.qti.extphone.ExtTelephonyManager.FEATURE_EMERGENCY_ENHANCEMENT;

import java.util.ArrayList;
import java.util.List;
/**
 * This class has logic to return the primary stack phone id or the phone id
 * most suitable for emergency call
 */
public class QtiEmergencyCallHelper {
    private static final String LOG_TAG = "QtiEmergencyCallHelper";
    private static QtiEmergencyCallHelper sInstance = null;
    private static final int INVALID = -1;
    private static final int PRIMARY_STACK_MODEMID = 0;
    private static final int PROVISIONED = 1;
    private static final String ALLOW_ECALL_ENHANCEMENT_PROPERTY =
        "persist.vendor.radio.enhance_ecall";

    /**
      * Pick the best possible phoneId for emergency call
      * This algorithm is currently designed for Dual Sim use cases.
      * Following are the conditions applicable when deciding the sub/phone for dial.
      * 1. In Dual Standby/Dual Active mode, consider the sub(ie Phone), which is in
      *    OFFHOOK state.
      * 2. If any sub is in Emergency Callback Mode, that sub will be chosen.
      * 3. If a private sim is inserted, then consider alternative logic, otherwise
      *    continue through the algorithm.
      * 4. If any sub is registered on satellite network with voice disallowed,
      *    choose the other sub if it is valid.
      * 5. If any one sub returns false for both EMC and EMF values when we query them,
      *    choose the other sub if it is valid. If both/neither subs have EMC and EMF
      *    values false, then continue to the next step of the algorithm.
      * 6. If CIWLAN is registered on non-DDS sub or IWLAN is registered on non-DDS sub
      *    and CIWLAN is next available RAT, the DDS sub will be chosen.
      * 7. If both subs are not activated(i.e NO SIM/PIN/PUK lock state) then choose
      *    the sub mapped to primary stack.
      *
      * These next steps apply when any subs are In Service/Limited Service/READY state.
      * If neither sub is activated, choose the sub mapped to primary stack.
      *
      * 8. If a sub is in SCBM state, that sub will be chosen to place the Ecall.
      * 9. If one sub is voice centric and another sub is data centric, choose the
      *    voice centric sub.
      * 10. If one sub is camped on home PLMN and another sub is roaming, choose
      *     the sub camped on home network.
      * 11. If one sub is camped on PS RATs and another sub is camped on CS RATs,
      *     choose the sub camped on PS RATs.
      * 12. If the previous conditions have not chosen a sub yet, place call on default
      *     voice sub. If there is no default voice sub, place call on default data sub.
      */
    public static int getPhoneIdForECall(Context context) {

        int phoneId = INVALID;

        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int phoneCount = tm.getPhoneCount();

        if (!isDeviceInSingleStandby(context)) {
            // In dual active mode, the below algorithm will take effect
            // The SUB with the ACTIVE call receives the highest priority
            // The SUB with a HELD call receives second priority
            // If both SUBs have a HELD call, then the first SUB is selected
            if (tm.isDsdaOrDsdsTransitionMode()) {
                for (Phone phone: PhoneFactory.getPhones()) {
                    if (phone.getState() == PhoneConstants.State.OFFHOOK) {
                        Phone imsPhone = phone.getImsPhone();
                        if (imsPhone == null) {
                            Log.w(LOG_TAG, "ImsPhone should not be null");
                            continue;
                        }
                        if (imsPhone.getForegroundCall().getState() == Call.State.ACTIVE) {
                            return phone.getPhoneId();
                        }
                        if (imsPhone.getBackgroundCall().getState() == Call.State.HOLDING &&
                                phoneId == INVALID) {
                            phoneId = phone.getPhoneId();
                        }
                    }
                }
                if (phoneId != INVALID) {
                    return phoneId;
                }
            }

            // If there is active call, place on same SUB when concurrent calls are not supported
            // or if ImsPhone is null
            for (Phone phone: PhoneFactory.getPhones()) {
                if (phone.getState() == PhoneConstants.State.OFFHOOK) {
                    Log.d(LOG_TAG, "Call already active on phoneId: " + phone.getPhoneId());
                    // Since already call is active on phone, send ecall also on
                    // same phone to avoid delay in emergency call setup by modem
                    return phone.getPhoneId();
                }
            }
        }

        // When one sub is in ECBM, the other sub will be suspended so we should
        // select the sub that is already in ECBM.
        for (int phId = 0; phId < phoneCount; phId++) {
            EcbmHandler ecbmHandler = EcbmHandler.getInstance();
            if (ecbmHandler.isInEcm(phId)) {
                Log.d(LOG_TAG, "Sub is ECBM: " + phId);
                return phId;
            }
        }

        for (int phId = 0; phId < phoneCount; phId++) {
            Phone phone = PhoneFactory.getPhone(phId);
            // When there is a private network SIM inserted, the below algorithm takes effect.
            // Prioritize the SUB with a public network SIM inserted
            // If the public network SIM is INVALID or there is no public network SIM
            // The first SUB with a private network SIM is selected
            CarrierConfigManager configManager = (CarrierConfigManager) context
                    .getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configManager != null && isPrivateNetwork(configManager, phone)) {
                int privateId = INVALID;
                for (int otherId = 0; otherId < phoneCount; otherId++) {
                    if (otherId != phId) {
                        Phone otherPhone = PhoneFactory.getPhone(otherId);
                        if (!isPrivateNetwork(configManager, otherPhone) &&
                                isSimProvisioned(otherId, context, tm)) {
                            Log.d(LOG_TAG, "public network sim is ready/PROVISIONED");
                            return otherId;
                        } else if (privateId == INVALID) {
                            privateId = phId;
                        }
                    }
                }

                if (privateId != INVALID) {
                    return privateId;
                }
            }
            // If the device is camped on satellite network with voice disallowed, then that means
            // the device was not able to camp on a cellular network so we should select the other
            // sub if it is valid. If the other sub is invalid, then we can choose the sub camped
            // on satellite network.
            // satelliteId is used to track the current satellite sub, so that we can select
            // the satellite sub if the other sub is not valid.
            if (isCallDisallowedDueToSatellite(phone)) {
                Log.i(LOG_TAG, "Voice not allowed over satellite: " + phId);
                int satelliteId = INVALID;
                for (int otherId = 0; otherId < phoneCount; otherId++) {
                    if (otherId != phId) {
                        if (isSimProvisioned(otherId, context, tm)) {
                            Log.i(LOG_TAG, "Other SIM is valid in Satellite check: " + otherId);
                            // Select the first provisioned sub we find
                            return otherId;
                        } else if (satelliteId == INVALID) {
                            satelliteId = phId;
                        }
                    }
                }
                if (satelliteId != INVALID) {
                    return satelliteId;
                }
            }
        }

        ExtTelephonyManager extTelephonyManager = ExtTelephonyManager.getInstance(context);
        if (extTelephonyManager.isFeatureSupported(FEATURE_EMERGENCY_ENHANCEMENT)) {
            for (int phId = 0; phId < phoneCount; phId++) {
                // If a particular sub does not support emergency calls and emergency call fallback
                // we should select the other sub given that it is valid and allows either
                // emergency calls or emergency calls in fallback mode.
                // emcemfId is used to track the current sub returning EMC/EMF false, so that we
                // can select this sub if the other sub is not valid.
                if (!extTelephonyManager.isEmcSupported(phId)
                        && !extTelephonyManager.isEmfSupported(phId)) {
                    Log.i(LOG_TAG, "EMC and EMF are both false:" + phId);
                    int emcemfId = INVALID;
                    for (int otherId = 0; otherId < phoneCount; otherId++) {
                        if (otherId != phId) {
                            if (isSimProvisioned(otherId, context, tm)) {
                                phoneId = (phoneId == INVALID) ? otherId : INVALID;
                            } else if (emcemfId == INVALID) {
                                emcemfId = phId;
                            }
                        }
                    }
                    if (emcemfId != INVALID) {
                        return emcemfId;
                    }
                }
            }
        }
        if (phoneId != INVALID) {
            return phoneId;
        }

        boolean hasUserSetDefaultVoiceSub = false;
        boolean hasUserSetDefaultDataSub = false;
        final int ddsPhoneId = QtiPhoneUtils.getInstance().getPhoneId(context,
                SubscriptionManager.getDefaultDataSubscriptionId());
        int voiceCentricPhoneId = INVALID;
        for (int phId = 0; phId < phoneCount; phId++) {
            Phone phone = PhoneFactory.getPhone(phId);
            // If CIWLAN is registered on non-DDS sub or IWLAN is registered on non-DDS
            // sub and CIWLAN is the next available RAT, select the DDS sub.
            int imsRegistration = phone.getImsRegistrationTech();
            if (phId != ddsPhoneId && (imsRegistration ==
                     ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM ||
                     (imsRegistration ==
                     ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN &&
                     extTelephonyManager.isCiwlanAvailable(phId)))) {
                return ddsPhoneId;
            }
            int ss = phone.getServiceState().getState();
            if (ss == ServiceState.STATE_IN_SERVICE) {
                //If sub is in service and scbm is enabled on the phone, outgoing emergency
                //call goes over that phone.
                if (isInScbm(phId)) {
                    Log.i(LOG_TAG, "In Sms Callback Mode on phoneId: " + phId);
                    return phId;
                }
                // This phoneId is set for a sub in service.
                phoneId = phId;
                // If a sub is in service, the next priority is checking if it is the only voice
                // centric sub available since voice centric subs are better equipped to support
                // calling versus data centric subs.
                SubscriptionManager subscriptionManager =
                        context.getSystemService(SubscriptionManager.class);
                if (subscriptionManager != null) {
                    int subId = subscriptionManager.getSubscriptionId(phId);
                    SubscriptionInfo subInfo =
                            subscriptionManager.getActiveSubscriptionInfo(subId);
                    if (subInfo != null && subInfo.getUsageSetting() ==
                            SubscriptionManager.USAGE_SETTING_VOICE_CENTRIC) {
                        Log.i(LOG_TAG, "Sub is Voice Centric: " + phId);
                        // If this is the first voice centric sub found, set the
                        // voiceCentricPhoneId. Otherwise, if voiceCentricPhoneId is already set
                        // and we have found another voice centric sub, reset the
                        // voiceCentricPhoneId.
                        voiceCentricPhoneId = (voiceCentricPhoneId == INVALID) ? phId : INVALID;
                    }
                }
            }
        }
        // If we have found one voice centric sub, return it.
        if (voiceCentricPhoneId != INVALID) {
            return voiceCentricPhoneId;
        }

        List<Integer> homePsList = new ArrayList<Integer>();
        List<Integer> homeList = new ArrayList<Integer>();
        List<Integer> roamPsList = new ArrayList<Integer>();
        List<Integer> roamList = new ArrayList<Integer>();
        int voicePhoneId = QtiPhoneUtils.getInstance().getPhoneId(context,
                SubscriptionManager.getDefaultVoiceSubscriptionId());
        for (int phId = 0; phId < phoneCount; phId++) {
            Phone phone = PhoneFactory.getPhone(phId);
            int ss = phone.getServiceState().getState();
            if (ss == ServiceState.STATE_IN_SERVICE) {
                // If a sub is in service and both/neither subs were found to be voice centric
                // the next priority is finding a home PLMN since home networks are more stable
                // than roaming networks.
                ServiceState sst = phone.getServiceStateTracker().getServiceState();
                if (!sst.getRoaming()) {
                    Log.i(LOG_TAG, "Sub is Home: " + phId);
                    // If we have found a sub in service on home PLMN, we should check if it is
                    // camped on a PS RAT and if so, select this sub for emergency call. Otherwise,
                    // set the homePhoneId and if both subs are on CS RATs, select the first sub
                    // found on home PLMN.
                    if (isPSRat(phone, sst)) {
                        homePsList.add(phId);
                    } else {
                        homeList.add(phId);
                    }
                } else {
                    if (isPSRat(phone, sst)) {
                        roamPsList.add(phId);
                    } else {
                        roamList.add(phId);
                    }
                }
            }
        }
        List<List<Integer>> phoneLists = new ArrayList<>();
        phoneLists.addAll(List.of(homePsList, homeList, roamPsList, roamList));
        phoneId = getBestPhoneInService(phoneLists, voicePhoneId, ddsPhoneId);
        if (phoneId != INVALID) {
            return phoneId;
        }

        if (phoneId == INVALID) {
            for (int phId = 0; phId < phoneCount; phId++) {
                Phone phone = PhoneFactory.getPhone(phId);
                int ss = phone.getServiceState().getState();
                if (phone.getServiceState().isEmergencyOnly()) {
                    //If sub is emergency only and scbm is enabled on the phone, outgoing emergency
                    //call goes over that phone.
                    if (isInScbm(phId)) {
                        Log.i(LOG_TAG, "In Sms Callback Mode on phoneId: " + phId);
                        return phId;
                    }
                    phoneId = phId;
                    if (phoneId == voicePhoneId) hasUserSetDefaultVoiceSub = true;
                }
            }
            // If no emergency only sub is in scbm, only then choose the default voice sub.
            // If no default voice sub, it will choose the last emergency only phone.
            if (hasUserSetDefaultVoiceSub) {
                phoneId = voicePhoneId;
            }
            Log.d(LOG_TAG, "Voice phoneId in Limited service = "+ phoneId);
        }

        if (phoneId == INVALID) {
            phoneId = getPrimaryStackPhoneId(context);
            for (int phId = 0; phId < phoneCount; phId++) {
                Phone phone = PhoneFactory.getPhone(phId);
                if (isSimProvisioned(phId, context, tm)) {
                    //If scbm is enabled on the phone, outgoing emergency
                    //call goes over that phone.
                    if (isInScbm(phId)) {
                        Log.i(LOG_TAG, "In Sms Callback Mode on phoneId: " + phId);
                        return phId;
                    }
                    phoneId = phId;
                    if (phoneId == voicePhoneId) hasUserSetDefaultVoiceSub = true;
                }
            }
            // If no READY sub is in scbm, only then choose the default outgoing phone account.
            // If no default voice sub, it will choose the last READY SIM.
            if (hasUserSetDefaultVoiceSub) {
                phoneId = voicePhoneId;
            }
        }
        Log.d(LOG_TAG, "Voice phoneId in service = "+ phoneId +
                " preferred phoneId = " + voicePhoneId +
                " hasUserSetDefaultVoiceSub = " + hasUserSetDefaultVoiceSub +
                " hasUserSetDefaultDataSub = " + hasUserSetDefaultDataSub);

        return phoneId;
    }

    //checks if the Phone is in SCBM
    private static boolean isInScbm(int phoneId) {
        return ScbmHandler.getInstance().isInScbm(phoneId);
    }

    public static int getPrimaryStackPhoneId(Context context) {
        String modemUuId = null;
        Phone phone = null;
        int primayStackPhoneId = INVALID;
        int phoneCount = ((TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE)).getPhoneCount();

        for (int i = 0; i < phoneCount; i++) {

            phone = PhoneFactory.getPhone(i);
            if (phone == null) continue;

            Log.d(LOG_TAG, "Logical Modem id: " + phone.getModemUuId() + " phoneId: " + i);
            modemUuId = phone.getModemUuId();
            if ((modemUuId == null) || (modemUuId.length() <= 0) ||
                    modemUuId.isEmpty()) {
                continue;
            }
            // Select the phone id based on modemUuid
            // if modemUuid is 0 for any phone instance, primary stack is mapped
            // to it so return the phone id as the primary stack phone id.
            if (Integer.parseInt(modemUuId) == PRIMARY_STACK_MODEMID) {
                primayStackPhoneId = i;
                Log.d(LOG_TAG, "Primay Stack phone id: " + primayStackPhoneId + " selected");
                break;
            }
        }

        // never return INVALID
        if( primayStackPhoneId == INVALID){
            Log.d(LOG_TAG, "Returning default phone id");
            primayStackPhoneId = 0;
        }

        return primayStackPhoneId;
    }

    public static boolean isDeviceInSingleStandby(Context context) {
        if (!SystemProperties.getBoolean(ALLOW_ECALL_ENHANCEMENT_PROPERTY, true)) {
            Log.d(LOG_TAG, "persist.vendor.radio.enhance_ecall not enabled" );
            return false;
        }

        TelephonyManager tm =
               (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int phoneCnt = tm.getPhoneCount();

        // If phone count is 1, then it is single sim device.
        // return true
        if (phoneCnt == 1)
            return true;

        for (int phoneId = 0; phoneId < phoneCnt; phoneId++) {
            if (!isSimProvisioned(phoneId, context, tm)) {
                Log.d(LOG_TAG, "modem is in single standby mode" );
                return true;
            }
        }

        Log.d(LOG_TAG, "modem is in dual standby mode" );
        return false;
    }

    // Helper function to check carrier config values for specific subId for private network
    private static boolean isPrivateNetwork(CarrierConfigManager configManager, Phone phone) {
        PersistableBundle b = configManager.getConfigForSubId(phone.getSubId());
        return b != null && b.getBoolean("is_private_network");
    }

    // Helper function to check if SIM is valid/provisioned
    private static boolean isSimProvisioned(int phoneId, Context context, TelephonyManager tm) {
        return tm.getSimState(phoneId) == TelephonyManager.SIM_STATE_READY &&
            (QtiPhoneUtils.getInstance().getCurrentUiccCardProvisioningStatus(phoneId)
            == PROVISIONED);
    }

    // Helper function to check if voice is disallowed by satellite
    private static boolean isCallDisallowedDueToSatellite(Phone phone) {
        if (!carrierEnabledSatelliteFlag() || (phone == null)) {
            Log.d(LOG_TAG, "carrierEnabledSatelliteFlag is false or phone is null");
            return false;
        }

        SatelliteController satelliteController = SatelliteController.getInstance();
        if (!satelliteController.isInSatelliteModeForCarrierRoaming(phone)) {
            // Device is not connected to satellite
            Log.d(LOG_TAG, "device not connected to satellite");
            return false;
        }

        List<Integer> capabilities =
                satelliteController.getCapabilitiesForCarrierRoamingSatelliteMode(phone);
        if (capabilities.contains(NetworkRegistrationInfo.SERVICE_TYPE_VOICE)) {
            // Call is supported while using satellite
            Log.i(LOG_TAG, "Call is supported while using satellite");
            return false;
        }

        // Call is disallowed while using satellite
        Log.i(LOG_TAG, "call is disallowed due to satellite");
        return true;
    }

    // Helper function to check if device is camped on PS RAT
    private static boolean isPSRat(Phone phone, ServiceState sst) {
        int radioTechnology = sst.getRilVoiceRadioTechnology();
        return sst.isPsTech(radioTechnology);
    }

    // Helper function to find best phone in service
    // @param phoneLists may include homePsList, homeList, roamPsList, roamList
    // The priority for in service lists is as follows:
    // Home+PS > Home+CS > Roam+PS > Roam+CS
    // The priority for the sub within a list is as follows:
    // default voice > default data > any other
    private static int getBestPhoneInService(List<List<Integer>> phoneLists,
            int voicePhoneId, int ddsPhoneId) {
        for (List<Integer> phoneList : phoneLists) {
            Log.d(LOG_TAG, "phoneList :" + phoneList);
            if (phoneList.contains(voicePhoneId)) {
                Log.d(LOG_TAG, "return default voice :" + voicePhoneId);
                return voicePhoneId;
            } else if (phoneList.contains(ddsPhoneId)) {
                Log.d(LOG_TAG, "return default data :" + ddsPhoneId);
                return ddsPhoneId;
            } else if (!phoneList.isEmpty()) {
                return phoneList.get(0);
            }
        }
        return INVALID;
    }
}
