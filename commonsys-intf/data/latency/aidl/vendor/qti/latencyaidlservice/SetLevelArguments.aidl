/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/


package vendor.qti.latencyaidlservice;

import vendor.qti.latencyaidlservice.Level;

@VintfStability
parcelable SetLevelArguments {
   Level effectiveUplink;
   Level effectiveDownlink;

}
