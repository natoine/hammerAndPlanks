pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "hammerAndPlanks"

include(":game")
include(":platform:desktop")
include(":platform:android")
include(":platform:web")
