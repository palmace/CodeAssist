pluginManagement {
    repositories {
        maven { url = uri("/storage/emulated/0/Download/CodeAssistProjects/repo") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("/storage/emulated/0/Download/CodeAssistProjects/repo") }
        google()
        mavenCentral()
    }
}
rootProject.name = "$appname"
include(":app")
