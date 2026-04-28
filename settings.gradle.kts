rootProject.name = "netty-socketio"

include("engineio-core")
include("engineio-netty")
include("socketio-core")
include("socketio-netty")
include("socketio-server")

include("netty-socketio-core")
include("examples:example-core")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()
    }

}
