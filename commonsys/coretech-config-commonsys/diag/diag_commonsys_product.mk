#DIAG_SYSTEM
DIAG_SYSTEM := libdiag_system
DIAG_SYSTEM += libdiag_system.qti
DIAG_SYSTEM += test_diag_system
DIAG_SYSTEM += diag_callback_sample_system
DIAG_SYSTEM += diag_dci_sample_system
DIAG_SYSTEM += diag_mdlog_system

#AIDL DIAGHAL Package
DIAG_SYSTEM += vendor.qti.diaghal

#HIDL DIAGHAL Package
DIAG_SYSTEM += vendor.qti.diaghal@1.0

PRODUCT_PACKAGES += $(DIAG_SYSTEM)
