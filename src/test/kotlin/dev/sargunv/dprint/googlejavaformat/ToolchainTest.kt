package dev.sargunv.dprint.googlejavaformat

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** The parity tests compare against the CLI pinned in mise.toml, so it must be the bundled one. */
class ToolchainTest {
  private fun version(file: String, pattern: String) =
    assertNotNull(Regex(pattern).find(File(file).readText()), "$pattern in $file").groupValues[1]

  @Test
  fun `reference CLI matches the bundled google-java-format`() {
    assertEquals(
      version("build.gradle.kts", """"com\.google\.googlejavaformat:google-java-format:([^"]+)""""),
      version("mise.toml", """"github:google/google-java-format" = "([^"]+)""""),
    )
  }
}
