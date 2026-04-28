plugins {
    java
}

dependencies {
    api(project(":engineio-core"))

    implementation(platform(libs.netty.bom))
    implementation(libs.bundles.netty)

    implementation(libs.jackson.databind)

    testImplementation(libs.bundles.junit)
}
