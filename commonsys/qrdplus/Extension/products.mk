ifneq ($(TARGET_HAS_LOW_RAM),true)
ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS),true)
PRODUCT_PACKAGES += \
    init.carrier.rc \
    com.qrd.wappush \
    com.qrd.wappush.xml \
    CarrierSwitch \
    SnapdragonSVA \
    SoundTriggerJAR \
    ListenJAR \
    QtiSettings \
    QtiSoundRecorder \
    SimContact

ifeq (,$(wildcard vendor/partner_gms/products/gms.mk))
PRODUCT_PACKAGES += \
    QrdChrome \
    QrdGmsCore \
    QrdGooglePartnerSetup \
    QrdGoogleServicesFramework \
    FrameworkResGmsOverlay \
    Framework-res-overlay \
    qcom-gms-hiddenapi-package-whitelist \
    google \
    privapp-permissions-google \
    split-permissions-google
endif # ifneq (,$(wildcard vendor/partner_gms/products/gms.mk))

endif
endif

ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS),true)
PRODUCT_PACKAGES += \
    Mms
endif

ifneq (,$(filter userdebug eng, $(TARGET_BUILD_VARIANT)))
  SMART_TRACE := binder_trace_dump.sh
  SMART_TRACE += perfetto_dump.sh
  SMART_TRACE += perfetto.cfg
  PRODUCT_PACKAGES += $(SMART_TRACE)
endif
