plugins {
    `java-library`
    `maven-publish`
}

group = "com.ccl.io.socket"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    dependencies {
    }

    tasks {
        clean {
            delete(".settings", ".classpath", ".project", "bin")
        }

        withType<Jar> {
            manifest {
                attributes(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                )
            }
        }

        withType<JavaCompile>().configureEach {
            options.compilerArgs.addAll(
                listOf(
                    "-Xlint:-options",
                    "-Xlint:-unchecked",
                )
            )
            options.release.set(8)
        }

        test {
            useJUnitPlatform()
        }
    }
}

