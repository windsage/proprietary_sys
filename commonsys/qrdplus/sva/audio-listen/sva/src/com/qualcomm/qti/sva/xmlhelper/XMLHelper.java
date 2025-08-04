/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.xmlhelper;

import android.os.Looper;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XMLHelper {
    private final static String TAG = XMLHelper.class.getSimpleName();
    private final static String TAG_NAME = "name";
    private final static String TAG_VALUE = "value";
    private final static String TAG_TYPE = "type";
    private final static String TAG_CONTAINERGROUP = "ContainerGroup";
    private final static String TAG_CONTAINER = "Container";
    private final static String TAG_ITEM = "Item";

    private String mSMLConfigPath;

    public XMLHelper(String smlConfigPath){
        mSMLConfigPath = smlConfigPath;
    }

    public ContainerGroup read(){
        ContainerGroup containerGroup = null;
        Container container = null;
        Item item = null;
        try (InputStream in = new FileInputStream(new File(mSMLConfigPath))){
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(in, "UTF-8");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if(TAG_CONTAINERGROUP.equals(parser.getName())){
                            containerGroup = createContainerGroup(parser);
                        } else if (TAG_CONTAINER.equals(parser.getName())) {
                            container = createContainer(parser);
                        } else if (TAG_ITEM.equals(parser.getName())) {
                            item = createItem(parser);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (TAG_ITEM.equals(parser.getName())) {
                            if(container != null) container.put(item);
                        } else if (TAG_CONTAINER.equals(parser.getName())) {
                            if (containerGroup != null) containerGroup.addContainer(container);
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "", e.getCause());
        }
        return containerGroup;
    }

    private Item createItem(XmlPullParser parser) {
        String name = parser.getAttributeValue(null,TAG_NAME);
        String value = parser.getAttributeValue(null,TAG_VALUE);
        String type = parser.getAttributeValue(null,TAG_TYPE);
        return new Item(name, value, type);
    }

    private Container createContainer(XmlPullParser parser) {
        Container container = new Container();
        container.setName(parser.getAttributeValue(null,TAG_NAME));
        return container;
    }

    private ContainerGroup createContainerGroup(XmlPullParser parser) {
        return new ContainerGroup();
    }
}
