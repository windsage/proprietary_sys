#FASTMMI
MMI_DBG := libminui
MMI_DBG += libmmi
MMI_DBG += mmi
MMI_DBG += mmi_agent
MMI_DBG += mmi_agent32
MMI_DBG += mmi_agent64
MMI_DBG += mmi_audio
MMI_DBG += mmi_battery
MMI_DBG += mmi_bluetooth
MMI_DBG += mmi_camera
MMI_DBG += mmi_cpu
MMI_DBG += mmi_debug
MMI_DBG += mmi_diag
MMI_DBG += mmi_exmaple
MMI_DBG += mmi_flashlight
MMI_DBG += mmi_fm
MMI_DBG += mmi_gps
MMI_DBG += mmi_headset
MMI_DBG += mmi_key
MMI_DBG += mmi_lcd
MMI_DBG += mmi_light
MMI_DBG += mmi_memory
MMI_DBG += mmi_nfc
MMI_DBG += mmi_power
MMI_DBG += mmi_sensor
MMI_DBG += mmi_sim
MMI_DBG += mmi_storage
MMI_DBG += mmi_sysinfo
MMI_DBG += mmi_touch
MMI_DBG += mmi_vibrator
MMI_DBG += mmi_wifi
MMI_DBG += mmi_telephone
MMI_DBG += mmi.xml

#FASTMMI resource files
MMI_RES_DBG := fail.png
MMI_RES_DBG += fonts.ttf
MMI_RES_DBG += footer.xml
MMI_RES_DBG += footer_fail.xml
MMI_RES_DBG += header.xml
MMI_RES_DBG += layout_battery.xml
MMI_RES_DBG += layout_bluetooth.xml
MMI_RES_DBG += layout_button_backlight.xml
MMI_RES_DBG += layout_camera_back.xml
MMI_RES_DBG += layout_camera_front.xml
MMI_RES_DBG += layout_cb.xml
MMI_RES_DBG += layout_common.xml
MMI_RES_DBG += layout_confirm.xml
MMI_RES_DBG += layout_cpu.xml
MMI_RES_DBG += layout_emmc.xml
MMI_RES_DBG += layout_feedback.xml
MMI_RES_DBG += layout_flashlight.xml
MMI_RES_DBG += layout_fm.xml
MMI_RES_DBG += layout_gps.xml
MMI_RES_DBG += layout_gsensor.xml
MMI_RES_DBG += layout_gyroscope.xml
MMI_RES_DBG += layout_handset.xml
MMI_RES_DBG += layout_headset.xml
MMI_RES_DBG += layout_headset_key.xml
MMI_RES_DBG += layout_indicator.xml
MMI_RES_DBG += layout_key.xml
MMI_RES_DBG += layout_keypad.xml
MMI_RES_DBG += layout_lcd.xml
MMI_RES_DBG += layout_lcd_backlight.xml
MMI_RES_DBG += layout_led_blue.xml
MMI_RES_DBG += layout_led_green.xml
MMI_RES_DBG += layout_led_red.xml
MMI_RES_DBG += layout_loudspeaker.xml
MMI_RES_DBG += layout_lsensor.xml
MMI_RES_DBG += layout_hsensor.xml
MMI_RES_DBG += layout_memory.xml
MMI_RES_DBG += layout_msensor.xml
MMI_RES_DBG += layout_nfc.xml
MMI_RES_DBG += layout_pcba.xml
MMI_RES_DBG += layout_power.xml
MMI_RES_DBG += layout_primary_mic.xml
MMI_RES_DBG += layout_psensor.xml
MMI_RES_DBG += layout_reboot.xml
MMI_RES_DBG += layout_report.xml
MMI_RES_DBG += layout_sdcard.xml
MMI_RES_DBG += layout_simcard1.xml
MMI_RES_DBG += layout_simcard2.xml
MMI_RES_DBG += layout_system_info.xml
MMI_RES_DBG += layout_touch.xml
MMI_RES_DBG += layout_vibrator.xml
MMI_RES_DBG += layout_wifi.xml
MMI_RES_DBG += layout_telephone.xml
MMI_RES_DBG += main.xml
MMI_RES_DBG += main_wear.xml
MMI_RES_DBG += path_config.xml
MMI_RES_DBG += pagedown.png
MMI_RES_DBG += pageup.png
MMI_RES_DBG += pass.png
MMI_RES_DBG += poweroff.png
MMI_RES_DBG += qualsound.wav
MMI_RES_DBG += reboot.png
MMI_RES_DBG += report.png
MMI_RES_DBG += reset.png
MMI_RES_DBG += runall.png
MMI_RES_DBG += strings.xml
MMI_RES_DBG += strings-zh-rCN.xml

#FASTMMI dependency applications
MMI_DEPS_DBG := bdt
MMI_DEPS_DBG += fmfactorytest
MMI_DEPS_DBG += fmfactorytestserver
MMI_DEPS_DBG += ftm_test_config
MMI_DEPS_DBG += garden_app
MMI_DEPS_DBG += iwlist
MMI_DEPS_DBG += iw
MMI_DEPS_DBG += mm-audio-ftm
MMI_DEPS_DBG += qmi_simple_ril_test

#QMMI JNI Library
QMMI_JNI_LIB_DBG := libmmi_jni

ifneq ($(TARGET_BOARD_AUTO),true)
#QMMI APK
QMMI_APK_DBG := Qmmi
endif

PRODUCT_PACKAGES_DEBUG  += $(MMI_DBG)
PRODUCT_PACKAGES_DEBUG  += $(MMI_RES_DBG)

#remove this part product packages, should control by tech team.
#PRODUCT_PACKAGES_DEBUG  += $(MMI_DEPS_DBG)

PRODUCT_PACKAGES_DEBUG  += $(QMMI_APK_DBG)
PRODUCT_PACKAGES_DEBUG  += $(QMMI_JNI_LIB_DBG)

