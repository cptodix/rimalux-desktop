# Rimalux Desktop installer for Windows.
#
#   irm https://raw.githubusercontent.com/cptodix/rimalux-desktop/main/scripts/install.ps1 | iex
#   iex "& { $(irm https://raw.githubusercontent.com/cptodix/rimalux-desktop/main/scripts/install.ps1) } -Uninstall"
#
# Optional:
#   $env:RIMALUX_VERSION = "0.1.2"
param(
    [string]$Version = $(if ($env:RIMALUX_VERSION) { $env:RIMALUX_VERSION } else { "latest" }),
    [switch]$Uninstall
)

$ErrorActionPreference = "Stop"
$repo = if ($env:RIMALUX_REPOSITORY) { $env:RIMALUX_REPOSITORY } else { "cptodix/rimalux-desktop" }

if ($Uninstall) {
    $uninstallRoots = @(
        "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*",
        "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*",
        "HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*"
    )
    $entry = Get-ItemProperty -Path $uninstallRoots -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName -like "Rimalux*" } |
        Select-Object -First 1

    if (-not $entry) {
        Write-Host "Rimalux is not installed."
        return
    }

    $command = if ($entry.QuietUninstallString) {
        $entry.QuietUninstallString
    } else {
        "$($entry.UninstallString) /quiet"
    }

    if (Get-Process -Name "Rimalux" -ErrorAction SilentlyContinue) {
        Write-Host "note: Rimalux is running; close it to finish uninstalling."
    }

    Write-Host "Uninstalling $($entry.DisplayName)..."
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$command`"" -Wait
    Write-Host "Rimalux uninstalled."
    Write-Host "Settings and cached wallpapers were kept in %APPDATA%\Rimalux and %LOCALAPPDATA%\Rimalux."
    return
}

$headers = @{
    "Accept"     = "application/vnd.github+json"
    "User-Agent" = "Rimalux-Installer"
}

$api = if ($Version -eq "latest") {
    "https://api.github.com/repos/$repo/releases/latest"
} else {
    "https://api.github.com/repos/$repo/releases/tags/v$($Version.TrimStart('v'))"
}

Write-Host "Resolving Rimalux release ($Version)..."
$release = Invoke-RestMethod -Uri $api -Headers $headers

$asset = $release.assets |
    Where-Object { $_.name -like "*.exe" } |
    Select-Object -First 1
if (-not $asset) {
    throw "Release $($release.tag_name) has no Windows installer."
}

$installer = Join-Path $env:TEMP $asset.name
Write-Host "Downloading $($release.tag_name)..."
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $installer -Headers $headers

Write-Host "Installing Rimalux $($release.tag_name)..."
$process = Start-Process -FilePath $installer -ArgumentList "/quiet" -Wait -PassThru
if ($process.ExitCode -ne 0) {
    Write-Host "Silent install returned $($process.ExitCode); opening the installer window."
    Start-Process -FilePath $installer -Wait
}

Remove-Item -Force $installer -ErrorAction SilentlyContinue
Write-Host "Rimalux $($release.tag_name) installed."
