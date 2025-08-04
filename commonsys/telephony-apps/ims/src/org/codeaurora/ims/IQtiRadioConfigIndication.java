/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

/*
 * Internal interface that forwards indications
 */
public interface IQtiRadioConfigIndication {

    // Trigger when QtiRadioConfigAidl service is up
    void onServiceUp();

    // Trigger when QtiRadioConfigAidl service is down
    void onServiceDown();
}
