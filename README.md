# dprint-plugin-google-java-format

A [dprint](https://dprint.dev) plugin that formats Java with
[google-java-format](https://github.com/google/google-java-format). Its output
matches the google-java-format CLI's.

## Setup

```sh
dprint add sargunv/dprint-plugin-google-java-format
```

This adds an entry like the following to your dprint config:

```jsonc
{
  "plugins": [
    "https://plugins.dprint.dev/sargunv/google-java-format-0.1.0.json@<checksum>"
  ]
}
```

## Configuration

Options go under the `googleJavaFormat` key:

```jsonc
{ "googleJavaFormat": { "style": "aosp" } }
```

| Option                | Values                         | Default                           |
| --------------------- | ------------------------------ | --------------------------------- |
| `style`               | `google`, `aosp`               | `google`                          |
| `lineWidth`           | integer                        | global `lineWidth`, else 100      |
| `useTabs`             | boolean                        | global `useTabs`, else `false`    |
| `formatJavadoc`       | boolean                        | `true`                            |
| `reorderModifiers`    | boolean                        | `true`                            |
| `sortImports`         | boolean                        | `true`                            |
| `removeUnusedImports` | boolean                        | `true`                            |
| `reflowLongStrings`   | boolean                        | `true`                            |
| `newLineKind`         | `auto`, `lf`, `crlf`, `system` | global `newLineKind`, else `auto` |

`style` matches the CLI's `--style`, `lineWidth` its `--max-line-length`, and
the other boolean options its `--skip-*` flags. `useTabs` has no CLI equivalent:
it indents with one tab per level, counted as 2 columns in the google style and
4 in aosp.

Like other dprint plugins, `lineWidth`, `useTabs`, and `newLineKind` default to
dprint's global options. If your global `lineWidth` isn't 100, set
`"googleJavaFormat": { "lineWidth": 100 }` to keep google-java-format's default.
The global `indentWidth` doesn't apply: each style sets its own indents.

Range formatting is not supported.

## Versioning

The plugin is versioned independently of google-java-format. Each release's
notes say which google-java-format version it bundles. Upgrading the plugin may
change formatting the same way upgrading google-java-format would.

## Platforms

Linux (x86_64, aarch64), macOS (aarch64), and Windows (x86_64).

## Performance

Formatting [commons-lang](https://github.com/apache/commons-lang) (630 files)
and [java-system-bindings](https://github.com/sargunv/java-system-bindings) (552
files) with `dprint fmt` on a 32-core Linux machine:

| Setup                                                   | commons-lang | java-system-bindings |
| ------------------------------------------------------- | ------------ | -------------------- |
| dprint-plugin-exec + google-java-format's native binary | 3.8 s        | 1.2 s                |
| dprint-plugin-google-java-format                        | 2.3 s        | 0.43 s               |

## License

MIT OR Apache-2.0. The plugin bundles google-java-format, which is Apache-2.0
(see `GOOGLE_JAVA_FORMAT_LICENSE`).
