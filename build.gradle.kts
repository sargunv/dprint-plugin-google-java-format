plugins {
  kotlin("jvm") version "2.4.10"
  id("org.graalvm.buildtools.native") version "1.1.8"
}

// A release build runs on its bare semver tag, which is the version; other builds report 0.0.0.
// scripts/lib.sh reads it the same way.
version =
  providers
    .exec {
      commandLine("git", "describe", "--tags", "--exact-match", "--match", "[0-9]*.[0-9]*.[0-9]*")
      isIgnoreExitValue = true
    }
    .standardOutput
    .asText
    .get()
    .trim()
    .ifEmpty { "0.0.0" }

repositories { mavenCentral() }

dependencies {
  testImplementation(kotlin("test"))
  implementation("com.google.googlejavaformat:google-java-format:1.37.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
  // native-image APIs for src/main/java.
  compileOnly("org.graalvm.sdk:nativeimage:25.0.1")
}

kotlin { jvmToolchain(25) }

// google-java-format parses with javac's internal APIs.
val javacExports =
  listOf("api", "code", "file", "parser", "tree", "util").map {
    "--add-exports=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED"
  }

graalvmNative {
  toolchainDetection = false
  binaries.named("main") {
    imageName = "dprint-plugin-google-java-format"
    mainClass = "dev.sargunv.dprint.googlejavaformat.MainKt"
    // Mirrors google-java-format's own native-image flags (core/pom.xml).
    buildArgs.addAll(
      "-O3",
      "-march=compatibility",
      "-H:+UnlockExperimentalVMOptions",
      "-H:IncludeResourceBundles=com.sun.tools.javac.resources.compiler",
      "-H:IncludeResourceBundles=com.sun.tools.javac.resources.javac",
      "--initialize-at-build-time=com.sun.tools.javac.file.Locations",
      "-H:+ReportExceptionStackTraces",
      "-H:-UseContainerSupport",
    )
    buildArgs.addAll(javacExports.map { "-J$it" })
  }
}

tasks.processResources {
  from("LICENSE")
  inputs.property("version", version)
  filesMatching("plugin.properties") { expand("version" to version) }
}

tasks.test {
  useJUnitPlatform()
  jvmArgs(javacExports)
  // ConfigTest and ToolchainTest read these.
  inputs.files("schema.json", "build.gradle.kts", "mise.toml")
}
