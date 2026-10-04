# Builds a signed release APK and publishes it as a GitHub Release (for Obtainium).
# Usage:  .\release.ps1            (commit your changes first)
#         .\release.ps1 -Notes "Fixed the handoff screen"
# Version: versionCode = git commit count, versionName/tag = 1.0.<count>, so every new commit bumps it.
param([string]$Notes = "")
# Not "Stop": git/gradle write normal progress to stderr, which Windows PowerShell would treat as fatal.
# Failures are caught via $LASTEXITCODE checks instead.
$ErrorActionPreference = "Continue"
Set-Location $PSScriptRoot

if (git status --porcelain) { throw "Uncommitted changes. Commit them first so the release matches the repo." }
$count = [int](git rev-list --count HEAD)
$version = "1.0.$count"
$tag = "v$version"
if (git tag --list $tag) { throw "$tag already released. Make a new commit to bump the version." }
if (-not (Test-Path "app\google-services.json")) { throw "app\google-services.json missing (git-ignored; download it from the Firebase console)." }

if (-not $env:JAVA_HOME) { $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr" }
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }

.\gradlew.bat assembleRelease "-PappVersionCode=$count" "-PappVersionName=$version"
if ($LASTEXITCODE) { throw "Gradle build failed" }

$built = "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $built)) { throw "No signed APK at $built (is RELEASE_STORE_FILE set in ~/.gradle/gradle.properties?)" }
$apk = "app\build\outputs\apk\release\missions-$tag.apk"
Copy-Item $built $apk -Force

git push origin HEAD
if ($LASTEXITCODE) { throw "git push failed" }
if (-not $Notes) { $Notes = "Release $version" }
gh release create $tag $apk --title $tag --notes $Notes --target (git rev-parse HEAD)
if ($LASTEXITCODE) { throw "gh release create failed" }
git fetch --tags --quiet
Write-Host "Published $tag"
