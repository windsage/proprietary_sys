#IMS_INTERNAL
TARGET_SUPPORTS_EPISTEME ?= true
ifeq ($(TARGET_SUPPORTS_EPISTEME), true)
PRODUCT_PACKAGES += Episteme
PRODUCT_PACKAGES += libepijni
endif
ifneq ($(ENABLE_HYP), true)
IMS_INTERNAL := IMSAudio
IMS_INTERNAL += libloopbackvtjni
IMS_INTERNAL += com.qualcomm.qti.imsaudio
IMS_INTERNAL += com.qti.vtloopback

PRODUCT_PACKAGES += $(IMS_INTERNAL)

endif
