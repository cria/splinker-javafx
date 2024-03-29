#!/bin/bash
export DOWNLOAD_LINK=$1
export OUTPUT_PATH=$2
# Uninstall the existing software (replace 'software_name' with the actual name)
sudo apt remove -y spLinker

# Download the new version (replace 'download_link' with the actual link)
wget -O $OUTPUT_PATH $DOWNLOAD_LINK

alias splinker_pkg_manager=$3
# Run GUI installer (replace 'installer.sh' with the actual installer script)
$splinker_pkg_manager $4 $HOME
#dpkg-deb -x spLinker.db $HOME
