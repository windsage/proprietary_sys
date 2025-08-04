/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

@VintfStability
@Backing(type="int")
enum UceStatusCode {
    SUCCESS,
    FAILURE,
    SUCCESS_ASYNC_UPDATE,
    INVALID_SERVICE_HANDLE,
    INVALID_LISTENER_HANDLE,
    INVALID_PARAM,
    FETCH_ERROR,
    REQUEST_TIMEOUT,
    INSUFFICIENT_MEMORY,
    LOST_NET,
    NOT_SUPPORTED,
    NOT_FOUND,
    SERVICE_UNAVAILABLE,
    NO_CHANGE_IN_CAP,
    INVALID_FEATURE_TAG,
    SERVICE_UP,
    SERVICE_DOWN,
    SERVICE_UNKNOWN,
}
