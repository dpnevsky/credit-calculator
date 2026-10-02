plugins {
    base
}

allprojects {
    group = "com.dpnevsky.creditcalculator"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}
