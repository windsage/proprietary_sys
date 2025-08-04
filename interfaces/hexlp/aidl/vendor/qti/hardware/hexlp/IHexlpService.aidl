/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.hardware.hexlp;

import vendor.qti.hardware.hexlp.IHexlp;

@VintfStability
interface IHexlpService {
    /**
    * !
    * @brief            Synchronous. Acquire IHexlp instance.
    *
    * @description      Should ONLY be called after Hexlp Service handle becomes available.
    *
    * @param in         Any necessary flags.
    *
    * @param return     IHexlp instance.
    */
    IHexlp AcquireHexlpSession(in int flags);
}
