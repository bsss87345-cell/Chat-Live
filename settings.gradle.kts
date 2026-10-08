pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // 🎙️ صوت الغرف الحيّة: حزمة Agora غير منشورة على Maven Central (4.7.0 → 404 هناك)
    maven { url = uri("https://download.agora.io/maven/") }
  }
}

rootProject.name = "Mujtamauna"

include(":app")
