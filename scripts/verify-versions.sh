#!/usr/bin/env bash
# Re-verifies Kordex's Minecraft version range for the Fabric and Forge adapters.
#
# A normal build compiles kordex-fabric / kordex-forge only against the OLDEST release of the 26.x
# line (26.1). This script compiles the very same adapter sources against every release of that
# line, one after another, so an API change between releases shows up as a compile error.
#
# kordex-bukkit and kordex-paper are covered by `./gradlew check` (it re-runs their tests against each
# supported server API), and the 1.21.11 line has its own modules that a normal build compiles.
#
# Usage:  scripts/verify-versions.sh [26.1 26.1.1 26.1.2 26.2]
# Note:   the first compile against a release downloads/prepares it, which takes several minutes.
set -u

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

if [ "$#" -gt 0 ]; then versions=("$@"); else versions=(26.1 26.1.1 26.1.2 26.2); fi

failed=()
for version in "${versions[@]}"; do
  for loader in fabric forge; do
    echo "== kordex-$loader against Minecraft $version"
    # Separate processes on purpose: Loom and ForgeGradle hold cache locks that a nested in-process build would contend for.
    if ! "$root/gradlew" ":kordex-$loader:compileKotlin" "-Pkordex.minecraft=$version" --no-build-cache --console=plain; then
      failed+=("kordex-$loader @ $version")
    fi
  done
done

if [ "${#failed[@]}" -gt 0 ]; then
  echo "FAILED: ${failed[*]}"
  exit 1
fi
echo "The Fabric and Forge adapters compile against Minecraft ${versions[*]}."
