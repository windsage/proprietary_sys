

IMS_RTP := vendor.qti.imsrtpservice@3.0
IMS_RTP += vendor.qti.imsrtpservice@3.0_product
IMS_RTP += vendor.qti.imsrtpservice@3.0_vendor

IMS_RTP += vendor.qti.imsrtpservice@3.1
IMS_RTP += vendor.qti.imsrtpservice@3.1_product
IMS_RTP += vendor.qti.imsrtpservice@3.1_vendor

IMS_RTP += vendor.qti.ImsRtpService-V1-ndk
IMS_RTP += vendor.qti.ImsrtpService-V1-ndk_product
IMS_RTP += vendor.qti.ImsrtpService-V1-ndk_vendor

ifneq ($(TARGET_HAS_LOW_RAM),true)
MSTAT := vendor.qti.mstatservice@1.0
MSTAT += vendor.qti.mstatservice@1.0-java
endif

CNE := com.quicinc.cne.api@1.0
CNE += com.quicinc.cne.api@1.1
CNE += com.quicinc.cne.server@1.0
CNE += com.quicinc.cne.server@2.0
CNE += com.quicinc.cne.server@2.1
CNE += com.quicinc.cne.server@2.2
CNE += com.quicinc.cne.constants@1.0
CNE += com.quicinc.cne.constants@2.0
CNE += com.quicinc.cne.constants@2.1
CNE += vendor.qti.hardware.data.cne.internal.api@1.0
CNE += vendor.qti.hardware.data.cne.internal.server@1.0
CNE += vendor.qti.hardware.data.cne.internal.server@1.1
CNE += vendor.qti.hardware.data.cne.internal.server@1.2
CNE += vendor.qti.hardware.data.cne.internal.server@1.3
CNE += vendor.qti.hardware.data.cne.internal.constants@1.0
CNE += vendor.qti.data.factory@1.0
CNE += vendor.qti.data.factory@2.0
CNE += vendor.qti.data.factory@2.1
CNE += vendor.qti.data.factory@2.2
CNE += vendor.qti.data.factory@2.3
CNE += vendor.qti.data.factory@2.4
CNE += vendor.qti.data.factory@2.5
CNE += vendor.qti.data.factory@2.6
CNE += vendor.qti.data.factory@2.7
CNE += vendor.qti.data.factory@2.8
CNE += vendor.qti.hardware.data.qmi@1.0
CNE += com.quicinc.cne.api-V1.0-java
CNE += com.quicinc.cne.api-V1.1-java
CNE += com.quicinc.cne.constants-V1.0-java
CNE += com.quicinc.cne.constants-V2.0-java
CNE += com.quicinc.cne.constants-V2.1-java
CNE += com.quicinc.cne.server-V1.0-java
CNE += com.quicinc.cne.server-V2.0-java
CNE += com.quicinc.cne.server-V2.1-java
CNE += com.quicinc.cne.server-V2.2-java
CNE += vendor.qti.hardware.data.cne.internal.api-V1.0-java
CNE += vendor.qti.hardware.data.cne.internal.server-V1.0-java
CNE += vendor.qti.hardware.data.cne.internal.server-V1.1-java
CNE += vendor.qti.hardware.data.cne.internal.server-V1.2-java
CNE += vendor.qti.hardware.data.cne.internal.server-V1.3-java
CNE += vendor.qti.hardware.data.cne.internal.constants-V1.0-java
CNE += vendor.qti.data.factory-V1.0-java
CNE += vendor.qti.data.factory-V2.0-java
CNE += vendor.qti.data.factory-V2.1-java
CNE += vendor.qti.data.factory-V2.2-java
CNE += vendor.qti.hardware.data.qmi-V1.0-java
CNE += vendor.qti.hardware.slmadapter@1.0
CNE += vendor.qti.hardware.slmadapter-V1.0-java
CNE += vendor.qti.hardware.mwqemadapter@1.0
CNE += vendor.qti.hardware.mwqemadapter-V1.0-java
CNE += vendor.qti.hardware.data.lce@1.0
CNE += vendor.qti.hardware.data.lce-V1.0-java

CNE += vendor.qti.data.factory-V2.2-java
CNE += vendor.qti.data.factory-V2.3-java
CNE += vendor.qti.data.factory-V2.4-java
CNE += vendor.qti.data.factory-V2.5-java
CNE += vendor.qti.data.factory-V2.6-java
CNE += vendor.qti.data.factory-V2.7-java
CNE += vendor.qti.data.factory-V2.8-java
CNE += vendor.qti.hardware.data.qmi-V1.0-java
CNE += vendor.qti.hardware.slmadapter@1.0
CNE += vendor.qti.hardware.slmadapter-V1.0-java
CNE += vendor.qti.hardware.mwqemadapter@1.0
CNE += vendor.qti.hardware.mwqemadapter-V1.0-java
CNE += vendor.qti.data.factoryservice-V1-ndk
CNE += vendor.qti.data.factoryservice-V1-java
CNE += vendor.qti.hardware.data.qmiaidlservice-V1-ndk
CNE += vendor.qti.hardware.data.qmiaidlservice-V1-java
CNE += vendor.qti.hardware.data.cneaidlservice.internal.constants-V1-ndk
CNE += vendor.qti.hardware.data.cneaidlservice.internal.constants-V1-java
CNE += vendor.qti.hardware.data.cneaidlservice.internal.api-V1-ndk
CNE += vendor.qti.hardware.data.cneaidlservice.internal.api-V1-java
CNE += vendor.qti.hardware.data.cneaidlservice.internal.server-V1-ndk
CNE += vendor.qti.hardware.data.cneaidlservice.internal.server-V1-java
CNE += vendor.qti.hardware.mwqemadapteraidlservice-V1-ndk
CNE += vendor.qti.hardware.mwqemadapteraidlservice-V1-java

CACERT := vendor.qti.hardware.cacert@1.0
CACERT += vendor.qti.hardware.cacertaidlservice-V1-ndk
CACERT += vendor.qti.hardware.cacertaidlservice-V1-java

DPM := com.qualcomm.qti.dpm.api@1.0
DPM += com.qualcomm.qti.dpm.api@1.0_system
DPM += vendor.qti.hardware.dpmservice@1.0
DPM += vendor.qti.hardware.dpmservice-V1.0-java
DPM += vendor.qti.hardware.dpmservice@1.1
DPM += vendor.qti.hardware.dpmservice-V1.1-java
DPM += vendor.qti.hardware.dpmaidlservice-V1-java
DPM += vendor.qti.hardware.dpmaidlservice-V1-ndk

IMS := vendor.qti.ims.factory@1.0
IMS += vendor.qti.ims.factory-V1.0-java
IMS += vendor.qti.ims.factory@1.1
IMS += vendor.qti.ims.factory-V1.1-java
IMS += vendor.qti.ims.factory@2.0
IMS += vendor.qti.ims.factory-V2.0-java
IMS += vendor.qti.ims.factory@2.1
IMS += vendor.qti.ims.factory-V2.1-java
IMS += vendor.qti.ims.factory@2.2
IMS += vendor.qti.ims.factory-V2.2-java
IMS += vendor.qti.ims.connection@1.0
IMS += vendor.qti.ims.connection-V1.0-java
IMS += vendor.qti.ims.rcsuce@1.0
IMS += vendor.qti.ims.rcsuce-V1.0-java
IMS += vendor.qti.ims.rcsuce@1.1
IMS += vendor.qti.ims.rcsuce-V1.1-java
IMS += vendor.qti.ims.rcsuce@1.2
IMS += vendor.qti.ims.rcsuce-V1.2-java
IMS += vendor.qti.ims.rcssip@1.0
IMS += vendor.qti.ims.rcssip-V1.0-java
IMS += vendor.qti.ims.rcssip@1.1
IMS += vendor.qti.ims.rcssip-V1.1-java
IMS += vendor.qti.ims.rcssip@1.2
IMS += vendor.qti.ims.rcssip-V1.2-java
IMS += vendor.qti.ims.datachannelservice-V1-java
IMS += vendor.qti.ims.datachannelservice-V1-ndk
IMS += vendor.qti.ims.datachannelservice-V2-java
IMS += vendor.qti.ims.datachannelservice-V2-ndk
IMS += vendor.qti.ims.rcsuceaidlservice-V1-java
IMS += vendor.qti.ims.rcsuceaidlservice-V1-ndk
IMS += vendor.qti.ims.rcssipaidlservice-V1-java
IMS += vendor.qti.ims.rcssipaidlservice-V1-ndk
IMS += vendor.qti.ims.connectionaidlservice-V1-java
IMS += vendor.qti.ims.connectionaidlservice-V1-ndk
IMS += vendor.qti.ims.factoryaidlservice-V1-java
IMS += vendor.qti.ims.factoryaidlservice-V1-ndk
IMS += vendor.qti.ims.datachannelservice-V3-java
IMS += vendor.qti.ims.datachannelservice-V3-ndk

QMS_HAL := vendor.qti.data.txpwrservice-V1-java
QMS_HAL += vendor.qti.data.txpwrservice-V1-ndk
QMS_HAL += vendor.qti.data.txpwrservice-V2-java
QMS_HAL += vendor.qti.data.txpwrservice-V2-ndk
QMS_HAL += vendor.qti.data.txpwrservice-V3-java
QMS_HAL += vendor.qti.data.txpwrservice-V3-ndk
QMS_HAL += vendor.qti.data.txpwrservice-V4-java
QMS_HAL += vendor.qti.data.txpwrservice-V4-ndk
QMS_HAL += vendor.qti.data.txpwrservice-V5-java
QMS_HAL += vendor.qti.data.txpwrservice-V5-ndk
QMS_HAL += vendor.qti.data.txpwrservice-V6-java
QMS_HAL += vendor.qti.data.txpwrservice-V6-ndk

QMS_HAL += vendor.qti.data.ntn-V1-java
QMS_HAL += vendor.qti.data.ntn-V1-ndk

ifneq ($(TARGET_SUPPORTS_WEARABLES),true)
QMS_HAL += vendor.qti.data.dmapconsent-V1-java
QMS_HAL += vendor.qti.data.dmapconsent-V1-ndk
QMS_HAL += vendor.qti.data.dmapconsent-V2-java
QMS_HAL += vendor.qti.data.dmapconsent-V2-ndk
endif

QHDC_HAL := vendor.qti.qepi-V1-ndk

PRODUCT_PACKAGES += $(IMS_RTP)
PRODUCT_PACKAGES += $(CNE)
PRODUCT_PACKAGES += $(CACERT)
PRODUCT_PACKAGES += $(DPM)
PRODUCT_PACKAGES += $(IMS)
PRODUCT_PACKAGES += $(QMS_HAL)
PRODUCT_PACKAGES += $(QHDC_HAL)

ifneq ($(TARGET_HAS_LOW_RAM),true)
PRODUCT_PACKAGES += $(MSTAT)
endif
