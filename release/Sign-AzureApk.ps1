param(
    [Parameter(Mandatory)][string]$UnsignedApk,
    [Parameter(Mandatory)][string]$OutputApk,
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z][A-Za-z0-9-]{1,22}[A-Za-z0-9]$')][string]$VaultName,
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z0-9-]+$')][string]$CertificateName,
    [Parameter(Mandatory)][string]$BuildTools,
    [Parameter(Mandatory)][string]$JsignJar
)
$ErrorActionPreference = 'Stop'
$pinPath = Join-Path $PSScriptRoot 'app-signing-sha256.txt'
if (!(Test-Path -LiteralPath $pinPath)) { throw 'Enroll the production certificate fingerprint first.' }
$pin = (Get-Content -LiteralPath $pinPath -Raw).Trim().Replace(':','').ToLowerInvariant()
if ($pin -notmatch '^[a-f0-9]{64}$' -or $pin -eq '55f7e012f91314aad8179f797a7aa8de26509f77fe51f1a1dc1f169aa1c681e4') {
    throw 'Invalid production certificate fingerprint'
}
if (Test-Path -LiteralPath $OutputApk) { throw 'Choose a new output filename.' }
if ((Get-FileHash -LiteralPath $JsignJar).Hash -ne '6cf58aa080e7ff82656f320cff8d4a3bff30408df8766806dcba3740dd839fa5') {
    throw 'Expected the official Jsign 7.6 all-in-one JAR.'
}
$accountType = az account show --query user.type -o tsv --only-show-errors
if ($LASTEXITCODE -ne 0 -or $accountType -ne 'user') { throw 'Sign in to Azure CLI with the authorized personal user identity.' }
$public = az keyvault certificate show --vault-name $VaultName --name $CertificateName --only-show-errors -o json
if ($LASTEXITCODE -ne 0) { throw 'Cannot read the enrolled public certificate.' }
$cert = $public | ConvertFrom-Json
if ($cert.policy.keyProperties.exportable -ne $false -or $cert.attributes.enabled -ne $true) {
    throw 'Expected an enabled certificate with a non-exportable key.'
}
$sha = [Security.Cryptography.SHA256]::Create()
try { $actual = [BitConverter]::ToString($sha.ComputeHash([Convert]::FromBase64String($cert.cer))).Replace('-','') } finally { $sha.Dispose() }
if ($actual -ne $pin) { throw 'Azure returned a different certificate from the pinned release identity.' }
$inputPath = (Resolve-Path -LiteralPath $UnsignedApk).Path
$outputPath = [IO.Path]::GetFullPath($OutputApk)
$manifest = & (Join-Path $BuildTools 'aapt2.exe') dump badging $inputPath
if ($LASTEXITCODE -ne 0 -or !($manifest -match "^package: name='com\.usbscreensaver' ") -or $manifest -match '^application-debuggable') {
    throw 'Input must be the release APK for com.usbscreensaver.'
}
& (Join-Path $BuildTools 'zipalign.exe') -c 4 $inputPath
if ($LASTEXITCODE -ne 0) { throw 'Input APK must already be zip-aligned.' }
$staging = $outputPath + '.' + [Guid]::NewGuid().ToString('N') + '.tmp'
$tokenVariable = 'SLIDESHOW_AZURE_TOKEN_' + [Guid]::NewGuid().ToString('N')
try {
    $token = az account get-access-token --resource https://vault.azure.net --query accessToken -o tsv --only-show-errors
    if ($LASTEXITCODE -ne 0 -or !$token) { throw 'Cannot obtain a short-lived Key Vault access token.' }
    [Environment]::SetEnvironmentVariable($tokenVariable, $token, 'Process')
    $token = $null
    $classpath = (Join-Path $BuildTools 'lib/apksigner.jar') + [IO.Path]::PathSeparator + (Resolve-Path -LiteralPath $JsignJar).Path
    & java -cp $classpath com.android.apksigner.ApkSignerTool sign --provider-class net.jsign.jca.JsignJcaProvider --provider-arg $VaultName --ks NONE --ks-type AZUREKEYVAULT --ks-key-alias $CertificateName --ks-pass "env:$tokenVariable" --key-pass "env:$tokenVariable" --v4-signing-enabled false --in $inputPath --out $staging
    if ($LASTEXITCODE -ne 0) { throw 'Azure signing failed.' }
    & (Join-Path $PSScriptRoot 'Verify-Apk.ps1') -Apk $staging -BuildTools $BuildTools | Out-Null
    [IO.File]::Move($staging, $outputPath)
    Get-FileHash -LiteralPath $outputPath -Algorithm SHA256
} finally {
    [Environment]::SetEnvironmentVariable($tokenVariable, $null, 'Process')
    if (Test-Path -LiteralPath $staging) { Remove-Item -LiteralPath $staging }
}
