pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "credit-calculator-backend"

include(
"apps:api-gateway",
"apps:application-service",
"apps:scoring-service",
"apps:document-service",

"libs:domain:calculator-engine",

"libs:contracts:event-envelope",
"libs:contracts:document-events",
"libs:contracts:scoring-internal-api",

"libs:testing:test-support"
)
