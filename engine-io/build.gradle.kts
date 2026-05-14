plugins {
    java
    `maven-publish`
}

group = "com.ccl.io.engine"

subprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.8.2"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
            options.isDeprecation = false
            options.release.set(8)
        }

        test {
            useJUnitPlatform()
        }
    }
}
