/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.minkipcbinder;

@VintfStability
interface IMinkTransact {

   @VintfStability
   parcelable TransactPayload {
     byte[] data;
     @nullable android.hardware.common.NativeHandle memObjects;
     int numMemObjects;
   }

    /** Allows Client and server to perform invoke
      * in TransactPayload  : transact data in the form of parcelable data (TransactPayload)
      */
   int doTransact(in TransactPayload minkPayload);
}
