dependencies {
    implementation(project(":socket-core"))

    implementation(libs.engine.io.core)
    implementation(libs.engine.io.netty)
    implementation(libs.bundles.netty)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.classic)

    testImplementation(libs.bundles.junit)
}


