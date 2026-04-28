plugins {
    java
}

dependencies {
    api(project(":engineio-core"))

    implementation(platform(libs.netty.bom))
    implementation(libs.bundles.netty)

    testImplementation(libs.bundles.junit)
}
