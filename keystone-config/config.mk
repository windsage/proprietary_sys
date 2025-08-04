ifneq ($(TARGET_PRESIL_SLOW_BOARD),true)
ifeq ($(TARGET_HAS_LOW_RAM),true)
    $(call inherit-product-if-exists, vendor/partner_gms/products/gms_go_2gb.mk)
else
    ifeq ($(TARGET_SUPPORTS_64_BIT_ONLY),true)
        $(call inherit-product-if-exists, vendor/partner_gms/products/gms_64bit_only.mk)
    else
        $(call inherit-product-if-exists, vendor/partner_gms/products/gms.mk)
    endif
endif
endif
