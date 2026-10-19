#!/usr/bin/env bash
# Formats every .java file under a corpus directory through dprint with the
# packaged plugin and through the google-java-format CLI, then diffs the
# results. Both run a single pass (dprint's stable-format retry is skipped), as
# the CLI does.
# usage: scripts/parity.sh <corpus-dir> [google|aosp]
# Expects the platform zip in dist/ (mise run package).
set -euo pipefail
# shellcheck source=SCRIPTDIR/lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

corpus="$(cd "$1" && pwd)"
style="${2:-google}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

mkdir -p "$work/dist"
cp "$repo_root/dist/$plugin_name-$(host_target).zip" "$work/dist/"
plugin_ref="$("$repo_root/scripts/plugin-json.sh" "$work/dist")"
for side in plugin cli; do
  mkdir -p "$work/$side"
  (cd "$corpus" && find . -type f -name '*.java' -print0 |
    tar --null -T - -cf -) | tar -xf - -C "$work/$side"
done
count="$(find "$work/cli" -type f | wc -l | tr -d ' ')"

# dprint formats the files under the config's directory.
cat >"$work/plugin/dprint.json" <<JSON
{ "googleJavaFormat": { "style": "$style" }, "plugins": ["$plugin_ref"] }
JSON
start="$(date +%s)"
dprint fmt --config "$work/plugin/dprint.json" --config-discovery=false \
  --skip-stable-format --log-level=warn || true
echo "plugin: formatted $count files in $(($(date +%s) - start))s"

start="$(date +%s)"
{
  printf '%s\n' --replace "--style=$style"
  find "$work/cli" -type f
} >"$work/gjf-args"
gjf_cli "@$work/gjf-args" || true
echo "cli: formatted $count files in $(($(date +%s) - start))s"

if diff -r -x dprint.json "$work/cli" "$work/plugin" >"$work/diff.txt"; then
  echo "parity: all $count files identical ($style style)"
else
  echo "parity: differences found ($style style):"
  grep -E '^(diff|Only in)' "$work/diff.txt" | head -50 || true
  exit 1
fi
