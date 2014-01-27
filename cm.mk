#
# Copyright (C) 2012 The CyanogenMod Project
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


# Specify phone tech before including full_phone
$(call inherit-product, vendor/cm/config/gsm.mk)

# Release name
PRODUCT_RELEASE_NAME := hwu9508

# Boot animation
TARGET_SCREEN_HEIGHT := 1280
TARGET_SCREEN_WIDTH := 720

# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit device configuration
$(call inherit-product, device/huawei/hwu9508/full_hwu9508.mk)

# Device identifier. This must come after all inclusions
PRODUCT_DEVICE := hwu9508
PRODUCT_NAME := cm_hwu9508
PRODUCT_BRAND := huawei
PRODUCT_MODEL := U9508
PRODUCT_MANUFACTURER := huawei

# Set build fingerprint / ID / Product Name ect.
PRODUCT_BUILD_PROP_OVERRIDES += PRODUCT_NAME=hwu9508 
PRODUCT_BUILD_PROP_OVERRIDES += BUILD_UTC_DATE=0

#from GS3 
#PRODUCT_BUILD_PROP_OVERRIDES += PRODUCT_NAME=m0xx TARGET_DEVICE=m0 BUILD_FINGERPRINT="samsung/m0xx/m0:4.1.1/JRO03C/I9300XXDLIB:user/release-keys" PRIVATE_BUILD_DESC="m0xx-user 4.1.1 JRO03C I9300XXDLIB release-keys"


#BUILD_FINGERPRINT=Huawei/U9508/hwu9508:4.1.1/HuaweiU9508/C00B023:user/release-keys PRIVATE_BUILD_DESC="u9508-userdebug 4.1.1 JRO03L userdebug.s00219286.20120919.191922 test-keys"

