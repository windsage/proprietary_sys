/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import java.lang.Integer;
import java.util.Optional;

public class CiwlanCapability {

    private static final String TAG = "CiwlanCapability";

    public static final int INVALID = -1;
    public static final int NONE = 0;
    public static final int DDS = 1;
    public static final int BOTH = 2;

    private int mCiwlanCapability = INVALID;

    public CiwlanCapability(int capability) {
        mCiwlanCapability = capability;
    }

    public Optional<Integer> getCiwlanCapability() {
        if (mCiwlanCapability != INVALID) {
            return Optional.of(new Integer(mCiwlanCapability));
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return "CiwlanCapability: " + mCiwlanCapability;
    }
}
