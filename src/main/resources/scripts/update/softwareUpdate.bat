@echo off

set DOWNLOAD_LINK=https://swupdate.openvpn.net/downloads/connect/openvpn-connect-3.3.7.2979_signed.msi
set OUTPUT_PATH=%userprofile%/Downloads/splinker_new_version.msi

REM Download the new version (replace 'download_link' with the actual link)
powershell -Command "(New-Object Net.WebClient).DownloadFile('%DOWNLOAD_LINK%', '%OUTPUT_PATH%')

REM Extract the downloaded file (replace 'new_version.zip' with the actual file name)
::powershell -Command "Expand-Archive -Path splinker_new_version.zip -DestinationPath ."

REM Uninstall the existing software
start /wait wmic product where "name='spLinker'" call uninstall
REM Run GUI installer (replace 'installer.bat' with the actual installer script)
start %OUTPUT_PATH%
