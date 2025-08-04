/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.xmlhelper;

public enum Type {
    UNKNOWN("UNKNOWN"),
    STRING("STRING"),
    INTEGER("INTEGER"),
    FLOAT("FLOAT"),
    BOOLEAN("BOOLEAN");

    private final String mValue;

    Type(String type){
        mValue = type;
    }

    public String getValue(){
        return mValue;
    }

    public static Type convert(String type){
        for (Type t : Type.values()) {
            if (t.getValue().equals(type)) {
                return t;
            }
        }
        return UNKNOWN;
    }

    public boolean isInt() {
        return this == INTEGER;
    }

    public boolean isBoolean() {
        return this == BOOLEAN;
    }

    public boolean isString() {
        return this == STRING;
    }

    public boolean isFloat() {
        return this == FLOAT;
    }
}
