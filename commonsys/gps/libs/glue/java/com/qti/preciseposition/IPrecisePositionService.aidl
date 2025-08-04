/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.preciseposition;

import com.qti.preciseposition.IPrecisePositionService;
import com.qti.preciseposition.IPrecisePositionCallback;

interface IPrecisePositionService {
    void startPrecisePositioningSession(in IPrecisePositionCallback callback, in long tbfMsec,
                                        in int preciseType, in int correctionType);
    void stopPrecisePositioningSession();
}
