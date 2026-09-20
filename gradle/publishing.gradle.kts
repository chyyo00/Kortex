// Maven Central publishing setup shared by every Kordex library module. Each module applies it at the
// end of its own build script:
//
//     description = "..."   // becomes the POM description
//     apply(from = rootProject.file("gradle/publishing.gradle.kts"))
//
// The example projects are never published.
//
// Nothing in here contacts a remote repository. `publishAllPublicationsToCentralBundleRepository`
// only writes a Maven-layout directory (build/central-bundle at the repository root); the root
// project's `centralBundle` task validates that directory against Maven Central's requirements and
// zips it into the file you upload to the Central Portal (see README, "Maven Central 배포").

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

apply(plugin = "maven-publish")
apply(plugin = "signing")

val moduleDescription: String = project.description
    ?: error("${project.path} must set `description = \"...\"` before applying gradle/publishing.gradle.kts (it becomes the POM description).")

fun gradleProp(name: String): String? = providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }

// ---- what Central requires next to the jar: -sources.jar and -javadoc.jar ------------------------

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
}

// Kordex is written in Kotlin, so the Java `javadoc` task has nothing to document and the javadoc jar
// would be empty. Central accepts a placeholder that says where the documentation lives; the KDoc
// itself ships in -sources.jar, which IDEs pick up automatically.
val javadocReadmeDir = layout.buildDirectory.dir("javadoc-readme")
val javadocReadme = tasks.register("javadocReadme") {
    val text = buildString {
        appendLine("${project.name} ${project.version}")
        appendLine()
        appendLine(moduleDescription)
        appendLine()
        appendLine("Kordex is a Kotlin library, so no Javadoc is generated. The KDoc comments of the public API are in")
        appendLine("the accompanying ${project.name}-${project.version}-sources.jar.")
        gradleProp("kordex.pom.url")?.let { appendLine("Documentation and usage: $it") }
    }
    inputs.property("text", text)
    outputs.dir(javadocReadmeDir)
    doLast { javadocReadmeDir.get().asFile.resolve("README.txt").apply { parentFile.mkdirs() }.writeText(text) }
}
tasks.named<Jar>("javadocJar") { from(javadocReadme) }

// ---- POM ----------------------------------------------------------------------------------------

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(project.name.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercase) })
                description.set(moduleDescription)
                gradleProp("kordex.pom.url")?.let { url.set(it) }

                gradleProp("kordex.pom.license.name")?.let { licenseName ->
                    licenses {
                        license {
                            name.set(licenseName)
                            gradleProp("kordex.pom.license.url")?.let { url.set(it) }
                        }
                    }
                }

                gradleProp("kordex.pom.developer.name")?.let { developerName ->
                    developers {
                        developer {
                            name.set(developerName)
                            gradleProp("kordex.pom.developer.email")?.let { email.set(it) }
                            gradleProp("kordex.pom.developer.organization")?.let { organization.set(it) }
                            gradleProp("kordex.pom.developer.organizationUrl")?.let { organizationUrl.set(it) }
                        }
                    }
                }

                gradleProp("kordex.pom.scm.url")?.let { scmUrl ->
                    // https://host/owner/repo -> the two git URLs Central asks for, unless given explicitly.
                    val gitPath = scmUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/").removeSuffix(".git")
                    scm {
                        url.set(scmUrl)
                        connection.set(gradleProp("kordex.pom.scm.connection") ?: "scm:git:git://$gitPath.git")
                        developerConnection.set(gradleProp("kordex.pom.scm.developerConnection") ?: "scm:git:ssh://git@$gitPath.git")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "centralBundle"
            url = uri(rootProject.layout.buildDirectory.dir("central-bundle"))
        }
    }
}

// Publishing must start from an empty bundle directory, otherwise files from an earlier version
// would end up in the upload.
tasks.matching { it.name.startsWith("publish") && it.name.endsWith("ToCentralBundleRepository") }
    .configureEach { mustRunAfter(":cleanCentralBundle") }

// ---- signing (Central requires a .asc for every file) -------------------------------------------
//
// Nothing is signed unless you provide a key, so ordinary builds and local publishing need no GPG:
//   * CI / one-off:   ORG_GRADLE_PROJECT_signingInMemoryKey (armored private key), optionally
//                     ORG_GRADLE_PROJECT_signingInMemoryKeyId and ORG_GRADLE_PROJECT_signingInMemoryKeyPassword
//   * local keyring:  -Pkordex.signing.gpg=true   (uses the `gpg` on PATH and its agent)
val inMemoryKey = gradleProp("signingInMemoryKey")
val useGpgCommand = gradleProp("kordex.signing.gpg")?.toBoolean() == true

if (inMemoryKey != null || useGpgCommand) {
    extensions.configure<SigningExtension> {
        if (inMemoryKey != null) {
            val keyId = gradleProp("signingInMemoryKeyId")
            val password = gradleProp("signingInMemoryKeyPassword")
            if (keyId != null) useInMemoryPgpKeys(keyId, inMemoryKey, password) else useInMemoryPgpKeys(inMemoryKey, password)
        } else {
            useGpgCmd()
        }
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}
