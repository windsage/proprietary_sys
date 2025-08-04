ifeq ($(ENABLE_CFI), true)
PRODUCT_CFI_INCLUDE_PATHS += \
    hardware/interfaces\
    hardware/qcom \
    vendor/nxp/opensource \
    vendor/qcom/proprietary \
    vendor/qcom/opensource

PRODUCT_CFI_EXCLUDE_PATHS += \
    hardware/interfaces/wifi \
    vendor/qcom/proprietary/kernel-tests-internal \
    vendor/qcom/proprietary/cv-internal \
    vendor/qcom/proprietary/graphics/adreno200 \
    vendor/qcom/proprietary/camx/camx-lib/system/fd/algo \
    vendor/qcom/proprietary/camx/camx-lib/system/registrationalgo/opencv \
    vendor/qcom/proprietary/camx-lib/system/fd/algo \
    vendor/qcom/proprietary/camx-lib/system/registrationalgo/opencv \
    vendor/qcom/proprietary/display-noship/hdr-intf \
    vendor/qcom/proprietary/rs-driver \
    vendor/qcom/proprietary/gps-noship-external \
    vendor/qcom/proprietary/gps-noship/xtwifi/server_protocol \
    vendor/qcom/proprietary/gps-noship/xtwifi/server_protocol_uri_v3 \
    vendor/qcom/proprietary/commonsys/display-tests/SFClientLuaTest \
    vendor/qcom/opensource/data-openssl
endif

ifeq ($(ENABLE_INTEGER_OVERFLOW), true)
PRODUCT_INTEGER_OVERFLOW_INCLUDE_PATHS += \
    hardware/interfaces\
    hardware/qcom \
    vendor/qcom/proprietary \
    vendor/qcom/opensource

PRODUCT_INTEGER_OVERFLOW_EXCLUDE_PATHS += \
    vendor/qcom/proprietary/graphics/adreno200 \
    vendor/qcom/proprietary/display-tests \
    vendor/qcom/proprietary/commonsys/display-tests \
    vendor/qcom/proprietary/cv-internal \
    vendor/qcom/opensource/data-openssl
endif

ifeq ($(ENABLE_BOUND_SANITIZER), true)
PRODUCT_BOUNDS_INCLUDE_PATHS += \
    hardware/interfaces\
    hardware/qcom \
    vendor/qcom/proprietary \
    vendor/qcom/opensource
PRODUCT_BOUNDS_EXCLUDE_PATHS += \
    vendor/qcom/proprietary/graphics/adreno200 \
    vendor/qcom/proprietary/display-tests \
    vendor/qcom/proprietary/commonsys/display-tests \
    vendor/qcom/proprietary/cv-internal
endif

ifeq ($(BUILD_HWASAN), true)
    $(call soong_config_set, qti_build_sanitizer_config,build_is_hwasan,enabled)
else
    $(call soong_config_set, qti_build_sanitizer_config,build_is_hwasan,disabled)
endif
