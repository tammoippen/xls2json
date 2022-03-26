import org.gradle.internal.os.OperatingSystem

println(OperatingSystem.current())

version = "1.2.2"

val poiVersion = "5.2.2"
val picocliVersion = "4.6.3"
val jacksonVersion = "2.13.2"
// configure outputs of shadowJar for nativeImage
val shadowJarConf by configurations.creating

plugins {
  idea
  // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
  id("org.jetbrains.kotlin.jvm") version "1.6.10"
  kotlin("kapt") version "1.6.10"

  jacoco
  id("org.barfuin.gradle.jacocolog") version "2.0.0"

  // Apply the application plugin to add support for building a CLI application in Java.
  application

  id("org.mikeneck.graalvm-native-image") version "1.4.1"
  // shadowJar / uberJar
  id("com.github.johnrengelman.shadow") version "7.1.2"

  id("com.github.ben-manes.versions") version "0.42.0"
  id("com.diffplug.spotless") version "6.3.0"
}

repositories { mavenCentral() }

dependencies {
  // Align versions of all Kotlin components
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  // Use the Kotlin JDK 8 standard library.
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  // cli
  implementation("info.picocli:picocli:$picocliVersion")
  kapt("info.picocli:picocli-codegen:$picocliVersion")
  // excel
  implementation("org.apache.poi:poi:$poiVersion")
  implementation("org.apache.poi:poi-ooxml:$poiVersion")
  implementation("org.apache.poi:poi-ooxml-full:$poiVersion")
  // json
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

  // Use the Kotlin test library.
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-params")

  // add uberJar task outputs to uberJar configuration
  shadowJarConf(provider { project.tasks.shadowJar.get().outputs.files })
}

idea {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}

application {
  // Define the main class for the application.
  mainClass.set("xls2json.AppKt")
}

sourceSets { main { java { srcDir("$buildDir/generated/kotlin") } } }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    ktfmt()
    ktlint("0.41.0").userData(mapOf("indent_size" to "2", "continuation_indent_size" to "2"))
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktfmt()
  }
}

kapt { arguments { arg("project", "${project.name}") } }

tasks.register<Copy>("generateBuildInfo") {
  val templateContext = mutableMapOf("version" to project.version)
  // for gradle up-to-date check
  inputs.properties(templateContext)
  inputs.files(fileTree("src/template/kotlin"))
  from("src/template/kotlin")
  into("$buildDir/generated/kotlin")
  expand(templateContext)
}

tasks.compileKotlin { dependsOn(":generateBuildInfo") }

tasks.named("spotlessKotlin") { dependsOn(":generateBuildInfo") }

tasks.spotlessApply { dependsOn(":generateBuildInfo") }

tasks.spotlessCheck { dependsOn(":generateBuildInfo") }

tasks.compileJava { options.release.set(11) }

tasks.withType<Test> {
  useJUnitPlatform()
  this.testLogging {
    this.showStandardStreams = true
    this.events("passed", "skipped", "failed")
  }
  finalizedBy(tasks.jacocoTestReport)
}

jacoco { toolVersion = "0.8.7" }

tasks.jacocoTestReport {
  dependsOn(tasks.test) // tests are required to run before generating the report

  reports {
    xml.required.set(true)
    csv.required.set(true)
    html.required.set(true)
  }
  sourceSets(sourceSets.main.get())
}

distributions {
  main {
    contents {
      from("./LICENSE")
      from("./README.md")
    }
  }
}

nativeImage {
  dependsOn(tasks.shadowJar)
  runtimeClasspath = shadowJarConf

  graalVmHome = System.getProperty("java.home")

  buildType { build -> build.executable(main = "xls2json.AppKt") }

  mainClass = "xls2json.AppKt"
  executableName = "xls2json"
  outputDirectory = file("$buildDir/executable")
  arguments(
      "--no-fallback", // build a standalone image or report a failure.
      if (OperatingSystem.current().isLinux()) "--static" else "",
      "--report-unsupported-elements-at-runtime",
      "--verbose",
      "--allow-incomplete-classpath",
      "--enable-all-security-services",
      "-H:+AddAllCharsets",
      "-H:ConfigurationFileDirectories=native-image-config",
      "-H:+RemoveUnusedSymbols",
      "-J-Dfile.encoding=UTF-8",
      // "-H:+PrintUniverse",
      // "--dry-run",
      )
}

generateNativeImageConfig {
  enabled = false
  // unnecessary, as we have the config precomuted and checked in
  // byRunningApplicationWithoutArguments()
  // byRunningApplication { arguments("-h") }
  // byRunningApplication { arguments("--list-tables", "src/test/resources/sample.xls") }
  // byRunningApplication { arguments("--list-tables", "src/test/resources/sample.xlsx") }
  // byRunningApplication { arguments("src/test/resources/sample.xls") }
  // byRunningApplication { arguments("src/test/resources/sample.xlsx") }
}

// Output to build/libs/shadow.jar
tasks.shadowJar {
  mergeServiceFiles()
  archiveBaseName.set(project.name)
}
