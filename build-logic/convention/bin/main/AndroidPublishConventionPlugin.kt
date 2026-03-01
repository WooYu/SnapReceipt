import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

/**
 * Configures maven-publish for Android Library modules.
 *
 * Publishes the "release" variant. Coordinates are derived from gradle.properties:
 *   groupId  = lib.groupId
 *   version  = lib.version
 *   artifactId = project.name (e.g. "core-foundation")
 *
 * To publish locally:   ./gradlew publishToMavenLocal
 * To publish to remote: configure a remote repository in this plugin or project.
 */
class AndroidPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("maven-publish")

        val groupId = property("lib.groupId") as String
        val libVersion = property("lib.version") as String

        extensions.configure<LibraryExtension> {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("release") {
                        from(components["release"])

                        this.groupId = groupId
                        this.artifactId = project.name
                        this.version = libVersion

                        pom {
                            name.set(project.name)
                            description.set("Skybound Space reusable Android module: ${project.name}")
                        }
                    }
                }
            }
        }
    }
}
