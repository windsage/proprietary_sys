/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

@VintfStability
@Backing(type="int")
enum PresPublishTriggerType {
    /**
     * ETag expired.
     */
    ETAG_EXPIRED,
    /**
     * Move to LTE with VoPS disabled.
     */
    MOVE_TO_LTE_VOPS_DISABLED,
    /**
     * Move to LTE with VoPS enabled.
     */
    MOVE_TO_LTE_VOPS_ENABLED,
    /**
     * Move to eHRPD.
     */
    MOVE_TO_EHRPD,
    /**
     * Move to HSPA+.
     */
    MOVE_TO_HSPAPLUS,
    /**
     * Move to 3G.
     */
    MOVE_TO_3G,
    /**
     * Move to 2G.
     */
    MOVE_TO_2G,
    /**
     * Move to WLAN
     */
    MOVE_TO_WLAN,
    /**
     * Move to IWLAN
     */
    MOVE_TO_IWLAN,
    /**
     * Trigger is unknown.
     */
    UNKNOWN,
    /**
     * Move to NR5G with VoPS disabled.
     */
    MOVE_TO_NR5G_VOPS_DISABLED = 10,
    /**
     * Move to NR5G with VoPS enabled.
     */
    MOVE_TO_NR5G_VOPS_ENABLED = 11,
}
