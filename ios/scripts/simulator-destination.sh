#!/usr/bin/env bash
# Prints an xcodebuild -destination value for an iPhone simulator running iOS 26 or later.
#
# Usage (from ios/):
#   DEST="$(scripts/simulator-destination.sh)"
#   xcodebuild test -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$DEST"
#
# Set SIMULATOR_UDID to use a specific simulator instead.
set -euo pipefail

if [[ -n "${SIMULATOR_UDID:-}" ]]; then
    echo "id=${SIMULATOR_UDID}"
    exit 0
fi

# `simctl list` prints runtime sections such as "-- iOS 27.2 --" followed by device lines such as
# "    iPhone 17 (UDID) (Shutdown)". Keep the first iPhone of each iOS 26+ runtime, then take the
# newest runtime.
udid="$(
    xcrun simctl list devices available | awk '
        /^-- iOS [0-9]+(\.[0-9]+)* --$/ {
            split($3, version, ".")
            runtime = version[1] * 1000 + version[2]
            eligible = version[1] >= 26
            picked = 0
            next
        }
        /^-- / { eligible = 0; next }
        eligible && !picked && $1 == "iPhone" {
            if (match($0, /\([0-9A-F-]{36}\)/)) {
                print runtime, substr($0, RSTART + 1, RLENGTH - 2)
                picked = 1
            }
        }
    ' | sort -n -r | head -n 1 | cut -d " " -f 2
)"

if [[ -z "$udid" ]]; then
    echo "No available iPhone simulator with iOS 26 or later." >&2
    exit 1
fi

echo "id=${udid}"
