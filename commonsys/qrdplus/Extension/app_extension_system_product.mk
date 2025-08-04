ifneq ($(TARGET_HAS_LOW_RAM),true)
ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS),true)
PRODUCT_PACKAGES += \
    PowerOffAlarm \
    SimContact \
    QColor \
    PowerSaveMode

ifneq (,$(filter userdebug eng, $(TARGET_BUILD_VARIANT)))
  SMART_TRACE := binder_trace_dump.sh
  SMART_TRACE += perfetto_dump.sh
  SMART_TRACE += perfetto.cfg
  PRODUCT_PACKAGES += $(SMART_TRACE)

  PRODUCT_PACKAGES += ramdump_copy_daemon \
    RamdumpCopyUI

  LOCAL_SCRIPT_PATH := $(QCPATH_COMMONSYS)/qrdplus/Extension/RamdumpCopy/scripts
  PRODUCT_COPY_FILES += $(LOCAL_SCRIPT_PATH)/ramdump_wrapper.sh:$(TARGET_COPY_OUT_SYSTEM_EXT)/ramdump_wrapper.sh
  PRODUCT_COPY_FILES += $(LOCAL_SCRIPT_PATH)/qcom_rawdump_copy.sh:$(TARGET_COPY_OUT_SYSTEM_EXT)/qcom_rawdump_copy.sh

  PRODUCT_PACKAGES += freezerwhitelist
endif
endif
endif
