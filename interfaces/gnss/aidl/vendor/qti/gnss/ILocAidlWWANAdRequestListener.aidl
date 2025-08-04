/*
* Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

@VintfStability
interface ILocAidlWWANAdRequestListener {
    void onWWANAdRequest(int requestId, in byte[] reqPayload);
}
