/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.power.powermodule;

@VintfStability
interface IPowerModule {
    /**
     * send hints to powermodule to perform optimizations
     * based on events/actions
     *
     * @param hintId is unique hint id
     * @param userData is hint name/app name
     * @param userData_1, @param userData_2, @param spaceholder can be used to send feature specific information
     */
    oneway void powerSendEvents(in int hintId, in String userData, in int userData_1,
        in int userData_2, in int[] spaceholder);
}
