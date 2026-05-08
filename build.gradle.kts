plugins {
    `java-library`
    `maven-publish`
}

group = "com.ccl"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    tasks.clean {
        delete(".settings", ".classpath", ".project", "bin")
    }

    tasks.withType<Jar> {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            )
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:-options",
                //"-Xlint:unchecked",
                //"-Xlint:deprecation"
            )
        )
        options.release.set(8)
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            filter {
                includeTestsMatching("*Test")
                includeTestsMatching("*Tests")
            }
        }

        jvmArgs(
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
        )
    }
}

// License header configuration
val license by tasks.registering {
    group = "verification"
    description = "Check license headers"
    // License check is part of checkstyle
}

tasks {
    clean {
        delete(".settings", ".classpath", ".project", "bin")
    }

    // Build task
    register("buildAll") {
        group = "build"
        description = "Build all modules"
        dependsOn(subprojects.map { it.tasks.named("build") })
    }

    // Clean task
    register("cleanAll") {
        group = "build"
        description = "Clean all modules"
        dependsOn(subprojects.map { it.tasks.named("clean") }, clean)
    }

    withType<Wrapper> {
        gradleVersion = "9.4.0"
    }

    // Set the JVM arguments for the plugin task
    withType<JavaCompile> {
        options.release.set(8)
    }

}

