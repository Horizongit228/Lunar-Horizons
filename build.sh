#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if command -v gradle >/dev/null 2>&1; then exec gradle build; fi
version=8.8
cache="$PWD/.gradle-local"
mkdir -p "$cache"
if [[ ! -x "$cache/gradle-$version/bin/gradle" ]]; then
  curl -fL "https://services.gradle.org/distributions/gradle-$version-bin.zip" -o "$cache/gradle.zip"
  unzip -q -o "$cache/gradle.zip" -d "$cache"
fi
exec "$cache/gradle-$version/bin/gradle" build
