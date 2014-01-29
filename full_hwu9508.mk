# Copyright (C) 2009 The Android Open Source Project
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

#
# This file is the build configuration for a full Android
# build for crespo hardware. This cleanly combines a set of
# device-specific aspects (drivers) with a device-agnostic
# product configuration (apps).
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# This is where we'd set a backup provider if we had one
# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/languages_full.mk)

# The gps config appropriate for this device
$(call inherit-product, device/common/gps/gps_us_supl.mk)

# Inherit from vendor specs
$(call inherit-product-if-exists, vendor/huawei/hwu9508/hwu9508-vendor.mk)

LOCAL_PATH := device/huawei/hwu9508
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# high-density artwork where available
PRODUCT_AAPT_CONFIG := normal mdpi hdpi
PRODUCT_AAPT_PREF_CONFIG := hdpi


#ifeq ($(TARGET_PREBUILT_KERNEL),)
#	LOCAL_KERNEL := $(LOCAL_PATH)/kernel
#else
#	LOCAL_KERNEL := $(TARGET_PREBUILT_KERNEL)
#endif

#PRODUCT_COPY_FILES += \
#    $(LOCAL_KERNEL):kernel

# init files from Huawei initramfs
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/init.rc:root/init.rc \
    $(LOCAL_PATH)/rootdir/init.trace.rc:root/init.trace.rc \
    $(LOCAL_PATH)/rootdir/init.k3v2oem1.rc:root/init.k3v2oem1.rc \
    $(LOCAL_PATH)/rootdir/init.usb.rc:root/init.usb.rc \
    $(LOCAL_PATH)/rootdir/ueventd.rc:root/ueventd.rc \
    $(LOCAL_PATH)/rootdir/ueventd.k3v2oem1.rc:root/ueventd.k3v2oem1.rc \
    $(LOCAL_PATH)/rootdir/ueventd.rc:recovery/root/ueventd.rc \
    $(LOCAL_PATH)/rootdir/ueventd.k3v2oem1.rc:recovery/root/ueventd.k3v2oem1.rc \
    $(LOCAL_PATH)/rootdir/fstab.k3v2oem1:recovery/root/fstab.k3v2oem1 

# Install the features available on this device.
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/permissions/android.hardware.camera.autofocus.xml:system/etc/permissions/android.hardware.camera.autofocus.xml \
    $(LOCAL_PATH)/permissions/android.hardware.camera.flash-autofocus.xml:system/etc/permissions/android.hardware.camera.flash-autofocus.xml \
    $(LOCAL_PATH)/permissions/android.hardware.camera.front.xml:system/etc/permissions/android.hardware.camera.front.xml \
    $(LOCAL_PATH)/permissions/android.hardware.location.gps.xml:system/etc/permissions/android.hardware.location.gps.xml \
    $(LOCAL_PATH)/permissions/android.hardware.sensor.light.xml:system/etc/permissions/android.hardware.sensor.light.xml \
    $(LOCAL_PATH)/permissions/android.hardware.sensor.proximity.xml:system/etc/permissions/android.hardware.sensor.proximity.xml \
    $(LOCAL_PATH)/permissions/android.hardware.telephony.gsm.xml:system/etc/permissions/android.hardware.telephony.gsm.xml \
    $(LOCAL_PATH)/permissions/android.hardware.touchscreen.multitouch.xml:system/etc/permissions/android.hardware.touchscreen.multitouch.xml \
    $(LOCAL_PATH)/permissions/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml \
    $(LOCAL_PATH)/permissions/android.hardware.usb.host.xml:system/etc/permissions/android.hardware.usb.host.xml \
    $(LOCAL_PATH)/permissions/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
   $(LOCAL_PATH)/permissions/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    $(LOCAL_PATH)/permissions/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml 

# Include keyboards
$(call inherit-product-if-exists, device/huawei/hwu9508/keyboards/keyboards.mk)

# packages
PRODUCT_PACKAGES += \
#    audio.a2dp.default \
#    Camera \
    com.android.future.usb.accessory 
#    Torch 

# HAL
#PRODUCT_PACKAGES += \
#	librs_jni \
#    libhwconverter \
#    libs5pjpeg \
#    libfimg

# Charger
#PRODUCT_PACKAGES += \
#    charger \
#    charger_res_images

# MFC API
#PRODUCT_PACKAGES += \
#    libsecmfcapi

# OMX
#PRODUCT_PACKAGES += \
#   LiveWallpapers \
#    LiveWallpapersPicker \
#    VisualizationWallpapers \
#    MagicSmokeWallpapers \
#    VisualizationWallpapers \
#    Gallery3d \
#    SpareParts \
#    Term \
#    librs_jni \
#    CMFileManager\
#    libOmxCore \
#    libOmxVdec 

# mount points SDCARDS
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/vold.fstab:system/etc/vold.fstab \
    $(LOCAL_PATH)/configs/internal_sd.fstab:system/etc/internal_sd.fstab 

#video
PRODUCT_COPY_FILES += \
	 $(LOCAL_PATH)/configs/media_profiles.xml:system/etc/media_profiles.xml

# Audio
#PRODUCT_COPY_FILES += \
#	$(LOCAL_PATH)/configs/audio_policy.conf:system/etc/audio_policy.conf 

# $(LOCAL_PATH)/configs/audio/front_audio_config.conf:system/etc/huawei/audio/front_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/front_factory_audio_config.conf:system/etc/huawei/audio/front_factory_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/u9508_audio_config.conf:system/etc/huawei/audio/u9508_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/u9508_factory_audio_config.conf:system/etc/huawei/audio/u9508_factory_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/viva_audio_config.conf:system/etc/huawei/audio/viva_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/viva_factory_audio_config.conf:system/etc/huawei/audio/viva_factory_audio_config.conf
#   \

# Camera
#PRODUCT_PACKAGES := \
#    Camera

# Sensors (Proprietry)
#GPS
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/gps.conf:system/etc/gps.conf \
    $(LOCAL_PATH)/configs/gpsconfig.xml:system/etc/gpsconfig.xml 

# Wifi 
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/wpa_supplicant.conf:/system/etc/wifi/wpa_supplicant.conf 

# Bluetooth 
# Include initscripts
$(call inherit-product-if-exists, device/huawei/hwu9508/initscripts/initscripts.mk)



# we have enough storage space to hold precise GC data
PRODUCT_TAGS += dalvik.gc.type-precise


# Filesystem management tools
PRODUCT_PACKAGES += \
	make_ext4fs \
	e2fsck \
	setup_fs


$(call inherit-product-if-exists, frameworks/native/build/phone-xhdpi-1024-dalvik-heap.mk)

$(call inherit-product, build/target/product/full.mk)


# Discard inherited values and use our own instead.
PRODUCT_DEVICE := hwu9508
PRODUCT_NAME := hwu9508
PRODUCT_BRAND := huawei
PRODUCT_MODEL := U9508
PRODUCT_MANUFACTURER := HUAWEI

