## BoardConfigVendor.mk
## Qualcomm Technologies, Inc proprietary product specific compile-time definitions.
#


## SECIMAGE_DUAL_SIGNING flag to enable the dual signing (RSA and ECC) of abl image.
## setting this to true will generate legacy abl.elf with RSA signing and abl_ecc.elf
## with ECC signing.
## Target using only RSA either set this flag to false or don't define it.
## Other can use this flag to enable the dual signing of their image or fw.

SECIMAGE_DUAL_SIGNING := true

##SECIMAGE tool feature flags
USES_SEC_POLICY_MULTIPLE_DEFAULT_SIGN := 1
USES_SEC_POLICY_INTEGRITY_CHECK := 1
USE_SOC_HW_VERSION := true
SOC_HW_VERSION := 0x90020100
SOC_VERS := 0x9002

## SOC_HW_VERSION_ECC & SOC_VERS_ECC defined for Scuba.
## These are used for ECC signing when SECIMAGE_DUAL_SIGNING is set to true.
## Other target with requirement to generate dual signed images can use these Macro.

SOC_HW_VERSION_ECC := 0x90030100
SOC_VERS_ECC := 0x9003

#Flags for generating signed images
USESECIMAGETOOL := true
QTI_GENSECIMAGE_MSM_IDS := vienna # Needs update for Vienna
QTI_GENSECIMAGE_SIGNED_DEFAULT := vienna # Needs update for Vienna
#USES_SEC_POLICY_MULTIPLE_DEFAULT_SIGN := 1
#USES_SEC_POLICY_INTEGRITY_CHECK := 1
HAVE_ADRENO_FIRMWARE := true


