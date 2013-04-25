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
# build for sapphire hardware. This cleanly combines a set of 
# device-specific aspects (drivers) with a device-agnostic
# product configuration (apps).
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/languages_full.mk)

# The gps config appropriate for this device
$(call inherit-product, device/common/gps/gps_us_supl.mk)

$(call inherit-product-if-exists, vendor/huawei/u9508/u9508-vendor.mk)

DEVICE_PACKAGE_OVERLAYS += device/huawei/u9508/overlay

#PRODUCT_BUILD_PROP_OVERRIDES += BUILD_UTC_DATE=0
PRODUCT_NAME := huawei_u9508
PRODUCT_DEVICE := u9508
PRODUCT_MODEL := huawei u9508
PRODUCT_MANUFACTURER := huawei

# high-density artwork where available
PRODUCT_AAPT_CONFIG := normal hdpi

LOCAL_PATH := device/huawei/u9508
ifeq ($(TARGET_PREBUILT_KERNEL),)
	LOCAL_KERNEL := $(LOCAL_PATH)/kernel
else
	LOCAL_KERNEL := $(TARGET_PREBUILT_KERNEL)
endif

PRODUCT_COPY_FILES += \
    $(LOCAL_KERNEL):kernel

# init files from Huawei initramfs B627
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/init.rc:root/init.rc \
    $(LOCAL_PATH)/init.huawei.rc:root/init.huawei.rc \
    $(LOCAL_PATH)/init.usb.rc:root/init.usb.rc \
    $(LOCAL_PATH)/ueventd.huawei.rc:root/ueventd.huawei.rc

# Install the features available on this device.
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/permissions/android.hardware.camera.autofocus.xml:system/etc/permissions/android.hardware.camera.autofocus.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.camera.flash-autofocus.xml:system/etc/permissions/android.hardware.camera.flash-autofocus.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.camera.front.xml:system/etc/permissions/android.hardware.camera.front.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.location.gps.xml:system/etc/permissions/android.hardware.location.gps.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.sensor.light.xml:system/etc/permissions/android.hardware.sensor.light.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.sensor.proximity.xml:system/etc/permissions/android.hardware.sensor.proximity.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.telephony.gsm.xml:system/etc/permissions/android.hardware.telephony.gsm.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.touchscreen.multitouch.xml:system/etc/permissions/android.hardware.touchscreen.multitouch.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.usb.host.xml:system/etc/permissions/android.hardware.usb.host.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    $(LOCAL_PATH)/configs/permissions/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    $(LOCAL_PATH)/configs/permissions/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml 

# Key maps (keylayouts and keychars)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/AVRCP.kl:system/usr/keylayout/AVRCP.kl \
    $(LOCAL_PATH)/configs/Generic.kl:system/usr/keylayout/Generic.kl \
    $(LOCAL_PATH)/configs/k3_keypad.kl:system/usr/keylayout/k3_keypad.kl \
    $(LOCAL_PATH)/configs/qwerty.kl:system/usr/keylayout/qwerty.kl \
    $(LOCAL_PATH)/configs/Vendor_05ac_Product_0239.kl:system/usr/keylayout/Vendor_05ac_Product_0239.kl \
    $(LOCAL_PATH)/configs/Vendor_22b8_Product_093d.kl:system/usr/keylayout/Vendor_22b8_Product_093d.kl \
    $(LOCAL_PATH)/configs/Vendor_045e_Product_028e.kl:system/usr/keylayout/Vendor_045e_Product_028e.kl \
    $(LOCAL_PATH)/configs/Vendor_046d_Product_c216.kl:system/usr/keylayout/Vendor_046d_Product_c216.kl \
    $(LOCAL_PATH)/configs/Vendor_046d_Product_c294.kl:system/usr/keylayout/Vendor_046d_Product_c294.kl \
    $(LOCAL_PATH)/configs/Vendor_046d_Product_c299.kl:system/usr/keylayout/Vendor_046d_Product_c299.kl \
    $(LOCAL_PATH)/configs/Vendor_046d_Product_c532.kl:system/usr/keylayout/Vendor_046d_Product_c532.kl \
    $(LOCAL_PATH)/configs/Vendor_054c_Product_0268.kl:system/usr/keylayout/Vendor_054c_Product_0268.kl \
    $(LOCAL_PATH)/configs/Generic.kcm:system/usr/keychars/Generic.kcm \
    $(LOCAL_PATH)/configs/qwerty.kcm:system/usr/keychars/qwerty.kcm \
    $(LOCAL_PATH)/configs/qwerty2.kcm:system/usr/keychars/qwerty2.kcm \
    $(LOCAL_PATH)/configs/Virtual.kcm:system/usr/keychars/Virtual.kcm 

# Input device calibration files
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/configs/hisik3_touchscreen.idc:system/usr/idc/hisik3_touchscreen.idc \
	$(LOCAL_PATH)/configs/k3_keypad.idc:system/usr/idc/k3_keypad.idc \
	$(LOCAL_PATH)/configs/qwerty.idc:system/usr/idc/qwerty.idc \
    	$(LOCAL_PATH)/configs/qwerty2.idc:system/usr/idc/qwerty2.idc \
	$(LOCAL_PATH)/configs/synaptics.idc:system/usr/idc/synaptics.idc

PRODUCT_PACKAGES += \
	librs_jni \
	com.android.future.usb.accessory
#PRODUCT_PACKAGES += \
#    LiveWallpapers \
#    LiveWallpapersPicker \
#    VisualizationWallpapers \
#    MagicSmokeWallpapers \
#    VisualizationWallpapers \
#    Gallery3d \
#    SpareParts \
#    Term \
#    librs_jni \
#    overlay.default \
#    gps.u8800 \
#    gralloc.u8800 \
#    copybit.u8800 \
#    lights.u8800 \
#    sensors.blade \
#    libOmxCore \
#    libOmxVdec \

#DISABLE_DEXPREOPT := false
#PRODUCT_LOCALES += hdpi
#PRODUCT_COPY_FILES += \
#    device/huawei/u8800/qwerty.kl:system/usr/keylayout/qwerty.kl


# Firmware

# Firmware wlan

# EGL

# Gralloc

# etc
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/vold.fstab:system/etc/vold.fstab \
    $(LOCAL_PATH)/configs/media_profiles.xml:system/etc/media_profiles.xml 

#    device/huawei/u8800/wpa_supplicant.conf:/system/etc/wifi/wpa_supplicant.conf 

#    device/huawei/u8800/vold.fstab:/system/etc/vold.fstab \
#    device/huawei/u8800/init.qcom.bt.sh:/system/etc/init.qcom.bt.sh \
#    device/huawei/u8800/init.qcom.sdio.sh:/system/etc/init.qcom.sdio.sh \
#    device/huawei/u8800/init.qcom.wifi.sh:/system/etc/init.qcom.wifi.sh \
#    device/huawei/u8800/init.qcom.coex.sh:/system/etc/init.qcom.coex.sh \
#    device/huawei/u8800/media_profiles.xml:/system/etc/media_profiles.xml \

# RIL (proprietry)

# other bin (proprietry)

# Audio
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/configs/audio_policy.conf:system/etc/audio_policy.conf 

# $(LOCAL_PATH)/configs/audio/front_audio_config.conf:system/etc/huawei/audio/front_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/front_factory_audio_config.conf:system/etc/huawei/audio/front_factory_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/u9508_audio_config.conf:system/etc/huawei/audio/u9508_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/u9508_factory_audio_config.conf:system/etc/huawei/audio/u9508_factory_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/viva_audio_config.conf:system/etc/huawei/audio/viva_audio_config.conf \
# $(LOCAL_PATH)/configs/audio/viva_factory_audio_config.conf:system/etc/huawei/audio/viva_factory_audio_config.conf

# Bluetooth 
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/init.bcm.chip_off.sh:system/etc/bluetooth/init.bcm.chip_off.sh \
    $(LOCAL_PATH)/configs/init.bcm.chip_on.sh:system/etc/bluetooth/init.bcm.chip_on.sh

# Camera
PRODUCT_PACKAGES := \
    Camera

# Sensors (Proprietry)

# Wifi 

# Huawei libs(proprietry)

#configs remainings (just disabled)
#PRODUCT_COPY_FILES += \
#    $(LOCAL_PATH)/configs/gps.conf:system/etc/gps.conf \
#    $(LOCAL_PATH)/configs/gpsconfig.xml:system/etc/gpsconfig.xml \
#    $(LOCAL_PATH)/configs/media_codecs.xml:system/etc/media_codecs.xml 


# Filesystem management tools
PRODUCT_PACKAGES += \
	make_ext4fs \
	e2fsck \
	setup_fs

# we have enough storage space to hold precise GC data
PRODUCT_TAGS += dalvik.gc.type-precise





$(call inherit-product, frameworks/native/build/phone-xhdpi-1024-dalvik-heap.mk)

$(call inherit-product, build/target/product/full.mk)


