package dev.sargunv.dprint.googlejavaformat

import dev.sargunv.dprint.googlejavaformat.protocol.Kind
import dev.sargunv.dprint.googlejavaformat.protocol.TestClient
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** The protocol server wired to the real google-java-format adapter, as `main` runs it. */
class GoogleJavaFormatHandlerTest {
  private val client = TestClient(GoogleJavaFormatHandler).also { it.handshake() }

  @AfterTest fun close() = client.close()

  @Test
  fun `plugin info`() {
    assertTrue(PLUGIN_VERSION.matches(Regex("""\d+\.\d+\.\d+""")), PLUGIN_VERSION)
    val v = PLUGIN_VERSION
    val base = "https://plugins.dprint.dev/sargunv/dprint-plugin-google-java-format"
    assertEquals(
      Json.parseToJsonElement(
          """{"name":"dprint-plugin-google-java-format","version":"$v",
            |"configKey":"googleJavaFormat",
            |"helpUrl":"https://github.com/sargunv/dprint-plugin-google-java-format",
            |"configSchemaUrl":"$base/$v/schema.json","updateUrl":"$base/latest.json"}"""
            .trimMargin(),
        )
        .jsonObject,
      Json.parseToJsonElement(client.request(Kind.PLUGIN_INFO).body!!).jsonObject,
    )
  }

  @Test
  fun license() {
    val license = client.request(Kind.LICENSE).body!!
    assertTrue(license.startsWith("MIT OR Apache-2.0"))
    assertContains(license, "Apache License")
  }

  @Test
  fun `diagnostics and file matching`() {
    client.registerConfig(1, plugin = """{"notAThing":true}""")
    assertContains(client.request(Kind.CONFIG_DIAGNOSTICS) { writeInt(1) }.body!!, "notAThing")
    assertEquals(
      """{"fileExtensions":["java"],"fileNames":[]}""",
      client.request(Kind.FILE_MATCHING) { writeInt(1) }.body,
    )
  }

  @Test
  fun `formats java`() {
    client.registerConfig(1)
    assertEquals("class A {}\n", client.format("A.java", "class  A { }").body)
    assertNull(client.format("A.java", "class A {}\n").body)
    val error = client.format("A.java", "class A{void f( {}}")
    assertEquals(Kind.ERROR, error.kind)
    assertContains(error.body!!, "illegal start of type")
  }

  @Test
  fun `applies override config`() {
    client.registerConfig(1)
    val text = "class A {\n  void f() {}\n}\n"
    assertNull(client.format("A.java", text).body)
    assertEquals(
      "class A {\n    void f() {}\n}\n",
      client.format("A.java", text, override = """{"style":"aosp"}""").body,
    )
    assertEquals(
      "class A {\n\tvoid f() {}\n}\n",
      client.format("A.java", text, override = """{"useTabs":true}""").body,
    )
  }
}
