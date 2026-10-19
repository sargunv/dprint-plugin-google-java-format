# Runs real dprint against the packaged plugin (mise run package).

setup_file() {
  bats_require_minimum_version 1.5.0
  # shellcheck source=SCRIPTDIR/../scripts/lib.sh
  source "$BATS_TEST_DIRNAME/../scripts/lib.sh"
  local plugin_dir="$BATS_FILE_TMPDIR/plugin"
  mkdir -p "$plugin_dir"
  cp "$repo_root/dist/$plugin_name-$(host_target).zip" "$plugin_dir/"
  PLUGIN_REF="$("$repo_root/scripts/plugin-json.sh" "$plugin_dir")"
  export PLUGIN_REF
  # Keep the user's plugin cache out of it; dprint wants an absolute path.
  export DPRINT_CACHE_DIR="$BATS_FILE_TMPDIR/dprint-cache"
}

setup() {
  # shellcheck source=SCRIPTDIR/../scripts/lib.sh
  source "$BATS_TEST_DIRNAME/../scripts/lib.sh"
  ws="$BATS_TEST_TMPDIR/ws"
  mkdir -p "$ws"
  cd "$ws" || exit 1
}

# write_config '<extra top-level JSON members, each followed by a comma>'
write_config() {
  cat >"$ws/dprint.json" <<JSON
{
  ${1:-}
  "plugins": ["$PLUGIN_REF"]
}
JSON
}

dprint_ws() {
  dprint "$@" --config "$ws/dprint.json" --config-discovery=false
}

write_sources() {
  cat >"$ws/Example.java" <<'JAVA'
package example;
import java.util.List;
import java.util.Map;
public class Example { private final Map<String,List<Integer>> items; final static public int LIMIT=10;
  Example(Map<String,List<Integer>> items){this.items=items;}
  public String describe(){ return items.keySet().stream().filter(k->k.length()<LIMIT).sorted().reduce("",(a,b)->a+", "+b); }
}
JAVA
  cat >"$ws/Other.java" <<'JAVA'
package example;
interface Other{ void   run( ) ; }
JAVA
}

# Keeps the unformatted sources outside the workspace for expect_gjf.
save_sources() {
  orig="$BATS_TEST_TMPDIR/orig"
  mkdir -p "$orig"
  cp "$ws/Example.java" "$ws/Other.java" "$orig/"
}

restore_sources() {
  cp "$orig/Example.java" "$orig/Other.java" "$ws/"
}

# expect_gjf <file> [google-java-format CLI flags]: compares the workspace's
# <file> to the CLI's output for the saved original.
expect_gjf() {
  local file="$1"
  shift
  run --separate-stderr gjf_cli "$@" - --assume-filename "$file" <"$orig/$file"
  [ "$status" -eq 0 ]
  diff -u <(printf '%s\n' "$output") "$ws/$file"
}

@test "fmt matches the google-java-format CLI with the default style" {
  write_config
  write_sources
  save_sources

  run dprint_ws fmt
  [ "$status" -eq 0 ]

  run ! cmp -s "$ws/Example.java" "$orig/Example.java"
  expect_gjf Example.java
  expect_gjf Other.java
}

@test "fmt matches the google-java-format CLI with the aosp style and options" {
  write_config '"googleJavaFormat": { "style": "aosp", "lineWidth": 80, "sortImports": false },'
  write_sources
  save_sources

  run dprint_ws fmt
  [ "$status" -eq 0 ]
  expect_gjf Example.java --aosp --max-line-length 80 --skip-sorting-imports
  expect_gjf Other.java --aosp --max-line-length 80 --skip-sorting-imports
}

@test "fmt formats stdin" {
  write_config
  write_sources

  run --separate-stderr dprint_ws fmt --stdin Example.java <"$ws/Example.java"
  [ "$status" -eq 0 ]
  [[ "$output" == *$'  public String describe() {\n'* ]]
}

@test "check reports unformatted files and passes once formatted" {
  write_config
  write_sources

  run dprint_ws check
  [ "$status" -ne 0 ]
  [[ "$output" == *"Example.java"* ]]
  [[ "$output" == *"Other.java"* ]]

  run dprint_ws fmt
  [ "$status" -eq 0 ]
  run dprint_ws check
  [ "$status" -eq 0 ]
}

@test "a syntax error is reported per file without stopping the run" {
  write_config
  write_sources
  save_sources
  printf 'class Broken {\n  void oops( {\n' >"$ws/Broken.java"
  cp "$ws/Broken.java" "$orig/"

  run dprint_ws fmt
  [ "$status" -ne 0 ]
  [[ "$output" == *"Broken.java"* ]]
  [[ "$output" != *"Example.java"* ]]
  cmp -s "$ws/Broken.java" "$orig/Broken.java"
  expect_gjf Example.java
  expect_gjf Other.java
}

@test "license includes the plugin's and google-java-format's" {
  write_config

  run --separate-stderr dprint_ws license
  [ "$status" -eq 0 ]
  [[ "$output" == *$'DPRINT-PLUGIN-GOOGLE-JAVA-FORMAT LICENSE ====\nMIT OR Apache-2.0'* ]]
  [[ "$output" == *"Apache License"* ]]
}

@test "an unknown config property is a diagnostic" {
  write_config '"googleJavaFormat": { "definitelyUnknownOption": true },'

  run dprint_ws output-resolved-config
  [ "$status" -ne 0 ]
  [[ "$output" == *"definitelyUnknownOption"* ]]
  [[ "$output" == *"Unknown property"* ]]
}

@test "lineWidth comes from the default, the global config, then the plugin config" {
  write_config '"googleJavaFormat": {},'
  run dprint_ws output-resolved-config
  [ "$status" -eq 0 ]
  [[ "$output" == *'"lineWidth": 100'* ]]

  write_config '"lineWidth": 80, "googleJavaFormat": {},'
  run dprint_ws output-resolved-config
  [ "$status" -eq 0 ]
  [[ "$output" == *'"lineWidth": 80'* ]]

  write_config '"lineWidth": 80, "googleJavaFormat": { "lineWidth": 120 },'
  run dprint_ws output-resolved-config
  [ "$status" -eq 0 ]
  [[ "$output" == *'"lineWidth": 120'* ]]
}
