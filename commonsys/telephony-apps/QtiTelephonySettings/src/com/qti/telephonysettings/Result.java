/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings;

import com.qti.extphone.Status;
import com.qti.extphone.Token;

public final class Result {
    int mSlotId;
    Status mStatus;
    Object mData;

    public Result(int slotId, Status status, Object data) {
        this.mSlotId = slotId;
        this.mStatus = status;
        this.mData = data;
    }

    public int getSlotId() {
        return mSlotId;
    }

    public Status getStatus() {
        return mStatus;
    }

    public Object getData() {
        return mData;
    }

    @Override
    public String toString() {
        return "Result{mSlotId=" + mSlotId + ", mStatus=" + mStatus + ", mData=" + mData + "}";
    }
}