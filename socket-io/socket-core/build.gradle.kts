dependencies {
    api(libs.slf4j.api)
    api(project(":socket-api"))
    api(libs.engine.io.core)

    implementation(libs.jackson.databind)

    testImplementation(libs.bundles.junit)
}


