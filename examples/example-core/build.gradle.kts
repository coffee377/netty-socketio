import org.gradle.kotlin.dsl.java

plugins {
    java
}

dependencies {
    implementation(project(":socketio-server"))
    implementation(libs.logback.classic)

    testImplementation(libs.bundles.junit)
}
