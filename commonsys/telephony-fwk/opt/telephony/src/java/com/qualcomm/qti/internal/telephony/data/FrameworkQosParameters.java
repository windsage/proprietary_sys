/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony.data;

import android.telephony.data.EpsQos;
import android.telephony.data.NrQos;
import android.telephony.data.Qos;
import android.telephony.data.QosBearerSession;

import com.qti.extphone.QosParametersResult;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a container for the QoS parameters that were added to
 * {@link android.telephony.data.DataCallResponse} in Android S
 */
public class FrameworkQosParameters {
    Qos mDefaultQos;
    List<QosBearerSession> mQosBearerSessions;

    public FrameworkQosParameters() {
        mDefaultQos = null;
        mQosBearerSessions = new ArrayList<>();
    }

    public FrameworkQosParameters(QosParametersResult qtiQosParameters) {
        mDefaultQos = QosParametersUtils
                .convertToFrameworkQos(qtiQosParameters.getDefaultQos());
        mQosBearerSessions = QosParametersUtils
                .convertToFrameworkQosBearerSessionsList(qtiQosParameters.getQosBearerSessions());
    }

    public FrameworkQosParameters(Qos defaultQos, List<QosBearerSession> bearerSessions) {
        mDefaultQos = defaultQos;
        mQosBearerSessions = bearerSessions;
    }

    /**
     * @return default QoS of the data connection received from the network
     */
    public Qos getDefaultQos() {
        return mDefaultQos;
    }

    /**
     * @return All the dedicated bearer QoS sessions of the data connection received from the
     * network.
     */
    public List<QosBearerSession> getQosBearerSessions() {
        return mQosBearerSessions;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("FrameworkQosParameters: {")
                .append(" defaultQos=").append(mDefaultQos)
                .append(" qosBearerSessions=").append(mQosBearerSessions)
                .append("}");
        return sb.toString();
    }
}