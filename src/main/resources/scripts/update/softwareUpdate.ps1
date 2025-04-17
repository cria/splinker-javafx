param (
    # Link para download do spLinker
    [Parameter(Mandatory=$true)]
    [string]$DOWNLOAD_LINK,
    
    # Caminho onde o instalador será salvo
    [string]$OUTPUT_PATH
)

# Faz Download da nova versão
Invoke-WebRequest -Uri $DOWNLOAD_LINK -OutFile "$HOME\Downloads\$OUTPUT_PATH"

# Remove a versão antiga do spLinker
$spLinker = Get-WmiObject -Class Win32_Product | Where-Object { $_.Name -like "*spLinker*" }
# msiexec.exe /x $spLinker.IdentifyingNumber /quiet /qn /norestart
$uninstallArgs = "/x $($spLinker.IdentifyingNumber) /qn"
Start-Process -FilePath "msiexec.exe" -ArgumentList $uninstallArgs -NoNewWindow -Wait
# Run GUI installer
Start-Process -FilePath "$HOME\Downloads\$OUTPUT_PATH" -Wait
