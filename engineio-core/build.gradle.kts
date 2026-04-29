import org.gradle.kotlin.dsl.java

plugins {
    java
}

dependencies {

    api(libs.slf4j.api)
    api("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    implementation(libs.jackson.databind)
    testImplementation(libs.bundles.junit)
}
