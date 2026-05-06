import org.gradle.kotlin.dsl.java

plugins {
    java
}

dependencies {
    implementation(project(":socketio-server"))
//    implementation(project(":netty-socketio-core"))
    implementation(libs.logback.classic)

    testImplementation(libs.bundles.junit)
}
