/*
* Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

import vendor.qti.gnss.ILocAidlGnssConfigServiceCallback;
import vendor.qti.gnss.LocAidlNtripConnectionParams;
import vendor.qti.gnss.LocAidlGnssConstellationType;

@VintfStability
interface ILocAidlGnssConfigService {

    void disablePPENtripStream();

    void enablePPENtripStream(in LocAidlNtripConnectionParams params,
        in boolean enableRTKEngine);

    void getGnssSvTypeConfig();

    void getRobustLocationConfig();

    boolean init(in ILocAidlGnssConfigServiceCallback callback);

    void resetGnssSvTypeConfig();

    void setGnssSvTypeConfig(
        in LocAidlGnssConstellationType[] disabledSvTypeList);

    void setRobustLocationConfig(in boolean enable, in boolean enableForE911);

    void updateNTRIPGGAConsent(in boolean consentAccepted);
    boolean configMerkleTree(in String merkleTreeXml, in int xmlSize);
    boolean configOsnmaEnablement(in boolean isEnabled);

    void set3rdPartyNtnCapability(in boolean isCapable);
    void getNtnConfigSignalMask();
    void setNtnConfigSignalMask(in int gpsSignalTypeConfigMask);

    void injectSuplCert(in int suplCertId, in byte[] suplCertData);
}
