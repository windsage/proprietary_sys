# This file lists all qti products and defines the QC_PROP flag which
# is used to enable projects inside $(QC_PROP_ROOT) directory.

# Also, This file intended for use by device/product makefiles
# to pick and choose the optional proprietary modules

# Root of QTI Proprietary component tree

BUILD_SDM845_PUREAOSP := false

ifeq ($(filter $(TARGET_BOARD_PLATFORM), sdm845 sdm710),$(TARGET_BOARD_PLATFORM))
ifeq ($(TARGET_USES_AOSP),true)
BUILD_SDM845_PUREAOSP := true
endif
endif

ifeq ($(filter $(TARGET_BOARD_PLATFORM), qssi_wear qssi_64 qssi kalama taro lahaina holi bengal neo parrot anorak pineapple hala blair crow niobe sun volcano canoe),$(TARGET_BOARD_PLATFORM))
ENABLE_INTEGER_OVERFLOW := true
ENABLE_BOUND_SANITIZER := true
endif

ifeq ($(filter $(TARGET_BOARD_PLATFORM), qssi_wear qssi_64 qssi kalama taro kona lito bengal lahaina holi neo parrot anorak pineapple hala blair crow niobe sun volcano canoe),$(TARGET_BOARD_PLATFORM))
ifeq ($(filter hwaddress,$(SANITIZE_TARGET)),)
ENABLE_CFI := true
$(warning  ENABLE_CFI : ${ENABLE_CFI})
endif
$(call inherit-product, vendor/qcom/proprietary/common/config/sanitizer.mk)
endif

# Account for 32 bit target and ensure hwasan is false by default.
BUILD_HWASAN := false

ifneq ($(filter hwaddress,$(SANITIZE_TARGET)),)
    BUILD_HWASAN := true
    $(warning  BUILD_HWASAN : ${BUILD_HWASAN})
    $(call inherit-product, vendor/qcom/proprietary/common/config/sanitizer.mk)
endif

ifeq ($(filter $(TARGET_BOARD_PLATFORM), msm8996 msm8998 sdm845 msmnile kona),$(TARGET_BOARD_PLATFORM))
BUILD_FUSION_HANDLERS := true
endif

ifeq  ($(BUILD_SDM845_PUREAOSP), true)
$(call inherit-product, vendor/qcom/proprietary/common/config/device-vendor-SDM845-pureAOSP.mk)
else

ifeq ($(TARGET_USES_QMAA),true)
 TARGET_USES_QMAA_HAL := true
 TARGET_USES_REAL_HAL := false
else
 TARGET_USES_QMAA_HAL := false
 TARGET_USES_REAL_HAL := true
endif

QC_PROP_ROOT := vendor/qcom/proprietary
SDCLANG_LTO_DEFS := device/qcom/common/sdllvm-lto-defs.mk

PRODUCT_LIST := msm7625_surf
PRODUCT_LIST += msm7625_ffa
PRODUCT_LIST += msm7627_6x
PRODUCT_LIST += msm7627_ffa
PRODUCT_LIST += msm7627_surf
PRODUCT_LIST += msm7627a
PRODUCT_LIST += msm8625
PRODUCT_LIST += msm7630_surf
PRODUCT_LIST += msm7630_1x
PRODUCT_LIST += msm7630_fusion
PRODUCT_LIST += msm8660_surf
PRODUCT_LIST += qsd8250_surf
PRODUCT_LIST += qsd8250_ffa
PRODUCT_LIST += qsd8650a_st1x
PRODUCT_LIST += msm8660_csfb
PRODUCT_LIST += msm8960
PRODUCT_LIST += msm8974
PRODUCT_LIST += mpq8064
PRODUCT_LIST += msm8610
PRODUCT_LIST += msm8226
PRODUCT_LIST += apq8084
PRODUCT_LIST += mpq8092
PRODUCT_LIST += msm8916_32
PRODUCT_LIST += msm8916_64
PRODUCT_LIST += msm8916_32_512
PRODUCT_LIST += msm8916_32_k64
PRODUCT_LIST += msm8916_64
PRODUCT_LIST += msm8994
PRODUCT_LIST += msm8996
PRODUCT_LIST += msm8996_gvmq
PRODUCT_LIST += msm8909
PRODUCT_LIST += msm8909go
PRODUCT_LIST += msm8909_512
PRODUCT_LIST += msm8992
PRODUCT_LIST += msm8952_64
PRODUCT_LIST += msm8952_32
PRODUCT_LIST += msm8937_32
PRODUCT_LIST += msm8937_32go
PRODUCT_LIST += msm8937_64
PRODUCT_LIST += msm8953_32
PRODUCT_LIST += msm8953_64
PRODUCT_LIST += msm8998
PRODUCT_LIST += msm8998_32
PRODUCT_LIST += sdm660_64
PRODUCT_LIST += sdm660_32
PRODUCT_LIST += sdm845
PRODUCT_LIST += apq8098_latv
PRODUCT_LIST += sdm710
PRODUCT_LIST += msmnile
PRODUCT_LIST += msmnile_au
PRODUCT_LIST += msmnile_gvmq
PRODUCT_LIST += qcs605
PRODUCT_LIST += qssi
PRODUCT_LIST += qssi_32
PRODUCT_LIST += qssi_32go
PRODUCT_LIST += qssi_64
PRODUCT_LIST += qssi_wear
PRODUCT_LIST += $(MSMSTEPPE)
PRODUCT_LIST += $(TRINKET)
PRODUCT_LIST += $(MSMSTEPPE)_au
PRODUCT_LIST += kona
PRODUCT_LIST += atoll
PRODUCT_LIST += lito
PRODUCT_LIST += bengal
PRODUCT_LIST += bengal_32
PRODUCT_LIST += bengal_32go
PRODUCT_LIST += lahaina
PRODUCT_LIST += holi
PRODUCT_LIST += taro
PRODUCT_LIST += kalama
PRODUCT_LIST += kalama64
PRODUCT_LIST += pineapple
PRODUCT_LIST += hala
PRODUCT_LIST += bengal_515
PRODUCT_LIST += anorak
PRODUCT_LIST += crow
PRODUCT_LIST += blair
PRODUCT_LIST += sun
PRODUCT_LIST += niobe
PRODUCT_LIST += parrot66
PRODUCT_LIST += volcano
PRODUCT_LIST += canoe

MSM7K_PRODUCT_LIST := msm7625_surf
MSM7K_PRODUCT_LIST += msm7625_ffa
MSM7K_PRODUCT_LIST += msm7627_6x
MSM7K_PRODUCT_LIST += msm7627_ffa
MSM7K_PRODUCT_LIST += msm7627_surf
MSM7K_PRODUCT_LIST += msm7627a
MSM7K_PRODUCT_LIST += msm7630_surf
MSM7K_PRODUCT_LIST += msm7630_1x
MSM7K_PRODUCT_LIST += msm7630_fusion

FOTA_PRODUCT_LIST := msm7627a

ifneq ($(strip $(TARGET_VENDOR)),)
  PRODUCT_LIST += $(TARGET_BOARD_PLATFORM)
endif

# Use TARGET_BASE_PRODUCT variable to include base product's rules in a
# derived product.
# e.g. If your product "xyz_abc" is derived from base product "xyz" but
# you still need to include base product's Boardconfig.mk file then define
# value of TARGET_BASE_PRODUCT in <device>/<vendor>/<target> or export
# it as "xyz".
ifneq ($(TARGET_BASE_PRODUCT),)
  target_base_product:=$(TARGET_BASE_PRODUCT)
else
  target_base_product:=$(TARGET_BOARD_PLATFORM)$(TARGET_BOARD_SUFFIX)
endif

ifneq (, $(filter $(PRODUCT_LIST), $(target_base_product)))

  ifneq ($(strip $(TARGET_VENDOR)),)
    include device/$(TARGET_VENDOR)/$(target_base_product)/BoardConfig.mk
  else
    include device/qcom/$(target_base_product)/BoardConfig.mk
  endif

  ifeq ($(call is-board-platform,msm8660),true)
    PREBUILT_BOARD_PLATFORM_DIR := msm8660_surf
  else ifeq ($(target_base_product),msm8625)
    PREBUILT_BOARD_PLATFORM_DIR := msm8625
  else ifeq ($(target_base_product),mpq8064)
    PREBUILT_BOARD_PLATFORM_DIR := mpq8064
  else ifneq ($(strip $(TARGET_BOARD_SUFFIX)),)
    PREBUILT_BOARD_PLATFORM_DIR := $(TARGET_BOARD_PLATFORM)$(TARGET_BOARD_SUFFIX)
  else
    PREBUILT_BOARD_PLATFORM_DIR := $(TARGET_BOARD_PLATFORM)
  endif

  ifneq ($(TARGET_NO_GMS_PACKAGES), true)
    $(call inherit-product-if-exists, $(QC_PROP_ROOT)/keystone-config/config.mk)
  endif
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/common-noship/etc/device-vendor-qssi-noship.mk)
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/prebuilt_grease/target/product/$(PREBUILT_BOARD_PLATFORM_DIR)/prebuilt.mk)
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/prebuilt_HY11/target/product/$(PREBUILT_BOARD_PLATFORM_DIR)/prebuilt.mk)
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/prebuilt_HY11-HWASAN/target/product/$(PREBUILT_BOARD_PLATFORM_DIR)/prebuilt.mk)
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/prebuilt_HY22/target/product/$(PREBUILT_BOARD_PLATFORM_DIR)/prebuilt.mk)
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/prebuilt_HY11/target/common/prebuilt.mk)
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/prebuilt_HY11-HWASAN/target/common/prebuilt.mk)
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/prebuilt_HY22/target/common/prebuilt.mk)
  $(call inherit-product-if-exists, $(QC_PROP_ROOT)/prebuilt_grease/target/common/prebuilt.mk)


  ifeq ($(BUILD_TINY_ANDROID),true)
    VENDOR_TINY_ANDROID_PACKAGES := $(QC_PROP_ROOT)/diag
    VENDOR_TINY_ANDROID_PACKAGES += $(QC_PROP_ROOT)/kernel-tests
    VENDOR_TINY_ANDROID_PACKAGES += $(QC_PROP_ROOT)/modem-apis
    VENDOR_TINY_ANDROID_PACKAGES += $(QC_PROP_ROOT)/oncrpc
    VENDOR_TINY_ANDROID_PACKAGES += $(QC_PROP_ROOT)/common/build/remote_api_makefiles
    VENDOR_TINY_ANDROID_PACKAGES += $(QC_PROP_ROOT)/common/build/fusion_api_makefiles
    VENDOR_TINY_ANDROID_PACKAGES += $(QC_PROP_ROOT)/common/config

    ifeq ($(call is-board-platform-in-list,$(MSM7K_PRODUCT_LIST)),true)
      VENDOR_TINY_ANDROID_PACKAGES += $(QC_PROP_ROOT)/gps
      VENDOR_TINY_ANDROID_PACKAGES += hardware
      VENDOR_TINY_ANDROID_PACKAGES += external/wpa_supplicant
    endif

  endif # BUILD_TINY_ANDROID
endif
#Do not use target_base_product variable now
target_base_product:=

ifeq ($(call is-board-platform-in-list,$(FOTA_PRODUCT_LIST)),true)
  TARGET_FOTA_UPDATE_LIB := libipth libipthlzmadummy
  TARGET_HAS_FOTA        := true
endif

#Include below QRD extensions,will real control compiling in Android.mk under qrdplus
ifeq ($(strip $(TARGET_USES_QTIC_CMCC)),true)
# Include the QRD customer extensions for ChinaMobile system and vendor part.
$(call inherit-product-if-exists, vendor/qcom/proprietary/commonsys/qrdplus/China/ChinaMobile/products.mk)
$(call inherit-product-if-exists, vendor/qcom/proprietary/qrdplus/China/ChinaMobile/products.mk)
endif

ifeq ($(strip $(TARGET_USES_QTIC_CT)),true)
# Include the QRD customer extensions for ChinaTelecom.
$(call inherit-product-if-exists, vendor/qcom/proprietary/qrdplus/China/ChinaTelecom/products.mk)
endif

ifeq ($(strip $(TARGET_USES_QTIC_CU)),true)
# Include the QRD customer extensions for ChinaUnicom.
$(call inherit-product-if-exists, vendor/qcom/proprietary/qrdplus/China/ChinaUnicom/products.mk)
endif

ifeq ($(strip $(TARGET_USES_QTIC_CTA)),true)
# Include the QRD customer extensions for CTA
$(call inherit-product-if-exists, vendor/qcom/proprietary/qrdplus/China/CTA/products.mk)
endif

ifeq ($(strip $(TARGET_USES_QTIC_EXTENSION)),true)
#include the QRD extensions for Extension system and vendor part.
$(call inherit-product-if-exists, vendor/qcom/proprietary/commonsys/qrdplus/Extension/products.mk)
$(call inherit-product-if-exists, vendor/qcom/proprietary/qrdplus/Extension/products.mk)
endif

ifeq ($(strip $(TARGET_USES_QTIC_SVA)),true)
#include the SVA system part
$(call inherit-product-if-exists, vendor/qcom/proprietary/commonsys/qrdplus/sva/products.mk)
endif

ifeq ($(strip $(TARGET_USES_QTIC_VOICEUI)),true)
#include the VoiceUI system part
$(call inherit-product-if-exists, vendor/qcom/proprietary/commonsys/voiceui/products.mk)
endif

ifeq ($(strip $(TARGET_USES_RRO)),true)
#Add Runtime Resource Overlays to build
$(call inherit-product-if-exists, $(QC_PROP_ROOT)/commonsys/resource-overlay/overlay.mk)
endif

ifeq ($(strip $(TARGET_USES_RRO)),true)
#Add Runtime Resource Overlays to build
$(call inherit-product-if-exists, $(QC_PROP_ROOT)/resource-overlay/overlay.mk)
endif

#Add logkit module to build
TARGET_USES_LOGKIT_LOGGING := true

#Include other rules if any
$(call inherit-product-if-exists, $(QC_PROP_ROOT)/common-noship/build/generate_extra_images_prop.mk)


#prebuilt javalib
#ifneq ($(wildcard $(QC_PROP_ROOT)/common/build/prebuilt_javalib.mk),)
#BUILD_PREBUILT_JAVALIB := $(QC_PROP_ROOT)/common/build/prebuilt_javalib.mk
#else
#BUILD_PREBUILT_JAVALIB := $(BUILD_PREBUILT)
#endif

# Each line here corresponds to an optional LOCAL_MODULE built by
# Android.mk(s) in the proprietary projects. Where project
# corresponds to the vars here in CAPs.

# These modules are tagged with optional as their LOCAL_MODULE_TAGS
# wouldn't be present in your on target images, unless listed here
# explicitly.

#Connection Security
CONNECTION_SECURITY := libminksocket
CONNECTION_SECURITY += ConnectionSecurityService
CONNECTION_SECURITY += SSGTelemetryService
CONNECTION_SECURITY += SSGCredentialService
CONNECTION_SECURITY += ssgqmigd
CONNECTION_SECURITY += ssgtzd
CONNECTION_SECURITY += TrustZoneAccessService

#ADC
ADC := qcci_adc_test
ADC += libqcci_adc

#ALLJOYN
ALLJOYN := liballjoyn

#AIVPLAY
AIVPLAY := libaivdrmclient
AIVPLAY += libAivPlay

#ANT
ANT := com.qualcomm.qti.ant@1.0-impl
ANT += com.qualcomm.qti.ant@1.0

#AntiTheftDemo
ANTITHEFTDEMO := AntiTheftDemo

#Backup Agent
BACKUP_AGENT := QtiBackupAgent

#CameraHawk  App
CAMERAHAWK_APPS := CameraHawk
CAMERAHAWK_APPS += libamui
CAMERAHAWK_APPS += libarcimgfilters
CAMERAHAWK_APPS += libarcimgutils
CAMERAHAWK_APPS += libarcimgutilsbase
CAMERAHAWK_APPS += libarcutils
CAMERAHAWK_APPS += libhdr
CAMERAHAWK_APPS += libphotofix
CAMERAHAWK_APPS += libpluginadaptor
CAMERAHAWK_APPS += libquickview
CAMERAHAWK_APPS += libstory
CAMERAHAWK_APPS += libvideopmk

#Perfect365 App
PERFECT365_APPS := Perfect365
PERFECT365_APPS += libArcSoftFlawlessFace

#Whip Apps
WHIP_APPS := WhipForPhone
WHIP_APPS += libamuiwhip
WHIP_APPS += libarcwhipimgutils
WHIP_APPS += libattextengine
WHIP_APPS += libsns_album
WHIP_APPS += libarcwhipimgutilsbase
WHIP_APPS += libclipmap3

# camerahawk,Whip Common Lib
CAMERAHAWK_WHIP_COMMON_LIB := libarcplatform

#Atmel's mxt cfg file
MXT_CFG := mxt1386e_apq8064_liquid.cfg
MXT_CFG += mxt224_7x27a_ffa.cfg

#BATTERY_CHARGING
BATTERY_CHARGING := battery_charging

#BT
BT := btnvtool
BT += dun-server
BT += hci_qcomm_init
BT += liboi_sbc_decoder
BT += PS_ASIC.pst
BT += RamPatch.txt
BT += sapd
BT += rampatch_tlv.img
BT += nvm_tlv.bin
BT += nvm_tlv_usb.bin
BT += rampatch_tlv_1.3.tlv
BT += nvm_tlv_1.3.bin
BT += rampatch_tlv_2.1.tlv
BT += nvm_tlv_2.1.bin
BT += rampatch_tlv_3.0.tlv
BT += nvm_tlv_3.0.bin
BT += rampatch_tlv_3.2.tlv
BT += nvm_tlv_3.2.bin
BT += wcnss_filter
BT += android.hardware.bluetooth@1.0-service-qti
BT += android.hardware.bluetooth@1.0-impl-qti
BT += android.hardware.bluetooth@1.0-service-qti.rc
BT += com.qualcomm.qti.bluetooth_audio@1.0
BT += btaudio_offload_if
BT += libbluetooth_audio_session
BT += android.hardware.bluetooth.audio@2.0-impl


#CCID
CCID := ccid_daemon

#CHARGER_MONITOR
CHARGER_MONITOR := charger_monitor

#CRASH_LOGGER
CRASH_LOGGER :=  libramdump

#DATA
DATA += CKPD-daemon
DATA += dsdnsutil
DATA += libdsnet
DATA += libdsnetutil
DATA += libdsprofile
DATA += libdss
DATA += libdssock

#FLASH
FLASH := install_flash_player.apk
FLASH += libflashplayer.so
FLASH += libstagefright_froyo.so
FLASH += libstagefright_honeycomb.so
FLASH += libysshared.so
FLASH += oem_install_flash_player.apk

#FM
FM := fmconfig
FM += fmfactorytest
FM += fmfactorytestserver
FM += fm_qsoc_patches
FM += vendor.qti.hardware.fm@1.0-impl

#BTSAR
BTSAR := vendor.qti.hardware.bluetooth_sar@1.0-impl

#FOTA
FOTA := ipth_dua
FOTA += libcrc32.a
FOTA += libdua.a
FOTA += libdme_main
FOTA += libidev_dua.a
FOTA += libipth.a
FOTA += libshared_lib.a
FOTA += libzlib.1.2.1.a
FOTA += MobileUpdateClient.apk

#FTM
FTM := ftmdaemon
FTM += wdsdaemon

#DIAG
DIAG := diag_callback_sample
DIAG += diag_dci_sample
DIAG += diag_klog
DIAG += diag_mdlog
DIAG += diag_socket_log
DIAG += diag_qshrink4_daemon
DIAG += diag_uart_log
DIAG += PktRspTest
DIAG += test_diag
DIAG += libdiag_system
DIAG += libdiagjni

# ENTERPRISE_SECURITY
ENTERPRISE_SECURITY := libescommonmisc
ENTERPRISE_SECURITY += lib_nqs
ENTERPRISE_SECURITY += lib_pfe
ENTERPRISE_SECURITY += libpfecommon
ENTERPRISE_SECURITY += ml_daemon
ENTERPRISE_SECURITY += nqs
ENTERPRISE_SECURITY += pfm
ENTERPRISE_SECURITY += seald
ENTERPRISE_SECURITY += libsealcomm
ENTERPRISE_SECURITY += libsealdsvc
ENTERPRISE_SECURITY += libsealaccess
ENTERPRISE_SECURITY += libsealjni
ENTERPRISE_SECURITY += libprotobuf-cpp-2.3.0-qti-lite
ENTERPRISE_SECURITY += qsb-port
ENTERPRISE_SECURITY += security_boot_check
ENTERPRISE_SECURITY += security-bridge
#ENTERPRISE_SECURITY += tloc_daemon

# FINGERPRINT
ifneq ($(TARGET_BOARD_PLATFORM), hala) # for non hala targets defines
ifneq ($(TARGET_BOARD_AUTO),true)
FINGERPRINT := board2.ini
FINGERPRINT += libqfp-service
FINGERPRINT += qfp-daemon
FINGERPRINT += QFingerprintService
FINGERPRINT += template_imaginary.bin
FINGERPRINT += template_real.bin
FINGERPRINT += vendor.qti.hardware.fingerprint@1.0
FINGERPRINT += vendor.qti.hardware.fingerprint-V1.0-java
FINGERPRINT += android.hardware.biometrics.fingerprint@2.1
FINGERPRINT += qti_fingerprint_interface.xml
endif
endif

#hvdcp 3.0 daemon
HVDCP_OPTI := hvdcp_opti

#GsmaNfcService
ifeq ($(strip $(TARGET_ENABLE_PROPRIETARY_SMARTCARD_SERVICE)),true)
ifeq ($(strip $(TARGET_USES_NQ_NFC)),true)
GSMA_NFC := GsmaNfcService
GSMA_NFC += com.gsma.services.nfc
GSMA_NFC += com.gsma.services.nfc.xml
GSMA_NFC += com.gsma.services.utils
GSMA_NFC += com.gsma.services.utils.xml
endif
endif

#HBTP
HBTP := hbtp_daemon
HBTP += hbtp_tool
HBTP += hbtpcfg.dat
HBTP += hbtpcfg_8917.dat
HBTP += qtc800s_dsp.bin
HBTP += libfastrpc_aue_stub
HBTP += libFastRPC_AUE_Forward_skel
HBTP += libhbtpdsp
HBTP += libhbtpclient
HBTP += libhbtpfrmwk
HBTP += libfastrpc_utf_stub
HBTP += libFastRPC_UTF_Forward_skel
HBTP += libhbtpjni
HBTP += improveTouchStudio
HBTP += libFastRPC_UTF_Forward_Rohm_skel
HBTP += qtc800s.bin
HBTP += qtc800t.bin
HBTP += qtc800h.bin
HBTP += qtc800h_8998_660.bin
HBTP += libFastRPC_UTF_Forward_Qtc2_skel
HBTP += hbtpcfg2.dat
HBTP += hbtpcfg_sdm660_800s_qhd.dat
HBTP += libFastRPC_UTF_Forward_sdm660_skel
HBTP += hbtpcfg_sdm660_800h_qhd.dat
HBTP += libFastRPC_UTF_Forward_800h_skel
HBTP += hbtpcfg_sdm630_800s_fhd.dat
HBTP += hbtpcfg_adsp_800s_fhd.dat
HBTP += libFastRPC_UTF_Forward_800s_sdm845_skel
HBTP += libFastRPC_UTF_Forward_800s_sdm845_uimg_skel
HBTP += hbtpcfg_sdm845_800s_qhd.dat
HBTP += hbtpcfg_sdm845_800s_4k.dat
HBTP += hbtpcfg_sdm845_800h_qhd.dat
HBTP += vendor.qti.hardware.improvetouch.touchcompanion@1.0
HBTP += vendor.qti.hardware.improvetouch.touchcompanion@1.0_vendor
HBTP += vendor.qti.hardware.improvetouch.touchcompanion@1.0-service
HBTP += vendor.qti.hardware.improvetouch.gesturemanager@1.0
HBTP += vendor.qti.hardware.improvetouch.gesturemanager@1.0_vendor
HBTP += vendor.qti.hardware.improvetouch.gesturemanager@1.0-service
HBTP += vendor.qti.hardware.improvetouch.blobmanager@1.0
HBTP += vendor.qti.hardware.improvetouch.blobmanager@1.0_vendor
HBTP += vendor.qti.hardware.improvetouch.blobmanager@1.0-service
HBTP += hbtpcfg_msm8937_800s_fhd.dat
HBTP += hbtpcfg_msm8953_800s_fhd.dat
HBTP += loader.cfg
HBTP += improveTouch
HBTP += qtc801s.bin
HBTP += qtc801s_450.bin
HBTP += hbtpcfg_sdm710_801s_qhd.dat
HBTP += hbtpcfg_sdm660_801s_qhd.dat
HBTP += hbtpcfg_sdm710_800h_qhd.dat
HBTP += hbtpcfg_sdm710_800s_fhd.dat
HBTP += hbtpcfg_sdm710_801s_2k.dat
HBTP += hbtpcfg_sdm855_801s_4k.dat
HBTP += hbtpcfg_hdk855_800h_qhd.dat
HBTP += qtc800s_msm8937.bin
HBTP += com.qualcomm.qti.improvetouch.service
HBTP += com.qualcomm.qti.improvetouch.svchook
HBTP += com.qualcomm.qti.improvetouch.nativelib
HBTP += hbtpcfg_sdm450_801s_fhdp.dat


#HY11_HY22 diff
HY11_HY22_diff += libacdb-fts
HY11_HY22_diff += libdatactrl
HY11_HY22_diff += libevent_observer
HY11_HY22_diff += libimsmedia_jni
HY11_HY22_diff += libwfdavenhancements
HY11_HY22_diff += libacdb-fts
HY11_HY22_diff += libacdbrtac
HY11_HY22_diff += libadiertac
HY11_HY22_diff += libdatactrl
HY11_HY22_diff += libdiag
HY11_HY22_diff += libevent_observer
HY11_HY22_diff += libimsmedia_jni
HY11_HY22_diff += libwbc_jni
HY11_HY22_diff += libmmcamera2_c2d_module
HY11_HY22_diff += libmmcamera2_cpp_module
HY11_HY22_diff += libmmcamera2_iface_modules
HY11_HY22_diff += libmmcamera2_imglib_modules
HY11_HY22_diff += libmmcamera2_isp_modules
HY11_HY22_diff += libmmcamera2_pp_buf_mgr
HY11_HY22_diff += libmmcamera2_pproc_modules
HY11_HY22_diff += libmmcamera2_sensor_modules
HY11_HY22_diff += libmmcamera2_stats_modules
HY11_HY22_diff += libmmcamera2_stats_lib
HY11_HY22_diff += libmmcamera_eztune_module
HY11_HY22_diff += libmmcamera_ppbase_module
HY11_HY22_diff += libmmcamera_sw2d_lib
HY11_HY22_diff += libmmcamera_thread_services
HY11_HY22_diff += libmmcamera_tuning_lookup
HY11_HY22_diff += libremosaic_daemon
HY11_HY22_diff += libsensor_reg
HY11_HY22_diff += libtime_genoff
HY11_HY22_diff += libwfdavenhancements
HY11_HY22_diff += libwfdmmsink
HY11_HY22_diff += libwfduibcsinkinterface
HY11_HY22_diff += libwfduibcsink
HY11_HY22_diff += libpdnotifier
HY11_HY22_diff += libvpptestutils
HY11_HY22_diff += vendor-qti-hardware-alarm.xml
HY11_HY22_diff += libsns_low_lat_stream_skel_system
HY11_HY22_diff += CarrierCacheService
HY11_HY22_diff += CarrierConfigure
HY11_HY22_diff += CarrierLoadService
HY11_HY22_diff += SnapdragonSVA
HY11_HY22_diff += PowerOffAlarmHandler
HY11_HY22_diff += vendor.qti.hardware.alarm@1.0-service
HY11_HY22_diff += vendor.qti.hardware.alarm@1.0-impl
HY11_HY22_diff += libexternal_dog_skel
HY11_HY22_diff += vendor.qti.hardware.alarm@1.0-impl
HY11_HY22_diff += qmi-framework-tests
HY11_HY22_diff += liblogwrap
HY11_HY22_diff += libminui
HY11_HY22_diff += libwificond_ipc
HY11_HY22_diff += vendor.qti.hardware.vpp@1.1_vendor
HY11_HY22_diff += vendor.display.color@1.3.vendor
HY11_HY22_diff += vendor.display.color@1.4.vendor
HY11_HY22_diff += vendor.display.color@1.5.vendor
HY11_HY22_diff += vendor.display.config@1.4.vendor
HY11_HY22_diff += vendor.display.config@1.5.vendor
HY11_HY22_diff += vendor.display.config@1.6.vendor
HY11_HY22_diff += vendor.display.config@1.7.vendor
HY11_HY22_diff += vendor.display.config@1.8.vendor
HY11_HY22_diff += vendor.display.config@1.9.vendor
HY11_HY22_diff += libmmosal.vendor
HY11_HY22_diff += libhidltransport.vendor
HY11_HY22_diff += libgui_vendor.vendor
HY11_HY22_diff += vendor.qti.hardware.display.mapper@1.0.vendor

#HY22 debug modules
HY11_HY22_diff_debug += uimremoteserver
HY11_HY22_diff_debug += uimremoteserver.xml
HY11_HY22_diff_debug += uimremoteclient
HY11_HY22_diff_debug += uimremoteclient.xml
HY11_HY22_diff_debug += PresenceApp
HY11_HY22_diff_debug += libomx-dts

#IMS
IMS := exe-ims-regmanagerprocessnative
#IMS += exe-ims-videoshareprocessnative
IMS += lib-imsdpl
IMS += lib-imsfiledemux
IMS += lib-imsfilemux
IMS += lib-imsqimf
IMS += lib-ims-regmanagerbindernative
IMS += lib-ims-regmanagerjninative
IMS += lib-ims-regmanager
#IMS += lib-ims-videosharebindernative
#IMS += lib-ims-videosharejninative
#IMS += lib-ims-videoshare

# for low RAM projects, remove btmultisim
ifneq ($(TARGET_HAS_LOW_RAM),true)
# BT-Telephony Lib for DSDA
BT_TEL := btmultisim.xml
BT_TEL += btmultisimlibrary
BT_TEL += btmultisim
endif

#IMS_VT
IMS_VT := lib-imsvt
IMS_VT += lib-imscamera
IMS_VT += lib-imsvtextutils
IMS_VT += lib-imsvtutils
IMS_VT += lib-imsvideocodec
IMS_VT += libloopbackvtjni
IMS_VT += LoopbackVT

ifeq ($(call is-board-platform-in-list,msm8939 msm8916 msm8974 msm8226 apq8084 msm8916_32 msm8916_32_512 msm8916_64),true)
IMS_VT += libvcel
else
IMS_VT += librcc
endif

#IMS_NEWARCH
IMS_NEWARCH += lib-rtpcommon
IMS_NEWARCH += lib-rtpcore
IMS_NEWARCH += lib-rtpsl
IMS_NEWARCH += lib-imsvtcore
IMS_NEWARCH += vendor.qti.imsrtpservice@2.0-service-Impl
IMS_NEWARCH += vendor.qti.imsrtpservice@2.1-service-Impl
IMS_NEWARCH += vendor.qti.imsrtpservice@2.0
IMS_NEWARCH += vendor.qti.imsrtpservice@2.0_vendor
IMS_NEWARCH += vendor.qti.imsrtpservice@2.0_system
IMS_NEWARCH += vendor.qti.imsrtpservice@2.1
IMS_NEWARCH += vendor.qti.imsrtpservice@2.1_vendor
IMS_NEWARCH += vendor.qti.imsrtpservice@2.1_system

#IMS_REGMGR
IMS_REGMGR := RegmanagerApi

#INTERFACE_PERMISSIONS
INTERFACE_PERMISSIONS := InterfacePermissions
INTERFACE_PERMISSIONS += interface_permissions.xml

#IQAGENT
IQAGENT := libiq_service
IQAGENT += libiq_client
IQAGENT += IQAgent
IQAGENT += iqmsd
IQAGENT += iqclient

# MARE/QBLAS
MARE := libmare-1.1
MARE += libmare-cpu-1.1
MARE += libQBLAS-0.13.0
MARE += libmare_hexagon_skel.so
MARE += libAMF_hexagon_skel.so

#MDM_HELPER
MDM_HELPER := mdm_helper

#MDM_HELPER_PROXY
MDM_HELPER_PROXY := mdm_helper_proxy

#QCAT_UNBUFFERED
QCAT_UNBUFFERED := qcat_unbuffered

#MM_CORE
MM_CORE := CABLService
MM_CORE += libdisp-aba
MM_CORE += libmm-abl
MM_CORE += libmm-abl-oem
MM_CORE += libscale
MM_CORE += mm-pp-daemon
MM_CORE += SVIService
MM_CORE += libmm-hdcpmgr
MM_CORE += libvpu
MM_CORE += libvfmclientutils
MM_CORE += libmm-qdcm
MM_CORE += libmm-disp-apis
MM_CORE += libmm-als

#MM_COLOR_CONVERSION
MM_COLOR_CONVERSION := libtile2linear

#MM_COLOR_CONVERTOR
MM_COLOR_CONVERTOR := libmm-color-convertor
MM_COLOR_CONVERTOR += libI420colorconvert

#MM_GESTURES
MM_GESTURES := gesture_mouse.idc
MM_GESTURES += GestureOverlayService
MM_GESTURES += GestureTouchInjectionConfig
MM_GESTURES += GestureTouchInjectionService
MM_GESTURES += libgesture-core
MM_GESTURES += libmmgesture-activity-trigger
MM_GESTURES += libmmgesture-bus
MM_GESTURES += libmmgesture-camera
MM_GESTURES += libmmgesture-camera-factory
MM_GESTURES += libmmgesture-client
MM_GESTURES += libmmgesture-jni
MM_GESTURES += libmmgesture-linux
MM_GESTURES += libmmgesture-service
MM_GESTURES += mm-gesture-daemon

#MM_HTTP
MM_HTTP := libmmipstreamaal
MM_HTTP += libmmipstreamnetwork
MM_HTTP += libmmipstreamutils
MM_HTTP += libmmiipstreammmihttp
MM_HTTP += libmmhttpstack
MM_HTTP += libmmipstreamsourcehttp
MM_HTTP += libmmqcmediaplayer
MM_HTTP += libmmipstreamdeal

#MM_DRMPLAY
MM_DRMPLAY := drmclientlib
MM_DRMPLAY += libDrmPlay

#MM_OSAL
MM_OSAL := libmmosal
MM_OSAL += libmmosal_proprietary

ifneq ($(TARGET_BOARD_AUTO),true)
#MM_PARSER
MM_PARSER := libASFParserLib
MM_PARSER += libExtendedExtractor
MM_PARSER += libmmparserextractor
MM_PARSER += libmmparser_lite
MM_PARSER += libmmparser_lite_proprietary
endif


#MM_QSM
MM_QSM := libmmQSM

#MM_STEREOLIB
MM_STEREOLIB := libmmstereo

#MM_STILL
MM_STILL := libadsp_jpege_skel
MM_STILL += libgemini
MM_STILL += libimage-jpeg-dec-omx-comp
MM_STILL += libimage-jpeg-enc-omx-comp
MM_STILL += libimage-omx-common
MM_STILL += libjpegdhw
MM_STILL += libjpegehw
MM_STILL += libmmipl
MM_STILL += libmmjpeg
MM_STILL += libmmjps
MM_STILL += libmmmpo
MM_STILL += libmmmpod
MM_STILL += libmmqjpeg_codec
MM_STILL += libmmstillomx
MM_STILL += libqomx_jpegenc
MM_STILL += libqomx_jpegdec
MM_STILL += mm-jpeg-dec-test
MM_STILL += mm-jpeg-dec-test-client
MM_STILL += mm-jpeg-enc-test
MM_STILL += mm-jpeg-enc-test-client
MM_STILL += mm-jps-enc-test
MM_STILL += mm-mpo-enc-test
MM_STILL += mm-mpo-dec-test
MM_STILL += mm-qjpeg-dec-test
MM_STILL += mm-qjpeg-enc-test
MM_STILL += mm-qomx-ienc-test
MM_STILL += mm-qomx-idec-test
MM_STILL += test_gemini
MM_STILL += libjpegdmahw
MM_STILL += libmmqjpegdma
MM_STILL += qjpeg-dma-test
MM_STILL += libqomx_jpegenc_pipe

#MM_VIDEO
ifneq ($(call is-board-platform,sdm845),true)
MM_VIDEO := ast-mm-vdec-omx-test7k
MM_VIDEO += iv_h264_dec_lib
MM_VIDEO += iv_mpeg4_dec_lib
MM_VIDEO += libh264decoder
MM_VIDEO += libHevcSwDecoder
MM_VIDEO += liblasic
MM_VIDEO += libmm-adspsvc
MM_VIDEO += libmp4decoder
MM_VIDEO += libmp4decodervlddsp
MM_VIDEO += libOmxH264Dec
MM_VIDEO += libOmxIttiamVdec
MM_VIDEO += libOmxIttiamVenc
MM_VIDEO += libOmxMpeg4Dec
MM_VIDEO += libOmxOn2Dec
MM_VIDEO += libOmxrv9Dec
MM_VIDEO += libon2decoder
MM_VIDEO += librv9decoder
MM_VIDEO += libVenusMbiConv
MM_VIDEO += mm-vdec-omx-test
MM_VIDEO += mm-venc-omx-test
MM_VIDEO += msm-vidc-test
MM_VIDEO += venc-device-android
MM_VIDEO += venus-v1.b00
MM_VIDEO += venus-v1.b01
MM_VIDEO += venus-v1.b02
MM_VIDEO += venus-v1.b03
MM_VIDEO += venus-v1.b04
MM_VIDEO += venus-v1.mdt
MM_VIDEO += venus-v1.mbn
MM_VIDEO += venus.b00
MM_VIDEO += venus.b01
MM_VIDEO += venus.b02
MM_VIDEO += venus.b03
MM_VIDEO += venus.b04
MM_VIDEO += venus.mbn
MM_VIDEO += venus.mdt
MM_VIDEO += vidc_1080p.fw
MM_VIDEO += vidc.b00
MM_VIDEO += vidc.b01
MM_VIDEO += vidc.b02
MM_VIDEO += vidc.b03
MM_VIDEO += vidcfw.elf
MM_VIDEO += vidc.mdt
MM_VIDEO += vidc_720p_command_control.fw
MM_VIDEO += vidc_720p_h263_dec_mc.fw
MM_VIDEO += vidc_720p_h264_dec_mc.fw
MM_VIDEO += vidc_720p_h264_enc_mc.fw
MM_VIDEO += vidc_720p_mp4_dec_mc.fw
MM_VIDEO += vidc_720p_mp4_enc_mc.fw
MM_VIDEO += vidc_720p_vc1_dec_mc.fw
MM_VIDEO += vt-sim-test
MM_VIDEO += libgpustats
MM_VIDEO += libvqzip
endif

MM_VIDEO += libOmxVidEnc
MM_VIDEO += libOmxWmvDec
MM_VIDEO += libMpeg4SwEncoder
MM_VIDEO += libswvdec
MM_VIDEO += libVC1DecDsp_skel
MM_VIDEO += libVC1DecDsp_skel.so
MM_VIDEO += libVC1Dec
MM_VIDEO += libVC1Dec.so
MM_VIDEO += mm-vidc-omx-test
MM_VIDEO += mm-swvenc-test
MM_VIDEO += mm-swvdec-test
ifneq ($(TARGET_BOARD_AUTO),true)
MM_VIDEO += libavenhancements
endif
MM_VIDEO += libfastcrc
MM_VIDEO += libstreamparser
MM_VIDEO += libvideoutils
MM_VIDEO += libUBWC

#MM_VPU
MM_VPU := vpu.b00
MM_VPU += vpu.b01
MM_VPU += vpu.b02
MM_VPU += vpu.b03
MM_VPU += vpu.b04
MM_VPU += vpu.b05
MM_VPU += vpu.b06
MM_VPU += vpu.b07
MM_VPU += vpu.b08
MM_VPU += vpu.b09
MM_VPU += vpu.b10
MM_VPU += vpu.b11
MM_VPU += vpu.b12
MM_VPU += vpu.mbn
MM_VPU += vpu.mdt


#MODEM_APIS
MODEM_APIS := libadc
MODEM_APIS += libauth
MODEM_APIS += libcm
MODEM_APIS += libdsucsd
MODEM_APIS += libfm_wan_api
MODEM_APIS += libgsdi_exp
MODEM_APIS += libgstk_exp
MODEM_APIS += libisense
MODEM_APIS += libloc_api
MODEM_APIS += libmmgsdilib
MODEM_APIS += libmmgsdisessionlib
MODEM_APIS += libmvs
MODEM_APIS += libnv
MODEM_APIS += liboem_rapi
MODEM_APIS += libpbmlib
MODEM_APIS += libpdapi
MODEM_APIS += libpdsm_atl
MODEM_APIS += libping_mdm
MODEM_APIS += libplayready
MODEM_APIS += librfm_sar
MODEM_APIS += libsnd
MODEM_APIS += libtime_remote_atom
MODEM_APIS += libuim
MODEM_APIS += libvoem_if
MODEM_APIS += libwidevine
MODEM_APIS += libcommondefs
MODEM_APIS += libcm_fusion
MODEM_APIS += libcm_mm_fusion
MODEM_APIS += libdsucsdappif_apis_fusion
MODEM_APIS += liboem_rapi_fusion
MODEM_APIS += libpbmlib_fusion
MODEM_APIS += libping_lte_rpc
MODEM_APIS += libwms_fusion

#MODEM_API_TEST
MODEM_API_TEST := librstf

#MPDCVS
MPDCVS := dcvsd
MPDCVS += mpdecision

#ENERGY-AWARENESS
ENERGY_AWARENESS := energy-awareness

#MPQ_COMMAND_IF
MPQ_COMMAND_IF := libmpq_ci
MPQ_COMMAND_IF += libmpqci_cimax_spi
MPQ_COMMAND_IF += libmpqci_tsc_ci_driver

#MPQ_PLATFORM
MPQ_PLATFORM := AvSyncTest
MPQ_PLATFORM += libavinput
MPQ_PLATFORM += libavinputhaladi
MPQ_PLATFORM += libfrc
MPQ_PLATFORM += libmpqaudiocomponent
MPQ_PLATFORM += libmpqaudiosettings
MPQ_PLATFORM += libmpqavs
MPQ_PLATFORM += libmpqavstreammanager
MPQ_PLATFORM += libmpqcore
MPQ_PLATFORM += libmpqplatform
MPQ_PLATFORM += libmpqplayer
MPQ_PLATFORM += libmpqplayerclient
MPQ_PLATFORM += libmpqsource
MPQ_PLATFORM += libmpqstobinder
MPQ_PLATFORM += libmpqstreambuffersource
MPQ_PLATFORM += libmpqtopology_cimax_mux_driver
MPQ_PLATFORM += libmpqtopology_ts_out_bridge_mux_driver
MPQ_PLATFORM += libmpqtopology_tsc_mux_driver
MPQ_PLATFORM += libmpqtsdm
MPQ_PLATFORM += libmpqutils
MPQ_PLATFORM += libmpqvcapsource
MPQ_PLATFORM += libmpqvcxo
MPQ_PLATFORM += libmpqvideodecoder
MPQ_PLATFORM += libmpqvideorenderer
MPQ_PLATFORM += libmpqvideosettings
MPQ_PLATFORM += librffrontend
MPQ_PLATFORM += libmpqtestpvr
MPQ_PLATFORM += MPQDvbCTestApp
MPQ_PLATFORM += MPQPlayerApp
MPQ_PLATFORM += MPQStrMgrTest
MPQ_PLATFORM += MPQUnitTest
MPQ_PLATFORM += MPQVideoRendererTestApp
MPQ_PLATFORM += mpqavinputtest
MPQ_PLATFORM += mpqi2ctest
MPQ_PLATFORM += mpqvcaptest
MPQ_PLATFORM += mpqdvbtestapps
MPQ_PLATFORM += mpqdvbservice

#N_SMUX
N_SMUX := n_smux

#NQ NFC Config files + firmware images
ifeq ($(strip $(TARGET_USES_NQ_NFC)),true)
NQ_NFC_PROP := libnfc-mtp_default.conf
NQ_NFC_PROP += libnfc-mtp_rf1.conf
NQ_NFC_PROP += libnfc-mtp_rf2.conf
NQ_NFC_PROP += libnfc-mtp-NQ3XX.conf
NQ_NFC_PROP += libnfc-mtp-NQ4XX.conf
NQ_NFC_PROP += libnfc-mtp-SN100.conf
NQ_NFC_PROP += libnfc-qrd_default.conf
NQ_NFC_PROP += libnfc-qrd_rf1.conf
NQ_NFC_PROP += libnfc-qrd_rf2.conf
NQ_NFC_PROP += libnfc-qrd-NQ3XX.conf
NQ_NFC_PROP += libnfc-qrd-NQ4XX.conf
NQ_NFC_PROP += libnfc-qrd-SN100.conf
NQ_NFC_PROP += libsn100u_fw.so
NQ_NFC_PROP += libpn547_fw.so
NQ_NFC_PROP += libpn548ad_fw.so
NQ_NFC_PROP += libpn551_fw.so
NQ_NFC_PROP += libpn553_fw.so
NQ_NFC_PROP += libpn557_fw.so
endif

#OEM_SERVICES - system monitor shutdown modules
OEM_SERVICES := libSubSystemShutdown
OEM_SERVICES += libsubsystem_control

#ONCRPC
ONCRPC := libdsm
ONCRPC += liboncrpc
ONCRPC += libping_apps
ONCRPC += libqueue
ONCRPC += ping_apps_client_test_0000

#PD_LOCATER - Service locater binary/libs
PD_LOCATER := pd-mapper
PD_LOCATER += libpdmapper
PD_LOCATER += libjson  # 3rd party support library

#LATENCY
LATENCY := vendor.qti.hardware.data.latency@1.0
LATENCY += vendor.qti.hardware.data.latency-V1.0-java
LATENCY += vendor.qti.hardware.data.latency.xml

#PLAYREADY
PLAYREADY := drmtest
PLAYREADY += libdrmfs
PLAYREADY += libdrmMinimalfs
PLAYREADY += libdrmtime
PLAYREADY += libtzplayready
PLAYREADY += libbase64
PLAYREADY += libprpk3
PLAYREADY += libprdrmengine

#PLAYREADY3.0 PRODUCT LIST
PLAYREADY_3_PRODUCT_LIST:= apq8098_latv

#PERIPHERAL MANAGER:
PERMGR := pm-service
PERMGR += libperipheral_client
PERMGR += pm-proxy

#PROFILER
PROFILER := profiler_tester
PROFILER += profiler_daemon
PROFILER += libprofiler_msmadc

#TREPN
TREPN := Trepn

#QCHAT
QCHAT := QComQMIPermissions

#QCNVITEM
QCNVITEM := qcnvitems
QCNVITEM += qcnvitems.xml

# QCRIL
QCRIL += vendor.qti.hardware.data.iwlan@1.0
QCRIL += libqcrildatactl
QCRIL += vendor.qti.hardware.data.connection@1.0
QCRIL += vendor.qti.hardware.data.connection-V1.0-java
QCRIL += vendor.qti.hardware.data.connection-V1.0-java.xml

#QMI
QMI := check_system_health
QMI := irsc_util
QMI += libqmi_cci
QMI += libqmi_common_so
QMI += libqmi_common_system
QMI += libqmi_csi
QMI += libqmi_encdec
QMI += libqmi_encdec_system
QMI += libsmemlog
QMI += libmdmdetect
QMI += libqsocket
QMI += qmiproxy
QMI += qmi_test_mt_client_init_instance
QMI += libqmi_cci_system
QMI += libqrtr
QMI += libqsocket
QMI += qrtr-cfg
QMI += qrtr-ns
QMI += qrtr-lookup

#QOSMGR
QOSMGR := qosmgr
QOSMGR += qosmgr_rules.xml

QRD_CALENDAR_APPS += LunarInfoProvider

QRD_APPS += PowerOffMusic
#QUICKCHARGE
QUICKCHARGE := hvdcp

#QVR
ifeq ($(call is-board-platform-in-list, lahaina kona sdm845 sdm710 msmnile $(MSMSTEPPE)),true)
QVR := qvrservice_6dof_config.xml
QVR += qvrservice_6dof_config_stereo.xml
QVR += qvrservice_config.txt
QVR += ov7251_640x480_cam_config.xml
QVR += ov9282_640x400_cam_config.xml
QVR += ov9282_stereo_1280x400_cam_config.xml
QVR += ov9282_stereo_2560x800_cam_config.xml
QVR += libqvrservice_ov7251_hvx_tuning
QVR += libqvrservice_ov9282_hvx_tuning
QVR += qvrservice
QVR += qvrservicetest
QVR += qvrservicetest64
QVR += qvrcameratest
QVR += qvrcameratest64
QVR += qvrcameratseq
QVR += qvrcameratseq64
QVR += qvrd.rc
QVR += libqvr_cdsp_driver_stub
QVR += libqvr_dsp_driver_skel
QVR += libtracker_6dof_skel
QVR += libqvrservice
QVR += libqvrservice_client.qti
QVR += libqvrcamera_client.qti
QVR += libqvrservice_hvxcameraclient
QVR += libdsp_streamer_qvrcam_receiver
QVR += libqvr_cam_cdsp_driver_stub
QVR += libqvr_cam_dsp_driver_skel
QVR += libqvr_eyetracking_plugin
QVR += libeye_tracking_dsp_sample_stub
QVR += libeye_tracking_dsp_sample_skel

QVR += sxrservice
QVR += sxrd.rc
QVR += sxrd_ext.rc
QVR += libsxrservice
QVR += libsxrservice_client
endif

#XRCOMM
XRCOMM += xrcommservice
XRCOMM += xrcommservice.rc
XRCOMM += libxrcommcoreutils
XRCOMM += libxrcommdiscoveryservice
XRCOMM += libxrcommdiscoveryservice_client
XRCOMM += libxrcommnetworkservice
XRCOMM += libxrcommnetworkservice_client
XRCOMM += libxrcommservice_client
XRCOMM += libxrcommtimerservice
XRCOMM += libxrcommtimerservice_client
XRCOMM += libxrcommwpacliwrapper

#REMOTEFS
REMOTEFS := rmt_storage

#RFS_ACCESS
RFS_ACCESS := rfs_access

# RIDL/LogKit II
RIDL_BINS := RIDL_KIT
RIDL_BINS += libsregex
RIDL_BINS += libtar
RIDL_BINS += RIDLClient.exe
RIDL_BINS += RIDLClient
RIDL_BINS += RIDL.db
RIDL_BINS += qdss.cfg
RIDL_BINS += GoldenLogmask.dmc
RIDL_BINS += OTA-Logs.dmc
RIDL_BINS += liblz4

#RNGD
RNGD := rngd



SECUREMSM += vendor.qti.hardware.qdutils_disp@1.0
SECUREMSM += vendor.qti.hardware.qdutils_disp@1.0_vendor
SECUREMSM += vendor.qti.hardware.qdutils_disp@1.0-impl-qti
SECUREMSM += vendor.qti.hardware.qdutils_disp@1.0-service-qti
SECUREMSM += vendor.qti.hardware.qdutils_disp@1.0-service-qti.rc
SECUREMSM += vendor.qti.hardware.seccam@1.0
SECUREMSM += vendor.qti.hardware.seccam@1.0_vendor
SECUREMSM += vendor.qti.hardware.seccam@1.0-service-qti
SECUREMSM += vendor.qti.hardware.seccam@1.0-service-qti.rc
SECUREMSM += aostlmd
SECUREMSM += dbAccess
SECUREMSM += drm_generic_prov_test
SECUREMSM += e2image_blocks
SECUREMSM += filefrag_blocks
SECUREMSM += isdbtmmtest
SECUREMSM += InstallKeybox
SECUREMSM += libcppf
SECUREMSM += libcpion
SECUREMSM += libdrmprplugin
SECUREMSM += libdrmprplugin_customer
SECUREMSM += libprdrmdecrypt
SECUREMSM += libprdrmdecrypt_customer
SECUREMSM += libprmediadrmdecrypt
SECUREMSM += libprmediadrmdecrypt_customer
SECUREMSM += libprmediadrmplugin
SECUREMSM += libprmediadrmplugin_customer
SECUREMSM += libgoogletest
SECUREMSM += libtzplayready
SECUREMSM += libbase64
SECUREMSM += libtzplayready_customer
SECUREMSM += libprpk3
SECUREMSM += libprdrmengine
SECUREMSM += libtzdrmgenprov
SECUREMSM += liboemcrypto
SECUREMSM += liboemcrypto.a
SECUREMSM += libhdcpsrm
SECUREMSM += libcpion
SECUREMSM += libops
SECUREMSM += hdcp_srm
SECUREMSM += libmdtp
SECUREMSM += libmdtp_crypto
SECUREMSM += libmdtpdemojni
SECUREMSM += libPaApi
SECUREMSM += libqmp_sphinx_jni
SECUREMSM += libqmp_sphinxlog
SECUREMSM += libqmpart
SECUREMSM += librmp
SECUREMSM += libpvr
SECUREMSM += libSampleAuthJNI
SECUREMSM += libSampleExtAuthJNI
SECUREMSM += libsecotacommon
SECUREMSM += libsecotanservice
SECUREMSM += libSecureSampleAuthJNI
SECUREMSM += libSecureExtAuthJNI
SECUREMSM += lib-sec-disp
SECUREMSM += libseemp_binder
SECUREMSM += libseempnative
SECUREMSM += libSeemplog
SECUREMSM += libSeempMsgService
SECUREMSM += libsi
SECUREMSM += libspcom
SECUREMSM += libspiris
SECUREMSM += libspl
SECUREMSM += spdaemon
SECUREMSM += sec_nvm
SECUREMSM += libqsappsver
SECUREMSM += libTLV
SECUREMSM += libqisl
SECUREMSM += libseccam
SECUREMSM += seccamservice
SECUREMSM += mcDriverDaemon
SECUREMSM += mdtp_fota
SECUREMSM += mdtp_ut
SECUREMSM += mdtp.img
SECUREMSM += mdtp
SECUREMSM += mdtpd
SECUREMSM += MdtpService
SECUREMSM += MdtpDemo
SECUREMSM += oemwvtest
SECUREMSM += qfipsverify
SECUREMSM += qfipsverify.hmac
SECUREMSM += bootimg.hmac
SECUREMSM += libDevHealth
SECUREMSM += libHealthAuthClient
SECUREMSM += libHealthAuthJNI
SECUREMSM += liblmclient
SECUREMSM += HealthAuthService
SECUREMSM += pvclicense_sample
SECUREMSM += qseecom_security_test
SECUREMSM += SampleAuthenticatorService
SECUREMSM += SampleExtAuthService
SECUREMSM += SecotaAPI
SECUREMSM += secotad
SECUREMSM += SecotaService
SECUREMSM += SecureExtAuthService
SECUREMSM += SecureSampleAuthService
SECUREMSM += seemp
SECUREMSM += SeempAPI
SECUREMSM += seemp_cli
SECUREMSM += SeempCommon
SECUREMSM += seempd
SECUREMSM += seempd.rc
SECUREMSM += SeempService
SECUREMSM += SoterProvisioningTool
SECUREMSM += StoreKeybox
SECUREMSM += sphinxproxy
SECUREMSM += TelemetryService
SECUREMSM += tbaseLoader
SECUREMSM += widevinetest
SECUREMSM += widevinetest_rpc
SECUREMSM += hdcp1prov
SECUREMSM += libhdcp1prov
SECUREMSM += hdcp2p2prov
SECUREMSM += hdcp_srm
SECUREMSM += libhdcp2p2prov
SECUREMSM += tloc_daemon
SECUREMSM += VtsHalDataLatencyV1_0TargetTest

#SENSORS
SENSORS += android.hardware.sensors@2.0-ScopedWakelock
SENSORS += vendor.qti.hardware.sensorscalibrate-V1.0-java

#SS_RESTART
SS_RESTART := ssr_diag
SS_RESTART += subsystem_ramdump

#SVGT
SVGT := libsvgecmascriptBindings
SVGT += libsvgutils
SVGT += libsvgabstraction
SVGT += libsvgscriptEngBindings
SVGT += libsvgnativeandroid
SVGT += libsvgt
SVGT += libsvgcore

#SWDEC2DTO3D
SW2DTO3D := libswdec_2dto3d

#SYSTEM HEALTH MONITOR
SYSTEM_HEALTH_MONITOR := libsystem_health_mon

#TFTP
TFTP := tftp_server

#TIME_SERVICES
TIME_SERVICES := time_daemon TimeService libTimeService move_time_data.sh

#TINY xml
TINYXML := libtinyxml

#TINYXML2
TINYXML2 := libtinyxml2

#TOUCH FUSION
TOUCH_FUSION := touch_fusion
TOUCH_FUSION += qtc800s.cfg
TOUCH_FUSION += qtc800s.bin

#TS_TOOLS
TS_TOOLS := evt-sniff.cfg

#TTSP firmware
TTSP_FW := cyttsp_7630_fluid.hex
TTSP_FW += cyttsp_8064_mtp.hex
TTSP_FW += cyttsp_8660_fluid_p2.hex
TTSP_FW += cyttsp_8660_fluid_p3.hex
TTSP_FW += cyttsp_8660_ffa.hex
TTSP_FW += cyttsp_8960_cdp.hex

#TV_TUNER
TV_TUNER := atv_fe_test
TV_TUNER += dtv_fe_test
TV_TUNER += lib_atv_rf_fe
TV_TUNER += lib_dtv_rf_fe
TV_TUNER += lib_MPQ_RFFE
TV_TUNER += libmpq_bsp8092_cdp_h1
TV_TUNER += libmpq_bsp8092_cdp_h5
TV_TUNER += libmpq_bsp8092_rd_h1
TV_TUNER += lib_tdsn_c231d
TV_TUNER += lib_tdsq_g631d
TV_TUNER += lib_tdss_g682d
TV_TUNER += libmpq_rf_utils
TV_TUNER += lib_sif_demod_stub
TV_TUNER += lib_tv_bsp_mpq8064_dvb
TV_TUNER += lib_tv_receiver_stub
TV_TUNER += libtv_tuners_io
TV_TUNER += tv_driver_test
TV_TUNER += tv_fe_test
TV_TUNER += libUCCP330
TV_TUNER += libForza


#ULTRASOUND_COMMON
ULTRASOUND_COMMON := UltrasoundSettings
ULTRASOUND_COMMON += form_factor_fluid.cfg
ULTRASOUND_COMMON += form_factor_liquid.cfg
ULTRASOUND_COMMON += form_factor_mtp.cfg
ULTRASOUND_COMMON += libual
ULTRASOUND_COMMON += libualutil
ULTRASOUND_COMMON += libusndroute
ULTRASOUND_COMMON += libusutils
ULTRASOUND_COMMON += mixer_paths_dragon.xml
ULTRASOUND_COMMON += mixer_paths_fluid.xml
ULTRASOUND_COMMON += mixer_paths_liquid.xml
ULTRASOUND_COMMON += mixer_paths_mtp.xml
ULTRASOUND_COMMON += readme.txt
ULTRASOUND_COMMON += usf_post_boot.sh
ULTRASOUND_COMMON += usf_settings.sh
ULTRASOUND_COMMON += usf_tester
ULTRASOUND_COMMON += usf_tester_echo_fluid.cfg
ULTRASOUND_COMMON += usf_tester_echo_mtp.cfg
ULTRASOUND_COMMON += usf_tester_epos_fluid.cfg
ULTRASOUND_COMMON += usf_tester_epos_liquid.cfg
ULTRASOUND_COMMON += usf_tester_epos_mtp.cfg
ULTRASOUND_COMMON += usf_tsc.idc
ULTRASOUND_COMMON += usf_tsc_ext.idc
ULTRASOUND_COMMON += usf_tsc_ptr.idc
ULTRASOUND_COMMON += version.txt

#ULTRASOUND_GESTURE
ULTRASOUND_GESTURE := libgessyncsockadapter
ULTRASOUND_GESTURE += libqcsyncgesture
ULTRASOUND_GESTURE += libsyncgesadapter
ULTRASOUND_GESTURE += usf_sync_gesture
ULTRASOUND_GESTURE += usf_sync_gesture_apps_fluid.cfg
ULTRASOUND_GESTURE += usf_sync_gesture_apps_mtp.cfg
ULTRASOUND_GESTURE += usf_sync_gesture_fluid.cfg
ULTRASOUND_GESTURE += usf_sync_gesture_fluid_tx_transparent_data.bin
ULTRASOUND_GESTURE += usf_sync_gesture_fw_apps_mtp.cfg
ULTRASOUND_GESTURE += usf_sync_gesture_fw_mtp.cfg
ULTRASOUND_GESTURE += usf_sync_gesture_liquid.cfg
ULTRASOUND_GESTURE += usf_sync_gesture_liquid_algo_transparent_data.bin
ULTRASOUND_GESTURE += usf_sync_gesture_liquid_tx_transparent_data.bin
ULTRASOUND_GESTURE += usf_sync_gesture_lpass_rec_mtp.cfg
ULTRASOUND_GESTURE += usf_sync_gesture_mtp.cfg
ULTRASOUND_GESTURE += usf_sync_gesture_mtp_algo_transparent_data.bin
ULTRASOUND_GESTURE += usf_sync_gesture_mtp_tx_transparent_data.bin

#ULTRASOUND_PEN
ULTRASOUND_PEN := ASDConf.sdc
ULTRASOUND_PEN += DigitalPenService
ULTRASOUND_PEN += DigitalPenSettings
ULTRASOUND_PEN += PenPairingApp
ULTRASOUND_PEN += ScopeDebuggerRecordingTool
ULTRASOUND_PEN += calibver
ULTRASOUND_PEN += digitalpenservice.xml
ULTRASOUND_PEN += libdpencalib
ULTRASOUND_PEN += libdpencalib_asm
ULTRASOUND_PEN += libepdsp
ULTRASOUND_PEN += libepdsp_SDserver
ULTRASOUND_PEN += libppl
ULTRASOUND_PEN += product_calib_dragon_ref1.dat
ULTRASOUND_PEN += product_calib_dragon_ref2.dat
ULTRASOUND_PEN += product_calib_dragon_ref3.dat
ULTRASOUND_PEN += product_calib_fluid_ref1.dat
ULTRASOUND_PEN += product_calib_fluid_ref2.dat
ULTRASOUND_PEN += product_calib_fluid_ref3.dat
ULTRASOUND_PEN += product_calib_liquid_ref1.dat
ULTRASOUND_PEN += product_calib_liquid_ref2.dat
ULTRASOUND_PEN += product_calib_liquid_ref3.dat
ULTRASOUND_PEN += product_calib_mtp_ref1.dat
ULTRASOUND_PEN += product_calib_mtp_ref2.dat
ULTRASOUND_PEN += product_calib_mtp_ref3.dat
ULTRASOUND_PEN += ps_tuning1_fluid.bin
ULTRASOUND_PEN += ps_tuning1_idle_liquid.bin
ULTRASOUND_PEN += ps_tuning1_idle_mtp.bin
ULTRASOUND_PEN += ps_tuning1_mtp.bin
ULTRASOUND_PEN += ps_tuning1_standby_liquid.bin
ULTRASOUND_PEN += ps_tuning1_standby_mtp.bin
ULTRASOUND_PEN += service_settings_dragon.xml
ULTRASOUND_PEN += service_settings_fluid.xml
ULTRASOUND_PEN += service_settings_liquid.xml
ULTRASOUND_PEN += service_settings_liquid_1_to_1.xml
ULTRASOUND_PEN += service_settings_liquid_folio.xml
ULTRASOUND_PEN += service_settings_mtp.xml
ULTRASOUND_PEN += service_settings_mtp_1_to_1.xml
ULTRASOUND_PEN += service_settings_mtp_folio.xml
ULTRASOUND_PEN += sw_calib_liquid.dat
ULTRASOUND_PEN += sw_calib_mtp.dat
ULTRASOUND_PEN += unit_calib_dragon_ref1.dat
ULTRASOUND_PEN += unit_calib_dragon_ref2.dat
ULTRASOUND_PEN += unit_calib_dragon_ref3.dat
ULTRASOUND_PEN += unit_calib_fluid_ref1.dat
ULTRASOUND_PEN += unit_calib_fluid_ref2.dat
ULTRASOUND_PEN += unit_calib_fluid_ref3.dat
ULTRASOUND_PEN += unit_calib_liquid_ref1.dat
ULTRASOUND_PEN += unit_calib_liquid_ref2.dat
ULTRASOUND_PEN += unit_calib_liquid_ref3.dat
ULTRASOUND_PEN += unit_calib_mtp_ref1.dat
ULTRASOUND_PEN += unit_calib_mtp_ref2.dat
ULTRASOUND_PEN += unit_calib_mtp_ref3.dat
ULTRASOUND_PEN += usf_epos
ULTRASOUND_PEN += usf_epos_fluid.cfg
ULTRASOUND_PEN += usf_epos_liquid.cfg
ULTRASOUND_PEN += usf_epos_liquid_6_channels.cfg
ULTRASOUND_PEN += usf_epos_liquid_ps_disabled.cfg
ULTRASOUND_PEN += usf_epos_mtp.cfg
ULTRASOUND_PEN += usf_epos_mtp_ps_disabled.cfg
ULTRASOUND_PEN += usf_pairing
ULTRASOUND_PEN += usf_pairing_fluid.cfg
ULTRASOUND_PEN += usf_pairing_liquid.cfg
ULTRASOUND_PEN += usf_pairing_mtp.cfg
ULTRASOUND_PEN += usf_sw_calib
ULTRASOUND_PEN += usf_sw_calib_dragon.cfg
ULTRASOUND_PEN += usf_sw_calib_fluid.cfg
ULTRASOUND_PEN += usf_sw_calib_liquid.cfg
ULTRASOUND_PEN += usf_sw_calib_mtp.cfg
ULTRASOUND_PEN += usf_sw_calib_tester_dragon.cfg
ULTRASOUND_PEN += usf_sw_calib_tester_fluid.cfg
ULTRASOUND_PEN += usf_sw_calib_tester_liquid.cfg
ULTRASOUND_PEN += usf_sw_calib_tester_mtp.cfg
REF1_SERIES_FILES_NUM := 1 2 3 4 5 6 7 8 9 10
REF2_SERIES_FILES_NUM := 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36
REF3_SERIES_FILES_NUM := 1
define add_series_packets
  ULTRASOUND_PEN += $1
endef
$(foreach item,$(REF1_SERIES_FILES_NUM),$(eval $(call add_series_packets,series_calib$(item)_ref1.dat)))
$(foreach item,$(REF2_SERIES_FILES_NUM),$(eval $(call add_series_packets,series_calib$(item)_ref2.dat)))
$(foreach item,$(REF3_SERIES_FILES_NUM),$(eval $(call add_series_packets,series_calib$(item)_ref3.dat)))

#ULTRASOUND_PROXIMITY
ULTRASOUND_PROXIMITY := libproxadapter
ULTRASOUND_PROXIMITY += libproxsockadapter
ULTRASOUND_PROXIMITY += libqcproximity
ULTRASOUND_PROXIMITY += Proximity_calib
ULTRASOUND_PROXIMITY += Proximity_tester
ULTRASOUND_PROXIMITY += usf_proximity_calib.sh
ULTRASOUND_PROXIMITY += sensors.oem
ULTRASOUND_PROXIMITY += us-syncproximity
ULTRASOUND_PROXIMITY += us-syncproximity.so
ULTRASOUND_PROXIMITY += usf_pocket_apps_mtp.cfg
ULTRASOUND_PROXIMITY += usf_pocket_mtp.cfg
ULTRASOUND_PROXIMITY += usf_pocket_mtp_algo_transparent_data.bin
ULTRASOUND_PROXIMITY += usf_proximity
ULTRASOUND_PROXIMITY += usf_proximity_apps_mtp.cfg
ULTRASOUND_PROXIMITY += usf_proximity_mtp.cfg
ULTRASOUND_PROXIMITY += usf_proximity_mtp_algo_transparent_data.bin
ULTRASOUND_PROXIMITY += usf_proximity_mtp_debug.cfg
ULTRASOUND_PROXIMITY += usf_proximity_mtp_rx_transparent_data.bin
ULTRASOUND_PROXIMITY += usf_proximity_mtp_tx_transparent_data.bin
ULTRASOUND_PROXIMITY += usf_ranging_apps_mtp.cfg
ULTRASOUND_PROXIMITY += usf_ranging_mtp_algo_transparent_data.bin

#USB
USB := ice40.bin

#USB_UICC_CLIENT
USB_UICC_CLIENT := usb_uicc_client

#VEHICLE_NETWORKS
VEHICLE_NETWORKS := libcanwrapper
VEHICLE_NETWORKS += canflasher
VEHICLE_NETWORKS += mpc5746c_firmware_A.bin
VEHICLE_NETWORKS += mpc5746c_firmware_B.bin
VEHICLE_NETWORKS += vendor.qti.hardware.automotive.vehicle@1.0-service
VEHICLE_NETWORKS += vendor.qti.hardware.automotive.vehicle@1.0-service.rc
VEHICLE_NETWORKS += android.hardware.automotive.vehicle@2.0-manager-lib
VEHICLE_NETWORKS += vendor.qti.hardware.automotive.vehicle@1.0.vendor

#VM_BMS
VM_BMS := vm_bms

#VPP
VPP := DE.o.msm8937
VPP += DE.o.msm8953
VPP += DE.o.sdm660
VPP += libhcp_rpc_skel
VPP += libhcp_rpc_skel.so
VPP += libmmsw_detail_enhancement
VPP += libmmsw_math
VPP += libmmsw_opencl
VPP += libmmsw_platform
VPP += libOmxVpp
VPP += libvpplibrary
VPP += libvpphcp
VPP += libvpphvx
VPP += libvppimmotion
VPP += libvpp_frc
VPP += libvpp_frc.so
VPP += libvpp_svc_skel
VPP += libvpp_svc_skel.so
VPP += libvppclient
VPP += vendor.qti.hardware.vpp@1.1
VPP += vppservice

#WEBKIT
WEBKIT := browsermanagement
WEBKIT += libwebkitaccel
WEBKIT += PrivInit
WEBKIT += libdnshostprio
WEBKIT += libnetmonitor
WEBKIT += qnet-plugin
WEBKIT += pp_proc_plugin
WEBKIT += tcp-connections
WEBKIT += libtcpfinaggr
WEBKIT += modemwarmup
WEBKIT += libgetzip
WEBKIT += spl_proc_plugin
WEBKIT += libsocketpoolextend
WEBKIT += libvideo_cor

#WIGIG
WIGIG := wigig-service
WIGIG += WigigSettings
WIGIG += libaoa
WIGIG += ftmtest
WIGIG += aoa_cldb_falcon.bin
WIGIG += aoa_cldb_swl14.bin
WIGIG += wigig_supplicant.conf
WIGIG += wigig_p2p_supplicant.conf
WIGIG += wigighalsvc
WIGIG += vendor.qti.hardware.wigig.supptunnel@1.0
WIGIG += vendor.qti.hardware.wigig.supptunnel-V1.0-java
WIGIG += wigignpt
WIGIG += vendor.qti.hardware.wigig.netperftuner@1.0
WIGIG += vendor.qti.hardware.wigig.netperftuner-V1.0-java

#WLAN
WLAN := pktlogconf
WLAN += athdiag
WLAN += cnss_diag
ifneq ($(WLAN_TARGET_HAS_LOW_RAM), true)
WLAN += cnss-daemon
endif
WLAN += libwifi-hal-qcom
WLAN += libwpa_client
WLAN += hal_proxy_daemon
WLAN += sigma_dut
WLAN += e_loop
WLAN += myftm
WLAN += vendor_cmd_tool
WLAN += icm
WLAN += libwpa_drv_oem
WLAN += android.hardware.wifi-service
#WLAN += android.hardware.wifi.offload@1.0-service
WLAN += spectraltool

#LOG_SYSTEM
LOG_SYSTEM := Logkit
LOG_SYSTEM += SystemAgent
LOG_SYSTEM += qlogd
LOG_SYSTEM += qlog-conf.xml
LOG_SYSTEM += qdss.cfg
LOG_SYSTEM += default_diag_mask.cfg
LOG_SYSTEM += rootagent
LOG_SYSTEM += init.qcom.rootagent.sh
LOG_SYSTEM += dynamic_debug_mask.cfg

# LogKit 3
QTI_LOGKIT_BINS := LKCore
QTI_LOGKIT_BINS += qti-logkit
QTI_LOGKIT_BINS += libUserAgent
QTI_LOGKIT_BINS += access-qcom-logkit
QTI_LOGKIT_BINS += qcom_logkit.xml
QTI_LOGKIT_BINS += qti_logkit_config.xml
QTI_LOGKIT_BINS += qti_logkit_command.xml
QTI_LOGKIT_BINS += goldenlogmask.dmc
QTI_LOGKIT_BINS += gnsslocationlogging.dmc
QTI_LOGKIT_BINS += audiobasic.dmc
QTI_LOGKIT_BINS += IMS.dmc
QTI_LOGKIT_BINS += default.png
QTI_LOGKIT_BINS += bluetooth.png
QTI_LOGKIT_BINS += IMS.png
QTI_LOGKIT_BINS += liblz4

#CSK
CSK := Csk
CSK += CskServer

#CSM
CSM := Csm
CSM += libcsm_data

#scr modules
SCR_MODULES := bg_daemon
SCR_MODULES += vendor.google_clockwork.sidekickgraphics@1.0-impl
SCR_MODULES += vendor.qti.hardware.sidekickgraphics@1.0-impl
SCR_MODULES += vendor.google_clockwork.sidekickgraphics@1.0-service
SCR_MODULES += vendor.google_clockwork.sidekickgraphics@1.0-service.rc
SCR_MODULES += skgtest

#POWER_OFF_ALARM
POWER_OFF_ALARM := vendor.qti.hardware.alarm@1.0
POWER_OFF_ALARM += vendor.qti.hardware.alarm-V1.0-java
POWER_OFF_ALARM += PowerOffAlarm
POWER_OFF_ALARM += vendor-qti-hardware-alarm.xml
POWER_OFF_ALARM += vendor.qti.hardware.alarm@1.0-impl
POWER_OFF_ALARM += vendor.qti.hardware.alarm@1.0-service
POWER_OFF_ALARM += power_off_alarm

#SECURE QTI_UTILS
QTIUTILS := libqti-utils

#PASR HAL and APP
PASRHAL := vendor.qti.power.pasrmanager@1.0-service
PASRHAL += vendor.qti.power.pasrmanager@1.0-service.rc
PASRHAL += vendor.qti.power.pasrmanager@1.0-impl
PASRHAL += pasrservice
#Property to enable/disable PASR
PRODUCT_PROPERTY_OVERRIDES += vendor.power.pasr.enabled=true

#capability config store
CCS_HIDL := vendor.qti.hardware.capabilityconfigstore@1.0
CCS_HIDL += vendor.qti.hardware.capabilityconfigstore-V1.0-java

ifneq ($(TARGET_HAS_LOW_RAM),true)
# QSPM-hal components
QSPM_HAL := vendor.qti.qspmhal@1.0.vendor
QSPM_HAL += vendor.qti.qspmhal@1.0-impl
QSPM_HAL += vendor.qti.qspmhal@1.0-service
QSPM_HAL += vendor.qti.qspmhal@1.0-service.rc
QSPM_HAL := vendor.qti.qspmhal.vendor
QSPM_HAL += vendor.qti.qspmhal-impl
QSPM_HAL += vendor.qti.qspmhal-service
QSPM_HAL += vendor.qti.qspmhal-service.rc

# QSPM-svc components
QSPM_SVC := libqspmsvc
QSPM_SVC += libtrigger-handler
QSPM_SVC += libthermalclient.qti
QSPM_SVC += vendor.qti.qspmhal@1.0
QSPM_SVC += qspmsvc
QSPM_SVC += libupdateprof.qti
QSPM_SVC += qspmsvc.rc
QSPM_SVC += libqspm-mem-utils
endif #TARGET_HAS_LOW_RAM

# Crosvm Internal
CROSVM_INTERNAL := qcrosvm qcrosvm.policy
CROSVM_INTERNAL += libbase_rust libcrosvm libdevices libdisk liblibc libvm_memory libsys_util libmmio libadapter libsync_rust
CROSVM_INTERNAL += libsyscall_defines libandroid_logger liblog_rust libminijail_rust libsimplelog libtermcolor

ifeq ($(TARGET_BOARD_PLATFORM),qssi)
 ifeq ($(TARGET_BOARD_SUFFIX),_64)
  CROSVM_INTERNAL += vendor.qti.qvirt-service vendor.qti.qvirt-service.rc vendor.qti.qvirt-V1-ndk.product
  CROSVM_INTERNAL += vendor.qti.qvirt-service_rs vendor.qti.qvirt-service_rs.rc vendor.qti.qvirt-V2-rust.product
  CROSVM_INTERNAL += liblogger.product liblog_rust.product libbinder_rs.product libnix.product libserde.product
  CROSVM_INTERNAL += libserde_json.product liblibc.product librustutils.product vendor.qti.qvirtvendor-V1-rust.product
 else
  CROSVM_INTERNAL += qvirtmgr qvirtmgr.rc qvirtmgr.json
 endif
endif

CROSVM_INTERNAL += vendor.qti.qvirtvendor-service vendor.qti.qvirtvendor-service.rc vendor.qti.qvirtvendor-V1-ndk
CROSVM_INTERNAL_DEBUG := qvirt-testclient qvirt-testclient.rc
CROSVM_INTERNAL_DEBUG += qvirtservice_testclient

ifneq ($(TARGET_BOARD_PLATFORM),taro)
CROSVM_INTERNAL += qvirtmgr-vndr.json
endif

#qguard
QGUARD := qguard
QGUARD += qguard.json

#smart_trace
SMART_TRACE := tracing_config.sh
SMART_TRACE += smart_trace.rc

USBGADGET := DeviceAsWebcam

PRODUCT_PACKAGES += $(NNHAL)
PRODUCT_PACKAGES += $(ADC)
PRODUCT_PACKAGES += $(ALLJOYN)
PRODUCT_PACKAGES += $(AIVPLAY)
PRODUCT_PACKAGES += $(ANT)
PRODUCT_PACKAGES += $(ANTITHEFTDEMO)
PRODUCT_PACKAGES += $(BACKUP_AGENT)
PRODUCT_PACKAGES += $(BATTERY_CHARGING)
PRODUCT_PACKAGES += $(BT)
PRODUCT_PACKAGES += $(CAMERAHAWK_APPS)
PRODUCT_PACKAGES += $(CAMERAHAWK_WHIP_COMMON_LIB)
PRODUCT_PACKAGES += $(CCID)
PRODUCT_PACKAGES += $(CHARGER_MONITOR)
PRODUCT_PACKAGES += $(CONNECTION_SECURITY)
PRODUCT_PACKAGES += $(CNE)
PRODUCT_PACKAGES += $(CACERT)
PRODUCT_PACKAGES += $(CRASH_LOGGER)
PRODUCT_PACKAGES += $(DATA)
PRODUCT_PACKAGES += $(DIAG)
PRODUCT_PACKAGES += $(ENERGY_AWARENESS)
#PRODUCT_PACKAGES += $(ENTERPRISE_SECURITY)
PRODUCT_PACKAGES += $(FINGERPRINT)
PRODUCT_PACKAGES += $(FLASH)
PRODUCT_PACKAGES += $(FM)
PRODUCT_PACKAGES += $(FOTA)
PRODUCT_PACKAGES += $(FTM)
PRODUCT_PACKAGES += $(GPQESE)
PRODUCT_PACKAGES += $(GSMA_NFC)
PRODUCT_PACKAGES += $(HBTP)
PRODUCT_PACKAGES += $(HVDCP_OPTI)
PRODUCT_PACKAGES += $(HY11_HY22_diff)
PRODUCT_PACKAGES += $(BTSAR)

#Ims modules are intentionally set to optional
#so that they are not packaged as part of system image.
#Hence removing them from PRODUCT_PACKAGES list
#PRODUCT_PACKAGES += $(IMS)
PRODUCT_PACKAGES += $(IMS_VT)
PRODUCT_PACKAGES += $(IMS_RCS)
PRODUCT_PACKAGES += $(IMS_NEWARCH)
PRODUCT_PACKAGES += $(IMS_REGMGR)
PRODUCT_PACKAGES += $(INTERFACE_PERMISSIONS)
PRODUCT_PACKAGES += $(IQAGENT)
PRODUCT_PACKAGES += $(IWLAN)
PRODUCT_PACKAGES += $(LATENCY)
PRODUCT_PACKAGES += $(MARE)

ifeq ($(BUILD_FUSION_HANDLERS),true)
PRODUCT_PACKAGES += $(MDM_HELPER)
PRODUCT_PACKAGES += $(MDM_HELPER_PROXY)
endif

PRODUCT_PACKAGES += $(MM_CORE)
PRODUCT_PACKAGES += $(MM_COLOR_CONVERSION)
PRODUCT_PACKAGES += $(MM_COLOR_CONVERTOR)
PRODUCT_PACKAGES += $(MM_DRMPLAY)
PRODUCT_PACKAGES += $(MM_GESTURES)
PRODUCT_PACKAGES += $(MM_GRAPHICS)
PRODUCT_PACKAGES += $(MM_HTTP)
PRODUCT_PACKAGES += $(MM_STA)
PRODUCT_PACKAGES += $(MM_OSAL)
PRODUCT_PACKAGES += $(MM_PARSER)
PRODUCT_PACKAGES += $(MM_QSM)
PRODUCT_PACKAGES += $(MM_STEREOLIB)
PRODUCT_PACKAGES += $(MM_STILL)
PRODUCT_PACKAGES += $(MM_VIDEO)
PRODUCT_PACKAGES += $(MM_VPU)
PRODUCT_PACKAGES += $(MODEM_APIS)
PRODUCT_PACKAGES += $(MODEM_API_TEST)
PRODUCT_PACKAGES += $(MPDCVS)
PRODUCT_PACKAGES += $(MPQ_COMMAND_IF)
PRODUCT_PACKAGES += $(MPQ_PLATFORM)
PRODUCT_PACKAGES += $(MXT_CFG)
PRODUCT_PACKAGES += $(N_SMUX)
PRODUCT_PACKAGES += $(NQ_NFC_PROP)
PRODUCT_PACKAGES += $(OEM_SERVICES)
PRODUCT_PACKAGES += $(ONCRPC)
PRODUCT_PACKAGES += $(PD_LOCATER)
PRODUCT_PACKAGES += $(PERMGR)
PRODUCT_PACKAGES += $(PERFECT365_APPS)
PRODUCT_PACKAGES += $(PLAYREADY)
PRODUCT_PACKAGES += $(PROFILER)
PRODUCT_PACKAGES += $(QCHAT)
PRODUCT_PACKAGES += $(QCRIL)
PRODUCT_PACKAGES += $(QCNVITEM)
PRODUCT_PACKAGES += $(QMI)
PRODUCT_PACKAGES += $(QOSMGR)
PRODUCT_PACKAGES += $(QUICKCHARGE)
PRODUCT_PACKAGES += $(QVR)
PRODUCT_PACKAGES += $(XRCOMM)
PRODUCT_PACKAGES += $(REMOTEFS)
PRODUCT_PACKAGES += $(RIDL_BINS)
#PRODUCT_PACKAGES += $(RFS_ACCESS)
PRODUCT_PACKAGES += $(RNGD)
PRODUCT_PACKAGES += $(SECUREMSM)
PRODUCT_PACKAGES += $(SENSORS)
PRODUCT_PACKAGES += $(SCS_PROP)
PRODUCT_PACKAGES += $(SS_RESTART)
PRODUCT_PACKAGES += $(SVGT)
PRODUCT_PACKAGES += $(SW2DTO3D)
PRODUCT_PACKAGES += $(SYSTEM_HEALTH_MONITOR)
PRODUCT_PACKAGES += $(QTI_LOGKIT_BINS)
PRODUCT_PACKAGES += $(TFTP)
PRODUCT_PACKAGES += $(TIME_SERVICES)
PRODUCT_PACKAGES += $(TINYXML)
PRODUCT_PACKAGES += $(TINYXML2)
PRODUCT_PACKAGES += $(TREPN)
PRODUCT_PACKAGES += $(TOUCH_FUSION)
PRODUCT_PACKAGES += $(TS_TOOLS)
PRODUCT_PACKAGES += $(TTSP_FW)
PRODUCT_PACKAGES += $(TV_TUNER)
PRODUCT_PACKAGES += $(ULTRASOUND_COMMON)
PRODUCT_PACKAGES += $(ULTRASOUND_PROXIMITY)
PRODUCT_PACKAGES += $(USB)
PRODUCT_PACKAGES += $(USB_UICC_CLIENT)
PRODUCT_PACKAGES += $(VEHICLE_NETWORKS)
PRODUCT_PACKAGES += $(VM_BMS)
PRODUCT_PACKAGES += $(VPP)
PRODUCT_PACKAGES += $(WEBKIT)
PRODUCT_PACKAGES += $(WIGIG)
PRODUCT_PACKAGES += $(WHIP_APPS)
ifeq ($(TARGET_USES_QMAA_OVERRIDE_WLAN), true)
PRODUCT_PACKAGES += $(WLAN)
endif
PRODUCT_PACKAGES += $(BT_TEL)
PRODUCT_PACKAGES += $(LOG_SYSTEM)
PRODUCT_PACKAGES += $(QRD_CALENDAR_APPS)
PRODUCT_PACKAGES += $(CSM)
PRODUCT_PACKAGES += $(POWER_OFF_ALARM)
PRODUCT_PACKAGES += $(CCS_HIDL)
ifneq ($(TARGET_HAS_LOW_RAM),true)
PRODUCT_PACKAGES += $(QSPM_SVC)
endif

# Enable for non go targets
ifneq ($(TARGET_SUPPORTS_WEAR_OS), true)
ifneq ($(TARGET_BOARD_SUFFIX),_32go)
PRODUCT_PACKAGES += $(CROSVM_INTERNAL)
PRODUCT_PACKAGES_DEBUG += $(CROSVM_INTERNAL_DEBUG)
endif
endif

ifeq ($(TARGET_USES_QSPM), true)
ifneq ($(TARGET_HAS_LOW_RAM),true)
PRODUCT_PACKAGES += $(QSPM_HAL)
endif
endif

ifeq ($(TARGET_SUPPORTS_WEARABLES), true)
PRODUCT_PACKAGES += $(SCR_MODULES)
endif

PRODUCT_PACKAGES += $(QTIUTILS)
PRODUCT_PACKAGES += $(NPU)
PRODUCT_PACKAGES += vndk-lib-hack


ifneq (,$(filter userdebug eng, $(TARGET_BUILD_VARIANT)))
  PRODUCT_PACKAGES += $(SMART_TRACE)
endif

PRODUCT_PACKAGES_ENG += cneapitest
PRODUCT_PACKAGES += $(USBGADGET)

# Each line here corresponds to a debug LOCAL_MODULE built by
# Android.mk(s) in the proprietary projects. Where project
# corresponds to the vars here in CAPs.

# These modules are tagged with LOCAL_MODULE_TAGS := optional.
# They would not be installed into the image unless
# they are listed here explicitly.

ifneq ($(TARGET_HAS_LOW_RAM),true)
#QSPM DBG
QSPM_DBG := clientTestqspmsvc
QSPM_DBG += clientTestqspmhal
QSPM_DBG += clientTestqspmhalCpu
endif #TARGET_HAS_LOW_RAM

#BT_DBG
BT_DBG := AR3002_PS_ASIC
BT_DBG += AR3002_RamPatch

#CNE_DBG
CNE_DBG := AndsfParser
CNE_DBG += cnelogger
CNE_DBG += swimcudpclient
CNE_DBG += swimstcpclient
CNE_DBG += swimtcpclient
CNE_DBG += swimudpclient
CNE_DBG += test2client


#COMMON_DBG
COMMON_DBG := remote_apis_verify

#DATA_DBG
DATA_DBG := libCommandSvc
DATA_DBG += libdsnetutils

#DIAG_DBG
DIAG_DBG := libdiag
DIAG_DBG += diag_buffering_test
DIAG_DBG += diag_callback_client
DIAG_DBG += diag_dci_client

#FM_DBG
FM_DBG := hal_ss_test_manual

#HBTP_DBG
HBTP_DBG := com.qualcomm.qti.improvetouch.testapp

#IMS_RCS_DBG
IMS_RCS_DBG := PresenceApp
IMS_RCS_DBG += ConnectionManagerTestApp
IMS_RCS_DBG += DCTestApp

#PASR
PRODUCT_PACKAGES += vendor.qti.memory.pasrmanager@1.0.vendor
PRODUCT_PACKAGES += libpsi.vendor
#Enable active mode PASR
ifeq ($(TARGET_BOARD_PLATFORM), holi)
PRODUCT_PROPERTY_OVERRIDES += vendor.pasr.activemode.enabled=false
else
PRODUCT_PROPERTY_OVERRIDES += vendor.pasr.activemode.enabled=true
endif

#KERNEL_DBG
KERNEL_TEST_DBG := cpuhotplug_test.sh
KERNEL_TEST_DBG += cputest.sh
KERNEL_TEST_DBG += msm_uart_test
KERNEL_TEST_DBG += probe_test.sh
KERNEL_TEST_DBG += msm_uart_test.sh
KERNEL_TEST_DBG += uarttest.sh
KERNEL_TEST_DBG += clocksourcetest.sh
KERNEL_TEST_DBG += clock_test.sh
KERNEL_TEST_DBG += socinfotest.sh
KERNEL_TEST_DBG += timertest.sh
KERNEL_TEST_DBG += vfp.sh
KERNEL_TEST_DBG += vfptest
KERNEL_TEST_DBG += pctest
KERNEL_TEST_DBG += modem_test
KERNEL_TEST_DBG += pc-compound-test.sh
KERNEL_TEST_DBG += msm_sps_test
KERNEL_TEST_DBG += cacheflush
KERNEL_TEST_DBG += cacheflush.sh
KERNEL_TEST_DBG += _cacheflush.sh
KERNEL_TEST_DBG += loop.sh
KERNEL_TEST_DBG += clk_test.sh
KERNEL_TEST_DBG += cpufreq_test.sh
KERNEL_TEST_DBG += fbtest
KERNEL_TEST_DBG += fbtest.sh
KERNEL_TEST_DBG += geoinfo_flash
KERNEL_TEST_DBG += gpio_lib.conf
KERNEL_TEST_DBG += gpio_lib.sh
KERNEL_TEST_DBG += gpio_tlmm.sh
KERNEL_TEST_DBG += gpio_tlmm.conf
KERNEL_TEST_DBG += i2c-msm-test
KERNEL_TEST_DBG += i2c-msm-test.sh
KERNEL_TEST_DBG += irq_test.sh
KERNEL_TEST_DBG += kgsl_test
KERNEL_TEST_DBG += mpp_test.sh
KERNEL_TEST_DBG += msm_adc_test
KERNEL_TEST_DBG += msm_dma
KERNEL_TEST_DBG += msm_dma.sh
KERNEL_TEST_DBG += mtd_driver_test.sh
KERNEL_TEST_DBG += mtd_test.sh
KERNEL_TEST_DBG += mtd_yaffs2_test.sh
KERNEL_TEST_DBG += AR_LUT_1_0_B0
KERNEL_TEST_DBG += AR_LUT_1_0_B
KERNEL_TEST_DBG += AR_LUT_1_0_G0
KERNEL_TEST_DBG += AR_LUT_1_0_G
KERNEL_TEST_DBG += AR_LUT_1_0_R0
KERNEL_TEST_DBG += r_only_igc
KERNEL_TEST_DBG += SanityCfg.cfg
KERNEL_TEST_DBG += qcedev_test
KERNEL_TEST_DBG += qcedev_test.sh
KERNEL_TEST_DBG += yv12_qcif.yuv
KERNEL_TEST_DBG += rotator
KERNEL_TEST_DBG += rtc_test
KERNEL_TEST_DBG += sd_test.sh
KERNEL_TEST_DBG += smd_pkt_loopback_test
KERNEL_TEST_DBG += smem_log_test
KERNEL_TEST_DBG += smd_tty_loopback_test
KERNEL_TEST_DBG += spidevtest
KERNEL_TEST_DBG += spidevtest.sh
KERNEL_TEST_DBG += spitest
KERNEL_TEST_DBG += spitest.sh
KERNEL_TEST_DBG += spiethernettest.sh
KERNEL_TEST_DBG += test_env_setup.sh
KERNEL_TEST_DBG += vreg_test.sh

#KS_DBG
KS_DBG := efsks
KS_DBG += ks
KS_DBG += qcks

#MM_CORE_DBG
MM_CORE_DBG := libmm-abl
MM_CORE_DBG += PPPreference
MM_CORE_DBG += ADService

#MM_VIDEO_DBG
MM_VIDEO_DBG := mm-adspsvc-test

#PERFDUMP_APP_DBG
PERFDUMP_APP_DBG := Perfdump

#QCOM_SETTINGS
QCOM_SETTINGS_DBG := QualcommSettings
QCOM_SETTINGS_DBG += libDiagService
QCOM_SETTINGS_DBG += QTIDiagServices

#QCOMSYSDAEMON
QCOMSYSDAEMON_DBG := qcom-system-daemon

##FFBM_DIAG_PLUGIN_LIB_DBG
FFBM_DIAG_PLUGIN_LIB_DBG := libffbmdiagplugin

#QMI
QMI_DBG := qmi_fw.conf
QMI_DBG += qmi_simple_ril_test
QMI_DBG += qmi_ping_clnt_test_0000
QMI_DBG += qmi_ping_clnt_test_0001
QMI_DBG += qmi_ping_clnt_test_1000
QMI_DBG += qmi_ping_clnt_test_1001
QMI_DBG += qmi_ping_clnt_test_2000
QMI_DBG += qmi_ping_svc
QMI_DBG += qmi_ping_test
QMI_DBG += qmi_test_service_clnt_test_0000
QMI_DBG += qmi_test_service_clnt_test_0001
QMI_DBG += qmi_test_service_clnt_test_1000
QMI_DBG += qmi_test_service_clnt_test_1001
QMI_DBG += qmi_test_service_clnt_test_2000
QMI_DBG += qmi_test_service_clnt_test_3000
QMI_DBG += qmi_test_service_clnt_test_3001
QMI_DBG += qmi_test_service_clnt_test_4000
QMI_DBG += qmi_test_service_clnt_test_4001
QMI_DBG += qmi_test_service_start_svc
QMI_DBG += qmi_test_service_test

#SECUREMSM_DBG
SECUREMSM_DBG := bintestapp_client
SECUREMSM_DBG += libcontentcopy
SECUREMSM_DBG += prdrmkeyprov
SECUREMSM_DBG += oemprtest
SECUREMSM_DBG += oemwvgtest
SECUREMSM_DBG += PlayreadySamplePlayer
SECUREMSM_DBG += playreadygtest
SECUREMSM_DBG += playreadygtest_error
SECUREMSM_DBG += playreadygtest_cryptoplugin
SECUREMSM_DBG += hdcpsrmgtest
SECUREMSM_DBG += widevinegtest
SECUREMSM_DBG += hdcp2p2_compliance_gtest
SECUREMSM_DBG += seemp_health_client_test
SECUREMSM_DBG += SeempHealthTestApp


#THERMAL_ENGINE_DBG
THERMAL_ENGINE_DBG := libthermalioctl

#TIME_SERVICES_DBG
TIME_SERVICES_DBG := libtime_genoff

#VPP_DBG
VPP_DBG := vpplibraryunittest
VPP_DBG += vpplibraryfunctionaltest
VPP_DBG += libvpptestutils
VPP_DBG += vppipcunittest

#WEBKIT_DBG
WEBKIT_DBG := pageload_proc_plugin
WEBKIT_DBG += modemwarmup.xml

#WLAN_DBG
WLAN_DBG := abtfilt
WLAN_DBG += artagent
WLAN_DBG += ar6004_wlan.conf
WLAN_DBG += ar6004_fw_12
WLAN_DBG += ar6004_bdata_12
WLAN_DBG += ar6004_usb_fw_13
WLAN_DBG += ar6004_usb_bdata_13
WLAN_DBG += ar6004_sdio_fw_13
WLAN_DBG += ar6004_sdio_bdata_13
WLAN_DBG += ar6004_usb_fw_ext_13
WLAN_DBG += ar6004_fw_30
WLAN_DBG += ar6004_usb_bdata_30
WLAN_DBG += ar6004_sdio_bdata_30
WLAN_DBG += proprietary_pronto_wlan.ko
WLAN_DBG += bdata.bin
WLAN_DBG += fw.ram.bin
WLAN_DBG += bdata.bin_sdio
WLAN_DBG += bdata.bin_usb
WLAN_DBG += fw_ext.ram.bin
WLAN_DBG += fw.ram.bin_sdio
WLAN_DBG += fw.ram.bin_usb

#QUICKBOOT
QUICKBOOT := QuickBoot

#SNAPDRAGON_SDK

SNAPDRAGON_SDK := com.qualcomm.snapdragon.sdk.deviceinfo.DeviceInfo
SNAPDRAGON_SDK += com.qualcomm.snapdragon.sdk.deviceinfo.DeviceInfoHelper
SNAPDRAGON_SDK += com.qualcomm.snapdragon.sdk.face.FacialProcessing
SNAPDRAGON_SDK += com.qualcomm.snapdragon.sdk.face.FacialProcessingHelper
SNAPDRAGON_SDK += libfacialproc_jni

PRODUCT_PACKAGES_DEBUG += $(SNAPDRAGON_SDK)

PRODUCT_PACKAGES_DEBUG += $(BT_DBG)
PRODUCT_PACKAGES_DEBUG += $(CNE_DBG)
PRODUCT_PACKAGES_DEBUG += $(COMMON_DBG)
PRODUCT_PACKAGES_DEBUG += $(DATA_DBG)
PRODUCT_PACKAGES_DEBUG += $(DIAG_DBG)
PRODUCT_PACKAGES_DEBUG += $(FM_DBG)
PRODUCT_PACKAGES_DEBUG += $(HBTP_DBG)
PRODUCT_PACKAGES_DEBUG += $(HY11_HY22_diff_debug)
PRODUCT_PACKAGES_DEBUG += $(IMS_RCS_DBG)
PRODUCT_PACKAGES_DEBUG += $(KERNEL_TEST_DBG)
PRODUCT_PACKAGES_DEBUG += $(KS_DBG)
PRODUCT_PACKAGES_DEBUG += $(MM_CORE_DBG)
PRODUCT_PACKAGES_DEBUG += $(MM_VIDEO_DBG)
PRODUCT_PACKAGES_DEBUG += $(PERFDUMP_APP_DBG)
PRODUCT_PACKAGES_DEBUG += $(QCAT_UNBUFFERED)
PRODUCT_PACKAGES_DEBUG += $(QCOM_SETTINGS_DBG)
PRODUCT_PACKAGES_DEBUG += $(QMI_DBG)
PRODUCT_PACKAGES_DEBUG += $(QCOMSYSDAEMON_DBG)
PRODUCT_PACKAGES_DEBUG += $(SECUREMSM_DBG)
PRODUCT_PACKAGES_DEBUG += $(THERMAL_ENGINE_DBG)
PRODUCT_PACKAGES_DEBUG += $(TIME_SERVICES_DBG)
PRODUCT_PACKAGES_DEBUG += $(VPP_DBG)
PRODUCT_PACKAGES_DEBUG += $(WEBKIT_DBG)
ifeq ($(TARGET_USES_QMAA_OVERRIDE_WLAN), true)
PRODUCT_PACKAGES_DEBUG += $(WLAN_DBG)
endif
PRODUCT_PACKAGES_DEBUG += $(QUICKBOOT)
PRODUCT_PACKAGES_DEBUG += $(CSK)
ifneq ($(TARGET_HAS_LOW_RAM),true)
PRODUCT_PACKAGES_DEBUG += $(QSPM_DBG)
endif
PRODUCT_PACKAGES_DEBUG += SeempAPIlibTest
PRODUCT_PACKAGES_DEBUG += QFPCalibration
PRODUCT_PACKAGES_DEBUG += $(FFBM_DIAG_PLUGIN_LIB_DBG)

## VNDK-LIBS
## Below libs are added explicitly in PRODUCT PACKAGES beacuse
## server(vendor) code is shipped as prebuilt and in customer
## variant no module is depends  on these libraries and
## compilation of vendor variant of these libraries will be
## skipped.
## Modules shiping HAL & Files with vendor module as prebuiilt
## should add entry here.

VNDK_LIBS := vendor.display.color@1.0.vendor
VNDK_LIBS += vendor.display.color@1.1.vendor
VNDK_LIBS += vendor.display.color@1.2.vendor
VNDK_LIBS += vendor.display.postproc@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.data.latency@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.fingerprint@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.iop@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.qdutils_disp@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.qteeconnector@1.0.vendor
VNDK_LIBS += com.qualcomm.qti.imscmservice@1.0.vendor
VNDK_LIBS += com.qualcomm.qti.imscmservice@2.0.vendor
VNDK_LIBS += com.qualcomm.qti.imscmservice@2.1.vendor
VNDK_LIBS += vendor.qti.ims.callinfo@1.0.vendor
VNDK_LIBS += com.qualcomm.qti.uceservice@2.0.vendor
VNDK_LIBS += vendor.qti.voiceprint@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.wigig.netperftuner@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.data.dynamicdds@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.data.connection@1.0.vendor
VNDK_LIBS += vendor.qti.ims.rcsconfig@1.0.vendor
VNDK_LIBS += vendor.qti.latency@2.0.vendor
VNDK_LIBS += $(patsubst vendor.qti.gnss%,vendor.qti.gnss%.vendor,$(LOCHIDL))
VNDK_LIBS += vendor.qti.power.pasrmanager@1.0.vendor
VNDK_LIBS += vendor.qti.esepowermanager@1.0.vendor
VNDK_LIBS += vendor.qti.hardware.vpp@1.1.vendor
VNDK_LIBS += libavservices_minijail.vendor

PRODUCT_PACKAGES += $(VNDK_LIBS)

endif

# CTS testcase check for security patch compliance date
# currently this is been defined as below but need to be modifed
# based on security issues been addressed.
BOOT_SECURITY_PATCH ?= $(PLATFORM_SECURITY_PATCH)
ifeq ($(ENABLE_VENDOR_IMAGE), true)
VENDOR_SECURITY_PATCH ?= $(PLATFORM_SECURITY_PATCH)
endif

# KEYSTONE(I6f9ed3e8a105bb3fad2103c174263d71daf4f8a2,b/290833247)
PRODUCT_HOST_PACKAGES += qiifa_py3

# UFS init scripts
PRODUCT_PACKAGES += init.qti.ufs.rc
# additional debugging on userdebug/eng builds
ifneq (,$(filter userdebug eng, $(TARGET_BUILD_VARIANT)))
  PRODUCT_PACKAGES += init.qti.ufs.debug.sh
  PRODUCT_PACKAGES += $(QGUARD)
endif

ifneq ($(filter $(TARGET_BOARD_PLATFORM), taro lahaina kona holi bengal blair parrot),$(TARGET_BOARD_PLATFORM))
ifneq ($(wildcard vendor/google/qcom-chre),)
  QTI_CHRE_ENABLE := true
endif
endif

ifeq ($(QTI_CHRE_ENABLE), true)

PRODUCT_SOONG_NAMESPACES += vendor/google/qcom-chre/prebuilts/aosp_hexagonv66_qsh_debug
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.context_hub.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.context_hub.xml
PRODUCT_PACKAGES += chre_plus_nanoapps
PRODUCT_PACKAGES += android.hardware.contexthub-service.qmi

endif

# Initial Package Stopped States
# Override default behavior for system apps
# Allows system apps to receive broadcasts before started by users.
ifeq ($(TARGET_BOARD_PLATFORM),qssi)
  PRODUCT_PACKAGES += qti-initial-package-stopped-states.xml
endif
