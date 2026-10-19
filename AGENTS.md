# dprint-plugin-google-java-format

dprint process plugin that formats Java with google-java-format, compiled to a
native executable with GraalVM native-image. Ported from
[dprint-plugin-ktfmt](https://github.com/sargunv/dprint-plugin-ktfmt); the
protocol, isolate pool, packaging, and workflows are shared code.

## Project map

- `src/main/kotlin/dev/sargunv/dprint/googlejavaformat/`:
  - `Main.kt`: entry point.
  - `GoogleJavaFormatHandler.kt`: what the plugin answers to dprint (plugin
    info, license, config, formatting).
  - `Config.kt`: config resolution and diagnostics.
  - `Format.kt`: formatting the way the google-java-format CLI does, also as the
    worker isolates run it.
- `src/main/kotlin/dev/sargunv/dprint/googlejavaformat/protocol/`: the dprint
  process plugin protocol (schema v5): `PluginServer`, the `PluginHandler` it
  calls, message framing, and the parent-process checker.
- `src/main/java/dev/sargunv/dprint/googlejavaformat/isolates/IsolatePool.java`:
  spreads native formats across native-image isolates with separate heaps.
- `src/main/resources/META-INF/native-image/dprint-plugin-google-java-format/`:
  registers this plugin's resources (licenses, `plugin.properties`).
  google-java-format ships its own native-image metadata in its jar.
- `schema.json`: config JSON schema, shipped as a release asset. `ConfigTest`
  checks its keys against `Config.kt`.
- `src/test/resources/parity/`: golden google-java-format CLI outputs per style
  (`.txt` so this repo's formatter skips them).
- `tests/plugin.bats`: integration tests running real dprint on the packaged
  plugin.
- `scripts/`: packaging (`package.sh`, `plugin-json.sh`), parity against the
  google-java-format CLI (`parity.sh`, `parity-corpus.sh`), and the Windows CI
  smoke test (`smoke.sh`).
- `.github/workflows/`: `ci.yml` and `release.yml` share the native build matrix
  in `native.yml`.

## Dev tool commands

- `mise run check`: run every formatter and linter in report-only mode.
- `mise run fix`: apply every formatter and auto-fixable lint.
- `mise run build`: build the JVM plugin and run unit tests.
- `mise run native`: compile the native executable (about 30 seconds).
- `mise run package`: native build, then zip it into `dist/`.
- `mise run test:integration`: package, then run the bats tests.
- `mise run parity <corpus-dir> [google|aosp]`: package, then diff against the
  google-java-format CLI.
- `mise run parity:corpus`: parity in every style on commons-lang at a pinned
  commit.
- `mise run verify`: check, build, and integration tests.

Use `mise tasks ls --all` for the full task list.

## Project invariants

- The release tag is the version, bare semver and independent of
  google-java-format's. Pushing it builds, tests, and publishes the GitHub
  release; untagged builds report `0.0.0`.
- The google-java-format version in `build.gradle.kts` and `mise.toml` (the
  reference CLI) must match.
- Release zips contain only the executable, named
  `dprint-plugin-google-java-format[.exe]`, at the root.
- stdout carries only protocol messages; `main` points `System.out` at stderr.
- Resources read at run time must be registered for native-image; the JVM unit
  tests won't notice a missing one, the bats suite will.
- google-java-format isn't always idempotent, and dprint retries until output is
  stable, so compare single passes (`--skip-stable-format`) against the CLI.
- Native formats run in worker isolates, which share no objects with the main
  one: the config crosses as `ResolvedConfig.workerJson`, so every setting must
  survive a `resolvedJson()` round trip.
