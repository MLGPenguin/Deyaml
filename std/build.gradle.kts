plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":common"))
    implementation("org.yaml:snakeyaml:2.4")

    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":common")))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
