ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS),true)
    ifneq ($(TARGET_BOARD_AUTO),true)
        # Preloading UxPerformance jar to ensure faster UX invoke in Boost Framework
        PRODUCT_BOOT_JARS += UxPerformance

        PRODUCT_PACKAGES += \
            UxPerformance \
            libqti-at \
            libqti-iopd-client_system

        #Below libs are needed by WorkloadClassifier and are specific to HY11 Builds
        ifneq ($(TARGET_HAS_LOW_RAM),true)
        PRODUCT_PACKAGES += \
            libtflite \
            libtextclassifier_hash \
            libtflite_context \
            libtflite_framework \
            libtflite_kernels \
            libtextclassifier \
            libtextclassifier_hash_defaults \
            libc++_static
        endif
    endif
endif
