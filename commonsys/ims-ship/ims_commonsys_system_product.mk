ifneq ($(ENABLE_HYP), true)

IMS_SHIP_RCS += datachannellib
IMS_SHIP_RCS += datachannellib.xml

#IMS_SHIP_VT += lib-imscamera
IMS_SHIP_VT += lib-imsvideocodec

PRODUCT_PACKAGES += $(IMS_SHIP_RCS)
PRODUCT_PACKAGES += $(IMS_SHIP_VT)

endif
