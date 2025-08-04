#/******************************************************************************
# *
# * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
# * All rights reserved.
# * Confidential and Proprietary - Qualcomm Technologies, Inc.
# *
# ******************************************************************************/
#!/bin/bash

mkdir -p aptxacuOverlay/res/values

cat > aptxacuOverlay/Android.mk <<'EOM1'
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_CERTIFICATE := platform
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_PACKAGE_NAME := aptxacuOverlay
LOCAL_SDK_VERSION := current
LOCAL_VENDOR_MODULE := true
include $(BUILD_RRO_PACKAGE)
EOM1


cat > aptxacuOverlay/AndroidManifest.xml <<'EOM2'
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.qualcomm.qtil.aptxacuOverlay">
                <overlay android:targetPackage="com.qualcomm.qtil.aptxacu"
                 android:priority="1" android:isStatic="true"/>
</manifest>
EOM2

cat > aptxacuOverlay/res/values/strings.xml <<'EOM5'
<?xml version="1.0" encoding="utf-8"?>
<!--
/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
-->
<resources>
    <string name="acu_label">Codec preferences</string>
    <string name="ppl_label">Application audio profile preference</string>

    <string name="pref_cat_aptx_and_aptx_hd">Qualcomm&#xae; aptX&#x2122; and Qualcomm&#xae; aptX&#x2122; HD</string>
    <string name="aptx_and_aptx_hd_priority_title">Set Qualcomm&#xae; aptX&#x2122; or Qualcomm&#xae; aptX&#x2122; HD preference when available"</string>
    <string name="aptx_and_aptx_hd_priority_dialog_title">Set Qualcomm&#xae; aptX&#x2122; or Qualcomm&#xae; aptX&#x2122; HD preference"</string>
    <string name="default_label">Default</string>
    <string name="aptx_label">Qualcomm&#xae; aptX&#x2122;</string>
    <string name="aptx_hd_label">Qualcomm&#xae; aptX&#x2122; HD</string>

    <string name="pref_cat_aptx_adaptive">Qualcomm&#xae; aptX&#x2122; Adaptive</string>
    <string name="aptx_adaptive_96khz_sample_rate_title">Enable Qualcomm&#xae; aptX&#x2122; Adaptive 96KHz sample rate support for high resolution audio when available"</string>
    <string name="aptx_adaptive_96khz_sample_rate_dialog_title">Enable Qualcomm&#xae; aptX&#x2122; Adaptive 96KHz sample rate support</string>
    <string name="aptx_adaptive_96khz_sample_rate_on_label">On</string>
    <string name="aptx_adaptive_96khz_sample_rate_off_label">Off</string>

    <string name="pref_cat_codec_audio_profile">Codec audio profile preference</string>
    <string name="audio_profile_override_title">Set audio profile preference</string>
    <string name="audio_profile_override_dialog_title">Set audio profile preference</string>
    <string name="audio_profile_override_auto_adjust_label">Auto-adjust</string>
    <string name="audio_profile_override_high_quality_label">High Quality</string>
    <string name="audio_profile_override_gaming_mode_label">Gaming Mode</string>

    <string name="audio_profile_preference_title">Set application audio profile preference</string>
    <string name="audio_profile_preference_summary">Configure audio profile preference for installed applications. i.e. Auto-adjust, High Quality, Gaming Mode</string>
    <string name="spinner_auto_adjust_label">Auto-adjust</string>
    <string name="spinner_high_quality_label">High Quality</string>
    <string name="spinner_gaming_mode_label">Gaming Mode</string>
</resources>
EOM5


cat > aptxacuOverlay/install.sh <<'EOM6'

if [ ! -d $ANDROID_PRODUCT_OUT ]; then
    echo "Missing Android build env. Exiting"
    exit
fi

RROAPPSRC="/vendor/overlay/aptxacuOverlay.apk"
RROAPPDST="/vendor/overlay/aptxacuOverlay/aptxacuOverlay.apk"
if [ ! -f $ANDROID_PRODUCT_OUT/$RROAPPSRC ]; then
    echo "File $ANDROID_PRODUCT_OUT/$RROAPPSRC doesn't exist. Build using 'mm'."
    echo "Exiting without installing"
    exit
fi

adb wait-for-device root
adb remount
adb push $ANDROID_PRODUCT_OUT/$RROAPPSRC $RROAPPDST
adb shell sync
EOM6

chmod +x aptxacuOverlay/install.sh
