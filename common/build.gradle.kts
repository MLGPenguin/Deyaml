plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    implementation(kotlin("reflect"))

    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
