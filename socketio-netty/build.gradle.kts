plugins {
    java
}

dependencies {
    api(project(":socketio-core"))
    api(project(":engineio-core"))
    api(project(":engineio-netty"))

    implementation(platform(libs.netty.bom))
    implementation(libs.bundles.netty)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.classic)

    testImplementation(libs.bundles.junit)
}