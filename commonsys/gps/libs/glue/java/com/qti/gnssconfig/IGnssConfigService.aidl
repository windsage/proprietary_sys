/* ======================================================================
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/

package com.qti.gnssconfig;

import com.qti.gnssconfig.IGnssConfigCallback;
import com.qti.gnssconfig.NtripConfigData;


interface IGnssConfigService {

    void registerCallback(in IGnssConfigCallback callback);
    void getRobustLocationConfig();
    void setRobustLocationConfig(boolean enable, boolean enableForE911);
    void enablePreciseLocation(in NtripConfigData data);
    void disablePreciseLocation();
    void updateNtripGgaConsent(boolean optIn);
    void setNetworkLocationUserConsent(boolean hasUserConsent);
    void injectSuplCert(int suplCertId, in byte[] suplCertData);
    boolean configMerkleTree(in String merkleTreeXml, int xmlSize);
    boolean configOsnmaEnablement(boolean isEnabled);
    void set3rdPartyNtnCapability(boolean isCapable);
    void getNtnConfigSignalMask();
    void setNtnConfigSignalMask(int gpsSignalTypeConfigMask);
}
