/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.fingerprint;

import vendor.qti.hardware.fingerprint.IQfpExtendedSession;
import vendor.qti.hardware.fingerprint.IQfpExtendedSessionCallback;

@VintfStability
interface IQfpExtendedFingerprint {
    /**
     * Creates an instance of IQfpExtendedSession that can be used by the clients
     * to perform various Extended Fingerprint operations.
     *
     * @param  cb  Session callback to be used by HAL to return Async response.
     * @return Extended session handle.
     */
    IQfpExtendedSession createSession(in IQfpExtendedSessionCallback cb);

}
