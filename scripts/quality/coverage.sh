#!/usr/bin/env bash
# Regenerate the JaCoCo XML report crapper reads.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# This module compiles against the published Nds4j jar.
echo "JaCoCo: $root/pom.xml"
mvn -B -ntp -f "$root/pom.xml" test -Djava.awt.headless=true
