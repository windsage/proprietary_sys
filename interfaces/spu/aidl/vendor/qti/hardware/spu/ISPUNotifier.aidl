/**
*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*
*/

package vendor.qti.hardware.spu;

@VintfStability
interface ISPUNotifier {
    /**
    * SPU notifier callback to be implemented by SPU HAL client
    *
    * @param in eventId  SPU event type
    */
    oneway void callback(in int eventId);
}
