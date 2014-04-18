#!/bin/bash

export LOCAL_FOLDER=device/huawei/hwu9508;
export RECOVERY_FOLDER=bootable/recovery;
export MANIFESTS_FOLDER=.repo/local_manifests;

PS3='Choose what you want to build '
options=("CM" "TWRP" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "CM")
            echo "you chose choice 1"
            cat $MANIFESTS_FOLDER/roomservice.xml.CM > $MANIFESTS_FOLDER/roomservice.xml
            rm -rf $RECOVERY_FOLDER
            repo sync
            ;;
        "TWRP")
            echo "you chose choice 2"
            cat $MANIFESTS_FOLDER/roomservice.xml.TWRP > $MANIFESTS_FOLDER/roomservice.xml
            rm -rf $RECOVERY_FOLDER
            repo sync
            cp $LOCAL_FOLDER/recovery/overlay/data.cpp $RECOVERY_FOLDER
	       cp $LOCAL_FOLDER/recovery/overlay/partition.cpp $RECOVERY_FOLDER
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option";;
    esac
done

