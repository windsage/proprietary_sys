/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.xmlhelper;

public class Item {
    private String mName;
    private String mValue;
    private Type mType;

    public Item(String name, String value, String type) {
        mName = name;
        mValue = value;
        mType = Type.convert(type);
    }

    public Item(String name, String value, Type type) {
        mName = name;
        mValue = value;
        mType = type;
    }

    public String getName(){
        return mName;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value){
        mValue = value;
    }

    public Type getType() {
        return mType;
    }

    @Override
    public String toString() {
        return "ITEM:" + mName + ":" + mValue;
    }
}
