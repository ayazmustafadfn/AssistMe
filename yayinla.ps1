# AssistMe yeni sürüm yayınlama
#
# Kullanım:  .\yayinla.ps1 -Not "Neler değişti (kullanıcının göreceği metin)"
#
# 1) app/build.gradle.kts içinde versionCode'u 1 artırın, versionName'i güncelleyin.
# 2) Bu betik APK'yı derler, AssistMe-<versionCode>.apk adıyla GitHub Release
#    olarak yayınlar. Telefonlardaki uygulama açılışta yeni sürümü görür.
#
# APK bu makinedeki debug anahtarıyla imzalanır; başka makinede derlenen APK
# mevcut kurulumun üzerine KURULMAZ (imza farklı olur).

param(
    [Parameter(Mandatory = $true)][string]$Not
)

$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME = 'D:\Android\Sdk'
Set-Location $PSScriptRoot

$gradle = Get-Content 'app\build.gradle.kts' -Raw
$code = [regex]::Match($gradle, 'versionCode\s*=\s*(\d+)').Groups[1].Value
$name = [regex]::Match($gradle, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $code -or -not $name) { throw 'versionCode / versionName okunamadı' }

$tag = "v$name"
# PS 5.1: native komutun stderr'i 'Stop' altında hata sayılır; bu denetimde yumuşat.
$ErrorActionPreference = 'Continue'
gh release view $tag *> $null
$varMi = ($LASTEXITCODE -eq 0)
# Bundan sonra native komutların stderr uyarıları betiği durdurmasın; hatalar çıkış koduyla yakalanır.
if ($varMi) { throw "$tag zaten yayınlanmış. Önce versionCode/versionName'i artırın." }

Write-Host "Derleniyor: $name ($code)..."
.\gradlew.bat assembleDebug --quiet
if ($LASTEXITCODE -ne 0) { throw 'Derleme başarısız' }

$apk = "app\build\outputs\apk\debug\AssistMe-$code.apk"
Copy-Item 'app\build\outputs\apk\debug\app-debug.apk' $apk -Force

Write-Host "Yayınlanıyor: $tag..."
gh release create $tag $apk --title "AssistMe $name" --notes $Not
if ($LASTEXITCODE -ne 0) { throw 'GitHub release oluşturulamadı' }

Write-Host "Tamam. Telefonlar bir sonraki açılışta $name sürümünü görecek."
