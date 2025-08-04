/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.preciseposition;

import android.location.Location;

oneway interface IPrecisePositionCallback {
    void onLocationAvailable(in Location location);
    void onResponseCallback(in int response);
}
