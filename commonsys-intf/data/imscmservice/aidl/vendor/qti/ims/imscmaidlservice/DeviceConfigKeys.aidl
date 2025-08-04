/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

@VintfStability
@Backing(type="int")
enum DeviceConfigKeys {
    UEBehindNAT = 2001,
    /*
     * Indicates whether the UE is behind NAT.
     * accepts a string type value
     */
    IpSecEnabled = 2002,
    /*
     * Indicates whether IPSec is enabled.
     * accepts a string type value
     */
    CompactFormEnabled = 2003,
    /*
     * Indicates whether compact form is enabled.
     * accepts a string type value
     */
    KeepAliveEnableStatus = 2004,
    /*
     * Indicates whether keep alive is enabled.
     * accepts a string type value
     */
    GruuEnabled = 2005,
    /*
     * Indicates whether GRUU is enabled.
     * accepts a string type value
     */
    StrSipOutBoundProxyName = 2006,
    /*
     * Outbound SIP proxy name/IP.
     * accepts a string type value
     */
    SipOutBoundProxyPort = 2007,
    /*
     * Outbound SIP proxy port.
     * accepts a string type value
     */
    PCSCFClientPort = 2008,
    /*
     * P-CSCF client port.
     * accepts a string type value
     */
    PCSCFServerPort = 2009,
    /*
     * P-CSCF server port.
     * accepts a string type value
     */
    ArrAuthChallenge = 2010,
    /*
     * Authentication header.
     * accepts a string type value
     */
    ArrNC = 2011,
    /*
     * Nonce count.
     * accepts a string type value
     */
    ServiceRoute = 2012,
    /*
     * Service route value.
     * accepts a string type value
     */
    SecurityVerify = 2013,
    /*
     * Security verify value.
     * accepts a string type value
     */
    PCSCFOldSAClientPort = 2014,
    /*
     * IPSec old SA PCSCF client port.
     * accepts a string type value
     */
    TCPThresholdValue = 2015,
    /*
     * Configured TCP Threshold Value for SIP
     * accepts a string type value
     */
    PANI = 2016,
    /*
     * PANI header value.
     * accepts a string type value
     */
    PATH = 2017,
    /*
     * Path header value from IMS registration.
     * accepts a string type value
     */
    UriUserPart = 2018,
    PLANI = 2019,
    /**
     * P-Preferred-Association header value
     * accepts a string type value
     */
    PPA = 2020,
    /**
     * PIDENTIFIER value
     * accepts a string type value
     */
    PIDENTIFIER = 2021,
}
