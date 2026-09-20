<#
    Re-verifies Kordex's Minecraft version range for the Fabric and Forge adapters.

    A normal build compiles kordex-fabric / kordex-forge only against the OLDEST release of the 26.x
    line (26.1). This script compiles the very same adapter sources against every release of that
    line, one after another, so an API change between releases shows up as a compile error.

    kordex-bukkit and kordex-paper are covered by `gradlew check` (it re-runs their tests against each
    supported server API), and the 1.21.11 line has its own modules that a normal build compiles.

    Usage:   powershell -File scripts\verify-versions.ps1 [-Versions 26.1,26.2]
    Note:    the first compile against a release downloads/prepares it, which takes several minutes.
#>
param(
    [string[]]$Versions = @('26.1', '26.1.1', '26.1.2', '26.2')
)

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$failed = @()
foreach ($version in $Versions) {
    foreach ($loader in 'fabric', 'forge') {
        Write-Host "== kordex-$loader against Minecraft $version" -ForegroundColor Cyan
        # Separate processes on purpose: Loom and ForgeGradle hold cache locks that a nested in-process build would contend for.
        & "$root\gradlew.bat" ":kordex-${loader}:compileKotlin" "-Pkordex.minecraft=$version" --no-build-cache --console=plain
        if ($LASTEXITCODE -ne 0) { $failed += "kordex-$loader @ $version" }
    }
}

if ($failed.Count -gt 0) {
    Write-Host "FAILED: $($failed -join ', ')" -ForegroundColor Red
    exit 1
}
Write-Host "The Fabric and Forge adapters compile against Minecraft $($Versions -join ', ')." -ForegroundColor Green
