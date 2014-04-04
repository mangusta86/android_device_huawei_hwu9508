#!/bin/bash

export LOCAL_FOLDER=device/huawei/u9508;

export RECOVERY_FOLDER=bootable/recovery;

# overlays for building TWRP
if [ -d $RECOVERY_FOLDER/dedupe ]; then
	echo "building full CM-10.1";
	else
	echo "building TWRP";
	cp $LOCAL_FOLDER/recovery/overlay/data.cpp $RECOVERY_FOLDER
	cp $LOCAL_FOLDER/recovery/overlay/partition.cpp $RECOVERY_FOLDER
fi;

