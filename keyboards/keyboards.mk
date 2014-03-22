idc := \
	hisik3_touchscreeen.idc \
	k3_keypad.idc

keyboard_layout := \
	k3_keypad.kl

#keyboard_chars := \


PRODUCT_COPY_FILES += $(foreach file,$(idc),\
	$(LOCAL_PATH)/$(file):system/usr/idc/$(file))

PRODUCT_COPY_FILES += $(foreach file,$(keyboard_layout),\
	$(LOCAL_PATH)/$(file):system/usr/keylayout/$(file))

#PRODUCT_COPY_FILES += $(foreach file,$(keyboard_chars),\
#	$(LOCAL_PATH)/$(file):system/usr/keychars/$(file))
