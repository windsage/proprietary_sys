/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

@VintfStability
@Backing(type="int")
enum StatusCode {
    SUCCESS,
    /**
     * < Request was processed successfully.
     */
    FAILURE,
    /**
     * < Request was processed unsuccessfully.
     */
    MEMORY_ERROR,
    /**
     * < Error in memory allocation.
     */
    INVALID_LISTENER,
    /**
     * < Provided listener is not valid.
     */
    INVALID_PARAM,
    /**
     * < Invalid parameter(s).
     */
    SERVICE_NOTALLOWED,
    /**
     * < Service is not allowed.
     */
    SERVICE_UNAVAILABLE,
    /**
     * < Service is not available.
     */
    INVALID_FEATURE_TAG,
    /**
     * < Invalid feature tag.
     */
    DNSQUERY_PENDING,
    /**
     * < DNS query pending.
     */
    DNSQUERY_FAILURE,
    /**
     * < DNS query failed.
     */
    SERVICE_DIED,
    /**
     * < Android native service stopped working.
     */
    MESSAGE_NOTALLOWED,
    /**
     * < SIP Message not allowed.
     */
    DISPATCHER_SEND_SUCCESS,
    /**
     * < This value is for internal use only. Should not be part of the document
     */
    INVALID_MAX,
    /**
     * Request is not supported.
     */
    UNSUPPORTED = -1,
}
