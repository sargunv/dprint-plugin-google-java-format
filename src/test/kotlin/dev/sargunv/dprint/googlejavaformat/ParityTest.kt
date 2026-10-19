package dev.sargunv.dprint.googlejavaformat

import kotlin.test.Test
import kotlin.test.assertEquals

/** Compares against the google-java-format CLI; see src/test/resources/parity/README.md. */
class ParityTest {
  private val fixtures =
    listOf(
      "ClassLoaderUtils.java",
      "FailableBiFunction.java",
      "IDKey.java",
      "Messy.java",
      "UncheckedFutureImpl.java",
    )

  private fun read(dir: String, name: String) =
    javaClass.getResource("/parity/$dir/$name.txt")!!.readText()

  private fun check(style: String) {
    val config = resolveConfig("{}", """{"style":"$style"}""")
    for (name in fixtures) {
      val input = read("input", name)
      assertEquals(read(style, name), formatJava(input, config) ?: input, "$style/$name")
    }
  }

  @Test fun google() = check("google")

  @Test fun aosp() = check("aosp")
}
