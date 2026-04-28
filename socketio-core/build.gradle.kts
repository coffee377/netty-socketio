plugins {
    java
}

dependencies {
    api(project(":engineio-core"))

    implementation(libs.jackson.databind)

    testImplementation(libs.bundles.junit)
}
