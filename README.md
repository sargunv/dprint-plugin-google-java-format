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
| `formatJavadoc`       | boolean                        | `true`                            |
| `reorderModifiers`    | boolean                        | `true`                            |
| `sortImports`         | boolean                        | `true`                            |
| `removeUnusedImports` | boolean                        | `true`                            |
| `reflowLongStrings`   | boolean                        | `true`                            |
| `newLineKind`         | `auto`, `lf`, `crlf`, `system` | global `newLineKind`, else `auto` |

`style` matches the CLI's `--style`, `lineWidth` its `--max-line-length`, and
the boolean options its `--skip-*` flags.

Like other dprint plugins, `lineWidth` and `newLineKind` default to dprint's
global options. If your global `lineWidth` isn't 100, set `"googleJavaFormat": {
"lineWidth": 100 }` to keep google-java-format's default. The global
`indentWidth` and `useTabs` don't apply: each style sets its own indents.

Range formatting is not supported.

## Versioning

The plugin is versioned independently of google-java-format. Each release's
notes say which google-java-format version it bundles. Upgrading the plugin may
change formatting the same way upgrading google-java-format would.

## Platforms

Linux (x86_64, aarch64), macOS (aarch64), and Windows (x86_64).

## Performance

Formatting with `dprint fmt` on a 32-core Linux machine:

| Corpus                           | dprint-plugin-exec + google-java-format's native binary | dprint-plugin-google-java-format |
| -------------------------------- | ------------------------------------------------------- | -------------------------------- |
| commons-lang (630 files)         | 3.8 s                                                   | 2.3 s                            |
| java-system-bindings (552 files) | 1.2 s                                                   | 0.43 s                           |

## License

MIT OR Apache-2.0. The plugin bundles google-java-format, which is Apache-2.0
(see `GOOGLE_JAVA_FORMAT_LICENSE`).
