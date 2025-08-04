/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.FeatureDesc;

/**
 * RCS (Rich Client Suite) Features supported on device,
 *  as specified in GSMA RCS 7.0.
 */
@VintfStability
parcelable CapabilityInfo {
    /**
     * Instant Message session feature support.
     */
    boolean imSupported;
    /**
     * File transfer feature support.
     */
    boolean ftSupported;
    /**
     * File transfer Thumbnail feature support.
     */
    boolean ftThumbSupported;
    /**
     * File transfer Store and forward feature support.
     */
    boolean ftSnFSupported;
    /**
     * File transfer over HTTP feature support.
     */
    boolean ftHttpSupported;
    /**
     * Image sharing feature support.
     */
    boolean imageShareSupported;
    /**
     * Video sharing during a CS call support -- IR-74.
     */
    boolean videoShareDuringCSSupported;
    /**
     * Video sharing outside of voice call support -- IR-84.
     */
    boolean videoShareSupported;
    /**
     * Social presence feature support.
     */
    boolean socialPresenceSupported;
    /**
     * Presence discovery feature support.
     */
    boolean capDiscViaPresenceSupported;
    /**
     * IP voice call feature support (IR-92/IR-58).
     */
    boolean ipVoiceSupported;
    /**
     * IP video call feature support (IR-92/IR-58).
     */
    boolean ipVideoSupported;
    /**
     * IP Geo location Pull using File Transfer feature support.
     */
    boolean geoPullFtSupported;
    /**
     * IP Geo location Pull feature support.
     */
    boolean geoPullSupported;
    /**
     * IP Geo location Push feature support.
     */
    boolean geoPushSupported;
    /**
     * Standalone messaging feature support.
     */
    boolean smSupported;
    /**
     * Full Store and Forward Group Chat information feature.
     */
    boolean fullSnFGroupChatSupported;
    /**
     * RCS IP Voice call feature support.
     */
    boolean rcsIpVoiceCallSupported;
    /**
     * RCS IP Video call feature support.
     */
    boolean rcsIpVideoCallSupported;
    /**
     * RCS IP Video call feature support.
     */
    boolean rcsIpVideoOnlyCallSupported;
    /**
     * IP Geo SMS support.
     */
    boolean geoSmsSupported;
    /**
     * Call Composer feature support.
     */
    boolean callComposerSupported;
    /**
     * Post Call Information feature support.
     */
    boolean postCallSupported;
    /**
     * Shared Map feature support.
     */
    boolean sharedMapSupported;
    /**
     * Shared Sketch feature support.
     */
    boolean sharedSketchSupported;
    /**
     * Chatbot feature support.
     */
    boolean chatBotSupported;
    /**
     * Chatbot role support.
     */
    boolean chatBotRoleSupported;
    /**
     * MMtel Call Composer feature support.
     */
    boolean mmtelCallComposerSupported;
    /**
     * Standalone feature support.
     */
    boolean standaloneChatbotSupported;
    /**
     * List of supported extensions.
     * used for custom Feature Tags
     * Please refer to GSMA RCS 7.1 documentation
     * for the format of feature Tags
     */
    String[] mExts;
    /*
     * Above fields kept for backwards compatibility only
     *
     *
     * List of features and versions supported on device.
     * All feature tags and extension feature tags supported will be part of
     * this list.
     */
    FeatureDesc[] featureTagData;
    /**
     * Time used to compute when to query again.
     *  Wallclock time of format type NTP (Network Time Protocol)
     */
    long capTimestamp;
}
