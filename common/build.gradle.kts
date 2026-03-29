plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    implementation(kotlin("reflect"))

    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testFixturesImplementation("org.yaml:snakeyaml:2.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
