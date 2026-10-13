#!/usr/bin/env bash
# Baut den .NET-Teil (ADR-005: getrennt vom Maven-Build):
#   1. Unity-Fixtures (testdata/unity-sources) zweimal und prüft, dass die Assembly byte-gleich ist,
#   2. das Frontend-Programm frontend-unity/dotnet (Release) und führt dessen Tests aus.
# Aufruf aus der Repo-Wurzel: scripts/build-dotnet.sh
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

if ! command -v dotnet >/dev/null 2>&1; then
  echo "dotnet nicht gefunden: .NET SDK 8 installieren (docs/setup.md)" >&2
  exit 1
fi

fixture_dll="testdata/unity-sources/bin/Release/netstandard2.1/Assembly-CSharp.dll"

build_fixtures() {
  rm -rf testdata/unity-sources/bin testdata/unity-sources/obj testdata/unity-stubs/bin testdata/unity-stubs/obj
  dotnet build testdata/unity-sources -c Release -nologo -v quiet
}

echo "== Fixtures (zweimal, Reproduzierbarkeit) =="
build_fixtures
first="$(sha256sum "$fixture_dll" | cut -d' ' -f1)"
build_fixtures
second="$(sha256sum "$fixture_dll" | cut -d' ' -f1)"
if [ "$first" != "$second" ]; then
  echo "Fixture-Build ist nicht reproduzierbar: $first != $second" >&2
  exit 1
fi
echo "Assembly-CSharp.dll reproduzierbar: $first"

if [ -d frontend-unity/dotnet ]; then
  echo "== Frontend =="
  dotnet build frontend-unity/dotnet -c Release -nologo -v quiet
  dotnet test frontend-unity/dotnet -c Release -nologo -v quiet --no-build
fi
