package dev.sargunv.dprint.googlejavaformat

import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.JavaFormatterOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

private val STYLES = listOf("google", "aosp")

enum class NewLineKind(val key: String) {
  AUTO("auto"),
  LF("lf"),
  CRLF("crlf"),
  SYSTEM("system"),
}

class ResolvedConfig(
  val style: String,
  val options: JavaFormatterOptions,
  val sortImports: Boolean,
  val removeUnusedImports: Boolean,
  val reflowLongStrings: Boolean,
  val newLineKind: NewLineKind,
  val diagnostics: List<Pair<String, String>>,
  private val raw: JsonObject,
  private val global: JsonObject,
) {
  val formatter by lazy { Formatter(options) }

  fun diagnosticsJson(): String = buildJsonArray {
    diagnostics.forEach { (property, message) ->
      add(
        buildJsonObject {
          put("propertyName", property)
          put("message", message)
        },
      )
    }
  }
    .toString()

  fun resolvedJson(): String = buildJsonObject {
    put("style", style)
    put("lineWidth", options.maxLineLength())
    put("formatJavadoc", options.formatJavadoc())
    put("reorderModifiers", options.reorderModifiers())
    put("sortImports", sortImports)
    put("removeUnusedImports", removeUnusedImports)
    put("reflowLongStrings", reflowLongStrings)
    put("newLineKind", newLineKind.key)
  }
    .toString()

  /** The config as [IsolatePool]'s workers receive it. */
  val workerJson: String by lazy { resolvedJson() }

  fun withOverrides(overrideJson: String): ResolvedConfig =
    resolve(global, JsonObject(raw + Json.parseToJsonElement(overrideJson).jsonObject))
}

fun resolveConfig(globalJson: String, pluginJson: String): ResolvedConfig =
  resolve(
    Json.parseToJsonElement(globalJson).jsonObject,
    Json.parseToJsonElement(pluginJson).jsonObject,
  )

/**
 * Resolves the plugin's config. Plugin keys win over dprint's global ones, which win over
 * google-java-format's defaults. Invalid values fall back as if unset and are reported as
 * diagnostics.
 */
fun resolve(global: JsonObject, raw: JsonObject): ResolvedConfig {
  val diagnostics = mutableListOf<Pair<String, String>>()
  val props = raw.toMutableMap()

  fun <T> take(name: String, expected: String, parse: (JsonElement) -> T?): T? =
    props.remove(name)?.let { value ->
      parse(value) ?: null.also { diagnostics += name to "Expected $expected, got $value" }
    }

  fun JsonElement.str() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

  fun JsonElement.int(min: Int) =
    (this as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull?.takeIf { it >= min }

  fun JsonElement.bool() = (this as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull

  fun JsonElement.newLineKind() = str()?.let { s -> NewLineKind.entries.find { it.key == s } }

  fun flag(name: String) = take(name, "a boolean") { it.bool() } ?: true

  val style =
    take("style", "one of ${STYLES.joinToString()}") { v -> v.str()?.takeIf { it in STYLES } }
      ?: "google"
  val lineWidth =
    take("lineWidth", "a positive integer") { it.int(1) }
      ?: global["lineWidth"]?.int(1)
      ?: JavaFormatterOptions.Style.GOOGLE.maxLineLength()
  val formatJavadoc = flag("formatJavadoc")
  val reorderModifiers = flag("reorderModifiers")
  val sortImports = flag("sortImports")
  val removeUnusedImports = flag("removeUnusedImports")
  val reflowLongStrings = flag("reflowLongStrings")
  val newLineKind =
    take("newLineKind", "one of ${NewLineKind.entries.joinToString { it.key }}") {
      it.newLineKind()
    } ?: global["newLineKind"]?.newLineKind() ?: NewLineKind.AUTO
  props.keys.forEach { diagnostics += it to "Unknown property in configuration" }

  val styleBuilder = JavaFormatterOptions.Style.builder().maxLineLength(lineWidth)
  if (style == "aosp") styleBuilder.aosp() else styleBuilder.google()
  val options =
    JavaFormatterOptions.builder()
      .style(styleBuilder.build())
      .formatJavadoc(formatJavadoc)
      .reorderModifiers(reorderModifiers)
      .build()
  return ResolvedConfig(
    style,
    options,
    sortImports,
    removeUnusedImports,
    reflowLongStrings,
    newLineKind,
    diagnostics,
    raw,
    global,
  )
}
