ifneq ($(TARGET_NO_TELEPHONY), true)

PRODUCT_PACKAGES += vendor.qti.hardware.radio.am@1.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.1
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.2
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.3
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.4
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.5
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.6
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.7
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.8
PRODUCT_PACKAGES += vendor.qti.hardware.radio.ims@1.9
PRODUCT_PACKAGES += vendor.qti.hardware.radio.internal.deviceinfo@1.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.lpa@1.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.lpa@1.1
PRODUCT_PACKAGES += vendor.qti.hardware.radio.lpa@1.2
PRODUCT_PACKAGES += vendor.qti.hardware.radio.lpa@1.3
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qcrilhook@1.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@1.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@2.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@2.1
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@2.2
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@2.3
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@2.4
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@2.5
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@2.6
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio@2.7
PRODUCT_PACKAGES += vendor.qti.hardware.radio.uim@1.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.uim@1.1
PRODUCT_PACKAGES += vendor.qti.hardware.radio.uim@1.2
PRODUCT_PACKAGES += vendor.qti.hardware.radio.uim_remote_client@1.0
PRODUCT_PACKAGES += vendor.qti.hardware.radio.uim_remote_client@1.1
PRODUCT_PACKAGES += vendor.qti.hardware.radio.uim_remote_client@1.2
PRODUCT_PACKAGES += vendor.qti.hardware.radio.uim_remote_server@1.0

##Stable AIDL
PRODUCT_PACKAGES += vendor.qti.hardware.radio.qtiradio-V1-java

endif # TARGET_NO_TELEPHONY
