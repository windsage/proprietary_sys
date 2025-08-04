/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.ridemodeaudio;

/**
 * Audio class can used as storage object which other class
 * can get Audio name and uri string from this object
 */
public class Audio {
    private String mName;
    private String mUriString;

    public Audio(String name, String uriString) {
        mName = name;
        mUriString = uriString;
    }

    /**
     * get Audio name from Audio object
     */
    public String getName() {
        return mName;
    }

    /**
     * get Audio uri string from Audio object
     */
    public String getUriString() {
        return mUriString;
    }
}
