/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.sva.xmlhelper;

import java.util.HashMap;
import java.util.Map.Entry;

public class ContainerGroup {

    private HashMap<String, Container> mContainers = new HashMap<>();

    public ContainerGroup() {
    }

    public Container getContainer(String containerTag) {
        return mContainers.get(containerTag);
    }

    public void addContainer(Container container) {
        mContainers.put(container.getName(), container);
    }

    @Override
    public String toString() {
        StringBuilder containerGroup = new StringBuilder();
        containerGroup.append("ContainerGroup = " + "{\n");
        for (Entry<String, Container> entry : mContainers.entrySet()) {
            containerGroup.append(entry.getValue().toString()).append("\n");
        }
        return containerGroup.append("}").toString();
    }
}
