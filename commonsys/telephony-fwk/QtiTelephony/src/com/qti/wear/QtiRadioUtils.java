/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.content.Context;
import android.os.SystemClock;
import android.telephony.AccessNetworkConstants;
import android.telephony.CellConfigLte;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.ClosedSubscriberGroupInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessSpecifier;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.EpsQos;
import com.qti.extphone.NrQos;
import com.qti.extphone.Qos;
import com.qti.extphone.QosBearerSession;
import com.qti.extphone.QosParametersResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.codeaurora.telephony.utils.EnhancedRadioCapabilityResponse;
import vendor.qti.hardware.data.dynamicdds.V1_0.ISubscriptionManager;

class QtiRadioUtils {
    private static final String LOG_TAG = "QtiRadioUtils";

    // NetworkScanRequest.SEARCH_TYPE_PLMN_ONLY
    private static final int SEARCH_TYPE_PLMN_ONLY = 1;

    // NetworkScanRequest.SEARCH_TYPE_PLMN_AND_CAG
    private static final int SEARCH_TYPE_PLMN_AND_CAG = 0;

    private static final int MAX_DATA_DEACTIVATE_DELAY_TIME = 7000;
    private static final String SMART_DDS_SWITCH_KEY = "smart_dds_switch";

    protected static int convertToQtiNetworkTypeBitMask(int raf) {
        int networkTypeRaf = 0;

        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.GSM) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_GSM;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.GPRS) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_GPRS;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EDGE) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EDGE;
        }
        // convert both IS95A/IS95B to CDMA as network mode doesn't support CDMA
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.IS95A) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_CDMA;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.IS95B) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_CDMA;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.ONE_X_RTT) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_1xRTT;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EVDO_0) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EVDO_0;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EVDO_A) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EVDO_A;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EVDO_B) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EVDO_B;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EHRPD) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EHRPD;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.HSUPA) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_HSUPA;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.HSDPA) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_HSDPA;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.HSPA) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_HSPA;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.HSPAP) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_HSPAP;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.UMTS) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_UMTS;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.TD_SCDMA) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_TD_SCDMA;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.LTE) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_LTE;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.LTE_CA) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_LTE_CA;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.NR_NSA) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_NR_NSA;
        }
        if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.NR_SA) != 0) {
            networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_NR_SA;
        }
        return (networkTypeRaf == 0) ? EnhancedRadioCapabilityResponse.
                NETWORK_TYPE_UNKNOWN : networkTypeRaf;
    }

    /**
     * Convert a list of CellInfo defined in CellInfo.aidl to a list of CellInfos
     * @param records List of CellInfo defined in CellInfo.aidl
     * @return The converted list of CellInfos
     */
    public static ArrayList<CellInfo> convertHalCellInfoList(
            vendor.qti.hardware.radio.qtiradio.QtiCellInfo[] records) {
        ArrayList<CellInfo> response = new ArrayList<>(records.length);
        if (records.length == 0) return response;
        final long nanotime = SystemClock.elapsedRealtimeNanos();
        for (vendor.qti.hardware.radio.qtiradio.QtiCellInfo ci : records) {
            response.add(convertHalCellInfo(ci, nanotime));
        }
        return response;
    }

    /**
     * Convert a CellInfo defined in CellInfo.aidl to CellInfo
     * @param cellInfo CellInfo defined in CellInfo.aidl
     * @param nanotime time the CellInfo was created
     * @return The converted CellInfo
     */
    private static CellInfo convertHalCellInfo(
            vendor.qti.hardware.radio.qtiradio.QtiCellInfo cellInfo, long nanotime) {
        if (cellInfo == null) return null;
        int connectionStatus = cellInfo.connectionStatus;
        boolean registered = cellInfo.registered;
        switch (cellInfo.ratSpecificInfo.getTag()) {
            case vendor.qti.hardware.radio.qtiradio.QtiCellInfoRatSpecificInfo.gsm:
                vendor.qti.hardware.radio.qtiradio.CellInfoGsm gsm =
                        cellInfo.ratSpecificInfo.getGsm();
                return new CellInfoGsm(connectionStatus, registered, nanotime,
                        convertHalCellIdentityGsm(gsm.cellIdentityGsm),
                        convertHalGsmSignalStrength(gsm.signalStrengthGsm));
            case vendor.qti.hardware.radio.qtiradio.QtiCellInfoRatSpecificInfo.cdma:
                vendor.qti.hardware.radio.qtiradio.CellInfoCdma cdma =
                        cellInfo.ratSpecificInfo.getCdma();
                return new CellInfoCdma(connectionStatus, registered, nanotime,
                        convertHalCellIdentityCdma(cdma.cellIdentityCdma),
                        convertHalCdmaSignalStrength(cdma.signalStrengthCdma,
                                cdma.signalStrengthEvdo));
            case vendor.qti.hardware.radio.qtiradio.QtiCellInfoRatSpecificInfo.lte:
                vendor.qti.hardware.radio.qtiradio.CellInfoLte lte =
                        cellInfo.ratSpecificInfo.getLte();
                return new CellInfoLte(connectionStatus, registered, nanotime,
                        convertHalCellIdentityLte(lte.cellIdentityLte),
                        convertHalLteSignalStrength(lte.signalStrengthLte), new CellConfigLte());
            case vendor.qti.hardware.radio.qtiradio.QtiCellInfoRatSpecificInfo.wcdma:
                vendor.qti.hardware.radio.qtiradio.CellInfoWcdma wcdma =
                        cellInfo.ratSpecificInfo.getWcdma();
                return new CellInfoWcdma(connectionStatus, registered, nanotime,
                        convertHalCellIdentityWcdma(wcdma.cellIdentityWcdma),
                        convertHalWcdmaSignalStrength(wcdma.signalStrengthWcdma));
            case vendor.qti.hardware.radio.qtiradio.QtiCellInfoRatSpecificInfo.tdscdma:
                vendor.qti.hardware.radio.qtiradio.CellInfoTdscdma tdscdma =
                        cellInfo.ratSpecificInfo.getTdscdma();
                return new CellInfoTdscdma(connectionStatus, registered, nanotime,
                        convertHalCellIdentityTdscdma(tdscdma.cellIdentityTdscdma),
                        convertHalTdscdmaSignalStrength(tdscdma.signalStrengthTdscdma));
            case vendor.qti.hardware.radio.qtiradio.QtiCellInfoRatSpecificInfo.nr:
                vendor.qti.hardware.radio.qtiradio.QtiCellInfoNr nr =
                        cellInfo.ratSpecificInfo.getNr();
                return new CellInfoNr(connectionStatus, registered, nanotime,
                        convertHalCellIdentityNr(nr.cellIdentityNr),
                        convertHalNrSignalStrength(nr.signalStrengthNr));
            default:
                return null;
        }
    }

    /**
     * Convert a CellIdentityGsm defined in CellIdentityGsm.aidl to CellIdentityGsm
     * @param cid CellIdentityGsm defined in CellIdentityGsm.aidl
     * @return The converted CellIdentityGsm
     */
    public static CellIdentityGsm convertHalCellIdentityGsm(
            vendor.qti.hardware.radio.qtiradio.CellIdentityGsm cid) {
        return new CellIdentityGsm(cid.lac, cid.cid, cid.arfcn,
                cid.bsic == (byte) 0xFF ? CellInfo.UNAVAILABLE : cid.bsic, cid.mcc, cid.mnc,
                cid.operatorNames.alphaLong, cid.operatorNames.alphaShort, new ArraySet<>());
    }

    /**
     * Convert a CellIdentityCdma defined in CellIdentityCdma.aidl to CellIdentityCdma
     * @param cid CellIdentityCdma defined in CelIdentityCdma.aidl
     * @return The converted CellIdentityCdma
     */
    public static CellIdentityCdma convertHalCellIdentityCdma(
            vendor.qti.hardware.radio.qtiradio.CellIdentityCdma cid) {
        return new CellIdentityCdma(cid.networkId, cid.systemId, cid.baseStationId, cid.longitude,
                cid.latitude, cid.operatorNames.alphaLong, cid.operatorNames.alphaShort);
    }

    /**
     * Convert a CellIdentityLte defined in CellIdentityLte.aidl to CellIdentityLte
     * @param cid CellIdentityLte defined in CellIdentityLte.aidl
     * @return The converted CellIdentityLte
     */
    public static CellIdentityLte convertHalCellIdentityLte(
            vendor.qti.hardware.radio.qtiradio.CellIdentityLte cid) {
        return new CellIdentityLte(cid.ci, cid.pci, cid.tac, cid.earfcn, cid.bands, cid.bandwidth,
                cid.mcc, cid.mnc, cid.operatorNames.alphaLong, cid.operatorNames.alphaShort,
                primitiveArrayToArrayList(cid.additionalPlmns),
                convertHalClosedSubscriberGroupInfo(cid.csgInfo));
    }

    /**
     * Convert a CellIdentityWcdma defined in CellIdentityWcdma.aidl to CellIdentityWcdma
     * @param cid CellIdentityWcdma defined in CellIdentityWcdma.aidl
     * @return The converted CellIdentityWcdma
     */
    public static CellIdentityWcdma convertHalCellIdentityWcdma(
            vendor.qti.hardware.radio.qtiradio.CellIdentityWcdma cid) {
        return new CellIdentityWcdma(cid.lac, cid.cid, cid.psc, cid.uarfcn, cid.mcc, cid.mnc,
                cid.operatorNames.alphaLong, cid.operatorNames.alphaShort,
                primitiveArrayToArrayList(cid.additionalPlmns),
                convertHalClosedSubscriberGroupInfo(cid.csgInfo));
    }

    /**
     * Convert a CellIdentityTdscdma defined in CellIdentityTdscdma.aidl to CellIdentityTdscdma
     * @param cid CellIdentityTdscdma defined in radio/1.0, 1.2, 1.5/types.hal
     * @return The converted CellIdentityTdscdma
     */
    public static CellIdentityTdscdma convertHalCellIdentityTdscdma(
            vendor.qti.hardware.radio.qtiradio.CellIdentityTdscdma cid) {
        return new CellIdentityTdscdma(cid.mcc, cid.mnc, cid.lac, cid.cid, cid.cpid, cid.uarfcn,
                cid.operatorNames.alphaLong, cid.operatorNames.alphaShort,
                primitiveArrayToArrayList(cid.additionalPlmns),
                convertHalClosedSubscriberGroupInfo(cid.csgInfo));
    }

    /** Convert a primitive byte array to an ArrayList<Integer>. */
    public static ArrayList<Byte> primitiveArrayToArrayList(byte[] arr) {
        ArrayList<Byte> arrayList = new ArrayList<>(arr.length);
        for (byte b : arr) {
            arrayList.add(b);
        }
        return arrayList;
    }

    /** Convert a primitive int array to an ArrayList<Integer>. */
    public static ArrayList<Integer> primitiveArrayToArrayList(int[] arr) {
        ArrayList<Integer> arrayList = new ArrayList<>(arr.length);
        for (int i : arr) {
            arrayList.add(i);
        }
        return arrayList;
    }

    /** Convert a primitive String array to an ArrayList<String>. */
    public static ArrayList<String> primitiveArrayToArrayList(String[] arr) {
        return new ArrayList<>(Arrays.asList(arr));
    }

    private static ClosedSubscriberGroupInfo convertHalClosedSubscriberGroupInfo(
            android.hardware.radio.V1_5.OptionalCsgInfo optionalCsgInfo) {
        android.hardware.radio.V1_5.ClosedSubscriberGroupInfo csgInfo =
                optionalCsgInfo.getDiscriminator()
                        == android.hardware.radio.V1_5.OptionalCsgInfo.hidl_discriminator.csgInfo
                        ? optionalCsgInfo.csgInfo() : null;
        if (csgInfo == null) return null;
        return new ClosedSubscriberGroupInfo(csgInfo.csgIndication, csgInfo.homeNodebName,
                csgInfo.csgIdentity);
    }

    private static ClosedSubscriberGroupInfo convertHalClosedSubscriberGroupInfo(
            vendor.qti.hardware.radio.qtiradio.ClosedSubscriberGroupInfo csgInfo) {
        if (csgInfo == null) return null;
        return new ClosedSubscriberGroupInfo(csgInfo.csgIndication, csgInfo.homeNodebName,
                csgInfo.csgIdentity);
    }


    /**
     * Convert a GsmSignalStrength defined in GsmSignalStrength.aidl to CellSignalStrengthGsm
     * @param ss GsmSignalStrength defined in GsmSignalStrength.aidl
     * @return The converted CellSignalStrengthGsm
     */
    public static CellSignalStrengthGsm convertHalGsmSignalStrength(
            vendor.qti.hardware.radio.qtiradio.GsmSignalStrength ss) {
        CellSignalStrengthGsm ret = new CellSignalStrengthGsm(
                CellSignalStrength.getRssiDbmFromAsu(ss.signalStrength), ss.bitErrorRate,
                ss.timingAdvance);
        if (ret.getRssi() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a CdmaSignalStrength and EvdoSignalStrength defined in radio/network to
     * CellSignalStrengthCdma
     * @param cdma CdmaSignalStrength defined in CdmaSignalStrength.aidl
     * @param evdo EvdoSignalStrength defined in EvdoSignalStrength.aidl
     * @return The converted CellSignalStrengthCdma
     */
    public static CellSignalStrengthCdma convertHalCdmaSignalStrength(
            vendor.qti.hardware.radio.qtiradio.CdmaSignalStrength cdma,
            vendor.qti.hardware.radio.qtiradio.EvdoSignalStrength evdo) {
        return new CellSignalStrengthCdma(-cdma.dbm, -cdma.ecio, -evdo.dbm, -evdo.ecio,
                evdo.signalNoiseRatio);
    }

    /**
     * Convert a LteSignalStrength defined in LteSignalStrength.aidl to CellSignalStrengthLte
     * @param ss LteSignalStrength defined in LteSignalStrength.aidl
     * @return The converted CellSignalStrengthLte
     */
    public static CellSignalStrengthLte convertHalLteSignalStrength(
            vendor.qti.hardware.radio.qtiradio.LteSignalStrength ss) {
        return new CellSignalStrengthLte(
                CellSignalStrengthLte.convertRssiAsuToDBm(ss.signalStrength),
                ss.rsrp != CellInfo.UNAVAILABLE ? -ss.rsrp : ss.rsrp,
                ss.rsrq != CellInfo.UNAVAILABLE ? -ss.rsrq : ss.rsrq,
                CellSignalStrengthLte.convertRssnrUnitFromTenDbToDB(ss.rssnr), ss.cqiTableIndex,
                ss.cqi, ss.timingAdvance);
    }

    /**
     * Convert a WcdmaSignalStrength defined in WcdmaSignalStrength.aidl to CellSignalStrengthWcdma
     * @param ss WcdmaSignalStrength defined in WcdmaSignalStrength.aidl
     * @return The converted CellSignalStrengthWcdma
     */
    public static CellSignalStrengthWcdma convertHalWcdmaSignalStrength(
            vendor.qti.hardware.radio.qtiradio.WcdmaSignalStrength ss) {
        CellSignalStrengthWcdma ret = new CellSignalStrengthWcdma(
                CellSignalStrength.getRssiDbmFromAsu(ss.signalStrength),
                ss.bitErrorRate, CellSignalStrength.getRscpDbmFromAsu(ss.rscp),
                CellSignalStrength.getEcNoDbFromAsu(ss.ecno));
        if (ret.getRssi() == CellInfo.UNAVAILABLE && ret.getRscp() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a TdscdmaSignalStrength defined in TdscdmaSignalStrength.aidl to
     * CellSignalStrengthTdscdma
     * @param ss TdscdmaSignalStrength defined in TdscdmaSignalStrength.aidl
     * @return The converted CellSignalStrengthTdscdma
     */
    public static CellSignalStrengthTdscdma convertHalTdscdmaSignalStrength(
            vendor.qti.hardware.radio.qtiradio.TdscdmaSignalStrength ss) {
        CellSignalStrengthTdscdma ret = new CellSignalStrengthTdscdma(
                CellSignalStrength.getRssiDbmFromAsu(ss.signalStrength),
                ss.bitErrorRate, CellSignalStrength.getRscpDbmFromAsu(ss.rscp));
        if (ret.getRssi() == CellInfo.UNAVAILABLE && ret.getRscp() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a NrSignalStrength defined in NrSignalStrength.aidl to CellSignalStrengthNr
     * @param ss NrSignalStrength defined in NrSignalStrength.aidl
     * @return The converted CellSignalStrengthNr
     */
    public static CellSignalStrengthNr convertHalNrSignalStrength(
            vendor.qti.hardware.radio.qtiradio.NrSignalStrength ss) {
        return new CellSignalStrengthNr(CellSignalStrengthNr.flip(ss.csiRsrp),
                CellSignalStrengthNr.flip(ss.csiRsrq), ss.csiSinr, ss.csiCqiTableIndex,
                primitiveArrayToArrayList(ss.csiCqiReport), CellSignalStrengthNr.flip(ss.ssRsrp),
                CellSignalStrengthNr.flip(ss.ssRsrq), ss.ssSinr, CellInfo.UNAVAILABLE);
    }

    /**
     * Convert to RadioAccessSpecifier.aidl
     * @param ras Radio access specifier
     * @return The converted RadioAccessSpecifier
     */
    public static vendor.qti.hardware.radio.qtiradio.RadioAccessSpecifier
            convertToHalRadioAccessSpecifierAidl(RadioAccessSpecifier ras) {
        vendor.qti.hardware.radio.qtiradio.RadioAccessSpecifier rasInHalFormat =
                new vendor.qti.hardware.radio.qtiradio.RadioAccessSpecifier();
        vendor.qti.hardware.radio.qtiradio.RadioAccessSpecifierBands bandsInHalFormat =
                new vendor.qti.hardware.radio.qtiradio.RadioAccessSpecifierBands();
        rasInHalFormat.accessNetwork = convertToHalAccessNetworkAidl(ras.getRadioAccessNetwork());
        int[] bands;
        if (ras.getBands() != null) {
            bands = new int[ras.getBands().length];
            for (int i = 0; i < ras.getBands().length; i++) {
                bands[i] = ras.getBands()[i];
            }
        } else {
            bands = new int[0];
        }
        switch (ras.getRadioAccessNetwork()) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                bandsInHalFormat.setGeranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                bandsInHalFormat.setUtranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                bandsInHalFormat.setEutranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.NGRAN:
                bandsInHalFormat.setNgranBands(bands);
                break;
            default:
                return null;
        }
        rasInHalFormat.bands = bandsInHalFormat;

        int[] channels;
        if (ras.getChannels() != null) {
            channels = new int[ras.getChannels().length];
            for (int i = 0; i < ras.getChannels().length; i++) {
                channels[i] = ras.getChannels()[i];
            }
        } else {
            channels = new int[0];
        }
        rasInHalFormat.channels = channels;

        return rasInHalFormat;
    }

    /**
     * Convert to AccessNetwork.aidl
     * @param accessNetworkType Access network type
     * @return The converted AccessNetwork
     */
    public static int convertToHalAccessNetworkAidl(int accessNetworkType) {
        switch (accessNetworkType) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                return vendor.qti.hardware.radio.qtiradio.AccessNetwork.GERAN;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                return vendor.qti.hardware.radio.qtiradio.AccessNetwork.UTRAN;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                return vendor.qti.hardware.radio.qtiradio.AccessNetwork.EUTRAN;
            case AccessNetworkConstants.AccessNetworkType.CDMA2000:
                return vendor.qti.hardware.radio.qtiradio.AccessNetwork.CDMA2000;
            case AccessNetworkConstants.AccessNetworkType.IWLAN:
                return vendor.qti.hardware.radio.qtiradio.AccessNetwork.IWLAN;
            case AccessNetworkConstants.AccessNetworkType.NGRAN:
                return vendor.qti.hardware.radio.qtiradio.AccessNetwork.NGRAN;
            case AccessNetworkConstants.AccessNetworkType.UNKNOWN:
            default:
                return vendor.qti.hardware.radio.qtiradio.AccessNetwork.UNKNOWN;
        }
    }

    /**
     * Convert to SearchType.aidl
     * @param searchType search type
     * @return The converted SearchType
     */
    public static int convertToHalSearchTypeAidl(int searchType) {
        switch (searchType) {
            case SEARCH_TYPE_PLMN_AND_CAG:
                return vendor.qti.hardware.radio.qtiradio.SearchType.PLMN_AND_CAG;
            case SEARCH_TYPE_PLMN_ONLY:
                return vendor.qti.hardware.radio.qtiradio.SearchType.PLMN_ONLY;
            default:
                return vendor.qti.hardware.radio.qtiradio.SearchType.INVALID;
        }
    }

    static QosParametersResult createQosParametersResultFromQtiRadioHalStruct(
            vendor.qti.hardware.radio.qtiradio.V2_7.QosParametersResult qosParametersResult) {

        if (qosParametersResult == null) return null;

        // Convert QoS parameters from IQtiRadio HAL type to IRadio HALtype
        android.hardware.radio.V1_6.Qos defaultQosIRadioStruct =
                convertQtiHalQosToIRadioHal(qosParametersResult.defaultQos);
        List<android.hardware.radio.V1_6.QosSession> qosSessionsIRadioStruct =
                convertQtiHalQosSessionListToIRadioHal(qosParametersResult.qosSessions);

        // Create Qos (extphonelib) objects from IRadio HAl structures
        Qos defaultQos = Qos.create(defaultQosIRadioStruct);
        List<QosBearerSession> qosSessions = new ArrayList<>();

        if (qosSessionsIRadioStruct != null) {
            qosSessions = qosSessionsIRadioStruct.stream().map(session ->
                    QosBearerSession.create(session)).collect(Collectors.toList());
        }

        QosParametersResult qosParams = new QosParametersResult.Builder()
                .setDefaultQos(defaultQos)
                .setQosBearerSessions(qosSessions)
                .build();

        Log.d(LOG_TAG, "Created QosParametersResult: " + (qosParams == null ? "null" : qosParams));

        return qosParams;
    }

    static android.hardware.radio.V1_6.Qos convertQtiHalQosToIRadioHal(
            vendor.qti.hardware.radio.qtiradio.V2_7.Qos srcQos) {

        Log.d(LOG_TAG, "convertQtiHalQosToIRadioHal, srcQos: " + srcQos);
        android.hardware.radio.V1_6.Qos desQos =
                new android.hardware.radio.V1_6.Qos();

        if (srcQos == null) {
            return desQos;
        }

        switch(srcQos.getDiscriminator()) {
            case vendor.qti.hardware.radio.qtiradio.V2_7.Qos.hidl_discriminator.eps:
                Log.d(LOG_TAG, "case eps");
                vendor.qti.hardware.radio.qtiradio.V2_7.EpsQos srcEps = srcQos.eps();

                android.hardware.radio.V1_6.QosBandwidth desEpsDownlink =
                        new android.hardware.radio.V1_6.QosBandwidth();
                desEpsDownlink.maxBitrateKbps = srcEps.downlink.maxBitrateKbps;
                desEpsDownlink.guaranteedBitrateKbps = srcEps.downlink.guaranteedBitrateKbps;

                android.hardware.radio.V1_6.QosBandwidth desEpsUplink =
                        new android.hardware.radio.V1_6.QosBandwidth();
                desEpsUplink.maxBitrateKbps = srcEps.uplink.maxBitrateKbps;
                desEpsUplink.guaranteedBitrateKbps = srcEps.uplink.guaranteedBitrateKbps;

                android.hardware.radio.V1_6.EpsQos desEps =
                        new android.hardware.radio.V1_6.EpsQos();
                desEps.qci = srcEps.qci;
                desEps.downlink = desEpsDownlink;
                desEps.uplink = desEpsUplink;

                desQos.eps(desEps);
                break;


            case vendor.qti.hardware.radio.qtiradio.V2_7.Qos.hidl_discriminator.nr:
                Log.d(LOG_TAG, "case nr");
                vendor.qti.hardware.radio.qtiradio.V2_7.NrQos srcNr = srcQos.nr();

                android.hardware.radio.V1_6.QosBandwidth desNrDownlink =
                        new android.hardware.radio.V1_6.QosBandwidth();
                desNrDownlink.maxBitrateKbps = srcNr.downlink.maxBitrateKbps;
                desNrDownlink.guaranteedBitrateKbps = srcNr.downlink.guaranteedBitrateKbps;

                android.hardware.radio.V1_6.QosBandwidth desNrUplink =
                        new android.hardware.radio.V1_6.QosBandwidth();
                desNrUplink.maxBitrateKbps = srcNr.uplink.maxBitrateKbps;
                desNrUplink.guaranteedBitrateKbps = srcNr.uplink.guaranteedBitrateKbps;

                android.hardware.radio.V1_6.NrQos desNr = new android.hardware.radio.V1_6.NrQos();
                desNr.fiveQi = srcNr.fiveQi;
                desNr.qfi = srcNr.qfi;
                desNr.averagingWindowMs = srcNr.averagingWindowMs;
                desNr.downlink = desNrDownlink;
                desNr.uplink = desNrUplink;

                desQos.nr(desNr);
                break;

            default:
                Log.d(LOG_TAG, "Found unknown descriminator for Qos: " + srcQos.getDiscriminator());

        }
        return desQos;
    }

    static List<android.hardware.radio.V1_6.QosSession> convertQtiHalQosSessionListToIRadioHal(
            List<vendor.qti.hardware.radio.qtiradio.V2_7.QosSession> srcQosSessions) {

        List<android.hardware.radio.V1_6.QosSession> destinationList = new ArrayList<>();

        if (srcQosSessions == null || srcQosSessions.size() == 0) {
            return destinationList;
        }

        for (vendor.qti.hardware.radio.qtiradio.V2_7.QosSession srcSession : srcQosSessions) {
            android.hardware.radio.V1_6.QosSession desSession =
                    new android.hardware.radio.V1_6.QosSession();

            List<vendor.qti.hardware.radio.qtiradio.V2_7.QosFilter> srcQosFilters =
                    srcSession.qosFilters;
            ArrayList<android.hardware.radio.V1_6.QosFilter> desQosFilters = new ArrayList<>();

            if (srcQosFilters != null) {
                for (vendor.qti.hardware.radio.qtiradio.V2_7.QosFilter srcQosFilter :
                        srcQosFilters) {
                    android.hardware.radio.V1_6.QosFilter desQosFilter =
                            new android.hardware.radio.V1_6.QosFilter();
                    desQosFilter.localPort =
                            convertQtiHalMaybePortToIRadioHal(srcQosFilter.localPort);
                    desQosFilter.remotePort =
                            convertQtiHalMaybePortToIRadioHal(srcQosFilter.remotePort);
                    desQosFilter.tos =
                            convertQtiHalTypeOfServiceToIRadioHal(srcQosFilter.tos);
                    desQosFilter.flowLabel =
                            convertQtiHalIpv6FlowLabelToIRadioHal(srcQosFilter.flowLabel);
                    desQosFilter.spi =
                            convertQtiHalIpsecSpiToIRadioHal(srcQosFilter.spi);
                    desQosFilter.localAddresses = srcQosFilter.localAddresses;
                    desQosFilter.remoteAddresses = srcQosFilter.remoteAddresses;
                    desQosFilter.protocol = srcQosFilter.protocol;
                    desQosFilter.direction = srcQosFilter.direction;
                    desQosFilter.precedence = srcQosFilter.precedence;

                    desQosFilters.add(desQosFilter);
                }
            }

            desSession.qos = convertQtiHalQosToIRadioHal(srcSession.qos);
            desSession.qosSessionId = srcSession.qosSessionId;
            desSession.qosFilters = desQosFilters;

            destinationList.add(desSession);
        }

        return destinationList;
    }

    static android.hardware.radio.V1_6.MaybePort convertQtiHalMaybePortToIRadioHal(
            vendor.qti.hardware.radio.qtiradio.V2_7.MaybePort srcMaybePort) {

        android.hardware.radio.V1_6.MaybePort destinationMaybePort =
                new android.hardware.radio.V1_6.MaybePort();

        if (srcMaybePort == null ||
                srcMaybePort.getDiscriminator() !=
                vendor.qti.hardware.radio.qtiradio.V2_7.MaybePort.hidl_discriminator.range) {
            return destinationMaybePort;
        }

        vendor.qti.hardware.radio.qtiradio.V2_7.PortRange sourcePortRange = srcMaybePort.range();
        android.hardware.radio.V1_6.PortRange destinationPortRange =
                convertQtiHalPortRangeToIRadioHal(sourcePortRange);
        destinationMaybePort.range(destinationPortRange);

        return destinationMaybePort;
    }

    static android.hardware.radio.V1_6.PortRange convertQtiHalPortRangeToIRadioHal(
            vendor.qti.hardware.radio.qtiradio.V2_7.PortRange sourcePortRange) {

        android.hardware.radio.V1_6.PortRange destinationPortRange =
                new android.hardware.radio.V1_6.PortRange();

        if (sourcePortRange == null) {
            return destinationPortRange;
        }

        destinationPortRange.start = sourcePortRange.start;
        destinationPortRange.end = sourcePortRange.end;

        return destinationPortRange;
    }

    static android.hardware.radio.V1_6.QosFilter.TypeOfService
            convertQtiHalTypeOfServiceToIRadioHal(
            vendor.qti.hardware.radio.qtiradio.V2_7.QosFilter.TypeOfService srcTypeOfService) {

        android.hardware.radio.V1_6.QosFilter.TypeOfService destinationTypeOfService =
                new android.hardware.radio.V1_6.QosFilter.TypeOfService();

        if (srcTypeOfService == null ||
                srcTypeOfService.getDiscriminator() !=
                vendor.qti.hardware.radio.qtiradio.V2_7.QosFilter.TypeOfService
                .hidl_discriminator.value) {
            return destinationTypeOfService;
        }

        destinationTypeOfService.value(srcTypeOfService.value());
        return destinationTypeOfService;
    }

    static android.hardware.radio.V1_6.QosFilter.Ipv6FlowLabel
            convertQtiHalIpv6FlowLabelToIRadioHal(
            vendor.qti.hardware.radio.qtiradio.V2_7.QosFilter.Ipv6FlowLabel srcIpv6FlowLabel) {

        android.hardware.radio.V1_6.QosFilter.Ipv6FlowLabel destinationIpv6FlowLabel =
                new android.hardware.radio.V1_6.QosFilter.Ipv6FlowLabel();

        if (srcIpv6FlowLabel == null ||
                srcIpv6FlowLabel.getDiscriminator() !=
                vendor.qti.hardware.radio.qtiradio.V2_7.QosFilter.Ipv6FlowLabel
                .hidl_discriminator.value) {
            return destinationIpv6FlowLabel;
        }

        destinationIpv6FlowLabel.value(srcIpv6FlowLabel.value());
        return destinationIpv6FlowLabel;
    }

    static android.hardware.radio.V1_6.QosFilter.IpsecSpi convertQtiHalIpsecSpiToIRadioHal(
            vendor.qti.hardware.radio.qtiradio.V2_7.QosFilter.IpsecSpi srcIpsecSpi) {

        android.hardware.radio.V1_6.QosFilter.IpsecSpi destinationIpsecSpi =
                new android.hardware.radio.V1_6.QosFilter.IpsecSpi();

        if (srcIpsecSpi == null ||
                srcIpsecSpi.getDiscriminator() !=
                vendor.qti.hardware.radio.qtiradio.V2_7.QosFilter.IpsecSpi
                .hidl_discriminator.value) {
            return destinationIpsecSpi;
        }

        destinationIpsecSpi.value(srcIpsecSpi.value());
        return destinationIpsecSpi;
    }

    public static vendor.qti.hardware.radio.qtiradio.CiwlanConfig
            converttoHalCiwlanConfig(CiwlanConfig config) {
        vendor.qti.hardware.radio.qtiradio.CiwlanConfig ciwlanConfig =
                new vendor.qti.hardware.radio.qtiradio.CiwlanConfig();

        ciwlanConfig.homeMode = config.getCiwlanHomeMode();
        ciwlanConfig.roamMode = config.getCiwlanRoamMode();

        return ciwlanConfig;
    }

    /**
     * Convert a CellIdentityNr defined in CellIdentityNr.aidl to CellIdentityNr
     * @param cid CellIdentityNr defined in CellIdentityNr.aidl
     * @return The converted CellIdentityNr
     */
    public static CellIdentityNr convertHalCellIdentityNr(
            vendor.qti.hardware.radio.qtiradio.QtiCellIdentityNr cid) {
        return new CellIdentityNr(cid.cNr.pci, cid.cNr.tac, cid.cNr.nrarfcn, cid.cNr.bands,
                cid.cNr.mcc, cid.cNr.mnc, cid.cNr.nci, cid.cNr.operatorNames.alphaLong,
                cid.cNr.operatorNames.alphaShort,
                primitiveArrayToArrayList(cid.cNr.additionalPlmns));
    }

    public static boolean isScbmEnabled() {
        return false;
    }

    public static int getMaxDataDeactivateDelayTime(Context context) {
        return MAX_DATA_DEACTIVATE_DELAY_TIME;
    }

    public static int getAccessMode(NetworkScanRequest networkScanRequest) {
        return -1;
    }

    public static int getSearchType(NetworkScanRequest networkScanRequest) {
        return -1;
    }

    public static String getSmartDdsSwitchKeyName() {
        return SMART_DDS_SWITCH_KEY;
    }

    public static boolean isCneAidlAvailable() {
        return false;
    }

    public static vendor.qti.hardware.data.dynamicddsaidlservice.
            ISubscriptionManager getDynamicddsISubscriptionManager() {
        return null;
    }

    public static ISubscriptionManager getDynamicSubscriptionManager() {
        return null;
    }

    public static boolean initIFactoryAidl() {
        return false;
    }
}
