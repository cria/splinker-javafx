param (
    # Link para download do spLinker
    [Parameter(Mandatory=$true)]
    [string]$DOWNLOAD_LINK,
    
    # Caminho onde o instalador será salvo
    [string]$OUTPUT_PATH
)

# Faz Download da nova versão
Invoke-WebRequest -Uri $DOWNLOAD_LINK -OutFile $OUTPUT_PATH

# Extrai o zip do download
# Expand-Archive -Path splinker_new_version.zip -DestinationPath .

# Remove a versão antiga do spLinker
$software = Get-WmiObject -Class Win32_Product | Where-Object {$_.Name -like "*spLinker*"} | Select-Object -Property Name, IdentifyingNumber
msiexec.exe /x $software.IdentifyingNumber /quiet /qn /norestart

# Run GUI installer
Start-Process -FilePath $OUTPUT_PATH -Wait
