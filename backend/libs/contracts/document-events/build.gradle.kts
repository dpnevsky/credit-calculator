plugins {
    `java-library`
}

dependencies {
    api(project(":libs:contracts:event-envelope"))
    api(libs.jakarta.validation.api)
}