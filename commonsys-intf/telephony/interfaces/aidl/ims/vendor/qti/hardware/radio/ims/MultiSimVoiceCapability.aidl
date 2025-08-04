/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum MultiSimVoiceCapability {
    NONE,        /* Dual Sim , but no voice capability(NONE) mode */
    DSSS,        /* Dual Sim Single Subscription(DSSS) mode, IMS will be registered only on one
                    subscription. */
    DSDS,        /* Dual Sim Dual Standby(DSDS) mode, concurrent calls on both subscriptions are not
                    possbile. */
    DSDA,        /* Dual Sim Dual Active(DSDA) mode, concurrent calls on both subscriptions are
                    possible. */
    PSEUDO_DSDA, /* Pseudo Dual Sim Dual Active(Pseudo DSDA) mode, concurrent calls on both
                    subscriptions are not possible but user will have option to accept MT call on
                    one subscription when there is an ongoing call on another subscription. */
}
