/*
  * Copyright (c) 2024 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qvirtvendor;

@VintfStability
enum VMErrorCodes {
    /* There are 2 reason for the timeout failure.
       Once if the time taken by each API is taking more
       time than intended, and the other reason is the Call
       to the VM is a blocking call and
       there is a return code option of a timeout
       to be returned by that blocking call.
       This timeout error is map with the same. */
    TIMEOUT = -2,

    /* If there is any failure in the VM Communication
       or failure to perform the task on the VM this
       code is returned.*/
    FAIL = -1,

    /* For all passing cases this code is returned.*/
    SUCCESS = 0,
}
