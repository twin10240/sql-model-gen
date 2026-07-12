<#
.SYNOPSIS
    Install the ModelConvertor CLI and Claude Code skill globally on Windows.

.DESCRIPTION
    Downloads the fat JAR (with bundled ojdbc8) from a GitHub Release, places the
    CLI on the user PATH, and copies the skill to the user global skills folder.
    Run this from inside a clone of the repository; the lightweight text files
    (modelconvertor.cmd, SKILL.md) come from the clone, the heavy JAR from the Release.

.PARAMETER Repo
    GitHub owner/repo that hosts the Release asset.

.PARAMETER Version
    Release tag to install, or 'latest' (default).

.PARAMETER InstallDir
    CLI install folder (added to PATH).

.PARAMETER Force
    Overwrite an existing install / global skill (use when updating).

.EXAMPLE
    .\install.ps1
    .\install.ps1 -Version v1.0.0 -Force
#>
[CmdletBinding()]
param(
    [string]$Repo = 'twin10240/sql-model-gen',
    [string]$Version = 'latest',
    [string]$InstallDir = 'C:\tools\modelconvertor',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot

# --- source text files from the clone -----------------------------------------
$cmdSource   = Join-Path $repoRoot 'modelconvertor.cmd'
$skillSource = Join-Path $repoRoot '.claude\skills\modelconvertor\SKILL.md'
foreach ($f in @($cmdSource, $skillSource)) {
    if (-not (Test-Path -LiteralPath $f)) {
        throw "Missing source file (run install.ps1 from inside the repo clone): $f"
    }
}

# --- preflight: Java runtime --------------------------------------------------
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Warning 'Java runtime not found on PATH. Install Java 8+ to run modelconvertor.'
}

# --- 1. install dir + CLI files -----------------------------------------------
$jarTarget = Join-Path $InstallDir 'modelconvertor.jar'
$cmdTarget = Join-Path $InstallDir 'modelconvertor.cmd'
if (-not $Force) {
    foreach ($t in @($jarTarget, $cmdTarget)) {
        if (Test-Path -LiteralPath $t) {
            throw "Install target already exists: $t (use -Force to update)"
        }
    }
}
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null

# --- download JAR from the Release --------------------------------------------
if ($Version -eq 'latest') {
    $jarUrl = "https://github.com/$Repo/releases/latest/download/modelconvertor.jar"
} else {
    $jarUrl = "https://github.com/$Repo/releases/download/$Version/modelconvertor.jar"
}
Write-Host "Downloading $jarUrl"
try {
    Invoke-WebRequest -Uri $jarUrl -OutFile $jarTarget -UseBasicParsing
} catch {
    throw ("Download failed: {0}. For a private repo, run: " -f $_.Exception.Message) +
          "gh release download $Version --repo $Repo --pattern modelconvertor.jar --dir '$InstallDir'"
}
if (-not (Test-Path -LiteralPath $jarTarget) -or (Get-Item -LiteralPath $jarTarget).Length -eq 0) {
    throw "Downloaded JAR is missing or empty: $jarTarget"
}
Copy-Item -LiteralPath $cmdSource -Destination $cmdTarget -Force

# --- 2. user PATH (idempotent) ------------------------------------------------
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$entries  = @($userPath -split ';' | Where-Object { $_ })
if ($entries -notcontains $InstallDir) {
    [Environment]::SetEnvironmentVariable('Path', (($entries + $InstallDir) -join ';'), 'User')
    Write-Host "Added to user PATH: $InstallDir (open a new terminal for it to take effect)"
}

# --- 3. global Claude Code skill ----------------------------------------------
$skillDir    = Join-Path $env:USERPROFILE '.claude\skills\modelconvertor'
$skillTarget = Join-Path $skillDir 'SKILL.md'
if ((Test-Path -LiteralPath $skillTarget) -and -not $Force) {
    throw "Global skill already exists: $skillTarget (use -Force to update)"
}
New-Item -ItemType Directory -Force -Path $skillDir | Out-Null
Copy-Item -LiteralPath $skillSource -Destination $skillTarget -Force

# --- 4. Oracle config check (do not create; it holds a plaintext password) -----
$oracleProps = Join-Path $env:USERPROFILE '.modelconvertor\oracle.properties'
if (-not (Test-Path -LiteralPath $oracleProps)) {
    Write-Warning "Oracle config not found: $oracleProps"
    Write-Warning 'Create it with oracle.url / oracle.username / oracle.password / oracle.schema before generating models.'
}

Write-Host ''
Write-Host 'ModelConvertor installed. Restart Claude Code so the global skill is picked up.'
Write-Host "Verify with: modelconvertor.cmd --help"
