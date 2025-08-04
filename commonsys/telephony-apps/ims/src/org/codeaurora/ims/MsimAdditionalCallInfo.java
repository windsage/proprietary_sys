/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import java.util.Objects;

/* This class is responsible to cache the multi sim additional Call Information received as part
 * of call state change indication.
 * Currently will receive this information as part of the call end and this can be extended to
 * other call states as well.
 */

public final class MsimAdditionalCallInfo {

    // To store additional info code.
    private int mCode = QtiCallConstants.CODE_UNSPECIFIED;

    public MsimAdditionalCallInfo() {}

    public MsimAdditionalCallInfo(MsimAdditionalCallInfo additionalCallInfo) {
        this(additionalCallInfo.getCode());
    }

    public MsimAdditionalCallInfo(int additionalInfoCode) {
        mCode = additionalInfoCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MsimAdditionalCallInfo)) {
            return false;
        }
        MsimAdditionalCallInfo in = (MsimAdditionalCallInfo)obj;
        return this.mCode == in.getCode();
    }

    public int getCode() {
        return mCode;
    }

    public void setCode(int additionalInfoCode) {
        mCode = additionalInfoCode;
    }

    public String toString() {
        return "MsimAdditionalCallInfo additional code : " + mCode;
    }
}
