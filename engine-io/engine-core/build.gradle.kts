
dependencies {
    api(libs.slf4j.api)
    api(project(":engine-api"))

    implementation(libs.jackson.databind)

    testImplementation(libs.bundles.junit)
}

