/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

/**
 * ************************************
 * End of Presence Service Data Types   *
 *
 *
 * ***********************************
 * Start of Options Service Data Types *
 */
@VintfStability
@Backing(type="int")
enum OptionsCmdId {
    /**
     * Command ID corresponding to API GetMyInfo().
     */
    GETMYCDINFO,
    /**
     * Command ID corresponding to API SetMyInfo().
     */
    SETMYCDINFO,
    /**
     * Command ID corresponding to API GetContactCap().
     */
    GETCONTACTCAP,
    /**
     * Command ID corresponding to API GetContactListCap().
     */
    GETCONTACTLISTCAP,
    /**
     * Command ID corresponding to API ResponseIncomingOptions().
     */
    RESPONSEINCOMINGOPTIONS,
    /**
     * Default Command ID as Unknown.
     */
    UNKNOWN,
}
