#!/bin/sh

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

VENDOR=huawei
DEVICE=hwu9508
DEVICEBASE=../../../vendor/$VENDOR/$DEVICE
DEVICEMAKEFILE=$DEVICEBASE/$DEVICE-vendor-blobs.mk
COMMONPROPS=proprietary-files.txt
EUIFOLDER=/home/mangusta86/Huawei_DEV/B509-p6


echo "Copying device specific files from EUI..."
for FILE in `cat $COMMONPROPS | grep -v ^# | grep -v ^$`; do
    DIR=`dirname $FILE`
    if [ ! -d $DEVICEBASE/proprietary/$DIR ]; then
        mkdir -p $DEVICEBASE/proprietary/$DIR
    fi
    cp $EUIFOLDER/$FILE $DEVICEBASE/proprietary/$FILE
done


(cat << EOF) > $DEVICEMAKEFILE
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

# This file is script-generated 

LOCAL_PATH := vendor/huawei/hwu9508

PRODUCT_COPY_FILES += \\
EOF

LINEEND=" \\"
COUNT=`cat $COMMONPROPS | grep -v ^# | grep -v ^$ | wc -l | awk {'print $1'}`
for FILE in `cat $COMMONPROPS | grep -v ^# | grep -v ^$`; do
    COUNT=`expr $COUNT - 1`
    if [ $COUNT = "0" ]; then
        LINEEND=""
    fi
    echo "    "\$"(LOCAL_PATH)/$DEVICEBASE/proprietary/$FILE:$FILE$LINEEND" >> $DEVICEMAKEFILE
done

(cat << EOF) > $DEVICEBASE/$DEVICE-vendor.mk
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

# This file is script-generated 


# Pick up overlay for features that depend on non-open-source files
DEVICE_PACKAGE_OVERLAYS := vendor/$VENDOR/$DEVICE/overlay

\$(call inherit-product, vendor/$VENDOR/$DEVICE/$DEVICE-vendor-blobs.mk)
EOF

(cat << EOF) > $DEVICEBASE/BoardConfigVendor.mk
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

# This file is script-generated 


EOF
