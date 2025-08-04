/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.internal.deviceinfo;

@VintfStability
@Backing(type="int")
enum ChargerType {
     /**
      * Indicates there is no power source
      */
     NONE = 0,
     /**
      * Indicates power source is an AC charger.
      */
     AC = 1 << 0,
     /**
      * Indicates power source is a USB port.
      */
     USB = 1 << 1,
     /**
      * Indicates power source is wireless.
      */
     WIRELESS = 1 << 2,
}
