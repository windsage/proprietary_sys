#include common overlays
PRODUCT_PACKAGES += \
    FrameworksResCommon_Sys \
    CarrierConfigResCommon_Sys \
    CellBroadcastReceiverResCommon_Sys \
    SystemUIResCommon_Sys \
    TelecommResCommon_Sys \
    TelephonyResCommon_Sys \
    FrameworksResCommonQva_Sys \
    SettingsResCommon_Sys \
    WifiResCommon_Sys \
    WifiResCommonMainline_Sys \
    UwbResCommon_Sys

BASE_PATH := vendor/qcom/proprietary/commonsys/resource-overlay
PRODUCT_COPY_FILES += $(BASE_PATH)/partition_order.xml:$(TARGET_COPY_OUT_PRODUCT)/overlay/partition_order.xml
