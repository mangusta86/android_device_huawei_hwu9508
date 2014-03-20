# Copyright (C) 2007 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# inherit from the proprietary version
#include vendor/huawei/u9508/BoardConfigVendor.mk

TARGET_SPECIFIC_HEADER_PATH := device/huawei/u9508/overlay/include


# Platform

TARGET_CPU_ABI := armeabi-v7a
TARGET_CPU_ABI2 := armeabi
TARGET_CPU_SMP := true
TARGET_ARCH := arm
TARGET_ARCH_VARIANT := armv7-a-neon
TARGET_ARCH_VARIANT_CPU := cortex-a9
ARCH_ARM_HAVE_NEON := true
ARCH_ARM_HAVE_TLS_REGISTER := true
TARGET_GLOBAL_CFLAGS += -mtune=cortex-a9 -mfpu=neon -mfloat-abi=softfp
TARGET_GLOBAL_CPPFLAGS += -mtune=cortex-a9 -mfpu=neon -mfloat-abi=softfp

TARGET_NO_BOOTLOADER := true


# kernel
BOARD_KERNEL_CMDLINE := console=ttyS0 vmalloc=384M k3v2_pmem=1 mmcparts=mmcblk0:p1(xloader),p3(nvme),p4(misc),p5(splash),p6(oeminfo),p7(reserved1),p8(reserved2),p9(recovery2),p10(recovery),p11(boot),p12(modemimage),p13(modemnvm1),p14(modemnvm2),p15(system),p16(cache),p17(cust),p18(userdata);mmcblk1:p1(ext_sdcard)
BOARD_KERNEL_PAGESIZE := 2048 
TARGET_PREBUILT_KERNEL := device/huawei/u9508/kernel
#u9508 specific files
#TARGET_KERNEL_SOURCE := kernel/huawei/k3v2oem1
#TARGET_KERNEL_CONFIG := cyanogenmod_u9508_defconfig


# Bluetooth
BOARD_HAVE_BLUETOOTH := true
BOARD_HAVE_BLUETOOTH_BCM := true
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := device/huawei/u9508/bluetooth

TARGET_NO_RADIOIMAGE := true

TARGET_BOARD_PLATFORM := k3v2oem1
TARGET_BOOTLOADER_BOARD_NAME := u9508
TARGET_BOOTLOADER_NAME= u9508
TARGET_BOARD_INFO_FILE := device/oppo/find5/board-info.txt

TARGET_PROVIDES_INIT := true
TARGET_PROVIDES_INIT_TARGET_RC := true




# USB
#TARGET_USE_CUSTOM_SECOND_LUN_NUM := 1


# filesystem
BOARD_BOOTIMAGE_PARTITION_SIZE := 8388608
BOARD_RECOVERYIMAGE_PARTITION_SIZE := 8388608
TARGET_USERIMAGES_USE_EXT4 := true
BOARD_SYSTEM_DEVICE := /dev/block/mmcblk0p15
BOARD_SYSTEM_FILESYSTEM := ext4
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 939524096
BOARD_DATA_DEVICE := /dev/block/mmcblk0p18
BOARD_DATA_FILESYSTEM := ext4
BOARD_USERDATAIMAGE_PARTITION_SIZE := 5926551552
BOARD_CACHE_DEVICE := /dev/block/mmcblk0p16
BOARD_CACHE_FILESYSTEM := ext4
BOARD_FLASH_BLOCK_SIZE := 131072


#Graphics
BOARD_EGL_CFG := device/huawei/u9508/prebuilts/lib/egl/egl.cfg

# HWComposer
BOARD_USES_HWCOMPOSER := true

# Enable WEBGL in WebKit
ENABLE_WEBGL := true

# Wifi 
BOARD_WPA_SUPPLICANT_DRIVER := NL80211
WPA_SUPPLICANT_VERSION      := VER_0_8_X
BOARD_WPA_SUPPLICANT_PRIVATE_LIB := lib_driver_cmd_bcmdhd
BOARD_HOSTAPD_DRIVER        := NL80211
BOARD_HOSTAPD_PRIVATE_LIB   := lib_driver_cmd_bcmdhd
BOARD_WLAN_DEVICE           := bcmdhd
WIFI_DRIVER_FW_PATH_PARAM   := "/sys/module/bcmdhd/parameters/firmware_path"
WIFI_DRIVER_FW_PATH_STA     := "/system/vendor/firmware/fw_bcmdhd.bin"
WIFI_DRIVER_FW_PATH_P2P     := "/system/vendor/firmware/fw_bcmdhd_p2p.bin"
WIFI_DRIVER_FW_PATH_AP      := "/system/vendor/firmware/fw_bcmdhd_apsta.bin"


## Audio
BOARD_USES_GENERIC_AUDIO := true
#TARGET_PROVIDES_LIBAUDIO := true
#BOARD_PREBUILT_LIBAUDIO := true
#BOARD_USES_GENERIC_AUDIO := false
#BOARD_USES_QCOM_AUDIO_V2 := true
#BUILD_WITHOUT_PV := false

## Lights
#TARGET_PROVIDES_LIBLIGHTS := true

# RIL
#BOARD_RIL_CLASS := ../../../device/huawei/u9508/ril/

# Camera
USE_CAMERA_STUB := true
#COMMON_GLOBAL_CFLAGS += -DMR0_CAMERA_BLOB
#BOARD_USE_FROYO_LIBCAMERA := true
#BOARD_CAMERA_USE_GETBUFFERINFO := true
#BOARD_USE_CAF_LIBCAMERA := true
#USE_CAMERA_STUB := false
# BOARD_USE_REVERSE_FFC := true

## FM Radio
#BOARD_HAVE_QCOM_FM := true
#COMMON_GLOBAL_CFLAGS += -DQCOM_FM_ENABLED
BOARD_HAVE_FM_RADIO := true
#BOARD_GLOBAL_CFLAGS += -DHAVE_FM_RADIO
#BOARD_FM_DEVICE := tavarua

# Gps


# HDMI
#TARGET_HAVE_HDMI_OUT := true

# adb has root
ADDITIONAL_DEFAULT_PROPERTIES += ro.secure=0
ADDITIONAL_DEFAULT_PROPERTIES += ro.allow.mock.location=1
ADDITIONAL_DEFAULT_PROPERTIES += persist.sys.usb.config=mass_storage


###################################
#
## Recovery - BEGIN
#
###################################

BOARD_TOUCH_RECOVERY := true
#TARGET_RECOVERY_INITRC := device/huawei/u9508/recovery/recovery.rc
TARGET_RECOVERY_FSTAB := device/huawei/u9508/recovery/etc/recovery.fstab
#TARGET_RECOVERY_FSTAB := device/huawei/u9508/recovery/etc/recovery.fstab2
RECOVERY_FSTAB_VERSION := 2
BOARD_HAS_NO_SELECT_BUTTON := true
BOARD_CUSTOM_RECOVERY_KEYMAPPING := ../../device/huawei/u9508/recovery/recovery_keys.c
# BOARD_RECOVERY_HANDLES_MOUNT 
# RECOVERY_EXTEND_NANDROID_MENU 
DEVICE_RESOLUTION := 720x1280
#TARGET_RECOVERY_PIXEL_FORMAT := "RGBX_8888"
#BOARD_USE_CUSTOM_RECOVERY_FONT := \"roboto_23x41.h\"

BOARD_RECOVERY_SWIPE := true

# USB mass storage
TARGET_USE_CUSTOM_LUN_FILE_PATH := /sys/devices/hisik3-usb-otg/gadget/lun0/file
BOARD_MTP_DEVICE := "/dev/mtp_usb"
BOARD_VOLD_MAX_PARTITIONS := 19
BOARD_VOLD_EMMC_SHARES_DEV_MAJOR := true
#BOARD_USE_USB_MASS_STORAGE_SWITCH := true
#BOARD_UMS_LUNFILE := "/sys/devices/hisik3-usb-otg/gadget/lun0/file"

#TWRP
HAVE_SELINUX := false


RECOVERY_GRAPHICS_USE_LINELENGTH := true

#TW_ALWAYS_RMRF := true
#TW_NEVER_UMOUNT_SYSTEM := true

#SP2_NAME := "osh"
#SP2_DISPLAY_NAME := "Webtop"
#SP2_BACKUP_METHOD := files
#SP2_MOUNTABLE := 1

TW_CUSTOM_BATTERY_PATH := "/sys/devices/platform/k3_battery_monitor.1/power_supply/Battery"
TW_BRIGHTNESS_PATH := /sys/class/leds/lcd_backlight0/brightness
TW_MAX_BRIGHTNESS := 255

RECOVERY_SDCARD_ON_DATA := true 
#TW_HAS_NO_RECOVERY_PARTITION := true
TW_FLASH_FROM_STORAGE := true
TW_DEFAULT_EXTERNAL_STORAGE := true

# dual storage configuration
TW_EXTERNAL_STORAGE_PATH := "/sdcard"
TW_EXTERNAL_STORAGE_MOUNT_POINT := "/sdcard"
TW_INTERNAL_STORAGE_PATH := "/data/share"
TW_INTERNAL_STORAGE_MOUNT_POINT := "/data"



TW_INCLUDE_JB_CRYPTO := true
#TW_CRYPTO_FS_TYPE := "ext4"
#TW_CRYPTO_REAL_BLKDEV := "/dev/block/platform/hi_mci.1/by-name/userdata"
#TW_CRYPTO_MNT_POINT := "/data"




###################################
#
## Recovery - END
#
###################################


TARGET_OTA_ASSERT_DEVICE := u9508,U9508,U9508B,u9508B,hwu9508,hwu9508B

