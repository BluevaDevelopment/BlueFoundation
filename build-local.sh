#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

COUNTER_FILE="build.properties"
CURRENT_YEAR="$(date +%y)"

if [[ -f "$COUNTER_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$COUNTER_FILE"
fi

YEAR="${year:-$CURRENT_YEAR}"
BUILD="${build:-0}"

if [[ "$YEAR" != "$CURRENT_YEAR" ]]; then
  YEAR="$CURRENT_YEAR"
  BUILD=0
fi

BUILD=$((BUILD + 1))
REVISION="$YEAR.$BUILD"

cat > "$COUNTER_FILE" <<PROPS
year=$YEAR
build=$BUILD
PROPS

if [[ $# -eq 0 ]]; then
  set -- clean install
fi

echo "Building BlueFoundation $REVISION"
exec mvn -Drevision="$REVISION" "$@"
