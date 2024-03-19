#!/bin/bash
export DOWNLOAD_LINK='https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb'
# Uninstall the existing software (replace 'software_name' with the actual name)
sudo apt remove -y spLinker

# Download the new version (replace 'download_link' with the actual link)
wget -O splinker_new_version.tar.gz $DOWNLOAD_LINK

# Extract the downloaded file (replace 'new_version.tar.gz' with the actual file name)
tar -xvf splinker_new_version.tar.gz

# Navigate to the extracted directory (replace 'new_version' with the actual directory name)
cd splinker_new_version

# Run GUI installer (replace 'installer.sh' with the actual installer script)
dpkg-deb -x spLinker.db $HOME
