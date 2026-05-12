plugins {
    java
    `maven-publish`
}

group = "com.ccl.io.engine"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    dependencies {
//        testImplementation(platform("org.junit:junit-bom:6.0.0"))
//        testImplementation("org.junit.jupiter:junit-jupiter")
//        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
                    //"-Xlint:unchecked",
                    //"-Xlint:deprecation"
                )
            )
            // options.release.set(8)
        }

        test {
            useJUnitPlatform()
        }
    }
}
