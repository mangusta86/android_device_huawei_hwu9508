## Specify phone tech before including full_phone
$(call inherit-product, vendor/cm/config/gsm.mk)

# bootanimation target
TARGET_SCREEN_HEIGHT := 1280
TARGET_SCREEN_WIDTH := 720

# Release name
PRODUCT_RELEASE_NAME := U9508

# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit device configuration
$(call inherit-product, device/huawei/hwu9508/device_hwu9508.mk)


## Device identifier. This must come after all inclusions
PRODUCT_DEVICE := hwu9508
PRODUCT_NAME := cm_hwu9508
PRODUCT_BRAND := Huawei
PRODUCT_MODEL := U9508
PRODUCT_MANUFACTURER := HUAWEI

#PRODUCT_BUILD_PROP_OVERRIDES += PRODUCT_NAME=U9508 BUILD_FINGERPRINT="Huawei/U9508/hwu9508:4.2.2/HuaweiU9508/B708:user/ota-rel-keys,release-keys" PRIVATE_BUILD_DESC="U9508-user 4.2.2 HuaweiU9508 B708 ota-rel-keys,release-keys"


