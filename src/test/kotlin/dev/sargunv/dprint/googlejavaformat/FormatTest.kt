package dev.sargunv.dprint.googlejavaformat

import com.google.googlejavaformat.java.FormatterException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatTest {
  private fun config(plugin: String = "{}", global: String = "{}") = resolveConfig(global, plugin)

  private fun format(code: String, plugin: String = "{}", global: String = "{}") =
    formatJava(code, config(plugin, global))

  @Test
  fun formats() {
    assertEquals("class A {\n  void f() {}\n}\n", format("class A{void f( ){}}"))
  }

  @Test
  fun unchangedInputReturnsNull() {
    assertNull(format("class A {}\n"))
  }

  @Test
  fun parseErrorsThrow() {
    val error = assertFailsWith<FormatterException> { format("class A{void f( {}}") }
    assertTrue(error.message!!.contains("illegal start of type"), error.message)
  }

  @Test
  fun aospStyle() {
    assertEquals(
      "class A {\n    void f() {\n        g();\n    }\n}\n",
      format("class A{void f(){g();}}", """{"style":"aosp"}"""),
    )
  }

  @Test
  fun lineWidthFromPluginOrGlobal() {
    val code = "class A{void f( ){g(aaaaaaaa,bbbbbbbb);}}"
    val wrapped = "class A {\n  void f() {\n    g(\n        aaaaaaaa,\n        bbbbbbbb);\n  }\n}\n"
    assertEquals("class A {\n  void f() {\n    g(aaaaaaaa, bbbbbbbb);\n  }\n}\n", format(code))
    assertEquals(wrapped, format(code, """{"lineWidth":20}"""))
    assertEquals(wrapped, format(code, global = """{"lineWidth":20}"""))
  }

  @Test
  fun imports() {
    val code = "import java.util.List;\nimport a.B;\nimport a.A;\nclass C{A a;B b;}\n"
    assertEquals("import a.A;\nimport a.B;\n\nclass C {\n  A a;\n  B b;\n}\n", format(code))
    assertEquals(
      "import java.util.List;\nimport a.B;\nimport a.A;\n\nclass C {\n  A a;\n  B b;\n}\n",
      format(code, """{"sortImports":false,"removeUnusedImports":false}"""),
    )
  }

  @Test
  fun reorderModifiers() {
    val code = "class A{final static public int X=1;}\n"
    assertEquals("class A {\n  public static final int X = 1;\n}\n", format(code))
    assertEquals(
      "class A {\n  final static public int X = 1;\n}\n",
      format(code, """{"reorderModifiers":false}"""),
    )
  }

  @Test
  fun formatJavadoc() {
    val code = "class A{/** Does   a\n * thing.*/void f(){}}\n"
    assertEquals("class A {\n  /** Does a thing. */\n  void f() {}\n}\n", format(code))
    assertEquals(
      "class A {\n  /** Does   a\n   * thing.*/\n  void f() {}\n}\n",
      format(code, """{"formatJavadoc":false}"""),
    )
  }

  @Test
  fun reflowLongStrings() {
    val words = ('a'..'i').joinToString(" ") { "$it".repeat(10) }
    val code = "class A{String s=\"$words\";}\n"
    assertEquals(
      "class A {\n  String s =\n      \"${words.dropLast(11)}\"\n          + \" iiiiiiiiii\";\n}\n",
      format(code),
    )
    assertEquals(
      "class A {\n  String s =\n      \"$words\";\n}\n",
      format(code, """{"reflowLongStrings":false}"""),
    )
  }

  @Test
  fun newLineKindAutoKeepsInputLineEndings() {
    assertNull(format("class A {}\r\n"))
    assertEquals("class A {}\r\n", format("class A{}\r\n"))
  }

  @Test
  fun newLineKindForcesLineEndings() {
    assertEquals("class A {}\r\n", format("class A {}\n", """{"newLineKind":"crlf"}"""))
    assertNull(format("class A {}\r\n", """{"newLineKind":"crlf"}"""))
    assertEquals("class A {}\n", format("class A {}\r\n", global = """{"newLineKind":"lf"}"""))
  }
}
