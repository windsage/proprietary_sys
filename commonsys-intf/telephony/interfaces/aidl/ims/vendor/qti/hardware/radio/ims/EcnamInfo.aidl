/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

/**
 * EcnamInfo is used to indicate information like display name, icon, info and business card.
 * Telephony will process eCNAM Info based on individual parameters if not null.
 */
@VintfStability
parcelable EcnamInfo {
    String name;     // Display name
    String iconUrl;  // Iconic representation of the callee or caller
    String infoUrl;  // Describes the caller or callee through web page
    String cardUrl;  // Business card
}
