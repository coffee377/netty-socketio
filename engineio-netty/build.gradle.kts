plugins {
    java
}

dependencies {
    api(project(":engineio-core"))
    compileOnly(project(":socketio-core"))

    implementation(platform(libs.netty.bom))
    implementation(libs.bundles.netty)

    implementation(libs.jackson.databind)

    testImplementation(libs.bundles.junit)
}
