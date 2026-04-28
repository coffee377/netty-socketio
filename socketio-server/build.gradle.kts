plugins {
    java
    application
}

application {
    mainClass.set("com.ccl.socketio.server.example.ChatServerExample")
}

dependencies {
    api(project(":socketio-netty"))
    api(project(":engineio-netty"))

    implementation(platform(libs.netty.bom))
    implementation(libs.bundles.netty)

    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    testImplementation(libs.bundles.junit)
}