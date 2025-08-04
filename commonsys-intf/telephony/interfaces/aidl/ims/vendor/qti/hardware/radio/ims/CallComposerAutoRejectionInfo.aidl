/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.AddressInfo;
import vendor.qti.hardware.radio.ims.AutoCallRejectionInfo;
import vendor.qti.hardware.radio.ims.CallComposerInfo;

/**
 * CallComposerAutoRejectionInfo is used to notify the rejected call information.
 * Telephony will consider CallComposerAutoRejectionInfo only if
 *         CallComposerAutoRejectionInfo#autoCallRejectionInfo is not null.
 */
@VintfStability
parcelable CallComposerAutoRejectionInfo {
    AutoCallRejectionInfo autoCallRejectionInfo;
    CallComposerInfo callComposerInfo;
}
