
$(call inherit-product, $(SRC_TARGET_DIR)/product/languages_full.mk)

# The gps config appropriate for this device
$(call inherit-product, device/common/gps/gps_us_supl.mk)

$(call inherit-product-if-exists, vendor/huawei/u9508/u9508-vendor.mk)

# This device is hdpi.  However the platform doesn't
# currently contain all of the bitmaps at xhdpi density so
# we do this little trick to fall back to the hdpi version
# if the xhdpi doesn't exist.
PRODUCT_AAPT_CONFIG := normal hdpi 
PRODUCT_AAPT_PREF_CONFIG := hdpi

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



$(call inherit-product, build/target/product/full.mk)

PRODUCT_BUILD_PROP_OVERRIDES += BUILD_UTC_DATE=0
PRODUCT_NAME := full_u9508
PRODUCT_DEVICE := u9508
