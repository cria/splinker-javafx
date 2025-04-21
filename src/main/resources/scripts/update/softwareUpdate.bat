@echo off

set "DOWNLOAD_LINK=%~1"
set "OUTPUT_PATH=%UserProfile%/Downloads/%~2"


echo %DOWNLOAD_LINK%
echo %OUTPUT_PATH%
REM Download the new version
powershell -Command "(New-Object Net.WebClient).DownloadFile('%DOWNLOAD_LINK%', '%OUTPUT_PATH%')"

REM Uninstall the existing software
wmic product where "name='splinker'" call uninstall /nointeractive

REM Run GUI installer
start %OUTPUT_PATH%
