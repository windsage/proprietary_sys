/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

@VintfStability
@Backing(type="int")
enum PresenceCmdId {
    /**
     * Command ID corresponding to function Publish().
     */
    PUBLISHMYCAP,
    /**
     * Command ID corresponding to function GetContactCap().
     */
    GETCONTACTCAP,
    /**
     * Command ID corresponding to function GetContactListCap().
     */
    GETCONTACTLISTCAP,
    /**
     * Command ID corresponding to function SetNewFeatureTag().
     */
    SETNEWFEATURETAG,
    /**
     * Command ID corresponding to API ReenableService().
     */
    REENABLE_SERVICE,
    /**
     * Command ID is unknown.
     */
    UNKNOWN,
}
