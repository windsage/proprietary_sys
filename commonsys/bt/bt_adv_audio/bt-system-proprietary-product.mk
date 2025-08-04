ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS), true)
ifeq ($(TARGET_USE_BT_DUN),true)
PRODUCT_PACKAGES += dun-server
endif #TARGET_USE_BT_DUN
PRODUCT_PACKAGES += BluetoothDsDaService
PRODUCT_PACKAGES += default_permissions_btdsda
PRODUCT_PACKAGES_DEBUG += libbluetooth-types-qti
PRODUCT_PACKAGES_DEBUG += bap_uclient_test
endif
