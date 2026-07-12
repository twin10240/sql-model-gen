<#
.SYNOPSIS
    Install the ModelConvertor CLI and Claude Code skill globally on Windows.

.DESCRIPTION
    Downloads the fat JAR (with bundled ojdbc8) from a GitHub Release, places the
    CLI on the user PATH, and copies the skill to the user global skills folder.
    Run this from inside a clone of the repository; the lightweight text files
    (modelconvertor.cmd, SKILL.md) come from the clone, the heavy JAR from the Release.

    Preconditions (source files, Java, install/skill conflicts) are checked before
    any change. The JAR is downloaded and structurally validated in a temporary file
    before it replaces the install target, so a failed download or validation never
    touches an existing install. The downloaded JAR is NOT executed during install.

    Note: if a later step (CMD copy, PATH, skill copy) fails, an update may be left
    partially applied. Back up an existing install before updating with -Force.

.PARAMETER Repo
    GitHub owner/repo that hosts the Release asset.

.PARAMETER Version
    Release tag to install (e.g. v1.0.0), or 'latest' (default). Pin a tag for
    reproducible installs.

.PARAMETER InstallDir
    CLI install folder (added to PATH).

.PARAMETER UseGh
    Download the asset with an authenticated 'gh release download' instead of an
    anonymous HTTP request. Required for private repositories (run 'gh auth login' first).

.PARAMETER Force
    Overwrite an existing install / global skill (use when updating).

.EXAMPLE
    .\install.ps1
    .\install.ps1 -Version v1.0.0
    .\install.ps1 -UseGh -Force
#>
[CmdletBinding()]
param(
    [string]$Repo = 'twin10240/sql-model-gen',
    [string]$Version = 'latest',
    [string]$InstallDir = 'C:\tools\modelconvertor',
    [switch]$UseGh,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot

# === Phase 1: preconditions (no changes made yet) =============================

# Source text files from the clone.
$cmdSource   = Join-Path $repoRoot 'modelconvertor.cmd'
$skillSource = Join-Path $repoRoot '.claude\skills\modelconvertor\SKILL.md'
foreach ($f in @($cmdSource, $skillSource)) {
    if (-not (Test-Path -LiteralPath $f)) {
        throw "Missing source file (run install.ps1 from inside the repo clone): $f"
    }
}

# Java is required to run modelconvertor.
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java runtime not found on PATH. Install Java 8+ and re-run install.ps1.'
}
if ($UseGh -and -not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'gh CLI not found but -UseGh was requested. Install GitHub CLI and run gh auth login.'
}

# Resolve targets and check every conflict up front, before any mutation.
$jarTarget   = Join-Path $InstallDir 'modelconvertor.jar'
$cmdTarget   = Join-Path $InstallDir 'modelconvertor.cmd'
$skillDir    = Join-Path $env:USERPROFILE '.claude\skills\modelconvertor'
$skillTarget = Join-Path $skillDir 'SKILL.md'
if (-not $Force) {
    $conflicts = @($jarTarget, $cmdTarget, $skillTarget) | Where-Object { Test-Path -LiteralPath $_ }
    if ($conflicts) {
        throw "Already installed (use -Force to update): $($conflicts -join ', ')"
    }
}

# === Phase 2: download to a temp file and validate (without executing it) =====

$tempJar = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), "modelconvertor-$([guid]::NewGuid()).jar")
try {
    Write-Host "Downloading modelconvertor.jar ($Version) from $Repo"
    if ($UseGh) {
        $ghArgs = @('release', 'download')
        if ($Version -ne 'latest') { $ghArgs += $Version }
        $ghArgs += @('--repo', $Repo, '--pattern', 'modelconvertor.jar', '--output', $tempJar, '--clobber')
        & gh @ghArgs
        if ($LASTEXITCODE -ne 0) { throw "gh release download failed (exit $LASTEXITCODE)." }
    } else {
        if ($Version -eq 'latest') {
            $jarUrl = "https://github.com/$Repo/releases/latest/download/modelconvertor.jar"
        } else {
            $jarUrl = "https://github.com/$Repo/releases/download/$Version/modelconvertor.jar"
        }
        try {
            Invoke-WebRequest -Uri $jarUrl -OutFile $tempJar -UseBasicParsing
        } catch {
            throw ("Download failed: {0}. For a private repo use: .\install.ps1 -UseGh" -f $_.Exception.Message)
        }
    }
    if (-not (Test-Path -LiteralPath $tempJar) -or (Get-Item -LiteralPath $tempJar).Length -eq 0) {
        throw "Downloaded JAR is missing or empty (check the Release asset name is modelconvertor.jar)."
    }

    # Validate JAR structure without running it: it must be a valid zip that
    # contains the manifest and the expected entry class. This rejects HTML error
    # pages and corrupt files without executing untrusted code.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($tempJar)
        try {
            $names = @($zip.Entries | ForEach-Object { $_.FullName })
        } finally {
            $zip.Dispose()
        }
    } catch {
        throw "Downloaded file is not a valid JAR (zip open failed): $($_.Exception.Message)"
    }
    $required = @('META-INF/MANIFEST.MF', 'org/sqlmodel/Main.class')
    $missing = $required | Where-Object { $names -notcontains $_ }
    if ($missing) {
        throw "Downloaded JAR is not modelconvertor (missing: $($missing -join ', '))."
    }

    # === Phase 3: apply changes ==============================================

    New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
    Move-Item -LiteralPath $tempJar -Destination $jarTarget -Force
    Copy-Item -LiteralPath $cmdSource -Destination $cmdTarget -Force

    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $entries  = @($userPath -split ';' | Where-Object { $_ })
    if ($entries -notcontains $InstallDir) {
        [Environment]::SetEnvironmentVariable('Path', (($entries + $InstallDir) -join ';'), 'User')
        Write-Host "Added to user PATH: $InstallDir (open a new terminal for it to take effect)"
    }

    New-Item -ItemType Directory -Force -Path $skillDir | Out-Null
    Copy-Item -LiteralPath $skillSource -Destination $skillTarget -Force
} finally {
    if (Test-Path -LiteralPath $tempJar) { Remove-Item -LiteralPath $tempJar -Force }
}

# --- Oracle config check (do not create; it holds a plaintext password) --------
$oracleProps = Join-Path $env:USERPROFILE '.modelconvertor\oracle.properties'
if (-not (Test-Path -LiteralPath $oracleProps)) {
    Write-Warning "Oracle config not found: $oracleProps"
    Write-Warning 'Create it with oracle.url / oracle.username / oracle.password / oracle.schema before generating models.'
}

Write-Host ''
Write-Host 'ModelConvertor installed. Restart Claude Code so the global skill is picked up.'
Write-Host 'Verify with: modelconvertor.cmd --help'
