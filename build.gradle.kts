import org.gradle.internal.os.OperatingSystem

println(OperatingSystem.current())

version = "1.0.0"

val poiVersion = "5.0.0"
val picocliVersion = "4.6.1"
val gsonVersion = "2.8.7"

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    kotlin("kapt") version "1.5.10"

    jacoco
    id("org.barfuin.gradle.jacocolog") version "1.2.4"

    // Apply the application plugin to add support for building a CLI application in Java.
    application

    id("org.mikeneck.graalvm-native-image") version "1.4.1"
    // shadowJar / uberJar
    id ("com.github.johnrengelman.shadow") version "7.0.0"

    id("com.github.ben-manes.versions") version "0.39.0"
}

repositories {
    mavenCentral()
}

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
    implementation("com.google.code.gson:gson:$gsonVersion")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

}

application {
    // Define the main class for the application.
    mainClass.set("xls2json.AppKt")
    // if you need more memory
    // applicationDefaultJvmArgs = listOf("-Xmx4G")
}

distributions {
    main {
        contents {
            from("./LICENSE")
            from("./README.md")
        }
    }
}

kapt {
    arguments {
        arg("project", "${project.name}")
    }
}

tasks.register<Copy>("generateBuildInfo") {
    val templateContext = mutableMapOf("version" to project.version)
    // for gradle up-to-date check
    inputs.properties(templateContext)
    inputs.files(fileTree("src/template/kotlin"))
    from("src/template/kotlin")
    into("$buildDir/generated/kotlin")
    expand(templateContext)
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated/kotlin")
        }
    }
}

tasks.compileKotlin {
    dependsOn(":generateBuildInfo")
}

tasks.compileJava {
    options.release.set(11)
}

tasks.withType<Test> {
    useJUnitPlatform()
    this.testLogging {
        this.showStandardStreams = true
        this.events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report

    reports {
        xml.isEnabled = true
        csv.isEnabled = true
        html.isEnabled = true
    }
    sourceSets(sourceSets.main.get())
}

configurations {
    shadowJar
}

nativeImage {
    dependsOn(tasks.shadowJar)
    runtimeClasspath = configurations.shadowJar

    graalVmHome = System.getProperty("java.home")

    buildType { build ->
        build.executable(main="xls2json.AppKt")
    }

    mainClass = "xls2json.AppKt"
    executableName = "xls2json"
    outputDirectory = file("$buildDir/executable")
    arguments(
        "--no-fallback",  // build a standalone image or report a failure.
        if (OperatingSystem.current().isLinux()) "--static" else "",
        "--report-unsupported-elements-at-runtime",
        "--verbose",
        "--allow-incomplete-classpath",
        "--enable-all-security-services",
        "-H:+AddAllCharsets",
        "-H:ConfigurationFileDirectories=native-image-config",
        "-H:+RemoveUnusedSymbols",
        // "-H:+PrintUniverse",
        // "--dry-run",
    )
}

generateNativeImageConfig {
    enabled = true
    byRunningApplicationWithoutArguments()
    byRunningApplication {
      arguments("-h")
    }
    byRunningApplication {
      arguments("--sheetnames", "src/test/resources/sample.xls")
    }
    byRunningApplication {
      arguments("--sheetnames", "src/test/resources/sample.xlsx")
    }
    byRunningApplication {
      arguments("src/test/resources/sample.xls")
    }
    byRunningApplication {
      arguments("src/test/resources/sample.xlsx")
    }
}

// Output to build/libs/shadow.jar
tasks.shadowJar {
    mergeServiceFiles()
    archiveBaseName.set("xls2json")
}
