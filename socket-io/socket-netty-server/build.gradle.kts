dependencies {
    implementation(libs.socket.io.core)
    api(libs.socket.io.netty)
    api(libs.engine.io.netty)

    implementation(libs.bundles.netty)

    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    testImplementation(libs.bundles.junit)
}
