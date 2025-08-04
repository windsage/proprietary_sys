/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony.data;

import android.telephony.data.EpsQos;
import android.telephony.data.NrQos;
import android.telephony.data.Qos;
import android.telephony.data.Qos.QosBandwidth;
import android.telephony.data.QosBearerFilter;
import android.telephony.data.QosBearerFilter.PortRange;
import android.telephony.data.QosBearerSession;
import android.util.Log;

import com.qti.extphone.QosParametersResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that takes care of conversion of Qos related objects from one type to another
 *
 * Types of conversions:
 *
 * 1. 'com.qti.extphone' package type to 'android.telephony.data' package type.
 *    e.g., com.qti.extphone.Qos to android.telephony.data.Qos
 *
 * 2. 'com.qti.extphone' package type to corresponding IRadio HAL type
 *    e.g., com.qti.extphone.NrQos to android.hardware.radio.V1_6.NrQos
 */
public class QosParametersUtils {

    public static final int QOS_TYPE_EPS = 1;
    public static final int QOS_TYPE_NR = 2;

    public static final String TAG = "QosParametersUtils";

    public static android.telephony.data.Qos convertToFrameworkQos(
            com.qti.extphone.Qos sourceQos) {
        if (sourceQos == null) {
            Log.e(TAG, "convertToFrameworkQos, sourceQos is null");
            return null;
        }

        Qos frameworkQos = null;
        QosBandwidth downlink = new QosBandwidth(
                sourceQos.getDownlinkBandwidth().getMaxBitrateKbps(),
                sourceQos.getDownlinkBandwidth().getGuaranteedBitrateKbps());

        QosBandwidth uplink = new QosBandwidth(
                sourceQos.getUplinkBandwidth().getMaxBitrateKbps(),
                sourceQos.getUplinkBandwidth().getGuaranteedBitrateKbps());

        switch(sourceQos.getType()) {
            case QOS_TYPE_EPS:
                com.qti.extphone.EpsQos sourceEpsQos = (com.qti.extphone.EpsQos) sourceQos;
                frameworkQos = new EpsQos(downlink, uplink, sourceEpsQos.getQci());
                break;
            case QOS_TYPE_NR:
                com.qti.extphone.NrQos sourceNrQos = (com.qti.extphone.NrQos) sourceQos;
                frameworkQos = new NrQos(downlink, uplink, sourceNrQos.getQfi(),
                        sourceNrQos.get5Qi(), sourceNrQos.getAveragingWindow());
                break;
            default:
                Log.d(TAG, "convertToFrameworkQos, unknown QoS type: " + sourceQos.getType());
        }

        Log.d(TAG, "convertToFrameworkQos"
                + ": source: " + sourceQos
                + ", target: " + frameworkQos);
        return frameworkQos;
    }

    public static QosBearerFilter convertToFrameworkQosBearerFilter(
            com.qti.extphone.QosBearerFilter sourceQosFilter) {
        if (sourceQosFilter == null) {
            Log.e(TAG, "convertToFrameworkQosBearerFilter, sourceQosFilter is null");
            return null;
        }

        com.qti.extphone.QosBearerFilter.PortRange sourceLocalPort =
                sourceQosFilter.getLocalPortRange();
        com.qti.extphone.QosBearerFilter.PortRange sourceRemotePort =
                sourceQosFilter.getRemotePortRange();

        PortRange targetLocalPort  = (sourceLocalPort != null)
                ? new PortRange(sourceLocalPort.getStart(), sourceLocalPort.getEnd())
                : null;
        PortRange targetRemotePort = (sourceRemotePort != null)
                ? new PortRange(sourceRemotePort.getStart(), sourceRemotePort.getEnd())
                : null;

        return new QosBearerFilter(
                sourceQosFilter.getLocalAddresses(),
                sourceQosFilter.getRemoteAddresses(),
                targetLocalPort,
                targetRemotePort,
                sourceQosFilter.getProtocol(),
                sourceQosFilter.getTypeOfServiceMask(),
                sourceQosFilter.getFlowLabel(),
                sourceQosFilter.getSpi(),
                sourceQosFilter.getDirection(),
                sourceQosFilter.getPrecedence());
    }

    public static List<QosBearerFilter> convertToFrameworkQosFilterList(
            List<com.qti.extphone.QosBearerFilter> sourceBearerFilters) {
        List<QosBearerFilter> targetBearerFilters = new ArrayList<>();

        if (sourceBearerFilters == null) {
            return targetBearerFilters;
        }

        for (com.qti.extphone.QosBearerFilter sourceFilter : sourceBearerFilters) {
            QosBearerFilter targetFilter = convertToFrameworkQosBearerFilter(sourceFilter);
            if (targetFilter != null) {
                targetBearerFilters.add(targetFilter);
            }
        }

        return targetBearerFilters;
    }

    public static QosBearerSession convertToFrameworkQosBearerSession(
            com.qti.extphone.QosBearerSession sourceQosSession) {
        if (sourceQosSession == null) {
            return null;
        }

        return new QosBearerSession(
                sourceQosSession.getQosBearerSessionId(),
                convertToFrameworkQos(sourceQosSession.getQos()),
                convertToFrameworkQosFilterList(sourceQosSession.getQosBearerFilterList()));
    }

    public static List<QosBearerSession> convertToFrameworkQosBearerSessionsList(
            List<com.qti.extphone.QosBearerSession> sourceBearerSessions) {
        List<QosBearerSession> targetBearerSessions = new ArrayList<>();

        if (sourceBearerSessions == null) {
            return targetBearerSessions;
        }

        for (com.qti.extphone.QosBearerSession sourceSession : sourceBearerSessions) {
            QosBearerSession targetSession = convertToFrameworkQosBearerSession(sourceSession);
            if (targetSession != null) {
                targetBearerSessions.add(targetSession);
            }
        }

        return targetBearerSessions;
    }
}