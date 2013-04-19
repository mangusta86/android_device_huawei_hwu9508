$(call inherit-product, $(SRC_TARGET_DIR)/product/languages_full.mk)

# The gps config appropriate for this device
$(call inherit-product, device/common/gps/gps_us_supl.mk)

$(call inherit-product-if-exists, vendor/huawei/u9508/u9508-vendor.mk)

DEVICE_PACKAGE_OVERLAYS += device/huawei/u9508/overlay

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

#init
#   $(LOCAL_PATH)/prebuilt/init.huawei.usb.rc:root/init.huawei.usb.rc \
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/prebuilt/init.rc:root/init.rc \
    $(LOCAL_PATH)/prebuilt/init.huawei.rc:root/init.huawei.rc \
    $(LOCAL_PATH)/prebuilt/init.usb.rc:root/init.usb.rc \
    $(LOCAL_PATH)/prebuilt/ueventd.huawei.rc:root/ueventd.huawei.rc

# vold
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/configs/vold.fstab:system/etc/vold.fstab

#configs
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/gps.conf:system/etc/gps.conf \
    $(LOCAL_PATH)/configs/gpsconfig.xml:system/etc/gpsconfig.xml \
    $(LOCAL_PATH)/configs/media_codecs.xml:system/etc/media_codecs.xml \
    $(LOCAL_PATH)/configs/media_profiles.xml:system/etc/media_profiles.xml

# These are the hardware-specific features
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
    $(LOCAL_PATH)/configs/permissions/android.software.live_wallpaper.xml:system/etc/permissions/android.software.live_wallpaper.xml \
    $(LOCAL_PATH)/configs/permissions/android.software.sip.voip.xml:system/etc/permissions/android.software.sip.voip.xml \
    $(LOCAL_PATH)/configs/permissions/com.android.location.provider.xml:system/etc/permissions/com.android.location.provider.xml \
    $(LOCAL_PATH)/configs/permissions/com.broadcom.bt.le.xml:system/etc/permissions/com.broadcom.bt.le.xml \
    $(LOCAL_PATH)/configs/permissions/com.broadcom.bt.xml:system/etc/permissions/com.broadcom.bt.xml \
    $(LOCAL_PATH)/configs/permissions/com.google.android.maps.xml:system/etc/permissions/com.google.android.maps.xml \
    $(LOCAL_PATH)/configs/permissions/com.google.android.media.effects.xml:system/etc/permissions/com.google.android.media.effects.xml \
    $(LOCAL_PATH)/configs/permissions/com.google.widevine.software.drm.xml:system/etc/permissions/com.google.widevine.software.drm.xml \
    $(LOCAL_PATH)/configs/permissions/com.huawei.hwextcamera.xml:system/etc/permissions/com.huawei.hwextcamera.xml \
    $(LOCAL_PATH)/configs/permissions/features.xml:system/etc/permissions/features.xml \
    $(LOCAL_PATH)/configs/permissions/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml \
    $(LOCAL_PATH)/configs/permissions/hwframework.xml:system/etc/permissions/hwframework.xml \
    $(LOCAL_PATH)/configs/permissions/platform.xml:system/etc/permissions/platform.xml


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

# Filesystem management tools
PRODUCT_PACKAGES += \
	make_ext4fs \
	e2fsck \
	setup_fs

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

$(call inherit-product, frameworks/native/build/phone-xhdpi-1024-dalvik-heap.mk)

$(call inherit-product, build/target/product/full.mk)

PRODUCT_BUILD_PROP_OVERRIDES += BUILD_UTC_DATE=0
PRODUCT_NAME := full_u9508
PRODUCT_DEVICE := u9508
