param (
    # height of largest column without top bar
    [Parameter(Mandatory=$true)]
    [string]$DOWNLOAD_LINK,
    
    # name of the output image
    [string]$OUTPUT_PATH
)

# Download the new version (replace 'download_link' with the actual link)
Invoke-WebRequest -Uri $DOWNLOAD_LINK -OutFile $OUTPUT_PATH

# Extract the downloaded file (replace 'new_version.zip' with the actual file name)
# Expand-Archive -Path new_version.zip -DestinationPath .

# Run GUI installer
$software = Get-WmiObject -Class Win32_Product | Where-Object {$_.Name -like '*spLinker*'}
$software.Uninstall()
Start-Process -FilePath $OUTPUT_PATH -Wait
