plugins {
    `java-library`
}

dependencies {
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.postgresql)
    api(libs.testcontainers.kafka)
    api(libs.awaitility)
}