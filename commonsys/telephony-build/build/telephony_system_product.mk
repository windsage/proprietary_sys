

#Disable telephony-ext temporarly here, its already added in qssi,
# once all changes merged enable it again
#PRODUCT_BOOT_JARS += telephony-ext
ifneq ($(TARGET_SUPPORTS_WEARABLES),true)
  PRODUCT_SYSTEM_EXT_PROPERTIES += ro.telephony.sim_slots.count=2
endif
PRODUCT_SYSTEM_EXT_PROPERTIES += telephony.active_modems.max_count=2

PRODUCT_PACKAGES += \
    telephony-ext \
    ims-ext-common \
    ims_ext_common.xml \
    qti_permissions.xml \
    qti_telephony_system_packages_config.xml

ifneq ($(TARGET_NO_TELEPHONY), true)
PRODUCT_PACKAGES += \
    CellBroadcastReceiver \
    Stk
endif #TARGET_NO_TELEPHONY

XML_CONF_PATH := $(QCPATH_COMMONSYS)/telephony-apps/etc

  #Add HY22 support
  HY22_XML_CONF_PATH := $(QC_PROP_ROOT)/prebuilt_HY22/target/product/$(PREBUILT_BOARD_PLATFORM_DIR)/$(TARGET_COPY_OUT_SYSTEM_EXT)/etc
  HY22_XML_CONF_PATH_PRODUCT := $(QC_PROP_ROOT)/prebuilt_HY22/target/product/$(PREBUILT_BOARD_PLATFORM_DIR)/product/etc
  ifneq ($(wildcard $(HY22_XML_CONF_PATH_PRODUCT)),)
    PRODUCT_COPY_FILES += $(HY22_XML_CONF_PATH_PRODUCT)/apns-conf.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/apns-conf.xml
  endif
  ifneq ($(wildcard $(HY22_XML_CONF_PATH)),)
    ifneq ($(wildcard $(HY22_XML_CONF_PATH)/permissions/qti_libpermissions.xml),)
      PRODUCT_COPY_FILES += $(HY22_XML_CONF_PATH)/permissions/qti_libpermissions.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/qti_libpermissions.xml
    endif
  endif

  ifneq ($(wildcard $(XML_CONF_PATH)),)
    PRODUCT_COPY_FILES += $(XML_CONF_PATH)/apns-conf.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/apns-conf.xml
    PRODUCT_COPY_FILES += $(XML_CONF_PATH)/qti_libpermissions.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/qti_libpermissions.xml
  endif

HY11_HY22_diff += qti_libpermissions.xml

HY11_HY22_diff_debug += radioconfig
HY11_HY22_diff_debug += radioconfig.xml

PRODUCT_PACKAGES += $(HY11_HY22_diff)
PRODUCT_PACKAGES_DEBUG += $(HY11_HY22_diff_debug)



# IMS Telephony Libs
IMS_TEL := ims.xml
IMS_TEL += imslibrary

QCRIL += android.hardware.radio@1.1
QCRIL += android.hardware.radio@1.2
QCRIL += android.hardware.radio@1.0
QCRIL += android.hardware.radio.config@1.0
QCRIL += android.hardware.radio.deprecated@1.0
QCRIL += android.hardware.secure_element@1.0

#TELEPHONY_APPS_DBG
TELEPHONY_APPS_DBG := Presence
TELEPHONY_APPS_DBG += qcom_qmi.xml

ifneq ($(TARGET_NO_TELEPHONY), true)
TELEPHONY_APPS_DBG += EuiccResource
TELEPHONY_APPS += ConferenceDialer
TELEPHONY_APPS += libimscamera_jni
TELEPHONY_APPS += libimsmedia_jni
TELEPHONY_APPS += datastatusnotification
TELEPHONY_APPS += QtiTelephonyService
TELEPHONY_APPS += telephonyservice.xml
TELEPHONY_APPS += uimremoteclientlibrary
TELEPHONY_APPS += uimremoteclient
TELEPHONY_APPS += uimremoteclient.xml
TELEPHONY_APPS += uimremoteserverlibrary
TELEPHONY_APPS += uimremoteserver
TELEPHONY_APPS += uimremoteserver.xml
TELEPHONY_APPS += qcrilmsgtunnel
TELEPHONY_APPS += qcrilhook
TELEPHONY_APPS += qcrilhook.xml
TELEPHONY_APPS += QtiDialer
TELEPHONY_APPS += ConfURIDialer
TELEPHONY_APPS += QtiTelephonySettings
TELEPHONY_APPS += uimlpalibrary
TELEPHONY_APPS += lpa.xml
TELEPHONY_APPS += libjni_aidl_service
IMS_TEL += ims
IMS_TEL += imssettings
QTI_TELEPHONY_FWK += QtiTelephonyServicelibrary
ifneq ($(TARGET_HAS_LOW_RAM),true)
TELEPHONY_APPS += atfwd
TELEPHONY_APPS += AtFwd2
TELEPHONY_APPS += uimremotesimlocklibrary
TELEPHONY_APPS += RemoteSimlock.xml
TELEPHONY_APPS += remoteSimLockAuthentication
TELEPHONY_APPS += remotesimlockmanagerlibrary
TELEPHONY_APPS += RemoteSimlockManager.xml
TELEPHONY_APPS += uimgbalibrary
TELEPHONY_APPS += UimGba.xml
TELEPHONY_APPS += uimgbamanagerlibrary
TELEPHONY_APPS += UimGbaManager.xml
ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS), true)
TELEPHONY_APPS += DeviceInfo
TELEPHONY_APPS += xdivert
TELEPHONY_APPS_DBG += ModemTestMode
TELEPHONY_APPS += SimSettings
TELEPHONY_APPS += RideModeAudio
ifeq (0,1)
TELEPHONY_APPS += saminterfacelibrary
TELEPHONY_APPS += sammanagerlibrary
TELEPHONY_APPS += sam
TELEPHONY_APPS += sammanager.xml
TELEPHONY_APPS += saminterface.xml
endif
endif
TELEPHONY_APPS += DeviceStatisticsService
endif
ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS), true)
TELEPHONY_APPS += QtiTelephony
else
ifeq ($(TARGET_SUPPORTS_WEARABLES),true) # Include QtiTelephonyWear for LW Wearables, use QtiTelephony for LAW Wearables and mobile SPs
TELEPHONY_APPS += QtiTelephonyWear
endif #TARGET_SUPPORTS_WEARABLES
endif #TARGET_FWK_SUPPORTS_FULL_VALUEADDS

#Qc extended functionality of android telephony
ifneq ($(TARGET_SUPPORTS_WEAR_OS),true)
QTI_TELEPHONY_FWK := qti-telephony-common
endif
endif #TARGET_NO_TELEPHONY

TELEPHONY_APPS += extphonelib
TELEPHONY_APPS += extphonelib-product
TELEPHONY_APPS += extphonelib.xml
TELEPHONY_APPS += extphonelib_product.xml

#Qc extended telephony framework resource APK
QTI_TELEPHONY_RES := telresources

ifneq ($(TARGET_NO_TELEPHONY), true)
HIDL_WRAPPER := qti-telephony-hidl-wrapper
HIDL_WRAPPER += qti_telephony_hidl_wrapper.xml
HIDL_WRAPPER += qti-telephony-hidl-wrapper-prd
HIDL_WRAPPER += qti_telephony_hidl_wrapper_prd.xml

QTI_TELEPHONY_UTILS := qti-telephony-utils
QTI_TELEPHONY_UTILS += qti_telephony_utils.xml
QTI_TELEPHONY_UTILS += qti-telephony-utils-prd
QTI_TELEPHONY_UTILS += qti_telephony_utils_prd.xml
endif # TARGET_NO_TELEPHONY

PRODUCT_PACKAGES += $(IMS_TEL)
PRODUCT_PACKAGES += $(QCRIL)
PRODUCT_PACKAGES += $(TELEPHONY_APPS)
ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS), true)
PRODUCT_PACKAGES += $(QTI_TELEPHONY_FWK)
endif
PRODUCT_PACKAGES += $(QTI_TELEPHONY_RES)
PRODUCT_PACKAGES += $(HIDL_WRAPPER)
PRODUCT_PACKAGES += $(QTI_TELEPHONY_UTILS)

PRODUCT_PACKAGES_DEBUG += $(TELEPHONY_APPS_DBG)

#copy telephony app's permissions
PRODUCT_COPY_FILES += $(QCPATH)/commonsys/telephony-build/build/telephony_product_privapp-permissions-qti.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/telephony_product_privapp-permissions-qti.xml

PRODUCT_COPY_FILES += $(QCPATH)/commonsys/telephony-build/build/telephony_system-ext_privapp-permissions-qti.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/telephony_system-ext_privapp-permissions-qti.xml

ifneq ($(TARGET_NO_TELEPHONY), true)
PRODUCT_COPY_FILES += \
frameworks/native/data/etc/android.hardware.telephony.gsm.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/android.hardware.telephony.gsm.xml \
frameworks/native/data/etc/android.hardware.telephony.ims.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/android.hardware.telephony.ims.xml
endif #TARGET_NO_TELEPHONY
