import org.gradle.kotlin.dsl.java

plugins {
    java
}

dependencies {

    api(libs.slf4j.api)
    implementation(libs.jackson.databind)
    testImplementation(libs.bundles.junit)
}
