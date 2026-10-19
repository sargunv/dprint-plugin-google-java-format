package dev.sargunv.dprint.googlejavaformat

import com.google.googlejavaformat.java.ImportOrderer
import com.google.googlejavaformat.java.RemoveUnusedImports
import com.google.googlejavaformat.java.StringWrapper
import java.util.concurrent.ConcurrentHashMap

/**
 * Formats like the google-java-format CLI: layout, then imports, then long strings. Returns the
 * formatted text, or null when nothing changed. Throws google-java-format's FormatterException on
 * invalid input.
 */
fun formatJava(text: String, config: ResolvedConfig): String? {
  val formatter = config.formatter
  var formatted = formatter.formatSource(text)
  if (config.removeUnusedImports) formatted = RemoveUnusedImports.removeUnusedImports(formatted)
  if (config.sortImports)
    formatted = ImportOrderer.reorderImports(formatted, config.options.style())
  if (config.reflowLongStrings) formatted = StringWrapper.wrap(formatted, formatter)
  // google-java-format keeps the input's line endings, which is what "auto" means.
  val output =
    when (config.newLineKind) {
      NewLineKind.AUTO -> formatted
      NewLineKind.LF -> formatted.withLineEndings("\n")
      NewLineKind.CRLF -> formatted.withLineEndings("\r\n")
      NewLineKind.SYSTEM -> formatted.withLineEndings(System.lineSeparator())
    }
  return output.takeIf { it != text }
}

private fun String.withLineEndings(separator: String) = lineSequence().joinToString(separator)

// Worker isolates share no objects with the main one, so the config arrives as its resolved JSON
// and is parsed once per isolate.
private val workerConfigs = ConcurrentHashMap<String, ResolvedConfig>()

/**
 * [formatJava] as the worker isolates in [dev.sargunv.dprint.googlejavaformat.isolates.IsolatePool]
 * run it.
 */
fun formatInWorker(configJson: String, text: ByteArray): ByteArray? {
  val config = workerConfigs.getOrPut(configJson) { resolveConfig("{}", configJson) }
  return formatJava(text.decodeToString(), config)?.encodeToByteArray()
}
