/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

@VintfStability
@Backing(type="int")
enum UserConfigKeys {
    UEClientPort = 1001,
    /*
     * UE client port.
     * accepts a string type value
     */
    UEServerPort = 1002,
    /*
     * UE server port.
     * accepts a string type value
     */
    AssociatedURI = 1003,
    /*
     * Associated URI value.
     * accepts a string type value
     */
    UEPublicIPAddress = 1004,
    /*
     * Recieved UE public IP address.
     * accepts a string type value
     */
    UEPublicPort = 1005,
    /*
     * UE public IP port.
     * accepts a string type value
     */
    SipPublicUserId = 1006,
    /*
     * User public ID.
     * accepts a string type value
     */
    SipPrivateUserId = 1008,
    /*
     * Private user ID.
     * accepts a string type value
     */
    SipHomeDomain = 1009,
    /*
     * Home domain address.
     * accepts a string type value
     */
    UEPubGruu = 1010,
    /*
     * UE public GRUU.
     * accepts a string type value
     */
    LocalHostIPAddress = 1011,
    /*
     * UE public IP address.
     * accepts a string type value
     */
    IpType = 1012,
    /*
     * UE IP type. of type ipTypeEnum
     * accepts a string type value
     */
    IMEIStr = 1013,
    /*
     * UE IMEI value.
     * accepts a string type value
     */
    UEOldSAClientPort = 1014,
}
