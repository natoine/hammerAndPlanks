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

include(":input")
include(":pose")
include(":gestures")
include(":pose-onnx")
include(":game")
include(":platform:desktop")
include(":platform:android")
include(":platform:web")
