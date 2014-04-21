$(call inherit-product, $(SRC_TARGET_DIR)/product/languages_full.mk)

# The gps config appropriate for this device
$(call inherit-product, device/common/gps/gps_us_supl.mk)

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

$(call inherit-product-if-exists, vendor/huawei/hwu9508/hwu9508-vendor.mk)


PRODUCT_AAPT_CONFIG := normal hdpi xhdpi xxhdpi
PRODUCT_AAPT_PREF_CONFIG := xhdpi


LOCAL_PATH := device/huawei/hwu9508
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay


# kernel
ifeq ($(TARGET_PREBUILT_KERNEL),)
	LOCAL_KERNEL := $(LOCAL_PATH)/kernel
else
	LOCAL_KERNEL := $(TARGET_PREBUILT_KERNEL)
endif

PRODUCT_COPY_FILES += \
    $(LOCAL_KERNEL):kernel


######################################################
#
## Hardware section - BEGIN
#
######################################################

# Lights
PRODUCT_PACKAGES += \
	lights.k3v2oem1


# Audio
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/prebuilts/lib/hw/audio.primary.k3v2oem1.so:system/lib/hw/audio.primary.k3v2oem1.so \
        $(LOCAL_PATH)/prebuilts/lib/hw/audio_policy.default.so:system/lib/hw/audio_policy.k3v2oem1.so \
	$(LOCAL_PATH)/prebuilts/etc/fir_filter/fir_coef_speaker_U9508.txt:system/etc/fir_filter/fir_coef_speaker_U9508.txt \
	$(LOCAL_PATH)/prebuilts/etc/fir_filter/fir_coef_speaker.txt:system/etc/fir_filter/fir_coef_speaker.txt \
	$(LOCAL_PATH)/prebuilts/etc/fir_filter/fir_coef_capture.txt:system/etc/fir_filter/fir_coef_capture.txt \
	$(LOCAL_PATH)/prebuilts/etc/fir_filter/fir_coef_capture_U9508.txt:system/etc/fir_filter/fir_coef_capture_U9508.txt \
	$(LOCAL_PATH)/prebuilts/etc/es305_uart.bin:system/etc/es305_uart.bin \
	$(LOCAL_PATH)/prebuilts/etc/es305.bin:system/etc/es305.bin \
	$(LOCAL_PATH)/prebuilts/etc/asound_ce_NDLR.dat:system/etc/asound_ce_NDLR.dat \
	$(LOCAL_PATH)/prebuilts/etc/asound_NDLR.dat:system/etc/asound_NDLR.dat \
	$(LOCAL_PATH)/prebuilts/etc/tpa2028.cfg:system/etc/tpa2028.cfg \
        $(LOCAL_PATH)/prebuilts/etc/audio_policy.conf:system/etc/audio_policy.conf 
    
PRODUCT_PACKAGES += \
    audio.a2dp.default \
    libaudioutils


# Camera
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/device.config:system/etc/camera/davinci/device.config \
	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/mt9m114_sunny/imgproc.xml:system/etc/camera/davinci/mt9m114_sunny/imgproc.xml \
	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/default/imgproc.xml:system/etc/camera/davinci/default/imgproc.xml \
	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/ov8830/imgproc.xml:system/etc/camera/davinci/ov8830/imgproc.xml \
    	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/ov8830/cm_correction.dat:system/etc/camera/davinci/ov8830/cm_correction.dat \
    	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/ov8830/cm_foliage.dat:system/etc/camera/davinci/ov8830/cm_foliage.dat \
    	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/ov8830/cm_normal.dat:system/etc/camera/davinci/ov8830/cm_normal.dat \
    	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/ov8830/cm_sky.dat:system/etc/camera/davinci/ov8830/cm_sky.dat \
    	$(LOCAL_PATH)/prebuilts/etc/camera/davinci/ov8830/cm_sunset.dat:system/etc/camera/davinci/ov8830/cm_sunset.dat \
	$(LOCAL_PATH)/prebuilts/etc/camera_orientation.cfg:system/etc/camera_orientation.cfg \
	$(LOCAL_PATH)/prebuilts/etc/camera_resolutions.cfg:system/etc/camera_resolutions.cfg \
	$(LOCAL_PATH)/libskia_patch/libskia.so:system/lib/libskia.so

PRODUCT_PACKAGES += \
       Torch 


# Bluetooth & FmRadio
PRODUCT_PACKAGES += \
       uim-sysfs \
       libbt-vendor \
       bt_sco_app \
       BluetoothSCOApp \
       libtinyalsa


# config files for wifi, GPS
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/prebuilts/etc/wifi/wpa_supplicant.conf:system/etc/wifi/wpa_supplicant.conf \
	$(LOCAL_PATH)/prebuilts/etc/gpsconfig.xml:system/etc/gpsconfig.xml 


# product specific permissions
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


######################################################
#
## Hardware section - END
#
######################################################


# Vold management
PRODUCT_COPY_FILES += \
        $(LOCAL_PATH)/prebuilts/etc/vold.fstab:system/etc/vold.fstab


# Enable switch storage 
PRODUCT_COPY_FILES += \
       $(LOCAL_PATH)/prebuilts/etc/init.d/preparesd:system/etc/init.d/preparesd


# rootdir
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/rootdir/init.k3v2oem1.rc:root/init.k3v2oem1.rc \
	$(LOCAL_PATH)/rootdir/init:root/init \
	$(LOCAL_PATH)/rootdir/init:recovery/root/init \
	$(LOCAL_PATH)/rootdir/init.rc:root/init.rc \
	$(LOCAL_PATH)/rootdir/init.k3v2oem1.usb.rc:root/init.k3v2oem1.usb.rc \
	$(LOCAL_PATH)/recovery/init.recovery.k3v2oem1.rc:root/init.recovery.k3v2oem1.rc \
	$(LOCAL_PATH)/rootdir/fstab.k3v2oem1:root/fstab.k3v2oem1 \
	$(LOCAL_PATH)/rootdir/ueventd.k3v2oem1.rc:root/ueventd.k3v2oem1.rc \
	$(LOCAL_PATH)/recovery/sbin/postrecoveryboot.sh:recovery/root/sbin/postrecoveryboot.sh


# Sim toolkit
PRODUCT_PACKAGES += \
       Stk


# config files for rild, media, keyboard
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/prebuilts/etc/k3_omx.cfg:system/etc/k3_omx.cfg \
	$(LOCAL_PATH)/prebuilts/etc/ril_xgold_radio.cfg:system/etc/ril_xgold_radio.cfg \
	$(LOCAL_PATH)/prebuilts/etc/event-log-tags:system/etc/event-log-tags \
	$(LOCAL_PATH)/prebuilts/etc/media_codecs.xml:system/etc/media_codecs.xml \
	$(LOCAL_PATH)/prebuilts/etc/media_profiles.xml:system/etc/media_profiles.xml \
	$(LOCAL_PATH)/prebuilts/etc/dbus.conf:system/etc/dbus.conf \
	$(LOCAL_PATH)/prebuilts/usr/idc/hisik3_touchscreen.idc:system/usr/idc/hisik3_touchscreen.idc \
	$(LOCAL_PATH)/prebuilts/usr/idc/k3_keypad.idc:system/usr/idc/k3_keypad.idc \
	$(LOCAL_PATH)/prebuilts/usr/keylayout/k3_keypad.kl:system/usr/keylayout/k3_keypad.kl


# Live Wallpapers
PRODUCT_PACKAGES += \
       LiveWallpapers \
       LiveWallpapersPicker \
       VisualizationWallpapers \
       librs_jni


# Misc
# This device have enough room for precise davick
PRODUCT_TAGS += dalvik.gc.type-precise

PRODUCT_PROPERTY_OVERRIDES += \
       ro.ril.hsxpa=2 \
       wifi.interface=wlan0 \
       ap.interface=wlan1 \
       ro.vold.switchablepair=yes \
       persist.sys.usb.config=mtp,adb \
       ro.opengles.version=131072

PRODUCT_PACKAGES += \
       setup_fs \
       libsrec_jni \
       com.android.future.usb.accessory \
       make_ext4fs 

$(call inherit-product, build/target/product/full.mk)

# call dalvik heap config
$(call inherit-product-if-exists, frameworks/native/build/phone-xhdpi-1024-dalvik-heap.mk)

PRODUCT_BUILD_PROP_OVERRIDES += BUILD_UTC_DATE=0
PRODUCT_NAME := cm_hwu9508
PRODUCT_DEVICE := hwu9508
