#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
chmod +x gradlew
./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace
