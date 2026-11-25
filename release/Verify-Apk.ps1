param(
    [Parameter(Mandatory)][string]$Apk,
    [Parameter(Mandatory)][string]$BuildTools
)

$ErrorActionPreference = "Stop"
$certFile = Join-Path $PSScriptRoot "app-signing-sha256.txt"
if (!(Test-Path -LiteralPath $certFile)) {
    throw "Enroll online signing and record the production app-signing certificate fingerprint first."
}
$expected = (Get-Content -LiteralPath $certFile -Raw).Trim().Replace(":", "").ToLowerInvariant()
$testKey = "55f7e012f91314aad8179f797a7aa8de26509f77fe51f1a1dc1f169aa1c681e4"
if ($expected -notmatch "^[a-f0-9]{64}$" -or $expected -eq $testKey) {
    throw "The production certificate fingerprint is invalid or is the historical test certificate."
}

$path = (Resolve-Path -LiteralPath $Apk).Path
$details = & (Join-Path $BuildTools "apksigner.bat") verify --verbose --print-certs $path
if ($LASTEXITCODE -ne 0) { throw "Android signature verification failed." }
$signers = @($details | Select-String "^Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})$")
if ($signers.Count -ne 1 -or $signers[0].Matches[0].Groups[1].Value -ne $expected) {
    throw "The APK does not match the enrolled production signer."
}
$manifest = & (Join-Path $BuildTools "aapt2.exe") dump badging $path
if ($LASTEXITCODE -ne 0 -or !($manifest -match "^package: name='com\.usbscreensaver' ")) {
    throw "Unexpected APK application ID."
}
if ($manifest -match "^application-debuggable") { throw "APK is debuggable." }
Get-FileHash -LiteralPath $path -Algorithm SHA256
