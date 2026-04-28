plugins {
    java
    `maven-publish`
}

description = "Socket.IO server core implementation"

val projectVersion = "4.0.0-SNAPSHOT"

configurations {
    testCompileOnly {
        extendsFrom(compileOnly.get())
    }
    testImplementation {
        extendsFrom(implementation.get(), compileOnly.get())
    }
}

dependencies {
    // Micrometer (provided scope in Maven)
    implementation(platform(libs.micrometer.bom))
    implementation(libs.micrometer.core)
//    api(libs.micrometer.registry.prometheus)
//    api(libs.micrometer.registry.otlp)
//    api(libs.micrometer.registry.datadog)
//    api(libs.micrometer.registry.newRelic)
//    api(libs.micrometer.registry.influx)

    // Compression (provided)
    api(libs.lz4.java)
    api(libs.hll)

    // Netty
    api(libs.netty.buffer)
    api(libs.netty.common)
    api(libs.netty.transport)
    api(libs.netty.handler)
    api(libs.netty.codec.http)
    api(libs.netty.codec)
    compileOnly(libs.netty.tcnative.boringssl.static)
    compileOnly(libs.netty.transport.native.epoll)
    compileOnly(libs.netty.transport.native.io.uring)
    compileOnly(libs.netty.transport.native.kqueue)

    // Logging
    api(libs.slf4j.api)

    // Jackson
    api(libs.jackson.core)
    api(libs.jackson.databind)

    // Stores (provided)
    compileOnly(libs.redisson) {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    compileOnly(libs.hazelcast)
    compileOnly(libs.jnats)
    compileOnly(libs.kafka.clients) {
        exclude(group = "org.lz4", module = "lz4-java")
    }

    // JetBrains annotations
    api(libs.jetbrains.annotations)

    // Test dependencies
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.jmockit)
    testImplementation(libs.byte.buddy.agent)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.vintage.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.testcontainers)
    testImplementation(libs.awaitility)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.logback.classic)
    testImplementation(libs.socket.io.client)
    testImplementation(libs.javafaker)
    testImplementation(libs.commons.lang3) {
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }
    testImplementation(libs.jmh.core)
    testImplementation(libs.jmh.generator.annprocess)
    testImplementation(libs.jsonassert)
    testImplementation(libs.netty.pkitesting)
}

// Platform-specific dependencies
//val osName = System.getProperty("os.name").lowercase()
//val osArch = System.getProperty("os.arch").lowercase()

//dependencies {
//    // Linux
//    if (osName.contains("linux")) {
//        "compileOnly"(libs.netty.transport.native.epoll) {
//            artifact {
//                classifier = when (osArch) {
//                    "amd64", "x86_64" -> "linux-x86_64"
//                    "aarch64", "arm64" -> "linux-aarch_64"
//                    else -> "linux-x86_64"
//                }
//            }
//        }
//        "compileOnly"(libs.netty.transport.native.io.uring) {
//            artifact {
//                classifier = when (osArch) {
//                    "amd64", "x86_64" -> "linux-x86_64"
//                    "aarch64", "arm64" -> "linux-aarch_64"
//                    else -> "linux-x86_64"
//                }
//            }
//        }
//    }
//
//    // macOS
//    if (osName.contains("mac") || osName.contains("darwin")) {
//        "compileOnly"(libs.netty.transport.native.kqueue) {
//            artifact {
//                classifier = when (osArch) {
//                    "amd64", "x86_64" -> "osx-x86_64"
//                    "aarch64", "arm64" -> "osx-aarch_64"
//                    else -> "osx-x86_64"
//                }
//            }
//        }
//    }
//}

//java {
//    sourceCompatibility = JavaVersion.VERSION_1_8
//    targetCompatibility = JavaVersion.VERSION_1_8
//    withSourcesJar()
//    withJavadocJar()
//}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Bundle-Name" to project.name,
            "Bundle-Version" to projectVersion,
            "Export-Package" to "com.socketio4j.socketio.*;version=${projectVersion}",
            "Import-Package" to "org.springframework.*;resolution:=optional,com.hazelcast.*;resolution:=optional,org.redisson.*;resolution:=optional,*",
            "Implementation-Title" to "NettySocketIO Core",
            "Implementation-Version" to projectVersion
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    // JMockit configuration
//    jvmArgs(
//        "--add-opens", "netty.socketio/com.socketio4j.socketio.store.pubsub=ALL-UNNAMED",
//        "--add-opens", "netty.socketio/com.socketio4j.socketio.store=ALL-UNNAMED",
//        "--add-opens", "netty.socketio/com.socketio4j.socketio.store.pubsub=redisson=ALL-UNNAMED",
//        "--add-opens", "netty.socketio/com.socketio4j.socketio.store=redisson=ALL-UNNAMED",
//        // JMockit agent
//        "-javaagent:${System.getProperty("user.home")}/.m2/repository/org/jmockit/jmockit/${libs.jmockit.get().version}/jmockit-${libs.jmockit.get().version}.jar",
//        // ByteBuddy agent
//        "-javaagent:${System.getProperty("user.home")}/.m2/repository/net/bytebuddy/byte-buddy-agent/${libs.byte.buddy.agent.get().version}/byte-buddy-agent-${libs.byte.buddy.agent.get().version}.jar",
//        "-Dnet.bytebuddy.experimental=true"
//    )

    forkEvery = 1
    maxParallelForks = 1

    filter {
        includeTestsMatching("*Test")
        includeTestsMatching("*Tests")
    }
}

//publishing {
//    publications {
//        create<MavenPublication>("mavenJava") {
//            from(components["java"])
//            artifact(tasks["sourcesJar"])
//            artifact(tasks["javadocJar"])
//
//            pom {
//                name.set("NettySocketIO Core")
//                description.set("Socket.IO server core implementation")
//                url.set("https://github.com/socketio4j/netty-socketio")
//
//                licenses {
//                    license {
//                        name.set("Apache License 2.0")
//                        url.set("https://www.apache.org/licenses/LICENSE-2.0.html")
//                    }
//                }
//
//                developers {
//                    developer {
//                        id.set("NeatGuyCoding")
//                        name.set("NeatGuyCoding")
//                    }
//                    developer {
//                        id.set("sanjomo")
//                        name.set("Santhosh Mohan")
//                    }
//                }
//
//                scm {
//                    url.set("https://github.com/socketio4j/netty-socketio")
//                    connection.set("scm:git:https://github.com/socketio4j/netty-socketio.git")
//                    developerConnection.set("scm:git:https://github.com/socketio4j/netty-socketio.git")
//                }
//            }
//        }
//    }
//}
//
//signing {
//    val signingKey = System.getenv("SIGNING_KEY")
//    val signingPassword = System.getenv("SIGNING_PASSWORD")
//
//    if (!signingKey.isNullOrEmpty()) {
//        useInMemoryPgpKeys(signingKey, signingPassword)
//        sign(publishing.publications["mavenJava"])
//    }
//}
