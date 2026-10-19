package dev.sargunv.dprint.googlejavaformat

import dev.sargunv.dprint.googlejavaformat.isolates.IsolatePool
import dev.sargunv.dprint.googlejavaformat.protocol.PluginHandler
import java.util.Properties
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

val PLUGIN_VERSION: String =
  Properties()
    .apply {
      GoogleJavaFormatHandler::class.java.getResourceAsStream("/plugin.properties")!!.use(::load)
    }
    .getProperty("version")

internal object GoogleJavaFormatHandler : PluginHandler<ResolvedConfig> {
  // In a native image, formats spread across up to 8 isolates so that one heap's stop-the-world
  // collection doesn't pause the rest.
  private val isolates =
    if (System.getProperty("org.graalvm.nativeimage.imagecode") == "runtime") {
      IsolatePool(Runtime.getRuntime().availableProcessors().coerceAtMost(8))
    } else {
      null
    }

  override fun pluginInfo() = buildJsonObject {
    put("name", "dprint-plugin-google-java-format")
    put("version", PLUGIN_VERSION)
    put("configKey", "googleJavaFormat")
    put("helpUrl", "https://github.com/sargunv/dprint-plugin-google-java-format")
    put(
      "configSchemaUrl",
      "https://plugins.dprint.dev/sargunv/dprint-plugin-google-java-format/$PLUGIN_VERSION/schema.json",
    )
    put(
      "updateUrl",
      "https://plugins.dprint.dev/sargunv/dprint-plugin-google-java-format/latest.json",
    )
  }
    .toString()

  override fun license() =
    listOf("/LICENSE", "/GOOGLE_JAVA_FORMAT_LICENSE").joinToString("\n\n") { name ->
      javaClass.getResourceAsStream(name)!!.use { it.readBytes().decodeToString() }
    }

  override fun registerConfig(globalJson: String, pluginJson: String) =
    resolveConfig(globalJson, pluginJson)

  override fun diagnostics(config: ResolvedConfig) = config.diagnosticsJson()

  override fun fileMatching(config: ResolvedConfig) =
    """{"fileExtensions":["java"],"fileNames":[]}"""

  override fun resolvedConfig(config: ResolvedConfig) = config.resolvedJson()

  override fun applyOverrides(config: ResolvedConfig, overrideJson: String) =
    config.withOverrides(overrideJson)

  // The JVM runs the worker path in-process, so its tests cover the config round trip too.
  override fun format(path: String, text: ByteArray, config: ResolvedConfig): ByteArray? {
    val pool = isolates
    return if (pool != null) pool.format(config.workerJson, text)
    else formatInWorker(config.workerJson, text)
  }
}
