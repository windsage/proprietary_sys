/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.trustedui;

@VintfStability
parcelable TUIConfig {
    /**
     * Enable/Disable use of Secure Indicator.
     */
    boolean useSecureIndicator;
    /**
     * String identifier for the layout to be displayed on the screen.
     */
    String layoutId;
    /**
     * Optionally enable periodic authentication of Secure content displayed on the screen. This
     * can be enabled if the screen content is not refreshed at display refresh rate.
     * @caution The use of this feature can cause some performance impact.
     */
    boolean enableFrameAuth;
}
