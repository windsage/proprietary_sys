ifneq ($(TARGET_NO_TELEPHONY), true)
PRODUCT_PACKAGES += \
    uimlpaservice
PRODUCT_PACKAGES_DEBUG += \
    uimlpatest \
    EuiccGoogle

#copy the EuiccGoogle permission xml file
EUICC_XML_PATH := $(QCPATH_COMMONSYS)/telephony-internal/EuiccGoogle
ifneq ($(wildcard $(EUICC_XML_PATH)),)
  PRODUCT_COPY_FILES += $(EUICC_XML_PATH)/privapp-permissions-com.google.android.euicc.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-com.google.android.euicc.xml
endif

ifneq ($(TARGET_HAS_LOW_RAM),true)
PRODUCT_PACKAGES += \
    remotesimlockservice \
    uimservicelibrary \
    UimService.xml \
    uimgbaservice
PRODUCT_PACKAGES_DEBUG += \
    CarrierTest
ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS), true)
PRODUCT_PACKAGES += \
    MultiplePdpTest
endif
endif
endif


PRODUCT_PACKAGES_DEBUG += \
    TestAppForSAM \
    QtiMmsTestApp \
