/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import java.util.UUID;
import android.bluetooth.BluetoothGattCharacteristic;

/*
 * XPAN Service UUID's
 *
 */

public class XpanUuid {

    static final UUID XPAN_SERVICE =
            UUID.fromString("13762fc5-320d-43bd-b883-481b13ce79b6");
    static final UUID STORED_SSIDS =
            UUID.fromString("8b6850ef-0790-4157-b5a7-3be5d672dd62");
    static final UUID IPV4_ADDRESS =
            UUID.fromString("0fa804c8-6e8f-4ca4-91cc-12a4ae15422f");
    static final UUID IPV6_ADDRESS =
            UUID.fromString("4c279ce1-a56e-4506-b45e-f4fc73267940");
    static final UUID L2CAP_TCP_PORT =
            UUID.fromString("5be21028-54a4-43db-bbc3-fcd88b813dee");
    static final UUID UDP_PORT =
            UUID.fromString("f96cf4bd-d9a9-4003-8a3a-ba4ff2a8489b");
    static final UUID MDNS_SERVICE_UUID =
            UUID.fromString("1da45885-6da9-49ff-b5e8-0817c37166c6");
    static final UUID TWT_CONFIGURATION =
            UUID.fromString("071f16ba-bf39-4b47-b369-03d3e3ea03ab");
    static final UUID CONNECTED_DEVICES =
            UUID.fromString("0020aeb0-8c9f-4e8c-9903-381025d8ea30");
    static final UUID CLEAR_TO_SEND =
            UUID.fromString("466a7804-44fd-4c4b-88c4-bf68da39b8e4");
    static final UUID XPAN_CONTROL_POINT =
            UUID.fromString("e0242199-455f-4955-9429-0d5171ee42f2");
    static final UUID BEARER_PREFERENCE =
            UUID.fromString("c6b94a97-6b6e-40eb-99d2-7521da588cdc");
    static final UUID MAC_ADDRESS =
            UUID.fromString("48207e6f-5166-4183-b60c-0e45138b797b");
    static final UUID REQ_SAP_POWER_STATE =
            UUID.fromString("61bcf3cd-cd5f-440f-8e34-29f46da86819");
    static final UUID SERVER_FEATURES = UUID
            .fromString("2ee884a8-75da-422e-8372-e789df4d26ac");
    static final UUID AUDIO_BEARER_SWITCH_RESPONSE =
            UUID.fromString("c3a62c81-2ea8-4555-8177-ce1e73fc9adc");
    static final UUID NUM_DEVICES_PRESENT =
            UUID.fromString("8b78c9b4-9bfe-438b-960b-867a3e3a755f");
    static final UUID VOICE_BACK_CHANNEL_PERIODICITY =
            UUID.fromString("430e1083-dd35-4506-9987-99285dac3118");
    static final UUID REQ_WIFI_SCAN_RESULTS =
            UUID.fromString("a06fd924-4f5a-46ce-98e1-5eec0d161eae");
    static final UUID ROAMING_REQUEST_RESPONSE =
            UUID.fromString("b0eefd27-42d6-4d46-9ab2-4968b3ac7a85");
    static final UUID WIFI_CHANNEL_SWITCH_REQUEST =
            UUID.fromString("d448b200-58b8-4345-9094-b51885b87980");
    static final UUID TWT_STATUS =
            UUID.fromString("35bb44a9-7195-4d3b-9968-1f3306cad3d0");
    static final UUID SAP_CONN_STATUS =
            UUID.fromString("01aae1a6-952d-40f7-84b3-79f378c232aa");

    // Descriptor UUID for enabling characteristic changed notifications
    static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString(
            "00002902-0000-1000-8000-00805f9b34fb");

    static String uuidToName(BluetoothGattCharacteristic ch) {
        UUID uuid = ch.getUuid();
        String name ;
        if (uuid.equals(XPAN_SERVICE)) {
            name = "XPAN_SERVICE";
        } else if (uuid.equals(STORED_SSIDS)) {
            name = "STORED_SSIDS";
        } else if (uuid.equals(IPV4_ADDRESS)) {
            name = "IPV4_ADDRESS";
        } else if (uuid.equals(IPV6_ADDRESS)) {
            name = "IPV6_ADDRESS";
        } else if (uuid.equals(L2CAP_TCP_PORT)) {
            name = "L2CAP_TCP_PORT";
        } else if (uuid.equals(UDP_PORT)) {
            name = "UDP_PORT";
        } else if (uuid.equals(MDNS_SERVICE_UUID)) {
            name = "MDNS_SERVICE_UUID";
        } else if (uuid.equals(TWT_CONFIGURATION)) {
            name = "TWT_CONFIGURATION";
        } else if (uuid.equals(CONNECTED_DEVICES)) {
            name = "CONNECTED_DEVICES";
        } else if (uuid.equals(CLEAR_TO_SEND)) {
            name = "CLEAR_TO_SEND";
        } else if (uuid.equals(XPAN_CONTROL_POINT)) {
            byte val[] = ch.getValue();
            if (val == null || val.length == 0) {
                name = "XPAN_CONTROL_POINT";
            } else {
                name = XpanConstants.getStrfromOpCode(val[0]);
            }
        } else if (uuid.equals(BEARER_PREFERENCE)) {
            name = "BEARER_PREFERENCE";
        } else if (uuid.equals(MAC_ADDRESS)) {
            name = "MAC_ADDRESS";
        } else if (uuid.equals(REQ_SAP_POWER_STATE)) {
            name = "REQ_SAP_POWER_STATE";
        } else if (uuid.equals(SERVER_FEATURES)) {
            name = "SERVER_FEATURES";
        } else if (uuid.equals(AUDIO_BEARER_SWITCH_RESPONSE)) {
            name = "AUDIO_BEARER_SWITCH_RESPONSE";
        } else if (uuid.equals(NUM_DEVICES_PRESENT)) {
            name = "NUM_DEVICES_PRESENT";
        } else if (uuid.equals(CLIENT_CHARACTERISTIC_CONFIG)) {
            name = "CLIENT_CHARACTERISTIC_CONFIG";
        } else if (uuid.equals(VOICE_BACK_CHANNEL_PERIODICITY)) {
            name = "VOICE_BACK_CHANNEL_PERIODICITY";
        } else if (uuid.equals(REQ_WIFI_SCAN_RESULTS)) {
            name = "REQ_WIFI_SCAN_RESULTS";
        }  else if (uuid.equals(WIFI_CHANNEL_SWITCH_REQUEST)) {
            name = "WIFI_CHANNEL_SWITCH_REQUEST";
        }  else if (uuid.equals(TWT_STATUS)) {
            name = "TWT_STATUS";
        }  else if (uuid.equals(SAP_CONN_STATUS)) {
            name = "SAP_CONN_STATUS";
        } else if (uuid.equals(CLIENT_CHARACTERISTIC_CONFIG)) {
            name = "CLIENT_CHARACTERISTIC_CONFIG";
        } else {
            name =uuid.toString();
        }
        return name;
    }
}
