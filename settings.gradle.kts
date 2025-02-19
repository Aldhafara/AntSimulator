val localProperties = file("gradle-local.properties")
if (localProperties.exists()) {
    println("Loading local Gradle properties from gradle-local.properties")
    localProperties.inputStream().use { stream ->
        val properties = java.util.Properties().apply { load(stream) }
        properties.forEach { key, value ->
            val keyStr = key.toString()
            val valueStr = value.toString()
            if (!settings.extra.has(keyStr)) {
                settings.extra[keyStr] = valueStr
                println("Loaded local property: $keyStr")
            }
        }
    }
}

rootProject.name = "AntSimulator"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":composeApp")
