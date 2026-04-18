plugins {
    `java-library`
}

dependencies {
    implementation(libs.poi.ooxml)

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
