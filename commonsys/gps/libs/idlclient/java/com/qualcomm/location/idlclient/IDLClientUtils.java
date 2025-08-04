/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2020-2021 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package com.qualcomm.location.idlclient;

import android.location.Location;
import android.location.Address;
import android.util.Log;
import vendor.qti.gnss.LocAidlLocation;
import vendor.qti.gnss.LocAidlLocationFlagsBits;
import vendor.qti.gnss.LocAidlAddress;
import java.lang.Throwable;

public class IDLClientUtils {

    public static final int ULP_LOCATION_IS_FROM_HYBRID = 0x0001;
    public static final int ULP_LOCATION_IS_FROM_GNSS   = 0x0002;

    public static Location translateAidlLocation(LocAidlLocation aidlLocation) {
        Location location = new Location("");

        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.LAT_LONG_BIT) != 0) {
            location.setLatitude(aidlLocation.latitude);
            location.setLongitude(aidlLocation.longitude);
            location.setTime(aidlLocation.timestamp);
        }
        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.ALTITUDE_BIT) != 0) {
            location.setAltitude(aidlLocation.altitude);
        }
        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.SPEED_BIT) != 0) {
            location.setSpeed(aidlLocation.speed);
        }
        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.BEARING_BIT) != 0) {
            location.setBearing(aidlLocation.bearing);
        }
        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.ACCURACY_BIT) != 0) {
            location.setAccuracy(aidlLocation.accuracy);
        }
        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.VERTICAL_ACCURACY_BIT)
                != 0) {
            location.setVerticalAccuracyMeters(aidlLocation.verticalAccuracy);
        }
        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.SPEED_ACCURACY_BIT) != 0) {
            location.setSpeedAccuracyMetersPerSecond(aidlLocation.speedAccuracy);
        }
        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.BEARING_ACCURACY_BIT) != 0) {
            location.setBearingAccuracyDegrees(aidlLocation.bearingAccuracy);
        }

        if ((aidlLocation.locationFlagsMask & LocAidlLocationFlagsBits.ELAPSED_REAL_TIME_BIT)
                != 0) {
            location.setElapsedRealtimeNanos(aidlLocation.elapsedRealTime);
            location.setElapsedRealtimeUncertaintyNanos(aidlLocation.elapsedRealTimeUnc);
        }
        location.makeComplete();
        return location;
    }

    public static LocAidlLocation convertAfwLocationToLocLocation(Location loc) {
        LocAidlLocation aidlLoc = new LocAidlLocation();
        int flagsMask = 0;
        aidlLoc.latitude = loc.getLatitude();
        aidlLoc.longitude = loc.getLongitude();
        aidlLoc.timestamp = loc.getTime();
        flagsMask |= LocAidlLocationFlagsBits.LAT_LONG_BIT;
        if (loc.hasAltitude()) {
            aidlLoc.altitude = loc.getAltitude();
            flagsMask |= LocAidlLocationFlagsBits.ALTITUDE_BIT;
        }
        if (loc.hasSpeed()) {
            aidlLoc.speed = loc.getSpeed();
            flagsMask |= LocAidlLocationFlagsBits.SPEED_BIT;
        }
        if (loc.hasBearing()) {
            aidlLoc.bearing = loc.getBearing();
            flagsMask |= LocAidlLocationFlagsBits.BEARING_BIT;
        }
        if (loc.hasAccuracy()) {
            aidlLoc.accuracy = loc.getAccuracy();
            flagsMask |= LocAidlLocationFlagsBits.ACCURACY_BIT;
        }
        if (loc.hasVerticalAccuracy()) {
            aidlLoc.verticalAccuracy = loc.getVerticalAccuracyMeters();
            flagsMask |= LocAidlLocationFlagsBits.VERTICAL_ACCURACY_BIT;
        }
        if (loc.hasSpeedAccuracy()) {
            aidlLoc.speedAccuracy = loc.getSpeedAccuracyMetersPerSecond();
            flagsMask |= LocAidlLocationFlagsBits.SPEED_ACCURACY_BIT;
        }
        if (loc.hasBearingAccuracy()) {
            aidlLoc.bearingAccuracy = loc.getBearingAccuracyDegrees();
            flagsMask |= LocAidlLocationFlagsBits.BEARING_ACCURACY_BIT;
        }
        if (loc.hasElapsedRealtimeUncertaintyNanos()) {
            aidlLoc.elapsedRealTimeUnc = (long)loc.getElapsedRealtimeUncertaintyNanos();
            aidlLoc.elapsedRealTime = loc.getElapsedRealtimeNanos();
            //AfwLocation's elapsedRealTimeUnc mask mirrors to LocAidlLocation's elapsedRealTime bit
            flagsMask |= LocAidlLocationFlagsBits.ELAPSED_REAL_TIME_BIT;
        }
        aidlLoc.locationFlagsMask = flagsMask;
        return aidlLoc;
    }

    public static LocAidlAddress convertAfwAddrToAidlAddr(Address addr) {
        LocAidlAddress aidlAddr = new LocAidlAddress();
        aidlAddr.adminArea = addr.getAdminArea() != null ? addr.getAdminArea() : new String();
        aidlAddr.countryCode = addr.getCountryCode() != null ? addr.getCountryCode() : new String();
        aidlAddr.countryName = addr.getCountryName() != null ? addr.getCountryName() : new String();
        aidlAddr.featureName = addr.getFeatureName() != null ? addr.getFeatureName() : new String();
        if (addr.hasLatitude()) {
            aidlAddr.hasLatitude = true;
            aidlAddr.latitude = addr.getLatitude();
        }
        if (addr.hasLongitude()) {
            aidlAddr.hasLongitude = true;
            aidlAddr.longitude = addr.getLongitude();
        }
        aidlAddr.locality = addr.getLocality() != null ? addr.getLocality() : new String();
        aidlAddr.locale = new String();
        aidlAddr.phone = addr.getPhone() != null ? addr.getPhone() : new String();
        aidlAddr.postalCode = addr.getPostalCode() != null ? addr.getPostalCode() : new String();
        aidlAddr.premises = addr.getPremises() != null ? addr.getPremises() : new String();
        aidlAddr.subAdminArea =
                addr.getSubAdminArea() != null ? addr.getSubAdminArea() : new String();
        aidlAddr.subLocality = addr.getSubLocality() != null ? addr.getSubLocality() : new String();
        aidlAddr.thoroughfare =
                addr.getThoroughfare() != null ? addr.getThoroughfare() : new String();
        aidlAddr.subThoroughfare =
                addr.getSubThoroughfare() != null ? addr.getSubThoroughfare() : new String();
        aidlAddr.url = addr.getUrl() != null ? addr.getUrl() : new String();
        return aidlAddr;
    }

    private static void printIDLInfo(String tag, String extraStr) {
        String nameofCurrMethod = Thread.currentThread().getStackTrace()[4].getMethodName();
        int lineNum = Thread.currentThread().getStackTrace()[4].getLineNumber();
        Log.d(tag, "[" + nameofCurrMethod + "][" + lineNum + "] " +  extraStr);
    }

    public static void toIDLService(String tag) {
        printIDLInfo(tag, "[HC] =>> [HS]");
    }

    public static void fromIDLService(String tag) {
        printIDLInfo(tag, "[HC] <<= [HS]");
    }

    public static long hexMacToLong(String mac_hex) {
        String hex_mac_lo = mac_hex.substring(0, 6);
        String hex_mac_hi = mac_hex.substring(6);

        long mac = Long.parseLong(hex_mac_lo, 16);
        mac <<= 24;
        mac |= Long.parseLong(hex_mac_hi, 16);
        return mac;
    }

    public static String longMacToHex(long mac_long) {
        // If mac value goes beyond 6 bytes
        if ((mac_long >> 48) > 0) {
            return String.format("%016X", mac_long);
        } else {
            return String.format("%012X", mac_long);
        }
    }
}
