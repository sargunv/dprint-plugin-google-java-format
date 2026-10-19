#!/usr/bin/env bash
# Runs scripts/parity.sh in every style on Apache commons-lang at a pinned
# commit, fetched shallowly into build/commons-lang.
set -euo pipefail
# shellcheck source=SCRIPTDIR/lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

commit=45c80d99559017ea6efcb5db6156bb9a5c850d34
dir="$repo_root/build/commons-lang"
if [[ "$(git -C "$dir" rev-parse HEAD 2>/dev/null)" != "$commit" ]]; then
  rm -rf "$dir"
  git init -q "$dir"
  git -C "$dir" fetch -q --depth 1 https://github.com/apache/commons-lang "$commit"
  git -C "$dir" checkout -q FETCH_HEAD
fi
for style in google aosp; do
  "$repo_root/scripts/parity.sh" "$dir" "$style"
done
