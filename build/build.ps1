<#
    冬瓜音乐 · 二创开源版 —— 构建脚本
    SPDX-License-Identifier: GPL-3.0-or-later

    用法：
        .\build.ps1 -DecodedDir <apktool解包目录> -OutDir <输出目录>

    所有路径都可通过参数覆盖，默认值按常见目录约定给出。
#>
param(
    [string]$DecodedDir = "$PSScriptRoot\..\decoded",
    [string]$OutDir     = "$PSScriptRoot\..\out",
    [string]$OutName    = "donggua-music-gpl",
    [string]$ToolsDir   = "$PSScriptRoot\..\tools",
    [string]$WorkDir    = "$PSScriptRoot\..\work",
    [string]$Keystore   = "",
    [switch]$V1Signing
)

# 原生命令的 stderr（JVM 的 "Picked up JAVA_TOOL_OPTIONS" 等）不应中断流程，
# 真实错误一律通过 $LASTEXITCODE 判定。
$ErrorActionPreference = "Continue"

$APKTOOL   = Join-Path $ToolsDir "apktool-3.0.3.jar"
$BuildTools = Join-Path $ToolsDir "android-sdk\build-tools\34.0.0"
$Zipalign  = Join-Path $BuildTools "zipalign.exe"
$Apksigner = Join-Path $BuildTools "apksigner.bat"
$D8Jar     = Join-Path $BuildTools "lib\d8.jar"

if (-not $Keystore) { $Keystore = Join-Path $ToolsDir "debug.keystore" }

if (-not (Test-Path -LiteralPath $DecodedDir)) { throw "解包目录不存在: $DecodedDir" }
if (-not (Test-Path -LiteralPath $APKTOOL))     { throw "找不到 apktool: $APKTOOL" }
if (-not (Test-Path -LiteralPath $Keystore))   { throw "找不到签名密钥: $Keystore" }

New-Item -ItemType Directory -Force -Path $OutDir  | Out-Null
New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null

$stamp     = Get-Date -Format "yyyyMMdd-HHmmss"
$unsigned  = Join-Path $WorkDir "$OutName-unsigned.apk"
$aligned   = Join-Path $WorkDir "$OutName-aligned.apk"
$signed    = Join-Path $OutDir  "$OutName`_$stamp.apk"

Write-Host "[1/4] apktool build (smali -> dex, aapt2 resources)" -ForegroundColor Cyan
& java -jar $APKTOOL b $DecodedDir -o $unsigned
if ($LASTEXITCODE -ne 0) { throw "apktool build failed" }

Write-Host "[2/4] zipalign" -ForegroundColor Cyan
& $Zipalign -f -p 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "[3/4] apksigner (v2 + v3)" -ForegroundColor Cyan
& $Apksigner sign --ks $Keystore --ks-pass pass:android --key-pass pass:android `
    --ks-key-alias androiddebugkey `
    --v1-signing-enabled $(if ($V1Signing) { "true" } else { "false" }) `
    --v2-signing-enabled true --v3-signing-enabled true `
    --out $signed $aligned
if ($LASTEXITCODE -ne 0) { throw "signing failed" }

Write-Host "[4/4] verify" -ForegroundColor Cyan
& $Apksigner verify $signed
if ($LASTEXITCODE -ne 0) { throw "verification failed" }

Remove-Item $unsigned, $aligned -Force -ErrorAction SilentlyContinue

$size = [math]::Round((Get-Item $signed).Length / 1MB, 2)
Write-Host "OK -> $signed  ($size MB)" -ForegroundColor Green