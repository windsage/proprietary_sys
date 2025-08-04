/* ======================================================================
*  Copyright (c) 2022, 2024 Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/
package com.qti.gtp;

import com.qti.gtp.IGTPServiceCallback;
import com.qti.gtp.GtpRequestData;

interface IGTPService {

   void requestLocationUpdates(in IGTPServiceCallback cb, in GtpRequestData request);

   void removeLocationUpdates();

   void requestPassiveLocationUpdates(in IGTPServiceCallback cb);

   void removePassiveLocationUpdates();

   void setUserConsent(in boolean userConsent);

   void requestWwanLocationUpdates(in IGTPServiceCallback cb, in GtpRequestData request);

   void removeWwanLocationUpdates();

   void requestPassiveWwanLocationUpdates(in IGTPServiceCallback cb);

   void removePassiveWwanLocationUpdates();

}
