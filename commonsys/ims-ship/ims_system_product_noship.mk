#IMS_VT
ifneq ($(ENABLE_HYP), true)

IMS_VT := lib-imsvt
IMS_VT += lib-imsvtutils
IMS_VT += lib-imsvtextutils
IMS_VT += imsvtdaemon
IMS_VT += imsvtdaemon.rc
IMS_VT += imsvtdaemon.policy

IMS_RCS := ImsRcsService
IMS_RCS += vendor.qti.ims.rcsservice.xml
IMS_RCS += whitelist_com.qualcomm.qti.uceShimService.xml
IMS_RCS += uceShimService

IMS_DCS := vendor.qti.imsdatachannel.xml
IMS_DCS += vendor.qti.imsdatachannel
IMS_DCS += ImsDataChannelService
IMS_DCS += vendor.qti.imsdcservice.xml

PRODUCT_PACKAGES += $(IMS_VT)
PRODUCT_PACKAGES += $(IMS_RCS)
PRODUCT_PACKAGES += $(IMS_DCS)

ifneq ($(TARGET_HAS_LOW_RAM),true)
    MSTAT := MStatsSystemService
    PRODUCT_PACKAGES += $(MSTAT)
endif

endif
