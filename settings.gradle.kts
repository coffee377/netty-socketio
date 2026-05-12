rootProject.name = "netty-socketio"

include("netty-socketio-core")
include("examples:example-core")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

includeBuild("engine-io") {
    name = "engineio"
    dependencySubstitution {
        arrayOf("api", "core", "netty").forEach { name ->
            substitute(module("com.ccl.io.engine:engine-${name}"))
                .using(project(":engine-$name"))
        }
    }
}

includeBuild("socket-io") {
    name = "socketio"
    dependencySubstitution {
        arrayOf("api", "core", "netty", "netty-server").forEach { name ->
            substitute(module("com.ccl.io.socket:socket-${name}"))
                .using(project(":socket-$name"))
        }
    }
}
