PREBUILT_PATH := $(call my-dir)
LOCAL_PATH         := $(PREBUILT_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE        := DeviceStatisticsService
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/product/app/DeviceStatisticsService/DeviceStatisticsService.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_PRODUCT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := remotesimlockservice
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/product/app/remotesimlockservice/remotesimlockservice.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_PRODUCT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := uimgbaservice
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/product/app/uimgbaservice/uimgbaservice.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_PRODUCT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := uimlpaservice
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/product/app/uimlpaservice/uimlpaservice.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_PRODUCT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qvirt-service_rs
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_CHECK_ELF_FILES := false
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/product/bin/vendor.qti.qvirt-service_rs
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_PRODUCT)/bin
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qvirt-service.xml
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := ETC
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/product/etc/vintf/manifest/vendor.qti.qvirt-service.xml
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_PRODUCT)/etc/vintf/manifest
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := DynamicDDSService
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/DynamicDDSService/DynamicDDSService.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := ImsDataChannelService
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/ImsDataChannelService/ImsDataChannelService.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := ImsRcsService
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/ImsRcsService/ImsRcsService.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := NtnSatApp
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/NtnSatApp/NtnSatApp.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := ODLT
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/ODLT/ODLT.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := QCC
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/QCC/QCC.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := QesdkSysService
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/QesdkSysService/QesdkSysService.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := QTIDiagServices
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/QTIDiagServices/QTIDiagServices.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := uceShimService
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/uceShimService/uceShimService.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := workloadclassifier
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/app/workloadclassifier/workloadclassifier.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := aptxalsOverlayCreateProject.sh
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES := 
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/aptxalsOverlayCreateProject.sh
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := dpmd
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  libdpmframework libdiag_system libhardware_legacy libhidlbase libcutils libutils com.qualcomm.qti.dpm.api@1.0 libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/dpmd
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := qccsyshal@1.2-service
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  libbase libhidlbase libutils libfmq libbinder libcutils liblog vendor.qti.hardware.qccsyshal@1.2-halimpl vendor.qti.hardware.qccsyshal@1.2 vendor.qti.hardware.qccsyshal@1.1 vendor.qti.hardware.qccsyshal@1.0 libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/qccsyshal@1.2-service
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := qccsyshal_aidl-service
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  libutils libfmq libbinder liblog libcutils libbase libbinder_ndk vendor.qti.qccsyshal_aidl-V1-ndk vendor.qti.qccsyshal_aidl-halimpl libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/qccsyshal_aidl-service
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := qsguard
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  libbase libcutils liblog libgui libutils libbinder libservices libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/qsguard
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := qspmsvc
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  liblog libutils libcutils libbinder_ndk libbinder libbase vendor.qti.qspmhal@1.0 vendor.qti.qspmhal-V1-ndk android.hardware.thermal@1.0 android.hardware.thermal@2.0 android.hidl.memory@1.0 libhidlmemory libhidlbase libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/qspmsvc
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := qxrsplitauxservice
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  liblog libutils libbinder_ndk libcutils libnativewindow libavservices_minijail vendor.qti.hardware.qxr-V1-ndk_platform libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/qxrsplitauxservice
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := sigma_miracasthalservice64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.sigma_miracast@1.0 vendor.qti.hardware.sigma_miracast@1.0-halimpl libhidlbase libutils liblog libbinder libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/sigma_miracasthalservice64
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := sigma_miracasthalservice_aidl
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.sigma_miracast_aidl-V1-ndk vendor.qti.hardware.sigma_miracast_aidl-halimpl libutils liblog libbase libbinder libbinder_ndk libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/sigma_miracasthalservice_aidl
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE        := tcmd
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  libbinder libbinder_ndk libcutils libutils libhidlbase libhardware libhardware_legacy liblog vendor.qti.hardware.dpmservice@1.0 vendor.qti.hardware.dpmaidlservice-V1-ndk libavservices_minijail libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/tcmd
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := wfdservice64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := EXECUTABLES
LOCAL_SHARED_LIBRARIES :=  liblog libutils libcutils libbinder libwfdservice libmmosal libwfdcommonutils libwfdconfigutils libhidlbase libavservices_minijail libc++ libc libm libdl
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/bin/wfdservice64
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/bin
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.sigma_miracast_aidl.xml
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := ETC
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/etc/vintf/manifest/vendor.qti.hardware.sigma_miracast_aidl.xml
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/vintf/manifest
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.sigma_miracast.xml
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := ETC
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/etc/vintf/manifest/vendor.qti.hardware.sigma_miracast.xml
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/vintf/manifest
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qccsyshal_aidl-service.xml
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := ETC
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/etc/vintf/manifest/vendor.qti.qccsyshal_aidl-service.xml
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/vintf/manifest
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qesdsys.service.xml
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := ETC
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/etc/vintf/manifest/vendor.qti.qesdsys.service.xml
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/vintf/manifest
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.qualcomm.qti.dpm.api@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.qualcomm.qti.dpm.api@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.api@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  com.quicinc.cne.constants@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.api@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.api@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  com.quicinc.cne.api@1.0 com.quicinc.cne.constants@2.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.api@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.constants@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.constants@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.constants@2.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.constants@2.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.constants@2.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.constants@2.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.server@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  com.quicinc.cne.constants@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.server@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.server@2.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  com.quicinc.cne.constants@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.server@2.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.server@2.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  com.quicinc.cne.constants@2.1 com.quicinc.cne.server@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.server@2.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := com.quicinc.cne.server@2.2
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  com.quicinc.cne.constants@2.0 com.quicinc.cne.constants@2.1 com.quicinc.cne.server@2.0 com.quicinc.cne.server@2.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/com.quicinc.cne.server@2.2.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.sigma_miracast@1.0-impl
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase libutils vendor.qti.hardware.sigma_miracast@1.0 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/hw/vendor.qti.hardware.sigma_miracast@1.0-impl.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64/hw
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE        := libAdaptiveEQ_recpp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libAdaptiveEQ_recpp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libAGC_recpp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libAGC_recpp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libANS_recpp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libANS_recpp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libbeluga
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbase liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libbeluga.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libbinauralrenderer_wrapper.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libvr_amb_engine liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libbinauralrenderer_wrapper.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libcomposerextn.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils liblog libutils libui libsync libhidlbase libdisplayconfig.system.qti libtinyxml2 vendor.qti.hardware.limits@1.0 vendor.qti.hardware.limits@1.1 android.hardware.thermal-V2-ndk libbinder libbinder_ndk vendor.qti.hardware.display.config-V12-ndk libgralloc.system.qti libvndksupport libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libcomposerextn.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libCompressor_recpp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libCompressor_recpp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libdashdatasource
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libmmiipstreammmihttp libembmsmmosal libmmipstreamutils libembmstinyxml liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libdashdatasource.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libdashsamplesource
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libmmiipstreammmihttp libembmsmmosal libmmipstreamutils libembmstinyxml liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libdashsamplesource.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libDiagService
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libandroid_runtime libnativehelper libutils libcutils liblog libdiag_system libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libDiagService.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libdolphin
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder libcutils libgui liblog libutils vendor.qti.hardware.perf2-V1-ndk libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libdolphin.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libdpmctmgr
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libdiag_system libbinder libcutils libutils libdpmframework com.qualcomm.qti.dpm.api@1.0 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libdpmctmgr.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libdpmfdmgr
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libdiag_system libbinder libcutils libutils libdpmframework com.qualcomm.qti.dpm.api@1.0 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libdpmfdmgr.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libdpmframework
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libdiag_system libbinder libcutils libutils liblog libhidlbase libhardware libhardware_legacy com.qualcomm.qti.dpm.api@1.0 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libdpmframework.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libdpmtcm
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libdiag_system libbinder libcutils libutils libdpmframework com.qualcomm.qti.dpm.api@1.0 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libdpmtcm.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libembmsmmosal
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libembmsmmosal.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libembmsmmparser_lite
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libembmsmmosal liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libembmsmmparser_lite.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libembmstinyxml
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libembmstinyxml.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libHDR_recpp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libHDR_recpp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libhoaeffects_csim
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libhoaeffects_csim.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libhoaeffects.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhoaeffects_csim libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libhoaeffects.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libHPFilter_recpp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libHPFilter_recpp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := lib-imsvtextutils
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils libcutils liblog libdiag_system lib-imsvtutils libGLESv2 libEGL libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/lib-imsvtextutils.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := lib-imsvt
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  lib-imsvideocodec libmediandk libnativewindow libion libdmabufheap lib-imsvtutils libandroid libhidlbase vendor.qti.imsrtpservice@3.0 vendor.qti.imsrtpservice@3.1 libbinder libbinder_ndk vendor.qti.ImsRtpService-V1-ndk libdiag_system libcutils libutils liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/lib-imsvt.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := lib-imsvtutils
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils libcutils liblog libdiag_system libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/lib-imsvtutils.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libjnihelpers
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libjnihelpers.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := liblayerext.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils liblog libutils libtinyxml2 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/liblayerext.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libLimiter_recpp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libLimiter_recpp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := liblistensoundmodel2.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/liblistensoundmodel2.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libloc2jnibridge
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils libqmi_cci_system liblog libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libloc2jnibridge.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmink-sock-native-api
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  liblog libminksocket_system libjnihelpers libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmink-sock-native-api.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmiracastsystem
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils libhardware_legacy libaudiofoundation audioclient-types-aidl-cpp android.media.audio.common.types-V2-cpp libmmosal libutils libmedia libbinder libnetutils libwfdclient libaudioclient libmdnssd libsigmautils liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmiracastsystem.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmhttpstack
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libembmsmmosal libcutils libmmipstreamutils libmmipstreamnetwork liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmhttpstack.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmiipstreammmihttp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libembmsmmosal libmmipstreamutils libembmsmmparser_lite libmmipstreamsourcehttp liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmiipstreammmihttp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmipstreamnetwork
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libembmsmmosal liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmipstreamnetwork.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmipstreamsourcehttp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libembmstinyxml libembmsmmosal libembmsmmparser_lite libmmipstreamnetwork libmmipstreamutils libmmhttpstack liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmipstreamsourcehttp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmipstreamutils
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libembmsmmosal liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmipstreamutils.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmparser_lite
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils libcutils liblog libmmosal libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmparser_lite.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmQSM
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libembmsmmosal liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmQSM.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/mm-rtp),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmrtpdecoder
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils liblog libcutils libmmosal libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmrtpdecoder.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/mm-rtp),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmmrtpencoder
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libmmosal liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmmrtpencoder.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE        := libmsp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libz libjsoncpp libxml2 liblog libutils libhidlbase vendor.qti.hardware.embmssl@1.0 vendor.qti.hardware.embmssl@1.1 libbinder_ndk vendor.qti.hardware.embmsslaidl-V2-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmsp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libmwqemiptablemgr
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  liblog libcutils libbase libutils libdpmframework libdiag_system com.qualcomm.qti.dpm.api@1.0 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libmwqemiptablemgr.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libNtnJni
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils liblog libutils libbinder libqms_ntnsatellite_sdk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libNtnJni.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libOpenCL_system
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils liblog libvndksupport libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libOpenCL_system.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libpenguin_impl
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder libcutils libgui liblog libui libutils libandroid libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libpenguin_impl.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libpenguin
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libpenguin.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqape.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libnativehelper libcutils libutils liblog libbinder libhidlbase libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqape.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqccdme
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcurl libssl libcrypto libcutils liblog libutils libbase libfmq libhidlbase libbinder libbinder_ndk vendor.qti.hardware.qccvndhal@1.0 vendor.qti.hardware.qccsyshal@1.2 android.hardware.common-V2-ndk vendor.qti.qccvndhal_aidl-V1-ndk vendor.qti.qccsyshal_aidl-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqccdme.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqcc_file_agent_sys
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqcc_file_agent_sys.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqccfileservice
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcurl libssl libcrypto libcutils liblog libutils libbase libfmq libhidlbase libbinder_ndk vendor.qti.hardware.qccvndhal@1.0 vendor.qti.hardware.qccsyshal@1.2 android.hardware.common-V2-ndk vendor.qti.qccvndhal_aidl-V1-ndk vendor.qti.qccsyshal_aidl-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqccfileservice.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqcc_netstats
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils liblog libutils libbase libhidlbase vendor.qti.hardware.qccvndhal@1.0 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqcc_netstats.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqcc
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcurl libssl libcrypto libcutils liblog libutils libbase libfmq libhidlbase libbinder_ndk vendor.qti.hardware.qccvndhal@1.0 vendor.qti.hardware.qccsyshal@1.2 android.hardware.common-V2-ndk vendor.qti.qccvndhal_aidl-V1-ndk vendor.qti.qccsyshal_aidl-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqcc.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqesdk_ndk_platform.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk liblog libnativehelper libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqesdk_ndk_platform.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libQmsNtnProto
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libprotobuf-cpp-lite libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libQmsNtnProto.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqms_ntnsatellite_sdk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  liblog libutils libbinder_ndk libprotobuf-cpp-lite libQmsNtnProto libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqms_ntnsatellite_sdk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqspm-mem-utils
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.qspmhal-V1-ndk libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqspm-mem-utils.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqspmsvc
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  liblog libutils libcutils libbinder libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqspmsvc.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqti_workloadclassifiermodel
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libnativehelper libcutils liblog libutils libbase libtflite libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqti_workloadclassifiermodel.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libqxrsplitauxservice.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils libcutils libaudioclient libaudiofoundation audioclient-types-aidl-cpp android.media.audio.common.types-V2-cpp libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libqxrsplitauxservice.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := librecpp_intf
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libHDR_recpp libWNR_intf libANS_recpp libHPFilter_recpp libAGC_recpp libCompressor_recpp libAdaptiveEQ_recpp libLimiter_recpp liblog libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/librecpp_intf.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libsdm-disp-apis.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils libutils liblog libhidlbase vendor.display.color@1.0 libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libsdm-disp-apis.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libskewknob_system
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  liblog libc libcutils libc++ libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libskewknob_system.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libsmomoconfig.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils libbinder libcomposerextn.qti liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libsmomoconfig.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libthermalclient.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils liblog libbinder libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libthermalclient.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libtrigger-handler
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libcutils libutils liblog libbinder_ndk vendor.qti.qspmhal-V1-ndk vendor.qti.qspmhal@1.0 libhidlbase libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libtrigger-handler.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libupdateprof.qti
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.qspmhal@1.0 libqspm-mem-utils libbinder_ndk libhidlbase libbase libhidltransport liblog libutils libcutils android.hidl.allocator@1.0 android.hidl.memory@1.0 libhidlmemory vendor.qti.qspmhal-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libupdateprof.qti.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libvr_amb_engine
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libvr_amb_engine.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libvr_object_engine
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libvr_object_engine.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfdclient
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfdclient.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfdcommonutils
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfdcommonutils.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfddisplayconfig
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase libutils libcutils liblog vendor.display.config@2.0 libdisplayconfig.system.qti libbinder_ndk vendor.qti.hardware.display.config-V5-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfddisplayconfig.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfdmminterface
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libmmosal liblog libutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfdmminterface.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfdmmsink
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfdmmsink.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfdmmsrc_system
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfdmmsrc_system.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfdrtsp
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfdrtsp.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfdservice
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfdservice.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfdsinksm
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfdsinksm.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfduibcinterface
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libmmosal liblog libutils libcutils libwfduibcsrcinterface libwfduibcsinkinterface libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfduibcinterface.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfduibcsinkinterface
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfduibcsinkinterface.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfduibcsink
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libmmosal libutils liblog libcutils libwfdconfigutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfduibcsink.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfduibcsrcinterface
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfduibcsrcinterface.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := libwfduibcsrc
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_CHECK_ELF_FILES := false
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libwfduibcsrc.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE        := libWNR_intf
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libWNR liblog libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libWNR_intf.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := libWNR
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/libWNR.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  com.quicinc.cne.api@1.0 com.quicinc.cne.api@1.1 com.quicinc.cne.constants@2.1 com.quicinc.cne.server@2.0 com.quicinc.cne.server@2.1 com.quicinc.cne.server@2.2 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.latency@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.latency@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.data.factory@2.0 vendor.qti.data.slm@1.0 vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.ims.rcsconfig@1.1 vendor.qti.latency@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.2
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.data.factory@2.0 vendor.qti.data.factory@2.1 vendor.qti.data.mwqem@1.0 vendor.qti.data.slm@1.0 vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.ims.rcsconfig@1.1 vendor.qti.latency@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.2.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.3
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.data.factory@2.0 vendor.qti.data.factory@2.1 vendor.qti.data.factory@2.2 vendor.qti.data.mwqem@1.0 vendor.qti.data.slm@1.0 vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.lce@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.ims.rcsconfig@1.1 vendor.qti.latency@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.3.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.4
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.data.factory@2.0 vendor.qti.data.factory@2.1 vendor.qti.data.factory@2.2 vendor.qti.data.factory@2.3 vendor.qti.data.mwqem@1.0 vendor.qti.data.slm@1.0 vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.cne.internal.server@1.1 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.lce@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.ims.rcsconfig@1.1 vendor.qti.latency@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.4.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.5
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.data.factory@2.0 vendor.qti.data.factory@2.1 vendor.qti.data.factory@2.2 vendor.qti.data.factory@2.3 vendor.qti.data.factory@2.4 vendor.qti.data.mwqem@1.0 vendor.qti.data.slm@1.0 vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.cne.internal.server@1.1 vendor.qti.hardware.data.cne.internal.server@1.2 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.flow@1.0 vendor.qti.hardware.data.lce@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.ims.rcsconfig@1.1 vendor.qti.latency@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.5.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.6
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.data.factory@2.0 vendor.qti.data.factory@2.1 vendor.qti.data.factory@2.2 vendor.qti.data.factory@2.3 vendor.qti.data.factory@2.4 vendor.qti.data.factory@2.5 vendor.qti.data.mwqem@1.0 vendor.qti.data.slm@1.0 vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.cne.internal.server@1.1 vendor.qti.hardware.data.cne.internal.server@1.2 vendor.qti.hardware.data.cne.internal.server@1.3 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.flow@1.0 vendor.qti.hardware.data.lce@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.ims.rcsconfig@1.1 vendor.qti.latency@2.0 vendor.qti.latency@2.1 vendor.qti.latency@2.2 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.6.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.7
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.data.factory@2.0 vendor.qti.data.factory@2.1 vendor.qti.data.factory@2.2 vendor.qti.data.factory@2.3 vendor.qti.data.factory@2.4 vendor.qti.data.factory@2.5 vendor.qti.data.factory@2.6 vendor.qti.data.mwqem@1.0 vendor.qti.data.slm@1.0 vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.cne.internal.server@1.1 vendor.qti.hardware.data.cne.internal.server@1.2 vendor.qti.hardware.data.cne.internal.server@1.3 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.flow@1.0 vendor.qti.hardware.data.flow@1.1 vendor.qti.hardware.data.lce@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.ims.rcsconfig@1.1 vendor.qti.latency@2.0 vendor.qti.latency@2.1 vendor.qti.latency@2.2 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.7.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factory@2.8
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  android.hidl.safe_union@1.0 vendor.qti.data.factory@2.0 vendor.qti.data.factory@2.1 vendor.qti.data.factory@2.2 vendor.qti.data.factory@2.3 vendor.qti.data.factory@2.4 vendor.qti.data.factory@2.5 vendor.qti.data.factory@2.6 vendor.qti.data.factory@2.7 vendor.qti.data.mwqem@1.0 vendor.qti.data.slm@1.0 vendor.qti.hardware.data.cne.internal.api@1.0 vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.cne.internal.server@1.1 vendor.qti.hardware.data.cne.internal.server@1.2 vendor.qti.hardware.data.cne.internal.server@1.3 vendor.qti.hardware.data.dynamicdds@1.0 vendor.qti.hardware.data.flow@1.0 vendor.qti.hardware.data.flow@1.1 vendor.qti.hardware.data.lce@1.0 vendor.qti.hardware.data.qmi@1.0 vendor.qti.ims.rcsconfig@1.0 vendor.qti.ims.rcsconfig@1.1 vendor.qti.latency@2.0 vendor.qti.latency@2.1 vendor.qti.latency@2.2 vendor.qti.latency@2.3 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factory@2.8.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.data.factoryservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk vendor.qti.hardware.data.dynamicddsaidlservice-V1-ndk vendor.qti.hardware.data.qmiaidlservice-V1-ndk vendor.qti.hardware.data.lceaidlservice-V1-ndk vendor.qti.hardware.data.flowaidlservice-V1-ndk vendor.qti.hardware.data.cneaidlservice.internal.api-V1-ndk vendor.qti.hardware.data.cneaidlservice.internal.server-V1-ndk vendor.qti.latencyaidlservice-V1-ndk vendor.qti.data.mwqemaidlservice-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.data.factoryservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.cacertaidlservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.cacertaidlservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cneaidlservice.internal.api-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk vendor.qti.hardware.data.cneaidlservice.internal.constants-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cneaidlservice.internal.api-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cneaidlservice.internal.constants-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cneaidlservice.internal.constants-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cneaidlservice.internal.server-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk vendor.qti.hardware.data.cneaidlservice.internal.constants-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cneaidlservice.internal.server-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cne.internal.api@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.data.cne.internal.constants@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cne.internal.api@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cne.internal.constants@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cne.internal.constants@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cne.internal.server@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.data.cne.internal.constants@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cne.internal.server@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cne.internal.server@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cne.internal.server@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cne.internal.server@1.2
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.cne.internal.server@1.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cne.internal.server@1.2.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.cne.internal.server@1.3
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.data.cne.internal.constants@1.0 vendor.qti.hardware.data.cne.internal.server@1.0 vendor.qti.hardware.data.cne.internal.server@1.1 vendor.qti.hardware.data.cne.internal.server@1.2 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.cne.internal.server@1.3.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.connectionfactory-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk vendor.qti.hardware.data.dataactivity-V1-ndk vendor.qti.hardware.data.ka-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.connectionfactory-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.dataactivity-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.dataactivity-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.ka-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.ka-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.qmi@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.qmi@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.data.qmiaidlservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.data.qmiaidlservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.dpmaidlservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.dpmaidlservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.dpmservice@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.dpmservice@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.dpmservice@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.dpmservice@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.dpmservice@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.embmssl@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.embmssl@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.embmssl@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.embmssl@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.embmssl@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.embmsslaidl-V2-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.embmsslaidl-V2-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.mwqemadapter@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.mwqemadapter@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.mwqemadapteraidlservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.mwqemadapteraidlservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.qccsyshal@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.qccsyshal@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.qccsyshal@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.qccsyshal@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.qccsyshal@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.qccsyshal@1.2-halimpl
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libprotobuf-cpp-full libcutils liblog libhidlbase libutils libfmq libqcc_file_agent_sys vendor.qti.hardware.qccsyshal@1.2 vendor.qti.hardware.qccsyshal@1.1 vendor.qti.hardware.qccsyshal@1.0 libz libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.qccsyshal@1.2-halimpl.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.qccsyshal@1.2
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.hardware.qccsyshal@1.0 vendor.qti.hardware.qccsyshal@1.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.qccsyshal@1.2.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.qccvndhal@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.qccvndhal@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.sigma_miracast@1.0-halimpl
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase vendor.qti.hardware.sigma_miracast@1.0 libutils libmmosal libmiracastsystem libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.sigma_miracast@1.0-halimpl.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

ifeq ($(wildcard vendor/qcom/proprietary/commonsys/wfd-framework),)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.sigma_miracast_aidl-halimpl
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils vendor.qti.hardware.sigma_miracast_aidl-V1-ndk liblog libbinder libbase libbinder_ndk libmmosal libmiracastsystem libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.sigma_miracast_aidl-halimpl.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.hardware.slmadapter@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.hardware.slmadapter@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.connection@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.connection@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.connectionaidlservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.connectionaidlservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.datachannelservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.datachannelservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.datachannelservice-V2-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.datachannelservice-V2-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.datachannelservice-V3-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.datachannelservice-V3-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.factory@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.callcapability@1.0 vendor.qti.ims.rcsconfig@2.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.factory@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.factory@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.callcapability@1.0 vendor.qti.ims.factory@1.0 vendor.qti.ims.rcsconfig@2.0 vendor.qti.ims.rcsconfig@2.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.factory@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.factory@2.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.connection@1.0 vendor.qti.ims.callcapability@1.0 vendor.qti.ims.rcsuce@1.0 vendor.qti.ims.rcssip@1.0 vendor.qti.ims.configservice@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.factory@2.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.factory@2.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.callcapability@1.0 vendor.qti.ims.configservice@1.0 vendor.qti.ims.configservice@1.1 vendor.qti.ims.connection@1.0 vendor.qti.ims.factory@2.0 vendor.qti.ims.rcssip@1.0 vendor.qti.ims.rcssip@1.1 vendor.qti.ims.rcsuce@1.0 vendor.qti.ims.rcsuce@1.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.factory@2.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.factory@2.2
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.callcapability@1.0 vendor.qti.ims.configservice@1.0 vendor.qti.ims.configservice@1.1 vendor.qti.ims.connection@1.0 vendor.qti.ims.factory@2.0 vendor.qti.ims.factory@2.1 vendor.qti.ims.rcssip@1.0 vendor.qti.ims.rcssip@1.1 vendor.qti.ims.rcssip@1.2 vendor.qti.ims.rcsuce@1.0 vendor.qti.ims.rcsuce@1.1 vendor.qti.ims.rcsuce@1.2 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.factory@2.2.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.factoryaidlservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk vendor.qti.ims.rcsuceaidlservice-V1-ndk vendor.qti.ims.rcssipaidlservice-V1-ndk vendor.qti.ims.callcapabilityaidlservice-V1-ndk vendor.qti.ims.connectionaidlservice-V1-ndk vendor.qti.ims.configaidlservice-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.factoryaidlservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.rcssip@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.rcssip@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.rcssip@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.rcssip@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.rcssip@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.rcssip@1.2
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.rcssip@1.0 vendor.qti.ims.rcssip@1.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.rcssip@1.2.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.rcssipaidlservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.rcssipaidlservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.rcsuce@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.rcsuce@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.rcsuce@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.rcsuce@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.rcsuce@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.rcsuce@1.2
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.ims.rcsuce@1.0 vendor.qti.ims.rcsuce@1.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.rcsuce@1.2.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ims.rcsuceaidlservice-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ims.rcsuceaidlservice-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.imsrtpservice@3.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.imsrtpservice@3.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.imsrtpservice@3.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.imsrtpservice@3.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.imsrtpservice@3.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.ImsRtpService-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk android.hardware.common-V2-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.ImsRtpService-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.mstatservice@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.mstatservice@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qccsyshal_aidl-halimpl
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libutils libfmq libqcc_file_agent_sys libcutils liblog libz libbase libbinder libbinder_ndk vendor.qti.qccsyshal_aidl-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.qccsyshal_aidl-halimpl.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qccsyshal_aidl-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk android.hardware.common-V2-ndk android.hardware.common.fmq-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.qccsyshal_aidl-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qccvndhal_aidl-V1-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk android.hardware.common-V2-ndk android.hardware.common.fmq-V1-ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.qccvndhal_aidl-V1-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qesdhal@1.0
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.qesdhal@1.0.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qesdhal@1.1
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.qesdhal@1.0 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.qesdhal@1.1.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qesdhal@1.2
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  vendor.qti.qesdhal@1.0 vendor.qti.qesdhal@1.1 libhidlbase liblog libutils libcutils libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.qesdhal@1.2.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := vendor.qti.qesdhalaidl-V2-ndk
LOCAL_MODULE_CLASS  := SHARED_LIBRARIES
LOCAL_SHARED_LIBRARIES :=  libbinder_ndk libc++ libc libm libdl
LOCAL_MODULE_SUFFIX := .so
LOCAL_STRIP_MODULE  := false
LOCAL_MULTILIB      := 64
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/lib64/vendor.qti.qesdhalaidl-V2-ndk.so
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/lib64
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := aptxals
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/priv-app/aptxals/aptxals.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/priv-app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := dpmserviceapp
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/priv-app/dpmserviceapp/dpmserviceapp.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/priv-app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := MSDC_UI
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/priv-app/MSDC_UI/MSDC_UI.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/priv-app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := QAS_DVC_MSP
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/priv-app/QAS_DVC_MSP/QAS_DVC_MSP.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/priv-app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := xrcbservice
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/priv-app/xrcbservice/xrcbservice.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/priv-app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := xrvdservice
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/priv-app/xrvdservice/xrvdservice.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/priv-app
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE        := xrwifiservice
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := ../../.././target/product/qssi_64/system_ext/priv-app/xrwifiservice/xrwifiservice.apk
LOCAL_MODULE_PATH   := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_SYSTEM_EXT)/priv-app
include $(BUILD_PREBUILT)
LOCAL_PATH         := $(PREBUILT_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE               := com.qti.dpmframework
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.qti.dpmframework_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.server-V2.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.server-V2.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.cacertaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.cacertaidlservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.cacertaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.cacertaidlservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.constants-V2.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.constants-V2.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factoryaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factoryaidlservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factoryaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factoryaidlservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.data.cne.internal.api-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.data.cne.internal.api-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.4-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.4-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.4-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.4-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.datachannelservice-V3-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.datachannelservice-V3-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.datachannelservice-V3-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.datachannelservice-V3-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcsuce-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcsuce-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcsuce-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcsuce-V1.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.dpmservice-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.dpmservice-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.dpmservice-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.dpmservice-V1.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.5-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.5-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.5-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.5-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V1.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.data.cne.internal.server-V1.3-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.data.cne.internal.server-V1.3-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.server-V2.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.server-V2.2-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.mwqemadapteraidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.mwqemadapteraidlservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.mwqemadapteraidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.mwqemadapteraidlservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.data.cne.internal.server-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.data.cne.internal.server-V1.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.data.cne.internal.constants-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.data.cne.internal.constants-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.connectionaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.connectionaidlservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.connectionaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.connectionaidlservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.6-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.6-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.6-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.6-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V1.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V1.1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := dpmapi
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/dpmapi_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := dpmapi
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/dpmapi_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.3-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.3-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.constants-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.constants-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcsuceaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcsuceaidlservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcsuceaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcsuceaidlservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.dpmservice-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.dpmservice-V1.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.dpmservice-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.dpmservice-V1.1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcssip-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcssip-V1.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcssip-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcssip-V1.1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcssip-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcssip-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcssip-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcssip-V1.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.7-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.7-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.7-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.7-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.server-V2.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.server-V2.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.connection-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.connection-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.connection-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.connection-V1.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.8-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.8-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.8-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.8-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.mwqemadapter-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.mwqemadapter-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.mwqemadapter-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.mwqemadapter-V1.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factoryservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factoryservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factoryservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factoryservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcsuce-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcsuce-V1.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcsuce-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcsuce-V1.1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V2.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V2.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V2.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V2.1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.api-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.api-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V2.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V2.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V2.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V2.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcsuce-V1.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcsuce-V1.2-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcsuce-V1.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcsuce-V1.2-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.dpmaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.dpmaidlservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.dpmaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.dpmaidlservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.2-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.slmadapter-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.slmadapter-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.slmadapter-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.slmadapter-V1.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.datachannelservice-V2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.datachannelservice-V2-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.datachannelservice-V2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.datachannelservice-V2-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcssipaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcssipaidlservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcssipaidlservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcssipaidlservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.data.cne.internal.server-V1.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.data.cne.internal.server-V1.2-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.api-V1.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.api-V1.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V1.0-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.data.qmi-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.data.qmi-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.data.factory-V2.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.data.factory-V2.1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.constants-V2.1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.constants-V2.1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.datachannelservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.datachannelservice-V1-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.datachannelservice-V1-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.datachannelservice-V1-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V2.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V2.2-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.factory-V2.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.factory-V2.2-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.hardware.data.cne.internal.server-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.hardware.data.cne.internal.server-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := tcmclient
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/tcmclient_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcssip-V1.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcssip-V1.2-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := vendor.qti.ims.rcssip-V1.2-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/vendor.qti.ims.rcssip-V1.2-java_intermediates/classes.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_MODULE               := com.quicinc.cne.server-V1.0-java
LOCAL_MODULE_OWNER         := qcom
LOCAL_MODULE_TAGS          := optional
LOCAL_MODULE_CLASS         := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX        := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_SRC_FILES            := ../../.././target/common/obj/JAVA_LIBRARIES/com.quicinc.cne.server-V1.0-java_intermediates/classes-header.jar
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(MODULE.TARGET.$(LOCAL_MODULE_CLASS).$(LOCAL_MODULE)),)
include $(BUILD_PREBUILT)
endif
