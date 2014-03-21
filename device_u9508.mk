
$(call inherit-product, $(SRC_TARGET_DIR)/product/languages_full.mk)

# The gps config appropriate for this device
$(call inherit-product, device/common/gps/gps_us_supl.mk)

$(call inherit-product-if-exists, vendor/huawei/u9508/u9508-vendor.mk)

# This device is hdpi.  However the platform doesn't
# currently contain all of the bitmaps at xhdpi density so
# we do this little trick to fall back to the hdpi version
# if the xhdpi doesn't exist.
PRODUCT_AAPT_CONFIG := normal hdpi xhdpi xxhdpi
PRODUCT_AAPT_PREF_CONFIG := xhdpi

DEVICE_PACKAGE_OVERLAYS += device/huawei/u9508/overlay

LOCAL_PATH := device/huawei/u9508
ifeq ($(TARGET_PREBUILT_KERNEL),)
	LOCAL_KERNEL := $(LOCAL_PATH)/kernel
else
	LOCAL_KERNEL := $(TARGET_PREBUILT_KERNEL)
endif

PRODUCT_COPY_FILES += \
    $(LOCAL_KERNEL):kernel

PRODUCT_PACKAGES := \
	lights.k3v2oem1

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.camera.flash-autofocus.xml:system/etc/permissions/android.hardware.camera.flash-autofocus.xml \
    frameworks/native/data/etc/android.hardware.camera.front.xml:system/etc/permissions/android.hardware.camera.front.xml \
    frameworks/native/data/etc/android.hardware.location.gps.xml:system/etc/permissions/android.hardware.location.gps.xml \
    frameworks/native/data/etc/android.hardware.sensor.compass.xml:system/etc/permissions/android.hardware.sensor.compass.xml \
    frameworks/native/data/etc/android.hardware.sensor.gyroscope.xml:system/etc/permissions/android.hardware.sensor.gyroscope.xml \
    frameworks/native/data/etc/android.hardware.sensor.light.xml:system/etc/permissions/android.hardware.sensor.light.xml \
    frameworks/native/data/etc/android.hardware.sensor.proximity.xml:system/etc/permissions/android.hardware.sensor.proximity.xml \
    frameworks/native/data/etc/android.hardware.telephony.gsm.xml:system/etc/permissions/android.hardware.telephony.gsm.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.jazzhand.xml:system/etc/permissions/android.hardware.touchscreen.multitouch.jazzhand.xml \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml \
    frameworks/native/data/etc/android.hardware.usb.host.xml:system/etc/permissions/android.hardware.usb.host.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.software.sip.voip.xml:system/etc/permissions/android.software.sip.voip.xml \
    frameworks/native/data/etc/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml

# Vold and Storage
PRODUCT_COPY_FILES += \
        device/huawei/u9508/prebuilts/etc/vold.fstab:system/etc/vold.fstab

# Live Wallpapers
PRODUCT_PACKAGES += \
        LiveWallpapers \
        LiveWallpapersPicker \
        VisualizationWallpapers \
        librs_jni

# rootdir
PRODUCT_COPY_FILES += \
	device/huawei/u9508/rootdir/init.k3v2oem1.rc:root/init.k3v2oem1.rc \
	device/huawei/u9508/rootdir/init.k3v2oem1.usb.rc:root/init.k3v2oem1.usb.rc \
	device/huawei/u9508/recovery/init.recovery.k3v2oem1.rc:root/init.recovery.k3v2oem1.rc \
	device/huawei/u9508/rootdir/fstab.k3v2oem1:root/fstab.k3v2oem1 \
	device/huawei/u9508/rootdir/ueventd.k3v2oem1.rc:root/ueventd.k3v2oem1.rc 


PRODUCT_PACKAGES += \
    	Torch \
	camera

# config files for wifi, camera, rild, media and GPS
PRODUCT_COPY_FILES += \
	device/huawei/u9508/prebuilts/etc/wifi/wpa_supplicant.conf:system/etc/wifi/wpa_supplicant.conf \
	device/huawei/u9508/prebuilts/etc/camera/davinci/device.config:system/etc/camera/davinci/device.config \
	device/huawei/u9508/prebuilts/etc/camera/davinci/default/imgproc.xml:system/etc/camera/davinci/default/imgproc.xml \
	device/huawei/u9508/prebuilts/etc/camera/davinci/ov8830/imgproc.xml:system/etc/camera/davinci/ov8830/imgproc.xml \
	device/huawei/u9508/prebuilts/etc/k3_omx.cfg:system/etc/k3_omx.cfg \
	device/huawei/u9508/prebuilts/etc/asound_NDLR.dat:system/etc/asound_NDLR.dat \
	device/huawei/u9508/prebuilts/etc/ril_xgold_radio.cfg:system/etc/ril_xgold_radio.cfg \
	device/huawei/u9508/prebuilts/etc/tpa2028.cfg:system/etc/tpa2028.cfg \
	device/huawei/u9508/prebuilts/etc/camera_orientation.cfg:system/etc/camera_orientation.cfg \
	device/huawei/u9508/prebuilts/etc/event-log-tags:system/etc/event-log-tags \
	device/huawei/u9508/prebuilts/etc/media_codecs.xml:system/etc/media_codecs.xml \
	device/huawei/u9508/prebuilts/etc/es305.bin:system/etc/es305.bin \
	device/huawei/u9508/prebuilts/etc/asound_ce_NDLR.dat:system/etc/asound_ce_NDLR.dat \
	device/huawei/u9508/prebuilts/etc/gpsconfig.xml:system/etc/gpsconfig.xml \
	device/huawei/u9508/prebuilts/etc/camera_resolutions.cfg:system/etc/camera_resolutions.cfg \
	device/huawei/u9508/prebuilts/etc/fir_filter/fir_coef_speaker_U9508.txt:system/etc/fir_filter/fir_coef_speaker_U9508.txt \
	device/huawei/u9508/prebuilts/etc/fir_filter/fir_coef_speaker.txt:system/etc/fir_filter/fir_coef_speaker.txt \
	device/huawei/u9508/prebuilts/etc/fir_filter/fir_coef_capture.txt:system/etc/fir_filter/fir_coef_capture.txt \
	device/huawei/u9508/prebuilts/etc/fir_filter/fir_coef_capture_U9508.txt:system/etc/fir_filter/fir_coef_capture_U9508.txt \
	device/huawei/u9508/prebuilts/etc/dbus.conf:system/etc/dbus.conf \
	device/huawei/u9508/prebuilts/etc/es305_uart.bin:/etc/es305_uart.bin 

$(call inherit-product, build/target/product/full.mk)

# call dalvik heap config
$(call inherit-product-if-exists, frameworks/native/build/phone-xhdpi-1024-dalvik-heap.mk)

PRODUCT_BUILD_PROP_OVERRIDES += BUILD_UTC_DATE=0
PRODUCT_NAME := full_u9508
PRODUCT_DEVICE := u9508
