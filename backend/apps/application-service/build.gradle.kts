plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencies {
    implementation(project(":libs:domain:calculator-engine"))
    implementation(project(":libs:contracts:event-envelope"))
    implementation(project(":libs:contracts:document-events"))
    implementation(project(":libs:contracts:scoring-internal-api"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    implementation("org.springframework.kafka:spring-kafka")
    implementation(libs.liquibase.core)

    runtimeOnly(libs.postgresql)

    testImplementation(project(":libs:testing:test-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
