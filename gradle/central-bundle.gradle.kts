// `./gradlew centralBundle` - builds the file you upload to the Maven Central Portal.
//
//   1. publishes every library module (see centralModules) into build/central-bundle,
//   2. checks that directory against Maven Central's published requirements (so a problem shows up
//      here in seconds instead of as a rejected deployment), and
//   3. zips it to build/central/kordex-<version>-central-bundle.zip.
//
// It never contacts Maven Central. Uploading is a separate, deliberate step (README, "Maven Central 배포"),
// because a release on Central can never be changed or deleted.
//
// Requirements (https://central.sonatype.org/publish/requirements/): sources + javadoc jars, a POM with
// name/description/url/license/developer(name+email)/scm, a .asc signature and md5+sha1 checksums for
// every file, and a non-SNAPSHOT version. Signing and the POM values come from the properties documented
// in gradle/publishing.gradle.kts and gradle.properties.

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Document

// Every module that is published. The examples and the shared/ source folders are not artifacts.
val centralModules = listOf(
    "kordex-core",
    "kordex-bukkit",
    "kordex-paper",
    "kordex-fabric",
    "kordex-fabric-1.21.11",
    "kordex-forge",
    "kordex-forge-1.21.11",
)

val bundleDirectory = layout.buildDirectory.dir("central-bundle")

val cleanCentralBundle = tasks.register<Delete>("cleanCentralBundle") {
    description = "Empties build/central-bundle so a bundle never contains files from an earlier run."
    delete(bundleDirectory)
}

/**
 * Every problem that would make Maven Central reject the bundle, as human-readable lines. Problems
 * that repeat across modules (no signature, no license, ...) are reported once, so the first run
 * on a fresh checkout is a short to-do list instead of a wall of text.
 */
fun bundleProblems(root: File, group: String, version: String, modules: List<String>): List<String> {
    val problems = mutableListOf<String>()
    if (version.endsWith("-SNAPSHOT")) problems += "version $version is a SNAPSHOT; Maven Central only accepts releases."

    val xpath = XPathFactory.newInstance().newXPath()
    fun Document.text(path: String): String? =
        (xpath.evaluate(path, this, XPathConstants.STRING) as String).trim().ifEmpty { null }

    // POM element -> the Gradle property that fills it (or the module's own build script, for name/description).
    val requiredPomFields = listOf(
        "/project/name" to "name",
        "/project/description" to "description",
        "/project/url" to "kordex.pom.url",
        "/project/licenses/license/name" to "kordex.pom.license.name",
        "/project/licenses/license/url" to "kordex.pom.license.url",
        "/project/developers/developer/name" to "kordex.pom.developer.name",
        "/project/developers/developer/email" to "kordex.pom.developer.email",
        "/project/scm/url" to "kordex.pom.scm.url",
        "/project/scm/connection" to "kordex.pom.scm.url",
        "/project/scm/developerConnection" to "kordex.pom.scm.url",
    )
    val isSideFile = Regex(".*\\.(asc|md5|sha1|sha256|sha512)")

    val unsigned = sortedSetOf<String>()
    val missingProperties = sortedSetOf<String>()

    for (module in modules) {
        val directory = root.resolve(group.replace('.', '/')).resolve(module).resolve(version)
        if (!directory.isDirectory) {
            problems += "$module: nothing was published for $version."
            continue
        }
        val base = "$module-$version"

        for (required in listOf("$base.jar", "$base-sources.jar", "$base-javadoc.jar", "$base.pom")) {
            if (!directory.resolve(required).isFile) problems += "$module: $required is missing."
        }

        directory.listFiles().orEmpty().filter { !it.name.matches(isSideFile) }.forEach { file ->
            if (!directory.resolve("${file.name}.asc").isFile) unsigned += file.name
            for (checksum in listOf("md5", "sha1")) {
                if (!directory.resolve("${file.name}.$checksum").isFile) problems += "$module: ${file.name}.$checksum is missing."
            }
        }

        val pomFile = directory.resolve("$base.pom")
        if (pomFile.isFile) {
            val pom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomFile)
            for ((path, property) in requiredPomFields) {
                if (pom.text(path) != null) continue
                if (property.startsWith("kordex.pom.")) missingProperties += property
                else problems += "$module: POM has no $path (set `$property = \"...\"` in the module's build script)."
            }
        }
    }

    if (unsigned.isNotEmpty()) {
        problems += "${unsigned.size} file(s) have no .asc signature (e.g. ${unsigned.first()}). Provide a signing key: " +
            "-Pkordex.signing.gpg=true to use your local gpg, or the environment variables " +
            "ORG_GRADLE_PROJECT_signingInMemoryKey / ORG_GRADLE_PROJECT_signingInMemoryKeyPassword."
    }
    if (missingProperties.isNotEmpty()) {
        problems += "The POMs lack metadata that Central requires. Set these Gradle properties in gradle.properties: " +
            missingProperties.joinToString(", ") + "."
    }
    return problems
}

tasks.register<Zip>("centralBundle") {
    group = "publishing"
    description = "Builds, validates and zips the Maven Central upload bundle. Uploads nothing."

    dependsOn(cleanCentralBundle)
    dependsOn(centralModules.map { ":$it:publishAllPublicationsToCentralBundleRepository" })

    // Central generates maven-metadata.xml itself.
    from(bundleDirectory) { exclude("**/maven-metadata.xml*") }
    archiveFileName.set("kordex-${project.version}-central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central"))

    val groupId = project.group.toString()
    val releaseVersion = project.version.toString()
    val bundleRoot = bundleDirectory
    doFirst {
        val problems = bundleProblems(bundleRoot.get().asFile, groupId, releaseVersion, centralModules)
        if (problems.isNotEmpty()) {
            throw GradleException(
                "The Central bundle would be rejected:\n" + problems.joinToString("\n") { "  - $it" } +
                    "\nSee README, \"Maven Central 배포\", for the signing and gradle.properties setup."
            )
        }
    }
}
