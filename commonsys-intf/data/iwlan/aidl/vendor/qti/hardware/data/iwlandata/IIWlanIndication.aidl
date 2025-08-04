/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.iwlandata;

import vendor.qti.hardware.data.iwlandata.QualifiedNetworks;
import android.hardware.radio.RadioIndicationType;
import android.hardware.radio.data.SetupDataCallResult;
import android.hardware.radio.data.DataProfileInfo;

/**
 * Interface declaring response functions for data service.
 */
@VintfStability
interface IIWlanIndication {

    /**
     * Indicates data call contexts have changed.
     *
     * @param type Type of radio indication
     * @param dcList Array of SetupDataCallResult identical to that returned by
     *        IRadioData.getDataCallList(). It is the complete list of current data contexts
     *        including new contexts that have been activated. A data call is only removed from
     *        this list when any of the below conditions is matched:
     *        - The framework sends a IRadioData.deactivateDataCall().
     *        - The radio is powered off/on.
     *        - Unsolicited disconnect from either modem or network side.
     */
    oneway void dataCallListChanged(in RadioIndicationType type, in SetupDataCallResult[] dcList);

    /**
     * Indicates that the data registration state parameters have changed.
     *
     */
    oneway void dataRegistrationStateChangeIndication();

    /**
     * Indicates that modem does not supports iWlan feature
     */
    oneway void modemSupportNotPresent();

    /**
     * Indicates that there is a change in the qualified networks
     * @param qualifiedNetworksList array of QualifiedNetworks. Identical to
     *        that returned by IRadio.getAllQualifiedNetworks
     */
    oneway void qualifiedNetworksChangeIndication(in QualifiedNetworks[] qualifiedNetworksList);

    /**
     * The modem can explicitly set SetupDataCallResult::suggestedRetryTime after a failure in
     * IRadioData.SetupDataCall. During that time, no new calls are allowed to
     * IRadioData.SetupDataCall that use the same APN. When IRadioDataIndication.unthrottleApn
     * is sent, AOSP will no longer throttle calls to IRadioData.SetupDataCall for the given APN.
     *
     * @param type Type of radio indication
     * @param dataProfileInfo Data profile info.
     */
    oneway void unthrottleApn(in RadioIndicationType type, in DataProfileInfo dataProfileInfo);
}
