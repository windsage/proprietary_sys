ifneq ($(strip $(TARGET_NO_BOOTLOADER)),true)
ifneq ($(strip $(QTI_GENSECIMAGE_MSM_IDS)),)

# Required:
#   QTI_GENSECIMAGE_MSM_IDS - space separated list of MSM ID's (e.g. 8084)
#   - This will trigger generation of the gensecimage signed bootloader
#     under $OUT/signed for each MSM_IDS in the list
# Optional:
#   QTI_GENSECIMAGE_SIGNED_DEFAULT - one of the MSM ID in the MSM_IDS list
#   - This will override default unsigned bootloader binaries with the
#     gensecimage signed one for the specified MSM ID.  That is, the default
#     bootloader binaries in $OUT will be gensecimage signed

#define basedir as LOCAL_PATH cannot be used with direct make target rules
#Chipsets 8916,8994 use a new tool called secimage while older chipsets like 8084 use gensecimage.
#Select the tool to be used for signing based on the chipset.

ifneq ($(filter $(TARGET_BOARD_PLATFORM), sdm845 sdm660 msm8937 msm8953),$(TARGET_BOARD_PLATFORM))
.PHONY: gensecimage_target
   TARGET_SIGNED_BOOTLOADER := $(PRODUCT_OUT)/signed/$(BOOTLOADER_EMMC)
   $(TARGET_SIGNED_BOOTLOADER): $(TARGET_EMMC_BOOTLOADER) $(SIGN_ABL) $(INSTALLED_BOOTLOADER_MODULE)
   gensecimage_target: $(TARGET_EMMC_BOOTLOADER) $(SIGN_ABL) $(INSTALLED_BOOTLOADER_MODULE)
else
# The following 3 bootloader image files are identical but with different names
BOOTLOADER_EMMC    = $(basename $(TARGET_EMMC_BOOTLOADER))
BOOTLOADER_EMMC2   = $(basename $(TARGET_BOOTLOADER))
BOOTLOADER_DEFAULT = $(basename $(INSTALLED_BOOTLOADER_MODULE))
ifeq ($(USESECIMAGETOOL), true)
   # SECIMAGE_BASE : sectools folder path
   ifneq ($(wildcard vendor/qcom/proprietary/sectools),)
     SECIMAGE_BASE := vendor/qcom/proprietary/sectools
   else
     SECIMAGE_BASE := vendor/qcom/proprietary/common/scripts/SecImage
   endif
else
GENSECIMAGE_BASE := vendor/qcom/proprietary/common/scripts/gensecimage
define gensectarget_param
$(eval GENSEC_MSM_PARAM :=) \
$(foreach t,$(1), \
  $(eval GENSEC_MSM_PARAM += --msmid=$(t)))
endef
endif

ifeq ($(USE_SOC_HW_VERSION), true)
   soc_hw_version = $(SOC_HW_VERSION)
   soc_vers = $(SOC_VERS)
endif

ifeq ($(TARGET_USES_UEFI), true)
SIGN_ID := abl
else
SIGN_ID := appsbl
endif

ifeq ($(USESECIMAGETOOL), true)
  ifeq ($(USE_SOC_HW_VERSION), true)
    # XML_FILE : this file will be used for singing abl and it might be different for each target
     ifeq ($(SECIMAGE_BASE), vendor/qcom/proprietary/sectools)
        ifeq ($(call is-board-platform-in-list,lahaina holi),true)
          XML_FILE := secimage_eccv3.xml
        else
          ifeq ($(call is-board-platform-in-list,sdm660),true)
            XML_FILE := secimagev2.xml
          else
            XML_FILE := secimagev3.xml
          endif
       endif
     else
       XML_FILE := secimagev2.xml
     endif
    define sec-image-generate
	@echo Generating signed appsbl using secimage tool for $(strip $(QTI_GENSECIMAGE_MSM_IDS))
	@rm -rf $(PRODUCT_OUT)/signed
	@rm -rf $(PRODUCT_OUT)/signed_ecc

	if [ "$(SECIMAGE_DUAL_SIGNING)" == "true" ]; then \
	SECIMAGE_LOCAL_DIR=$(SECIMAGE_BASE) USES_SEC_POLICY_MULTIPLE_DEFAULT_SIGN=$(USES_SEC_POLICY_MULTIPLE_DEFAULT_SIGN) \
			USES_SEC_POLICY_DEFAULT_SUBFOLDER_SIGN=$(USES_SEC_POLICY_DEFAULT_SUBFOLDER_SIGN) \
			USES_SEC_POLICY_INTEGRITY_CHECK=$(USES_SEC_POLICY_INTEGRITY_CHECK) python $(SECIMAGE_BASE)/sectools_builder.py \
		-i $(TARGET_EMMC_BOOTLOADER) \
		-t $(PRODUCT_OUT)/signed_ecc \
		-g $(SIGN_ID) \
		--soc_hw_version $(SOC_HW_VERSION_ECC) \
		--soc_vers $(SOC_VERS_ECC) \
		--config=$(SECIMAGE_BASE)/config/integration/secimage_eccv3.xml \
		--install_file_name=abl_ecc.elf \
		--install_base_dir=$(PRODUCT_OUT) \
		 > $(PRODUCT_OUT)/secimage_ecc.log 2>&1; \
	fi

	$(hide) SECIMAGE_LOCAL_DIR=$(SECIMAGE_BASE) USES_SEC_POLICY_MULTIPLE_DEFAULT_SIGN=$(USES_SEC_POLICY_MULTIPLE_DEFAULT_SIGN) \
			USES_SEC_POLICY_DEFAULT_SUBFOLDER_SIGN=$(USES_SEC_POLICY_DEFAULT_SUBFOLDER_SIGN) \
			USES_SEC_POLICY_INTEGRITY_CHECK=$(USES_SEC_POLICY_INTEGRITY_CHECK) python $(SECIMAGE_BASE)/sectools_builder.py \
		-i $(TARGET_EMMC_BOOTLOADER) \
		-t $(PRODUCT_OUT)/signed \
		-g $(SIGN_ID) \
		--soc_hw_version $(soc_hw_version) \
                --soc_vers $(soc_vers) \
                --config=$(SECIMAGE_BASE)/config/integration/$(XML_FILE) \
		--install_base_dir=$(PRODUCT_OUT) \
		 > $(PRODUCT_OUT)/secimage.log 2>&1
	@echo Completed secimage signed appsbl \(logs in $(PRODUCT_OUT)/secimage.log\)
    endef
  else
    define sec-image-generate
        @echo Generating signed appsbl using secimage tool for $(strip $(QTI_GENSECIMAGE_MSM_IDS))
        @rm -rf $(PRODUCT_OUT)/signed
        $(hide) SECIMAGE_LOCAL_DIR=$(SECIMAGE_BASE) USES_SEC_POLICY_MULTIPLE_DEFAULT_SIGN=$(USES_SEC_POLICY_MULTIPLE_DEFAULT_SIGN) \
                        USES_SEC_POLICY_DEFAULT_SUBFOLDER_SIGN=$(USES_SEC_POLICY_DEFAULT_SUBFOLDER_SIGN) \
                        USES_SEC_POLICY_INTEGRITY_CHECK=$(USES_SEC_POLICY_INTEGRITY_CHECK) python $(SECIMAGE_BASE)/sectools_builder.py \
                -i $(TARGET_EMMC_BOOTLOADER) \
                -t $(PRODUCT_OUT)/signed \
                -g $(SIGN_ID) \
                --config=$(SECIMAGE_BASE)/config/integration/secimage.xml \
                --install_base_dir=$(PRODUCT_OUT) \
                 > $(PRODUCT_OUT)/secimage.log 2>&1
        @echo Completed secimage signed appsbl \(logs in $(PRODUCT_OUT)/secimage.log\)
    endef
  endif
else
define sec-image-generate
	@echo Generating signed appsbl using gensecimage tool for $(strip $(QTI_GENSECIMAGE_MSM_IDS))
	@rm -rf $(PRODUCT_OUT)/signed
	$(call gensectarget_param,$(QTI_GENSECIMAGE_MSM_IDS))
	$(hide) GENSECIMAGE_LOCAL_DIR=$(GENSECIMAGE_BASE) python $(GENSECIMAGE_BASE)/gensecimage_builder.py \
		--section=appsbl \
		-t $(PRODUCT_OUT)/signed \
		-s $(TARGET_EMMC_BOOTLOADER) \
		$(GENSEC_MSM_PARAM) \
		--basic_cfg_template=$(GENSECIMAGE_BASE)/resources/gensecimage.appsbl.cfg \
		--signing_cfg_template=$(GENSECIMAGE_BASE)/resources/signingattr_qpsa.appsbl.cfg \
		--msm_jtag_mapping_file=$(GENSECIMAGE_BASE)/resources/msm_jtag_mapping.txt \
		> $(PRODUCT_OUT)/gensecimage.log 2>&1
	@echo Completed gensecimage signed appsbl \(logs in $(PRODUCT_OUT)/gensecimage.log\)
endef
endif

TARGET_SIGNED_BOOTLOADER := $(PRODUCT_OUT)/signed/$(BOOTLOADER_EMMC)
$(TARGET_SIGNED_BOOTLOADER): $(TARGET_EMMC_BOOTLOADER) $(INSTALLED_BOOTLOADER_MODULE)
	$(hide) $(call sec-image-generate)
endif

.PHONY: gensecimage_install
gensecimage_install: $(TARGET_SIGNED_BOOTLOADER)
	@echo -e `if [ -n "$(QTI_GENSECIMAGE_SIGNED_DEFAULT)" ] && \
	                    [ -d "$(PRODUCT_OUT)/signed/$(QTI_GENSECIMAGE_SIGNED_DEFAULT)" ]; then \
		rm -rf $(PRODUCT_OUT)/unsigned; \
		mkdir -p $(PRODUCT_OUT)/unsigned; \
		echo "Enabling gensecimage appsbl as default"; \
		cp $(TARGET_EMMC_BOOTLOADER) $(PRODUCT_OUT)/unsigned/$(BOOTLOADER_EMMC); \
		cp $(TARGET_EMMC_BOOTLOADER) $(PRODUCT_OUT)/unsigned/$(BOOTLOADER_EMMC2); \
		cp $(TARGET_EMMC_BOOTLOADER) $(PRODUCT_OUT)/unsigned/$(BOOTLOADER_DEFAULT); \
		cp $(PRODUCT_OUT)/signed/$(QTI_GENSECIMAGE_SIGNED_DEFAULT)/$(BOOTLOADER_EMMC) $(PRODUCT_OUT)/; \
		cp $(PRODUCT_OUT)/signed/$(QTI_GENSECIMAGE_SIGNED_DEFAULT)/$(BOOTLOADER_EMMC) $(PRODUCT_OUT)/$(BOOTLOADER_EMMC2); \
		cp $(PRODUCT_OUT)/signed/$(QTI_GENSECIMAGE_SIGNED_DEFAULT)/$(BOOTLOADER_EMMC) $(PRODUCT_OUT)/$(BOOTLOADER_DEFAULT); \
	fi`

ifeq ($(USESECIMAGETOOL), true)
droidcore: $(TARGET_SIGNED_BOOTLOADER)
droidcore-unbundled: $(TARGET_SIGNED_BOOTLOADER)
.PHONY:otapackage
otapackage: $(TARGET_SIGNED_BOOTLOADER)
else
droidcore: $(TARGET_SIGNED_BOOTLOADER) gensecimage_install
droidcore-unbundled: $(TARGET_SIGNED_BOOTLOADER) gensecimage_install
.PHONY:otapackage
otapackage: $(TARGET_SIGNED_BOOTLOADER) gensecimage_install
endif

endif
endif
