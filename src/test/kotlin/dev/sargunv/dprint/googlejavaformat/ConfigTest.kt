package dev.sargunv.dprint.googlejavaformat

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ConfigTest {
  private fun resolved(global: String = "{}", plugin: String = "{}"): JsonObject =
    Json.parseToJsonElement(resolveConfig(global, plugin).resolvedJson()).jsonObject

  private fun expected(json: String) = Json.parseToJsonElement(json).jsonObject

  @Test
  fun defaultsToGoogleStyle() {
    val config = resolveConfig("{}", "{}")
    assertEquals(emptyList(), config.diagnostics)
    assertEquals("[]", config.diagnosticsJson())
    assertEquals(
      expected(
        """
        |{"style":"google","lineWidth":100,"formatJavadoc":true,"reorderModifiers":true,
        |"sortImports":true,"removeUnusedImports":true,"reflowLongStrings":true,
        |"newLineKind":"auto"}
        """
          .trimMargin(),
      ),
      resolved(),
    )
  }

  @Test
  fun aospStyle() {
    val style = resolveConfig("{}", """{"style":"aosp"}""").options.style()
    assertEquals(2, style.indentationMultiplier())
    assertEquals(true, style.isAosp)
  }

  @Test
  fun inheritsGlobalConfig() {
    val global = """{"lineWidth":80,"indentWidth":8,"useTabs":true,"newLineKind":"crlf"}"""
    val config = resolveConfig(global, "{}")
    assertEquals(emptyList(), config.diagnostics)
    assertEquals(80, config.options.maxLineLength())
    // Indentation comes from the style; global indentWidth and useTabs don't apply.
    assertEquals(1, config.options.indentationMultiplier())
    assertEquals(false, config.options.useTabs())
    assertEquals(NewLineKind.CRLF, config.newLineKind)
  }

  @Test
  fun pluginConfigWinsOverGlobal() {
    val config =
      resolveConfig(
        """{"lineWidth":80,"newLineKind":"crlf"}""",
        """{"lineWidth":120,"newLineKind":"lf"}""",
      )
    assertEquals(120, config.options.maxLineLength())
    assertEquals(NewLineKind.LF, config.newLineKind)
  }

  @Test
  fun allKeys() {
    val plugin =
      """
      |{"style":"aosp","lineWidth":90,"formatJavadoc":false,"reorderModifiers":false,
      |"sortImports":false,"removeUnusedImports":false,"reflowLongStrings":false,
      |"newLineKind":"system"}
      """
        .trimMargin()
    val config = resolveConfig("{}", plugin)
    assertEquals(emptyList(), config.diagnostics)
    assertEquals(expected(plugin), resolved(plugin = plugin))
  }

  @Test
  fun withOverrides() {
    val config = resolveConfig("""{"lineWidth":80}""", """{"style":"aosp","sortImports":false}""")
    val overridden = config.withOverrides("""{"lineWidth":60,"sortImports":true}""")
    assertEquals("aosp", overridden.style)
    assertEquals(60, overridden.options.maxLineLength())
    assertEquals(true, overridden.sortImports)
    assertEquals(80, config.withOverrides("{}").options.maxLineLength())
  }

  @Test
  fun diagnostics() {
    val config =
      resolveConfig(
        "{}",
        """
        |{"style":"palantir","lineWidth":"wide","formatJavadoc":"yes","reorderModifiers":1,
        |"sortImports":null,"removeUnusedImports":"no","reflowLongStrings":0,
        |"newLineKind":"cr","useTabs":true}
        """
          .trimMargin(),
      )
    assertEquals(
      listOf(
        "style" to "Expected one of google, aosp, got \"palantir\"",
        "lineWidth" to "Expected a positive integer, got \"wide\"",
        "formatJavadoc" to "Expected a boolean, got \"yes\"",
        "reorderModifiers" to "Expected a boolean, got 1",
        "sortImports" to "Expected a boolean, got null",
        "removeUnusedImports" to "Expected a boolean, got \"no\"",
        "reflowLongStrings" to "Expected a boolean, got 0",
        "newLineKind" to "Expected one of auto, lf, crlf, system, got \"cr\"",
        "useTabs" to "Unknown property in configuration",
      ),
      config.diagnostics,
    )
    // Invalid values fall back to the defaults.
    assertEquals(resolved(), resolved(plugin = """{"style":"palantir","lineWidth":"wide"}"""))
    assertEquals(
      expected("""{"propertyName":"useTabs","message":"Unknown property in configuration"}"""),
      Json.parseToJsonElement(config.diagnosticsJson()).jsonArray.last(),
    )
  }

  @Test
  fun schemaCoversResolvedConfig() {
    val schema = Json.parseToJsonElement(File("schema.json").readText()).jsonObject
    assertEquals(resolved().keys, schema.getValue("properties").jsonObject.keys)
  }
}
